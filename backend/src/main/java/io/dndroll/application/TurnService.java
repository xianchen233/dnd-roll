package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Character turns; multiple token placements never create additional resource owners. */
public final class TurnService implements SceneService.MovementPermission {
    public record Command(UUID requestId,long expectedVersion,String kind,String characterId,List<UUID> order,String reason) {}
    public record State(long version,String status,int round,List<UUID> order,String currentId,List<UUID> grants,long endVersion,String reason) {}
    public record History(String actor,String kind,Instant at,State before,State after) {}
    public interface Store {
        State state(UUID c);void state(UUID c,State state);void history(UUID c,History h);List<History> history(UUID c);
        UUID characterForToken(UUID c,UUID token);
    }
    private final Store store;private final CharacterService.Store cards;private final InitiativeService.Store initiative;private final CombatService.Store receipts;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public TurnService(Store store,CharacterService.Store cards,InitiativeService.Store initiative,CombatService.Store receipts,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.cards=cards;this.initiative=initiative;this.receipts=receipts;this.events=events;this.clock=clock;this.json=json;}
    private static void require(boolean ok){if(!ok)throw new DomainException("FORBIDDEN",403,"Turn permission required");}
    private Role member(UUID c,String actor){var r=cards.role(c,actor);require(r!=null);return r;}
    private boolean visible(UUID c,UUID id){var card=cards.card(c,id);return card!=null&&!card.hidden()&&!card.importReview();}
    private Map<String,Object> publicBase(UUID c,State s){
        var rows=s.order().stream().filter(id->visible(c,id)).map(id->Map.of("characterId",id,"name",cards.card(c,id).sheet().name())).toList();
        boolean shown=!s.currentId().isEmpty()&&visible(c,UUID.fromString(s.currentId()));
        var result=new LinkedHashMap<String,Object>();result.put("status",rows.isEmpty()?"IDLE":s.status());result.put("order",rows);result.put("currentId",shown?s.currentId():"");return result;
    }
    private CharacterService.Card card(UUID c,String id,Role r){UUID uuid;try{uuid=UUID.fromString(id);}catch(IllegalArgumentException e){throw CharacterSheet.invalid();}var v=cards.card(c,uuid);if(v==null||r!=Role.DM&&!visible(c,uuid))throw DiceService.missing();return v;}
    public Object view(UUID c,String actor){return cards.locked(c,()->{var r=member(c,actor);var s=store.state(c);if(r==Role.DM)return s;
        var result=publicBase(c,s);boolean shown=!result.get("currentId").equals("");
        if(shown&&cards.card(c,UUID.fromString(s.currentId())).controllers().contains(actor))result.put("endVersion",s.endVersion());return result;
    });}
    public List<History> history(UUID c,String actor){return cards.locked(c,()->{require(member(c,actor)==Role.DM);return store.history(c);});}
    public CombatService.Ack command(UUID c,String actor,Command d){
        if(d.requestId()==null||d.expectedVersion()<0||d.order()==null||d.order().size()>100||d.order().stream().anyMatch(Objects::isNull)||new HashSet<>(d.order()).size()!=d.order().size()||!Set.of("START","END_TURN","SKIP","SWITCH","ORDER","GRANT","STOP").contains(d.kind()))throw CharacterSheet.invalid();
        CharacterSheet.text(d.characterId(),36,false);CharacterSheet.text(d.reason(),1000,true);
        if(!d.kind().equals("ORDER")&&!d.order().isEmpty())throw CharacterSheet.invalid();
        if(!Set.of("END_TURN","SWITCH","GRANT").contains(d.kind())&&!d.characterId().isEmpty())throw CharacterSheet.invalid();
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            var r=member(c,actor);CharacterService.Card source=null;
            if(d.kind().equals("END_TURN")){source=card(c,d.characterId(),r);require(r==Role.DM||source.controllers().contains(actor));}else require(r==Role.DM);
            if(Set.of("SWITCH","GRANT").contains(d.kind()))card(c,d.characterId(),r);
            String fp=DiceService.fingerprint(json,List.of("TURN",c,d));var receipt=receipts.receipt(actor,d.requestId());
            if(receipt!=null){if(!receipt.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return receipt.result();}
            var old=store.state(c);if(d.kind().equals("END_TURN"))require(old.currentId().equals(source.id().toString()));AccountPolicy.version(r==Role.DM?old.version():old.endVersion(),d.expectedVersion());var order=old.order();String current=old.currentId();int round=old.round();String status=old.status();List<UUID> grants=List.of();
            if(d.kind().equals("START")){
                var prepared=startState(c,old,r);
                order=prepared.entries().stream().map(InitiativeService.Entry::characterId).toList();current=order.getFirst().toString();round=1;status="ACTIVE";
            }else{
                if(!status.equals("ACTIVE"))throw new DomainException("VERSION_CONFLICT",409,"Combat is not active");
                switch(d.kind()){
                    case "END_TURN","SKIP" -> {if(d.kind().equals("END_TURN"))require(current.equals(source.id().toString()));int index=order.indexOf(UUID.fromString(current))+1;if(index==order.size()){index=0;if(round==Integer.MAX_VALUE)throw CharacterSheet.invalid();round++;}current=order.get(index).toString();}
                    case "SWITCH" -> {if(!order.contains(UUID.fromString(d.characterId())))throw CharacterSheet.invalid();current=d.characterId();}
                    case "ORDER" -> {if(!new HashSet<>(order).equals(new HashSet<>(d.order())))throw CharacterSheet.invalid();order=List.copyOf(d.order());}
                    case "GRANT" -> {UUID id=UUID.fromString(d.characterId());if(!order.contains(id))throw CharacterSheet.invalid();var all=new LinkedHashSet<>(old.grants());all.add(id);grants=List.copyOf(all);}
                    case "STOP" -> {status="IDLE";current="";order=List.of();round=0;}
                    default -> throw CharacterSheet.invalid();
                }
            }
            // A random compare token permits player commands without disclosing the
            // global count of private DM operations. Grants retain the current token.
            var candidate=new State(old.version()+1,status,round,order,current,grants,old.endVersion(),d.reason());
            boolean visibleChange=!publicBase(c,old).equals(publicBase(c,candidate));
            long endVersion=status.equals("ACTIVE")?(visibleChange||old.endVersion()==0?java.util.concurrent.ThreadLocalRandom.current().nextLong(1,9007199254740991L):old.endVersion()):0;
            var next=new State(candidate.version(),status,round,order,current,grants,endVersion,d.reason());store.state(c,next);store.history(c,new History(actor,d.kind(),clock.instant(),old,next));var ack=new CombatService.Ack(c,r==Role.DM?next.version():next.endVersion());receipts.receipt(c,actor,d.requestId(),new CombatService.Receipt(fp,ack));
            boolean publicGrant=d.kind().equals("GRANT")&&visible(c,UUID.fromString(d.characterId()))&&cards.card(c,UUID.fromString(d.characterId())).controllers().stream().anyMatch(user->cards.role(c,user)==Role.PLAYER);
            events.changed(c,!visibleChange&&!publicGrant);return ack;
        }));
    }
    private InitiativeService.State startState(UUID c,State old,Role role){
        if(old.status().equals("ACTIVE"))throw new DomainException("VERSION_CONFLICT",409,"Combat already active");
        var prepared=initiative.state(c);if(!prepared.status().equals("CONFIRMED")||prepared.entries().isEmpty())throw CharacterSheet.invalid();
        for(var entry:prepared.entries())card(c,entry.characterId().toString(),role);
        return prepared;
    }
    /** Reuses START's preconditions without changing combat or histories. */
    public State reviewStart(UUID c,String actor,long expectedVersion){
        return cards.locked(c,()->{var role=member(c,actor);require(role==Role.DM);var old=store.state(c);AccountPolicy.version(old.version(),expectedVersion);startState(c,old,role);return old;});
    }
    /** Called under SceneService's campaign lock, before returning any old move receipt. */
    public boolean canMove(UUID c,UUID token,String actor,Role role){
        if(role==Role.DM)return true;var state=store.state(c);if(!state.status().equals("ACTIVE"))return true;
        if(state.order().stream().noneMatch(id->visible(c,id)))return true;
        UUID id=store.characterForToken(c,token);if(id==null||!visible(c,id))return false;var card=cards.card(c,id);
        return card.controllers().contains(actor)&&(id.toString().equals(state.currentId())||state.grants().contains(id));
    }
}
