package io.dndroll.application;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

/** Dedicated private Help attempts; arbitrary skill rolls never supply evidence. */
public final class StabilizationService {
    public record Completion(String actor,String reason,Instant at) {}
    public record Attempt(UUID id,String status,CharacterService.Card target,CharacterService.Card helper,DiceService.Roll roll,boolean reviewed,String reason,String actor,Instant at,Completion completion) {}
    public record State(UUID id,long version,Attempt latest) {}
    public record Receipt(String fingerprint,CharacterService.Ack result) {}
    public record RollInput(UUID requestId,long expectedVersion,long targetVersion,UUID helperId,long helperVersion,boolean reviewed,String reason) {}
    public record Review(long expectedVersion,long targetVersion,UUID helperId,long helperVersion,UUID rollId,boolean reviewed) {}
    public record Apply(UUID requestId,long expectedVersion,long targetVersion,UUID helperId,long helperVersion,UUID rollId,boolean reviewed,String planHash,String reason) {
        Review review(){return new Review(expectedVersion,targetVersion,helperId,helperVersion,rollId,reviewed);}
    }
    public record Plan(UUID id,long version,long targetVersion,long helperVersion,UUID rollId,String hash,StabilizationEffect.Plan effect) {}
    public interface Store {
        State state(UUID campaign,UUID target);void save(UUID campaign,State state);List<State> history(UUID campaign,UUID target);
        Receipt receipt(String actor,UUID request);void receipt(UUID campaign,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store cards;private final DiceService dice;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public StabilizationService(Store store,CharacterService.Store cards,DiceService dice,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.cards=cards;this.dice=dice;this.events=events;this.clock=clock;this.json=json;}
    private void dm(UUID c,String actor){if(cards.role(c,actor)!=Role.DM)throw new DomainException("FORBIDDEN",403,"DM permission required");}
    private CharacterService.Card card(UUID c,UUID id){var card=cards.card(c,id);if(card==null)throw new DomainException("NOT_FOUND",404,"Character not found");return card;}
    private static void conflict(boolean allowed){if(!allowed)throw new DomainException("VERSION_CONFLICT",409,"Refresh stabilization attempt and cards");}
    private static void input(long version,long target,UUID helper,long helperVersion){if(version<0||target<0||helper==null||helperVersion<0)throw CharacterSheet.invalid();}
    private String fp(Object... value){return DiceService.fingerprint(json,Arrays.asList(value));}
    private CharacterService.Ack replay(String actor,UUID request,String fingerprint){var r=store.receipt(actor,request);if(r==null)return null;if(!r.fingerprint().equals(fingerprint))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return r.result();}
    public State state(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.state(c,id);});}
    public List<State> history(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.history(c,id);});}
    private void versions(State state,CharacterService.Card target,CharacterService.Card helper,long expected,long tv,long hv){AccountPolicy.version(state.version(),expected);AccountPolicy.version(target.version(),tv);AccountPolicy.version(helper.version(),hv);}
    private CharacterService.Ack finish(UUID c,String actor,UUID request,String fingerprint,State state){store.save(c,state);var ack=new CharacterService.Ack(state.id(),state.version());store.receipt(c,actor,request,new Receipt(fingerprint,ack));events.changed(c,true);return ack;}
    public CharacterService.Ack roll(UUID c,UUID id,String actor,RollInput i){
        input(i.expectedVersion(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.requestId()==null)throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var target=card(c,id);var helper=card(c,i.helperId());var fingerprint=fp("A16_ROLL",c,id,i);var old=replay(actor,i.requestId(),fingerprint);if(old!=null)return old;
            var state=store.state(c,id);versions(state,target,helper,i.expectedVersion(),i.targetVersion(),i.helperVersion());conflict(!target.importReview()&&!helper.importReview());
            conflict(StabilizationEffect.plan(target.sheet(),helper.sheet(),i.requestId(),i.reviewed(),!id.equals(helper.id()),true,10).status().equals("CANDIDATE"));
            var child=UUID.nameUUIDFromBytes(("A16_STABILIZATION:"+i.requestId()).getBytes(StandardCharsets.UTF_8));var roll=dice.plannedStabilization(c,actor,child,helper,id,target.version(),state.version());
            var attempt=new Attempt(roll.id(),"ROLLED",target,helper,roll,i.reviewed(),i.reason(),actor,clock.instant(),null);return finish(c,actor,i.requestId(),fingerprint,new State(id,state.version()+1,attempt));
        }));
    }
    private Plan plan(UUID c,String actor,State state,CharacterService.Card target,CharacterService.Card helper,Review i){
        versions(state,target,helper,i.expectedVersion(),i.targetVersion(),i.helperVersion());var a=state.latest();conflict(a!=null&&a.status().equals("ROLLED")&&a.id().equals(i.rollId()));var roll=dice.detail(c,i.rollId(),actor);
        boolean bound=a.target().equals(target)&&a.helper().equals(helper)&&a.roll().equals(roll)&&a.reviewed()&&roll.characterId().equals(helper.id().toString())&&roll.sourceVersion()==helper.version()
            &&roll.key().equals("skill:medicine")&&roll.visibility().equals("DM")&&roll.advantage().isEmpty()&&roll.disadvantage().isEmpty()&&roll.calculation().equals(helper.sheet().calculate().get("skill:medicine"))&&!target.importReview()&&!helper.importReview();
        var effect=StabilizationEffect.plan(target.sheet(),helper.sheet(),a.id(),i.reviewed(),!target.id().equals(helper.id()),bound,roll.result().total());return new Plan(target.id(),state.version(),target.version(),helper.version(),a.id(),fp("A16_PLAN",c,state,target,helper,i,effect),effect);
    }
    public Plan preview(UUID c,UUID id,String actor,Review i){input(i.expectedVersion(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.rollId()==null)throw CharacterSheet.invalid();return cards.locked(c,()->{dm(c,actor);return plan(c,actor,store.state(c,id),card(c,id),card(c,i.helperId()),i);});}
    public CharacterService.Ack apply(UUID c,UUID id,String actor,Apply i){
        input(i.expectedVersion(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.requestId()==null||i.rollId()==null||i.planHash()==null||!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var before=card(c,id);var helper=card(c,i.helperId());var fingerprint=fp("A16_APPLY",c,id,i);var old=replay(actor,i.requestId(),fingerprint);if(old!=null)return old;
            var state=store.state(c,id);var plan=plan(c,actor,state,before,helper,i.review());conflict(plan.hash().equals(i.planHash())&&plan.effect().status().equals("CANDIDATE"));var e=plan.effect();var s=before.sheet();var b=s.combat();
            var combat=new CharacterSheet.Combat(b.maxHp(),e.hp(),e.tempHp(),b.speed(),e.deathSuccesses(),e.deathFailures(),b.acMode(),b.manualAc(),b.source());var sheet=s.change(s.name(),combat,s.resources(),e.annotations(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(id,c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);cards.save(after,false);
            cards.history(new CharacterService.History(after.version(),actor,"STABILIZATION","DM确认伤势稳定；仍为0HP且昏迷。",clock.instant(),before,after));
            var a=state.latest();var applied=new Attempt(a.id(),"APPLIED",a.target(),a.helper(),a.roll(),a.reviewed(),a.reason(),a.actor(),a.at(),new Completion(actor,i.reason(),clock.instant()));var ack=finish(c,actor,i.requestId(),fingerprint,new State(id,state.version()+1,applied));events.changed(c,false);return ack;
        }));
    }
}
