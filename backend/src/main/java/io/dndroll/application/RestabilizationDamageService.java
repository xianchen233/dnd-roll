package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** First bounded damage to the unchanged original A19 result, preserving its historical lineage. */
public final class RestabilizationDamageService {
    public record Review(long expectedVersion,long originVersion,long targetVersion,UUID attemptId,int amount,boolean critical,boolean reviewed) {}
    public record Apply(UUID requestId,long expectedVersion,long originVersion,long targetVersion,UUID attemptId,int amount,boolean critical,boolean reviewed,String planHash,String reason) {Review review(){return new Review(expectedVersion,originVersion,targetVersion,attemptId,amount,critical,reviewed);}}
    public record Plan(UUID id,long version,long originVersion,long targetVersion,UUID attemptId,String hash,RestabilizationDamageEffect.Plan effect) {}
    public record Entry(UUID id,long version,long originVersion,UUID attemptId,String actor,Instant at,String reason,Review review,String planHash,CharacterService.Card before,CharacterService.Card after) {}
    public interface Store {void append(UUID c,Entry e);List<Entry> history(UUID c,UUID id);}
    private record Origin(List<CharacterSheet.Condition> canonical,boolean unchanged) {}
    private final Store store;private final RestabilizationService.Store restabilization;private final StabilizationService.Store parent;private final StabilizationDamageService.Store damage;private final CharacterService.Store cards;private final DiceService dice;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public RestabilizationDamageService(Store store,RestabilizationService.Store restabilization,StabilizationService.Store parent,StabilizationDamageService.Store damage,CharacterService.Store cards,DiceService dice,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.restabilization=restabilization;this.parent=parent;this.damage=damage;this.cards=cards;this.dice=dice;this.events=events;this.clock=clock;this.json=json;}
    private void dm(UUID c,String actor){if(cards.role(c,actor)!=Role.DM)throw new DomainException("FORBIDDEN",403,"DM permission required");}
    private CharacterService.Card card(UUID c,UUID id){var v=cards.card(c,id);if(v==null)throw new DomainException("NOT_FOUND",404,"Character not found");return v;}
    private static void conflict(boolean ok){if(!ok)throw new DomainException("VERSION_CONFLICT",409,"Refresh restabilization origin and target");}
    private static void input(Review i){if(i.expectedVersion()<0||i.originVersion()<0||i.targetVersion()<0||i.attemptId()==null||i.amount()<0||i.amount()>220000)throw CharacterSheet.invalid();}
    private String fp(Object... values){return DiceService.fingerprint(json,Arrays.asList(values));}
    private CharacterService.Card changed(CharacterService.Card before,int hp,int temp,int successes,int failures,List<CharacterSheet.Condition> conditions){var s=before.sheet();var b=s.combat();var combat=new CharacterSheet.Combat(b.maxHp(),hp,temp,b.speed(),successes,failures,b.acMode(),b.manualAc(),b.source());return new CharacterService.Card(before.id(),before.campaignId(),before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),s.change(s.name(),combat,s.resources(),conditions,s.overrides()));}
    private boolean bound(DiceService.Roll saved,DiceService.Roll actual,CharacterService.Card helper){return saved.equals(actual)&&actual.id().equals(saved.id())&&actual.characterId().equals(helper.id().toString())&&actual.sourceVersion()==helper.version()&&actual.visibility().equals("DM")&&actual.key().equals("skill:medicine")&&actual.advantage().isEmpty()&&actual.disadvantage().isEmpty()&&actual.calculation().equals(helper.sheet().calculate().get("skill:medicine"));}
    private Origin origin(UUID c,String actor,CharacterService.Card target,RestabilizationService.Attempt a,StabilizationService.State source){
        var old=source.latest();if(a.completion()==null||old==null||old.completion()==null||!old.status().equals("RESTABILIZED")||source.version()!=a.originVersion()+1)return new Origin(null,false);
        var prior=parent.history(c,target.id()).stream().filter(v->v.version()==a.originVersion()).findFirst().orElse(null);
        if(prior==null||prior.latest()==null||!prior.latest().status().equals("UNSTABLE"))return new Origin(null,false);
        var o=prior.latest();var expectedOld=new StabilizationService.Attempt(o.id(),"RESTABILIZED",o.target(),o.helper(),o.roll(),o.reviewed(),o.reason(),o.actor(),o.at(),o.completion());if(!expectedOld.equals(old))return new Origin(null,false);
        var j=damage.history(c,target.id()).stream().filter(v->v.version()==a.originVersion()).findFirst().orElse(null);
        if(j==null||!j.attemptId().equals(o.id())||!j.review().attemptId().equals(o.id())||j.review().expectedVersion()!=a.originVersion()-1)return new Origin(null,false);
        var roll=dice.detail(c,o.id(),actor);var stable=StabilizationEffect.plan(o.target().sheet(),o.helper().sheet(),o.id(),o.reviewed(),!o.target().id().equals(o.helper().id()),bound(o.roll(),roll,o.helper()),roll.result().total());if(!stable.status().equals("CANDIDATE"))return new Origin(null,false);
        var before=changed(o.target(),0,o.target().sheet().combat().tempHp(),0,0,stable.annotations());if(!before.equals(j.before())||j.review().targetVersion()!=before.version())return new Origin(null,false);
        var hit=StabilizationDamageEffect.plan(before.sheet(),stable.annotations(),true,j.review().amount(),j.review().critical(),j.review().reviewed());if(!hit.status().equals("CANDIDATE"))return new Origin(null,false);
        var unstable=changed(before,hit.hp(),hit.tempHp(),hit.deathSuccesses(),hit.deathFailures(),before.sheet().conditions().stream().filter(v->!hit.removed().contains(v.id())).toList());if(!unstable.equals(j.after())||!unstable.equals(a.target()))return new Origin(null,false);
        var newRoll=dice.detail(c,a.id(),actor);var again=RestabilizationEffect.plan(a.target().sheet(),a.helper().sheet(),stable.annotations().get(1),a.id(),true,a.reviewed(),!a.target().id().equals(a.helper().id()),a.id().equals(newRoll.id())&&bound(a.roll(),newRoll,a.helper()),newRoll.result().total());if(!again.status().equals("CANDIDATE"))return new Origin(null,false);
        var applied=changed(a.target(),0,again.tempHp(),0,0,again.annotations());return new Origin(again.annotations(),applied.equals(target)&&!target.importReview());
    }
    private Plan plan(UUID c,String actor,CharacterService.Card target,RestabilizationService.State state,StabilizationService.State source,Review i){
        AccountPolicy.version(state.version(),i.expectedVersion());AccountPolicy.version(source.version(),i.originVersion());AccountPolicy.version(target.version(),i.targetVersion());var a=state.latest();conflict(a!=null&&a.status().equals("APPLIED")&&a.id().equals(i.attemptId()));var o=origin(c,actor,target,a,source);var effect=RestabilizationDamageEffect.plan(target.sheet(),o.canonical(),o.unchanged(),i.amount(),i.critical(),i.reviewed());return new Plan(target.id(),state.version(),source.version(),target.version(),a.id(),fp("A21_PLAN",c,state,source,target,i,effect),effect);
    }
    public List<Entry> history(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.history(c,id);});}
    public Plan preview(UUID c,UUID id,String actor,Review i){input(i);return cards.locked(c,()->{dm(c,actor);return plan(c,actor,card(c,id),restabilization.state(c,id),parent.state(c,id),i);});}
    public CharacterService.Ack apply(UUID c,UUID id,String actor,Apply i){
        input(i.review());if(i.requestId()==null||i.planHash()==null||!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{dm(c,actor);var before=card(c,id);String fingerprint=fp("A21_DAMAGE",c,id,i);var receipt=parent.receipt(actor,i.requestId());if(receipt!=null){if(!receipt.fingerprint().equals(fingerprint))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return receipt.result();}
            var state=restabilization.state(c,id);var source=parent.state(c,id);var p=plan(c,actor,before,state,source,i.review());conflict(p.hash().equals(i.planHash())&&p.effect().status().equals("CANDIDATE"));var e=p.effect();var after=changed(before,e.hp(),e.tempHp(),e.deathSuccesses(),e.deathFailures(),before.sheet().conditions().stream().filter(v->!e.removed().contains(v.id())).toList());after.sheet().validate();cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"RESTABILIZATION_DAMAGE","DM确认原再次稳定目标受伤失稳；仍0HP且昏迷，后续死豁由DM处理。",clock.instant(),before,after));
            var a=state.latest();var next=new RestabilizationService.State(id,state.version()+1,new RestabilizationService.Attempt(a.id(),"UNSTABLE",a.originVersion(),a.target(),a.helper(),a.roll(),a.reviewed(),a.reason(),a.actor(),a.at(),a.completion()));restabilization.save(c,next);var old=source.latest();var parentNext=new StabilizationService.State(id,source.version()+1,new StabilizationService.Attempt(old.id(),"RESTABILIZATION_UNSTABLE",old.target(),old.helper(),old.roll(),old.reviewed(),old.reason(),old.actor(),old.at(),old.completion()));parent.save(c,parentNext);
            store.append(c,new Entry(id,next.version(),parentNext.version(),a.id(),actor,clock.instant(),i.reason(),i.review(),i.planHash(),before,after));var ack=new CharacterService.Ack(id,next.version());parent.receipt(c,actor,i.requestId(),new StabilizationService.Receipt(fingerprint,ack));events.changed(c,false);return ack;
        }));
    }
}
