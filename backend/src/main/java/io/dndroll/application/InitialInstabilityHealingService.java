package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Explicit adjudicated healing of an unchanged, durably bound A18 outcome. */
public final class InitialInstabilityHealingService {
    public record Review(long expectedVersion,long targetVersion,UUID attemptId,int amount,boolean reviewed) {}
    public record Apply(UUID requestId,long expectedVersion,long targetVersion,UUID attemptId,int amount,boolean reviewed,String planHash,String reason) {
        Review review(){return new Review(expectedVersion,targetVersion,attemptId,amount,reviewed);}
    }
    public record Plan(UUID id,long version,long damageVersion,long targetVersion,UUID attemptId,String hash,InitialInstabilityHealingEffect.Plan effect) {}
    public record Entry(UUID id,long version,long damageVersion,UUID attemptId,String actor,Instant at,String reason,Review review,String planHash,CharacterService.Card before,CharacterService.Card after) {}
    public interface Store { void append(UUID campaign,Entry entry);List<Entry> history(UUID campaign,UUID target); }
    private record Origin(List<CharacterSheet.Condition> canonical,boolean unchanged,StabilizationDamageService.Entry journal) {}
    private final StabilizationDamageService.Store damage;private final Store store;private final StabilizationService.Store stabilization;private final CharacterService.Store cards;private final DiceService dice;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public InitialInstabilityHealingService(Store store,StabilizationDamageService.Store damage,StabilizationService.Store stabilization,CharacterService.Store cards,DiceService dice,DiceService.Events events,Clock clock,JsonMapper json){this.damage=damage;this.store=store;this.stabilization=stabilization;this.cards=cards;this.dice=dice;this.events=events;this.clock=clock;this.json=json;}
    private void dm(UUID c,String actor){if(cards.role(c,actor)!=Role.DM)throw new DomainException("FORBIDDEN",403,"DM permission required");}
    private CharacterService.Card card(UUID c,UUID id){var value=cards.card(c,id);if(value==null)throw new DomainException("NOT_FOUND",404,"Character not found");return value;}
    private static void conflict(boolean allowed){if(!allowed)throw new DomainException("VERSION_CONFLICT",409,"Refresh stabilization origin and target");}
    private static void input(Review i){if(i.expectedVersion()<0||i.targetVersion()<0||i.attemptId()==null||i.amount()<1||i.amount()>100000)throw CharacterSheet.invalid();}
    private String fp(Object... values){return DiceService.fingerprint(json,Arrays.asList(values));}
    public List<Entry> history(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);card(c,id);return store.history(c,id);});}
    private CharacterService.Card changed(CharacterService.Card before,int hp,int temp,int successes,int failures,List<CharacterSheet.Condition> conditions){var sheet=before.sheet();var b=sheet.combat();var combat=new CharacterSheet.Combat(b.maxHp(),hp,temp,b.speed(),successes,failures,b.acMode(),b.manualAc(),b.source());return new CharacterService.Card(before.id(),before.campaignId(),before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet.change(sheet.name(),combat,sheet.resources(),conditions,sheet.overrides()));}
    private Origin origin(UUID c,String actor,CharacterService.Card current,StabilizationService.State state){
        var a=state.latest();if(a.completion()==null)return new Origin(null,false,null);
        var prior=stabilization.history(c,current.id()).stream().filter(v->v.version()==state.version()-1).findFirst().orElse(null);
        if(prior==null||prior.latest()==null||!prior.latest().status().equals("APPLIED"))return new Origin(null,false,null);
        var old=prior.latest();var expected=new StabilizationService.Attempt(old.id(),"UNSTABLE",old.target(),old.helper(),old.roll(),old.reviewed(),old.reason(),old.actor(),old.at(),old.completion());if(!expected.equals(a))return new Origin(null,false,null);
        var j=damage.history(c,current.id()).stream().filter(v->v.version()==state.version()).findFirst().orElse(null);
        if(j==null||!j.id().equals(current.id())||!j.attemptId().equals(a.id())||!j.review().attemptId().equals(a.id())||j.review().expectedVersion()!=state.version()-1||j.review().targetVersion()!=j.before().version())return new Origin(null,false,j);
        var roll=dice.detail(c,a.id(),actor);boolean bound=a.roll().equals(roll)&&a.id().equals(roll.id())&&roll.visibility().equals("DM")&&roll.characterId().equals(a.helper().id().toString())&&roll.key().equals("skill:medicine")&&roll.sourceVersion()==a.helper().version()&&roll.calculation().equals(a.helper().sheet().calculate().get("skill:medicine"))&&roll.advantage().isEmpty()&&roll.disadvantage().isEmpty();
        var original=StabilizationEffect.plan(a.target().sheet(),a.helper().sheet(),a.id(),a.reviewed(),!a.target().id().equals(a.helper().id()),bound,roll.result().total());if(!original.status().equals("CANDIDATE"))return new Origin(null,false,j);
        var before=changed(a.target(),0,a.target().sheet().combat().tempHp(),0,0,original.annotations());if(!before.equals(j.before()))return new Origin(null,false,j);
        var hit=StabilizationDamageEffect.plan(before.sheet(),original.annotations(),true,j.review().amount(),j.review().critical(),j.review().reviewed());if(!hit.status().equals("CANDIDATE")||!j.planHash().equals(fp("A18_PLAN",c,prior,before,j.review(),hit)))return new Origin(null,false,j);
        var after=changed(before,hit.hp(),hit.tempHp(),hit.deathSuccesses(),hit.deathFailures(),before.sheet().conditions().stream().filter(v->!hit.removed().contains(v.id())).toList());return new Origin(List.of(original.annotations().get(1)),after.equals(j.after())&&current.equals(after)&&!current.importReview(),j);
    }
    private Plan plan(UUID c,String actor,CharacterService.Card current,StabilizationService.State state,Review i){
        AccountPolicy.version(state.version(),i.expectedVersion());AccountPolicy.version(current.version(),i.targetVersion());var a=state.latest();conflict(a!=null&&a.status().equals("UNSTABLE")&&a.id().equals(i.attemptId()));var o=origin(c,actor,current,state);var effect=InitialInstabilityHealingEffect.plan(current.sheet(),o.canonical(),o.unchanged(),i.amount(),i.reviewed());return new Plan(current.id(),state.version(),state.version(),current.version(),a.id(),fp("A23_PLAN",c,state,current,o.journal(),i,effect),effect);
    }
    public Plan preview(UUID c,UUID id,String actor,Review i){input(i);return cards.locked(c,()->{dm(c,actor);return plan(c,actor,card(c,id),stabilization.state(c,id),i);});}
    public CharacterService.Ack apply(UUID c,UUID id,String actor,Apply i){
        input(i.review());if(i.requestId()==null||i.planHash()==null||!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var before=card(c,id);String fingerprint=fp("A23_HEAL",c,id,i);var old=stabilization.receipt(actor,i.requestId());
            if(old!=null){if(!old.fingerprint().equals(fingerprint))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old.result();}
            var state=stabilization.state(c,id);var p=plan(c,actor,before,state,i.review());conflict(p.hash().equals(i.planHash())&&p.effect().status().equals("CANDIDATE"));var e=p.effect();var s=before.sheet();var b=s.combat();
            var combat=new CharacterSheet.Combat(b.maxHp(),e.hp(),e.tempHp(),b.speed(),e.deathSuccesses(),e.deathFailures(),b.acMode(),b.manualAc(),b.source());var sheet=s.change(s.name(),combat,s.resources(),s.conditions().stream().filter(v->!e.removed().contains(v.id())).toList(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(id,c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"INITIAL_INSTABILITY_HEALING","DM确认首次失稳后治疗恢复生命，死豁归零并解除原0HP昏迷记录；其他效果由DM处理。",clock.instant(),before,after));
            var a=state.latest();var healed=new StabilizationService.Attempt(a.id(),"HEALED",a.target(),a.helper(),a.roll(),a.reviewed(),a.reason(),a.actor(),a.at(),a.completion());var next=new StabilizationService.State(id,state.version()+1,healed);stabilization.save(c,next);
            store.append(c,new Entry(id,next.version(),p.damageVersion(),a.id(),actor,clock.instant(),i.reason(),i.review(),i.planHash(),before,after));var ack=new CharacterService.Ack(id,next.version());stabilization.receipt(c,actor,i.requestId(),new StabilizationService.Receipt(fingerprint,ack));events.changed(c,false);return ack;
        }));
    }
}
