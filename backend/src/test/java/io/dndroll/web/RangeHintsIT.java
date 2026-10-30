package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=a07_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class RangeHintsIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    final JsonMapper json=JsonMapper.builder().build();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build(); String csrf;
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
        Browser(String user)throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();tree(post("/api/auth/login",Map.of("username",user,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
    }
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return json.readTree(r.body());}
    UUID campaign,scene,source,target,hidden; String user; Browser dm,player;
    String base(){return "/api/campaigns/"+campaign;}
    String route(){return base()+"/range-hints/preview";}
    @BeforeEach void setup()throws Exception{
        dm=new Browser("a07_owner");campaign=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","A07 range")),201).get("id").asString());
        scene=jdbc.queryForObject("select current_scene_id from campaign where id=?",UUID.class,campaign);
        user="a07_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),user,passwords.encode("integration-owner-password"));
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",campaign,user);
        source=token("Visible source",1,1,false);target=token("Visible target",4,3,false);hidden=token("secret endpoint",9,9,true);
        player=new Browser(user);
    }
    UUID token(String name,int x,int y,boolean hide){UUID id=UUID.randomUUID();jdbc.update("insert into token(id,campaign_id,scene_id,name,x,y,hidden) values (?,?,?,?,?,?,?)",id,campaign,scene,name,x,y,hide);return id;}
    Map<String,Object> command(){var d=new LinkedHashMap<String,Object>();d.put("sceneId",scene);d.put("sceneVersion",0);d.put("sourceId",source);d.put("sourceVersion",0);d.put("targetId",target);d.put("targetVersion",0);d.put("profile","GRID_5R_5FT_RANGE_V1");d.put("reviewed",true);d.put("mode","RANGED");d.put("normalFeet",10);d.put("longFeet",15);return d;}
    void privateHeaders(HttpResponse<?> r){assertEquals("no-store, private",r.headers().firstValue("Cache-Control").orElse(""));assertEquals("nosniff",r.headers().firstValue("X-Content-Type-Options").orElse(""));}
    // Entire application schema, not just new tables: repeated preview must be read-only.
    Map<String,List<String>> rows(){var out=new TreeMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname='public' order by tablename",String.class))out.put(table,jdbc.queryForList("select row_to_json(t)::text from \""+table+"\" t order by row_to_json(t)::text",String.class));return out;}
    @Test void diagonalThresholdsAndExactRepeatAreReadOnlyWithoutControlOrCards()throws Exception{
        var before=rows();var body=command();var response=player.post(route(),body);privateHeaders(response);var result=tree(response,200);
        assertEquals(15,result.get("distanceFeet").asInt());assertEquals("LONG_RANGE",result.get("band").asString());assertEquals("CANDIDATE",result.get("status").asString());assertEquals(4,result.size());
        assertEquals(result,tree(player.post(route(),body),200));assertEquals(before,rows());
        body.put("normalFeet",15);assertEquals("NORMAL_RANGE",tree(player.post(route(),body),200).get("band").asString());
        body.put("normalFeet",10);body.put("longFeet",10);assertEquals("OUT_OF_RANGE",tree(player.post(route(),body),200).get("band").asString());
        body.put("mode","MELEE");body.put("normalFeet",15);body.put("longFeet",15);assertEquals("WITHIN_REACH",tree(player.post(route(),body),200).get("band").asString());assertEquals(before,rows());
    }
    @Test void hiddenMissingAndOtherSceneEndpointsAreIndistinguishableBeforeVersions()throws Exception{
        assertEquals(200,player.post(route(),command()).statusCode());var body=command();body.put("sceneVersion",99);body.put("targetId",hidden);var secret=player.post(route(),body);privateHeaders(secret);var error=tree(secret,404);assertFalse(secret.body().contains("secret endpoint"));assertFalse(secret.body().contains("distanceFeet"));
        body.put("targetId",UUID.randomUUID());assertEquals(error,tree(player.post(route(),body),404));
        UUID otherScene=UUID.randomUUID();jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'other',24,16,48)",otherScene,campaign);jdbc.update("update token set scene_id=? where id=?",otherScene,target);body.put("targetId",target);assertEquals(error,tree(player.post(route(),body),404));
        body.put("sourceId",hidden);assertEquals(error,tree(player.post(route(),body),404));
    }
    @Test void currentVisibilityMembershipAndVersionsAreRecheckedOnEveryRepeat()throws Exception{
        var body=command();tree(player.post(route(),body),200);jdbc.update("update token set hidden=true,version=version+1 where id=?",target);tree(player.post(route(),body),404);
        jdbc.update("update token set hidden=false where id=?",target);tree(player.post(route(),body),409);body.put("targetVersion",1);tree(player.post(route(),body),200);
        jdbc.update("update scene set version=version+1 where id=?",scene);tree(player.post(route(),body),409);body.put("sceneVersion",1);tree(player.post(route(),body),200);
        body.put("sceneId",UUID.randomUUID());tree(player.post(route(),body),409);body.put("sceneId",scene);
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",campaign,user);privateHeaders(player.post(route(),body));tree(player.post(route(),body),403);
    }
    @Test void unreviewedContextIsManualWithoutPartialDistanceAndDmCanMeasurePrivateEndpoint()throws Exception{
        var body=command();body.put("reviewed",false);var result=tree(player.post(route(),body),200);assertEquals("MANUAL_REQUIRED",result.get("status").asString());assertTrue(result.get("distanceFeet").isNull());assertTrue(result.get("band").isNull());assertEquals("REVIEW_REQUIRED",result.get("reason").asString());
        body.put("reviewed",true);body.put("targetId",hidden);assertEquals(40,tree(dm.post(route(),body),200).get("distanceFeet").asInt());
        body.put("targetId",source);assertEquals(0,tree(player.post(route(),body),200).get("distanceFeet").asInt());
    }
    @Test void strictShapeAndThresholdsNeverReturnPartialCandidate()throws Exception{
        tree(player.post(route(),command()),200);
        for(String field:command().keySet()){var body=command();body.remove(field);privateHeaders(player.post(route(),body));tree(player.post(route(),body),400);body=command();body.put(field,null);tree(player.post(route(),body),400);}
        for(var change:List.of(Map.of("x",1),Map.of("normalFeet",-1),Map.of("longFeet",100001),Map.of("longFeet",9),Map.of("normalFeet",2.5),Map.of("sceneVersion",9007199254740992L),Map.of("mode","SPELL_AREA"),Map.of("mode","MELEE"),Map.of("profile","2014"),Map.of("reviewed","true"),Map.of("normalFeet","5"))){var body=command();body.putAll(change);tree(player.post(route(),body),400);}
    }
    @Test void anonymousAndMissingCsrfFailuresArePrivate()throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+route())).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(command()))).build();var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());assertTrue(Set.of(401,403).contains(response.statusCode()));privateHeaders(response);
        var response2=player.client.send(request,HttpResponse.BodyHandlers.ofString());assertEquals(403,response2.statusCode());privateHeaders(response2);
    }
    @Test void nonFiveFootCalibrationAndForeignCampaignEndpointsNeverGivePartialHints()throws Exception{
        UUID asset=UUID.randomUUID();
        jdbc.update("insert into campaign_asset(id,campaign_id,storage_key,sha256,media_type,byte_size,width,height) values (?,?,?,?,'image/png',1,1,1)",asset,campaign,UUID.randomUUID().toString(),"0".repeat(64));
        jdbc.update("insert into scene_map(campaign_id,scene_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,48,0,0,10)",campaign,scene,asset);
        try{
            var before=rows();var response=player.post(route(),command());privateHeaders(response);var result=tree(response,200);assertEquals("GRID_REQUIRES_MANUAL",result.get("reason").asString());assertTrue(result.get("distanceFeet").isNull());assertTrue(result.get("band").isNull());assertEquals(before,rows());
        }finally{jdbc.update("delete from scene_map where scene_id=?",scene);jdbc.update("delete from campaign_asset where id=?",asset);}
        UUID other=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Other A07")),201).get("id").asString());
        var body=command();body.put("targetId",hidden);var error=tree(player.post(route(),body),404);
        jdbc.update("update token set campaign_id=?,scene_id=(select current_scene_id from campaign where id=?) where id=?",other,other,target);body.put("targetId",target);assertEquals(error,tree(player.post(route(),body),404));
    }
}
