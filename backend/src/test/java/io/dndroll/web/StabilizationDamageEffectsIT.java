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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=a05_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@org.springframework.context.annotation.Import(StabilizationDamageEffectsIT.EntropyConfig.class)
class StabilizationDamageEffectsIT {
    static class ControlledEntropy implements io.dndroll.domain.Dice.Entropy { volatile int d20=20;public int die(int sides){return sides==20?d20:2;} }
    @org.springframework.boot.test.context.TestConfiguration static class EntropyConfig { @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary ControlledEntropy controlledEntropy(){return new ControlledEntropy();} }
    @Autowired ControlledEntropy entropy;
    @BeforeEach void resetEntropy(){entropy.d20=20;}
    @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;
    final JsonMapper json=JsonMapper.builder().build();final Set<UUID> campaigns=new HashSet<>();final Set<String> users=new HashSet<>();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf,username;
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return raw(path,json.writeValueAsString(body),true);}
        HttpResponse<String> raw(String path,String body,boolean token)throws Exception{var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");if(token)r.header("X-CSRF-TOKEN",csrf);return client.send(r.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
        Browser()throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        void login(String n)throws Exception{username=n;tree(post("/api/auth/login",Map.of("username",n,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    Browser dm()throws Exception{var b=new Browser();b.login("a05_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","ZZZ Dice test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="a05_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
    String base(UUID c){return "/api/campaigns/"+c;}
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return r.body().isEmpty()?null:json.readTree(r.body());}
    JsonNode create(Browser b,UUID c,long v,String name)throws Exception{return tree(b.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",CharacterSheet.empty(name))),200);}
    Map<String,Object> roll(long version,String character,String key,String expression,String visibility){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"characterId",character,"key",key,"expression",expression,"visibility",visibility,"advantage",List.of(),"disadvantage",List.of());}
    @AfterEach void cleanup(){
        for(UUID c:campaigns){if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='stabilization_state'",Integer.class)>0){jdbc.update("delete from stabilization_receipt where campaign_id=?",c);jdbc.update("delete from stabilization_state where campaign_id=?",c);}if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='attack_preview' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from attack_preview where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='combat_action' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from combat_action where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='dice_roll' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from dice_roll where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}
        jdbc.update("delete from account_command_receipt where actor='a05_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }


    record Fixture(UUID c,String target,String helper) {}
    Fixture fixture(Browser dm)throws Exception{
        var c=campaign(dm);var s=CharacterSheet.empty("Target");var b=s.combat();
        var sheet=s.change(s.name(),new CharacterSheet.Combat(12,0,7,b.speed(),1,2,b.acMode(),b.manualAc(),b.source()),s.resources(),s.conditions(),s.overrides());
        String target=tree(dm.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet)),200).get("id").asString();
        String helper=create(dm,c,1,"Helper").get("id").asString();return new Fixture(c,target,helper);
    }
    String root(Fixture f){return base(f.c())+"/characters/"+f.target()+"/stabilization";}
    String card(Fixture f,String id){return base(f.c())+"/characters/"+id;}
    Map<String,Object> attempt(Browser dm,Fixture f)throws Exception{return new LinkedHashMap<>(Map.of("requestId",UUID.randomUUID(),"expectedVersion",dm.read(root(f)).get("version").asLong(),"targetVersion",dm.read(card(f,f.target())).get("version").asLong(),"helperId",f.helper(),"helperVersion",dm.read(card(f,f.helper())).get("version").asLong(),"reviewed",true,"reason","Ordinary living not-stable character, legal Help action/contact and no exceptions"));}
    Map<String,Object> review(Browser dm,Fixture f)throws Exception{var s=dm.read(root(f));return new LinkedHashMap<>(Map.of("expectedVersion",s.get("version").asLong(),"targetVersion",dm.read(card(f,f.target())).get("version").asLong(),"helperId",f.helper(),"helperVersion",dm.read(card(f,f.helper())).get("version").asLong(),"rollId",s.get("latest").get("id").asString(),"reviewed",true));}
    Map<String,Object> apply(Map<String,Object> review,JsonNode plan){var b=new LinkedHashMap<>(review);b.put("requestId",UUID.randomUUID());b.put("planHash",plan.get("hash").asString());b.put("reason","Confirm Stable at0HP, remains Unconscious");return b;}
    JsonNode roll(Browser dm,Fixture f)throws Exception{tree(dm.post(root(f)+"/rolls",attempt(dm,f)),200);return dm.read(root(f));}
    int count(String table,Fixture f){return jdbc.queryForObject("select count(*) from "+table+" where campaign_id=?",Integer.class,f.c());}
    void edit(Browser dm,Fixture f,String id)throws Exception{var card=dm.read(card(f,id));var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();sheet.put("notes","later edit "+UUID.randomUUID());tree(dm.post(card(f,id),Map.of("requestId",UUID.randomUUID(),"expectedVersion",card.get("version").asLong(),"sheet",sheet,"reason","Later play")),200);}
    void stabilize(Browser dm,Fixture f)throws Exception{roll(dm,f);var r=review(dm,f);tree(dm.post(root(f)+"/applications",apply(r,tree(dm.post(root(f)+"/preview",r),200))),200);}
    Map<String,Object> damage(Browser dm,Fixture f,int amount)throws Exception{return new LinkedHashMap<>(Map.of("expectedVersion",dm.read(root(f)).get("version").asLong(),"targetVersion",dm.read(card(f,f.target())).get("version").asLong(),"attemptId",dm.read(root(f)).get("latest").get("id").asString(),"amount",amount,"critical",false,"reviewed",true));}
    Map<String,Object> damageApply(Map<String,Object> r,JsonNode p){var b=new LinkedHashMap<>(r);b.put("requestId",UUID.randomUUID());b.put("planHash",p.get("hash").asString());b.put("reason","PRIVATE_DAMAGE_REASON");return b;}
    @Test void firstCriticalDamageConsumesTempHpAndRemovesOnlyStableWithoutChangingOriginalEvidence()throws Exception{
        var dm=dm();var f=fixture(dm);stabilize(dm,f);edit(dm,f,f.helper());var before=dm.read(card(f,f.target()));var helper=dm.read(card(f,f.helper()));var state=dm.read(root(f));var dice=dm.read(base(f.c())+"/rolls");
        var r=damage(dm,f,3);r.put("critical",true);var p=tree(dm.post(root(f)+"/damage-preview",r),200);assertEquals("CANDIDATE",p.get("effect").get("status").asString());assertEquals(0,p.get("effect").get("hp").asInt());assertEquals(p,tree(dm.post(root(f)+"/damage-preview",r),200));assertEquals(before,dm.read(card(f,f.target())));
        var ack=tree(dm.post(root(f)+"/damages",damageApply(r,p)),200);assertEquals(3,ack.get("version").asLong());var after=dm.read(card(f,f.target()));assertEquals(2,after.get("version").asLong());var b=after.get("sheet").get("combat");assertEquals(0,b.get("hp").asInt());assertEquals(4,b.get("tempHp").asInt());assertEquals(0,b.get("deathSuccesses").asInt());assertEquals(2,b.get("deathFailures").asInt());assertEquals(1,after.get("sheet").get("conditions").size());assertEquals(before.get("sheet").get("conditions").get(1),after.get("sheet").get("conditions").get(0));assertEquals(before.get("sheet").get("resources"),after.get("sheet").get("resources"));assertEquals(helper,dm.read(card(f,f.helper())));assertEquals(dice,dm.read(base(f.c())+"/rolls"));
        var latest=dm.read(root(f)).get("latest");assertEquals("UNSTABLE",latest.get("status").asString());for(String key:List.of("target","helper","roll","reason","completion"))assertEquals(state.get("latest").get(key),latest.get(key));assertEquals(3,dm.read(root(f)+"/history").size());var journal=dm.read(root(f)+"/damage-history");assertEquals(1,journal.size());assertEquals(before.get("sheet"),journal.get(0).get("before").get("sheet"));assertEquals(after.get("sheet"),journal.get(0).get("after").get("sheet"));assertTrue(journal.toString().contains("PRIVATE_DAMAGE_REASON"));
    }
    @Test void preciseRetriesSurviveLaterPlayAndDifferentBodiesAndCrossRouteReuseConflict()throws Exception{
        var dm=dm();var f=fixture(dm);var old=attempt(dm,f);tree(dm.post(root(f)+"/rolls",old),200);var sr=review(dm,f);tree(dm.post(root(f)+"/applications",apply(sr,tree(dm.post(root(f)+"/preview",sr),200))),200);
        var r=damage(dm,f,1);var p=tree(dm.post(root(f)+"/damage-preview",r),200);var body=damageApply(r,p);var clash=new LinkedHashMap<>(body);clash.put("requestId",old.get("requestId"));tree(dm.post(root(f)+"/damages",clash),409);var ack=tree(dm.post(root(f)+"/damages",body),200);assertEquals(ack,tree(dm.post(root(f)+"/damages",body),200));edit(dm,f,f.target());var after=dm.read(card(f,f.target()));assertEquals(ack,tree(dm.post(root(f)+"/damages",body),200));assertEquals(after,dm.read(card(f,f.target())));body.put("reason","changed");tree(dm.post(root(f)+"/damages",body),409);assertEquals(1,count("stabilization_damage_history",f));
    }
    @Test void authorizationPrecedesReceiptsAndPlayerHistoryContainsNoPrivateDamageReason()throws Exception{
        var dm=dm();var f=fixture(dm);var player=player(f.c());jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,?)",f.c(),UUID.fromString(f.target()),player.username);stabilize(dm,f);var r=damage(dm,f,3);var p=tree(dm.post(root(f)+"/damage-preview",r),200);var body=damageApply(r,p);
        tree(player.post(root(f)+"/damage-preview",r),403);tree(player.post(root(f)+"/damages",body),403);tree(dm.raw(root(f)+"/damages",json.writeValueAsString(body),false),403);tree(new Browser().get(root(f)+"/damage-history"),401);tree(player.get(root(f)+"/damage-history"),403);tree(dm.post(root(f)+"/damages",body),200);
        var history=player.read(card(f,f.target())+"/history");assertFalse(history.toString().contains("PRIVATE_DAMAGE_REASON"));assertEquals("STABILIZATION_DAMAGE",history.get(history.size()-1).get("kind").asString());var denied=player.get(root(f)+"/damage-history");assertTrue(denied.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",f.c(),dm.username);tree(dm.post(root(f)+"/damages",body),403);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",f.c(),dm.username);tree(dm.post(root(f)+"/damages",body),200);
    }
    @Test void strictInputRejectsClientFinalValuesDuplicatesNullAndInvalidAmounts()throws Exception{
        var dm=dm();var f=fixture(dm);stabilize(dm,f);var r=damage(dm,f,3);
        for(String key:List.of("hp","removed","boundOrigin","actor","helperId")){var bad=new LinkedHashMap<>(r);bad.put(key,1);tree(dm.post(root(f)+"/damage-preview",bad),400);}for(Object n:Arrays.asList(-1,220001,1.5,"3",null)){var bad=new LinkedHashMap<>(r);bad.put("amount",n);tree(dm.post(root(f)+"/damage-preview",bad),400);}
        var missing=new LinkedHashMap<>(r);missing.remove("reviewed");tree(dm.post(root(f)+"/damage-preview",missing),400);tree(dm.raw(root(f)+"/damage-preview",json.writeValueAsString(r).replace("\"reviewed\":true","\"reviewed\":true,\"reviewed\":false"),true),400);assertEquals(0,count("stabilization_damage_history",f));
    }
    @Test void provenanceCurrentVersionsImportReviewAndReviewAreRequired()throws Exception{
        var dm=dm();var f=fixture(dm);roll(dm,f);var r=damage(dm,f,3);tree(dm.post(root(f)+"/damage-preview",r),409);var sr=review(dm,f);tree(dm.post(root(f)+"/applications",apply(sr,tree(dm.post(root(f)+"/preview",sr),200))),200);r=damage(dm,f,3);var p=tree(dm.post(root(f)+"/damage-preview",r),200);
        var wrong=damageApply(r,p);wrong.put("planHash","0".repeat(64));tree(dm.post(root(f)+"/damages",wrong),409);var bad=new LinkedHashMap<>(r);bad.put("attemptId",UUID.randomUUID());tree(dm.post(root(f)+"/damage-preview",bad),409);bad=new LinkedHashMap<>(r);bad.put("reviewed",false);assertEquals("MANUAL_REQUIRED",tree(dm.post(root(f)+"/damage-preview",bad),200).get("effect").get("status").asString());
        edit(dm,f,f.target());tree(dm.post(root(f)+"/damages",damageApply(r,p)),409);r=damage(dm,f,3);assertEquals("MANUAL_REQUIRED",tree(dm.post(root(f)+"/damage-preview",r),200).get("effect").get("status").asString());
        var g=fixture(dm);stabilize(dm,g);jdbc.update("update character_card set import_review=true where id=?",UUID.fromString(g.target()));assertEquals("MANUAL_REQUIRED",tree(dm.post(root(g)+"/damage-preview",damage(dm,g,3)),200).get("effect").get("status").asString());tree(dm.post(root(g)+"/damage-preview",r),409);
    }
    @Test void concurrentSameAndDifferentKeysApplyOnce()throws Exception{
        var dm=dm();var pool=Executors.newFixedThreadPool(2);try{for(boolean same:List.of(true,false)){var f=fixture(dm);stabilize(dm,f);var r=damage(dm,f,3);var p=tree(dm.post(root(f)+"/damage-preview",r),200);var one=damageApply(r,p);var two=same?one:damageApply(r,p);var a=pool.submit(()->dm.post(root(f)+"/damages",one));var b=pool.submit(()->dm.post(root(f)+"/damages",two));var ar=a.get();var br=b.get();if(same)assertEquals(tree(ar,200),tree(br,200));else assertEquals(Set.of(200,409),Set.of(ar.statusCode(),br.statusCode()));assertEquals(1,count("stabilization_damage_history",f));assertEquals(3,dm.read(root(f)).get("version").asLong());assertEquals(2,dm.read(card(f,f.target())).get("version").asLong());}}finally{pool.shutdownNow();}
    }
    @Test void immediateAndDeferredFailuresRollbackWholeDamageTransaction()throws Exception{
        var dm=dm();var f=fixture(dm);stabilize(dm,f);var r=damage(dm,f,3);var p=tree(dm.post(root(f)+"/damage-preview",r),200);var body=damageApply(r,p);var before=dm.read(card(f,f.target()));var state=dm.read(root(f));var history=dm.read(card(f,f.target())+"/history");
        jdbc.execute("create function a18_fail() returns trigger language plpgsql as $$ begin raise exception 'injected A17'; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute(deferred?"create constraint trigger a18_failure after insert on stabilization_damage_history deferrable initially deferred for each row execute function a18_fail()":"create trigger a18_failure before insert on stabilization_damage_history for each row execute function a18_fail()");assertTrue(dm.post(root(f)+"/damages",body).statusCode()>=500);assertEquals(before,dm.read(card(f,f.target())));assertEquals(state,dm.read(root(f)));assertEquals(history,dm.read(card(f,f.target())+"/history"));assertEquals(2,count("stabilization_receipt",f));assertEquals(2,count("stabilization_history",f));assertEquals(0,count("stabilization_damage_history",f));jdbc.execute("drop trigger a18_failure on stabilization_damage_history");}tree(dm.post(root(f)+"/damages",body),200);}finally{jdbc.execute("drop trigger if exists a18_failure on stabilization_damage_history");jdbc.execute("drop function a18_fail()");}
    }
    @Test void zeroAndPotentiallyFatalAmountsHaveNoExecutableApplication()throws Exception{
        var dm=dm();var f=fixture(dm);stabilize(dm,f);var before=dm.read(card(f,f.target()));for(int amount:List.of(0,12,220000)){var r=damage(dm,f,amount);r.put("critical",true);var p=tree(dm.post(root(f)+"/damage-preview",r),200);assertEquals("MANUAL_REQUIRED",p.get("effect").get("status").asString());assertTrue(p.get("effect").get("hp").isNull());tree(dm.post(root(f)+"/damages",damageApply(r,p)),409);assertEquals(before,dm.read(card(f,f.target())));}assertEquals(0,count("stabilization_damage_history",f));assertEquals(2,count("stabilization_history",f));
    }
}
