package io.dndroll.modules;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleUpdateTest {
    final String a="10000000-0000-4000-8000-000000000001",b="10000000-0000-4000-8000-000000000002";
    ModuleArchive.Node node(String id,String name,String text){return new ModuleArchive.Node(id,"DOCUMENT",name,text,"DM",List.of(),List.of(),List.of());}
    ModuleBlueprint.Document doc(ModuleArchive.Node... nodes){return new ModuleBlueprint.Document(List.of(nodes),ModuleBlueprint.empty());}
    @Test void preservesIndependentEditsAndShowsOnlyTrueConflicts(){
        var changes=ModuleUpdate.compare(doc(node(a,"original","base")),doc(node(a,"local name","local text")),doc(node(a,"original","upstream")));
        assertEquals(2,changes.size());
        var name=changes.stream().filter(v->v.path().endsWith("/name")).findFirst().orElseThrow();
        assertFalse(name.conflict());assertEquals("local name",name.current().asString());assertEquals("original",name.incoming().asString());
        var text=changes.stream().filter(v->v.path().endsWith("/text")).findFirst().orElseThrow();
        assertTrue(text.conflict());assertEquals("base",text.baseline().asString());assertEquals("local text",text.current().asString());assertEquals("upstream",text.incoming().asString());
    }
    @Test void identityArrayReorderIsNotAChange(){
        var first=node(a,"one","");var second=node(b,"two","");
        assertTrue(ModuleUpdate.compare(doc(first,second),doc(second,first),doc(first,second)).isEmpty());
    }
    @Test void deletingLocallyEditedIdentityRequiresExplicitConflictDecision(){
        var changes=ModuleUpdate.compare(doc(node(a,"base","")),doc(node(a,"local","")),doc());
        assertEquals(1,changes.size());var c=changes.getFirst();assertEquals("/nodes/"+a,c.path());assertTrue(c.conflict());assertTrue(c.incoming().isNull());assertEquals("local",c.current().get("name").asString());
    }
    @Test void additionsAndIdenticalEditsDoNotInventConflicts(){
        var n=node(a,"added","");var added=ModuleUpdate.compare(doc(),doc(),doc(n));assertEquals(1,added.size());assertFalse(added.getFirst().conflict());assertTrue(added.getFirst().baseline().isNull());
        var same=ModuleUpdate.compare(doc(node(a,"old","")),doc(n),doc(n));assertEquals(1,same.size());assertFalse(same.getFirst().conflict());
        var competing=ModuleUpdate.compare(doc(),doc(n),doc(node(a,"other","")));assertTrue(competing.getFirst().conflict());
    }
    @Test void typedSceneChangesUseStableNodeIdAndNullableMapAsOneValue(){
        var n=new ModuleArchive.Node(a,"SCENE","scene","","DM",List.of(),List.of(),List.of());
        var base=new ModuleBlueprint.Document(List.of(n),new ModuleBlueprint.Play(List.of(new ModuleBlueprint.Scene(a,20,15,48,null)),List.of(),List.of(),"REVIEW",List.of()));
        var next=new ModuleBlueprint.Document(List.of(n),new ModuleBlueprint.Play(List.of(new ModuleBlueprint.Scene(a,25,15,48,null)),List.of(),List.of(),"REVIEW",List.of()));
        var changes=ModuleUpdate.compare(base,base,next);assertEquals(1,changes.size());assertEquals("/play/scenes/"+a+"/width",changes.getFirst().path());assertFalse(changes.getFirst().conflict());
    }
}
