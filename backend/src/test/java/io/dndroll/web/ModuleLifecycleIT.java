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
