package io.dndroll.modules;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleUpdateTest {
    ModuleArchive.Verified verified(ModuleBlueprint.Document d,String rights,List<ModuleArchive.Asset> assets,List<ModuleArchive.Dependency> deps){return new ModuleArchive.Verified(new ModuleArchive.Manifest("dnd-roll.module",2,a,"1.0.0","test","5R",rights,deps,new ModuleArchive.Content(1,"a".repeat(64)),assets),d.nodes(),"a".repeat(64),d);}
    @Test void manifestChangesBindActualImageBytesAndRightsWithBothAssetClosures(){
        byte[] old={1,2},next={3,4};var oldAsset=new ModuleArchive.Asset(a,"image/png",2,ModuleArchive.hash(old));var newAsset=new ModuleArchive.Asset(a,"image/png",2,ModuleArchive.hash(next));
        var base=verified(doc(),"original",List.of(oldAsset),List.of());var incoming=verified(doc(),"changed rights",List.of(newAsset),List.of());
        var changes=ModuleUpdate.comparePackages(base,base,incoming);assertEquals(Set.of("/manifest/rights","/manifest/assets/"+a+"/sha256"),changes.stream().map(ModuleUpdate.Change::path).collect(java.util.stream.Collectors.toSet()));
        var choices=changes.stream().map(c->new ModuleUpdate.Selection(c.path(),"INCOMING")).toList();var prepared=ModuleUpdate.prepare(base,base,incoming,Map.of(a,old),Map.of(a,next),choices);
        assertEquals("changed rights",prepared.manifest().rights());assertArrayEquals(next,prepared.images().get(a));
        assertThrows(io.dndroll.domain.DomainException.class,()->ModuleUpdate.prepare(base,base,incoming,Map.of(a,old),Map.of(a,old),choices));
        var kept=ModuleUpdate.prepare(base,base,incoming,Map.of(a,old),Map.of(a,next),changes.stream().map(c->new ModuleUpdate.Selection(c.path(),"KEEP")).toList());assertArrayEquals(old,kept.images().get(a));
    }
    @Test void assembledDocumentCannotKeepDanglingNodeOrDependencyReferences(){
        var base=verified(doc(node(a,"one","")),"original",List.of(),List.of());
        var reference=new ModuleArchive.Node(b,"DOCUMENT","two","","DM",List.of(a),List.of(),List.of());var incoming=verified(doc(reference,node(a,"one","")),"original",List.of(),List.of());
        var local=verified(doc(),"original",List.of(),List.of());
        assertThrows(io.dndroll.domain.DomainException.class,()->ModuleUpdate.prepare(base,local,incoming,Map.of(),Map.of(),List.of(new ModuleUpdate.Selection("/nodes/"+a,"KEEP"),new ModuleUpdate.Selection("/nodes/"+b,"INCOMING"))));
    }
    @Test void explicitChoicesPreserveLocalFieldsAndResolveConflicts(){
        var base=doc(node(a,"base","base"));var local=doc(node(a,"local","local"));var next=doc(node(a,"base","upstream"));
        var selected=ModuleUpdate.select(base,local,next,List.of(new ModuleUpdate.Selection("/nodes/"+a+"/name","KEEP"),new ModuleUpdate.Selection("/nodes/"+a+"/text","INCOMING")));
        assertEquals("local",selected.nodes().getFirst().name());assertEquals("upstream",selected.nodes().getFirst().text());
    }
    @Test void choicesMustCoverEveryChangeExactlyOnceWithoutForgedPaths(){
        var base=doc(node(a,"old",""));var next=doc(node(a,"new",""));var s=new ModuleUpdate.Selection("/nodes/"+a+"/name","INCOMING");
        for(var choices:List.of(List.<ModuleUpdate.Selection>of(),List.of(s,s),List.of(new ModuleUpdate.Selection("/nodes/"+a+"/name","AUTO")),List.of(new ModuleUpdate.Selection("/actor","INCOMING"))))
            assertThrows(io.dndroll.domain.DomainException.class,()->ModuleUpdate.select(base,base,next,choices));
    }
    @Test void selectionsHandleStableIdentityAddDeleteAndNullableValues(){
        var base=doc(node(a,"one",""));var next=doc(node(b,"two",""));
        var selected=ModuleUpdate.select(base,base,next,List.of(new ModuleUpdate.Selection("/nodes/"+a,"INCOMING"),new ModuleUpdate.Selection("/nodes/"+b,"INCOMING")));
        assertEquals(next,selected);
    }
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
