package io.dndroll.modules;

import com.fasterxml.jackson.annotation.*;
import io.dndroll.application.*;
import io.dndroll.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Explicit DM combinations orchestrate existing services in the campaign transaction. */
public final class ModuleOperations {
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME,property="kind")
    @JsonSubTypes({@JsonSubTypes.Type(value=RevealToken.class,name="REVEAL_TOKEN"),@JsonSubTypes.Type(value=RevealCharacter.class,name="REVEAL_CHARACTER"),@JsonSubTypes.Type(value=PublishText.class,name="PUBLISH_TEXT"),@JsonSubTypes.Type(value=PrepareEncounter.class,name="PREPARE_ENCOUNTER"),@JsonSubTypes.Type(value=StartEncounter.class,name="START_ENCOUNTER")})
    public sealed interface Step permits RevealToken,RevealCharacter,PublishText,PrepareEncounter,StartEncounter {}
    public record RevealToken(UUID sceneId,UUID tokenId,long expectedVersion) implements Step {}
    public record RevealCharacter(UUID characterId,long expectedVersion) implements Step {}
    public record PublishText(UUID noteId,long expectedVersion) implements Step {}
    public record PrepareEncounter(long expectedVersion,List<InitiativeService.Participant> participants,String reason) implements Step {}
    public record StartEncounter(long expectedVersion,long initiativeVersion,String reason) implements Step {}
    public record Definition(UUID id,long version,String name,List<Step> steps) {}
    public record Save(UUID requestId,long expectedVersion,String name,List<Step> steps) {}
    private record SaveWire(UUID requestId,long expectedVersion,String name,List<JsonNode> steps) {}
    public record Review(long expectedVersion,long campaignVersion) {}
    public record Execute(UUID requestId,long expectedVersion,long campaignVersion,String impactHash) {}
    public record Impact(UUID id,long version,long campaignVersion,String impactHash,Definition definition,List<JsonNode> targets) {}
    public record Execution(UUID id,UUID requestId,String actor,Instant at,Impact impact,List<JsonNode> results) {}
    public record Receipt(String fingerprint,JsonNode result) {}
    public interface Store {
        List<Definition> list(UUID c);Definition definition(UUID c,UUID id);void save(UUID c,Definition value,boolean create);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt value);
        List<Execution> executions(UUID c,UUID id);void execution(UUID c,UUID id,Execution value);
    }
    private final Store store;private final CharacterService.Store campaigns;private final ModuleLifecycle lifecycle;
    private final TokenService tokens;private final CharacterService characters;private final NotesService notes;
    private final InitiativeService initiative;private final TurnService turns;private final ContentService content;
    private final JsonMapper json;private final Clock clock;
    public ModuleOperations(Store store,CharacterService.Store campaigns,ModuleLifecycle lifecycle,TokenService tokens,CharacterService characters,NotesService notes,InitiativeService initiative,TurnService turns,ContentService content,JsonMapper json,Clock clock){this.store=store;this.campaigns=campaigns;this.lifecycle=lifecycle;this.tokens=tokens;this.characters=characters;this.notes=notes;this.initiative=initiative;this.turns=turns;this.content=content;this.json=json;this.clock=clock;}
    public static Save parseSave(JsonNode n){
        if(n==null||n.toString().getBytes(StandardCharsets.UTF_8).length>65536)throw ModuleBlueprint.invalid();
        var wire=ModuleBlueprint.command(n,SaveWire.class);var steps=new ArrayList<Step>();
        for(var value:wire.steps()){
            if(!value.isObject()||!value.has("kind")||!value.get("kind").isString())throw ModuleBlueprint.invalid();
            var body=value.deepCopy();((tools.jackson.databind.node.ObjectNode)body).remove("kind");
            Class<?> type=switch(value.get("kind").asString()){
                case "REVEAL_TOKEN" -> RevealToken.class;
                case "REVEAL_CHARACTER" -> RevealCharacter.class;
                case "PUBLISH_TEXT" -> PublishText.class;
                case "PREPARE_ENCOUNTER" -> PrepareEncounter.class;
                case "START_ENCOUNTER" -> StartEncounter.class;
                default -> throw ModuleBlueprint.invalid();
            };
            ModuleBlueprint.shape(body,type);
            try{steps.add(JsonMapper.builder().build().treeToValue(value,Step.class));}catch(RuntimeException e){throw ModuleBlueprint.invalid();}
        }
        var result=new Save(wire.requestId(),wire.expectedVersion(),wire.name(),List.copyOf(steps));validate(result);return result;
    }
    private static void validate(Save r){
        if(r.requestId()==null||r.expectedVersion()<0||r.steps()==null||r.steps().isEmpty()||r.steps().size()>20)throw ModuleBlueprint.invalid();CharacterSheet.text(r.name(),100,true);
        var seen=new HashSet<String>();
        for(var step:r.steps()){
            String key;long version;
            switch(step){
                case RevealToken s -> {if(s.sceneId()==null||s.tokenId()==null)throw ModuleBlueprint.invalid();key="token:"+s.tokenId();version=s.expectedVersion();}
                case RevealCharacter s -> {if(s.characterId()==null)throw ModuleBlueprint.invalid();key="card:"+s.characterId();version=s.expectedVersion();}
                case PublishText s -> {if(s.noteId()==null)throw ModuleBlueprint.invalid();key="note:"+s.noteId();version=s.expectedVersion();}
                case PrepareEncounter s -> {InitiativeService.validateGenerate(new InitiativeService.Generate(r.requestId(),s.expectedVersion(),s.participants(),s.reason()));key="encounter";version=s.expectedVersion();}
                case StartEncounter s -> {if(s.initiativeVersion()<0)throw ModuleBlueprint.invalid();CharacterSheet.text(s.reason(),1000,true);key="encounter";version=s.expectedVersion();}
            }
            if(version<0||!seen.add(key))throw ModuleBlueprint.invalid();
        }
    }
    private <T>T read(UUID c,String actor,Supplier<T> work){return campaigns.locked(c,()->{lifecycle.readInstance(c,actor);return work.get();});}
    private <T>T command(UUID c,String actor,Supplier<T> work){return campaigns.actorLocked(actor,()->read(c,actor,work));}
    private Definition target(UUID c,UUID id){var v=store.definition(c,id);if(v==null)throw missing();return v;}
    private String fp(Object... fields){return DiceService.fingerprint(json,Arrays.asList(fields));}
    private Receipt replay(String actor,UUID request,String hash){var old=store.receipt(actor,request);if(old!=null&&!old.fingerprint().equals(hash))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old;}
    public List<Definition> list(UUID c,String actor){return read(c,actor,()->store.list(c));}
    public Definition save(UUID c,UUID id,String actor,Save r){
        validate(r);return command(c,actor,()->{
            String hash=fp("SAVE",c,id,r);var old=replay(actor,r.requestId(),hash);if(old!=null)return json.treeToValue(old.result(),Definition.class);
            var before=store.definition(c,id);AccountPolicy.version(before==null?0:before.version(),r.expectedVersion());
            if(before==null&&store.list(c).size()>=100)throw ModuleBlueprint.invalid();
            var value=new Definition(id,r.expectedVersion()+1,r.name(),List.copyOf(r.steps()));store.save(c,value,before==null);
            store.receipt(c,actor,r.requestId(),new Receipt(hash,json.valueToTree(value)));return value;
        });
    }
    public Impact impact(UUID c,UUID id,String actor,Review r){return read(c,actor,()->plan(c,id,actor,r));}
    private Impact plan(UUID c,UUID id,String actor,Review r){
        if(r.expectedVersion()<0||r.campaignVersion()<0)throw ModuleBlueprint.invalid();var d=target(c,id);
        AccountPolicy.version(d.version(),r.expectedVersion());AccountPolicy.version(campaigns.campaignVersion(c),r.campaignVersion());
        var instance=lifecycle.readInstance(c,actor);var entries=content.list(c,actor,"","","","","").entries();
        for(var dep:instance.source().manifest().dependencies()){
            UUID local=instance.mapping().getOrDefault("dependencies",Map.of()).get(dep.id());
            if(local==null||entries.stream().noneMatch(e->e.id().equals(local)&&e.source().revision().equals(dep.version())&&e.source().rules().equals(dep.rules())))throw new DomainException("MODULE_DEPENDENCIES_MISSING",409,"Resolve exact local module dependencies");
        }
        var targets=new ArrayList<JsonNode>();
        for(var step:d.steps()){
            Object snapshot=switch(step){
                case RevealToken s -> {var token=token(c,actor,s);AccountPolicy.version(token.version(),s.expectedVersion());yield token;}
                case RevealCharacter s -> {var access=characters.access(c,s.characterId(),actor);AccountPolicy.version(access.version(),s.expectedVersion());if(access.importReview())throw unsupported();yield List.of(access,characters.detail(c,s.characterId(),actor));}
                case PublishText s -> {var note=notes.detail(c,s.noteId(),actor);AccountPolicy.version(note.version(),s.expectedVersion());if(!note.visibility().equals("DM")||!note.attachments().isEmpty())throw unsupported();yield note;}
                case PrepareEncounter s -> {var g=new InitiativeService.Generate(UUID.randomUUID(),s.expectedVersion(),s.participants(),s.reason());yield initiative.reviewGenerate(c,actor,g);}
                case StartEncounter s -> {var state=(InitiativeService.State)initiative.view(c,actor);AccountPolicy.version(state.version(),s.initiativeVersion());var combat=turns.reviewStart(c,actor,s.expectedVersion());var roster=state.entries().stream().map(e->List.of(characters.access(c,e.characterId(),actor),characters.detail(c,e.characterId(),actor))).toList();yield List.of(state,combat,roster);}
            };
            targets.add(json.valueToTree(snapshot));
        }
        return new Impact(id,d.version(),r.campaignVersion(),fp(c,d,r.campaignVersion(),instance,targets),d,List.copyOf(targets));
    }
    private TokenService.Token token(UUID c,String actor,RevealToken s){return tokens.list(c,s.sceneId(),actor).tokens().stream().filter(t->t.id().equals(s.tokenId())).findFirst().orElseThrow(ModuleOperations::missing);}
    private static DomainException missing(){return new DomainException("NOT_FOUND",404,"Operation target not found");}
    private static DomainException unsupported(){return new DomainException("MODULE_OPERATION_UNSUPPORTED",409,"Operation requires separate explicit authoring");}
    public List<Execution> executions(UUID c,UUID id,String actor){return read(c,actor,()->{target(c,id);return store.executions(c,id);});}
    public Execution execute(UUID c,UUID id,String actor,Execute r){
        if(r.requestId()==null||r.expectedVersion()<0||r.campaignVersion()<0||r.impactHash()==null||!r.impactHash().matches("[a-f0-9]{64}"))throw ModuleBlueprint.invalid();
        return command(c,actor,()->{
            String hash=fp("EXECUTE",c,id,r);var old=replay(actor,r.requestId(),hash);if(old!=null)return json.treeToValue(old.result(),Execution.class);
            var impact=plan(c,id,actor,new Review(r.expectedVersion(),r.campaignVersion()));
            if(!impact.impactHash().equals(r.impactHash()))throw new DomainException("MODULE_IMPACT_CONFLICT",409,"Review the current operation impact");
            var results=new ArrayList<JsonNode>();var revealed=new HashSet<UUID>();
            for(var step:impact.definition().steps()){
                // Child request IDs are server-owned and only committed with this execution.
                UUID child=UUID.randomUUID();Object result=switch(step){
                    case RevealToken s -> {var t=token(c,actor,s);yield tokens.update(c,s.sceneId(),s.tokenId(),actor,new TokenService.Command(child,s.expectedVersion(),t.name(),t.x(),t.y(),false,t.controllers()));}
                    case RevealCharacter s -> {var access=characters.access(c,s.characterId(),actor);var ack=characters.access(c,s.characterId(),actor,child,s.expectedVersion(),access.controllers(),false,access.shareFull(),false,"模组手动揭露："+impact.definition().name());revealed.add(s.characterId());yield ack;}
                    case PublishText s -> {var n=notes.detail(c,s.noteId(),actor);yield notes.create(c,actor,new NotesService.Create(child,r.campaignVersion(),"TEAM",n.title(),n.text(),List.of()));}
                    case PrepareEncounter s -> {var participants=s.participants().stream().map(p->new InitiativeService.Participant(p.characterId(),p.expectedVersion()+(revealed.contains(p.characterId())?1:0),p.kind(),p.group(),p.surprised(),p.advantage(),p.disadvantage())).toList();yield initiative.generate(c,actor,new InitiativeService.Generate(child,s.expectedVersion(),participants,s.reason()));}
                    case StartEncounter s -> turns.command(c,actor,new TurnService.Command(child,s.expectedVersion(),"START","",List.of(),s.reason()));
                };results.add(json.valueToTree(result));
            }
            var value=new Execution(UUID.randomUUID(),r.requestId(),actor,clock.instant(),impact,List.copyOf(results));store.execution(c,id,value);
            store.receipt(c,actor,r.requestId(),new Receipt(hash,json.valueToTree(value)));return value;
        });
    }
}
