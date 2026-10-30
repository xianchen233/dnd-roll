package io.dndroll.modules;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ModuleOperationsTest {
    final JsonMapper json=JsonMapper.builder().build();
    @Test void materialPublicationIsExplicitStrictAndSharesDuplicateIdentityWithText(){
        var note=UUID.randomUUID();var material=Map.of("kind","PUBLISH_MATERIAL","noteId",note,"expectedVersion",0);
        assertEquals(1,ModuleOperations.parseSave(save(List.of(material))).steps().size());
        assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(material,Map.of("kind","PUBLISH_TEXT","noteId",note,"expectedVersion",0)))));
        for(String field:material.keySet()){var bad=new HashMap<String,Object>(material);bad.remove(field);assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(bad))));bad.put(field,null);assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(bad))));}
        var extra=new HashMap<String,Object>(material);extra.put("attachments",List.of());assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(extra))));
    }
    Map<String,Object> token(){return Map.of("kind","REVEAL_TOKEN","sceneId",UUID.randomUUID(),"tokenId",UUID.randomUUID(),"expectedVersion",0);}
    JsonNode save(List<?> steps){return json.valueToTree(Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","中文组合","steps",steps));}
    @Test void twentyDistinctTypedStepsFitButZeroTwentyOneDuplicatesAndUnknownCodeDoNot(){
        var steps=new ArrayList<Map<String,Object>>();for(int n=0;n<20;n++)steps.add(token());assertEquals(20,ModuleOperations.parseSave(save(steps)).steps().size());
        steps.add(token());assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(steps)));assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of())));
        var one=token();assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(one,one))));assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(Map.of("kind","SCRIPT","code","alert(1)")))));
    }
    @Test void strictDiscriminatedShapeRejectsEveryMissingNullExtraAndWrongScalarField(){
        var one=token();for(String field:one.keySet()){
            var bad=new HashMap<>(one);bad.remove(field);assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(bad))));
            bad.put(field,null);assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(bad))));
        }
        for(Object value:List.of(-1,0.5,"0",false)){var bad=new HashMap<>(one);bad.put("expectedVersion",value);assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(bad))));}
        var extra=new HashMap<>(one);extra.put("actor","forged");assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(extra))));
    }
    @Test void onlyOneEncounterStepAndNoImplicitConfirmOrAutomaticConditions(){
        var start=Map.of("kind","START_ENCOUNTER","expectedVersion",0,"initiativeVersion",2,"reason","DM starts");assertEquals(1,ModuleOperations.parseSave(save(List.of(start))).steps().size());
        assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(start,start))));
        for(String kind:List.of("CONFIRM_ENCOUNTER","APPLY_DAMAGE","CONDITION","HTTP","REVEAL_MAP"))assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(Map.of("kind",kind)))));
    }
    @Test void completeUtf8BodyHasSixtyFourKiBLimitAndParticipantFieldsStayStrict(){
        var people=new ArrayList<Map<String,Object>>();for(int n=0;n<100;n++)people.add(Map.of("characterId",UUID.randomUUID(),"expectedVersion",0,"kind","MONSTER","group","","surprised",false,"advantage",List.of("中".repeat(180)),"disadvantage",List.of()));
        var body=save(List.of(Map.of("kind","PREPARE_ENCOUNTER","expectedVersion",0,"participants",people,"reason","Prepare")));assertTrue(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536);assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(body));
        var bad=Map.of("kind","PREPARE_ENCOUNTER","expectedVersion",0,"participants",List.of(Map.of("characterId",UUID.randomUUID())),"reason","Prepare");assertThrows(RuntimeException.class,()->ModuleOperations.parseSave(save(List.of(bad))));
    }
}
