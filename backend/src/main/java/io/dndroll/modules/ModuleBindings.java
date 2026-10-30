package io.dndroll.modules;

import io.dndroll.application.*;
import io.dndroll.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;

/** Imported identities are inert until a current target DM explicitly reviews and binds them. */
public final class ModuleBindings {
    public record Binding(UUID nodeId,UUID operationId,long version,String name,ModulePortable.Definition source,long boundVersion) {}
    public record View(Binding binding,ModuleOperations.Definition operation,boolean locallyModified) {}
    public record Listing(long instanceVersion,long campaignVersion,List<View> bindings) {}
    public record Review(long expectedVersion,long operationVersion,long instanceVersion,long campaignVersion) {}
    public record Bind(UUID requestId,long expectedVersion,long operationVersion,long instanceVersion,long campaignVersion,String impactHash,String reason) {}
    public record Preview(Binding binding,long operationVersion,long instanceVersion,long campaignVersion,ModuleOperations.Save candidate,List<tools.jackson.databind.JsonNode> targets,String impactHash) {}
    public record Ack(UUID nodeId,UUID operationId,long version,long operationVersion) {}
    public record Evidence(long version,String actor,Instant at,String reason,Binding before,Binding after,ModuleOperations.Definition previous,Preview preview) {}
    public record Receipt(String fingerprint,Ack result) {}
    public interface Store {
        List<Binding> list(UUID c);Binding binding(UUID c,UUID node);void put(UUID c,Binding binding,boolean create);
        List<Evidence> history(UUID c,UUID node);void history(UUID c,UUID node,Evidence value);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,UUID node,String actor,UUID request,Receipt value);
    }
    private final Store store;private final CharacterService.Store campaigns;private final ModuleLifecycle lifecycle;private final ModuleOperations operations;private final ModuleOperations.Store definitions;
    private final CharacterService characters;private final TokenService tokens;private final NotesService notes;private final InitiativeService initiative;private final TurnService turns;private final JsonMapper json;private final Clock clock;private final SceneService.Events events;
    public ModuleBindings(Store store,CharacterService.Store campaigns,ModuleLifecycle lifecycle,ModuleOperations operations,ModuleOperations.Store definitions,CharacterService characters,TokenService tokens,NotesService notes,InitiativeService initiative,TurnService turns,JsonMapper json,Clock clock,SceneService.Events events){this.store=store;this.campaigns=campaigns;this.lifecycle=lifecycle;this.operations=operations;this.definitions=definitions;this.characters=characters;this.tokens=tokens;this.notes=notes;this.initiative=initiative;this.turns=turns;this.json=json;this.clock=clock;this.events=events;}
    private <T>T read(UUID c,String actor,Supplier<T> work){return campaigns.locked(c,()->{lifecycle.readInstance(c,actor);return work.get();});}
    private Binding target(UUID c,UUID node,ModuleLifecycle.Instance instance){var binding=store.binding(c,node);if(binding==null||!binding.operationId().equals(instance.mapping().getOrDefault("operations",Map.of()).get(node.toString())))throw new DomainException("NOT_FOUND",404,"Portable binding not found");return binding;}
    public Listing list(UUID c,String actor){return read(c,actor,()->{var instance=lifecycle.readInstance(c,actor);var views=store.list(c).stream().map(b->{var d=definitions.definition(c,b.operationId());return new View(b,d,d!=null&&d.version()!=b.boundVersion());}).toList();return new Listing(instance.version(),campaigns.campaignVersion(c),views);});}
    private static UUID mapped(ModuleLifecycle.Instance instance,String group,String id){var value=instance.mapping().getOrDefault(group,Map.of()).get(id);if(value==null)throw new DomainException("MODULE_BINDING_NOT_READY",409,"Stable target is not mapped in this instance");return value;}
    private ModuleOperations.Save resolve(UUID c,String actor,Binding binding,ModuleLifecycle.Instance instance,long version){
        var steps=new ArrayList<ModuleOperations.Step>();
        for(var step:binding.source().steps())steps.add(switch(step){
            case ModulePortable.RevealToken s -> {
                var placement=instance.document().play().placements().stream().filter(p->p.id().equals(s.placementId())).findFirst().orElseThrow(ModuleBlueprint::invalid);
                UUID scene=mapped(instance,"scenes",placement.sceneId()),token=mapped(instance,"placements",s.placementId()),card=mapped(instance,"units",placement.unitId());
                var current=tokens.list(c,scene,actor).tokens().stream().filter(t->t.id().equals(token)).findFirst().orElseThrow(()->notReady());
                if(characters.placements(c,card,actor).stream().noneMatch(p->p.tokenId().equals(token)&&p.sceneId().equals(scene)))throw notReady();
                yield new ModuleOperations.RevealToken(scene,token,current.version());
            }
            case ModulePortable.RevealCharacter s -> {UUID id=mapped(instance,"units",s.unitId());yield new ModuleOperations.RevealCharacter(id,characters.access(c,id,actor).version());}
            case ModulePortable.PublishText s -> {UUID id=mapped(instance,"documents",s.documentId());yield new ModuleOperations.PublishText(id,notes.detail(c,id,actor).version());}
            case ModulePortable.PublishMaterial s -> {UUID id=mapped(instance,"documents",s.documentId());yield new ModuleOperations.PublishMaterial(id,notes.detail(c,id,actor).version());}
            case ModulePortable.PrepareEncounter s -> {
                var state=(InitiativeService.State)initiative.view(c,actor);var participants=s.participants().stream().map(p->{UUID id=mapped(instance,"units",p.unitId());return new InitiativeService.Participant(id,characters.access(c,id,actor).version(),p.kind(),p.group(),p.surprised(),p.advantage(),p.disadvantage());}).toList();yield new ModuleOperations.PrepareEncounter(state.version(),participants,s.reason());
            }
            case ModulePortable.StartEncounter s -> {var state=(InitiativeService.State)initiative.view(c,actor);var combat=(TurnService.State)turns.view(c,actor);if(!state.status().equals("CONFIRMED")||!combat.status().equals("IDLE"))throw notReady();yield new ModuleOperations.StartEncounter(combat.version(),state.version(),s.reason());}
        });
        // The complete real save envelope, including name/UUID/versions, must fit the existing bound.
        return ModuleOperations.parseSave(json.valueToTree(new ModuleOperations.Save(new UUID(0,0),version,binding.name(),steps)));
    }
    private static DomainException notReady(){return new DomainException("MODULE_BINDING_NOT_READY",409,"Review current mapped targets and confirm initiative separately");}
    private Preview plan(UUID c,UUID node,String actor,Review r){
        if(r.expectedVersion()<1||r.operationVersion()<0||r.instanceVersion()<0||r.campaignVersion()<0)throw ModuleBlueprint.invalid();var instance=lifecycle.readInstance(c,actor);var binding=target(c,node,instance);var previous=definitions.definition(c,binding.operationId());
        AccountPolicy.version(binding.version(),r.expectedVersion());AccountPolicy.version(instance.version(),r.instanceVersion());AccountPolicy.version(campaigns.campaignVersion(c),r.campaignVersion());AccountPolicy.version(previous==null?0:previous.version(),r.operationVersion());
        var candidate=resolve(c,actor,binding,instance,r.operationVersion());var impact=operations.reviewCandidate(c,binding.operationId(),actor,candidate,r.campaignVersion());
        String hash=DiceService.fingerprint(json,List.of(c,binding,instance,previous==null?"":previous,candidate,impact.targets(),r.campaignVersion()));return new Preview(binding,r.operationVersion(),instance.version(),r.campaignVersion(),candidate,impact.targets(),hash);
    }
    public Preview preview(UUID c,UUID node,String actor,Review r){return read(c,actor,()->plan(c,node,actor,r));}
    public List<Evidence> history(UUID c,UUID node,String actor){return read(c,actor,()->{if(store.binding(c,node)==null)throw new DomainException("NOT_FOUND",404,"Portable binding not found");return store.history(c,node);});}
    public Ack bind(UUID c,UUID node,String actor,Bind r){
        if(r.requestId()==null||r.impactHash()==null||!r.impactHash().matches("[a-f0-9]{64}"))throw ModuleBlueprint.invalid();CharacterSheet.text(r.reason(),1000,true);
        return campaigns.actorLocked(actor,()->read(c,actor,()->{
            // read() has already checked current target-DM access. Retirement blocks
            // fresh bindings, while an exact historical receipt remains inert.
            String fp=DiceService.fingerprint(json,List.of(c,node,"BIND",r));var old=store.receipt(actor,r.requestId());if(old!=null){if(!old.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old.result();}
            var before=target(c,node,lifecycle.readInstance(c,actor));
            var preview=plan(c,node,actor,new Review(r.expectedVersion(),r.operationVersion(),r.instanceVersion(),r.campaignVersion()));if(!preview.impactHash().equals(r.impactHash()))throw new DomainException("MODULE_IMPACT_CONFLICT",409,"Review current binding targets before saving");
            var previous=definitions.definition(c,before.operationId());var candidate=preview.candidate();var saved=operations.saveBound(c,before.operationId(),actor,new ModuleOperations.Save(UUID.randomUUID(),candidate.expectedVersion(),candidate.name(),candidate.steps()));var after=new Binding(node,before.operationId(),before.version()+1,before.name(),before.source(),saved.version());store.put(c,after,false);store.history(c,node,new Evidence(after.version(),actor,clock.instant(),r.reason(),before,after,previous,preview));
            var ack=new Ack(node,after.operationId(),after.version(),saved.version());store.receipt(c,node,actor,r.requestId(),new Receipt(fp,ack));events.changed(c);return ack;
        }));
    }
}
