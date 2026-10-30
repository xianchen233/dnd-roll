package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.domain.CharacterSheet;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=a06_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class MovementPathsIT {
    @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;
    final JsonMapper json=JsonMapper.builder().build();final Set<UUID> campaigns=new HashSet<>();final Set<String> users=new HashSet<>();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf;
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
        Browser(String user)throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();tree(post("/api/auth/login",Map.of("username",user,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return r.body().isEmpty()?null:json.readTree(r.body());}
    record Fixture(Browser dm,Browser player,UUID c,UUID card,UUID token,UUID scene,String user) {}
    Fixture fixture()throws Exception{
        var dm=new Browser("a06_owner");UUID c=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","A06 test")),201).get("id").asString());campaigns.add(c);
        String user="a06_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(user);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),user,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,user);
        UUID card=UUID.fromString(tree(dm.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",CharacterSheet.empty("Walker"))),200).get("id").asString());jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,?)",c,card,user);
        UUID scene=jdbc.queryForObject("select current_scene_id from campaign where id=?",UUID.class,c),token=UUID.randomUUID();jdbc.update("insert into token(id,campaign_id,scene_id,name,x,y,character_id) values (?,?,?,'Walker',1,1,?)",token,c,scene,card);jdbc.update("insert into token_controller(token_id,username) values (?,?)",token,user);
        return new Fixture(dm,new Browser(user),c,card,token,scene,user);
    }
    String base(UUID c){return "/api/campaigns/"+c;}
    String path(Fixture f){return base(f.c)+"/tokens/"+f.token+"/movement";}
    Map<String,Object> step(int x,int y,String mode,boolean terrain){return Map.of("x",x,"y",y,"mode",mode,"difficult",terrain);}
    Map<String,Object> command(JsonNode v,List<?> steps){var d=new LinkedHashMap<String,Object>();d.put("requestId",UUID.randomUUID());d.put("expectedVersion",v.get("token").get("version").asLong());d.put("sceneVersion",v.get("sceneVersion").asLong());d.put("characterVersion",v.get("characterVersion").asLong());d.put("budgetVersion",v.get("budgetVersion").asString());d.put("turnKey",v.get("turnKey").asLong());d.put("reviewed",true);d.put("path",steps);d.put("override",false);d.put("cost",0);d.put("reason","");return d;}
    void start(Fixture f)throws Exception{
        start(f,List.of(f.card));
    }
    void start(Fixture f,List<UUID> order)throws Exception{
        // Existing P05/P06 entry points, not a direct combat-state fixture.
        var participants=order.stream().map(id->Map.of("characterId",id,"expectedVersion",0,"kind","PC","group","","surprised",false,"advantage",List.of(),"disadvantage",List.of())).toList();
        var generated=tree(f.dm.post(base(f.c)+"/initiative",Map.of("requestId",UUID.randomUUID(),"expectedVersion",f.dm.read(base(f.c)+"/initiative").get("version").asLong(),"participants",participants,"reason","Roster")),200);
        tree(f.dm.post(base(f.c)+"/initiative/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",generated.get("version").asLong(),"order",order,"reason","Confirmed")),200);
        tree(f.dm.post(base(f.c)+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",f.dm.read(base(f.c)+"/combat").get("version").asLong(),"kind","START","characterId","","order",List.of(),"reason","start")),200);
    }
    @AfterEach void cleanup(){for(UUID c:campaigns){jdbc.update("delete from movement_journal where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from movement_receipt where campaign_id=?",c);jdbc.update("delete from token_controller where campaign_id=?",c);jdbc.update("delete from token where campaign_id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}jdbc.update("delete from account_command_receipt where actor='a06_owner'");for(String u:users){jdbc.update("delete from app_user where username=?",u);}}
    @Test void previewDoesNotMoveAndCommitCountsActualLoopWithSingleEffectReplay()throws Exception{
        var f=fixture();start(f);var v=f.player.read(path(f));var d=command(v,List.of(step(2,2,"WALK",false),step(2,1,"CRAWL",true),step(1,1,"WALK",false)));
        var p=tree(f.player.post(path(f)+"/preview",d),200);assertEquals(25,p.get("cost").asInt());assertEquals(0,f.player.read(path(f)).get("used").asInt());
        var result=tree(f.player.post(path(f)+"/paths",d),200);assertEquals(25,result.get("calculation").get("usedAfter").asInt());assertEquals(result,tree(f.player.post(path(f)+"/paths",d),200));assertEquals(1,f.player.read(path(f)).get("token").get("version").asInt());assertEquals(0,f.player.read(path(f)).get("history").size());assertEquals(1,f.dm.read(path(f)).get("history").size());
    }
    @Test void sameCharacterPlacementsShareBudgetAndLegacyMovementMakesItIncomplete()throws Exception{
        var f=fixture();start(f);var v=f.player.read(path(f));tree(f.player.post(path(f)+"/paths",command(v,List.of(step(2,1,"WALK",false)))),200);
        UUID other=UUID.randomUUID();jdbc.update("insert into token(id,campaign_id,scene_id,name,x,y,character_id) values (?,?,?,'Other placement',3,3,?)",other,f.c,f.scene,f.card);jdbc.update("insert into token_controller(token_id,username) values (?,?)",other,f.user);
        String second=base(f.c)+"/tokens/"+other+"/movement";var shared=f.player.read(second);assertEquals(5,shared.get("used").asInt());assertFalse(shared.get("complete").asBoolean());
        tree(f.player.post(base(f.c)+"/tokens/"+other+"/moves",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",4,"y",4)),200);assertFalse(f.player.read(path(f)).get("complete").asBoolean());
    }
    @Test void dmAdjustmentUsesFinalValuesKeepsHistoryAndOldReceiptCannotRewindLaterMoves()throws Exception{
        var f=fixture();start(f);var v=f.dm.read(path(f));var d=new LinkedHashMap<>(command(v,List.of(step(9,9,"MANUAL",false))));d.put("override",true);d.put("cost",40);d.put("reason","private teleport ruling");var original=tree(f.dm.post(path(f)+"/paths",d),200);assertTrue(original.get("calculation").get("overBudget").asBoolean());
        var now=f.dm.read(path(f));var a=new LinkedHashMap<>(command(now,List.of()));a.remove("path");a.remove("reviewed");a.remove("override");a.remove("cost");a.put("used",5);a.put("allowance",60);a.put("reason","DM corrected final values");tree(f.dm.post(path(f)+"/adjustments",a),200);
        tree(f.dm.post(path(f)+"/paths",d),200);var after=f.player.read(path(f));assertEquals(5,after.get("used").asInt());assertEquals(60,after.get("allowance").asInt());assertFalse(after.toString().contains("private teleport"));assertEquals(2,f.dm.read(path(f)).get("history").size());
    }
    @Test void freshStaleVersionsAndChangedIdempotentPayloadRejectWithoutMovement()throws Exception{
        var f=fixture();start(f);var v=f.player.read(path(f));var d=command(v,List.of(step(2,1,"WALK",false)));tree(f.player.post(path(f)+"/paths",d),200);var changed=new LinkedHashMap<>(d);changed.put("path",List.of(step(1,2,"WALK",false)));tree(f.player.post(path(f)+"/paths",changed),409);changed.put("requestId",UUID.randomUUID());tree(f.player.post(path(f)+"/paths",changed),409);assertEquals(5,f.player.read(path(f)).get("used").asInt());
    }
    @Test void currentTokenCardTurnAndMembershipPermissionsPrecedeOldReceipts()throws Exception{
        var f=fixture();start(f);var v=f.player.read(path(f));var d=command(v,List.of(step(2,1,"WALK",false)));tree(f.player.post(path(f)+"/paths",d),200);
        jdbc.update("delete from character_controller where campaign_id=? and character_id=? and username=?",f.c,f.card,f.user);tree(f.player.post(path(f)+"/paths",d),403);jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,?)",f.c,f.card,f.user);
        jdbc.update("update token set hidden=true where id=?",f.token);tree(f.player.post(path(f)+"/paths",d),404);jdbc.update("update token set hidden=false where id=?",f.token);
        jdbc.update("delete from token_controller where token_id=? and username=?",f.token,f.user);tree(f.player.post(path(f)+"/paths",d),403);jdbc.update("insert into token_controller(token_id,username) values (?,?)",f.token,f.user);
        jdbc.update("delete from character_controller where campaign_id=? and username=?",f.c,f.user);jdbc.update("delete from token_controller where campaign_id=? and username=?",f.c,f.user);jdbc.update("delete from campaign_member where campaign_id=? and username=?",f.c,f.user);tree(f.player.post(path(f)+"/paths",d),403);assertEquals(5,f.dm.read(path(f)).get("used").asInt());
    }
    @Test void strictShapeModeReviewAndDmOnlyOverridesCannotPartiallyWrite()throws Exception{
        var f=fixture();start(f);var v=f.player.read(path(f));var d=command(v,List.of(step(2,1,"WALK",false)));
        for(String key:List.copyOf(d.keySet())){var invalid=new LinkedHashMap<>(d);invalid.remove(key);tree(f.player.post(path(f)+"/paths",invalid),400);}
        var extra=new LinkedHashMap<>(d);extra.put("actor","forged");tree(f.player.post(path(f)+"/paths",extra),400);
        var mode=new LinkedHashMap<>(d);mode.put("path",List.of(step(2,1,"WALK",false),step(3,1,"FLY",false)));tree(f.player.post(path(f)+"/paths",mode),400);
        var override=new LinkedHashMap<>(d);override.put("override",true);override.put("reason","forged DM");tree(f.player.post(path(f)+"/paths",override),403);
        var unreviewed=new LinkedHashMap<>(d);unreviewed.put("reviewed",false);tree(f.player.post(path(f)+"/paths",unreviewed),409);assertEquals(0,f.dm.read(path(f)).get("history").size());assertEquals(0,f.dm.read(path(f)).get("token").get("version").asInt());
    }
    @Test void grantsDoNotResetAndNextActualTurnDoesResetOnce()throws Exception{
        var f=fixture();start(f);var v=f.player.read(path(f));tree(f.player.post(path(f)+"/paths",command(v,List.of(step(2,1,"WALK",false)))),200);
        tree(f.dm.post(base(f.c)+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"kind","GRANT","characterId",f.card,"order",List.of(),"reason","grant")),200);assertEquals(5,f.player.read(path(f)).get("used").asInt());
        tree(f.dm.post(base(f.c)+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"kind","SWITCH","characterId",f.card,"order",List.of(),"reason","same current")),200);assertEquals(5,f.player.read(path(f)).get("used").asInt());
        var combat=f.player.read(base(f.c)+"/combat");var end=Map.of("requestId",UUID.randomUUID(),"expectedVersion",combat.get("endVersion").asLong(),"kind","END_TURN","characterId",f.card,"order",List.of(),"reason","next round");tree(f.player.post(base(f.c)+"/combat",end),200);var after=f.player.read(path(f));assertEquals(0,after.get("used").asInt());assertNotEquals(v.get("budgetVersion"),after.get("budgetVersion"));tree(f.player.post(base(f.c)+"/combat",end),200);assertEquals(after,f.player.read(path(f)));
    }
    @Test void receiptFailureAndDeferredCommitFailureRollBackPositionBudgetHistoryAndBothReceipts()throws Exception{
        var f=fixture();start(f);var before=f.dm.read(path(f));var d=command(before,List.of(step(2,1,"WALK",false)));
        for(boolean deferred:List.of(false,true)){
            String suffix=UUID.randomUUID().toString().replace("-","");String name="a06_fail_"+suffix;
            jdbc.execute("create function "+name+"() returns trigger language plpgsql as $$ begin raise exception 'A06 rollback'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+name+" after insert on movement_path_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+name+"()");
            try{tree(f.dm.post(path(f)+"/paths",d),500);assertEquals(before,f.dm.read(path(f)));assertEquals(0,jdbc.queryForObject("select count(*) from movement_receipt where campaign_id=?",Integer.class,f.c));assertEquals(0,jdbc.queryForObject("select count(*) from movement_path_receipt where campaign_id=?",Integer.class,f.c));}
            finally{jdbc.execute("drop trigger "+name+" on movement_path_receipt");jdbc.execute("drop function "+name+"()");}
        }
        var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->f.dm.post(path(f)+"/paths",d));var two=pool.submit(()->f.dm.post(path(f)+"/paths",d));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
        assertEquals(5,f.dm.read(path(f)).get("used").asInt());assertEquals(1,f.dm.read(path(f)).get("history").size());
    }
    @Test void concurrentDistinctRequestsOnOneVersionHaveOneWinnerAndCardChangesInvalidateDraft()throws Exception{
        var f=fixture();start(f);var v=f.dm.read(path(f));var first=command(v,List.of(step(2,1,"WALK",false)));var second=command(v,List.of(step(1,2,"WALK",false)));
        var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->f.dm.post(path(f)+"/paths",first));var two=pool.submit(()->f.dm.post(path(f)+"/paths",second));var statuses=new ArrayList<>(List.of(one.get().statusCode(),two.get().statusCode()));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);}finally{pool.shutdownNow();}
        var now=f.dm.read(path(f));var draft=command(now,List.of(step(2,2,"WALK",false)));jdbc.update("update character_card set version=version+1 where id=?",f.card);tree(f.dm.post(path(f)+"/paths",draft),409);assertEquals(5,f.dm.read(path(f)).get("used").asInt());
    }
    @Test void movementHeadersStayPrivateForSuccessAndAnonymousFailure()throws Exception{
        var f=fixture();assertEquals("no-store, private",f.dm.get(path(f)).headers().firstValue("Cache-Control").orElse(""));
        var anonymous=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path(f))).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(401,anonymous.statusCode());assertEquals("no-store, private",anonymous.headers().firstValue("Cache-Control").orElse(""));
    }
    @Test void outOfTurnReplayIsDeniedUntilGrantAndNewCombatCannotReuseAnotherCharactersPriorBudget()throws Exception{
        var f=fixture();UUID second=UUID.fromString(tree(f.dm.post(base(f.c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"sheet",CharacterSheet.empty("Second"))),200).get("id").asString());
        start(f,List.of(f.card,second));var original=f.player.read(path(f));var d=command(original,List.of(step(2,1,"WALK",false)));tree(f.player.post(path(f)+"/paths",d),200);
        tree(f.dm.post(base(f.c)+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"kind","SWITCH","characterId",second,"order",List.of(),"reason","next")),200);tree(f.player.post(path(f)+"/paths",d),403);
        tree(f.dm.post(base(f.c)+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"kind","GRANT","characterId",f.card,"order",List.of(),"reason","grant")),200);tree(f.player.post(path(f)+"/paths",d),200);assertEquals(5,f.player.read(path(f)).get("used").asInt());
        tree(f.dm.post(base(f.c)+"/combat",Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"kind","STOP","characterId","","order",List.of(),"reason","stop")),200);
        start(f,List.of(second,f.card));assertEquals(0,f.player.read(path(f)).get("used").asInt());assertEquals(30,f.player.read(path(f)).get("allowance").asInt());
    }
    @Test void malformedContextVersionsAreInvalidInputsRatherThanStaleDrafts()throws Exception{
        var f=fixture();start(f);var d=command(f.dm.read(path(f)),List.of(step(2,1,"WALK",false)));
        for(String key:List.of("expectedVersion","sceneVersion","characterVersion","turnKey","budgetVersion")){var invalid=new LinkedHashMap<>(d);invalid.put(key,key.equals("budgetVersion")?"not-a-uuid":-1);tree(f.dm.post(path(f)+"/paths",invalid),400);}
        assertEquals(0,f.dm.read(path(f)).get("used").asInt());
    }
    @Test void unlinkedControlledTokenIsAnUnavailableCapabilityWithoutPrivateFields()throws Exception{
        var f=fixture();jdbc.update("update token set character_id=null where id=?",f.token);var result=tree(f.player.get(path(f)),200);assertEquals(json.readTree("{\"available\":false}"),result);
        jdbc.update("update token set character_id=? where id=?",f.card,f.token);jdbc.update("update character_card set hidden=true where id=?",f.card);assertEquals(result,tree(f.player.get(path(f)),200));
        jdbc.update("update character_card set hidden=false,import_review=true where id=?",f.card);assertEquals(result,tree(f.player.get(path(f)),200));
        jdbc.update("update token set hidden=true where id=?",f.token);tree(f.player.get(path(f)),404);
    }
}
