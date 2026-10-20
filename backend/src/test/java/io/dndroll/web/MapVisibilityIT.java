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
class MapVisibilityIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired SceneManagementService service;
    @Autowired PasswordEncoder passwords;
    @Autowired AssetService assets;
    @Autowired JdbcAssetStore assetStore;
    @Autowired PrivateAssetFiles files;
    @Autowired SceneEvents events;
    @Autowired io.dndroll.application.MapVisibilityService visibility;
    @Autowired org.springframework.messaging.simp.user.SimpUserRegistry registry;
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
        String username;
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(String path, Object body) throws Exception { return post(path, json.writeValueAsString(body), true); }
        HttpResponse<String> post(String path, String body, boolean csrfHeader) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
            if (csrfHeader) request.header("X-CSRF-TOKEN", csrf);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        Browser() throws Exception { refreshCsrf(); }
        void refreshCsrf() throws Exception { csrf = tree(get("/api/auth/csrf"), 200).get("token").asString(); }
        void login(String name) throws Exception { tree(post("/api/auth/login", Map.of("username", name, "password", "integration-owner-password")), 200); username=name; refreshCsrf(); }
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
    String viewPath(UUID id) { return base(id)+"/scene/map-view"; }
    String fogPath(UUID id, UUID scene) { return base(id)+"/scenes/"+scene+"/fog"; }
    String imagePath(UUID id, UUID scene, long version) { return base(id)+"/scene/map-image?sceneId="+scene+"&version="+version; }
    Map<String,Integer> cell(int x,int y) { return Map.of("x",x,"y",y); }
    Map<String,Object> fog(UUID request,long version,List<?> cells) { var result=new HashMap<String,Object>(command(request,version));result.put("revealed",cells);return result; }
    HttpResponse<byte[]> download(Browser b,String path) throws Exception {
        return b.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("If-None-Match","*").header("If-Modified-Since","Wed, 21 Oct 2037 07:28:00 GMT").header("Range","bytes=0-3").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    BufferedImage png(HttpResponse<byte[]> result) throws Exception {
        assertEquals(200,result.statusCode(),new String(result.body(),java.nio.charset.StandardCharsets.UTF_8));privateHeaders(result);
        assertEquals("image/png",result.headers().firstValue("Content-Type").orElse(""));assertTrue(result.headers().firstValue("Last-Modified").isEmpty());
        assertTrue(result.headers().firstValue("Content-Range").isEmpty());return ImageIO.read(new ByteArrayInputStream(result.body()));
    }
    @Test void playerGetsOnlyFreshBlackPixelsAndSourceRoutesCannotBypassFog() throws Exception {
        var dm=owner();var id=campaign(dm);var player=member(id,"PLAYER");var scene=scene(dm,id);
        var source=new BufferedImage(16,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++)for(int x=0;x<16;x++)source.setRGB(x,y,x<8?0xff12ab34:0xffed23cd);
        var bytes=new ByteArrayOutputStream();ImageIO.write(source,"png",bytes);
        UUID asset=UUID.fromString(tree(upload(dm,id,UUID.randomUUID(),0,bytes.toByteArray(),"image/png"),200).get("asset").get("id").asString());
        tree(dm.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),0,Map.of("assetId",asset,"gridSize",8,"offsetX",0,"offsetY",0,"distancePerCell",5.0))),200);
        var view=tree(player.get(viewPath(id)),200);assertEquals(Set.of("sceneId","sceneVersion","map"),keys(view));assertEquals(Set.of("distancePerCell"),keys(view.get("map")));
        var covered=png(download(player,imagePath(id,scene,1)));assertEquals(24*48,covered.getWidth());assertEquals(16*48,covered.getHeight());
        for(int y=0;y<covered.getHeight();y++)for(int x=0;x<covered.getWidth();x++)assertEquals(0xff000000,covered.getRGB(x,y));
        var revealed=tree(dm.post(fogPath(id,scene),fog(UUID.randomUUID(),1,List.of(cell(0,0)))),200);assertEquals(2,revealed.get("sceneVersion").asLong());
        var safe=png(download(player,imagePath(id,scene,2)));for(int y=0;y<48;y++)for(int x=0;x<96;x++)assertEquals(x<48?0xff12ab34:0xff000000,safe.getRGB(x,y));
        assertEquals(0xffed23cd,png(download(dm,imagePath(id,scene,2))).getRGB(48,0));
        for(var target:List.of(asset,UUID.randomUUID())) {var denied=player.get(contentPath(id,target));error(denied,403,"FORBIDDEN");privateHeaders(denied);}
        error(player.get(base(id)+"/assets"),403,"FORBIDDEN");error(player.get(mapPath(id,scene)),403,"FORBIDDEN");
        tree(dm.post(fogPath(id,scene),fog(UUID.randomUUID(),2,List.of())),200);
        var stale=download(player,imagePath(id,scene,2));assertEquals(409,stale.statusCode());privateHeaders(stale);
        assertEquals(0xff000000,png(download(player,imagePath(id,scene,3))).getRGB(0,0));
    }
    @Test void failuresIncludingMalformedAndAnonymousNeverOfferReusableCacheCapabilities() throws Exception {
        var dm=owner();var id=campaign(dm);var scene=scene(dm,id);var anonymous=new Browser();var player=member(id,"PLAYER");
        for(String path:List.of(viewPath(id),imagePath(id,scene,0),fogPath(id,scene),base(id)+"/scene/map-image?sceneId=no&version=-1")) {
            var response=anonymous.get(path);assertEquals(401,response.statusCode());privateHeaders(response);
        }
        var missing=dm.get(imagePath(id,scene,0));error(missing,404,"NOT_FOUND");privateHeaders(missing);
        var fog=dm.get(fogPath(id,scene));assertEquals(200,fog.statusCode());privateHeaders(fog);
        for(String query:List.of("","?sceneId="+scene,"?version=0","?sceneId=no&version=0","?sceneId="+scene+"&version=-1","?sceneId="+scene+"&version=0.5")) {
            var malformed=player.get(base(id)+"/scene/map-image"+query);error(malformed,400,"INVALID_INPUT");privateHeaders(malformed);
        }
    }
    @Test void currentMembershipCurrentSceneAndExactVersionPrecedeImageAccessAndRoleCannotBeSelected() throws Exception {
        var dm=owner();var id=campaign(dm);var foreign=campaign(dm);var player=member(id,"PLAYER");var outsider=member(foreign,"DM");var scene=scene(dm,id);var other=scene(dm,foreign);
        for(String path:List.of(viewPath(id),imagePath(id,other,0),fogPath(id,UUID.randomUUID()))) {var denied=outsider.get(path);error(denied,403,"FORBIDDEN");privateHeaders(denied);}
        for(var target:List.of(other,UUID.randomUUID())) {var missing=player.get(imagePath(id,target,0));error(missing,404,"NOT_FOUND");privateHeaders(missing);}
        var version=player.get(imagePath(id,scene,999));error(version,409,"VERSION_CONFLICT");privateHeaders(version);
        var fakeRole=player.get(imagePath(id,scene,0)+"&role=DM");error(fakeRole,400,"INVALID_INPUT");privateHeaders(fakeRole);
        var duplicate=player.get(imagePath(id,scene,0)+"&version=0");error(duplicate,400,"INVALID_INPUT");privateHeaders(duplicate);
        var asset=uploadAsset(dm,id,0);tree(dm.post(mapPath(id,scene),mapCommand(UUID.randomUUID(),0,config(asset))),200);
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",id,player.username);
        for(String path:List.of(viewPath(id),imagePath(id,scene,1),contentPath(id,asset))) {var denied=player.get(path);error(denied,403,"FORBIDDEN");privateHeaders(denied);}
        tree(dm.post(base(id)+"/scenes/"+scene(dm,foreign)+"/activate",command(1)),404);
        var inactive=UUID.fromString(tree(dm.post(base(id)+"/scenes",createBody(UUID.randomUUID(),1,"Next")),200).get("id").asString());
        tree(dm.post(base(id)+"/scenes/"+inactive+"/activate",command(2)),200);
        error(dm.get(imagePath(id,scene,1)),404,"NOT_FOUND");assertEquals(inactive.toString(),dm.read(viewPath(id)).get("sceneId").asString());assertTrue(dm.read(viewPath(id)).get("map").isNull());
    }
    @Test void fogAuthoringHasStrictCellsCurrentDmBeforeReplayAndHistoricalReceiptsNeverRestore() throws Exception {
        var dm=owner();var id=campaign(dm);var foreign=campaign(dm);var s=scene(dm,id);var player=member(id,"PLAYER");var path=fogPath(id,s);
        var request=UUID.randomUUID();var body=fog(request,0,List.of(cell(3,2),cell(1,0)));var result=tree(dm.post(path,body),200);
        assertEquals(List.of(cell(1,0),cell(3,2)),json.readValue(result.get("revealed").toString(),List.class));privateHeaders(dm.get(path));
        var reversed=fog(request,0,List.of(cell(1,0),cell(3,2)));assertEquals(result,tree(dm.post(path,reversed),200));
        error(dm.post(path,fog(request,0,List.of())),409,"IDEMPOTENCY_CONFLICT");error(dm.post(path,fog(UUID.randomUUID(),0,List.of())),409,"VERSION_CONFLICT");
        var covered=tree(dm.post(path,fog(UUID.randomUUID(),1,List.of())),200);assertEquals(result,tree(dm.post(path,body),200));assertEquals(covered,dm.read(path));
        for(String target:List.of(path,fogPath(id,UUID.randomUUID()),fogPath(id,scene(dm,foreign)))) {
            error(player.get(target),403,"FORBIDDEN");var denied=player.post(target,body);error(denied,403,"FORBIDDEN");privateHeaders(denied);
        }
        error(dm.get(fogPath(foreign,s)),404,"NOT_FOUND");error(dm.post(fogPath(foreign,s),body),404,"NOT_FOUND");
        var noCsrf=dm.post(path,json.writeValueAsString(fog(UUID.randomUUID(),2,List.of())),false);assertEquals(403,noCsrf.statusCode());privateHeaders(noCsrf);
        for(String value:List.of("null","-1","0.5","\"2\""))error(dm.post(path,"{\"requestId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":"+value+",\"revealed\":[]}",true),400,"INVALID_INPUT");
        for(String cells:List.of("null","[null]","[{\"x\":null,\"y\":0}]","[{\"x\":0}]","[{\"x\":0.5,\"y\":0}]","[{\"x\":\"0\",\"y\":0}]","[{\"x\":0,\"y\":0,\"hidden\":false}]","[{\"x\":-1,\"y\":0}]","[{\"x\":24,\"y\":0}]","[{\"x\":0,\"y\":16}]","[{\"x\":0,\"y\":0},{\"x\":0,\"y\":0}]")) {
            var denied=dm.post(path,"{\"requestId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":2,\"revealed\":"+cells+"}",true);error(denied,400,"INVALID_INPUT");privateHeaders(denied);
        }
        for(String missing:List.of("requestId","expectedVersion","revealed")){var bad=new HashMap<String,Object>(fog(UUID.randomUUID(),2,List.of()));bad.remove(missing);error(dm.post(path,bad),400,"INVALID_INPUT");}
        var unknown=new HashMap<String,Object>(fog(UUID.randomUUID(),2,List.of()));unknown.put("assetId",UUID.randomUUID());error(dm.post(path,unknown),400,"INVALID_INPUT");
        assertEquals(covered,dm.read(path));jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username='p02_owner'",id);
        var denied=dm.post(path,body);error(denied,403,"FORBIDDEN");privateHeaders(denied);error(dm.get(path),403,"FORBIDDEN");
    }
    @Test void mapAssignmentCalibrationAndDetachResetFogAtomicallyWithoutChangingTokens() throws Exception {
        var dm=owner();var id=campaign(dm);var s=scene(dm,id);var player=member(id,"PLAYER");var asset=uploadAsset(dm,id,0);
        UUID visible=UUID.randomUUID(),hidden=UUID.randomUUID();jdbc.update("insert into token(id,scene_id,name,x,y,hidden) values (?,?,'Visible on covered map',0,0,false),(?,?,'Hidden secret',1,1,true)",visible,s,hidden,s);
        jdbc.update("insert into token_controller(token_id,username) values (?,?)",visible,player.username);
        var before=player.read(base(id)+"/scene").get("tokens");assertEquals(1,before.size());
        tree(dm.post(fogPath(id,s),fog(UUID.randomUUID(),0,List.of(cell(0,0)))),200);
        var mapRequest=UUID.randomUUID();var map=mapCommand(mapRequest,1,config(asset));tree(dm.post(mapPath(id,s),map),200);assertEquals(0,dm.read(fogPath(id,s)).get("revealed").size());
        tree(dm.post(fogPath(id,s),fog(UUID.randomUUID(),2,List.of(cell(0,0)))),200);tree(dm.post(mapPath(id,s),map),200);assertEquals(1,dm.read(fogPath(id,s)).get("revealed").size());
        tree(dm.post(mapPath(id,s),mapCommand(UUID.randomUUID(),3,config(asset))),200);assertEquals(0,dm.read(fogPath(id,s)).get("revealed").size());
        tree(dm.post(fogPath(id,s),fog(UUID.randomUUID(),4,List.of(cell(0,0)))),200);
        tree(dm.post(mapPath(id,s),mapCommand(UUID.randomUUID(),5,null)),200);assertEquals(0,dm.read(fogPath(id,s)).get("revealed").size());assertEquals(6,dm.read(fogPath(id,s)).get("sceneVersion").asLong());
        assertEquals(before,player.read(base(id)+"/scene").get("tokens"));
        var prep=UUID.fromString(tree(dm.post(base(id)+"/scenes",createBody(UUID.randomUUID(),1,"Preparation")),200).get("id").asString());
        tree(dm.post(fogPath(id,prep),fog(UUID.randomUUID(),0,List.of(cell(2,2)))),200);assertEquals(s,scene(dm,id));
    }
    @Test void fogRetriesRacesGlobalNamespaceAndSharedSceneVersionsHaveOneEffect() throws Exception {
        var dm=owner();var id=campaign(dm);var s=scene(dm,id);var second=member(id,"DM");var request=UUID.randomUUID();var body=fog(request,0,List.of(cell(1,1)));
        try(var pool=Executors.newFixedThreadPool(2)) {var a=pool.submit(()->dm.post(fogPath(id,s),body));var b=pool.submit(()->dm.post(fogPath(id,s),body));assertEquals(tree(a.get(15,TimeUnit.SECONDS),200),tree(b.get(15,TimeUnit.SECONDS),200));}
        assertEquals(1,dm.read(fogPath(id,s)).get("sceneVersion").asLong());
        var asset=uploadAsset(dm,id,0);
        try(var pool=Executors.newFixedThreadPool(2)) {var a=pool.submit(()->dm.post(fogPath(id,s),fog(UUID.randomUUID(),1,List.of())));var b=pool.submit(()->second.post(mapPath(id,s),mapCommand(UUID.randomUUID(),1,config(asset))));assertStatuses(a,b);}
        var foreign=campaign(dm);var fs=scene(dm,foreign);var global=UUID.randomUUID();
        try(var pool=Executors.newFixedThreadPool(2)) {var a=pool.submit(()->dm.post(fogPath(id,s),fog(global,2,List.of())));var b=pool.submit(()->dm.post(fogPath(foreign,fs),fog(global,0,List.of())));assertStatuses(a,b);}
        // The same UUID belongs independently to asset and fog receipt namespaces.
        tree(dm.post(fogPath(foreign,fs),fog(request,dm.read(fogPath(foreign,fs)).get("sceneVersion").asLong(),List.of())),409);
        tree(dm.post(mapPath(foreign,fs),mapCommand(request,dm.read(fogPath(foreign,fs)).get("sceneVersion").asLong(),null)),200);
    }
    void assertStatuses(Future<HttpResponse<String>>a,Future<HttpResponse<String>>b)throws Exception {var values=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode()));Collections.sort(values);assertEquals(List.of(200,409),values);}
    @Test void fogAndMapReceiptFailuresRollbackFogVersionAndNoAfterCommitEvent() throws Exception {
        var dm=owner();var id=campaign(dm);var s=scene(dm,id);var asset=uploadAsset(dm,id,0);
        tree(dm.post(fogPath(id,s),fog(UUID.randomUUID(),0,List.of(cell(1,1)))),200);var before=dm.read(fogPath(id,s));
        for(boolean deferred:List.of(false,true))for(String table:List.of("fog_command_receipt","asset_command_receipt")) {
            jdbc.execute("create function p02d_fail_receipt() returns trigger language plpgsql as $$ begin raise exception 'forced visibility receipt failure'; end $$");
            jdbc.execute(deferred?"create constraint trigger p02d_fail_receipt after insert on "+table+" deferrable initially deferred for each row execute function p02d_fail_receipt()":"create trigger p02d_fail_receipt before insert on "+table+" for each row execute function p02d_fail_receipt()");
            try {
                Class<? extends RuntimeException> expected=deferred?org.springframework.transaction.TransactionSystemException.class:org.springframework.dao.DataAccessException.class;
                if(table.equals("fog_command_receipt"))assertThrows(expected,()->visibility.configure(id,s,"p02_owner",UUID.randomUUID(),1,List.of()));
                else assertThrows(expected,()->assets.configure(id,s,"p02_owner",UUID.randomUUID(),1,new AssetService.MapConfig(asset,8,0,0,5.0)));
                assertEquals(before,dm.read(fogPath(id,s)));assertTrue(dm.read(mapPath(id,s)).get("map").isNull());assertEquals(1,jdbc.queryForObject("select count(*) from fog_command_receipt where campaign_id=?",Integer.class,id));
            } finally {jdbc.execute("drop trigger p02d_fail_receipt on "+table);jdbc.execute("drop function p02d_fail_receipt()");}
        }
    }
    @Test void mapMissingBackingFileIsGenericAndFogEventsNeverContainSecretsOrReplayEvents() throws Exception {
        var dm=owner();var id=campaign(dm);var s=scene(dm,id);var player=member(id,"PLAYER");var asset=uploadAsset(dm,id,0);
        tree(dm.post(mapPath(id,s),mapCommand(UUID.randomUUID(),0,config(asset))),200);
        var frames=new LinkedBlockingQueue<String>();var fragments=new StringBuilder();
        var socket=player.client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:"+port+"/ws"),new WebSocket.Listener(){
            public void onOpen(WebSocket ws){ws.request(1);}public CompletionStage<?>onText(WebSocket ws,CharSequence text,boolean last){fragments.append(text);if(last){frames.add(fragments.toString());fragments.setLength(0);}ws.request(1);return null;}
        }).get(5,TimeUnit.SECONDS);
        try {
            socket.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nX-CSRF-TOKEN:"+player.csrf+"\n\n\0",true).join();assertTrue(Objects.requireNonNull(frames.poll(5,TimeUnit.SECONDS)).startsWith("CONNECTED"));
            socket.sendText("SUBSCRIBE\nid:fog-changes\ndestination:/user/queue/scene-changes\n\n\0",true).join();
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);boolean subscribed=false;
            while(System.nanoTime()<until){var user=registry.getUser(player.username);if(user!=null&&user.getSessions().stream().anyMatch(x->!x.getSubscriptions().isEmpty())){subscribed=true;break;}Thread.sleep(20);}assertTrue(subscribed);
            var body=fog(UUID.randomUUID(),1,List.of(cell(7,9)));var result=tree(dm.post(fogPath(id,s),body),200);
            var frame=frames.poll(5,TimeUnit.SECONDS);assertNotNull(frame);var event=json.readTree(frame.substring(frame.indexOf("\n\n")+2).replace("\0",""));assertEquals(Set.of("type","campaignId"),keys(event));assertEquals("SCENE_CHANGED",event.get("type").asString());
            for(String secret:List.of("revealed",asset.toString(),"map-image","storageKey","7","9"))if(secret.length()>1)assertFalse(frame.contains(secret));
            assertEquals(result,tree(dm.post(fogPath(id,s),body),200));assertNull(frames.poll(250,TimeUnit.MILLISECONDS));
            String key=jdbc.queryForObject("select storage_key from campaign_asset where id=?",String.class,asset);files.discard(key);
            var unavailable=player.get(imagePath(id,s,2));error(unavailable,503,"ASSET_UNAVAILABLE");privateHeaders(unavailable);assertFalse(unavailable.body().contains(key));assertFalse(unavailable.body().contains("dnd-roll-assets-it"));
        } finally {socket.abort();}
    }
    @Test void imageGenerationHoldsCampaignLockThroughSourceReadAndConcurrentCover() throws Exception {
        var dm=owner();var id=campaign(dm);var s=scene(dm,id);var player=member(id,"PLAYER");var asset=uploadAsset(dm,id,0);
        tree(dm.post(mapPath(id,s),mapCommand(UUID.randomUUID(),0,config(asset))),200);tree(dm.post(fogPath(id,s),fog(UUID.randomUUID(),1,List.of(cell(0,0)))),200);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var lockedStore=new io.dndroll.persistence.JdbcMapVisibilityStore(jdbc,new io.dndroll.persistence.JdbcSceneStore(jdbc,new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()))),assetStore,files,json){
            @Override public byte[] original(UUID campaign,UUID assetId){entered.countDown();try{assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException failure){throw new AssertionError(failure);}return super.original(campaign,assetId);}
        };
        var lockedService=new io.dndroll.application.MapVisibilityService(lockedStore,ignored->fail("Read emitted an event"));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var image=pool.submit(()->lockedService.image(id,player.username,s,2));assertTrue(entered.await(5,TimeUnit.SECONDS));
            var cover=pool.submit(()->dm.post(fogPath(id,s),fog(UUID.randomUUID(),2,List.of())));
            assertThrows(TimeoutException.class,()->cover.get(150,TimeUnit.MILLISECONDS),"Cover must wait until image authorization/read completes");
            release.countDown();assertNotNull(ImageIO.read(new ByteArrayInputStream(image.get(5,TimeUnit.SECONDS))));tree(cover.get(5,TimeUnit.SECONDS),200);
        }finally{release.countDown();}
        assertEquals(0,dm.read(fogPath(id,s)).get("revealed").size());assertEquals(409,download(player,imagePath(id,s,2)).statusCode());png(download(player,imagePath(id,s,3)));
    }

    @Test void integratedSceneReturnCalibrationAndRevocationPreserveIndependentFogAndHistoricalReceipts() throws Exception {
        var dm=owner();var id=campaign(dm);var player=member(id,"PLAYER");var a=scene(dm,id);
        try {
            var source=new BufferedImage(16,8,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<8;y++)for(int x=0;x<16;x++)source.setRGB(x,y,x<8?0xff12ab34:0xffed23cd);
            var bytes=new ByteArrayOutputStream();ImageIO.write(source,"png",bytes);
            var asset=UUID.fromString(tree(upload(dm,id,UUID.randomUUID(),0,bytes.toByteArray(),"image/png"),200).get("asset").get("id").asString());
            var b=UUID.fromString(tree(dm.post(base(id)+"/scenes",createBody(UUID.randomUUID(),1,"Independent map B")),200).get("id").asString());
            var originalMap=Map.of("assetId",asset,"gridSize",8,"offsetX",0,"offsetY",0,"distancePerCell",5.0);
            tree(dm.post(mapPath(id,a),mapCommand(UUID.randomUUID(),0,originalMap)),200);
            var mapB=mapCommand(UUID.randomUUID(),0,originalMap);
            var mapBReceipt=tree(dm.post(mapPath(id,b),mapB),200);
            var tokenBody=Map.<String,Object>of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","Public independent token","x",0,"y",0,"hidden",false,"controllers",List.of(player.username));
            var tokenPath=base(id)+"/scenes/"+a+"/tokens";
            var tokenReceipt=tree(dm.post(tokenPath,tokenBody),200);
            var tokenId=tokenReceipt.get("token").get("id").asString();
            tree(dm.post(tokenPath,Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"name","Never public hidden token","x",0,"y",0,"hidden",true,"controllers",List.of())),200);
            var fogA=fog(UUID.randomUUID(),3,List.of(cell(0,0)));
            tree(dm.post(fogPath(id,a),fogA),200);
            var fogB=fog(UUID.randomUUID(),1,List.of(cell(1,0)));
            var fogBReceipt=tree(dm.post(fogPath(id,b),fogB),200);
            assertEquals(a,scene(dm,id),"Preparing B must not activate it");
            error(player.get(base(id)+"/scenes"),403,"FORBIDDEN");
            var movePath=base(id)+"/tokens/"+tokenId+"/moves";
            var move=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",2,"y",3);
            tree(player.post(movePath,move),200);
            var preservedA=dm.read(fogPath(id,a));
            assertEquals(5,preservedA.get("sceneVersion").asLong());
            var snapshotA=player.read(base(id)+"/scene");
            assertEquals(1,snapshotA.get("tokens").size());
            assertEquals(2,snapshotA.get("tokens").get(0).get("x").asInt());
            var beforeSwitch=png(download(player,imagePath(id,a,5)));
            assertEquals(0xff12ab34,beforeSwitch.getRGB(20,20));assertEquals(0xff000000,beforeSwitch.getRGB(68,20));

            var activateB=command(2);var activateBReceipt=tree(dm.post(base(id)+"/scenes/"+b+"/activate",activateB),200);
            error(player.get(imagePath(id,a,5)),404,"NOT_FOUND");
            var visibleB=png(download(player,imagePath(id,b,2)));
            assertEquals(0xff000000,visibleB.getRGB(20,20));assertEquals(0xffed23cd,visibleB.getRGB(68,20));
            var recalibrated=Map.of("assetId",asset,"gridSize",8,"offsetX",8,"offsetY",0,"distancePerCell",10.0);
            error(dm.post(mapPath(id,b),mapCommand(UUID.randomUUID(),1,recalibrated)),409,"VERSION_CONFLICT");
            assertEquals(fogBReceipt,dm.read(fogPath(id,b)),"Rejected calibration cannot clear B fog");
            tree(dm.post(mapPath(id,b),mapCommand(UUID.randomUUID(),2,recalibrated)),200);
            var resetB=dm.read(fogPath(id,b));assertEquals(3,resetB.get("sceneVersion").asLong());assertEquals(0,resetB.get("revealed").size());
            assertEquals(0xff000000,png(download(player,imagePath(id,b,3))).getRGB(68,20));
            assertEquals(mapBReceipt,tree(dm.post(mapPath(id,b),mapB),200));
            assertEquals(fogBReceipt,tree(dm.post(fogPath(id,b),fogB),200));
            assertEquals(resetB,dm.read(fogPath(id,b)),"Historical receipts cannot reveal or recalibrate B");
            assertEquals(8,dm.read(mapPath(id,b)).get("map").get("offsetX").asInt());

            tree(dm.post(base(id)+"/scenes/"+a+"/activate",command(3)),200);
            assertEquals(snapshotA,player.read(base(id)+"/scene"));assertEquals(preservedA,dm.read(fogPath(id,a)));
            assertEquals(0xff12ab34,png(download(player,imagePath(id,a,5))).getRGB(20,20));
            assertEquals(activateBReceipt,tree(dm.post(base(id)+"/scenes/"+b+"/activate",activateB),200));
            assertEquals(a,scene(dm,id),"Historical activation cannot switch the group back to B");
            var revoke=Map.<String,Object>of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","Public independent token","x",2,"y",3,"hidden",false,"controllers",List.of());
            tree(dm.post(tokenPath+"/"+tokenId,revoke),200);
            error(player.post(movePath,move),403,"FORBIDDEN");
            assertEquals(tokenReceipt,tree(dm.post(tokenPath,tokenBody),200));
            var current=player.read(base(id)+"/scene");assertEquals(6,current.get("version").asLong());
            assertEquals(1,current.get("tokens").size());assertFalse(current.get("tokens").get(0).get("canMove").asBoolean());
            assertEquals(preservedA.get("revealed"),dm.read(fogPath(id,a)).get("revealed"));
            var knownImage=imagePath(id,a,6);assertEquals(0xff12ab34,png(download(player,knownImage)).getRGB(20,20));
            assertEquals(204,dm.post(base(id)+"/members/"+player.username+"/remove",command(4)).statusCode());
            for(String path:List.of(base(id)+"/scene",viewPath(id),knownImage,contentPath(id,asset))) {
                var denied=player.get(path);error(denied,403,"FORBIDDEN");
                if(!path.endsWith("/scene"))privateHeaders(denied);
            }
            error(player.post(movePath,move),403,"FORBIDDEN");
            var reconnected=new Browser();reconnected.login(player.username);
            var denied=reconnected.get(knownImage);error(denied,403,"FORBIDDEN");privateHeaders(denied);
            assertEquals(resetB,dm.read(fogPath(id,b)),"A control and membership changes leave unrelated B state intact");
        } finally {
            // The shared fixture predates token authoring; remove these receipts before its campaign cleanup.
            jdbc.update("delete from token_command_receipt where campaign_id=?",id);
        }
    }
}
