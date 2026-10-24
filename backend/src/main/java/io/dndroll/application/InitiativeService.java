package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.CharacterSheet.Calculation;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;

/** Initiative preparation only. P06 owns rounds, turns and resource application. */
public final class InitiativeService {
    public record Participant(UUID characterId,long expectedVersion,String kind,String group,boolean surprised,List<String> advantage,List<String> disadvantage) {}
    public record Generate(UUID requestId,long expectedVersion,List<Participant> participants,String reason) {}
    public record Confirm(UUID requestId,long expectedVersion,List<UUID> order,String reason) {}
    public record Entry(UUID characterId,String name,long sourceVersion,String kind,String group,boolean surprised,Calculation calculation,List<String> advantage,List<String> disadvantage,Dice.Result result) {}
    public record State(long version,String status,List<Entry> entries,String reason) {}
    public record PublicEntry(UUID characterId,String name,int total) {}
    public record PublicState(String status,List<PublicEntry> entries) {}
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,Ack result) {}
    public record History(long version,String actor,String kind,String reason,Instant at,State snapshot) {}
    public interface Store {
        State state(UUID c);void state(UUID c,State state);
        List<History> history(UUID c);void history(UUID c,History history);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store cards;private final Dice.Entropy entropy;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public InitiativeService(Store store,CharacterService.Store cards,Dice.Entropy entropy,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.cards=cards;this.entropy=entropy;this.events=events;this.clock=clock;this.json=json;}
    private Role member(UUID c,String actor){var r=cards.role(c,actor);DiceService.require(r!=null);return r;}
    private boolean visible(UUID c,Entry entry){var card=cards.card(c,entry.characterId());return card!=null&&!card.hidden()&&!card.importReview();}
    public Object view(UUID c,String actor){return cards.locked(c,()->{var role=member(c,actor);var state=store.state(c);if(role==Role.DM)return state;
        var rows=state.status().equals("CONFIRMED")?state.entries().stream().filter(e->visible(c,e)).map(e->new PublicEntry(e.characterId(),e.name(),e.result().total())).toList():List.<PublicEntry>of();
        return new PublicState(rows.isEmpty()?"EMPTY":"CONFIRMED",rows);
    });}
    public List<History> history(UUID c,String actor){return cards.locked(c,()->{DiceService.require(member(c,actor)==Role.DM);return store.history(c);});}
    public Ack generate(UUID c,String actor,Generate g){
        validateGenerate(g);
        return execute(c,actor,g.requestId(),g.expectedVersion(),List.of("GENERATE",g.participants(),g.reason()),()->{for(var p:g.participants())if(cards.card(c,p.characterId())==null)throw DiceService.missing();},old->{
            var candidates=candidates(c,g);
            var results=new HashMap<String,Dice.Result>();var rolled=new ArrayList<Entry>();
            for(var e:candidates){var result=e.group().isEmpty()?draw(e):results.computeIfAbsent(e.group(),ignored->draw(e));rolled.add(new Entry(e.characterId(),e.name(),e.sourceVersion(),e.kind(),e.group(),e.surprised(),e.calculation(),e.advantage(),e.disadvantage(),result));}
            rolled.sort(Comparator.comparingInt((Entry e)->e.result().total()).reversed());
            return new State(old.version()+1,"DRAFT",List.copyOf(rolled),g.reason());
        },"GENERATE",g.reason());
    }
    public static void validateGenerate(Generate g){
        CharacterSheet.text(g.reason(),1000,true);if(g.participants()==null||g.participants().isEmpty()||g.participants().size()>100||g.participants().stream().anyMatch(Objects::isNull))throw CharacterSheet.invalid();
        if(new HashSet<>(g.participants().stream().map(Participant::characterId).toList()).size()!=g.participants().size())throw CharacterSheet.invalid();
        for(var p:g.participants()){if(p.characterId()==null||p.expectedVersion()<0||!Set.of("PC","MONSTER").contains(p.kind()))throw CharacterSheet.invalid();CharacterSheet.text(p.group(),100,false);Dice.sources(p.advantage());Dice.sources(p.disadvantage());if(p.kind().equals("PC")&&!p.group().isEmpty())throw CharacterSheet.invalid();}
    }
    private List<Entry> candidates(UUID c,Generate g){
        var candidates=new ArrayList<Entry>();var groups=new HashMap<String,Entry>();
        // Validate the complete roster/group/source versions before consuming entropy.
        for(var p:g.participants()){
            var card=cards.card(c,p.characterId());AccountPolicy.version(card.version(),p.expectedVersion());var calculation=card.sheet().calculate().get("initiative");
            var dis=new ArrayList<>(p.disadvantage());if(p.surprised())dis.add("5R 突袭：先攻劣势（SRD 5.2.1 p13）");
            if(dis.size()>10)throw CharacterSheet.invalid();Dice.validate("d20"+signed(calculation.value()),p.advantage(),dis);
            var entry=new Entry(card.id(),card.sheet().name(),card.version(),p.kind(),p.group(),p.surprised(),calculation,List.copyOf(p.advantage()),List.copyOf(dis),null);
            if(!p.group().isEmpty()){
                var previous=groups.putIfAbsent(p.group(),entry);
                if(previous!=null&&(!previous.calculation().equals(calculation)||previous.surprised()!=p.surprised()||!previous.advantage().equals(entry.advantage())||!previous.disadvantage().equals(entry.disadvantage())))throw CharacterSheet.invalid();
            }
            candidates.add(entry);
        }
        return List.copyOf(candidates);
    }
    /** Same validation as generation, without entropy or writes. */
    public List<Entry> reviewGenerate(UUID c,String actor,Generate g){
        return cards.locked(c,()->{
            DiceService.require(member(c,actor)==Role.DM);validateGenerate(g);
            if(g.requestId()==null||g.expectedVersion()<0)throw CharacterSheet.invalid();
            AccountPolicy.version(store.state(c).version(),g.expectedVersion());
            for(var p:g.participants())if(cards.card(c,p.characterId())==null)throw DiceService.missing();
            return candidates(c,g);
        });
    }
    private static String signed(int value){return value<0?Integer.toString(value):"+"+value;}
    private Dice.Result draw(Entry entry){return Dice.roll("d20"+signed(entry.calculation().value()),entry.advantage(),entry.disadvantage(),entropy);}
    public Ack confirm(UUID c,String actor,Confirm command){
        CharacterSheet.text(command.reason(),1000,true);if(command.order()==null||command.order().isEmpty()||command.order().size()>100||command.order().contains(null)||new HashSet<>(command.order()).size()!=command.order().size())throw CharacterSheet.invalid();
        return execute(c,actor,command.requestId(),command.expectedVersion(),List.of("CONFIRM",command.order(),command.reason()),()->{},old->{
            if(!old.status().equals("DRAFT"))throw new DomainException("VERSION_CONFLICT",409,"Generate candidates first");
            var byId=new HashMap<UUID,Entry>();old.entries().forEach(e->byId.put(e.characterId(),e));if(!byId.keySet().equals(new HashSet<>(command.order())))throw CharacterSheet.invalid();
            return new State(old.version()+1,"CONFIRMED",command.order().stream().map(byId::get).toList(),command.reason());
        },"CONFIRM",command.reason());
    }
    private Ack execute(UUID c,String actor,UUID request,long expected,Object payload,Runnable authorize,java.util.function.Function<State,State> work,String kind,String reason){
        if(request==null||expected<0)throw CharacterSheet.invalid();return cards.actorLocked(actor,()->cards.locked(c,()->{
            DiceService.require(member(c,actor)==Role.DM);authorize.run();String fp=DiceService.fingerprint(json,List.of(c,expected,payload));var receipt=store.receipt(actor,request);
            if(receipt!=null){if(!receipt.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return receipt.result();}
            var old=store.state(c);AccountPolicy.version(old.version(),expected);var next=work.apply(old);store.state(c,next);store.history(c,new History(next.version(),actor,kind,reason,clock.instant(),next));var ack=new Ack(c,next.version());store.receipt(c,actor,request,new Receipt(fp,ack));
            boolean publicChange=old.status().equals("CONFIRMED")&&old.entries().stream().anyMatch(e->visible(c,e))||next.status().equals("CONFIRMED")&&next.entries().stream().anyMatch(e->visible(c,e));events.changed(c,!publicChange);return ack;
        }));
    }
}
