package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.CharacterSheet.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;

/** Explicit manual decisions; rolling and recording a verdict never spend resources. */
public final class CombatService {
    public record Declare(UUID requestId,long expectedVersion,UUID characterId,UUID targetId,String attackId,String visibility) {}
    public record ResourceValue(String id,int current) {}
    public record Change(UUID characterId,long expectedVersion,int hp,int tempHp,List<ResourceValue> resources) {}
    public record Command(UUID requestId,long expectedVersion,String kind,String rollId,String verdict,List<Change> changes,String correctionOf,String reason) {}
    public record Applied(CharacterService.Card before,CharacterService.Card after) {}
    public record Event(UUID id,String actor,Instant at,String kind,String verdict,String reason,String correctionOf,DiceService.Roll roll,List<Applied> changes,boolean privateOnly) {}
    public record Action(UUID id,long version,long publicVersion,UUID characterId,String name,long sourceVersion,UUID targetId,String targetName,Attack attack,String visibility,String actor,Instant at,String verdict,List<Event> events) {}
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,Ack result) {}
    public record Listing(List<Object> actions,String next) {}
    public interface Store {
        Action action(UUID c,UUID id);void save(UUID c,Action action,boolean create);
        List<Action> list(UUID c,boolean dm,UUID before,int limit);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store cards;private final DiceService dice;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public CombatService(Store store,CharacterService.Store cards,DiceService dice,DiceService.Events events,Clock clock,JsonMapper json){this.store=store;this.cards=cards;this.dice=dice;this.events=events;this.clock=clock;this.json=json;}
    private Role member(UUID c,String actor){var r=cards.role(c,actor);require(r!=null);return r;}
    private static void require(boolean ok){if(!ok)throw new DomainException("FORBIDDEN",403,"Combat permission required");}
    private boolean publicCard(UUID c,UUID id){var card=cards.card(c,id);return card!=null&&!card.hidden()&&!card.importReview();}
    private CharacterService.Card card(UUID c,UUID id,Role r){var v=cards.card(c,id);if(v==null||r!=Role.DM&&!publicCard(c,id))throw DiceService.missing();return v;}
    private void control(CharacterService.Card v,Role r,String actor){require(r==Role.DM||v.controllers().contains(actor));}
    private Action target(UUID c,UUID id,Role r){var a=store.action(c,id);if(a==null||r!=Role.DM&&(!a.visibility().equals("PUBLIC")||!publicCard(c,a.characterId())||!publicCard(c,a.targetId())))throw DiceService.missing();return a;}
    private Object project(UUID c,Action a,Role r,String actor){
        if(r==Role.DM)return a;
        var out=new LinkedHashMap<String,Object>();out.put("id",a.id());out.put("characterId",a.characterId());out.put("name",a.name());out.put("targetId",a.targetId());out.put("targetName",a.targetName());out.put("attack",a.attack());out.put("actor",a.actor());out.put("at",a.at());out.put("verdict",a.verdict());
        if(cards.card(c,a.characterId()).controllers().contains(actor))out.put("mutationVersion",a.publicVersion());
        var visible=new ArrayList<Object>();
        for(var e:a.events())if(!e.privateOnly()){
            var row=new LinkedHashMap<String,Object>();row.put("id",e.id());row.put("actor",e.actor());row.put("at",e.at());row.put("kind",e.kind());row.put("verdict",e.verdict());row.put("correctionOf",e.correctionOf());
            if(e.roll()!=null){try{row.put("roll",dice.detail(c,e.roll().id(),actor));}catch(DomainException denied){if(denied.status()!=404)throw denied;continue;}}
            visible.add(row);
        }
        out.put("events",visible);return out;
    }
    public Object detail(UUID c,UUID id,String actor){return cards.locked(c,()->{var r=member(c,actor);return project(c,target(c,id,r),r,actor);});}
    public Listing list(UUID c,String actor,UUID before){return cards.locked(c,()->{var r=member(c,actor);if(before!=null)target(c,before,r);var rows=store.list(c,r==Role.DM,before,51);var page=rows.subList(0,Math.min(50,rows.size()));return new Listing(page.stream().map(a->project(c,a,r,actor)).toList(),rows.size()>50?page.getLast().id().toString():"");});}
    public Ack declare(UUID c,String actor,Declare d){
        if(d.characterId()==null||d.targetId()==null||d.expectedVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(d.attackId(),100,true);if(!Set.of("PUBLIC","DM").contains(d.visibility()))throw CharacterSheet.invalid();
        return locked(c,actor,d.requestId(),()->{
            var r=member(c,actor);var source=card(c,d.characterId(),r);card(c,d.targetId(),r);control(source,r,actor);require(r==Role.DM||d.visibility().equals("PUBLIC"));
            if(d.visibility().equals("PUBLIC"))require(publicCard(c,d.characterId())&&publicCard(c,d.targetId()));
            String fp=fp(c,"DECLARE",d);var replay=replay(actor,d.requestId(),fp);if(replay!=null){target(c,replay.id(),r);return replay;}
            AccountPolicy.version(source.version(),d.expectedVersion());var attack=source.sheet().attacks().stream().filter(v->v.id().equals(d.attackId())).findFirst().orElseThrow(CharacterSheet::invalid);
            var a=new Action(UUID.randomUUID(),0,0,source.id(),source.sheet().name(),source.version(),d.targetId(),cards.card(c,d.targetId()).sheet().name(),attack,d.visibility(),actor,clock.instant(),"",List.of());store.save(c,a,true);return finish(c,actor,d.requestId(),fp,new Ack(a.id(),0),d.visibility().equals("DM"));
        });
    }
    public Ack append(UUID c,UUID id,String actor,Command command){
        CharacterSheet.text(command.reason(),1000,false);CharacterSheet.text(command.rollId(),36,false);CharacterSheet.text(command.correctionOf(),36,false);CharacterSheet.text(command.verdict(),10,false);
        if(command.expectedVersion()<0||!Set.of("LINK_ROLL","VERDICT","APPLY","CORRECT").contains(command.kind())||command.changes()==null||command.changes().size()>2)throw CharacterSheet.invalid();
        return locked(c,actor,command.requestId(),()->{
            var r=member(c,actor);var old=target(c,id,r);var source=card(c,old.characterId(),r);control(source,r,actor);
            boolean link=command.kind().equals("LINK_ROLL");if(!link)require(r==Role.DM);
            DiceService.Roll roll=null;
            if(link){
                if(command.rollId().isEmpty()||!command.verdict().isEmpty()||!command.changes().isEmpty()||!command.correctionOf().isEmpty())throw CharacterSheet.invalid();
                roll=dice.detail(c,uuid(command.rollId()),actor);require(r==Role.DM||roll.actor().equals(actor));
                if(!roll.characterId().equals(old.characterId().toString())||!Set.of("attack:"+old.attack().id(),"damage:"+old.attack().id()).contains(roll.key()))throw CharacterSheet.invalid();
            }else if(!command.rollId().isEmpty())throw CharacterSheet.invalid();
            for(var change:command.changes()){if(change==null||!List.of(old.characterId(),old.targetId()).contains(change.characterId()))throw CharacterSheet.invalid();card(c,change.characterId(),r);}
            String fingerprint=fp(c,id,command);var prior=replay(actor,command.requestId(),fingerprint);if(prior!=null)return prior;
            AccountPolicy.version(r==Role.DM?old.version():old.publicVersion(),command.expectedVersion());
            boolean privateOnly=old.visibility().equals("DM")||(roll!=null&&roll.visibility().equals("DM"));
            if(link){var linked=roll; if(old.events().stream().anyMatch(e->e.roll()!=null&&e.roll().id().equals(linked.id())))throw new DomainException("VERSION_CONFLICT",409,"Roll already linked");}
            else{
                CharacterSheet.text(command.reason(),1000,true);
                if(command.kind().equals("CORRECT")){
                    var original=old.events().stream().filter(e->e.id().toString().equals(command.correctionOf())).findFirst().orElseThrow(DiceService::missing);
                    if(command.verdict().isEmpty()&&command.changes().isEmpty())throw CharacterSheet.invalid();
                    privateOnly|=original.privateOnly();
                }else if(!command.correctionOf().isEmpty())throw CharacterSheet.invalid();
                if(command.kind().equals("VERDICT")&&(command.verdict().isEmpty()||!command.changes().isEmpty()))throw CharacterSheet.invalid();
                if(command.kind().equals("APPLY")){
                    if(old.verdict().isEmpty()||!command.verdict().isEmpty()||command.changes().isEmpty())throw CharacterSheet.invalid();
                    if(old.events().stream().anyMatch(e->e.kind().equals("APPLY")))throw new DomainException("VERSION_CONFLICT",409,"Already applied; append a correction");
                }
                if(!command.verdict().isEmpty()&&!Set.of("HIT","MISS","OTHER").contains(command.verdict()))throw CharacterSheet.invalid();
            }
            var applied=apply(c,actor,command);
            var event=new Event(UUID.randomUUID(),actor,clock.instant(),command.kind(),command.verdict(),command.reason(),command.correctionOf(),roll,applied,privateOnly);
            var journal=new ArrayList<>(old.events());journal.add(event);
            // A private correction may not rewrite a public verdict implicitly.
            String verdict=command.verdict().isEmpty()||privateOnly&&old.visibility().equals("PUBLIC")?old.verdict():command.verdict();
            var next=new Action(old.id(),old.version()+1,old.publicVersion()+(privateOnly?0:1),old.characterId(),old.name(),old.sourceVersion(),old.targetId(),old.targetName(),old.attack(),old.visibility(),old.actor(),old.at(),verdict,List.copyOf(journal));
            store.save(c,next,false);return finish(c,actor,command.requestId(),fingerprint,new Ack(id,r==Role.DM?next.version():next.publicVersion()),privateOnly);
        });
    }
    private List<Applied> apply(UUID c,String actor,Command command){
        var changes=reviewChanges(c,command.changes());
        for(var change:changes){cards.save(change.after(),false);cards.history(new CharacterService.History(change.after().version(),actor,"COMBAT_"+command.kind(),command.reason(),clock.instant(),change.before(),change.after()));}
        return changes;
    }
    List<Applied> reviewChanges(UUID c,List<Change> requested){
        var seen=new HashSet<UUID>();var changes=new ArrayList<Applied>();
        for(var v:requested){
            if(!seen.add(v.characterId())||v.expectedVersion()<0||v.resources()==null||v.resources().size()>100)throw CharacterSheet.invalid();
            var before=cards.card(c,v.characterId());AccountPolicy.version(before.version(),v.expectedVersion());var s=before.sheet();var b=s.combat();
            var combat=new Combat(b.maxHp(),v.hp(),v.tempHp(),b.speed(),b.deathSuccesses(),b.deathFailures(),b.acMode(),b.manualAc(),b.source());
            var values=new HashMap<String,Integer>();for(var value:v.resources()){if(value==null||value.id()==null||values.putIfAbsent(value.id(),value.current())!=null||s.resources().stream().noneMatch(x->x.id().equals(value.id())))throw CharacterSheet.invalid();}
            var resources=s.resources().stream().map(x->values.containsKey(x.id())?new Resource(x.id(),x.name(),x.kind(),values.get(x.id()),x.max(),x.recovery(),x.source()):x).toList();
            var sheet=s.change(s.name(),combat,resources,s.conditions(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(before.id(),c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);changes.add(new Applied(before,after));
        }
        return List.copyOf(changes);
    }
    private <T>T locked(UUID c,String actor,UUID request,Supplier<T> work){if(request==null)throw CharacterSheet.invalid();return cards.actorLocked(actor,()->cards.locked(c,work));}
    private String fp(Object... fields){return DiceService.fingerprint(json,Arrays.asList(fields));}
    private Ack replay(String actor,UUID request,String fp){var r=store.receipt(actor,request);if(r==null)return null;if(!r.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return r.result();}
    private Ack finish(UUID c,String actor,UUID request,String fp,Ack ack,boolean privateOnly){store.receipt(c,actor,request,new Receipt(fp,ack));events.changed(c,privateOnly);return ack;}
    private static UUID uuid(String text){try{return UUID.fromString(text);}catch(IllegalArgumentException e){throw CharacterSheet.invalid();}}
}
