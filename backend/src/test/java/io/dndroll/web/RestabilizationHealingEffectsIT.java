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
@org.springframework.context.annotation.Import(RestabilizationHealingEffectsIT.EntropyConfig.class)
class RestabilizationHealingEffectsIT {
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
    void restabilize(Browser dm,Fixture f)throws Exception{stabilize(dm,f);restabilizeAfterStable(dm,f);}
    void restabilizeAfterStable(Browser dm,Fixture f)throws Exception{
        var ds=dm.read(root(f));var d=new LinkedHashMap<String,Object>(Map.of("expectedVersion",ds.get("version").asLong(),"targetVersion",dm.read(card(f,f.target())).get("version").asLong(),"attemptId",ds.get("latest").get("id").asString(),"amount",3,"critical",true,"reviewed",true));
        var dp=tree(dm.post(root(f)+"/damage-preview",d),200);var db=new LinkedHashMap<>(d);db.put("requestId",UUID.randomUUID());db.put("planHash",dp.get("hash").asString());db.put("reason","PRIVATE damage");tree(dm.post(root(f)+"/damages",db),200);
        var rr=new LinkedHashMap<String,Object>(Map.of("expectedVersion",0,"originVersion",3,"targetVersion",2,"helperId",f.helper(),"helperVersion",dm.read(card(f,f.helper())).get("version").asLong(),"reviewed",true));var rb=new LinkedHashMap<>(rr);rb.put("requestId",UUID.randomUUID());rb.put("reason","PRIVATE new Help");tree(dm.post(root(f)+"/restabilization/rolls",rb),200);
        rr.put("expectedVersion",1);rr.put("rollId",dm.read(root(f)+"/restabilization").get("latest").get("id").asString());var rp=tree(dm.post(root(f)+"/restabilization/preview",rr),200);tree(dm.post(root(f)+"/restabilization/applications",apply(rr,rp)),200);
    }
    Map<String,Object> healing(Browser dm,Fixture f,int amount)throws Exception{return new LinkedHashMap<>(Map.of("expectedVersion",dm.read(root(f)+"/restabilization").get("version").asLong(),"originVersion",dm.read(root(f)).get("version").asLong(),"targetVersion",dm.read(card(f,f.target())).get("version").asLong(),"attemptId",dm.read(root(f)+"/restabilization").get("latest").get("id").asString(),"amount",amount,"reviewed",true));}
    Map<String,Object> healApply(Map<String,Object> r,JsonNode p){var b=new LinkedHashMap<>(r);b.put("requestId",UUID.randomUUID());b.put("planHash",p.get("hash").asString());b.put("reason","PRIVATE_HEALING_REASON");return b;}
    @Test void realHealingCapsHpAndRemovesOriginalPairWithoutTouchingHelperDiceOrPrivateOrigin()throws Exception{
        var dm=dm();var f=fixture(dm);restabilize(dm,f);edit(dm,f,f.helper());var before=dm.read(card(f,f.target()));var helper=dm.read(card(f,f.helper()));var state=dm.read(root(f)+"/restabilization");var dice=dm.read(base(f.c())+"/rolls");
        var r=healing(dm,f,100000);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);assertEquals("CANDIDATE",p.get("effect").get("status").asString());assertEquals(12,p.get("effect").get("hp").asInt());assertEquals(p,tree(dm.post(root(f)+"/restabilization/healing-preview",r),200));assertEquals(before,dm.read(card(f,f.target())));
        var ack=tree(dm.post(root(f)+"/restabilization/healings",healApply(r,p)),200);assertEquals(3,ack.get("version").asLong());var after=dm.read(card(f,f.target()));assertEquals(4,after.get("version").asLong());var b=after.get("sheet").get("combat");assertEquals(12,b.get("hp").asInt());assertEquals(4,b.get("tempHp").asInt());assertEquals(0,b.get("deathSuccesses").asInt());assertEquals(0,b.get("deathFailures").asInt());assertEquals(0,after.get("sheet").get("conditions").size());assertEquals(before.get("sheet").get("resources"),after.get("sheet").get("resources"));assertEquals(helper,dm.read(card(f,f.helper())));assertEquals(dice,dm.read(base(f.c())+"/rolls"));
        var latest=dm.read(root(f)+"/restabilization").get("latest");assertEquals("HEALED",latest.get("status").asString());for(String key:List.of("target","helper","roll","reason","completion"))assertEquals(state.get("latest").get(key),latest.get(key));assertEquals(3,dm.read(root(f)+"/restabilization/history").size());assertEquals("HEALED",dm.read(root(f)).get("latest").get("status").asString());var journal=dm.read(root(f)+"/restabilization/healing-history");assertEquals(1,journal.size());assertEquals(before.get("sheet"),journal.get(0).get("before").get("sheet"));assertEquals(after.get("sheet"),journal.get(0).get("after").get("sheet"));assertTrue(journal.toString().contains("PRIVATE_HEALING_REASON"));
    }
    @Test void preciseRetriesSurviveLaterPlayAndDifferentBodiesAndCrossRouteReuseConflict()throws Exception{
        var dm=dm();var f=fixture(dm);var old=attempt(dm,f);tree(dm.post(root(f)+"/rolls",old),200);var sr=review(dm,f);tree(dm.post(root(f)+"/applications",apply(sr,tree(dm.post(root(f)+"/preview",sr),200))),200);
        restabilizeAfterStable(dm,f);var r=healing(dm,f,1);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);var body=healApply(r,p);var clash=new LinkedHashMap<>(body);clash.put("requestId",old.get("requestId"));tree(dm.post(root(f)+"/restabilization/healings",clash),409);var ack=tree(dm.post(root(f)+"/restabilization/healings",body),200);assertEquals(ack,tree(dm.post(root(f)+"/restabilization/healings",body),200));edit(dm,f,f.target());var after=dm.read(card(f,f.target()));assertEquals(ack,tree(dm.post(root(f)+"/restabilization/healings",body),200));assertEquals(after,dm.read(card(f,f.target())));body.put("reason","changed");tree(dm.post(root(f)+"/restabilization/healings",body),409);assertEquals(1,count("restabilization_healing_history",f));
    }
    @Test void authorizationPrecedesReceiptsAndPlayerHistoryContainsNoPrivateHealingReason()throws Exception{
        var dm=dm();var f=fixture(dm);var player=player(f.c());jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,?)",f.c(),UUID.fromString(f.target()),player.username);restabilize(dm,f);var r=healing(dm,f,3);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);var body=healApply(r,p);
        tree(player.post(root(f)+"/restabilization/healing-preview",r),403);tree(player.post(root(f)+"/restabilization/healings",body),403);tree(dm.raw(root(f)+"/restabilization/healings",json.writeValueAsString(body),false),403);tree(new Browser().get(root(f)+"/restabilization/healing-history"),401);tree(player.get(root(f)+"/restabilization/healing-history"),403);tree(dm.post(root(f)+"/restabilization/healings",body),200);
        var history=player.read(card(f,f.target())+"/history");assertFalse(history.toString().contains("PRIVATE_HEALING_REASON"));assertEquals("RESTABILIZATION_HEALING",history.get(history.size()-1).get("kind").asString());var denied=player.get(root(f)+"/restabilization/healing-history");assertTrue(denied.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",f.c(),dm.username);tree(dm.post(root(f)+"/restabilization/healings",body),403);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",f.c(),dm.username);tree(dm.post(root(f)+"/restabilization/healings",body),200);
    }
    @Test void strictInputRejectsClientFinalValuesDuplicatesNullAndInvalidAmounts()throws Exception{
        var dm=dm();var f=fixture(dm);restabilize(dm,f);var r=healing(dm,f,3);
        for(String key:List.of("hp","removed","boundOrigin","actor","helperId")){var bad=new LinkedHashMap<>(r);bad.put(key,1);tree(dm.post(root(f)+"/restabilization/healing-preview",bad),400);}for(Object n:Arrays.asList(0,-1,100001,1.5,"3",null)){var bad=new LinkedHashMap<>(r);bad.put("amount",n);tree(dm.post(root(f)+"/restabilization/healing-preview",bad),400);}
        var missing=new LinkedHashMap<>(r);missing.remove("reviewed");tree(dm.post(root(f)+"/restabilization/healing-preview",missing),400);tree(dm.raw(root(f)+"/restabilization/healing-preview",json.writeValueAsString(r).replace("\"reviewed\":true","\"reviewed\":true,\"reviewed\":false"),true),400);assertEquals(0,count("restabilization_healing_history",f));
    }
    @Test void provenanceCurrentVersionsImportReviewAndReviewAreRequired()throws Exception{
        var dm=dm();var f=fixture(dm);restabilize(dm,f);var r=healing(dm,f,3);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);
        var wrong=healApply(r,p);wrong.put("planHash","0".repeat(64));tree(dm.post(root(f)+"/restabilization/healings",wrong),409);var bad=new LinkedHashMap<>(r);bad.put("attemptId",UUID.randomUUID());tree(dm.post(root(f)+"/restabilization/healing-preview",bad),409);bad=new LinkedHashMap<>(r);bad.put("reviewed",false);assertEquals("MANUAL_REQUIRED",tree(dm.post(root(f)+"/restabilization/healing-preview",bad),200).get("effect").get("status").asString());
        edit(dm,f,f.target());tree(dm.post(root(f)+"/restabilization/healings",healApply(r,p)),409);r=healing(dm,f,3);assertEquals("MANUAL_REQUIRED",tree(dm.post(root(f)+"/restabilization/healing-preview",r),200).get("effect").get("status").asString());
        var g=fixture(dm);restabilize(dm,g);jdbc.update("update character_card set import_review=true where id=?",UUID.fromString(g.target()));assertEquals("MANUAL_REQUIRED",tree(dm.post(root(g)+"/restabilization/healing-preview",healing(dm,g,3)),200).get("effect").get("status").asString());tree(dm.post(root(g)+"/restabilization/healing-preview",r),409);
    }
    @Test void concurrentSameAndDifferentKeysApplyOnce()throws Exception{
        var dm=dm();var pool=Executors.newFixedThreadPool(2);try{for(boolean same:List.of(true,false)){var f=fixture(dm);restabilize(dm,f);var r=healing(dm,f,3);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);var one=healApply(r,p);var two=same?one:healApply(r,p);var a=pool.submit(()->dm.post(root(f)+"/restabilization/healings",one));var b=pool.submit(()->dm.post(root(f)+"/restabilization/healings",two));var ar=a.get();var br=b.get();if(same)assertEquals(tree(ar,200),tree(br,200));else assertEquals(Set.of(200,409),Set.of(ar.statusCode(),br.statusCode()));assertEquals(1,count("restabilization_healing_history",f));assertEquals(3,dm.read(root(f)+"/restabilization").get("version").asLong());assertEquals(5,dm.read(root(f)).get("version").asLong());assertEquals(4,dm.read(card(f,f.target())).get("version").asLong());}}finally{pool.shutdownNow();}
    }
    @Test void immediateAndDeferredFailuresRollbackWholeHealingTransaction()throws Exception{
        var dm=dm();var f=fixture(dm);restabilize(dm,f);var r=healing(dm,f,3);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);var body=healApply(r,p);var before=dm.read(card(f,f.target()));var state=dm.read(root(f));var history=dm.read(card(f,f.target())+"/history");
        jdbc.execute("create function a20_fail() returns trigger language plpgsql as $$ begin raise exception 'injected A20'; return new; end $$");
        try{for(boolean deferred:List.of(false,true)){jdbc.execute(deferred?"create constraint trigger a20_failure after insert on restabilization_healing_history deferrable initially deferred for each row execute function a20_fail()":"create trigger a20_failure before insert on restabilization_healing_history for each row execute function a20_fail()");assertTrue(dm.post(root(f)+"/restabilization/healings",body).statusCode()>=500);assertEquals(before,dm.read(card(f,f.target())));assertEquals(state,dm.read(root(f)));assertEquals(history,dm.read(card(f,f.target())+"/history"));assertEquals(5,count("stabilization_receipt",f));assertEquals(4,count("stabilization_history",f));assertEquals(2,count("restabilization_history",f));assertEquals(0,count("restabilization_healing_history",f));jdbc.execute("drop trigger a20_failure on restabilization_healing_history");}tree(dm.post(root(f)+"/restabilization/healings",body),200);}finally{jdbc.execute("drop trigger if exists a20_failure on restabilization_healing_history");jdbc.execute("drop function a20_fail()");}
    }
    @Test void allVersionsAndOldSourceGatesRemainIndependent()throws Exception{
        var dm=dm();var f=fixture(dm);restabilize(dm,f);var r=healing(dm,f,3);var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);
        for(String key:List.of("expectedVersion","originVersion","targetVersion")){var bad=new LinkedHashMap<>(r);bad.put(key,99);tree(dm.post(root(f)+"/restabilization/healing-preview",bad),409);}
        var legacy=new LinkedHashMap<>(r);legacy.remove("originVersion");legacy.put("expectedVersion",4);legacy.put("attemptId",dm.read(root(f)).get("latest").get("id").asString());tree(dm.post(root(f)+"/healing-preview",legacy),409);
        var other=fixture(dm);tree(dm.post(root(other)+"/restabilization/healing-preview",r),409);
        var sheet=dm.read(card(f,f.target())).get("sheet");var forged=tree(dm.post(base(f.c())+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",dm.read(base(f.c())+"/characters").get("campaignVersion").asLong(),"sheet",sheet)),200);tree(dm.post(base(f.c())+"/characters/"+forged.get("id").asString()+"/stabilization/restabilization/healing-preview",r),409);
        var body=healApply(r,p);tree(dm.post(root(f)+"/restabilization/healings",body),200);tree(dm.post(root(f)+"/restabilization/healing-preview",r),409);assertEquals(3,dm.read(root(f)+"/restabilization/history").size());assertEquals(5,dm.read(root(f)+"/history").size());
    }
    @Test void alteredOriginalDamageJournalBlocksFreshHealingWithoutAnyWrite()throws Exception{
        var dm=dm();var f=fixture(dm);restabilize(dm,f);var r=healing(dm,f,3);var before=dm.read(card(f,f.target()));var original=jdbc.queryForObject("select snapshot::text from stabilization_damage_history where campaign_id=?",String.class,f.c());
        try{jdbc.update("update stabilization_damage_history set snapshot=jsonb_set(snapshot,'{review,amount}','2') where campaign_id=?",f.c());var p=tree(dm.post(root(f)+"/restabilization/healing-preview",r),200);assertEquals("MANUAL_REQUIRED",p.get("effect").get("status").asString());tree(dm.post(root(f)+"/restabilization/healings",healApply(r,p)),409);assertEquals(before,dm.read(card(f,f.target())));assertEquals(0,count("restabilization_healing_history",f));}finally{jdbc.update("update stabilization_damage_history set snapshot=?::jsonb where campaign_id=?",original,f.c());}
    }

}
