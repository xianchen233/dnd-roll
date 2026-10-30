package io.dndroll.modules;

import io.dndroll.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;

class ModuleBlueprintTest {
    final JsonMapper json=JsonMapper.builder().build();
    final String scene="10000000-0000-4000-8000-000000000001",unit="10000000-0000-4000-8000-000000000002",placement="10000000-0000-4000-8000-000000000003";
    ModuleArchive.Manifest manifest(){return new ModuleArchive.Manifest("dnd-roll.module",2,scene,"1.0.0","fixture","5R","original",List.of(),new ModuleArchive.Content(1,"a".repeat(64)),List.of());}
    ObjectNode document(){var nodes=List.of(new ModuleArchive.Node(scene,"SCENE","场景","","DM",List.of(),List.of(),List.of()),new ModuleArchive.Node(unit,"UNIT","单位","","DM",List.of(),List.of(),List.of()));return (ObjectNode)json.valueToTree(new ModuleBlueprint.Document(nodes,new ModuleBlueprint.Play(List.of(new ModuleBlueprint.Scene(scene,20,15,48,null)),List.of(new ModuleBlueprint.Unit(unit,CharacterSheet.empty("测试单位"),true,false)),List.of(new ModuleBlueprint.Placement(placement,scene,unit,1,2,true)),"REVIEW",List.of())));}
    void invalid(ObjectNode n){assertThrows(DomainException.class,()->ModuleBlueprint.parse(n,manifest()));}
    @Test void usesExplicitTypedPlacementsAndPortableSheets(){var d=ModuleBlueprint.parse(document(),manifest());assertEquals(unit,d.play().placements().getFirst().unitId());assertTrue(d.play().units().getFirst().hidden());assertEquals(1,d.play().units().getFirst().sheet().combat().hp());}
    @Test void rejectsUnknownOrMissingNestedFieldsAndScalarCoercion(){
        for(String f:List.of("nodes","play")){var n=document();n.remove(f);invalid(n);}
        var n=document();((ObjectNode)n.get("play").get("units").get(0)).put("controllers","forged");invalid(n);
        n=document();((ObjectNode)n.get("play").get("scenes").get(0)).remove("map");invalid(n);
        for(Object value:List.of("20",20.5)){n=document();((ObjectNode)n.get("play").get("scenes").get(0)).set("width",json.valueToTree(value));invalid(n);}
        n=document();((ObjectNode)n.get("play").get("units").get(0).get("sheet").get("combat")).put("hp",-1);invalid(n);
    }
    @Test void rejectsDanglingDuplicateAndOutOfBoundsReferences(){
        for(String field:List.of("sceneId","unitId","id")){var n=document();((ObjectNode)n.get("play").get("placements").get(0)).put(field,field.equals("sceneId")?unit:scene);invalid(n);}
        var n=document();var list=(tools.jackson.databind.node.ArrayNode)n.get("play").get("placements");list.add(list.get(0).deepCopy());invalid(n);
        n=document();((ObjectNode)n.get("play").get("placements").get(0)).put("x",20);invalid(n);
        n=document();((ObjectNode)n.get("play")).set("approvedDependencyIds",json.valueToTree(List.of(unit)));invalid(n);
    }
    @Test void rejectsUnboundedAndExecutableShapesButPreservesLiteralProse(){
        var n=document();((ObjectNode)n.get("nodes").get(0)).put("text","<script>literal</script>");assertEquals("<script>literal</script>",ModuleBlueprint.parse(n,manifest()).nodes().getFirst().text());
        ((ObjectNode)n.get("play")).put("execute","arbitrary()");invalid(n);
        n=document();((ObjectNode)n.get("nodes").get(0)).put("text","x".repeat(10001));invalid(n);
    }
}
