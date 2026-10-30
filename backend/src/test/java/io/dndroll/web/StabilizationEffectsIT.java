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
@org.springframework.context.annotation.Import(StabilizationEffectsIT.EntropyConfig.class)
class StabilizationEffectsIT {
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
    @Test void savesOneCheckAndExplicitApplicationResetsCountersWithoutHealingOrChangingHelper()throws Exception{
        var dm=dm();var f=fixture(dm);var before=dm.read(card(f,f.target()));var helper=dm.read(card(f,f.helper()));var input=attempt(dm,f);
        var ack=tree(dm.post(root(f)+"/rolls",input),200);assertEquals(ack,tree(dm.post(root(f)+"/rolls",input),200));assertEquals(1,count("dice_roll",f));assertEquals(before,dm.read(card(f,f.target())));
        var state=dm.read(root(f));assertEquals(1,state.get("version").asLong());assertEquals("ROLLED",state.get("latest").get("status").asString());
        var review=review(dm,f);var plan=tree(dm.post(root(f)+"/preview",review),200);assertEquals("CANDIDATE",plan.get("effect").get("status").asString());assertEquals(plan,tree(dm.post(root(f)+"/preview",review),200));assertEquals(1,count("dice_roll",f));
        var body=apply(review,plan);var applied=tree(dm.post(root(f)+"/applications",body),200);assertEquals(2,applied.get("version").asLong());assertEquals(applied,tree(dm.post(root(f)+"/applications",body),200));
        var after=dm.read(card(f,f.target()));var combat=after.get("sheet").get("combat");assertEquals(1,after.get("version").asLong());assertEquals(0,combat.get("hp").asInt());assertEquals(7,combat.get("tempHp").asInt());assertEquals(0,combat.get("deathSuccesses").asInt());assertEquals(0,combat.get("deathFailures").asInt());assertEquals(2,after.get("sheet").get("conditions").size());assertEquals(helper,dm.read(card(f,f.helper())));
        var history=dm.read(card(f,f.target())+"/history");assertEquals("STABILIZATION",history.get(history.size()-1).get("kind").asString());assertEquals(before.get("sheet"),history.get(history.size()-1).get("before").get("sheet"));assertEquals(2,dm.read(root(f)+"/history").size());
        edit(dm,f,f.target());var later=dm.read(card(f,f.target()));assertEquals(applied,tree(dm.post(root(f)+"/applications",body),200));assertEquals(ack,tree(dm.post(root(f)+"/rolls",input),200));assertEquals(later,dm.read(card(f,f.target())));assertEquals(1,count("dice_roll",f));
        var changed=new LinkedHashMap<>(body);changed.put("reason","changed retry");tree(dm.post(root(f)+"/applications",changed),409);
    }
    @Test void failedCheckPersistsThenNewHelpActionInvalidatesOldAttempt()throws Exception{
        var dm=dm();var f=fixture(dm);entropy.d20=1;roll(dm,f);var old=review(dm,f);var plan=tree(dm.post(root(f)+"/preview",old),200);assertEquals("MANUAL_REQUIRED",plan.get("effect").get("status").asString());tree(dm.post(root(f)+"/applications",apply(old,plan)),409);
        entropy.d20=20;roll(dm,f);assertEquals(2,count("dice_roll",f));old.put("expectedVersion",2);tree(dm.post(root(f)+"/preview",old),409);var next=review(dm,f);assertEquals("CANDIDATE",tree(dm.post(root(f)+"/preview",next),200).get("effect").get("status").asString());
    }
    @Test void permissionsCsrfStrictFieldsAndPrivateEvidenceAreEnforced()throws Exception{
        var dm=dm();var f=fixture(dm);var input=attempt(dm,f);var player=player(f.c());var anon=new Browser();assertEquals(401,anon.get(root(f)).statusCode());tree(player.get(root(f)),403);tree(player.post(root(f)+"/rolls",input),403);tree(dm.raw(root(f)+"/rolls",json.writeValueAsString(input),false),403);
        for(String name:List.of("total","boundCheck","actor","dc")){var b=new LinkedHashMap<>(input);b.put(name,20);tree(dm.post(root(f)+"/rolls",b),400);}var missing=new LinkedHashMap<>(input);missing.remove("reviewed");tree(dm.post(root(f)+"/rolls",missing),400);
        tree(dm.raw(root(f)+"/rolls",json.writeValueAsString(input).replace("\"reviewed\":true","\"reviewed\":true,\"reviewed\":false"),true),400);
        var response=dm.get(root(f));assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));tree(dm.post(root(f)+"/rolls",input),200);assertEquals(0,player.read(base(f.c())+"/rolls").get("rolls").size());tree(player.get(root(f)+"/history"),403);
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",f.c(),dm.username);tree(dm.post(root(f)+"/rolls",input),403);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",f.c(),dm.username);
    }
    @Test void currentTargetAndHelperVersionsHashImportReviewAndCrossCampaignObjectsAreRequired()throws Exception{
        var dm=dm();for(boolean helper:List.of(false,true)){var f=fixture(dm);roll(dm,f);var review=review(dm,f);var plan=tree(dm.post(root(f)+"/preview",review),200);var wrong=apply(review,plan);wrong.put("planHash","0".repeat(64));tree(dm.post(root(f)+"/applications",wrong),409);edit(dm,f,helper?f.helper():f.target());tree(dm.post(root(f)+"/applications",apply(review,plan)),409);}
        var f=fixture(dm);var body=attempt(dm,f);var other=fixture(dm);body.put("helperId",other.helper());tree(dm.post(root(f)+"/rolls",body),404);body.put("helperId",f.target());tree(dm.post(root(f)+"/rolls",body),409);
        body=attempt(dm,f);jdbc.update("update character_card set import_review=true where id=?",UUID.fromString(f.helper()));tree(dm.post(root(f)+"/rolls",body),409);assertEquals(0,count("dice_roll",f));
    }
    @Test void genericDiceAndPreseededRequestCannotSubstituteDedicatedCheck()throws Exception{
        var dm=dm();var f=fixture(dm);var body=attempt(dm,f);UUID request=(UUID)body.get("requestId");UUID child=UUID.nameUUIDFromBytes(("A16_STABILIZATION:"+request).getBytes(java.nio.charset.StandardCharsets.UTF_8));var generic=roll(0,f.helper(),"skill:medicine","","DM");generic=new LinkedHashMap<>(generic);generic.put("requestId",child);var ordinary=tree(dm.post(base(f.c())+"/rolls",generic),200);
        tree(dm.post(root(f)+"/rolls",body),409);assertEquals(0,dm.read(root(f)).get("version").asLong());assertEquals(1,count("dice_roll",f));
        roll(dm,f);var review=review(dm,f);review.put("rollId",ordinary.get("id").asString());tree(dm.post(root(f)+"/preview",review),409);
    }
    @Test void sameRequestAndDifferentRequestSameVersionRacesHaveOneEffect()throws Exception{
        var dm=dm();var f=fixture(dm);var body=attempt(dm,f);var pool=Executors.newFixedThreadPool(2);try{
            var a=pool.submit(()->dm.post(root(f)+"/rolls",body));var b=pool.submit(()->dm.post(root(f)+"/rolls",body));assertEquals(tree(a.get(),200),tree(b.get(),200));assertEquals(1,count("dice_roll",f));
            var review=review(dm,f);var plan=tree(dm.post(root(f)+"/preview",review),200);var one=apply(review,plan);var two=apply(review,plan);var x=pool.submit(()->dm.post(root(f)+"/applications",one));var y=pool.submit(()->dm.post(root(f)+"/applications",two));assertEquals(Set.of(200,409),Set.of(x.get().statusCode(),y.get().statusCode()));assertEquals(1,dm.read(card(f,f.target())).get("version").asLong());
        }finally{pool.shutdownNow();}
    }
    @Test void targetControllerHistoryNeverDisclosesPrivateCheckOrApplicationReason()throws Exception{
        var dm=dm();var f=fixture(dm);roll(dm,f);var review=review(dm,f);var plan=tree(dm.post(root(f)+"/preview",review),200);var body=apply(review,plan);body.put("reason","PRIVATE_APPLICATION_REASON");tree(dm.post(root(f)+"/applications",body),200);
        var player=player(f.c());jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,?)",f.c(),UUID.fromString(f.target()),player.username);
        var history=player.read(card(f,f.target())+"/history");assertFalse(history.toString().contains("PRIVATE_APPLICATION_REASON"));assertFalse(history.toString().contains("total="));
        tree(player.get(root(f)),403);tree(player.get(root(f)+"/history"),403);assertTrue(dm.read(root(f)+"/history").toString().contains("PRIVATE_APPLICATION_REASON"));
    }
    @Test void databaseFailureRollsBackDiceStateHistoryReceiptAndCardForBothStages()throws Exception{
        var dm=dm();var f=fixture(dm);var input=attempt(dm,f);var before=dm.read(card(f,f.target()));dm.read(root(f));
        jdbc.execute("create function a16b_fail() returns trigger language plpgsql as $$ begin raise exception 'injected'; return new; end $$");
        try{
            jdbc.execute("create trigger a16b_failure before insert or update on stabilization_state for each row execute function a16b_fail()");
            assertTrue(dm.post(root(f)+"/rolls",input).statusCode()>=500);assertEquals(0,count("dice_roll",f));assertEquals(0,count("stabilization_history",f));assertEquals(0,count("stabilization_receipt",f));
            jdbc.execute("drop trigger a16b_failure on stabilization_state");tree(dm.post(root(f)+"/rolls",input),200);var review=review(dm,f);var plan=tree(dm.post(root(f)+"/preview",review),200);var apply=apply(review,plan);
            jdbc.execute("create constraint trigger a16b_failure after insert or update on stabilization_state deferrable initially deferred for each row execute function a16b_fail()");assertTrue(dm.post(root(f)+"/applications",apply).statusCode()>=500);assertEquals(before,dm.read(card(f,f.target())));assertEquals(1,count("stabilization_history",f));assertEquals(1,count("stabilization_receipt",f));
            jdbc.execute("drop trigger a16b_failure on stabilization_state");tree(dm.post(root(f)+"/applications",apply),200);
        }finally{jdbc.execute("drop trigger if exists a16b_failure on stabilization_state");jdbc.execute("drop function a16b_fail()");}
    }
}
