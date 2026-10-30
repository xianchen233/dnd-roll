package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import io.dndroll.modules.ModuleArchive;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=a02_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ModuleLifecycleIT {
    @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;
    final JsonMapper json=JsonMapper.builder().build();final Set<UUID> campaigns=new HashSet<>();final Set<String> users=new HashSet<>();
    final String module="10000000-0000-4000-8000-000000000001";
    Map<String,Object> operationReview(Browser dm,String root,String decision)throws Exception{
        var instance=dm.read(root+"/module-instance");var source=instance.get("source");var copy=dm.read("/api/campaigns/"+source.get("campaignId").asString()+"/module-customizations/"+source.get("customizationId").asString());
        var options=tree(dm.post(root+"/module-instance/operation-options",Map.of("expectedVersion",instance.get("version").asLong(),"customizationVersion",copy.get("version").asLong())),200);var selections=new ArrayList<Map<String,Object>>();
        for(var o:options.get("options"))selections.add(Map.of("nodeId",o.get("nodeId").asString(),"choice",decision,"bindingVersion",o.get("binding").isNull()?0:o.get("binding").get("version").asLong(),"operationVersion",o.get("operation").isNull()?0:o.get("operation").get("version").asLong()));
        return Map.of("expectedVersion",options.get("version").asLong(),"customizationVersion",options.get("customizationVersion").asLong(),"campaignVersion",options.get("campaignVersion").asLong(),"selections",selections);
    }
    Map<String,Object> operationCommand(Browser dm,String root,String decision)throws Exception{var body=new LinkedHashMap<String,Object>(operationReview(dm,root,decision));var impact=tree(dm.post(root+"/module-instance/operation-migration-impact",body),200);assertTrue(impact.get("migration").get("blockers").isEmpty(),impact.toString());body.put("requestId",UUID.randomUUID());body.put("impactHash",impact.get("migration").get("impactHash").asString());body.put("confirmations",impact.get("migration").get("confirmations"));return body;}
    @Test void retiredBindingExactReceiptRemainsReadableWithoutReactivationButFreshBindAndRevokedReplayFail()throws Exception{
        var dm=dm();String root=bindingParty(dm);var instance=dm.read(root+"/module-instance");
        String node=instance.get("mapping").get("operations").properties().iterator().next().getKey();
        String route=root+"/module-operation-bindings/"+node+"/bind";
        var command=bindingCommand(dm,root,node);var ack=tree(dm.post(route,command),200);
        tree(dm.post(root+"/module-instance/operation-migrations",operationCommand(dm,root,"RETIRE")),200);
        var before=bindingRows();assertEquals(ack,tree(dm.post(route,command),200));assertEquals(before,bindingRows());
        var changed=new LinkedHashMap<String,Object>(command);changed.put("reason","different request body");
        tree(dm.post(route,changed),409);assertEquals(before,bindingRows());
        var fresh=new LinkedHashMap<String,Object>(command);fresh.put("requestId",UUID.randomUUID());
        tree(dm.post(route,fresh),404);assertEquals(before,bindingRows());
        assertTrue(dm.read(root+"/module-instance").get("mapping").get("operations").isEmpty());
        assertTrue(dm.read(root+"/module-operations").isEmpty());
        UUID library=UUID.fromString(instance.get("source").get("campaignId").asString());
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",library,dm.username);
        before=bindingRows();assertEquals(ack,tree(dm.post(route,command),200));assertEquals(before,bindingRows());
        UUID campaign=UUID.fromString(root.substring(root.lastIndexOf('/')+1));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",campaign,dm.username);
        before=bindingRows();tree(dm.post(route,command),403);assertEquals(before,bindingRows());
    }
    @Test void selectedIncomingReviewsLocalEditAndRequiresRebindingEvenWhenSourceItselfIsUnchanged()throws Exception{
        var dm=dm();String root=bindingParty(dm);String node=dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("binding").get("nodeId").asString();var bound=tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,root,node)),200);String op=bound.get("operationId").asString();var saved=dm.read(root+"/module-operations").get(0);
        tree(dm.post(root+"/module-operations/"+op,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","本团独立编辑","steps",saved.get("steps"))),200);
        var keep=operationCommand(dm,root,"KEEP");tree(dm.post(root+"/module-instance/operation-migrations",keep),200);assertEquals("本团独立编辑",dm.read(root+"/module-operations").get(0).get("name").asString());
        var adopt=operationCommand(dm,root,"INCOMING");tree(dm.post(root+"/module-instance/operation-migrations",adopt),200);assertEquals(0,dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("binding").get("boundVersion").asLong());
        tree(dm.post(root+"/module-operations/"+op+"/impact",Map.of("expectedVersion",2,"campaignVersion",2)),409);assertTrue(dm.read(root+"/module-operations").isEmpty());
        var rebound=tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,root,node)),200);assertEquals(op,rebound.get("operationId").asString());assertEquals(3,rebound.get("operationVersion").asLong());
    }
    @Test void selectedMigrationRejectsStaleLocalEditsStrictChoicesAndRevokedMembershipBeforeReplay()throws Exception{
        var dm=dm();String root=bindingParty(dm);String node=dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("binding").get("nodeId").asString();var bound=tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,root,node)),200);String op=bound.get("operationId").asString();
        var command=operationCommand(dm,root,"RETIRE");var current=dm.read(root+"/module-operations").get(0);tree(dm.post(root+"/module-operations/"+op,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","预览后的本团修改","steps",current.get("steps"))),200);var before=bindingRows();tree(dm.post(root+"/module-instance/operation-migrations",command),409);assertEquals(before,bindingRows());
        var review=operationReview(dm,root,"KEEP");var bad=new LinkedHashMap<String,Object>(review);bad.put("selections",List.of());tree(dm.post(root+"/module-instance/operation-migration-impact",bad),400);bad=new LinkedHashMap<>(review);bad.put("selections",List.of(((List<?>)review.get("selections")).getFirst(),((List<?>)review.get("selections")).getFirst()));tree(dm.post(root+"/module-instance/operation-migration-impact",bad),400);bad=new LinkedHashMap<>(review);bad.put("actor","forged");tree(dm.post(root+"/module-instance/operation-migration-impact",bad),400);assertEquals(before,bindingRows());
        var fresh=operationCommand(dm,root,"RETIRE");var missing=new LinkedHashMap<>(fresh);missing.put("confirmations",List.of());tree(dm.post(root+"/module-instance/operation-migrations",missing),409);assertEquals(before,bindingRows());
        var ack=tree(dm.post(root+"/module-instance/operation-migrations",fresh),200);var stable=bindingRows();assertEquals(ack,tree(dm.post(root+"/module-instance/operation-migrations",fresh),200));assertEquals(stable,bindingRows());var changed=new LinkedHashMap<>(fresh);changed.put("impactHash","b".repeat(64));tree(dm.post(root+"/module-instance/operation-migrations",changed),409);
        UUID campaign=UUID.fromString(root.substring(root.lastIndexOf('/')+1));var player=member(campaign,"PLAYER");var denied=player.post(root+"/module-instance/operation-options",Map.of("expectedVersion",1,"customizationVersion",1));tree(denied,403);assertTrue(denied.headers().firstValue("Cache-Control").orElse("").contains("private"));
        String recovery=dm.read(root+"/module-instance/migrations").get(0).get("recoveryCampaignId").asString();jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",UUID.fromString(recovery),dm.username);tree(dm.post(root+"/module-instance/operation-migrations",fresh),403);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",UUID.fromString(recovery),dm.username);
        var source=dm.read(root+"/module-instance").get("source");jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",UUID.fromString(source.get("campaignId").asString()),dm.username);tree(dm.post(root+"/module-instance/operation-migrations",fresh),403);
    }
    @Test void selectedMigrationImmediateDeferredRollbackConcurrentRetryAndSameVersionRaceAreAtomic()throws Exception{
        var dm=dm();String root=bindingParty(dm);var body=operationCommand(dm,root,"RETIRE");var before=bindingRows();var beforeFiles=files();String fn="selected_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'injected selected migration failure'; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(root+"/module-instance/operation-migrations",body),500);assertEquals(before,bindingRows());assertEquals(beforeFiles,files());jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");}}finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->dm.post(root+"/module-instance/operation-migrations",body));var b=pool.submit(()->dm.post(root+"/module-instance/operation-migrations",body));assertEquals(tree(a.get(),200),tree(b.get(),200));}assertEquals(1,dm.read(root+"/module-instance/migrations").size());
        var next=operationCommand(dm,root,"INCOMING");var other=member(UUID.fromString(root.substring(root.lastIndexOf('/')+1)),"DM");var source=dm.read(root+"/module-instance").get("source");jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'DM')",UUID.fromString(source.get("campaignId").asString()),other.username);var rival=new LinkedHashMap<>(next);rival.put("requestId",UUID.randomUUID());
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->dm.post(root+"/module-instance/operation-migrations",next));var b=pool.submit(()->other.post(root+"/module-instance/operation-migrations",rival));assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get().statusCode(),b.get().statusCode()).sorted().toList());}
    }
    @Test void selectedSourceDeletionKeepRetireAndReintroductionRetainIdentityWithoutResurrection()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);var d=bindingDocument();edit(dm,library,copy,0,d);String root="/api/campaigns/"+tree(dm.post(copies(library)+"/"+copy+"/campaigns",fresh(1,"来源删除")),200).get("id").asString();String node=d.get("operations").get("definitions").get(0).get("nodeId").asString(),op=dm.read(root+"/module-instance").get("mapping").get("operations").get(node).asString();
        var removed=d.deepCopy();((tools.jackson.databind.node.ArrayNode)removed.get("operations").get("definitions")).removeAll();edit(dm,library,copy,1,removed);tree(dm.post(root+"/module-instance/operation-migrations",operationCommand(dm,root,"KEEP")),200);assertEquals(op,dm.read(root+"/module-instance").get("mapping").get("operations").get(node).asString());
        tree(dm.post(root+"/module-instance/operation-migration-impact",operationReview(dm,root,"INCOMING")),400);tree(dm.post(root+"/module-instance/operation-migrations",operationCommand(dm,root,"RETIRE")),200);
        edit(dm,library,copy,2,d);tree(dm.post(root+"/module-instance/operation-migrations",operationCommand(dm,root,"KEEP")),200);assertTrue(dm.read(root+"/module-instance").get("mapping").get("operations").isEmpty());
        tree(dm.post(root+"/module-instance/operation-migrations",operationCommand(dm,root,"INCOMING")),200);assertEquals(op,dm.read(root+"/module-instance").get("mapping").get("operations").get(node).asString());assertEquals(0,dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("binding").get("boundVersion").asLong());
    }
    @Test void selectedNewSourcesAndStructuralTargetRemovalRequireValidAssembledChoices()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);var d=bindingDocument();edit(dm,library,copy,0,d);String root="/api/campaigns/"+tree(dm.post(copies(library)+"/"+copy+"/campaigns",fresh(1,"新来源与结构")),200).get("id").asString();String node=d.get("operations").get("definitions").get(0).get("nodeId").asString(),op=dm.read(root+"/module-instance").get("mapping").get("operations").get(node).asString();
        var next=d.deepCopy();((tools.jackson.databind.node.ArrayNode)next.get("play").get("placements")).removeAll();String added=UUID.randomUUID().toString();var trigger=(tools.jackson.databind.node.ObjectNode)next.get("nodes").get(next.get("nodes").size()-1).deepCopy();trigger.put("id",added);trigger.put("name","新增独立组合");((tools.jackson.databind.node.ArrayNode)next.get("nodes")).add(trigger);var definition=json.valueToTree(Map.of("nodeId",node,"steps",List.of(Map.of("kind","REVEAL_CHARACTER","unitId","10000000-0000-4000-8000-000000000002"))));((tools.jackson.databind.node.ArrayNode)next.get("operations").get("definitions")).removeAll().add(definition);var newDefinition=(tools.jackson.databind.node.ObjectNode)definition.deepCopy();newDefinition.put("nodeId",added);((tools.jackson.databind.node.ArrayNode)next.get("operations").get("definitions")).add(newDefinition);edit(dm,library,copy,1,next);
        var before=bindingRows();tree(dm.post(root+"/module-instance/operation-migration-impact",operationReview(dm,root,"KEEP")),400);assertEquals(before,bindingRows());tree(dm.post(root+"/module-instance/operation-migrations",operationCommand(dm,root,"INCOMING")),200);var instance=dm.read(root+"/module-instance");assertEquals(op,instance.get("mapping").get("operations").get(node).asString());assertNotEquals(op,instance.get("mapping").get("operations").get(added).asString());assertTrue(instance.get("mapping").get("placements").isEmpty());assertTrue(dm.read(root+"/module-operations").isEmpty());
        String recovery=dm.read(root+"/module-instance/migrations").get(0).get("recoveryCampaignId").asString();assertEquals(1,dm.read("/api/campaigns/"+recovery+"/module-instance").get("mapping").get("placements").size());
    }
    @Test void selectedRetiredIdentitiesContinueReservingCapacityBeforeAnyWrites()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);var d=bindingDocument();String first=d.get("operations").get("definitions").get(0).get("nodeId").asString();for(int i=1;i<100;i++){String node=UUID.randomUUID().toString();((tools.jackson.databind.node.ArrayNode)d.get("nodes")).add(json.valueToTree(Map.of("id",node,"kind","TRIGGER","name","容量组合 "+i,"text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of())));((tools.jackson.databind.node.ArrayNode)d.get("operations").get("definitions")).add(json.valueToTree(Map.of("nodeId",node,"steps",List.of(Map.of("kind","REVEAL_CHARACTER","unitId","10000000-0000-4000-8000-000000000002")))));}
        edit(dm,library,copy,0,d);String root="/api/campaigns/"+tree(dm.post(copies(library)+"/"+copy+"/campaigns",fresh(1,"保留容量")),200).get("id").asString();var changed=d.deepCopy();String added=UUID.randomUUID().toString();((tools.jackson.databind.node.ObjectNode)changed.get("operations").get("definitions").get(0)).put("nodeId",added);for(var n:changed.get("nodes"))if(n.get("id").asString().equals(first))((tools.jackson.databind.node.ObjectNode)n).put("id",added);edit(dm,library,copy,1,changed);
        var review=new LinkedHashMap<String,Object>(operationReview(dm,root,"INCOMING"));var choices=new ArrayList<Map<String,Object>>();for(var item:(List<?>)review.get("selections")){var choice=new LinkedHashMap<String,Object>((Map<String,Object>)item);if(choice.get("nodeId").equals(first))choice.put("choice","RETIRE");choices.add(choice);}review.put("selections",choices);var before=bindingRows();var impact=tree(dm.post(root+"/module-instance/operation-migration-impact",review),200);assertTrue(impact.get("migration").get("blockers").toString().contains("/operation-capacity"));var body=new LinkedHashMap<>(review);body.put("requestId",UUID.randomUUID());body.put("impactHash",impact.get("migration").get("impactHash").asString());body.put("confirmations",impact.get("migration").get("confirmations"));tree(dm.post(root+"/module-instance/operation-migrations",body),409);assertEquals(before,bindingRows());
    }
    @Test void selectedOperationMigrationKeepsIdentityAndRetirementPreservesHistoryWithIndependentUnboundRecovery()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);var d=bindingDocument();edit(dm,library,copy,0,d);
        String root="/api/campaigns/"+tree(dm.post(copies(library)+"/"+copy+"/campaigns",fresh(1,"选择迁移团")),200).get("id").asString();
        String node=d.get("operations").get("definitions").get(0).get("nodeId").asString();
        var bound=tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,root,node)),200);String op=bound.get("operationId").asString();
        var executionImpact=tree(dm.post(root+"/module-operations/"+op+"/impact",Map.of("expectedVersion",1,"campaignVersion",0)),200);var execution=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"campaignVersion",0,"impactHash",executionImpact.get("impactHash").asString());var executed=tree(dm.post(root+"/module-operations/"+op+"/executions",execution),200);
        var options=tree(dm.post(root+"/module-instance/operation-options",Map.of("expectedVersion",0,"customizationVersion",1)),200);
        var choice=Map.of("nodeId",node,"choice","RETIRE","bindingVersion",2,"operationVersion",1);
        var review=Map.of("expectedVersion",0,"customizationVersion",1,"campaignVersion",options.get("campaignVersion").asLong(),"selections",List.of(choice));
        var impact=tree(dm.post(root+"/module-instance/operation-migration-impact",review),200);assertTrue(impact.get("migration").get("blockers").isEmpty(),impact.toString());
        var body=new LinkedHashMap<String,Object>(review);body.put("requestId",UUID.randomUUID());body.put("impactHash",impact.get("migration").get("impactHash").asString());body.put("confirmations",impact.get("migration").get("confirmations"));
        var ack=tree(dm.post(root+"/module-instance/operation-migrations",body),200);assertEquals(ack,tree(dm.post(root+"/module-instance/operation-migrations",body),200));
        assertTrue(dm.read(root+"/module-instance").get("mapping").get("operations").isEmpty());assertEquals(op,dm.read(root+"/module-instance").get("mapping").get("retiredOperations").get(node).asString());
        tree(dm.post(root+"/module-operations/"+op+"/impact",Map.of("expectedVersion",1,"campaignVersion",0)),409);
        assertEquals(3,dm.read(root+"/module-operation-bindings/"+node+"/history").size());assertEquals(1,dm.read(root+"/module-operations/"+op+"/executions").size());assertEquals(executed,tree(dm.post(root+"/module-operations/"+op+"/executions",execution),200));
        String recovery=dm.read(root+"/module-instance/migrations").get(0).get("recoveryCampaignId").asString(),rr="/api/campaigns/"+recovery;
        var restored=dm.read(rr+"/module-operation-bindings").get("bindings").get(0).get("binding");assertNotEquals(op,restored.get("operationId").asString());assertEquals(0,restored.get("boundVersion").asLong());assertTrue(dm.read(rr+"/module-operations").isEmpty());
        tree(dm.post(rr+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,rr,node)),200);assertEquals("EMPTY",dm.read(rr+"/initiative").get("status").asString());assertEquals("IDLE",dm.read(rr+"/combat").get("status").asString());
    }
    tools.jackson.databind.node.ObjectNode bindingDocument(){
        var d=portable(playable());String trigger=d.get("operations").get("definitions").get(0).get("nodeId").asString();((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(2)).put("visibility","DM");
        ((tools.jackson.databind.node.ObjectNode)d.get("operations").get("definitions").get(0)).set("steps",json.valueToTree(List.of(Map.of("kind","REVEAL_TOKEN","placementId","10000000-0000-4000-8000-000000000004"),Map.of("kind","REVEAL_CHARACTER","unitId","10000000-0000-4000-8000-000000000002"),Map.of("kind","PUBLISH_TEXT","documentId","10000000-0000-4000-8000-000000000003"))));return d;
    }
    @Test void bindingLaunchReservesIndependentIdentitiesAndOnlyExplicitCurrentPreviewCanSaveExecutableSteps()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;var d=bindingDocument();edit(dm,c,id,0,d);String a=tree(dm.post(path+"/campaigns",fresh(1,"绑定团A")),200).get("id").asString(),b=tree(dm.post(path+"/campaigns",fresh(1,"绑定团B")),200).get("id").asString();String root="/api/campaigns/"+a,bindings=root+"/module-operation-bindings";
        var listing=dm.read(bindings);var binding=listing.get("bindings").get(0).get("binding");String node=binding.get("nodeId").asString(),operation=binding.get("operationId").asString();assertNotEquals(operation,dm.read("/api/campaigns/"+b+"/module-operation-bindings").get("bindings").get(0).get("binding").get("operationId").asString());assertEquals(operation,dm.read(instance(a)).get("mapping").get("operations").get(node).asString());assertTrue(dm.read(root+"/module-operations").isEmpty());
        String card=dm.read(instance(a)).get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();tree(dm.post(root+"/characters/"+card+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-2,"reason","绑定前实际变化")),200);
        var before=dm.read(root+"/characters/"+card);var review=Map.of("expectedVersion",1,"operationVersion",0,"instanceVersion",0,"campaignVersion",listing.get("campaignVersion").asLong());var preview=tree(dm.post(bindings+"/"+node+"/preview",review),200);assertEquals(1,preview.get("candidate").get("steps").get(1).get("expectedVersion").asLong());assertTrue(dm.read(root+"/module-operations").isEmpty());
        var command=new LinkedHashMap<String,Object>(review);command.put("requestId",UUID.randomUUID());command.put("impactHash",preview.get("impactHash").asString());command.put("reason","已核对本团当前目标，保存但不执行");var ack=tree(dm.post(bindings+"/"+node+"/bind",command),200);assertEquals(ack,tree(dm.post(bindings+"/"+node+"/bind",command),200));assertEquals(before,dm.read(root+"/characters/"+card));assertEquals(1,dm.read(root+"/module-operations").size());assertTrue(dm.read(root+"/module-operations/"+operation+"/executions").isEmpty());
        var player=member(UUID.fromString(a),"PLAYER");var denied=player.get(bindings);tree(denied,403);assertEquals("no-store, private",denied.headers().firstValue("Cache-Control").orElse(""));assertEquals("nosniff",denied.headers().firstValue("X-Content-Type-Options").orElse(""));tree(player.post(bindings+"/"+node+"/bind",command),403);jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",UUID.fromString(a),dm.username);tree(dm.post(bindings+"/"+node+"/bind",command),403);
    }
    String bindingParty(Browser dm)throws Exception{UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,bindingDocument());return "/api/campaigns/"+tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"独立绑定团")),200).get("id").asString();}
    Map<String,Object> bindingReview(Browser dm,String root,String node)throws Exception{var listing=dm.read(root+"/module-operation-bindings");for(var item:listing.get("bindings"))if(item.get("binding").get("nodeId").asString().equals(node))return Map.of("expectedVersion",item.get("binding").get("version").asLong(),"operationVersion",item.get("operation").isNull()?0:item.get("operation").get("version").asLong(),"instanceVersion",listing.get("instanceVersion").asLong(),"campaignVersion",listing.get("campaignVersion").asLong());throw new AssertionError("Missing binding");}
    Map<String,Object> bindingCommand(Browser dm,String root,String node)throws Exception{var body=new LinkedHashMap<String,Object>(bindingReview(dm,root,node));var preview=tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",body),200);body.put("requestId",UUID.randomUUID());body.put("impactHash",preview.get("impactHash").asString());body.put("reason","明确核对当前本团版本");return body;}
    Map<String,Object> bindingRows(){var rows=new TreeMap<String,Object>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=current_schema() and tablename<>'flyway_schema_history' order by tablename",String.class)){if(!table.matches("[a-z_]+"))throw new AssertionError();rows.put(table,jdbc.queryForList("select to_jsonb(t)::text data from "+table+" t order by to_jsonb(t)::text"));}return rows;}
    @Test void bindingStaleTargetsInstanceVersionsStrictBodiesAndReservedIdentitiesCannotBypassReview()throws Exception{
        var dm=dm();String root=bindingParty(dm);var item=dm.read(root+"/module-operation-bindings").get("bindings").get(0);String node=item.get("binding").get("nodeId").asString(),op=item.get("binding").get("operationId").asString();var command=bindingCommand(dm,root,node);var candidate=tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",bindingReview(dm,root,node)),200).get("candidate");
        var forged=new LinkedHashMap<String,Object>();for(var e:candidate.properties())forged.put(e.getKey(),e.getValue());forged.put("requestId",UUID.randomUUID());tree(dm.post(root+"/module-operations/"+op,forged),409);assertTrue(dm.read(root+"/module-operations").isEmpty());
        String card=dm.read(root+"/module-instance").get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();tree(dm.post(root+"/characters/"+card+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-1,"reason","预览后发生变化")),200);var before=bindingRows();tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",command),409);assertEquals(before,bindingRows());
        var bad=new LinkedHashMap<>(bindingReview(dm,root,node));bad.put("instanceVersion",99);tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",bad),409);bad=new LinkedHashMap<>(command);bad.put("steps",List.of());tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bad),400);assertEquals(before,bindingRows());
        UUID foreign=campaign(dm);tree(dm.get("/api/campaigns/"+foreign+"/module-operation-bindings"),404);tree(dm.post(root+"/module-operation-bindings/"+UUID.randomUUID()+"/preview",bindingReview(dm,root,node)),404);
        var noCsrf=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+root+"/module-operation-bindings/"+node+"/preview")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(bindingReview(dm,root,node)))).build(),HttpResponse.BodyHandlers.ofString());tree(noCsrf,403);
    }
    @Test void bindingImmediateDeferredFailureConcurrentRetryAndLaterManualEditRemainAtomic()throws Exception{
        var dm=dm();String root=bindingParty(dm);String node=dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("binding").get("nodeId").asString();var body=bindingCommand(dm,root,node);var before=bindingRows();String fn="binding_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'injected binding failure'; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on module_operation_binding_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",body),500);assertEquals(before,bindingRows());jdbc.execute("drop trigger "+fn+" on module_operation_binding_receipt");}}finally{jdbc.execute("drop trigger if exists "+fn+" on module_operation_binding_receipt");jdbc.execute("drop function "+fn+"()");}
        JsonNode ack;try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->dm.post(root+"/module-operation-bindings/"+node+"/bind",body));var b=pool.submit(()->dm.post(root+"/module-operation-bindings/"+node+"/bind",body));ack=tree(a.get(),200);assertEquals(ack,tree(b.get(),200));}
        String op=ack.get("operationId").asString();var current=dm.read(root+"/module-operations").get(0);tree(dm.post(root+"/module-operations/"+op,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","本团手动编辑保持","steps",current.get("steps"))),200);before=bindingRows();assertEquals(ack,tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",body),200));assertEquals(before,bindingRows());assertTrue(dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("locallyModified").asBoolean());assertEquals(2,dm.read(root+"/module-operation-bindings/"+node+"/history").size());
        var changed=new LinkedHashMap<>(body);changed.put("reason","changed same key");tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",changed),409);
        var fresh=bindingCommand(dm,root,node);var rival=new LinkedHashMap<>(fresh);rival.put("requestId",UUID.randomUUID());var other=member(UUID.fromString(root.substring(root.lastIndexOf('/')+1)),"DM");try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->dm.post(root+"/module-operation-bindings/"+node+"/bind",fresh));var b=pool.submit(()->other.post(root+"/module-operation-bindings/"+node+"/bind",rival));assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get().statusCode(),b.get().statusCode()).sorted().toList());}
    }
    @Test void bindingPrepareAndStartRequireIndependentConfirmationAndNeverExecuteOnSave()throws Exception{
        var dm=dm();UUID c=campaign(dm);String copy=copy(dm,c);var d=bindingDocument();var definitions=(tools.jackson.databind.node.ArrayNode)d.get("operations").get("definitions");var steps=(tools.jackson.databind.node.ArrayNode)definitions.get(0).get("steps");steps.add(json.valueToTree(Map.of("kind","PREPARE_ENCOUNTER","participants",List.of(Map.of("unitId","10000000-0000-4000-8000-000000000002","kind","MONSTER","group","","surprised",false,"advantage",List.of(),"disadvantage",List.of())),"reason","明确准备")));String start=UUID.randomUUID().toString();((tools.jackson.databind.node.ArrayNode)d.get("nodes")).add(json.valueToTree(Map.of("id",start,"kind","TRIGGER","name","单独确认后开始","text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of())));definitions.add(json.valueToTree(Map.of("nodeId",start,"steps",List.of(Map.of("kind","START_ENCOUNTER","reason","仅本团确认版本")))));edit(dm,c,copy,0,d);String root="/api/campaigns/"+tree(dm.post(copies(c)+"/"+copy+"/campaigns",fresh(1,"遭遇绑定")),200).get("id").asString();String prepare=definitions.get(0).get("nodeId").asString();
        tree(dm.post(root+"/module-operation-bindings/"+start+"/preview",bindingReview(dm,root,start)),409);var bound=tree(dm.post(root+"/module-operation-bindings/"+prepare+"/bind",bindingCommand(dm,root,prepare)),200);assertEquals("EMPTY",dm.read(root+"/initiative").get("status").asString());assertEquals("IDLE",dm.read(root+"/combat").get("status").asString());String path=root+"/module-operations/"+bound.get("operationId").asString();var impact=tree(dm.post(path+"/impact",Map.of("expectedVersion",1,"campaignVersion",0)),200);tree(dm.post(path+"/executions",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"campaignVersion",0,"impactHash",impact.get("impactHash").asString())),200);assertEquals("DRAFT",dm.read(root+"/initiative").get("status").asString());tree(dm.post(root+"/module-operation-bindings/"+start+"/preview",bindingReview(dm,root,start)),409);
        String card=dm.read(root+"/initiative").get("entries").get(0).get("characterId").asString();tree(dm.post(root+"/initiative/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",List.of(card),"reason","明确确认次序")),200);var preview=tree(dm.post(root+"/module-operation-bindings/"+start+"/preview",bindingReview(dm,root,start)),200);assertEquals(2,preview.get("candidate").get("steps").get(0).get("initiativeVersion").asLong());tree(dm.post(root+"/module-operation-bindings/"+start+"/bind",bindingCommand(dm,root,start)),200);assertEquals("IDLE",dm.read(root+"/combat").get("status").asString());
    }
    tools.jackson.databind.node.ObjectNode portable(JsonNode original){
        var d=(tools.jackson.databind.node.ObjectNode)original.deepCopy();String trigger=UUID.randomUUID().toString();((tools.jackson.databind.node.ArrayNode)d.get("nodes")).add(json.valueToTree(Map.of("id",trigger,"kind","TRIGGER","name","便携开始意图","text","不会自动执行","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of())));
        d.set("operations",json.valueToTree(Map.of("formatVersion",1,"definitions",List.of(Map.of("nodeId",trigger,"steps",List.of(Map.of("kind","START_ENCOUNTER","reason","实例绑定时另行审阅")))))));return d;
    }
    @Test void bindingMissingRelinkedRetiredTargetsBlockWithoutNameFallbackAndSourceRevocationDoesNotRevokeTarget()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);edit(dm,library,copy,0,bindingDocument());String live=tree(dm.post(copies(library)+"/"+copy+"/campaigns",fresh(1,"本团独立权限")),200).get("id").asString(),root="/api/campaigns/"+live;var instance=dm.read(root+"/module-instance");String node=dm.read(root+"/module-operation-bindings").get("bindings").get(0).get("binding").get("nodeId").asString();UUID token=UUID.fromString(instance.get("mapping").get("placements").get("10000000-0000-4000-8000-000000000004").asString()),card=UUID.fromString(instance.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString());
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",library,dm.username);var body=bindingReview(dm,root,node);tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",body),200);
        jdbc.update("update token set character_id=null where id=?",token);var before=bindingRows();tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",body),409);assertEquals(before,bindingRows());jdbc.update("update token set character_id=? where id=?",card,token);
        jdbc.update("update token set module_retired=true where id=?",token);before=bindingRows();tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",body),409);assertEquals(before,bindingRows());jdbc.update("update token set module_retired=false where id=?",token);
        var mapping=(tools.jackson.databind.node.ObjectNode)instance.get("mapping").deepCopy();((tools.jackson.databind.node.ObjectNode)mapping.get("units")).remove("10000000-0000-4000-8000-000000000002");jdbc.update("update module_instance set mapping=?::jsonb where campaign_id=?",json.writeValueAsString(mapping),UUID.fromString(live));before=bindingRows();tree(dm.post(root+"/module-operation-bindings/"+node+"/preview",body),409);assertEquals(before,bindingRows());jdbc.update("update module_instance set mapping=?::jsonb where campaign_id=?",json.writeValueAsString(instance.get("mapping")),UUID.fromString(live));
        tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,root,node)),200);assertTrue(dm.read(root+"/module-operations/"+instance.get("mapping").get("operations").get(node).asString()+"/executions").isEmpty());
    }
    @Test void bindingReservationCapacityAndLaunchReceiptFailuresRemainAtomic()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);var d=bindingDocument();var defs=(tools.jackson.databind.node.ArrayNode)d.get("operations").get("definitions");var nodes=(tools.jackson.databind.node.ArrayNode)d.get("nodes");for(int i=1;i<100;i++){String node=UUID.randomUUID().toString();nodes.add(json.valueToTree(Map.of("id",node,"kind","TRIGGER","name","额外组合 "+i,"text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of())));defs.add(json.valueToTree(Map.of("nodeId",node,"steps",List.of(Map.of("kind","REVEAL_CHARACTER","unitId","10000000-0000-4000-8000-000000000002")))));}edit(dm,library,copy,0,d);
        var body=fresh(1,"100个待绑定身份");String path=copies(library)+"/"+copy+"/campaigns",fn="binding_launch_"+UUID.randomUUID().toString().replace("-","");var before=bindingRows();var beforeFiles=files();jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.result_kind='CAMPAIGN' then raise exception 'injected launch failure'; end if; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(path,body),500);assertEquals(before,bindingRows());assertEquals(beforeFiles,files());jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");}}finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        var ack=tree(dm.post(path,body),200);assertEquals(ack,tree(dm.post(path,body),200));String root="/api/campaigns/"+ack.get("id").asString();assertEquals(100,dm.read(root+"/module-operation-bindings").get("bindings").size());String node=defs.get(0).get("nodeId").asString();tree(dm.post(root+"/module-operation-bindings/"+node+"/bind",bindingCommand(dm,root,node)),200);assertEquals(1,dm.read(root+"/module-operations").size());var current=dm.read(root+"/module-operations").get(0);before=bindingRows();tree(dm.post(root+"/module-operations/"+UUID.randomUUID(),Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","超过100个身份","steps",current.get("steps"))),400);assertEquals(before,bindingRows());
    }
    @Test void bindingChecksCompleteRuntimeSaveEnvelopeAfterStableReferenceExpansion()throws Exception{
        var dm=dm();UUID library=campaign(dm);String copy=copy(dm,library);var d=bindingDocument();var defs=(tools.jackson.databind.node.ArrayNode)d.get("operations").get("definitions");String trigger=defs.get(0).get("nodeId").asString();for(var node:d.get("nodes"))if(node.get("id").asString().equals(trigger))((tools.jackson.databind.node.ObjectNode)node).put("name","绑".repeat(100));
        var units=(tools.jackson.databind.node.ArrayNode)d.get("play").get("units");var people=new ArrayList<Map<String,Object>>();for(int i=0;i<20;i++){String unit;if(i==0)unit=units.get(0).get("nodeId").asString();else{unit=UUID.randomUUID().toString();var u=(tools.jackson.databind.node.ObjectNode)units.get(0).deepCopy();u.put("nodeId",unit);units.add(u);((tools.jackson.databind.node.ArrayNode)d.get("nodes")).add(json.valueToTree(Map.of("id",unit,"kind","UNIT","name","有界参战者 "+i,"text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of())));}people.add(new LinkedHashMap<>(Map.of("unitId",unit,"kind","MONSTER","group","","surprised",false,"advantage",Collections.nCopies(10,"x".repeat(200)),"disadvantage",List.of())));}
        var step=new LinkedHashMap<String,Object>(Map.of("kind","PREPARE_ENCOUNTER","participants",people,"reason","明确准备"));int last=0;for(int n=1;n<=200;n++){for(var p:people)p.put("disadvantage",Collections.nCopies(10,"y".repeat(n)));((tools.jackson.databind.node.ObjectNode)defs.get(0)).set("steps",json.valueToTree(List.of(step)));if(json.writeValueAsBytes(defs.get(0)).length>65536)break;last=n;}assertTrue(last>0);for(var p:people)p.put("disadvantage",Collections.nCopies(10,"y".repeat(last)));((tools.jackson.databind.node.ObjectNode)defs.get(0)).set("steps",json.valueToTree(List.of(step)));assertTrue(json.writeValueAsBytes(defs.get(0)).length<=65536);edit(dm,library,copy,0,d);
        String root="/api/campaigns/"+tree(dm.post(copies(library)+"/"+copy+"/campaigns",fresh(1,"完整保存封套边界")),200).get("id").asString();var before=bindingRows();tree(dm.post(root+"/module-operation-bindings/"+trigger+"/preview",bindingReview(dm,root,trigger)),400);assertEquals(before,bindingRows());assertTrue(dm.read(root+"/module-operations").isEmpty());assertEquals("EMPTY",dm.read(root+"/initiative").get("status").asString());
    }
    @Test void portableDefinitionRoundtripKeepsOriginalBaselineAndReservesUnboundLaunch()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;var before=dm.read(path);var d=portable(before.get("document"));var edit=new LinkedHashMap<String,Object>(fresh(0,"便携定制"));edit.put("document",d);
        var ack=tree(dm.post(path,edit),200);assertEquals(ack,tree(dm.post(path,edit),200));assertEquals(d,dm.read(path).get("document"));assertEquals(before.get("baseline"),dm.read(path).get("baseline"));
        int campaignsBefore=jdbc.queryForObject("select count(*) from campaign",Integer.class);var filesBefore=files();String launched=tree(dm.post(path+"/campaigns",fresh(1,"便携身份保持未绑定")),200).get("id").asString();assertEquals(campaignsBefore+1,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(filesBefore,files());assertEquals(0,dm.read("/api/campaigns/"+launched+"/module-operations").size());assertEquals(0,dm.read("/api/campaigns/"+launched+"/module-operation-bindings").get("bindings").get(0).get("binding").get("boundVersion").asLong());
        var save=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"moduleId",UUID.randomUUID(),"moduleVersion","1.0.0","name","便携往返模板");var saved=tree(dm.post(path+"/templates",save),200);assertEquals(saved,tree(dm.post(path+"/templates",save),200));
        var response=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+"/"+saved.get("id").asString()+"/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,response.statusCode());var exported=new ModuleArchive().verify(response.body());assertEquals(3,exported.manifest().formatVersion());assertEquals(d,json.valueToTree(exported.document()));
        long cv=dm.read(copies(c)).get("campaignVersion").asLong();assertEquals(saved.get("id"),tree(upload(dm,c,command(cv,false,false),response.body()),200).get("id"));
        var copy=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",cv,"packageId",saved.get("id").asString(),"name","第二独立便携副本")),200);assertEquals(d,dm.read(copies(c)+"/"+copy.get("id").asString()).get("baseline"));
        var player=member(c,"PLAYER");tree(player.get(path),403);tree(player.post(path,edit),403);jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path,edit),403);tree(dm.post(path+"/templates",save),403);
    }
    @Test void portableDefinitionCannotPassExistingTextOrStructuralLiveGate()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;String live=tree(dm.post(path+"/campaigns",fresh(0,"旧实况保持")),200).get("id").asString();var before=dm.read(instance(live));var d=portable(dm.read(path).get("document"));var edit=new LinkedHashMap<String,Object>(fresh(0,"准备新便携定义"));edit.put("document",d);tree(dm.post(path,edit),200);
        for(String gate:List.of("update-impact","migration-impact")){var impact=tree(dm.post(instance(live)+"/"+gate,Map.of("expectedVersion",0,"customizationVersion",1)),200);assertTrue(impact.get("blockers").toString().contains("/portable-operations"));var apply=new LinkedHashMap<String,Object>();apply.put("requestId",UUID.randomUUID());apply.put("expectedVersion",0);apply.put("customizationVersion",1);apply.put("impactHash",impact.get("impactHash").asString());if(gate.equals("migration-impact"))apply.put("confirmations",impact.get("confirmations"));tree(dm.post(instance(live)+"/"+(gate.equals("migration-impact")?"migrations":"updates"),apply),409);}assertEquals(before,dm.read(instance(live)));
    }
    Map<String,Object> portableRows(){var rows=new TreeMap<String,Object>();for(String table:List.of("campaign","module_package","module_customization","module_customization_archive","module_instance","module_lifecycle_receipt","module_command_receipt","module_operation","module_operation_execution"))rows.put(table,jdbc.queryForList("select to_jsonb(t)::text data from "+table+" t order by to_jsonb(t)::text"));return rows;}
    @Test void portableDefinitionRejectsRuntimeAndDanglingFieldsWithoutPartialChanges()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;var d=portable(dm.read(path).get("document"));var before=portableRows();var beforeFiles=files();
        for(String field:List.of("expectedVersion","initiativeVersion","requestId","actor","tokenId")){var invalid=d.deepCopy();((tools.jackson.databind.node.ObjectNode)invalid.get("operations").get("definitions").get(0).get("steps").get(0)).put(field,"forged");var body=new LinkedHashMap<String,Object>(fresh(0,"拒绝运行时字段"));body.put("document",invalid);tree(dm.post(path,body),400);assertEquals(before,portableRows());assertEquals(beforeFiles,files());}
        var invalid=d.deepCopy();((tools.jackson.databind.node.ObjectNode)invalid.get("operations").get("definitions").get(0)).put("nodeId",UUID.randomUUID().toString());var body=new LinkedHashMap<String,Object>(fresh(0,"拒绝悬空引用"));body.put("document",invalid);tree(dm.post(path,body),400);assertEquals(before,portableRows());
        byte[] content=json.writeValueAsBytes(invalid);var m=new ModuleArchive().verify(archive()).manifest();var manifest=new ModuleArchive.Manifest(m.format(),3,m.moduleId(),"9.0.0",m.name(),m.rules(),m.rights(),m.dependencies(),new ModuleArchive.Content(content.length,ModuleArchive.hash(content)),m.assets());var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){for(var e:Map.of("manifest.json",json.writeValueAsBytes(manifest),"content.json",content).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}
        tree(upload(dm,c,command(dm.read(copies(c)).get("campaignVersion").asLong(),false,false),bytes.toByteArray()),400);assertEquals(before,portableRows());assertEquals(beforeFiles,files());
    }
    @Test void portableDefinitionEditRollbackAndConcurrentRetriesPreserveSingleVersions()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;var d=portable(dm.read(path).get("document"));var body=new LinkedHashMap<String,Object>(fresh(0,"原子便携修改"));body.put("document",d);var before=portableRows();String fn="a03g_fail_"+c.toString().replace("-","");
        jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.campaign_id='"+c+"'::uuid and new.result_kind='EDIT' then raise exception 'injected portable edit failure'; end if; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(path,body),500);assertEquals(before,portableRows());jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");}}finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        var other=member(c,"DM");try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->dm.post(path,body));var b=pool.submit(()->dm.post(path,body));assertEquals(tree(a.get(),200),tree(b.get(),200));var next=new LinkedHashMap<String,Object>(fresh(1,"下一版"));next.put("document",d);var rival=new LinkedHashMap<>(next);rival.put("requestId",UUID.randomUUID());var one=pool.submit(()->dm.post(path,next));var two=pool.submit(()->other.post(path,rival));assertEquals(List.of(200,409),java.util.stream.Stream.of(one.get().statusCode(),two.get().statusCode()).sorted().toList());}
        assertEquals(2,dm.read(path).get("version").asLong());assertEquals(d,dm.read(path).get("document"));assertEquals(2,jdbc.queryForObject("select count(*) from module_lifecycle_receipt where campaign_id=? and result_kind='EDIT'",Integer.class,c));
    }
    @Test void portableDefinitionSelectiveUpdateAndRecoveryRetainVersionThreeAndExactSteps()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;var d=portable(dm.read(path).get("document"));edit(dm,c,id,0,d);UUID identity=UUID.randomUUID();var save=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"moduleId",identity,"moduleVersion","1.0.0","name","便携基线");var pkg=tree(dm.post(path+"/templates",save),200);
        String other=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",pkg.get("version").asLong(),"packageId",pkg.get("id").asString(),"name","便携独立副本")),200).get("id").asString(),otherPath=copies(c)+"/"+other;
        var local=d.deepCopy();((tools.jackson.databind.node.ObjectNode)local.get("operations").get("definitions").get(0).get("steps").get(0)).put("reason","本地定制理由");edit(dm,c,other,0,local);
        var incoming=d.deepCopy();((tools.jackson.databind.node.ObjectNode)incoming.get("operations").get("definitions").get(0).get("steps").get(0)).put("reason","新模板理由");edit(dm,c,id,1,incoming);var next=tree(dm.post(path+"/templates",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"moduleId",identity,"moduleVersion","2.0.0","name","便携更新")),200);
        var options=tree(dm.post(otherPath+"/update-options",Map.of("expectedVersion",1,"packageId",next.get("id").asString())),200);assertEquals(1,options.get("changes").size());assertTrue(options.get("changes").get(0).get("conflict").asBoolean());String change=options.get("changes").get(0).get("path").asString();assertTrue(change.startsWith("/operations/definitions/"));
        var review=Map.of("expectedVersion",1,"packageId",next.get("id").asString(),"selections",List.of(Map.of("path",change,"choice","INCOMING")));var plan=tree(dm.post(otherPath+"/update-impact",review),200);var apply=new LinkedHashMap<String,Object>(review);apply.put("requestId",UUID.randomUUID());apply.put("sha256",plan.get("sha256").asString());apply.put("impactHash",plan.get("impactHash").asString());var ack=tree(dm.post(otherPath+"/updates",apply),200);assertEquals(ack,tree(dm.post(otherPath+"/updates",apply),200));assertEquals(incoming,dm.read(otherPath).get("document"));
        var response=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+otherPath+"/updates/2/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,response.statusCode());var restored=new ModuleArchive().verify(response.body());assertEquals(3,restored.manifest().formatVersion());assertEquals(local,json.valueToTree(restored.document()));
    }
    static final String schema="a02_http_"+UUID.randomUUID().toString().replace("-","");
    static final String root=System.getProperty("java.io.tmpdir")+"/"+schema;
    static final org.springframework.jdbc.datasource.DriverManagerDataSource admin=new org.springframework.jdbc.datasource.DriverManagerDataSource(FirstSliceIT.required("TEST_DATABASE_URL"),FirstSliceIT.required("TEST_DATABASE_USERNAME"),FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){new JdbcTemplate(admin).execute("create schema "+schema);String url=FirstSliceIT.required("TEST_DATABASE_URL");p.add("spring.datasource.url",()->url+(url.contains("?")?"&":"?")+"currentSchema="+schema);p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));p.add("assets.storage-root",()->root);}
    @AfterAll static void removeSchema(){new JdbcTemplate(admin).execute("drop schema "+schema+" cascade");}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf,username;
        Browser()throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
        void login(String name)throws Exception{username=name;tree(post("/api/auth/login",Map.of("username",name,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return json.readTree(r.body());}
    Browser dm()throws Exception{var b=new Browser();b.login("a02_owner");return b;}
    UUID campaign(Browser dm)throws Exception{UUID c=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Module import test")),201).get("id").asString());campaigns.add(c);return c;}
    Browser member(UUID c,String role)throws Exception{String name="a02_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(name);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),name,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?)",c,name,role);var b=new Browser();b.login(name);return b;}
    String base(UUID c){return "/api/campaigns/"+c+"/module-packages";}
    Map<String,Object> command(long version,boolean copy,boolean legacy){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"createCopy",copy,"confirmLegacy",legacy);}
    byte[] archive(String version,String rules,List<?> dependencies,String name)throws Exception{
        byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of(Map.of("id",module,"kind","DOCUMENT","name","Secret preparation","text","<script>inert</script>","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()))));
        var manifest=Map.of("format","dnd-roll.module","formatVersion",1,"moduleId",module,"moduleVersion",version,"name",name,"rules",rules,"rights","Synthetic original fixture","dependencies",dependencies,"content",Map.of("size",content.length,"sha256",ModuleArchive.hash(content)),"assets",List.of());
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){for(var e:Map.of("manifest.json",json.writeValueAsBytes(manifest),"content.json",content).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}return bytes.toByteArray();
    }
    byte[] archive()throws Exception{return archive("1.0.0","5R",List.of(),"Synthetic module");}
    HttpResponse<String> upload(Browser b,UUID c,String suffix,Object command,byte[] bytes,boolean csrf,String extra)throws Exception{
        String boundary="a01-"+UUID.randomUUID();var out=new ByteArrayOutputStream();
        if(command!=null)out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"command\"\r\nContent-Type: application/json\r\n\r\n"+json.writeValueAsString(command)+"\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"../../ignored.zip\"\r\nContent-Type: application/zip\r\n\r\n").getBytes());out.write(bytes);out.write("\r\n".getBytes());
        if(!extra.isEmpty())out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+extra+"\"\r\n\r\nextra\r\n").getBytes());
        out.write(("--"+boundary+"--\r\n").getBytes());var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+suffix)).header("Content-Type","multipart/form-data; boundary="+boundary);if(csrf)request.header("X-CSRF-TOKEN",b.csrf);
        return b.client.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> upload(Browser b,UUID c,Object command,byte[] bytes)throws Exception{return upload(b,c,"",command,bytes,true,"");}
    String copies(UUID c){return "/api/campaigns/"+c+"/module-customizations";}
    String instance(String c){return "/api/campaigns/"+c+"/module-instance";}
    String copy(Browser dm,UUID c)throws Exception{
        String pkg=tree(upload(dm,c,command(0,false,false),archive()),200).get("id").asString();
        return tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"packageId",pkg,"name","定制副本")),200).get("id").asString();
    }
    Map<String,Object> fresh(long version,String name){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"name",name);}
    String candidate(Browser dm,UUID c,String id)throws Exception{
        var detail=dm.read(copies(c)+"/"+id);
        var d=(tools.jackson.databind.node.ObjectNode)detail.get("baseline").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(0)).put("text","upstream revision");
        var archive=new ModuleArchive();var original=archive.verify(archive());
        byte[] bytes=archive.export(original.manifest(),json.treeToValue(d,io.dndroll.modules.ModuleBlueprint.Document.class),Map.of(),module,"2.0.0","Next template");
        long version=dm.read(copies(c)).get("campaignVersion").asLong();
        return tree(upload(dm,c,command(version,false,false),bytes),200).get("id").asString();
    }
    Map<String,Object> selection(String pkg,long version){return Map.of("expectedVersion",version,"packageId",pkg,"selections",List.of(Map.of("path","/nodes/"+module+"/text","choice","INCOMING")));}
    @Test void migrationAcceptsExactlyFiveHundredConfirmationsAndBlocksFiveHundredOne()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();var nodes=(tools.jackson.databind.node.ArrayNode)d.get("nodes");nodes.removeAll();
        nodes.add(json.valueToTree(Map.of("id",module,"kind","SCENE","name","Limit scene","text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of())));
        for(int n=0;n<499;n++){var node=(tools.jackson.databind.node.ObjectNode)nodes.get(0).deepCopy();node.put("id",UUID.nameUUIDFromBytes(("confirmation:"+n).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());node.put("kind","CHAPTER");nodes.add(node);}
        ((tools.jackson.databind.node.ArrayNode)d.get("play").get("units")).removeAll();((tools.jackson.databind.node.ArrayNode)d.get("play").get("placements")).removeAll();edit(dm,c,id,0,d);
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Confirmation capacity")),200).get("id").asString(),target=instance(live);var before=dm.read(target);
        for(int n=1;n<=498;n++)((tools.jackson.databind.node.ObjectNode)nodes.get(n)).put("text","Changed");edit(dm,c,id,1,d);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertEquals(499,impact.get("confirmations").size());assertTrue(impact.get("blockers").isEmpty());
        ((tools.jackson.databind.node.ObjectNode)nodes.get(499)).put("text","Changed");edit(dm,c,id,2,d);impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",3)),200);assertEquals(500,impact.get("confirmations").size());assertTrue(impact.get("blockers").isEmpty());
        ((tools.jackson.databind.node.ObjectNode)nodes.get(0)).put("text","Changed");edit(dm,c,id,3,d);impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",4)),200);assertEquals(501,impact.get("confirmations").size());assertTrue(impact.get("blockers").toString().contains("/confirmation-limit"));
        var confirmations=new ArrayList<String>();for(var value:impact.get("confirmations"))confirmations.add(value.asString());var filesBefore=files();int count=jdbc.queryForObject("select count(*) from campaign",Integer.class);
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",4,"impactHash",impact.get("impactHash").asString(),"confirmations",confirmations.subList(0,500))),409);assertEquals(before,dm.read(target));assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(filesBefore,files());assertTrue(dm.read(target+"/migrations").isEmpty());
        ((tools.jackson.databind.node.ObjectNode)nodes.get(0)).put("text","");edit(dm,c,id,4,d);impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",5)),200);assertEquals(500,impact.get("confirmations").size());assertTrue(impact.get("blockers").isEmpty());
        confirmations.clear();for(var value:impact.get("confirmations"))confirmations.add(value.asString());var apply=new LinkedHashMap<String,Object>(Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",5,"impactHash",impact.get("impactHash").asString()));
        var duplicate=new ArrayList<>(confirmations);duplicate.set(499,duplicate.getFirst());var unknown=new ArrayList<>(confirmations);unknown.set(0,"UNKNOWN");
        for(var invalid:List.of(confirmations.subList(0,499),duplicate,unknown)){apply.put("confirmations",invalid);tree(dm.post(target+"/migrations",apply),409);assertEquals(before,dm.read(target));assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(filesBefore,files());assertTrue(dm.read(target+"/migrations").isEmpty());}
        apply.put("confirmations",confirmations);var player=member(UUID.fromString(live),"PLAYER");tree(player.post(target+"/migrations",apply),403);var ack=tree(dm.post(target+"/migrations",apply),200);assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));assertEquals(count+1,jdbc.queryForObject("select count(*) from campaign",Integer.class));
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();assertEquals(500,dm.read(instance(recovery)).get("document").get("nodes").size());assertEquals(before,dm.read(target+"/migrations").get(0).get("before"));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(target+"/migrations",apply),403);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,dm.username);assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));assertEquals(count+1,jdbc.queryForObject("select count(*) from campaign",Integer.class));
    }
    UUID liveMap(UUID campaign,byte[] bytes)throws Exception{
        var image=io.dndroll.domain.AssetImage.validate(bytes,"image/png");UUID scene=UUID.randomUUID(),asset=UUID.randomUUID();String key=UUID.randomUUID().toString();files.publish(key,bytes);
        jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Capacity side room',24,16,48)",scene,campaign);
        jdbc.update("insert into campaign_asset(id,campaign_id,media_type,byte_size,width,height,storage_key,sha256) values (?,?,'image/png',?,?,?,?,?)",asset,campaign,bytes.length,image.width(),image.height(),key,image.sha256());
        jdbc.update("insert into scene_map(scene_id,campaign_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,48,0,0,5)",scene,campaign,asset);return scene;
    }
    void removeLiveMap(UUID scene){
        UUID asset=jdbc.queryForObject("select asset_id from scene_map where scene_id=?",UUID.class,scene);String key=jdbc.queryForObject("select storage_key from campaign_asset where id=?",String.class,asset);jdbc.update("delete from scene_map where scene_id=?",scene);jdbc.update("delete from scene where id=?",scene);jdbc.update("delete from campaign_asset where id=?",asset);files.discard(key);
    }
    void applyWithinArchiveLimit(Browser dm,String live,long customizationVersion)throws Exception{
        String target=instance(live);var before=dm.read(target);var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",customizationVersion)),200);assertTrue(impact.get("blockers").isEmpty(),impact.get("blockers").toString());
        var body=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",customizationVersion,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));var ack=tree(dm.post(target+"/migrations",body),200);assertEquals(ack,tree(dm.post(target+"/migrations",body),200));
        var evidence=dm.read(target+"/migrations");assertEquals(1,evidence.size());assertEquals(before,evidence.get(0).get("before"));UUID original=UUID.fromString(live),recovery=UUID.fromString(evidence.get(0).get("recoveryCampaignId").asString());
        assertEquals(jdbc.queryForList("select sha256,byte_size from campaign_asset where campaign_id=? order by sha256,byte_size",original),jdbc.queryForList("select sha256,byte_size from campaign_asset where campaign_id=? order by sha256,byte_size",recovery));
        for(var stored:jdbc.queryForList("select storage_key,sha256 from campaign_asset where campaign_id=?",recovery))assertEquals(stored.get("sha256"),ModuleArchive.hash(files.read((String)stored.get("storage_key"))));
    }
    byte[] paddedPng(int size,boolean entropy)throws Exception{
        byte[] original=png(),payload=new byte[size-original.length-12];if(entropy)new Random(77123).nextBytes(payload);var out=new ByteArrayOutputStream();out.write(original,0,original.length-12);var chunk=new DataOutputStream(out);chunk.writeInt(payload.length);byte[] type={'r','a','N','d'};chunk.write(type);chunk.write(payload);var crc=new CRC32();crc.update(type);crc.update(payload);chunk.writeInt((int)crc.getValue());out.write(original,original.length-12,12);byte[] bytes=out.toByteArray();assertEquals(size,bytes.length);io.dndroll.domain.AssetImage.validate(bytes,"image/png");return bytes;
    }
    @Test void migrationAppliesCombinedAssetCountLimitAndRecoversExactlyOneHundredTwentyEight()throws Exception{
        var dm=dm();UUID c=campaign(dm);var archive=new ModuleArchive();var original=archive.verify(imageArchive(List.of()));var m=original.manifest();var entries=new ArrayList<ModuleArchive.Asset>();var images=new LinkedHashMap<String,byte[]>();
        for(int n=0;n<127;n++){String asset=n==0?imageId:UUID.nameUUIDFromBytes(("asset-limit:"+n).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();byte[] bytes=png();entries.add(new ModuleArchive.Asset(asset,"image/png",bytes.length,ModuleArchive.hash(bytes)));images.put(asset,bytes);}
        var manifest=new ModuleArchive.Manifest(m.format(),2,m.moduleId(),m.moduleVersion(),m.name(),m.rules(),m.rights(),m.dependencies(),m.content(),entries);var d=imageDocument();byte[] bytes=archive.export(manifest,json.treeToValue(d,io.dndroll.modules.ModuleBlueprint.Document.class),images,module,"1.0.0","127 images");
        String pkg=tree(upload(dm,c,command(0,false,false),bytes),200).get("id").asString(),id=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"packageId",pkg,"name","Image count source")),200).get("id").asString();
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(0,"Image count limit")),200).get("id").asString(),target=instance(live);UUID campaign=UUID.fromString(live),mapped=liveMap(campaign,png()),shared=UUID.randomUUID();
        jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Shared map',24,16,48)",shared,campaign);jdbc.update("insert into scene_map(scene_id,campaign_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) select ?,campaign_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell from scene_map where scene_id=?",shared,mapped);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",0)),200);assertTrue(impact.get("blockers").isEmpty(),impact.get("blockers").toString());
        UUID overflow=liveMap(campaign,png());blockedMigration(dm,target,0,0,"/recovery-definition");
        removeLiveMap(overflow);
        impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",0)),200);assertTrue(impact.get("blockers").isEmpty());var apply=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",0,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));var ack=tree(dm.post(target+"/migrations",apply),200);assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));
        String recovered=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();assertEquals(128,dm.read("/api/campaigns/"+recovered+"/assets").get("assets").size());assertEquals(128,dm.read(instance(recovered)).get("source").get("manifest").get("assets").size());
        assertEquals(3,dm.read(instance(recovered)).get("document").get("play").get("scenes").size());assertEquals(2,jdbc.queryForObject("select max(refs) from (select count(*) refs from scene_map where campaign_id=? group by asset_id) r",Integer.class,UUID.fromString(recovered)));
    }
    @Test void migrationBlocksCombinedExpandedAndCompressedArchiveSizesWithoutWrites()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=imageCopy(dm,c,List.of());edit(dm,c,id,0,imageDocument());
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Expanded capacity")),200).get("id").asString();UUID campaign=UUID.fromString(live);int imageSize=8*1024*1024;byte[] large=paddedPng(imageSize,false);
        for(int n=0;n<3;n++)liveMap(campaign,large);UUID expandedOverflow=liveMap(campaign,paddedPng(imageSize-png().length,false));
        // Images alone fit 32 MiB exactly; manifest/content overhead must count too.
        assertEquals(ModuleArchive.EXPANDED_LIMIT,jdbc.queryForObject("select sum(byte_size) from campaign_asset where campaign_id=?",Long.class,campaign));blockedMigration(dm,instance(live),0,1,"/recovery-definition");
        removeLiveMap(expandedOverflow);liveMap(campaign,paddedPng(7*1024*1024,false));applyWithinArchiveLimit(dm,live,1);
        String second=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Compressed capacity")),200).get("id").asString();UUID other=UUID.fromString(second);byte[] noisy=paddedPng(6*1024*1024,true);liveMap(other,noisy);UUID compressedOverflow=liveMap(other,noisy);
        assertTrue(jdbc.queryForObject("select sum(byte_size) from campaign_asset where campaign_id=?",Long.class,other)<ModuleArchive.EXPANDED_LIMIT);blockedMigration(dm,instance(second),0,1,"/recovery-definition");
        removeLiveMap(compressedOverflow);applyWithinArchiveLimit(dm,second,1);
    }
    void blockedMigration(Browser dm,String target,long version,long customizationVersion,String blocker)throws Exception{
        var review=Map.of("expectedVersion",version,"customizationVersion",customizationVersion);var before=dm.read(target);var impact=tree(dm.post(target+"/migration-impact",review),200);
        assertTrue(impact.get("blockers").toString().contains(blocker),impact.get("blockers").toString());
        int count=jdbc.queryForObject("select count(*) from campaign",Integer.class),receipts=jdbc.queryForObject("select count(*) from module_lifecycle_receipt",Integer.class);var filesBefore=files();var evidence=dm.read(target+"/migrations");
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"customizationVersion",customizationVersion,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),409);
        assertEquals(before,dm.read(target));assertEquals(impact.get("runtime"),tree(dm.post(target+"/migration-impact",review),200).get("runtime"));assertEquals(evidence,dm.read(target+"/migrations"));assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(receipts,jdbc.queryForObject("select count(*) from module_lifecycle_receipt",Integer.class));assertEquals(filesBefore,files());
    }
    @Test void migrationRejectsUnavailableDestinationMapBeforeRecoveryWrites()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=imageCopy(dm,c,List.of());var d=imageDocument();edit(dm,c,id,0,d);
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Destination map integrity")),200).get("id").asString();String target=instance(live);UUID campaign=UUID.fromString(live);
        var before=dm.read(target);UUID asset=UUID.fromString(before.get("mapping").get("assets").get(imageId).asString());String key=jdbc.queryForObject("select storage_key from campaign_asset where id=?",String.class,asset);
        // The live map was detached: recovery no longer reads the template asset.
        jdbc.update("delete from scene_map where campaign_id=?",campaign);
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0).get("map")).put("offsetX",12);edit(dm,c,id,1,d);
        files.discard(key);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);
        assertTrue(impact.get("blockers").toString().contains("/map-assets/"+imageId),impact.toString());
        int count=jdbc.queryForObject("select count(*) from campaign",Integer.class);var filesBefore=assetFiles();
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),409);
        assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(filesBefore,assetFiles());assertEquals(before,dm.read(target));assertTrue(dm.read(target+"/migrations").isEmpty());
        files.publish(key,new byte[]{1,2,3});blockedMigration(dm,target,0,2,"/map-assets/"+imageId);
        files.discard(key);files.publish(key,png());jdbc.update("update campaign_asset set width=13 where id=?",asset);blockedMigration(dm,target,0,2,"/map-assets/"+imageId);jdbc.update("update campaign_asset set width=12 where id=?",asset);
        jdbc.update("update campaign_asset set sha256=? where id=?","0".repeat(64),asset);blockedMigration(dm,target,0,2,"/map-assets/"+imageId);jdbc.update("update campaign_asset set sha256=? where id=?",ModuleArchive.hash(png()),asset);
        jdbc.update("delete from campaign_asset where id=?",asset);blockedMigration(dm,target,0,2,"/map-assets/"+imageId);
        jdbc.update("insert into campaign_asset(id,campaign_id,media_type,byte_size,width,height,storage_key,sha256) values (?,?,'image/png',?,12,10,?,?)",asset,campaign,png().length,key,ModuleArchive.hash(png()));
        jdbc.update("update module_instance set mapping=jsonb_set(mapping,'{assets}', '{}'::jsonb) where campaign_id=?",campaign);blockedMigration(dm,target,0,2,"/map-assets/"+imageId);jdbc.update("update module_instance set mapping=?::jsonb where campaign_id=?",before.get("mapping").toString(),campaign);
        impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertTrue(impact.get("blockers").isEmpty());
        var apply=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));files.discard(key);tree(dm.post(target+"/migrations",apply),409);assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(before,dm.read(target));
        files.publish(key,png());var ack=tree(dm.post(target+"/migrations",apply),200);assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));
        String scene=before.get("mapping").get("scenes").get(module).asString();var map=dm.read("/api/campaigns/"+live+"/scenes/"+scene+"/map").get("map");assertEquals(asset.toString(),map.get("assetId").asString());assertEquals(12,map.get("offsetX").asInt());
        String recovered=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();var recoveredInstance=dm.read(instance(recovered));assertTrue(dm.read("/api/campaigns/"+recovered+"/scenes/"+recoveredInstance.get("mapping").get("scenes").get(module).asString()+"/map").get("map").isNull());
    }
    @Test void migrationIgnoresDocumentOrderingButStillBlocksMaterialEdits()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();var nodes=(tools.jackson.databind.node.ArrayNode)d.get("nodes");
        var extra=(tools.jackson.databind.node.ObjectNode)nodes.get(2).deepCopy();extra.put("id",UUID.randomUUID().toString());extra.put("name","Second material");nodes.add(extra);edit(dm,c,id,0,d);
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Material ordering")),200).get("id").asString();String target=instance(live),root="/api/campaigns/"+live;var notes=dm.read(root+"/notes");
        var first=nodes.remove(2);nodes.add(first);edit(dm,c,id,1,d);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertTrue(impact.get("changes").isEmpty());assertTrue(impact.get("blockers").isEmpty(),impact.toString());
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);var afterNotes=dm.read(root+"/notes");assertEquals(notes.get("notes"),afterNotes.get("notes"));assertEquals(notes.get("campaignVersion").asLong()+1,afterNotes.get("campaignVersion").asLong());
        ((tools.jackson.databind.node.ObjectNode)nodes.get(2)).put("text","Changed published material");edit(dm,c,id,2,d);
        impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",1,"customizationVersion",3)),200);assertTrue(impact.get("blockers").toString().contains("/published-materials"));
        blockedMigration(dm,target,1,3,"/published-materials");
    }
    @Test void migrationBlocksChangedRuntimeLinksAndMissingMappedObjects()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Broken links")),200).get("id").asString();String target=instance(live);UUID campaign=UUID.fromString(live);var before=dm.read(target);
        UUID token=UUID.fromString(before.get("mapping").get("placements").get("10000000-0000-4000-8000-000000000004").asString()),scene=UUID.fromString(before.get("mapping").get("scenes").get(module).asString()),card=UUID.fromString(before.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString());
        jdbc.update("update token set character_id=null where id=?",token);blockedMigration(dm,target,0,1,"/changed-placement-link/");jdbc.update("update token set character_id=? where id=?",card,token);
        UUID side=UUID.randomUUID();jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Side',24,16,48)",side,campaign);jdbc.update("update token set scene_id=? where id=?",side,token);blockedMigration(dm,target,0,1,"/changed-placement-link/");jdbc.update("update token set scene_id=? where id=?",scene,token);
        jdbc.update("update token set module_retired=true where id=?",token);blockedMigration(dm,target,0,1,"/changed-placement-link/");jdbc.update("update token set module_retired=false where id=?",token);
        jdbc.update("update scene set module_retired=true where id=?",scene);blockedMigration(dm,target,0,1,"/missing-scene/");jdbc.update("update scene set module_retired=false where id=?",scene);
        jdbc.update("update module_instance set mapping=jsonb_set(mapping,'{units,10000000-0000-4000-8000-000000000002}',to_jsonb(?::text)) where campaign_id=?",UUID.randomUUID().toString(),campaign);blockedMigration(dm,target,0,1,"/missing-unit/");jdbc.update("update module_instance set mapping=?::jsonb where campaign_id=?",before.get("mapping").toString(),campaign);
        assertTrue(tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",1)),200).get("blockers").isEmpty());
    }
    @Test void migrationBlocksAdmissionPolicyChanges()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();edit(dm,c,id,0,d);String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Policy boundary")),200).get("id").asString();
        ((tools.jackson.databind.node.ObjectNode)d.get("play")).put("admissionMode","FREE");edit(dm,c,id,1,d);blockedMigration(dm,instance(live),0,2,"admissionMode");
    }
    @Test void migrationBlocksManifestRightsAdoptedByTheSourceCustomization()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();edit(dm,c,id,0,d);String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Manifest boundary")),200).get("id").asString();
        var archives=new ModuleArchive();var m=archives.verify(archive()).manifest();var revised=new ModuleArchive.Manifest(m.format(),2,m.moduleId(),m.moduleVersion(),m.name(),m.rules(),"Revised original fixture rights",m.dependencies(),m.content(),m.assets());
        byte[] bytes=archives.export(revised,json.treeToValue(d,io.dndroll.modules.ModuleBlueprint.Document.class),Map.of(),module,"2.0.0","Changed rights");
        long campaignVersion=dm.read(copies(c)).get("campaignVersion").asLong();String pkg=tree(upload(dm,c,command(campaignVersion,false,false),bytes),200).get("id").asString(),path=copies(c)+"/"+id;
        var options=tree(dm.post(path+"/update-options",Map.of("expectedVersion",1,"packageId",pkg)),200);var choices=new ArrayList<Map<String,String>>();for(var change:options.get("changes"))choices.add(Map.of("path",change.get("path").asString(),"choice","INCOMING"));
        var impact=tree(dm.post(path+"/update-impact",Map.of("expectedVersion",1,"packageId",pkg,"selections",choices)),200);
        tree(dm.post(path+"/updates",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"packageId",pkg,"selections",choices,"sha256",impact.get("sha256").asString(),"impactHash",impact.get("impactHash").asString())),200);
        blockedMigration(dm,instance(live),0,2,"/manifest");
    }
    @Test void structuralMigrationRequiresExactConfirmationAndCreatesPlayableRecovery()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Structural party")),200).get("id").asString();
        String target=instance(live),root="/api/campaigns/"+live;var before=dm.read(target);
        String unit="10000000-0000-4000-8000-000000000002",cardId=before.get("mapping").get("units").get(unit).asString();
        tree(dm.post(root+"/characters/"+cardId+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-5,"reason","Damage before migration")),200);
        var damaged=dm.read(root+"/characters/"+cardId);var d=playable();
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0)).put("width",30);
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("units").get(0).get("sheet")).put("name","Updated guard");edit(dm,c,id,1,d);
        var review=Map.of("expectedVersion",0,"customizationVersion",2);
        var impact=tree(dm.post(target+"/migration-impact",review),200);assertFalse(impact.get("confirmations").isEmpty());assertEquals(before,dm.read(target));
        var apply=new LinkedHashMap<String,Object>(review);apply.put("requestId",UUID.randomUUID());apply.put("impactHash",impact.get("impactHash").asString());apply.put("confirmations",List.of());
        tree(dm.post(target+"/migrations",apply),409);assertEquals(before,dm.read(target));
        apply.put("confirmations",impact.get("confirmations"));var ack=tree(dm.post(target+"/migrations",apply),200);assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));
        assertEquals(30,dm.read(root+"/scene").get("width").asInt());var updated=dm.read(root+"/characters/"+cardId);
        assertEquals(damaged.get("sheet").get("combat"),updated.get("sheet").get("combat"));assertEquals(damaged.get("sheet").get("resources"),updated.get("sheet").get("resources"));assertEquals("Updated guard",updated.get("sheet").get("name").asString());
        var evidence=dm.read(target+"/migrations").get(0);String recovery=evidence.get("recoveryCampaignId").asString();var restored=dm.read(instance(recovery));
        String recoveredCard=restored.get("mapping").get("units").get(unit).asString();assertEquals(damaged.get("sheet"),dm.read("/api/campaigns/"+recovery+"/characters/"+recoveredCard).get("sheet"));
        assertEquals(20,dm.read("/api/campaigns/"+recovery+"/scene").get("width").asInt());assertEquals(before,evidence.get("before"));
        var player=member(UUID.fromString(live),"PLAYER");tree(player.get(target+"/migrations"),403);tree(player.post(target+"/migrations",apply),403);
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(target+"/migrations",apply),403);
    }
    @Test void migrationRetiresCurrentSceneAndReplacesPlacementWithoutChangingCombatIdentity()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Replacement party")),200).get("id").asString();String target=instance(live),root="/api/campaigns/"+live;
        var before=dm.read(target);String oldCard=before.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();var oldScene=dm.read(root+"/scene");
        var controller=member(UUID.fromString(live),"PLAYER");String tokenId=before.get("mapping").get("placements").get("10000000-0000-4000-8000-000000000004").asString();jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,?)",UUID.fromString(tokenId),UUID.fromString(live),controller.username);
        var participant=Map.of("characterId",oldCard,"expectedVersion",0,"kind","PC","group","","surprised",false,"advantage",List.of(),"disadvantage",List.of());
        tree(dm.post(root+"/initiative",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",List.of(participant),"reason","Migration combat")),200);
        tree(dm.post(root+"/initiative/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",List.of(oldCard),"reason","Confirm")),200);
        tree(dm.post(root+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"kind","START","characterId","","order",List.of(),"reason","Start")),200);var combat=dm.read(root+"/combat");var initiative=dm.read(root+"/initiative");
        var d=playable();String nextScene=UUID.randomUUID().toString(),nextUnit=UUID.randomUUID().toString();
        ((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(0)).put("id",nextScene);((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(1)).put("id",nextUnit);
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0)).put("nodeId",nextScene);((tools.jackson.databind.node.ObjectNode)d.get("play").get("units").get(0)).put("nodeId",nextUnit);
        var p=(tools.jackson.databind.node.ObjectNode)d.get("play").get("placements").get(0);p.put("sceneId",nextScene);p.put("unitId",nextUnit);edit(dm,c,id,1,d);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertTrue(impact.get("confirmations").toString().contains("RETAIN_COMBAT_IDENTITY"));assertTrue(impact.get("confirmations").toString().contains("RETIRE_SCENE"));
        var apply=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));tree(dm.post(target+"/migrations",apply),200);
        assertEquals(combat,dm.read(root+"/combat"));assertEquals(initiative,dm.read(root+"/initiative"));assertEquals(oldCard,dm.read(root+"/combat").get("currentId").asString());
        var migrated=dm.read(target);assertNotEquals(oldCard,migrated.get("mapping").get("units").get(nextUnit).asString());assertNotEquals(oldScene.get("id"),dm.read(root+"/scene").get("id"));
        assertEquals(1,dm.read(root+"/scenes").get("scenes").size());assertEquals(1,jdbc.queryForObject("select count(*) from scene where campaign_id=? and module_retired",Integer.class,UUID.fromString(live)));
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();assertEquals(1,dm.read("/api/campaigns/"+recovery+"/scene").get("tokens").size());
        assertAll(()->assertTrue(tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",1,"customizationVersion",2)),200).get("blockers").isEmpty()),
            ()->assertEquals(0,jdbc.queryForObject("select count(*) from token_controller where token_id=? and username=?",Integer.class,UUID.fromString(tokenId),controller.username)));
        var oldSheet=dm.read(root+"/characters/"+oldCard).get("sheet");
        var again=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",1,"customizationVersion",2)),200);
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"customizationVersion",2,"impactHash",again.get("impactHash").asString(),"confirmations",again.get("confirmations"))),200);
        String secondRecovery=dm.read(target+"/migrations").get(1).get("recoveryCampaignId").asString();
        var recovered=dm.read(instance(secondRecovery)).get("document").get("play").get("units");
        assertEquals(2,recovered.size(),"Recovery must include the retained old combat identity on later migrations");
        assertTrue(java.util.stream.StreamSupport.stream(recovered.spliterator(),false).anyMatch(u->u.get("sheet").equals(oldSheet)));
        assertEquals(combat,dm.read(root+"/combat"));assertEquals(initiative,dm.read(root+"/initiative"));
    }
    @Test void migrationContinuesAfterAllMappedScenesAreRemoved()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();edit(dm,c,id,0,d);
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Empty scenes")),200).get("id").asString(),target=instance(live),root="/api/campaigns/"+live;
        ((tools.jackson.databind.node.ArrayNode)d.get("play").get("scenes")).removeAll();((tools.jackson.databind.node.ArrayNode)d.get("play").get("placements")).removeAll();edit(dm,c,id,1,d);
        var first=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertTrue(first.get("blockers").isEmpty());
        var body=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",first.get("impactHash").asString(),"confirmations",first.get("confirmations"));var ack=tree(dm.post(target+"/migrations",body),200);
        var fallback=dm.read(root+"/scene");assertTrue(dm.read(target).get("mapping").get("scenes").isEmpty());
        var next=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",1,"customizationVersion",2)),200);assertTrue(next.get("blockers").isEmpty(),next.toString());
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"customizationVersion",2,"impactHash",next.get("impactHash").asString(),"confirmations",next.get("confirmations"))),200);
        assertEquals(fallback,dm.read(root+"/scene"));String recovery=dm.read(target+"/migrations").get(1).get("recoveryCampaignId").asString();assertEquals(fallback.get("name"),dm.read("/api/campaigns/"+recovery+"/scene").get("name"));
        assertEquals(1,dm.read(instance(recovery)).get("document").get("play").get("scenes").size());assertEquals(ack,tree(dm.post(target+"/migrations",body),200));assertEquals(fallback,dm.read(root+"/scene"));
    }
    @Test void migrationRecoversUnmappedScenesTokensAndMapBytesWithoutAdoptingThem()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Ad hoc play")),200).get("id").asString(),target=instance(live),root="/api/campaigns/"+live;
        var before=dm.read(target);UUID campaign=UUID.fromString(live),extraScene=UUID.fromString(module),extraToken=UUID.randomUUID(),asset=UUID.randomUUID();String card=before.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();
        jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Live side room',31,19,64)",extraScene,campaign);
        jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,hidden,character_id) values (?,?,?,'Live alias',7,8,true,?)",extraToken,extraScene,campaign,UUID.fromString(card));
        UUID loose=UUID.randomUUID();jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,hidden) values (?,?,?,'Loose marker',4,5,false)",loose,extraScene,campaign);
        String key=UUID.randomUUID().toString();files.publish(key,png());
        jdbc.update("insert into campaign_asset(id,campaign_id,media_type,byte_size,width,height,storage_key,sha256) values (?,?,'image/png',?,12,10,?,?)",asset,campaign,png().length,key,ModuleArchive.hash(png()));
        jdbc.update("insert into scene_map(scene_id,campaign_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,32,3,4,10)",extraScene,campaign,asset);
        jdbc.update("update campaign set current_scene_id=? where id=?",extraScene,campaign);var runtime=dm.read(root+"/scene");
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",1)),200);assertTrue(impact.get("blockers").isEmpty(),impact.toString());
        assertTrue(impact.get("confirmations").toString().contains("RECOVER_EXTRA_SCENE"));assertTrue(impact.get("confirmations").toString().contains("RECOVER_EXTRA_PLACEMENT"));assertTrue(impact.get("confirmations").toString().contains("RECOVER_EXTRA_MAP"));
        var apply=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",1,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));
        var incomplete=new LinkedHashMap<String,Object>(apply);incomplete.put("confirmations",List.of());tree(dm.post(target+"/migrations",incomplete),409);
        var overflow=new ArrayList<UUID>();for(int n=0;n<49;n++){UUID sceneId=UUID.randomUUID();overflow.add(sceneId);jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Overflow',24,16,48)",sceneId,campaign);}assertTrue(tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",1)),200).get("blockers").toString().contains("/recovery-definition"));tree(dm.post(target+"/migrations",apply),409);for(UUID sceneId:overflow)jdbc.update("delete from scene where id=?",sceneId);
        int count=jdbc.queryForObject("select count(*) from campaign",Integer.class);files.discard(key);var unavailable=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",1)),200);assertTrue(unavailable.get("blockers").toString().contains("/recovery-definition"));tree(dm.post(target+"/migrations",apply),409);assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));files.publish(key,png());
        jdbc.update("update token set name='Concurrent edit' where id=?",extraToken);tree(dm.post(target+"/migrations",apply),409);jdbc.update("update token set name='Live alias' where id=?",extraToken);
        String fn="extra_fail_"+c.toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.result_kind='MIGRATE' then raise exception 'injected extra recovery failure'; end if; return new; end $$");
        var filesBefore=assetFiles();try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" "+(deferred?"after":"before")+" insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(target+"/migrations",apply),500);assertEquals(before,dm.read(target));assertEquals(runtime,dm.read(root+"/scene"));assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));assertEquals(filesBefore,assetFiles());jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");}}finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        var pool=Executors.newFixedThreadPool(2);JsonNode ack;try{var a=pool.submit(()->dm.post(target+"/migrations",apply));var b=pool.submit(()->dm.post(target+"/migrations",apply));ack=tree(a.get(),200);assertEquals(ack,tree(b.get(),200));}finally{pool.shutdownNow();}assertEquals(count+1,jdbc.queryForObject("select count(*) from campaign",Integer.class));
        assertEquals(before.get("mapping"),dm.read(target).get("mapping"));assertEquals(runtime,dm.read(root+"/scene"));assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();var restored=dm.read(instance(recovery));var scene=dm.read("/api/campaigns/"+recovery+"/scene");assertEquals("Live side room",scene.get("name").asString());assertEquals(31,scene.get("width").asInt());assertEquals(2,scene.get("tokens").size());
        String token=restored.get("mapping").get("placements").get(extraToken.toString()).asString();var tokenRow=jdbc.queryForMap("select name,x,y,hidden,character_id from token where id=?",UUID.fromString(token));assertEquals("Live alias",tokenRow.get("name"));assertEquals(7,tokenRow.get("x"));assertEquals(true,tokenRow.get("hidden"));assertEquals(UUID.fromString(restored.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString()),tokenRow.get("character_id"));
        var map=dm.read("/api/campaigns/"+recovery+"/scenes/"+scene.get("id").asString()+"/map").get("map");assertEquals(32,map.get("gridSize").asInt());String recoveredAsset=map.get("assetId").asString();assertNotEquals(asset.toString(),recoveredAsset);
        var stored=jdbc.queryForMap("select storage_key from campaign_asset where id=?",UUID.fromString(recoveredAsset));assertNotEquals(key,stored.get("storage_key"));assertArrayEquals(png(),((io.dndroll.application.AssetService.Files)files).read((String)stored.get("storage_key")));
        UUID recoveredLoose=UUID.fromString(restored.get("mapping").get("placements").get(loose.toString()).asString());assertNull(jdbc.queryForObject("select character_id from token where id=?",UUID.class,recoveredLoose));assertEquals(List.of(dm.username),jdbc.queryForList("select username from token_controller where token_id=?",String.class,recoveredLoose));
        assertEquals(0,jdbc.queryForObject("select count(*) from scene_revealed_cell where campaign_id=?",Integer.class,UUID.fromString(recovery)));var player=member(campaign,"PLAYER");tree(player.get(instance(recovery)),403);tree(player.post(target+"/migrations",apply),403);
    }
    @Autowired io.dndroll.application.AssetService.Files files;
    Set<String> assetFiles()throws Exception{try(var paths=java.nio.file.Files.walk(java.nio.file.Path.of(root))){return paths.filter(java.nio.file.Files::isRegularFile).map(Object::toString).collect(java.util.stream.Collectors.toSet());}}
    @Test void migrationBlocksShrinkingAroundUnmappedTokens()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();edit(dm,c,id,0,d);String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Unmapped bounds")),200).get("id").asString(),target=instance(live),root="/api/campaigns/"+live;
        var scene=dm.read(root+"/scene");UUID extra=UUID.randomUUID();jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y) values (?,?,?,'Stay at 19',19,14)",extra,UUID.fromString(scene.get("id").asString()),UUID.fromString(live));
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0)).put("width",10);edit(dm,c,id,1,d);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertTrue(impact.get("blockers").toString().contains("/scene-bounds/"),impact.get("blockers").toString());
        int count=jdbc.queryForObject("select count(*) from campaign",Integer.class);tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),409);assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));
        ((tools.jackson.databind.node.ArrayNode)d.get("play").get("scenes")).removeAll();((tools.jackson.databind.node.ArrayNode)d.get("play").get("placements")).removeAll();edit(dm,c,id,2,d);
        for(int version=0;version<2;version++){impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",version,"customizationVersion",3)),200);assertTrue(impact.get("blockers").isEmpty());tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"customizationVersion",3,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);}
        String recovery=dm.read(target+"/migrations").get(1).get("recoveryCampaignId").asString();assertEquals(2,dm.read(instance(recovery)).get("document").get("play").get("scenes").size());assertEquals(1,jdbc.queryForObject("select count(*) from token where campaign_id=?",Integer.class,UUID.fromString(recovery)),"Recover active ad-hoc token in retired scene without resurrecting retired template tokens");
    }
    @Test void migrationRecoversAdditionalLiveCardsWithoutInheritingPlayerAccess()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Additional character")),200).get("id").asString(),root="/api/campaigns/"+live,target=instance(live);
        var player=member(UUID.fromString(live),"PLAYER");long v=jdbc.queryForObject("select version from campaign where id=?",Long.class,UUID.fromString(live));
        var extra=tree(dm.post(root+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",io.dndroll.domain.CharacterSheet.empty("Live companion"))),200);
        String card=extra.get("id").asString();
        tree(dm.post(root+"/characters/"+card+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"controllers",List.of(player.username),"hidden",false,"shareFull",true,"approveImport",true,"reason","Shared companion")),200);
        var before=dm.read(root+"/characters/"+card);
        var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",1)),200);assertTrue(impact.get("blockers").isEmpty(),impact.toString());
        var apply=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",1,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));
        var ack=tree(dm.post(target+"/migrations",apply),200);assertEquals(ack,tree(dm.post(target+"/migrations",apply),200));assertEquals(before,dm.read(root+"/characters/"+card));
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();var restored=dm.read(instance(recovery));assertEquals(2,restored.get("document").get("play").get("units").size());
        String recoveredCard=restored.get("mapping").get("units").get(card).asString();assertNotEquals(card,recoveredCard);assertEquals(before.get("sheet"),dm.read("/api/campaigns/"+recovery+"/characters/"+recoveredCard).get("sheet"));
        tree(player.get("/api/campaigns/"+recovery+"/characters/"+recoveredCard),403);
    }
    @Test void migrationReportsRecoveryCapacityAndAvoidsExtraCardNodeCollisions()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Recovery capacity")),200).get("id").asString(),target=instance(live);
        UUID campaign=UUID.fromString(live),card=UUID.fromString(dm.read(target).get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString());
        var extras=new ArrayList<UUID>();for(int n=0;n<100;n++){UUID extra=n==0?UUID.fromString(module):UUID.randomUUID();extras.add(extra);jdbc.update("insert into character_card(id,campaign_id,sheet) select ?,campaign_id,sheet from character_card where id=?",extra,card);}
        var review=Map.of("expectedVersion",0,"customizationVersion",1);var impact=tree(dm.post(target+"/migration-impact",review),200);assertTrue(impact.get("blockers").toString().contains("/recovery-definition"));
        int count=jdbc.queryForObject("select count(*) from campaign",Integer.class);tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",1,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),409);assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));
        for(UUID extra:extras.subList(1,extras.size()))jdbc.update("delete from character_card where id=?",extra);
        impact=tree(dm.post(target+"/migration-impact",review),200);assertTrue(impact.get("blockers").isEmpty());
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",1,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();var document=dm.read(instance(recovery)).get("document");assertEquals(2,document.get("play").get("units").size());assertNotEquals(module,document.get("play").get("units").get(1).get("nodeId").asString());
        assertEquals(2,jdbc.queryForObject("select count(*) from character_card where campaign_id=?",Integer.class,campaign));
    }
    @Test void migrationBindsRuntimeAndRollsBackRecoveryOnDeferredFailure()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Atomic migration")),200).get("id").asString();String target=instance(live),root="/api/campaigns/"+live;var before=dm.read(target);var scene=dm.read(root+"/scene");
        var d=playable();((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0)).put("width",25);edit(dm,c,id,1,d);var review=Map.of("expectedVersion",0,"customizationVersion",2);
        var impact=tree(dm.post(target+"/migration-impact",review),200);var apply=new LinkedHashMap<String,Object>(review);apply.put("requestId",UUID.randomUUID());apply.put("impactHash",impact.get("impactHash").asString());apply.put("confirmations",impact.get("confirmations"));
        String card=before.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();tree(dm.post(root+"/characters/"+card+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-1,"reason","Concurrent damage")),200);tree(dm.post(target+"/migrations",apply),409);
        impact=tree(dm.post(target+"/migration-impact",review),200);apply.put("impactHash",impact.get("impactHash").asString());apply.put("confirmations",impact.get("confirmations"));
        int count=jdbc.queryForObject("select count(*) from campaign",Integer.class);String fn="migration_fail_"+c.toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.result_kind='MIGRATE' then raise exception 'injected migration failure'; end if; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" "+(deferred?"after":"before")+" insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(target+"/migrations",apply),500);assertEquals(before,dm.read(target));assertEquals(scene,dm.read(root+"/scene"));assertTrue(dm.read(target+"/migrations").isEmpty());assertEquals(count,jdbc.queryForObject("select count(*) from campaign",Integer.class));jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");}}finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        var ready=Map.copyOf(apply);var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(target+"/migrations",ready));var b=pool.submit(()->dm.post(target+"/migrations",ready));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}assertEquals(count+1,jdbc.queryForObject("select count(*) from campaign",Integer.class));
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",UUID.fromString(recovery),dm.username);tree(dm.post(target+"/migrations",apply),403);
    }
    @Test void migrationHidesRetainedPlacementWhenItsUnitBecomesSecretWithoutResettingPosition()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var original=playable();
        ((tools.jackson.databind.node.ObjectNode)original.get("play").get("units").get(0)).put("hidden",false);((tools.jackson.databind.node.ObjectNode)original.get("play").get("placements").get(0)).put("hidden",false);edit(dm,c,id,0,original);
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Visibility migration")),200).get("id").asString();String target=instance(live),root="/api/campaigns/"+live;var before=dm.read(target);String token=before.get("mapping").get("placements").get("10000000-0000-4000-8000-000000000004").asString();
        tree(dm.post(root+"/tokens/"+token+"/moves",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",7,"y",8)),200);
        var next=original.deepCopy();((tools.jackson.databind.node.ObjectNode)next.get("play").get("units").get(0)).put("hidden",true);edit(dm,c,id,1,next);
        var review=Map.of("expectedVersion",0,"customizationVersion",2);var impact=tree(dm.post(target+"/migration-impact",review),200);
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);
        var player=member(UUID.fromString(live),"PLAYER");assertTrue(player.read(root+"/scene").get("tokens").isEmpty());
        var moved=dm.read(root+"/scene").get("tokens").get(0);assertEquals(7,moved.get("x").asInt());assertEquals(8,moved.get("y").asInt());
    }
    @Test void placementMigrationPreservesUnchangedLiveCoordinatesAndNeverRevealsHiddenTokens()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("units").get(0)).put("hidden",false);var p=(tools.jackson.databind.node.ObjectNode)d.get("play").get("placements").get(0);p.put("hidden",false);edit(dm,c,id,0,d);
        String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Placement preservation")),200).get("id").asString(),root="/api/campaigns/"+live,target=instance(live);String token=dm.read(root+"/scene").get("tokens").get(0).get("id").asString();
        tree(dm.post(root+"/tokens/"+token+"/moves",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",7,"y",8)),200);
        p.put("hidden",true);edit(dm,c,id,1,d);var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);
        var firstBody=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",2,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"));var firstAck=tree(dm.post(target+"/migrations",firstBody),200);
        var hidden=dm.read(root+"/scene").get("tokens").get(0);assertAll(()->assertEquals(7,hidden.get("x").asInt()),()->assertEquals(8,hidden.get("y").asInt()));
        p.put("hidden",false);p.put("x",9);edit(dm,c,id,2,d);impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",1,"customizationVersion",3)),200);
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"customizationVersion",3,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);
        var moved=dm.read(root+"/scene").get("tokens").get(0);var player=member(UUID.fromString(live),"PLAYER");assertAll(()->assertEquals(9,moved.get("x").asInt()),()->assertEquals(8,moved.get("y").asInt()),()->assertTrue(player.read(root+"/scene").get("tokens").isEmpty()));
        var after=dm.read(root+"/scene");assertEquals(firstAck,tree(dm.post(target+"/migrations",firstBody),200));assertEquals(after,dm.read(root+"/scene"));tree(player.post(target+"/migrations",firstBody),403);
    }
    @Test void sceneShrinkReviewsFinalPlacementPositionsAndAllowsExplicitRetirement()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var d=playable();edit(dm,c,id,0,d);String live=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"Shrink placement")),200).get("id").asString(),root="/api/campaigns/"+live,target=instance(live);String token=dm.read(root+"/scene").get("tokens").get(0).get("id").asString();
        tree(dm.post(root+"/tokens/"+token+"/moves",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",7,"y",8)),200);
        var scene=(tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0);scene.put("width",5);scene.put("height",5);edit(dm,c,id,1,d);var impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",2)),200);assertTrue(impact.get("blockers").toString().contains("/scene-bounds/"));
        var p=(tools.jackson.databind.node.ObjectNode)d.get("play").get("placements").get(0);p.put("x",3);p.put("y",4);edit(dm,c,id,2,d);impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",0,"customizationVersion",3)),200);assertTrue(impact.get("blockers").isEmpty(),impact.get("blockers").toString());
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"customizationVersion",3,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);var moved=dm.read(root+"/scene").get("tokens").get(0);assertEquals(3,moved.get("x").asInt());assertEquals(4,moved.get("y").asInt());
        String recovery=dm.read(target+"/migrations").get(0).get("recoveryCampaignId").asString();assertEquals(7,dm.read("/api/campaigns/"+recovery+"/scene").get("tokens").get(0).get("x").asInt());
        scene.put("width",2);scene.put("height",2);((tools.jackson.databind.node.ArrayNode)d.get("play").get("placements")).removeAll();edit(dm,c,id,3,d);impact=tree(dm.post(target+"/migration-impact",Map.of("expectedVersion",1,"customizationVersion",4)),200);assertTrue(impact.get("blockers").isEmpty());
        tree(dm.post(target+"/migrations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"customizationVersion",4,"impactHash",impact.get("impactHash").asString(),"confirmations",impact.get("confirmations"))),200);assertTrue(dm.read(root+"/scene").get("tokens").isEmpty());assertEquals(1,jdbc.queryForObject("select count(*) from token where campaign_id=? and module_retired",Integer.class,UUID.fromString(live)));
    }
    @Test void liveDefinitionUpdatePreservesPlayAndRequiresFreshImpact()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;
        edit(dm,c,id,0,playable());String live=tree(dm.post(path+"/campaigns",fresh(1,"A03c party")),200).get("id").asString();
        var before=dm.read(instance(live));String root="/api/campaigns/"+live;
        String card=root+"/characters/"+before.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();
        tree(dm.post(card+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-5,"reason","Live damage")),200);var damaged=dm.read(card);
        var scene=dm.read(root+"/scene");var d=(tools.jackson.databind.node.ObjectNode)before.get("document").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(0)).put("text","Reviewed live preparation");edit(dm,c,id,1,d);
        var body=Map.of("expectedVersion",0,"customizationVersion",2);
        var impact=tree(dm.post(instance(live)+"/update-impact",body),200);
        assertEquals(before,dm.read(instance(live)));assertEquals(0,dm.read(instance(live)+"/updates").size());
        var apply=new LinkedHashMap<String,Object>(body);apply.put("requestId",UUID.randomUUID());apply.put("impactHash",impact.get("impactHash").asString());
        var wrong=new LinkedHashMap<>(apply);wrong.put("impactHash","0".repeat(64));tree(dm.post(instance(live)+"/updates",wrong),409);
        var ack=tree(dm.post(instance(live)+"/updates",apply),200);assertEquals(ack,tree(dm.post(instance(live)+"/updates",apply),200));
        var after=dm.read(instance(live));assertEquals(d,after.get("document"));assertEquals(before.get("mapping"),after.get("mapping"));assertEquals(before.get("progress"),after.get("progress"));assertEquals(damaged,dm.read(card));assertEquals(scene,dm.read(root+"/scene"));
        assertEquals(before,dm.read(instance(live)+"/updates").get(0).get("before"));
        tree(dm.post(instance(live)+"/update-impact",body),409);
        var player=member(UUID.fromString(live),"PLAYER");tree(player.post(instance(live)+"/updates",apply),403);tree(player.get(instance(live)+"/updates"),403);
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,dm.username);tree(dm.post(instance(live)+"/updates",apply),403);
    }
    @Test void liveDefinitionRejectsStructuralChangesAndRollsBackDeferredFailures()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;
        edit(dm,c,id,0,playable());String live=tree(dm.post(path+"/campaigns",fresh(1,"A03c atomic")),200).get("id").asString();String target=instance(live);
        var before=dm.read(target);var d=(tools.jackson.databind.node.ObjectNode)before.get("document").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0)).put("width",100);edit(dm,c,id,1,d);
        var review=Map.of("expectedVersion",0,"customizationVersion",2);var impact=tree(dm.post(target+"/update-impact",review),200);assertFalse(impact.get("blockers").isEmpty());
        var apply=new LinkedHashMap<String,Object>(review);apply.put("requestId",UUID.randomUUID());apply.put("impactHash",impact.get("impactHash").asString());tree(dm.post(target+"/updates",apply),409);assertEquals(before,dm.read(target));
        d=(tools.jackson.databind.node.ObjectNode)before.get("document").deepCopy();((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(0)).put("text","atomic preparation");edit(dm,c,id,2,d);
        apply.put("customizationVersion",3);impact=tree(dm.post(target+"/update-impact",Map.of("expectedVersion",0,"customizationVersion",3)),200);apply.put("impactHash",impact.get("impactHash").asString());
        String fn="a03c_fail_"+c.toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.result_kind='LIVE' then raise exception 'injected live failure'; end if; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){
            jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" "+(deferred?"after":"before")+" insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");
            tree(dm.post(target+"/updates",apply),500);assertEquals(before,dm.read(target));assertTrue(dm.read(target+"/updates").isEmpty());jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");
        }}finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        var ready=Map.copyOf(apply);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->dm.post(target+"/updates",ready));var b=pool.submit(()->dm.post(target+"/updates",ready));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}
        assertEquals(1,dm.read(target+"/updates").size());
        var changed=new LinkedHashMap<>(apply);changed.put("impactHash","1".repeat(64));tree(dm.post(target+"/updates",changed),409);
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",UUID.fromString(live),dm.username);tree(dm.post(target+"/updates",apply),403);tree(dm.get(target+"/updates"),403);
    }
    @Test void selectiveUpdateBindsImpactAndPersistsBeforeImageWithSingleEffectRetry()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),pkg=candidate(dm,c,id),path=copies(c)+"/"+id;var before=dm.read(path);
        var body=selection(pkg,0);int receipts=jdbc.queryForObject("select count(*) from module_lifecycle_receipt",Integer.class);
        var impact=tree(dm.post(path+"/update-impact",body),200);assertEquals(before,dm.read(path));assertEquals(receipts,jdbc.queryForObject("select count(*) from module_lifecycle_receipt",Integer.class));
        var apply=new LinkedHashMap<String,Object>(body);apply.put("requestId",UUID.randomUUID());apply.put("impactHash",impact.get("impactHash").asString());apply.put("sha256",impact.get("sha256").asString());
        var wrong=new LinkedHashMap<>(apply);wrong.put("impactHash","0".repeat(64));tree(dm.post(path+"/updates",wrong),409);
        var ack=tree(dm.post(path+"/updates",apply),200);assertEquals(1,ack.get("version").asLong());assertEquals(ack,tree(dm.post(path+"/updates",apply),200));
        var after=dm.read(path);assertEquals(before.get("baseline"),after.get("baseline"));assertEquals(before.get("sourcePackageId"),after.get("sourcePackageId"));assertEquals("upstream revision",after.get("document").get("nodes").get(0).get("text").asString());
        var history=dm.read(path+"/updates");assertEquals(1,history.size());assertEquals(before.get("document"),history.get(0).get("before").get("document"));assertEquals(pkg,history.get(0).get("packageId").asString());
        var recovered=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path+"/updates/1/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,recovered.statusCode());assertEquals(before.get("document"),json.valueToTree(new ModuleArchive().verify(recovered.body()).document()));
        assertEquals(ack,tree(dm.post(path+"/updates",apply),200));
        wrong.put("requestId",apply.get("requestId"));tree(dm.post(path+"/updates",wrong),409);
        tree(dm.post(path+"/updates",new LinkedHashMap<>(apply){{put("requestId",UUID.randomUUID());}}),409);
        var player=member(c,"PLAYER");tree(player.post(path+"/update-impact",body),403);tree(player.get(path+"/updates"),403);
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path+"/updates",apply),403);
    }
    @Test void selectiveUpdateRejectsIncompleteSelectionsAndRollsBackReceiptFailure()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),pkg=candidate(dm,c,id),path=copies(c)+"/"+id;var before=dm.read(path);
        tree(dm.post(path+"/update-impact",Map.of("expectedVersion",0,"packageId",pkg,"selections",List.of())),400);
        var body=selection(pkg,0);var impact=tree(dm.post(path+"/update-impact",body),200);var apply=new LinkedHashMap<String,Object>(body);UUID request=UUID.randomUUID();apply.put("requestId",request);apply.put("impactHash",impact.get("impactHash").asString());apply.put("sha256",impact.get("sha256").asString());
        jdbc.execute("alter table module_lifecycle_receipt add constraint reject_update_receipt check (request_id <> '"+request+"'::uuid)");
        try{tree(dm.post(path+"/updates",apply),500);assertEquals(before,dm.read(path));assertEquals(0,dm.read(path+"/updates").size());}finally{jdbc.execute("alter table module_lifecycle_receipt drop constraint reject_update_receipt");}
        tree(dm.post(path+"/updates",apply),200);
    }
    @Test void selectiveUpdateUsesSelectedImageBytesInSaveAsAndRecoversLocalBeforeImage()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=imageCopy(dm,c,List.of()),path=copies(c)+"/"+id;var before=dm.read(path);var archive=new ModuleArchive();var source=archive.verify(imageArchive(List.of()));
        var image=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,0x00ff00);var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);byte[] bytes=out.toByteArray();var m=source.manifest();
        var updated=new ModuleArchive.Manifest(m.format(),2,m.moduleId(),"2.0.0",m.name(),m.rules(),"Changed image rights",m.dependencies(),m.content(),List.of(new ModuleArchive.Asset(imageId,"image/png",bytes.length,ModuleArchive.hash(bytes))));
        String pkg=tree(upload(dm,c,command(2,false,false),archive.export(updated,source.document(),Map.of(imageId,bytes),m.moduleId(),"2.0.0",m.name())),200).get("id").asString();
        var options=tree(dm.post(path+"/update-options",Map.of("expectedVersion",0,"packageId",pkg)),200);var choices=new ArrayList<Map<String,String>>();for(var change:options.get("changes"))choices.add(Map.of("path",change.get("path").asString(),"choice","INCOMING"));assertTrue(choices.size()>=2);
        var body=new LinkedHashMap<String,Object>();body.put("expectedVersion",0);body.put("packageId",pkg);body.put("selections",choices);var impact=tree(dm.post(path+"/update-impact",body),200);body.put("requestId",UUID.randomUUID());body.put("sha256",impact.get("sha256").asString());body.put("impactHash",impact.get("impactHash").asString());tree(dm.post(path+"/updates",body),200);
        var saved=tree(dm.post(path+"/templates",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"moduleId",UUID.randomUUID(),"moduleVersion","1.0.0","name","Updated images")),200);byte[] exported=jdbc.queryForObject("select archive from module_package where id=?",byte[].class,UUID.fromString(saved.get("id").asString()));assertArrayEquals(bytes,archive.images(exported).get(imageId));assertEquals("Changed image rights",archive.verify(exported).manifest().rights());assertEquals(before.get("baseline"),dm.read(path).get("baseline"));
    }
    @Test void selectiveUpdateDeferredCommitFailureLeavesNoArchiveEvidenceOrReceipt()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),pkg=candidate(dm,c,id),path=copies(c)+"/"+id;var before=dm.read(path);var body=new LinkedHashMap<String,Object>(selection(pkg,0));var plan=tree(dm.post(path+"/update-impact",body),200);body.put("requestId",UUID.randomUUID());body.put("sha256",plan.get("sha256").asString());body.put("impactHash",plan.get("impactHash").asString());String fn="a03_fail_"+c.toString().replace("-","");
        jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.campaign_id='"+c+"'::uuid and new.result_kind='UPDATE' then raise exception 'injected deferred update failure'; end if; return new; end $$");
        jdbc.execute("create constraint trigger "+fn+" after insert on module_lifecycle_receipt deferrable initially deferred for each row execute function "+fn+"()");
        try{tree(dm.post(path+"/updates",body),500);assertEquals(before,dm.read(path));assertEquals(0,dm.read(path+"/updates").size());assertEquals(0,jdbc.queryForObject("select count(*) from module_customization_archive where customization_id=?",Integer.class,UUID.fromString(id)));}finally{jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        tree(dm.post(path+"/updates",body),200);
    }
    @Test void selectiveUpdateConcurrentRequestsApplyOnceAndPreserveLiveState()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),pkg=candidate(dm,c,id),path=copies(c)+"/"+id;
        var body=selection(pkg,0);var impact=tree(dm.post(path+"/update-impact",body),200);var apply=new LinkedHashMap<String,Object>(body);apply.put("requestId",UUID.randomUUID());apply.put("sha256",impact.get("sha256").asString());apply.put("impactHash",impact.get("impactHash").asString());
        try(var pool=Executors.newFixedThreadPool(2)){var f=pool.submit(()->dm.post(path+"/updates",apply));var s=pool.submit(()->dm.post(path+"/updates",apply));assertEquals(tree(f.get(),200),tree(s.get(),200));}
        assertEquals(1,dm.read(path+"/updates").size());assertEquals(1,dm.read(path).get("version").asLong());
        // An existing live campaign retains damaged HP, progress and old messages.
        edit(dm,c,id,1,playable());String live=tree(dm.post(path+"/campaigns",fresh(2,"Live party")),200).get("id").asString();var instanceBefore=dm.read(instance(live));String root="/api/campaigns/"+live;String card=root+"/characters/"+instanceBefore.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString();
        tree(dm.post(card+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-5,"reason","Live damage")),200);var cardBefore=dm.read(card);
        var message=tree(dm.post(root+"/messages",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"kind","OOC","characterId","","text","Persistent RP","attachments",List.of(),"replyTo","")),200);var messageBefore=dm.read(root+"/timeline/"+message.get("id").asString());
        var options=tree(dm.post(path+"/update-options",Map.of("expectedVersion",2,"packageId",pkg)),200);var choices=new ArrayList<Map<String,String>>();for(var change:options.get("changes"))choices.add(Map.of("path",change.get("path").asString(),"choice","KEEP"));
        var request=new LinkedHashMap<String,Object>();request.put("expectedVersion",2);request.put("packageId",pkg);request.put("selections",choices);var plan=tree(dm.post(path+"/update-impact",request),200);request.put("requestId",UUID.randomUUID());request.put("sha256",plan.get("sha256").asString());request.put("impactHash",plan.get("impactHash").asString());tree(dm.post(path+"/updates",request),200);
        assertEquals(instanceBefore,dm.read(instance(live)));assertEquals(cardBefore,dm.read(card));assertEquals(messageBefore,dm.read(root+"/timeline/"+message.get("id").asString()));
    }
    @Test void selectiveUpdateReportsButCannotApplyMissingExactDependency()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id;var before=dm.read(path);
        String pkg=tree(upload(dm,c,command(2,false,false),archive("2.0.0","5R",List.of(Map.of("id",module,"version","missing","rules","5R")),"Missing")),200).get("id").asString();
        var options=tree(dm.post(path+"/update-options",Map.of("expectedVersion",0,"packageId",pkg)),200);assertEquals(1,options.get("changes").size());
        var body=Map.of("expectedVersion",0,"packageId",pkg,"selections",List.of(Map.of("path","/manifest/dependencies/"+module,"choice","INCOMING")));var plan=tree(dm.post(path+"/update-impact",body),200);assertEquals(1,plan.get("missingDependencies").size());
        var apply=new LinkedHashMap<String,Object>(body);apply.put("requestId",UUID.randomUUID());apply.put("sha256",plan.get("sha256").asString());apply.put("impactHash",plan.get("impactHash").asString());tree(dm.post(path+"/updates",apply),409);assertEquals(before,dm.read(path));
    }
    @Test void updatePreviewComparesThreeVersionsWithoutMutatingCopiesOrLiveCampaigns()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);String pkg=candidate(dm,c,id);String path=copies(c)+"/"+id;
        var local=(tools.jackson.databind.node.ObjectNode)dm.read(path).get("document").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)local.get("nodes").get(0)).put("text","local revision");edit(dm,c,id,0,local);
        var before=dm.read(path);int receipts=jdbc.queryForObject("select count(*) from module_lifecycle_receipt",Integer.class);
        var request=Map.of("expectedVersion",1,"packageId",pkg);
        var preview=tree(dm.post(path+"/update-preview",request),200);
        assertEquals(1,preview.get("version").asLong());assertEquals(pkg,preview.get("packageId").asString());assertEquals(1,preview.get("changes").size());
        var change=preview.get("changes").get(0);assertTrue(change.get("conflict").asBoolean());assertEquals("local revision",change.get("current").asString());assertEquals("upstream revision",change.get("incoming").asString());
        assertEquals(preview,tree(dm.post(path+"/update-preview",request),200));assertEquals(before,dm.read(path));assertEquals(receipts,jdbc.queryForObject("select count(*) from module_lifecycle_receipt",Integer.class));
        edit(dm,c,id,1,playable());String target=tree(dm.post(path+"/campaigns",fresh(2,"In progress")),200).get("id").asString();
        var live=dm.read(instance(target));String targetRoot="/api/campaigns/"+target;var scene=dm.read(targetRoot+"/scene");var card=dm.read(targetRoot+"/characters/"+live.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString());
        tree(dm.post(path+"/update-preview",Map.of("expectedVersion",2,"packageId",pkg)),200);
        assertEquals(live,dm.read(instance(target)));assertEquals(scene,dm.read(targetRoot+"/scene"));assertEquals(card,dm.read(targetRoot+"/characters/"+live.get("mapping").get("units").get("10000000-0000-4000-8000-000000000002").asString()));
    }
    @Test void updatePreviewChecksCurrentDmVersionScopeAndStrictBody()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);String pkg=candidate(dm,c,id);String path=copies(c)+"/"+id+"/update-preview";
        var body=Map.of("expectedVersion",0,"packageId",pkg);var player=member(c,"PLAYER");tree(player.post(path,body),403);
        tree(dm.post(path,Map.of("expectedVersion",1,"packageId",pkg)),409);tree(dm.post(path,Map.of("expectedVersion",0,"packageId",UUID.randomUUID())),404);
        tree(dm.post(path,Map.of("expectedVersion",0,"packageId",pkg,"requestId",UUID.randomUUID())),400);
        tree(dm.post(path,Map.of("expectedVersion",0,"packageId",dm.read(copies(c)+"/"+id).get("sourcePackageId").asString())),409);
        tree(dm.post(copies(campaign(dm))+"/"+id+"/update-preview",body),404);
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path,body),403);
    }
    @Test void updatePreviewRequiresSameModuleAndRulesButReportsExactMissingDependencies()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c),path=copies(c)+"/"+id+"/update-preview";
        var a=new ModuleArchive();var original=a.verify(archive());
        byte[] other=a.export(original.manifest(),original.document(),Map.of(),UUID.randomUUID().toString(),"2.0.0","Other module");
        String pkg=tree(upload(dm,c,command(2,false,false),other),200).get("id").asString();tree(dm.post(path,Map.of("expectedVersion",0,"packageId",pkg)),409);
        pkg=tree(upload(dm,c,command(3,false,true),archive("3.0.0","2014",List.of(),"Other rules")),200).get("id").asString();tree(dm.post(path,Map.of("expectedVersion",0,"packageId",pkg)),409);
        pkg=tree(upload(dm,c,command(4,false,false),archive("4.0.0","5R",List.of(Map.of("id",module,"version","exact-missing","rules","5R")),"Missing dependency")),200).get("id").asString();
        var response=dm.post(path,Map.of("expectedVersion",0,"packageId",pkg));assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElse(""));assertEquals("nosniff",response.headers().firstValue("X-Content-Type-Options").orElse(""));
        var preview=tree(response,200);assertEquals("exact-missing",preview.get("missingDependencies").get(0).get("version").asString());
        var noCsrf=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("expectedVersion",0,"packageId",pkg)))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(403,noCsrf.statusCode());
    }
    tools.jackson.databind.node.ObjectNode playable(){
        var n=json.createObjectNode();n.set("nodes",json.valueToTree(List.of(
            Map.of("id",module,"kind","SCENE","name","入口","text","故事起点","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()),
            Map.of("id","10000000-0000-4000-8000-000000000002","kind","UNIT","name","守卫","text","","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()),
            Map.of("id","10000000-0000-4000-8000-000000000003","kind","DOCUMENT","name","线索","text","原始剧情","visibility","PUBLIC","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()))));
        var s=io.dndroll.domain.CharacterSheet.empty("守卫");var sheet=s.change(s.name(),new io.dndroll.domain.CharacterSheet.Combat(20,20,0,30,0,0,"MANUAL",12,io.dndroll.domain.CharacterSheet.manual()),List.of(new io.dndroll.domain.CharacterSheet.Resource("uses","次数","CUSTOM",3,3,"手动",io.dndroll.domain.CharacterSheet.manual())),List.of(),List.of());
        var scene=new LinkedHashMap<String,Object>();scene.put("nodeId",module);scene.put("width",20);scene.put("height",15);scene.put("gridSize",48);scene.put("map",null);
        n.set("play",json.valueToTree(Map.of("scenes",List.of(scene),"units",List.of(Map.of("nodeId","10000000-0000-4000-8000-000000000002","sheet",sheet,"hidden",true,"shareFull",false)),"placements",List.of(Map.of("id","10000000-0000-4000-8000-000000000004","sceneId",module,"unitId","10000000-0000-4000-8000-000000000002","x",2,"y",3,"hidden",true)),"admissionMode","REVIEW","approvedDependencyIds",List.of())));return n;
    }
    void edit(Browser dm,UUID c,String id,long version,Object doc)throws Exception{tree(dm.post(copies(c)+"/"+id,Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"name","定制副本","document",doc)),200);}
    @Test void twoCampaignsHaveIndependentResourcesProgressAndPlacements()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);edit(dm,c,id,0,playable());
        var first=fresh(1,"第一团");String a=tree(dm.post(copies(c)+"/"+id+"/campaigns",first),200).get("id").asString();String b=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"第二团")),200).get("id").asString();assertNotEquals(a,b);
        var ia=dm.read(instance(a));var ib=dm.read(instance(b));String unit="10000000-0000-4000-8000-000000000002";
        String ca=ia.get("mapping").get("units").get(unit).asString(),cb=ib.get("mapping").get("units").get(unit).asString();assertNotEquals(ca,cb);assertNotEquals(ia.get("mapping").get("scenes"),ib.get("mapping").get("scenes"));
        String cardsA="/api/campaigns/"+a+"/characters/"+ca,cardsB="/api/campaigns/"+b+"/characters/"+cb;
        tree(dm.post(cardsA+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-5,"reason","第一团战斗")),200);
        tree(dm.post(cardsA+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"resourceId","uses","delta",-1,"reason","第一团资源")),200);
        var progress=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"progress",Map.of(module,"第一章完成"));var ack=tree(dm.post(instance(a),progress),200);assertEquals(ack,tree(dm.post(instance(a),progress),200));
        assertEquals(15,dm.read(cardsA).get("sheet").get("combat").get("hp").asInt());assertEquals(20,dm.read(cardsB).get("sheet").get("combat").get("hp").asInt());assertEquals(3,dm.read(cardsB).get("sheet").get("resources").get(0).get("current").asInt());assertTrue(dm.read(instance(b)).get("progress").isEmpty());assertEquals("第一章完成",dm.read(instance(a)).get("progress").get(module).asString());
        assertEquals(a,tree(dm.post(copies(c)+"/"+id+"/campaigns",first),200).get("id").asString());
        var player=member(UUID.fromString(a),"PLAYER");tree(player.get(instance(a)),403);assertEquals(0,player.read("/api/campaigns/"+a+"/scene").get("tokens").size());assertFalse(player.read("/api/campaigns/"+a+"/characters").toString().contains("守卫"));
    }
    @Test void saveAsTemplateNeverOverwritesBaselineCampaignOrOldRecords()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var original=dm.read(copies(c)+"/"+id);edit(dm,c,id,0,playable());
        String a=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"旧团")),200).get("id").asString();var runtime=dm.read(instance(a));var scene=dm.read("/api/campaigns/"+a+"/scene");
        var message=tree(dm.post("/api/campaigns/"+a+"/messages",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"kind","OOC","characterId","","text","不可被模板替换的旧记录","attachments",List.of(),"replyTo","")),200);String entry="/api/campaigns/"+a+"/timeline/"+message.get("id").asString();var oldRecord=dm.read(entry);
        var changed=playable();((tools.jackson.databind.node.ObjectNode)changed.get("nodes").get(2)).put("text","新剧情");edit(dm,c,id,1,changed);
        var body=Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"moduleId",UUID.randomUUID(),"moduleVersion","1.0.0","name","另存模板");var saved=tree(dm.post(copies(c)+"/"+id+"/templates",body),200);assertEquals(saved,tree(dm.post(copies(c)+"/"+id+"/templates",body),200));
        assertEquals(oldRecord,dm.read(entry));assertEquals(runtime,dm.read(instance(a)));assertEquals(scene,dm.read("/api/campaigns/"+a+"/scene"));assertEquals(original.get("baseline"),dm.read(copies(c)+"/"+id).get("baseline"));
        var listing=dm.read(base(c));assertEquals(2,listing.get("packages").size());assertEquals(2,listing.get("packages").get(1).get("manifest").get("formatVersion").asInt());
        String next=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",listing.get("campaignVersion").asLong(),"packageId",saved.get("id").asString(),"name","导出模板再定制")),200).get("id").asString();assertEquals(changed,dm.read(copies(c)+"/"+next).get("document"));
    }
    @Test void authorizationVersionsConcurrentRetriesAndIndependentTargetMembership()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);var player=member(c,"PLAYER");var other=member(c,"DM");
        tree(player.get(copies(c)),403);tree(player.get(copies(c)+"/"+id),403);tree(new Browser().get(copies(c)),401);tree(player.post(copies(c)+"/"+id+"/campaigns",fresh(0,"越权")),403);
        var request=fresh(0,"并发新团");JsonNode ack;
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->dm.post(copies(c)+"/"+id+"/campaigns",request));var b=pool.submit(()->dm.post(copies(c)+"/"+id+"/campaigns",request));ack=tree(a.get(),200);assertEquals(ack,tree(b.get(),200));
            var doc=playable();var edit=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","并发修改","document",doc);var one=pool.submit(()->dm.post(copies(c)+"/"+id,edit));var two=pool.submit(()->other.post(copies(c)+"/"+id,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","另一修改","document",doc)));assertEquals(List.of(200,409),java.util.stream.Stream.of(one.get().statusCode(),two.get().statusCode()).sorted().toList());
        }
        var changed=new HashMap<>(request);changed.put("name","changed");tree(dm.post(copies(c)+"/"+id+"/campaigns",changed),409);tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(0,"stale")),409);
        String target=ack.get("id").asString();jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(copies(c)+"/"+id+"/campaigns",request),403);assertEquals(0,dm.read(instance(target)).get("version").asInt());
        jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,dm.username);jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",UUID.fromString(target),dm.username);tree(dm.post(copies(c)+"/"+id+"/campaigns",request),403);
        UUID foreign=campaign(dm);tree(dm.get(copies(foreign)+"/"+id),404);assertEquals("no-store, private",dm.get(copies(c)).headers().firstValue("Cache-Control").orElse(""));
    }
    @Test void malformedTypedDataAndDuplicateJsonKeysCannotMutateCopies()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=copy(dm,c);String path=copies(c)+"/"+id;
        var before=dm.read(path);for(String jsonText:List.of("{\"requestId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"name\":\"bad\",\"name\":\"duplicate\",\"document\":"+playable()+"}")){
            var r=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",dm.csrf).POST(HttpRequest.BodyPublishers.ofString(jsonText)).build(),HttpResponse.BodyHandlers.ofString());tree(r,400);
        }
        assertEquals(before,dm.read(path));
        var invalid=playable();((tools.jackson.databind.node.ObjectNode)invalid.get("play").get("units").get(0)).put("hidden","false");tree(dm.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","invalid","document",invalid)),400);
        invalid=playable();((tools.jackson.databind.node.ObjectNode)invalid.get("play").get("placements").get(0)).put("x",20);tree(dm.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","invalid","document",invalid)),400);
        assertEquals(before,dm.read(path));
        var noCsrf=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+copies(c))).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());assertEquals(403,noCsrf.statusCode());
    }
    @Test void missingDependenciesAndLegacyRulesStayInertUntilExplicitlyAdapted()throws Exception{
        var dm=dm();UUID c=campaign(dm);byte[] bytes=archive("1.0.0","5R",List.of(Map.of("id",module,"version","exact","rules","5R")),"缺依赖");String pkg=tree(upload(dm,c,command(0,false,false),bytes),200).get("id").asString();String id=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"packageId",pkg,"name","缺依赖副本")),200).get("id").asString();assertEquals(1,dm.read(copies(c)+"/"+id).get("missingDependencies").size());tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(0,"blocked")),409);
        pkg=tree(upload(dm,c,command(2,false,true),archive("2.0.0","2014",List.of(),"旧版")),200).get("id").asString();id=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"packageId",pkg,"name","旧版副本")),200).get("id").asString();tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(0,"blocked")),409);
    }
    final String imageId="10000000-0000-4000-8000-000000000010";
    byte[] png()throws Exception{var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(12,10,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    byte[] imageArchive(List<?> dependencies)throws Exception{
        byte[] image=png(),content=json.writeValueAsBytes(Map.of("nodes",List.of()));var manifest=Map.of("format","dnd-roll.module","formatVersion",1,"moduleId",module,"moduleVersion","1.0.0","name","有图原件","rules","5R","rights","Original test image","dependencies",dependencies,"content",Map.of("size",content.length,"sha256",ModuleArchive.hash(content)),"assets",List.of(Map.of("id",imageId,"mediaType","image/png","size",image.length,"sha256",ModuleArchive.hash(image))));var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){for(var e:Map.of("manifest.json",json.writeValueAsBytes(manifest),"content.json",content,"assets/"+imageId,image).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}return out.toByteArray();
    }
    String imageCopy(Browser dm,UUID c,List<?> dependencies)throws Exception{String pkg=tree(upload(dm,c,command(0,false,false),imageArchive(dependencies)),200).get("id").asString();return tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"packageId",pkg,"name","图像定制")),200).get("id").asString();}
    tools.jackson.databind.node.ObjectNode imageDocument(){var d=playable();((tools.jackson.databind.node.ObjectNode)d.get("play").get("scenes").get(0)).set("map",json.valueToTree(Map.of("assetId",imageId,"gridSize",48,"offsetX",0,"offsetY",0,"distancePerCell",5)));((tools.jackson.databind.node.ObjectNode)d.get("nodes").get(2)).set("assetIds",json.valueToTree(List.of(imageId)));return d;}
    Set<String> files()throws Exception{var path=java.nio.file.Path.of(root);if(!java.nio.file.Files.exists(path))return Set.of();try(var stream=java.nio.file.Files.walk(path)){return stream.filter(java.nio.file.Files::isRegularFile).map(Object::toString).collect(java.util.stream.Collectors.toSet());}}
    @Test void mapsDocumentsCustomDependenciesAndMultiplePlacementsAreCopiedWithFreshIdentities()throws Exception{
        var dm=dm();UUID c=campaign(dm);var entry=Map.of("name","自制规则","aliases",List.of(),"type","RULE","source",Map.of("book","原创","revision","exact-v2","rules","5R","detail",""),"description","仅说明","spellLevel",0,"dice","","damageType","","rights","Original fixture");var published=tree(dm.post("/api/campaigns/"+c+"/content/custom",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"entry",entry)),200);String dep=published.get("id").asString();
        // Import/create versions account for the custom catalog entry.
        String pkg=tree(upload(dm,c,command(1,false,false),imageArchive(List.of(Map.of("id",dep,"version","exact-v2","rules","5R")))),200).get("id").asString();String id=tree(dm.post(copies(c),Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"packageId",pkg,"name","图像及规则定制")),200).get("id").asString();var d=imageDocument();((tools.jackson.databind.node.ObjectNode)d.get("play")).set("approvedDependencyIds",json.valueToTree(List.of(dep)));var placements=(tools.jackson.databind.node.ArrayNode)d.get("play").get("placements");var extra=(tools.jackson.databind.node.ObjectNode)placements.get(0).deepCopy();extra.put("id","10000000-0000-4000-8000-000000000005");extra.put("x",4);placements.add(extra);edit(dm,c,id,0,d);
        String a=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"图片新团")),200).get("id").asString();var i=dm.read(instance(a));String asset=i.get("mapping").get("assets").get(imageId).asString();assertNotEquals(imageId,asset);String root="/api/campaigns/"+a;var map=dm.read(root+"/scenes/"+i.get("mapping").get("scenes").get(module).asString()+"/map");assertEquals(asset,map.get("map").get("assetId").asString());var tokens=dm.read(root+"/scene").get("tokens");assertEquals(2,tokens.size());assertEquals(1,jdbc.queryForObject("select count(distinct character_id) from token where campaign_id=?",Integer.class,UUID.fromString(a)));String mapped=i.get("mapping").get("dependencies").get(dep).asString();assertNotEquals(dep,mapped);assertEquals(mapped,dm.read(root+"/content").get("approvedIds").get(0).asString());
        var player=member(UUID.fromString(a),"PLAYER");tree(player.get(root+"/assets/"+asset+"/content"),403);var visible=player.read(root+"/notes");assertEquals(1,visible.get("notes").size());String attachment=visible.get("notes").get(0).get("attachments").get(0).get("id").asString();var response=player.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+root+"/note-attachments/"+attachment+"/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,response.statusCode());assertArrayEquals(png(),response.body());assertNotEquals(imageId,attachment);
        var save=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"moduleId",UUID.randomUUID(),"moduleVersion","1.0.0","name","有图导出");String saved=tree(dm.post(copies(c)+"/"+id+"/templates",save),200).get("id").asString();byte[] exported=jdbc.queryForObject("select archive from module_package where id=?",byte[].class,UUID.fromString(saved));var verified=new ModuleArchive().verify(exported);assertEquals(2,verified.document().play().placements().size());assertArrayEquals(png(),new ModuleArchive().images(exported).get(imageId));var preview=tree(upload(dm,c,"/preview",null,exported,true,""),200);assertEquals(json.valueToTree(verified.document()),preview.get("document"));
    }
    @Test void immediateAndDeferredReceiptFailureRollBackCampaignFilesHistoriesAndRetry()throws Exception{
        var dm=dm();UUID c=campaign(dm);String id=imageCopy(dm,c,List.of());edit(dm,c,id,0,imageDocument());var before=files();String name="A02 atomic "+UUID.randomUUID();var request=fresh(1,name);String fn="a02_fail_"+c.toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.campaign_id='"+c+"'::uuid and new.result_kind='CAMPAIGN' then raise exception 'injected A02 receipt failure'; end if; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" "+(deferred?"after":"before")+" insert on module_lifecycle_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");tree(dm.post(copies(c)+"/"+id+"/campaigns",request),500);assertEquals(0,jdbc.queryForObject("select count(*) from campaign where name=?",Integer.class,name));assertEquals(before,files());assertEquals(1,dm.read(copies(c)+"/"+id).get("version").asInt());assertEquals(0,jdbc.queryForObject("select count(*) from module_lifecycle_receipt where actor=? and request_id=?",Integer.class,dm.username,request.get("requestId")));jdbc.execute("drop trigger "+fn+" on module_lifecycle_receipt");}}
        finally{jdbc.execute("drop trigger if exists "+fn+" on module_lifecycle_receipt");jdbc.execute("drop function "+fn+"()");}
        var result=tree(dm.post(copies(c)+"/"+id+"/campaigns",request),200);assertEquals(result,tree(dm.post(copies(c)+"/"+id+"/campaigns",request),200));assertEquals(1,jdbc.queryForObject("select count(*) from campaign where name=?",Integer.class,name));assertEquals(before.size()+2,files().size());
    }
    @Test void builtinDependencyApprovalDoesNotPreventLaterContentPolicyEdits()throws Exception{
        var dm=dm();UUID c=campaign(dm);var builtin=dm.read("/api/campaigns/"+c+"/content").get("entries").get(0);String dependency=builtin.get("id").asString();var source=builtin.get("source");String id=imageCopy(dm,c,List.of(Map.of("id",dependency,"version",source.get("revision").asString(),"rules",source.get("rules").asString())));
        var definition=imageDocument();((tools.jackson.databind.node.ObjectNode)definition.get("play")).set("approvedDependencyIds",json.valueToTree(List.of(dependency)));edit(dm,c,id,0,definition);
        String target=tree(dm.post(copies(c)+"/"+id+"/campaigns",fresh(1,"内置规则新团")),200).get("id").asString(),path="/api/campaigns/"+target+"/content";var policy=dm.read(path);
        tree(dm.post(path+"/settings",Map.of("requestId",UUID.randomUUID(),"expectedVersion",policy.get("campaignVersion").asLong(),"mode","FREE","approvedIds",policy.get("approvedIds"))),200);
        assertEquals("FREE",dm.read(path).get("mode").asString());assertTrue(dm.read(path).get("approvedIds").isEmpty());
    }
}
