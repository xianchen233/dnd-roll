package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import io.dndroll.application.AssetService;
import io.dndroll.persistence.JdbcAssetStore;
import io.dndroll.persistence.PrivateAssetFiles;
import io.dndroll.application.SceneManagementService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=", "BOOTSTRAP_USERNAME=p02_owner", "BOOTSTRAP_PASSWORD=integration-owner-password", "assets.storage-root=${java.io.tmpdir}/dnd-roll-assets-it"})
class AssetsIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired SceneManagementService service;
    @Autowired PasswordEncoder passwords;
    @Autowired AssetService assets;
    @Autowired JdbcAssetStore assetStore;
    @Autowired PrivateAssetFiles files;
    @Autowired SceneEvents events;
    final JsonMapper json = JsonMapper.builder().build();
    final Set<UUID> campaigns = new HashSet<>();
    final Set<String> users = new HashSet<>();
    @DynamicPropertySource static void database(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", () -> FirstSliceIT.required("TEST_DATABASE_URL"));
        props.add("spring.datasource.username", () -> FirstSliceIT.required("TEST_DATABASE_USERNAME"));
        props.add("spring.datasource.password", () -> FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
    }
    class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String csrf;
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(String path, Object body) throws Exception { return post(path, json.writeValueAsString(body), true); }
        HttpResponse<String> post(String path, String body, boolean csrfHeader) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
            if (csrfHeader) request.header("X-CSRF-TOKEN", csrf);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        Browser() throws Exception { refreshCsrf(); }
        void refreshCsrf() throws Exception { csrf = tree(get("/api/auth/csrf"), 200).get("token").asString(); }
        void login(String name) throws Exception { tree(post("/api/auth/login", Map.of("username", name, "password", "integration-owner-password")), 200); refreshCsrf(); }
        JsonNode read(String path) throws Exception { return tree(get(path), 200); }
    }
    Browser owner() throws Exception { var browser = new Browser(); browser.login("p02_owner"); return browser; }
    Browser member(UUID campaign, String role) throws Exception {
        String name = "p02_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12); users.add(name);
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)", UUID.randomUUID(), name, passwords.encode("integration-owner-password"));
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?)", campaign, name, role);
        var browser = new Browser(); browser.login(name); return browser;
    }
    JsonNode tree(HttpResponse<String> response, int status) throws Exception { assertEquals(status, response.statusCode(), response.body()); return json.readTree(response.body()); }
    void error(HttpResponse<String> response, int status, String code) throws Exception { assertEquals(code, tree(response, status).get("code").asString()); }
    String base(UUID id) { return "/api/campaigns/" + id; }
    Map<String, Object> command(UUID request, long version) { return Map.of("requestId", request, "expectedVersion", version); }
    Map<String, Object> command(long version) { return command(UUID.randomUUID(), version); }
    Map<String, Object> createBody(UUID request, long version, String name) { return Map.of("requestId", request, "expectedVersion", version, "name", name); }
    UUID campaign(Browser b) throws Exception {
        var created = tree(b.post("/api/campaigns", Map.of("requestId", UUID.randomUUID(), "name", "P02 test campaign")), 201);
        var id = UUID.fromString(created.get("id").asString()); campaigns.add(id); return id;
    }
    @AfterEach void cleanup() {
        for (var id : campaigns) {
            for (var key : jdbc.queryForList("select storage_key from campaign_asset where campaign_id=?", String.class, id)) files.discard(key);
            jdbc.update("delete from asset_command_receipt where campaign_id=?", id);
            jdbc.update("delete from scene_map where campaign_id=?", id);
            jdbc.update("delete from campaign_asset where campaign_id=?", id);
            jdbc.update("update campaign set current_scene_id=null where id=?", id);
            jdbc.update("delete from scene_command_receipt where campaign_id=?", id);
            jdbc.update("delete from token_controller where token_id in (select t.id from token t join scene s on s.id=t.scene_id where s.campaign_id=?)", id);
            jdbc.update("delete from movement_receipt where campaign_id=?", id);
            jdbc.update("delete from token where scene_id in (select id from scene where campaign_id=?)", id);
            jdbc.update("delete from scene where campaign_id=?", id);
            jdbc.update("delete from campaign_invitation where campaign_id=?", id);
            jdbc.update("delete from campaign_member where campaign_id=?", id);
            jdbc.update("delete from campaign where id=?", id);
        }
        jdbc.update("delete from account_command_receipt where actor='p02_owner'");
        for (var name : users) { jdbc.update("delete from scene_command_receipt where actor=?", name); jdbc.update("delete from app_user where username=?", name); }
    }
    @Test void privateAssetListingStartsEmptyWithManagementVersion() throws Exception {
        var dm = owner(); var id = campaign(dm);
        var list = tree(dm.get(base(id) + "/assets"), 200);
        assertEquals(0, list.get("campaignVersion").asLong()); assertEquals(0, list.get("assets").size());
    }
    byte[] image(String format, int width, int height) throws Exception {
        var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),format,out); return out.toByteArray();
    }
    HttpResponse<String> upload(Browser browser, UUID id, UUID request, long version, byte[] bytes, String media) throws Exception {
        return upload(browser, id, json.writeValueAsString(command(request,version)), bytes, media, true, "");
    }
    HttpResponse<String> upload(Browser browser, UUID id, String commandJson, byte[] bytes, String media, boolean csrf, String extra) throws Exception {
        String boundary = "p02b-" + UUID.randomUUID(); var body = new ByteArrayOutputStream();
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"command\"\r\nContent-Type: application/json\r\n\r\n"+commandJson+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"../../private-original-name.png\"\r\nContent-Type: "+media+"\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.write(bytes); body.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if(!extra.isEmpty()) body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+extra+"\"\r\nContent-Type: text/plain\r\n\r\nunexpected\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.write(("--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(id)+"/assets")).header("Content-Type","multipart/form-data; boundary="+boundary);
        if(csrf) builder.header("X-CSRF-TOKEN",browser.csrf);
        return browser.client.send(builder.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    UUID uploadAsset(Browser dm, UUID id, long version) throws Exception {
        return UUID.fromString(tree(upload(dm,id,UUID.randomUUID(),version,image("png",3,2),"image/png"),200).get("asset").get("id").asString());
    }
    UUID scene(Browser dm, UUID id) throws Exception { return UUID.fromString(dm.read(base(id)+"/scene").get("id").asString()); }
    String mapPath(UUID id, UUID scene) { return base(id)+"/scenes/"+scene+"/map"; }
    String contentPath(UUID id, UUID asset) { return base(id)+"/assets/"+asset+"/content"; }
    Map<String,Object> config(UUID asset) { return Map.of("assetId",asset,"gridSize",64,"offsetX",-12,"offsetY",13,"distancePerCell",5.0); }
    Map<String,Object> mapCommand(UUID request,long version,Object map) { var result=new HashMap<String,Object>(command(request,version)); result.put("map",map); return result; }
    void privateHeaders(HttpResponse<?> response) {
        assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElse(""));
        assertEquals("nosniff",response.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertTrue(response.headers().firstValue("ETag").isEmpty());
    }
    Set<String> keys(JsonNode object) { var keys=new HashSet<String>();object.propertyNames().forEach(keys::add);return keys; }
    @Test void uploadPublishesIndependentPrivateFileAndDownloadHasNoCacheCapability() throws Exception {
        var dm=owner();var id=campaign(dm);byte[] png=image("png",7,4);var request=UUID.randomUUID();
        var result=tree(upload(dm,id,request,0,png,"image/png"),200); var asset=result.get("asset");UUID assetId=UUID.fromString(asset.get("id").asString());
        assertEquals(Set.of("asset","campaignVersion"),keys(result));assertEquals(Set.of("id","mediaType","byteSize","width","height"),keys(asset));
        assertEquals(7,asset.get("width").asInt());assertEquals(4,asset.get("height").asInt());assertEquals(png.length,asset.get("byteSize").asInt());assertEquals(1,result.get("campaignVersion").asLong());
        String key=jdbc.queryForObject("select storage_key from campaign_asset where id=?",String.class,assetId);assertNotEquals(assetId.toString(),key);assertArrayEquals(png,files.read(key));
        assertEquals(result,tree(upload(dm,id,request,0,png,"image/png"),200));assertEquals(1,dm.read(base(id)+"/assets").get("assets").size());
        var download=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+contentPath(id,assetId))).header("If-None-Match","*").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,download.statusCode());assertArrayEquals(png,download.body());privateHeaders(download);
        assertEquals("image/png",download.headers().firstValue("Content-Type").orElse(""));assertEquals("attachment",download.headers().firstValue("Content-Disposition").orElse(""));
        assertFalse(result.toString().contains("private-original-name"));assertFalse(result.toString().contains(key));
        var jpeg=tree(upload(dm,id,UUID.randomUUID(),1,image("jpeg",2,3),"image/jpeg"),200);assertEquals("image/jpeg",jpeg.get("asset").get("mediaType").asString());
    }
    @Test void inactiveMapConfigurationPreservesLogicalCanvasAndHistoricalRetryNeverReattaches() throws Exception {
        var dm=owner();var id=campaign(dm);var player=member(id,"PLAYER");UUID current=scene(dm,id);var original=player.read(base(id)+"/scene");
        UUID inactive=UUID.fromString(tree(dm.post(base(id)+"/scenes",createBody(UUID.randomUUID(),0,"Private prep")),200).get("id").asString());
        UUID asset=uploadAsset(dm,id,1);var request=UUID.randomUUID();String path=mapPath(id,inactive);var body=mapCommand(request,0,config(asset));
        var result=tree(dm.post(path,body),200);assertEquals(1,result.get("sceneVersion").asLong());assertEquals(asset.toString(),result.get("map").get("assetId").asString());
        assertEquals(original,player.read(base(id)+"/scene"));assertEquals(current,scene(dm,id));assertEquals(2,dm.read(base(id)+"/assets").get("campaignVersion").asLong());
        var detached=tree(dm.post(path,mapCommand(UUID.randomUUID(),1,null)),200);assertTrue(detached.get("map").isNull());assertEquals(2,detached.get("sceneVersion").asLong());
        assertEquals(result,tree(dm.post(path,body),200));assertEquals(detached,dm.read(path));
        assertEquals(48,jdbc.queryForObject("select grid_size from scene where id=?",Integer.class,inactive));
        tree(dm.post(mapPath(id,current),mapCommand(UUID.randomUUID(),0,config(asset))),200);
        var snapshot=player.read(base(id)+"/scene");assertEquals(Set.of("id","campaignId","name","width","height","gridSize","version","tokens"),keys(snapshot));
        assertFalse(snapshot.toString().contains(asset.toString()));assertFalse(snapshot.toString().contains("storage"));
    }
    @Test void everyRouteRequiresCurrentDmAndCsrfAndForeignIdsStayScoped() throws Exception {
        var dm=owner();var id=campaign(dm);var foreign=campaign(dm);var player=member(id,"PLAYER");var outsider=member(foreign,"DM");
        var asset=uploadAsset(dm,id,0);var scene=scene(dm,id);var map=mapCommand(UUID.randomUUID(),0,config(asset));
        for(var browser:List.of(player,outsider)) {
            error(browser.get(base(id)+"/assets"),403,"FORBIDDEN");error(upload(browser,id,UUID.randomUUID(),1,image("png",1,1),"image/png"),403,"FORBIDDEN");
            var download=browser.get(contentPath(id,asset));error(download,403,"FORBIDDEN");privateHeaders(download);
            error(browser.get(mapPath(id,scene)),403,"FORBIDDEN");error(browser.post(mapPath(id,scene),map),403,"FORBIDDEN");
        }
        var anonymous=new Browser();var denied=anonymous.get(contentPath(id,asset));assertEquals(401,denied.statusCode());privateHeaders(denied);
        assertEquals(401,anonymous.get(base(id)+"/assets").statusCode());assertEquals(401,anonymous.get(mapPath(id,scene)).statusCode());
        assertEquals(403,upload(dm,id,json.writeValueAsString(command(1)),image("png",1,1),"image/png",false,"").statusCode());
        assertEquals(403,dm.post(mapPath(id,scene),json.writeValueAsString(map),false).statusCode());
        var missing=dm.get(contentPath(foreign,asset));error(missing,404,"NOT_FOUND");privateHeaders(missing);
        error(dm.get(mapPath(foreign,scene)),404,"NOT_FOUND");error(dm.post(mapPath(foreign,scene(dm,foreign)),mapCommand(UUID.randomUUID(),0,config(asset))),404,"NOT_FOUND");
        error(dm.get(contentPath(id,UUID.randomUUID())),404,"NOT_FOUND");error(dm.get(mapPath(id,UUID.randomUUID())),404,"NOT_FOUND");
    }
    @Test void malformedUploadAndStrictMapFieldsFailWithoutMetadataVersionsOrFiles() throws Exception {
        var dm=owner();var id=campaign(dm);var scene=scene(dm,id);var before=dm.read(base(id)+"/assets");
        for(var media:List.of("image/jpeg","image/svg+xml","text/plain")) error(upload(dm,id,UUID.randomUUID(),0,image("png",2,2),media),400,"INVALID_INPUT");
        for(var bytes:List.of(new byte[0],"<svg/>".getBytes(),Arrays.copyOf(image("png",2,2),32))) error(upload(dm,id,UUID.randomUUID(),0,bytes,"image/png"),400,"INVALID_INPUT");
        for(var value:List.of("null","-1","0.5","\"0\""))error(upload(dm,id,"{\"requestId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":"+value+"}",image("png",1,1),"image/png",true,""),400,"INVALID_INPUT");
        error(upload(dm,id,"{\"expectedVersion\":0}",image("png",1,1),"image/png",true,""),400,"INVALID_INPUT");
        error(upload(dm,id,"{\"requestId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"filename\":\"no\"}",image("png",1,1),"image/png",true,""),400,"INVALID_INPUT");
        assertEquals(413,upload(dm,id,UUID.randomUUID(),0,new byte[10*1024*1024+1],"image/png").statusCode());
        assertEquals(before,dm.read(base(id)+"/assets"));
        UUID asset=uploadAsset(dm,id,0);String path=mapPath(id,scene);
        error(dm.post(path,command(0)),400,"INVALID_INPUT");
        for(String field:config(asset).keySet()){var missing=new HashMap<>(config(asset));missing.remove(field);error(dm.post(path,mapCommand(UUID.randomUUID(),0,missing)),400,"INVALID_INPUT");}
        var unknown=new HashMap<>(config(asset));unknown.put("storageKey","secret");error(dm.post(path,mapCommand(UUID.randomUUID(),0,unknown)),400,"INVALID_INPUT");
        for(var entry:Map.of("gridSize",7,"offsetX",-8193,"offsetY",8193,"distancePerCell",0).entrySet()){var invalid=new HashMap<>(config(asset));invalid.put(entry.getKey(),entry.getValue());error(dm.post(path,mapCommand(UUID.randomUUID(),0,invalid)),400,"INVALID_INPUT");}
        var fractional=new HashMap<>(config(asset));fractional.put("gridSize",48.5);error(dm.post(path,mapCommand(UUID.randomUUID(),0,fractional)),400,"INVALID_INPUT");
        assertEquals(0,dm.read(path).get("sceneVersion").asLong());assertTrue(dm.read(path).get("map").isNull());
    }
    @Test void receiptNamespaceFingerprintStalenessAndAuthorizationAreDurable() throws Exception {
        var dm=owner();var id=campaign(dm);var foreign=campaign(dm);var request=UUID.randomUUID();byte[] image=image("png",3,2);
        var uploaded=tree(upload(dm,id,request,0,image,"image/png"),200);var asset=UUID.fromString(uploaded.get("asset").get("id").asString());var scene=scene(dm,id);
        error(upload(dm,id,request,0,image("png",4,2),"image/png"),409,"IDEMPOTENCY_CONFLICT");error(upload(dm,id,request,1,image,"image/png"),409,"IDEMPOTENCY_CONFLICT");error(upload(dm,foreign,request,0,image,"image/png"),409,"IDEMPOTENCY_CONFLICT");
        error(upload(dm,id,UUID.randomUUID(),0,image,"image/png"),409,"VERSION_CONFLICT");error(dm.post(mapPath(id,scene),mapCommand(request,0,null)),409,"IDEMPOTENCY_CONFLICT");
        tree(dm.post(base(id)+"/scenes",createBody(request,1,"Independent scene namespace")),200);tree(dm.post(base(id)+"/invitations",command(request,2)),201);
        var mapRequest=UUID.randomUUID();var mapBody=mapCommand(mapRequest,0,config(asset));var mapped=tree(dm.post(mapPath(id,scene),mapBody),200);
        error(dm.post(mapPath(id,scene),mapCommand(mapRequest,0,null)),409,"IDEMPOTENCY_CONFLICT");error(dm.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),0,null)),409,"VERSION_CONFLICT");
        assertEquals(mapped,tree(dm.post(mapPath(id,scene),mapBody),200));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username='p02_owner'",id);
        error(upload(dm,id,request,0,image,"image/png"),403,"FORBIDDEN");error(dm.post(mapPath(id,scene),mapBody),403,"FORBIDDEN");var denied=dm.get(contentPath(id,asset));error(denied,403,"FORBIDDEN");privateHeaders(denied);
        jdbc.update("delete from campaign_member where campaign_id=? and username='p02_owner'",id);error(dm.get(contentPath(id,asset)),403,"FORBIDDEN");
        assertEquals(204,dm.post("/api/auth/logout",Map.of()).statusCode());var loggedOut=dm.get(contentPath(id,asset));assertEquals(401,loggedOut.statusCode());privateHeaders(loggedOut);
    }
    @Test void simultaneousRetriesAndSharedManagementVersionsHaveOneEffect() throws Exception {
        var dm=owner();var id=campaign(dm);var second=member(id,"DM");var request=UUID.randomUUID();byte[] png=image("png",2,2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->upload(dm,id,request,0,png,"image/png"));var b=executor.submit(()->upload(dm,id,request,0,png,"image/png"));
            assertEquals(tree(a.get(15,TimeUnit.SECONDS),200),tree(b.get(15,TimeUnit.SECONDS),200));
            var c=executor.submit(()->upload(dm,id,UUID.randomUUID(),1,png,"image/png"));var d=executor.submit(()->second.post(base(id)+"/scenes",createBody(UUID.randomUUID(),1,"Competing scene")));
            var statuses=new ArrayList<>(List.of(c.get(15,TimeUnit.SECONDS).statusCode(),d.get(15,TimeUnit.SECONDS).statusCode()));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);
        }
        assertEquals(2,dm.read(base(id)+"/assets").get("campaignVersion").asLong());
        var asset=UUID.fromString(dm.read(base(id)+"/assets").get("assets").get(0).get("id").asString());var scene=scene(dm,id);var body=mapCommand(UUID.randomUUID(),0,config(asset));
        try(var executor=Executors.newFixedThreadPool(2)){var a=executor.submit(()->dm.post(mapPath(id,scene),body));var b=executor.submit(()->dm.post(mapPath(id,scene),body));assertEquals(tree(a.get(15,TimeUnit.SECONDS),200),tree(b.get(15,TimeUnit.SECONDS),200));}
        assertEquals(1,dm.read(mapPath(id,scene)).get("sceneVersion").asLong());
    }
    @Test void globallyActorKeyedCrossCampaignRaceReturnsConflictAndNotServerError() throws Exception {
        var dm=owner();var first=campaign(dm);var second=campaign(dm);var request=UUID.randomUUID();byte[] png=image("png",1,1);
        try(var executor=Executors.newFixedThreadPool(2)){var a=executor.submit(()->upload(dm,first,request,0,png,"image/png"));var b=executor.submit(()->upload(dm,second,request,0,png,"image/png"));var statuses=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode()));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);}
        assertEquals(1,dm.read(base(first)+"/assets").get("campaignVersion").asLong()+dm.read(base(second)+"/assets").get("campaignVersion").asLong());
    }
    @Test void receiptAndCommitFailureRollbackFileMetadataReferencesAndBothVersions() throws Exception {
        var dm=owner();var id=campaign(dm);var asset=uploadAsset(dm,id,0);var scene=scene(dm,id);var map= new AssetService.MapConfig(asset,48,0,0,5.0);
        assets.configure(id,scene,"p02_owner",UUID.randomUUID(),0,map);var before=dm.read(base(id)+"/assets");var beforeMap=dm.read(mapPath(id,scene));String oldKey=jdbc.queryForObject("select storage_key from campaign_asset where id=?",String.class,asset);
        var fileRoot=Path.of(System.getProperty("java.io.tmpdir"),"dnd-roll-assets-it");Set<String> oldFiles;try(var stream=java.nio.file.Files.list(fileRoot)){oldFiles=stream.map(p->p.getFileName().toString()).collect(java.util.stream.Collectors.toSet());}
        for(boolean deferred:List.of(false,true)) {
            jdbc.execute("create function p02b_fail_receipt() returns trigger language plpgsql as $$ begin raise exception 'forced asset receipt failure'; end $$");
            jdbc.execute(deferred?"create constraint trigger p02b_fail_receipt after insert on asset_command_receipt deferrable initially deferred for each row execute function p02b_fail_receipt()":"create trigger p02b_fail_receipt before insert on asset_command_receipt for each row execute function p02b_fail_receipt()");
            try {
                Class<? extends RuntimeException> expected=deferred?org.springframework.transaction.TransactionSystemException.class:org.springframework.dao.DataAccessException.class;
                assertThrows(expected,()->assets.upload(id,"p02_owner",UUID.randomUUID(),1,image("png",2,3),"image/png"));
                assertThrows(expected,()->assets.configure(id,scene,"p02_owner",UUID.randomUUID(),1,null));
                assertEquals(before,dm.read(base(id)+"/assets"));assertEquals(beforeMap,dm.read(mapPath(id,scene)));assertArrayEquals(image("png",3,2),files.read(oldKey));
                try(var stream=java.nio.file.Files.list(fileRoot)){assertEquals(oldFiles,stream.map(p->p.getFileName().toString()).collect(java.util.stream.Collectors.toSet()));}
            } finally {jdbc.execute("drop trigger p02b_fail_receipt on asset_command_receipt");jdbc.execute("drop function p02b_fail_receipt()");}
        }
    }
    @Test void filePublicationFailureAndMissingBackingFileAreGenericAndNeverExposePaths() throws Exception {
        var dm=owner();var id=campaign(dm);var temporary=java.nio.file.Files.createTempFile("asset-root-is-file-",".tmp");
        try {
            var failing=new AssetService(assetStore,new PrivateAssetFiles(temporary.toString()),events);
            var error=assertThrows(io.dndroll.domain.DomainException.class,()->failing.upload(id,"p02_owner",UUID.randomUUID(),0,image("png",1,1),"image/png"));assertEquals("ASSET_UNAVAILABLE",error.code());
            assertEquals(0,dm.read(base(id)+"/assets").get("campaignVersion").asLong());assertEquals(0,dm.read(base(id)+"/assets").get("assets").size());
        } finally {java.nio.file.Files.deleteIfExists(temporary);}
        var asset=uploadAsset(dm,id,0);String key=jdbc.queryForObject("select storage_key from campaign_asset where id=?",String.class,asset);files.discard(key);
        var missing=dm.get(contentPath(id,asset));error(missing,503,"ASSET_UNAVAILABLE");privateHeaders(missing);assertFalse(missing.body().contains(key));assertFalse(missing.body().contains("dnd-roll-assets-it"));
    }
    @Test void mapAndMovementShareOnlySceneVersionAndCompetingMapCommandsHaveOneWinner() throws Exception {
        var dm=owner();var id=campaign(dm);var second=member(id,"DM");var asset=uploadAsset(dm,id,0);var scene=scene(dm,id);var token=UUID.randomUUID();
        jdbc.update("insert into token(id,scene_id,name,x,y) values (?,?,'Hero',1,1)",token,scene);
        tree(dm.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),0,config(asset))),200);
        tree(dm.post(base(id)+"/tokens/"+token+"/moves",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",4,"y",5)),200);
        assertEquals(2,dm.read(mapPath(id,scene)).get("sceneVersion").asLong());assertEquals(1,dm.read(base(id)+"/assets").get("campaignVersion").asLong());
        error(dm.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),1,null)),409,"VERSION_CONFLICT");
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->dm.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),2,null)));
            var b=executor.submit(()->second.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),2,config(asset))));
            var statuses=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode()));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);
        }
        assertEquals(3,dm.read(mapPath(id,scene)).get("sceneVersion").asLong());assertEquals(1,dm.read(base(id)+"/assets").get("campaignVersion").asLong());
    }
    @Test void multipartRejectsAdditionalAndDuplicatePartsBeforeCreatingAnAsset() throws Exception {
        var dm=owner();var id=campaign(dm);var before=dm.read(base(id)+"/assets");
        for(var extra:List.of("extra","command","file"))
            error(upload(dm,id,json.writeValueAsString(command(0)),image("png",2,2),"image/png",true,extra),400,"INVALID_INPUT");
        assertEquals(before,dm.read(base(id)+"/assets"));
        assertEquals(0,jdbc.queryForObject("select count(*) from asset_command_receipt where campaign_id=?",Integer.class,id));
    }
}
