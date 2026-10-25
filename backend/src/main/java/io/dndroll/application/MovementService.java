package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.*;
import io.dndroll.domain.MovementPath.Step;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Declared movement evidence and advisory character-owned budgets. */
public final class MovementService {
    public record Budget(String version,int used,Integer allowance,boolean complete,String anchor) {}
    public record Path(UUID requestId,long expectedVersion,long sceneVersion,long characterVersion,String budgetVersion,long turnKey,boolean reviewed,List<Step> path,boolean override,int cost,String reason) {}
    public record Adjustment(UUID requestId,long expectedVersion,long sceneVersion,long characterVersion,String budgetVersion,long turnKey,int used,int allowance,String reason) {}
    public record View(TokenView token,long sceneVersion,UUID characterId,long characterVersion,String budgetVersion,long turnKey,boolean tracked,int allowance,int used,boolean complete,List<Journal> history) {}
    public record Preview(int cost,int usedAfter,int remaining,boolean overBudget,boolean complete,boolean tracked) {}
    public record Result(TokenView token,Preview calculation) {}
    public record Journal(UUID id,String kind,String actor,Instant at,UUID tokenId,Object command,View before,Preview calculation,CharacterSheet.Source source) {}
    public record Receipt(String fingerprint,Result result) {}
    public interface Store {
        Budget budget(UUID c,UUID character);void budget(UUID c,UUID character,Budget budget);
        String anchor(UUID c,UUID character);boolean fiveFoot(UUID scene);
        boolean controls(UUID c,UUID token,String actor);
        List<Journal> history(UUID c,UUID character);void journal(UUID c,UUID character,Journal journal);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store cards;private final TurnService.Store turns;private final SceneService scenes;private final SceneService.MovementPermission permission;private final SceneService.Events events;private final Clock clock;private final JsonMapper json;
    public MovementService(Store store,CharacterService.Store cards,TurnService.Store turns,SceneService scenes,SceneService.MovementPermission permission,SceneService.Events events,Clock clock,JsonMapper json){this.store=store;this.cards=cards;this.turns=turns;this.scenes=scenes;this.permission=permission;this.events=events;this.clock=clock;this.json=json;}
    private static void require(boolean ok){if(!ok)throw new DomainException("FORBIDDEN",403,"Movement permission required");}
    private View current(UUID c,UUID token,String actor,boolean moving){
        var role=cards.role(c,actor);require(role!=null);
        var scene=scenes.snapshot(c,actor);var t=scene.tokens().stream().filter(v->v.id().equals(token)).findFirst().orElseThrow(DiceService::missing);
        // Read access does not depend on the current turn, but does require both controls.
        var id=turns.characterForToken(c,token);var card=id==null?null:cards.card(c,id);
        if(card==null||role!=Role.DM&&(card.hidden()||card.importReview()))throw DiceService.missing();
        // snapshot canMove includes turn gating, so inspect token controllers separately.
        require(role==Role.DM||store.controls(c,token,actor)&&card.controllers().contains(actor));
        if(moving)require(permission.canMove(c,token,actor,role));
        var combat=turns.state(c);boolean tracked=combat.status().equals("ACTIVE")&&combat.order().contains(id);
        var b=store.budget(c,id);int speed=card.sheet().combat().speed();String version=b==null?id.toString():b.version();
        return new View(t,scene.version(),id,card.version(),version,tracked?combat.endVersion():0,tracked,tracked&&b!=null&&b.allowance()!=null?b.allowance():speed,tracked&&b!=null?b.used():0,!tracked||b!=null&&b.complete()&&b.anchor().equals(store.anchor(c,id)),role==Role.DM?store.history(c,id):List.of());
    }
    public Object view(UUID c,UUID token,String actor){return cards.locked(c,()->{
        var role=cards.role(c,actor);require(role!=null);var scene=scenes.snapshot(c,actor);
        scene.tokens().stream().filter(t->t.id().equals(token)).findFirst().orElseThrow(DiceService::missing);
        require(role==Role.DM||store.controls(c,token,actor));
        if(turns.characterForToken(c,token)==null)return Map.of("available",false);
        return current(c,token,actor,false);
    });}
    private void versions(View v,long token,long scene,long character,String budget,long turn){AccountPolicy.version(v.token().version(),token);AccountPolicy.version(v.sceneVersion(),scene);AccountPolicy.version(v.characterVersion(),character);if(!v.budgetVersion().equals(budget)||v.turnKey()!=turn)throw new DomainException("VERSION_CONFLICT",409,"Movement context changed; reselect current state");}
    private Preview calculate(UUID c,View v,Path d,String actor){
        if(!d.reviewed())throw new DomainException("VERSION_CONFLICT",409,"Review the 5-foot grid and applicable movement context first");
        if(!d.override()&&!store.fiveFoot(scenes.snapshot(c,actor).id()))throw new DomainException("VERSION_CONFLICT",409,"This map requires a DM manual distance decision");
        CharacterSheet.text(d.reason(),1000,d.override());if(!d.override()&&!d.reason().isEmpty())throw CharacterSheet.invalid();if(d.override())require(cards.role(c,actor)==Role.DM);
        var scene=scenes.snapshot(c,actor);int cost=MovementPath.cost(v.token().x(),v.token().y(),scene.width(),scene.height(),d.path(),d.override(),d.cost());int used=v.tracked()?Math.addExact(v.used(),cost):cost;if(used>100000)throw CharacterSheet.invalid();
        return new Preview(cost,used,v.allowance()-used,v.tracked()&&used>v.allowance(),v.complete(),v.tracked());
    }
    private static void context(long token,long scene,long character,String budget,long turn){
        for(long v:new long[]{token,scene,character,turn})if(v<0||v>9007199254740991L)throw CharacterSheet.invalid();
        try{if(budget==null||!UUID.fromString(budget).toString().equals(budget))throw CharacterSheet.invalid();}catch(IllegalArgumentException invalid){throw CharacterSheet.invalid();}
    }
    private static void pathInput(Path d){if(d.requestId()==null||d.path()==null||d.reason()==null)throw CharacterSheet.invalid();context(d.expectedVersion(),d.sceneVersion(),d.characterVersion(),d.budgetVersion(),d.turnKey());}
    public Preview preview(UUID c,UUID token,String actor,Path d){pathInput(d);return cards.locked(c,()->{var v=current(c,token,actor,true);versions(v,d.expectedVersion(),d.sceneVersion(),d.characterVersion(),d.budgetVersion(),d.turnKey());return calculate(c,v,d,actor);});}
    public Result move(UUID c,UUID token,String actor,Path d){
        pathInput(d);return cards.actorLocked(actor,()->cards.locked(c,()->{
            var v=current(c,token,actor,true);if(d.override())require(cards.role(c,actor)==Role.DM);String fp=DiceService.fingerprint(json,List.of(c,token,"PATH",d));var replay=replay(actor,d.requestId(),fp);if(replay!=null)return replay;
            versions(v,d.expectedVersion(),d.sceneVersion(),d.characterVersion(),d.budgetVersion(),d.turnKey());var p=calculate(c,v,d,actor);var last=d.path().getLast();
            // Separate receipt namespace also prevents collision with a legacy move key.
            var move=new Command(UUID.randomUUID(),d.expectedVersion(),last.x(),last.y());
            var result=scenes.moveRecorded(c,token,actor,move,()->{
                if(v.tracked()){var old=store.budget(c,v.characterId());store.budget(c,v.characterId(),new Budget(UUID.randomUUID().toString(),p.usedAfter(),old==null?null:old.allowance(),p.complete(),store.anchor(c,v.characterId())));}
                journal(c,actor,token,"PATH",d,v,p);
            });
            var response=new Result(result,p);store.receipt(c,actor,d.requestId(),new Receipt(fp,response));return response;
        }));
    }
    public Result adjust(UUID c,UUID token,String actor,Adjustment d){
        if(d.requestId()==null||d.used()<0||d.used()>100000||d.allowance()<0||d.allowance()>100000)throw CharacterSheet.invalid();CharacterSheet.text(d.reason(),1000,true);context(d.expectedVersion(),d.sceneVersion(),d.characterVersion(),d.budgetVersion(),d.turnKey());
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            var v=current(c,token,actor,false);require(cards.role(c,actor)==Role.DM);String fp=DiceService.fingerprint(json,List.of(c,token,"ADJUST",d));var replay=replay(actor,d.requestId(),fp);if(replay!=null)return replay;
            versions(v,d.expectedVersion(),d.sceneVersion(),d.characterVersion(),d.budgetVersion(),d.turnKey());if(!v.tracked())throw new DomainException("VERSION_CONFLICT",409,"No active character movement budget");
            var p=new Preview(0,d.used(),d.allowance()-d.used(),d.used()>d.allowance(),true,true);store.budget(c,v.characterId(),new Budget(UUID.randomUUID().toString(),d.used(),d.allowance(),true,store.anchor(c,v.characterId())));journal(c,actor,token,"ADJUST",d,v,p);
            var result=new Result(v.token(),p);store.receipt(c,actor,d.requestId(),new Receipt(fp,result));events.changed(c);return result;
        }));
    }
    private Result replay(String actor,UUID request,String fp){var r=store.receipt(actor,request);if(r==null)return null;if(!r.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return r.result();}
    private void journal(UUID c,String actor,UUID token,String kind,Object command,View v,Preview p){var card=cards.card(c,v.characterId());var before=new View(v.token(),v.sceneVersion(),v.characterId(),v.characterVersion(),v.budgetVersion(),v.turnKey(),v.tracked(),v.allowance(),v.used(),v.complete(),List.of());store.journal(c,v.characterId(),new Journal(UUID.randomUUID(),kind,actor,clock.instant(),token,command,before,p,card.sheet().combat().source()));}
}
