package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** First bounded adjudicated damage to an unchanged, durably applied A16 outcome. */
public final class StabilizationDamageService {
    public record Review(long expectedVersion,long targetVersion,UUID attemptId,int amount,boolean critical,boolean reviewed) {}
    public record Apply(UUID requestId,long expectedVersion,long targetVersion,UUID attemptId,int amount,boolean critical,boolean reviewed,String planHash,String reason) {
        Review review(){return new Review(expectedVersion,targetVersion,attemptId,amount,critical,reviewed);}
    }
    public record Plan(UUID id,long version,long targetVersion,UUID attemptId,String hash,StabilizationDamageEffect.Plan effect) {}
    public record Entry(UUID id,long version,UUID attemptId,String actor,Instant at,String reason,Review review,String planHash,CharacterService.Card before,CharacterService.Card after) {}
    public interface Store { void append(UUID campaign,Entry entry);List<Entry> history(UUID campaign,UUID target); }
    private final Store store;private final StabilizationService.Store stabilization;private final CharacterService.Store cards;private final DiceService dice;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public StabilizationDamageService(Store store,StabilizationService.Store stabilization,CharacterService.Store cards,DiceService dice,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.stabilization=stabilization;this.cards=cards;this.dice=dice;this.events=events;this.clock=clock;this.json=json;}
    private void dm(UUID c,String actor){if(cards.role(c,actor)!=Role.DM)throw new DomainException("FORBIDDEN",403,"DM permission required");}
    private CharacterService.Card card(UUID c,UUID id){var value=cards.card(c,id);if(value==null)throw new DomainException("NOT_FOUND",404,"Character not found");return value;}
    private static void conflict(boolean allowed){if(!allowed)throw new DomainException("VERSION_CONFLICT",409,"Refresh stabilization origin and target");}
    private static void input(Review i){if(i.expectedVersion()<0||i.targetVersion()<0||i.attemptId()==null||i.amount()<0||i.amount()>220000)throw CharacterSheet.invalid();}
    private String fp(Object... values){return DiceService.fingerprint(json,Arrays.asList(values));}
    public List<Entry> history(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.history(c,id);});}
    private Plan plan(UUID c,String actor,CharacterService.Card current,StabilizationService.State state,Review i){
        AccountPolicy.version(state.version(),i.expectedVersion());AccountPolicy.version(current.version(),i.targetVersion());
        var a=state.latest();conflict(a!=null&&a.status().equals("APPLIED")&&a.id().equals(i.attemptId()));
        var roll=dice.detail(c,a.id(),actor);
        boolean bound=a.roll().equals(roll)&&a.id().equals(roll.id())&&roll.visibility().equals("DM")&&roll.characterId().equals(a.helper().id().toString())&&roll.key().equals("skill:medicine")&&roll.sourceVersion()==a.helper().version()&&roll.calculation().equals(a.helper().sheet().calculate().get("skill:medicine"))&&roll.advantage().isEmpty()&&roll.disadvantage().isEmpty();
        var original=StabilizationEffect.plan(a.target().sheet(),a.helper().sheet(),a.id(),a.reviewed(),!a.target().id().equals(a.helper().id()),bound,roll.result().total());
        List<CharacterSheet.Condition> canonical=original.status().equals("CANDIDATE")?original.annotations():null;
        boolean unchanged=false;
        if(canonical!=null){var s=a.target().sheet();var b=s.combat();var combat=new CharacterSheet.Combat(b.maxHp(),0,b.tempHp(),b.speed(),0,0,b.acMode(),b.manualAc(),b.source());var expected=new CharacterService.Card(a.target().id(),c,a.target().version()+1,a.target().hidden(),a.target().shareFull(),a.target().importReview(),a.target().controllers(),s.change(s.name(),combat,s.resources(),canonical,s.overrides()));unchanged=current.equals(expected)&&a.completion()!=null&&!current.importReview();}
        var effect=StabilizationDamageEffect.plan(current.sheet(),canonical,unchanged,i.amount(),i.critical(),i.reviewed());
        return new Plan(current.id(),state.version(),current.version(),a.id(),fp("A18_PLAN",c,state,current,i,effect),effect);
    }
    public Plan preview(UUID c,UUID id,String actor,Review i){input(i);return cards.locked(c,()->{dm(c,actor);return plan(c,actor,card(c,id),stabilization.state(c,id),i);});}
    public CharacterService.Ack apply(UUID c,UUID id,String actor,Apply i){
        input(i.review());if(i.requestId()==null||i.planHash()==null||!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var before=card(c,id);String fingerprint=fp("A18_HEAL",c,id,i);var old=stabilization.receipt(actor,i.requestId());
            if(old!=null){if(!old.fingerprint().equals(fingerprint))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old.result();}
            var state=stabilization.state(c,id);var p=plan(c,actor,before,state,i.review());conflict(p.hash().equals(i.planHash())&&p.effect().status().equals("CANDIDATE"));var e=p.effect();var s=before.sheet();var b=s.combat();
            var combat=new CharacterSheet.Combat(b.maxHp(),e.hp(),e.tempHp(),b.speed(),e.deathSuccesses(),e.deathFailures(),b.acMode(),b.manualAc(),b.source());var sheet=s.change(s.name(),combat,s.resources(),s.conditions().stream().filter(v->!e.removed().contains(v.id())).toList(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(id,c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"STABILIZATION_DAMAGE","DM确认原稳定角色受伤失稳；仍0HP且昏迷，后续死豁由DM处理。",clock.instant(),before,after));
            var a=state.latest();var unstable=new StabilizationService.Attempt(a.id(),"UNSTABLE",a.target(),a.helper(),a.roll(),a.reviewed(),a.reason(),a.actor(),a.at(),a.completion());var next=new StabilizationService.State(id,state.version()+1,unstable);stabilization.save(c,next);
            store.append(c,new Entry(id,next.version(),a.id(),actor,clock.instant(),i.reason(),i.review(),i.planHash(),before,after));var ack=new CharacterService.Ack(id,next.version());stabilization.receipt(c,actor,i.requestId(),new StabilizationService.Receipt(fingerprint,ack));events.changed(c,false);return ack;
        }));
    }
}
