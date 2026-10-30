package io.dndroll.modules;

import com.fasterxml.jackson.annotation.*;
import io.dndroll.application.InitiativeService;
import io.dndroll.domain.CharacterSheet;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Inert stable references. Runtime identities and versions are deliberately a different type. */
public final class ModulePortable {
    public record Block(int formatVersion,List<Definition> definitions) {public Block{definitions=List.copyOf(definitions);}}
    public record Definition(String nodeId,List<Step> steps) {public Definition{steps=List.copyOf(steps);}}
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME,property="kind")
    @JsonSubTypes({@JsonSubTypes.Type(value=RevealToken.class,name="REVEAL_TOKEN"),@JsonSubTypes.Type(value=RevealCharacter.class,name="REVEAL_CHARACTER"),@JsonSubTypes.Type(value=PublishText.class,name="PUBLISH_TEXT"),@JsonSubTypes.Type(value=PublishMaterial.class,name="PUBLISH_MATERIAL"),@JsonSubTypes.Type(value=PrepareEncounter.class,name="PREPARE_ENCOUNTER"),@JsonSubTypes.Type(value=StartEncounter.class,name="START_ENCOUNTER")})
    public sealed interface Step permits RevealToken,RevealCharacter,PublishText,PublishMaterial,PrepareEncounter,StartEncounter {}
    public record RevealToken(String placementId) implements Step {}
    public record RevealCharacter(String unitId) implements Step {}
    public record PublishText(String documentId) implements Step {}
    public record PublishMaterial(String documentId) implements Step {}
    public record PrepareEncounter(List<Participant> participants,String reason) implements Step {public PrepareEncounter{participants=List.copyOf(participants);}}
    public record StartEncounter(String reason) implements Step {}
    public record Participant(String unitId,String kind,String group,boolean surprised,List<String> advantage,List<String> disadvantage) {public Participant{advantage=List.copyOf(advantage);disadvantage=List.copyOf(disadvantage);}}
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public static void shape(JsonNode value){
        if(!value.isObject()||!value.has("kind")||!value.get("kind").isString())throw ModuleBlueprint.invalid();
        Class<?> type=switch(value.get("kind").asString()){
            case "REVEAL_TOKEN" -> RevealToken.class;case "REVEAL_CHARACTER" -> RevealCharacter.class;
            case "PUBLISH_TEXT" -> PublishText.class;case "PUBLISH_MATERIAL" -> PublishMaterial.class;
            case "PREPARE_ENCOUNTER" -> PrepareEncounter.class;case "START_ENCOUNTER" -> StartEncounter.class;
            default -> throw ModuleBlueprint.invalid();
        };
        var body=(tools.jackson.databind.node.ObjectNode)value.deepCopy();body.remove("kind");ModuleBlueprint.shape(body,type);
    }
    public static boolean requiresBinding(ModuleBlueprint.Document d){return d.operations()!=null&&!d.operations().definitions().isEmpty();}
    public static void validate(Block block,ModuleBlueprint.Document d){
        if(block==null)return;
        if(block.formatVersion()!=1||block.definitions().size()>100)throw ModuleBlueprint.invalid();
        var nodes=new HashMap<String,ModuleArchive.Node>();d.nodes().forEach(n->nodes.put(n.id(),n));
        var units=new HashSet<String>();d.play().units().forEach(u->units.add(u.nodeId()));var placements=new HashSet<String>();d.play().placements().forEach(p->placements.add(p.id()));
        var seen=new HashSet<String>();
        for(var definition:block.definitions()){
            var node=nodes.get(definition.nodeId());if(node==null||!node.kind().equals("TRIGGER")||!node.visibility().equals("DM")||!seen.add(definition.nodeId())||definition.steps().isEmpty()||definition.steps().size()>20||JSON.writeValueAsBytes(definition).length>65536)throw ModuleBlueprint.invalid();
            var targets=new HashSet<String>();
            for(var step:definition.steps()){
                String key=switch(step){
                    case RevealToken s -> {if(!placements.contains(s.placementId()))throw ModuleBlueprint.invalid();yield "placement:"+s.placementId();}
                    case RevealCharacter s -> {if(!units.contains(s.unitId()))throw ModuleBlueprint.invalid();yield "unit:"+s.unitId();}
                    case PublishText s -> {var note=document(nodes,s.documentId());if(!note.assetIds().isEmpty())throw ModuleBlueprint.invalid();yield "document:"+s.documentId();}
                    case PublishMaterial s -> {document(nodes,s.documentId());yield "document:"+s.documentId();}
                    case PrepareEncounter s -> {
                        var participants=new ArrayList<InitiativeService.Participant>();for(var p:s.participants()){if(!units.contains(p.unitId()))throw ModuleBlueprint.invalid();participants.add(new InitiativeService.Participant(UUID.fromString(p.unitId()),0,p.kind(),p.group(),p.surprised(),p.advantage(),p.disadvantage()));}
                        InitiativeService.validateGenerate(new InitiativeService.Generate(new UUID(0,0),0,participants,s.reason()));yield "encounter";
                    }
                    case StartEncounter s -> {CharacterSheet.text(s.reason(),1000,true);yield "encounter";}
                };
                if(!targets.add(key))throw ModuleBlueprint.invalid();
            }
        }
    }
    private static ModuleArchive.Node document(Map<String,ModuleArchive.Node> nodes,String id){var n=nodes.get(id);if(n==null||!n.kind().equals("DOCUMENT")||!n.visibility().equals("DM"))throw ModuleBlueprint.invalid();return n;}
}
