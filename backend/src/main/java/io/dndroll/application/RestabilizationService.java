package io.dndroll.application;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

/** A19 keeps new checks separate from the immutable original A16/A18 lineage. */
public final class RestabilizationService {
    public record Attempt(UUID id,String status,long originVersion,CharacterService.Card target,CharacterService.Card helper,DiceService.Roll roll,boolean reviewed,String reason,String actor,Instant at,StabilizationService.Completion completion) {}
    public record State(UUID id,long version,Attempt latest) {}
    public record RollInput(UUID requestId,long expectedVersion,long originVersion,long targetVersion,UUID helperId,long helperVersion,boolean reviewed,String reason) {}
    public record Review(long expectedVersion,long originVersion,long targetVersion,UUID helperId,long helperVersion,UUID rollId,boolean reviewed) {}
    public record Apply(UUID requestId,long expectedVersion,long originVersion,long targetVersion,UUID helperId,long helperVersion,UUID rollId,boolean reviewed,String planHash,String reason){Review review(){return new Review(expectedVersion,originVersion,targetVersion,helperId,helperVersion,rollId,reviewed);}}
    public record Plan(UUID id,long version,long originVersion,long targetVersion,long helperVersion,UUID rollId,String hash,RestabilizationEffect.Plan effect) {}
    public interface Store {State state(UUID c,UUID id);void save(UUID c,State state);List<State> history(UUID c,UUID id);}
    private record Origin(CharacterSheet.Condition unconscious,boolean unchanged) {}
    private final Store store;private final StabilizationService.Store parent;private final StabilizationDamageService.Store damage;private final CharacterService.Store cards;private final DiceService dice;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public RestabilizationService(Store store,StabilizationService.Store parent,StabilizationDamageService.Store damage,CharacterService.Store cards,DiceService dice,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.parent=parent;this.damage=damage;this.cards=cards;this.dice=dice;this.events=events;this.clock=clock;this.json=json;}
    private void dm(UUID c,String actor){if(cards.role(c,actor)!=Role.DM)throw new DomainException("FORBIDDEN",403,"DM permission required");}
    private CharacterService.Card card(UUID c,UUID id){var value=cards.card(c,id);if(value==null)throw new DomainException("NOT_FOUND",404,"Character not found");return value;}
    private static void conflict(boolean ok){if(!ok)throw new DomainException("VERSION_CONFLICT",409,"Refresh restabilization source and cards");}
    private static void input(long v,long origin,long target,UUID helper,long hv){if(v<0||origin<0||target<0||helper==null||hv<0)throw CharacterSheet.invalid();}
    private String fp(Object... values){return DiceService.fingerprint(json,Arrays.asList(values));}
    private CharacterService.Ack replay(String actor,UUID request,String fingerprint){var r=parent.receipt(actor,request);if(r==null)return null;if(!r.fingerprint().equals(fingerprint))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return r.result();}
    private CharacterService.Card changed(CharacterService.Card before,long version,CharacterSheet.Combat combat,List<CharacterSheet.Condition> conditions){var s=before.sheet();return new CharacterService.Card(before.id(),before.campaignId(),version,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),s.change(s.name(),combat,s.resources(),conditions,s.overrides()));}
    private boolean bound(DiceService.Roll saved,DiceService.Roll actual,CharacterService.Card helper){return saved.equals(actual)&&actual.characterId().equals(helper.id().toString())&&actual.sourceVersion()==helper.version()&&actual.visibility().equals("DM")&&actual.key().equals("skill:medicine")&&actual.advantage().isEmpty()&&actual.disadvantage().isEmpty()&&actual.calculation().equals(helper.sheet().calculate().get("skill:medicine"));}
    private Origin origin(UUID c,String actor,CharacterService.Card target,StabilizationService.State state){
        var a=state.latest();if(a==null||!a.status().equals("UNSTABLE")||a.completion()==null)return new Origin(null,false);
        var j=damage.history(c,target.id()).stream().filter(e->e.version()==state.version()).findFirst().orElse(null);if(j==null||!j.id().equals(target.id())||!j.attemptId().equals(a.id())||!j.review().attemptId().equals(a.id())||j.review().expectedVersion()!=state.version()-1)return new Origin(null,false);
        var oldRoll=dice.detail(c,a.id(),actor);var stable=StabilizationEffect.plan(a.target().sheet(),a.helper().sheet(),a.id(),a.reviewed(),!a.target().id().equals(a.helper().id()),bound(a.roll(),oldRoll,a.helper()),oldRoll.result().total());if(!stable.status().equals("CANDIDATE"))return new Origin(null,false);
        var b=a.target().sheet().combat();var before=changed(a.target(),a.target().version()+1,new CharacterSheet.Combat(b.maxHp(),0,b.tempHp(),b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),stable.annotations());
        if(!before.equals(j.before())||j.review().targetVersion()!=before.version())return new Origin(null,false);
        var hit=StabilizationDamageEffect.plan(before.sheet(),stable.annotations(),true,j.review().amount(),j.review().critical(),j.review().reviewed());if(!hit.status().equals("CANDIDATE"))return new Origin(null,false);
        b=before.sheet().combat();var after=changed(before,before.version()+1,new CharacterSheet.Combat(b.maxHp(),hit.hp(),hit.tempHp(),b.speed(),hit.deathSuccesses(),hit.deathFailures(),b.acMode(),b.manualAc(),b.source()),before.sheet().conditions().stream().filter(v->!hit.removed().contains(v.id())).toList());
        return new Origin(stable.annotations().get(1),after.equals(j.after())&&target.equals(after)&&!target.importReview());
    }
    public State state(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.state(c,id);});}
    public List<State> history(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.history(c,id);});}
    private CharacterService.Ack finish(UUID c,String actor,UUID request,String fingerprint,State state,boolean dmOnly){store.save(c,state);var ack=new CharacterService.Ack(state.id(),state.version());parent.receipt(c,actor,request,new StabilizationService.Receipt(fingerprint,ack));events.changed(c,dmOnly);return ack;}
    public CharacterService.Ack roll(UUID c,UUID id,String actor,RollInput i){
        input(i.expectedVersion(),i.originVersion(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.requestId()==null)throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{dm(c,actor);var target=card(c,id);var helper=card(c,i.helperId());String fingerprint=fp("A19_ROLL",c,id,i);var old=replay(actor,i.requestId(),fingerprint);if(old!=null)return old;
            var state=store.state(c,id);var source=parent.state(c,id);AccountPolicy.version(state.version(),i.expectedVersion());AccountPolicy.version(source.version(),i.originVersion());AccountPolicy.version(target.version(),i.targetVersion());AccountPolicy.version(helper.version(),i.helperVersion());var o=origin(c,actor,target,source);
            conflict(!helper.importReview()&&RestabilizationEffect.plan(target.sheet(),helper.sheet(),o.unconscious(),i.requestId(),o.unchanged(),i.reviewed(),!id.equals(helper.id()),true,10).status().equals("CANDIDATE"));
            var child=UUID.nameUUIDFromBytes(("A19_RESTABILIZATION:"+i.requestId()).getBytes(StandardCharsets.UTF_8));var roll=dice.plannedRestabilization(c,actor,child,helper,id,target.version(),source.version(),state.version());var a=new Attempt(roll.id(),"ROLLED",source.version(),target,helper,roll,i.reviewed(),i.reason(),actor,clock.instant(),null);return finish(c,actor,i.requestId(),fingerprint,new State(id,state.version()+1,a),true);
        }));
    }
    private Plan plan(UUID c,String actor,State state,StabilizationService.State source,CharacterService.Card target,CharacterService.Card helper,Review i){
        AccountPolicy.version(state.version(),i.expectedVersion());AccountPolicy.version(source.version(),i.originVersion());AccountPolicy.version(target.version(),i.targetVersion());AccountPolicy.version(helper.version(),i.helperVersion());var a=state.latest();conflict(a!=null&&a.status().equals("ROLLED")&&a.id().equals(i.rollId())&&a.originVersion()==i.originVersion());var roll=dice.detail(c,a.id(),actor);var o=origin(c,actor,target,source);
        boolean bound=a.target().equals(target)&&a.helper().equals(helper)&&a.reviewed()&&bound(a.roll(),roll,helper)&&!target.importReview()&&!helper.importReview();var effect=RestabilizationEffect.plan(target.sheet(),helper.sheet(),o.unconscious(),a.id(),o.unchanged(),i.reviewed(),!target.id().equals(helper.id()),bound,roll.result().total());return new Plan(target.id(),state.version(),source.version(),target.version(),helper.version(),a.id(),fp("A19_PLAN",c,state,source,target,helper,i,effect),effect);
    }
    public Plan preview(UUID c,UUID id,String actor,Review i){input(i.expectedVersion(),i.originVersion(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.rollId()==null)throw CharacterSheet.invalid();return cards.locked(c,()->{dm(c,actor);return plan(c,actor,store.state(c,id),parent.state(c,id),card(c,id),card(c,i.helperId()),i);});}
    public CharacterService.Ack apply(UUID c,UUID id,String actor,Apply i){
        input(i.expectedVersion(),i.originVersion(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.requestId()==null||i.rollId()==null||i.planHash()==null||!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{dm(c,actor);var before=card(c,id);var helper=card(c,i.helperId());String fingerprint=fp("A19_APPLY",c,id,i);var old=replay(actor,i.requestId(),fingerprint);if(old!=null)return old;var state=store.state(c,id);var source=parent.state(c,id);var p=plan(c,actor,state,source,before,helper,i.review());conflict(p.hash().equals(i.planHash())&&p.effect().status().equals("CANDIDATE"));var e=p.effect();var b=before.sheet().combat();var after=changed(before,before.version()+1,new CharacterSheet.Combat(b.maxHp(),0,e.tempHp(),b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),e.annotations());after.sheet().validate();cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"RESTABILIZATION","DM确认再次稳定；仍0HP且昏迷，死豁计数归零。",clock.instant(),before,after));
            var prior=source.latest();parent.save(c,new StabilizationService.State(id,source.version()+1,new StabilizationService.Attempt(prior.id(),"RESTABILIZED",prior.target(),prior.helper(),prior.roll(),prior.reviewed(),prior.reason(),prior.actor(),prior.at(),prior.completion())));
            var a=state.latest();var applied=new Attempt(a.id(),"APPLIED",a.originVersion(),a.target(),a.helper(),a.roll(),a.reviewed(),a.reason(),a.actor(),a.at(),new StabilizationService.Completion(actor,i.reason(),clock.instant()));return finish(c,actor,i.requestId(),fingerprint,new State(id,state.version()+1,applied),false);
        }));
    }
}
