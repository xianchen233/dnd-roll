package io.dndroll.modules;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.domain.DomainException;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;

class ModulePortableTest {
    final JsonMapper json=JsonMapper.builder().build();final ModuleArchive archive=new ModuleArchive();final ModuleBlueprintTest fixture=new ModuleBlueprintTest();
    final String trigger="20000000-0000-4000-8000-000000000001",start="20000000-0000-4000-8000-000000000002",document="20000000-0000-4000-8000-000000000003";
    ObjectNode node(String id,String kind){return (ObjectNode)json.valueToTree(Map.of("id",id,"kind",kind,"name","稳定身份 <literal>","text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()));}
    Map<String,Object> step(String kind,String key,String id){return Map.of("kind",kind,key,id);}
    Map<String,Object> participant(String unit){return Map.of("unitId",unit,"kind","MONSTER","group","","surprised",true,"advantage",List.of("明确来源"),"disadvantage",List.of());}
    ObjectNode document(){
        var d=fixture.document();var nodes=(ArrayNode)d.get("nodes");nodes.add(node(trigger,"TRIGGER"));nodes.add(node(start,"TRIGGER"));nodes.add(node(document,"DOCUMENT"));
        d.set("operations",json.valueToTree(Map.of("formatVersion",1,"definitions",List.of(Map.of("nodeId",trigger,"steps",List.of(step("REVEAL_TOKEN","placementId",fixture.placement),step("REVEAL_CHARACTER","unitId",fixture.unit),step("PUBLISH_MATERIAL","documentId",document),Map.of("kind","PREPARE_ENCOUNTER","participants",List.of(participant(fixture.unit)),"reason","明确准备"))),Map.of("nodeId",start,"steps",List.of(Map.of("kind","START_ENCOUNTER","reason","需绑定当前确认先攻")))))));return d;
    }
    byte[] zip(JsonNode content,int version)throws Exception{
        byte[] bytes=json.writeValueAsBytes(content);var m=fixture.manifest();var manifest=new ModuleArchive.Manifest(m.format(),version,m.moduleId(),m.moduleVersion(),m.name(),m.rules(),m.rights(),m.dependencies(),new ModuleArchive.Content(bytes.length,ModuleArchive.hash(bytes)),m.assets());
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){for(var e:Map.of("manifest.json",json.writeValueAsBytes(manifest),"content.json",bytes).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}return out.toByteArray();
    }
    ModuleArchive.Verified valid(JsonNode d)throws Exception{return archive.verify(zip(d,3));}
    void invalid(JsonNode d)throws Exception{var bytes=zip(d,3);var error=assertThrows(DomainException.class,()->archive.verify(bytes));assertEquals("INVALID_MODULE",error.code());assertEquals(400,error.status());}
    ArrayNode definitions(ObjectNode d){return (ArrayNode)d.get("operations").get("definitions");}
    ArrayNode steps(ObjectNode d){return (ArrayNode)definitions(d).get(0).get("steps");}
    @Test void versionThreeRetainsStableReferencesAndStrictInertVocabulary()throws Exception{
        var d=document();var verified=valid(d);assertEquals(3,verified.manifest().formatVersion());assertEquals(d,json.valueToTree(verified.document()));
        var text=document();((ObjectNode)steps(text).get(2)).put("kind","PUBLISH_TEXT");assertEquals(text,json.valueToTree(valid(text).document()));
    }
    @Test void originalFormatsStayExactAndDoNotAcquirePortableFields()throws Exception{
        var old=fixture.document();assertEquals(old,json.valueToTree(archive.verify(zip(old,2)).document()));
        var legacy=json.createObjectNode();legacy.set("nodes",old.get("nodes"));assertFalse(json.valueToTree(archive.verify(zip(legacy,1)).document()).has("operations"));
        var bytes=zip(document(),2);assertThrows(DomainException.class,()->archive.verify(bytes));var missing=document();missing.remove("operations");invalid(missing);
        var empty=document();definitions(empty).removeAll();assertEquals(empty,json.valueToTree(valid(empty).document()));
    }
    @Test void everyEnvelopeDefinitionAndStepFieldRejectsMissingNullExtraAndRuntimeIdentity()throws Exception{
        for(String field:List.of("formatVersion","definitions")){var d=document();((ObjectNode)d.get("operations")).remove(field);invalid(d);d=document();((ObjectNode)d.get("operations")).putNull(field);invalid(d);}
        for(String field:List.of("nodeId","steps")){var d=document();((ObjectNode)definitions(d).get(0)).remove(field);invalid(d);}
        for(String extra:List.of("expectedVersion","tokenId","characterId","noteId","requestId","initiativeVersion","actor","controllers","script")){var d=document();((ObjectNode)steps(d).get(0)).put(extra,"forged");invalid(d);}
        var d=document();((ObjectNode)d.get("operations")).put("formatVersion",2);invalid(d);d=document();((ObjectNode)d.get("operations")).put("formatVersion","1");invalid(d);
        for(String kind:List.of("SCRIPT","CONDITION","HTTP","CONFIRM_ENCOUNTER","APPLY_DAMAGE")){d=document();((ObjectNode)steps(d).get(0)).put("kind",kind);invalid(d);}
    }
    @Test void danglingWrongKindAndDuplicateReferencesAreNeverAcceptedByName()throws Exception{
        var d=document();((ObjectNode)steps(d).get(0)).put("placementId",fixture.unit);invalid(d);
        d=document();((ObjectNode)steps(d).get(1)).put("unitId",fixture.scene);invalid(d);
        d=document();((ObjectNode)steps(d).get(2)).put("documentId",trigger);invalid(d);
        d=document();((ObjectNode)definitions(d).get(0)).put("nodeId",document);invalid(d);
        d=document();((ObjectNode)d.get("nodes").get(2)).put("visibility","PUBLIC");invalid(d);
        d=document();definitions(d).add(definitions(d).get(0).deepCopy());invalid(d);
        d=document();steps(d).add(json.valueToTree(step("PUBLISH_TEXT","documentId",document)));invalid(d);
        d=document();steps(d).add(steps(d).get(0).deepCopy());invalid(d);
        d=document();steps(d).add(json.valueToTree(Map.of("kind","START_ENCOUNTER","reason","不能隐式确认")));invalid(d);
    }
    @Test void definitionAndStepCapacityUseDistinctStableTargets()throws Exception{
        var d=document();definitions(d).removeAll();var nodes=(ArrayNode)d.get("nodes");for(int n=0;n<100;n++){String id=UUID.randomUUID().toString();nodes.add(node(id,"TRIGGER"));definitions(d).add(json.valueToTree(Map.of("nodeId",id,"steps",List.of(Map.of("kind","START_ENCOUNTER","reason","人工确认")))));}
        assertEquals(100,json.valueToTree(valid(d).document()).get("operations").get("definitions").size());String id=UUID.randomUUID().toString();nodes.add(node(id,"TRIGGER"));definitions(d).add(json.valueToTree(Map.of("nodeId",id,"steps",List.of(Map.of("kind","START_ENCOUNTER","reason","多一个")))));invalid(d);
        d=document();steps(d).removeAll();for(int n=0;n<20;n++){id=UUID.randomUUID().toString();((ArrayNode)d.get("nodes")).add(node(id,"DOCUMENT"));steps(d).add(json.valueToTree(step("PUBLISH_MATERIAL","documentId",id)));}valid(d);id=UUID.randomUUID().toString();((ArrayNode)d.get("nodes")).add(node(id,"DOCUMENT"));steps(d).add(json.valueToTree(step("PUBLISH_MATERIAL","documentId",id)));invalid(d);d=document();steps(d).removeAll();invalid(d);
    }
    @Test void participantsUseStableUnitIdentityAndExistingP05Bounds()throws Exception{
        var d=document();var p=(ObjectNode)steps(d).get(3).get("participants").get(0);p.put("unitId",fixture.scene);invalid(d);
        d=document();p=(ObjectNode)steps(d).get(3).get("participants").get(0);p.put("kind","PC");p.put("group","not allowed");invalid(d);
        d=document();((ArrayNode)steps(d).get(3).get("participants")).add(json.valueToTree(participant(fixture.unit)));invalid(d);
        d=document();((ObjectNode)steps(d).get(3).get("participants").get(0)).put("expectedVersion",0);invalid(d);
    }
    @Test void completeUtf8DefinitionLimitDoesNotPermitOversizedSourceLists()throws Exception{
        var d=document();var people=json.createArrayNode();for(int n=0;n<20;n++){String id=UUID.randomUUID().toString();((ArrayNode)d.get("nodes")).add(node(id,"UNIT"));var spec=fixture.document().get("play").get("units").get(0).deepCopy();((ObjectNode)spec).put("nodeId",id);((ArrayNode)d.get("play").get("units")).add(spec);var p=(ObjectNode)json.valueToTree(participant(id));p.set("advantage",json.valueToTree(Collections.nCopies(10,"中".repeat(200))));people.add(p);}
        steps(d).removeAll();steps(d).add(json.valueToTree(Map.of("kind","PREPARE_ENCOUNTER","participants",people,"reason","明确准备")));assertTrue(json.writeValueAsBytes(definitions(d).get(0)).length>65536);invalid(d);
    }
    @Test void saveAsRoundtripKeepsVersionAndStableReferencesWithoutInventingRuntimeFields()throws Exception{
        var d=document();var verified=valid(d);byte[] first=archive.export(verified.manifest(),verified.document(),Map.of(),fixture.scene,"2.0.0","便携操作"),second=archive.export(verified.manifest(),verified.document(),Map.of(),fixture.scene,"2.0.0","便携操作");assertArrayEquals(first,second);var reopened=archive.verify(first);assertEquals(3,reopened.manifest().formatVersion());assertEquals(d,json.valueToTree(reopened.document()));
        var old=archive.verify(zip(fixture.document(),2));assertEquals(2,archive.verify(archive.export(old.manifest(),old.document(),Map.of(),fixture.scene,"3.0.0","旧格式")).manifest().formatVersion());
    }
    @Test void comparisonUsesDefinitionIdentityRatherThanArrayOrder()throws Exception{
        var d=document();var b=valid(d).document();var reordered=d.deepCopy();var list=definitions(reordered);var first=list.remove(0);list.add(first);assertTrue(ModuleUpdate.compare(b,b,valid(reordered).document()).isEmpty());
        ((ObjectNode)list.get(0).get("steps").get(0)).put("reason","更新的明确理由");var changes=ModuleUpdate.compare(b,b,valid(reordered).document());assertEquals(1,changes.size());assertTrue(changes.getFirst().path().startsWith("/operations/definitions/"+start+"/"));
    }
}
