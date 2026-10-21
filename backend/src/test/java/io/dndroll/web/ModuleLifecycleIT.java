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
