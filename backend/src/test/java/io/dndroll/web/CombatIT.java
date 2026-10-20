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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p06_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class CombatIT {
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
    Browser dm()throws Exception{var b=new Browser();b.login("p06_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","ZZZ Dice test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="p06_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
    String base(UUID c){return "/api/campaigns/"+c;}
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return r.body().isEmpty()?null:json.readTree(r.body());}
    JsonNode create(Browser b,UUID c,long v,String name)throws Exception{return tree(b.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",CharacterSheet.empty(name))),200);}
    Map<String,Object> roll(long version,String character,String key,String expression,String visibility){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"characterId",character,"key",key,"expression",expression,"visibility",visibility,"advantage",List.of(),"disadvantage",List.of());}
    @AfterEach void cleanup(){
        for(UUID c:campaigns){if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='combat_action' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from combat_action where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='dice_roll' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from dice_roll where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}
        jdbc.update("delete from account_command_receipt where actor='p06_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }

    String attackCard(Browser p,UUID c,long version,String name)throws Exception{
        var sheet=(tools.jackson.databind.node.ObjectNode)json.valueToTree(CharacterSheet.empty(name));
        sheet.set("attacks",json.valueToTree(List.of(new CharacterSheet.Attack("sword","Sword","STR",true,0,0,"1d6","slashing","",CharacterSheet.manual()))));
        ((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("maxHp",20).put("hp",20).put("tempHp",5);
        return tree(p.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"sheet",sheet)),200).get("id").asString();
    }
    Map<String,Object> declaration(String a,String b){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"characterId",a,"targetId",b,"attackId","sword","visibility","PUBLIC");}
    Map<String,Object> event(long version,String kind,String roll,String verdict,List<?> changes,String correction,String reason){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"kind",kind,"rollId",roll,"verdict",verdict,"changes",changes,"correctionOf",correction,"reason",reason);}
    Map<String,Object> change(String id,long version,int hp,int temp){return Map.of("characterId",id,"expectedVersion",version,"hp",hp,"tempHp",temp,"resources",List.of());}
    @Test void movementRequiresBothControlsCurrentTurnOrGrantAndRechecksOldReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"Hero"),b=attackCard(p,c,1,"Other");var scene=dm.read(base(c)+"/scene");UUID token=UUID.randomUUID();
        jdbc.update("insert into token(id,scene_id,name,x,y,character_id) values (?,?,?,0,0,?)",token,UUID.fromString(scene.get("id").asString()),"Hero token",UUID.fromString(a));jdbc.update("insert into token_controller(token_id,username) values (?,?)",token,p.username);
        try{
            String path=base(c)+"/tokens/"+token+"/moves";var move=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",1,"y",1);tree(p.post(path,move),200);
            initiative(dm,c,List.of(b,a));tree(dm.post(base(c)+"/combat",turn(0,"START","",List.of())),200);assertFalse(p.read(base(c)+"/scene").get("tokens").get(0).get("canMove").asBoolean());tree(p.post(path,move),403);
            tree(dm.post(base(c)+"/combat",turn(1,"GRANT",a,List.of())),200);assertTrue(p.read(base(c)+"/scene").get("tokens").get(0).get("canMove").asBoolean());tree(p.post(path,move),200);
            tree(dm.post(base(c)+"/combat",turn(2,"SKIP","",List.of())),200);tree(p.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"x",2,"y",2)),200);
            jdbc.update("delete from character_controller where character_id=? and username=?",UUID.fromString(a),p.username);tree(p.post(path,move),403);
            tree(dm.post(base(c)+"/combat",turn(3,"STOP","",List.of())),200);tree(p.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"x",3,"y",3)),200);
        }finally{jdbc.update("delete from movement_receipt where token_id=?",token);jdbc.update("delete from token_controller where token_id=?",token);jdbc.update("delete from token where id=?",token);}
    }
    @Test void concurrentApplicationHasOneEffectAndCommitFailureRollsBackSheetsJournalAndReceipt()throws Exception{
        var dm=dm();var c=campaign(dm);String a=attackCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Enemy"),path=base(c)+"/actions";String id=tree(dm.post(path,declaration(a,b)),200).get("id").asString(),ep=path+"/"+id+"/events";
        tree(dm.post(ep,event(0,"VERDICT","","HIT",List.of(),"","Hit")),200);var apply=event(1,"APPLY","","",List.of(change(a,0,19,0),change(b,0,17,0)),"","Explicit application");
        String suffix=UUID.randomUUID().toString().replace("-","");jdbc.execute("create function p06_fail_"+suffix+"() returns trigger language plpgsql as $$ begin raise exception 'receipt commit failure'; end $$");jdbc.execute("create constraint trigger p06_fail_"+suffix+" after insert on combat_command_receipt deferrable initially deferred for each row execute function p06_fail_"+suffix+"()");
        try{tree(dm.post(ep,apply),500);assertEquals(1,dm.read(path+"/"+id).get("version").asLong());assertEquals(20,dm.read(base(c)+"/characters/"+a).get("sheet").get("combat").get("hp").asInt());assertEquals(1,dm.read(base(c)+"/characters/"+b+"/history").size());}
        finally{jdbc.execute("drop trigger p06_fail_"+suffix+" on combat_command_receipt");jdbc.execute("drop function p06_fail_"+suffix+"()");}
        var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(ep,apply));var two=pool.submit(()->dm.post(ep,apply));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
        assertEquals(1,dm.read(base(c)+"/characters/"+a).get("version").asLong());assertEquals(2,dm.read(path+"/"+id).get("events").size());var changed=new HashMap<>(apply);changed.put("reason","Changed body");tree(dm.post(ep,changed),409);
        String original=dm.read(path+"/"+id).get("events").get(1).get("id").asString();var pool2=Executors.newFixedThreadPool(2);try{var one=pool2.submit(()->dm.post(ep,event(2,"CORRECT","","",List.of(change(b,1,18,0)),original,"Correction A")));var two=pool2.submit(()->dm.post(ep,event(2,"CORRECT","","",List.of(change(b,1,19,0)),original,"Correction B")));assertEquals(List.of(200,409),java.util.stream.Stream.of(one.get().statusCode(),two.get().statusCode()).sorted().toList());}finally{pool2.shutdownNow();}
    }
    @Test void strictFieldsCrossCampaignSourcesAndRevokedReplaysCannotMutate()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"Hero"),b=attackCard(dm,c,1,"Enemy"),path=base(c)+"/actions";var body=declaration(a,b);String id=tree(p.post(path,body),200).get("id").asString();
        assertEquals("no-store, private",p.get(path).headers().firstValue("Cache-Control").orElse(""));assertEquals("no-store, private",new Browser().get(base(c)+"/combat").headers().firstValue("Cache-Control").orElse(""));
        var missing=new HashMap<>(body);missing.remove("expectedVersion");tree(p.post(path,missing),400);missing=new HashMap<>(body);missing.put("actor","dm");tree(p.post(path,missing),400);tree(p.raw(path,json.writeValueAsString(body),false),403);tree(new Browser().get(path),401);tree(player(campaign(dm)).get(path),403);
        String foreign=attackCard(dm,campaign(dm),0,"Foreign");tree(dm.post(path,declaration(a,foreign)),404);tree(p.post(path+"/"+id+"/events",event(0,"APPLY","","",List.of(change(b,0,0,0)),"","No player damage")),403);
        jdbc.update("delete from character_controller where campaign_id=? and username=?",c,p.username);tree(p.post(path,body),403);jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,p.username);tree(p.get(path),403);tree(p.post(path,body),403);
    }
    Map<String,Object> turn(long v,String kind,String character,List<String> order){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"kind",kind,"characterId",character,"order",order,"reason","DM turn decision");}
    void initiative(Browser dm,UUID c,List<String> order)throws Exception{
        var participants=order.stream().map(id->Map.of("characterId",id,"expectedVersion",0,"kind","PC","group","","surprised",false,"advantage",List.of(),"disadvantage",List.of())).toList();
        tree(dm.post(base(c)+"/initiative",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",participants,"reason","Roster")),200);
        tree(dm.post(base(c)+"/initiative/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",order,"reason","Confirmed")),200);
    }
    @Test void independentCharactersEndTurnsOnlyOnceAndDmCanOverride()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"First"),b=attackCard(p,c,1,"Second"),path=base(c)+"/combat";initiative(dm,c,List.of(a,b));
        tree(p.post(path,turn(0,"START","",List.of())),403);tree(dm.post(path,turn(0,"START","",List.of())),200);
        var end=turn(p.read(path).get("endVersion").asLong(),"END_TURN",a,List.of());tree(player(c).post(path,end),403);var ack=tree(p.post(path,end),200);assertEquals(ack,tree(p.post(path,end),200));assertEquals(b,dm.read(path).get("currentId").asString());
        long next=p.read(path).get("endVersion").asLong();tree(p.post(path,turn(next,"END_TURN",a,List.of())),403);tree(p.post(path,turn(next,"END_TURN",b,List.of())),200);assertEquals(2,dm.read(path).get("round").asInt());
        tree(dm.post(path,turn(3,"SWITCH",b,List.of())),200);tree(dm.post(path,turn(4,"ORDER","",List.of(b,a))),200);assertEquals(b,dm.read(path).get("currentId").asString());
        tree(dm.post(path,turn(5,"STOP","",List.of())),200);assertEquals("IDLE",dm.read(path).get("status").asString());assertEquals(6,dm.read(path+"/history").size());
    }
    @Test void privateRollEventsAndDmGrantsDoNotExposeGlobalVersionCounters()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"Hero"),b=attackCard(dm,c,1,"Enemy"),path=base(c)+"/actions";
        String id=tree(p.post(path,declaration(a,b)),200).get("id").asString();long version=p.read(path+"/"+id).get("mutationVersion").asLong();
        var dark=tree(dm.post(base(c)+"/rolls",roll(0,a,"attack:sword","","DM")),200);tree(dm.post(path+"/"+id+"/events",event(0,"LINK_ROLL",dark.get("id").asString(),"",List.of(),"","Private")),200);
        assertEquals(version,p.read(path+"/"+id).get("mutationVersion").asLong());
        initiative(dm,c,List.of(a,b));tree(dm.post(base(c)+"/combat",turn(0,"START","",List.of())),200);var before=p.read(base(c)+"/combat");
        tree(dm.post(base(c)+"/combat",turn(1,"GRANT",b,List.of())),200);var after=p.read(base(c)+"/combat");assertEquals(before,after,"DM-only movement grants must not expose their number through an end-turn counter");
    }
    @Test void declarationLinksServerRollAndKeepsHistoricalAttackWithoutSpending()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"Hero"),b=attackCard(dm,c,1,"Enemy");String path=base(c)+"/actions";var body=declaration(a,b);
        var ack=tree(p.post(path,body),200);assertEquals(ack,tree(p.post(path,body),200));String id=ack.get("id").asString();var roll=tree(p.post(base(c)+"/rolls",roll(0,a,"attack:sword","","PUBLIC")),200);
        tree(p.post(path+"/"+id+"/events",event(0,"LINK_ROLL",roll.get("id").asString(),"",List.of(),"","")),200);
        var detail=dm.read(path+"/"+id);assertEquals("Sword",detail.get("attack").get("name").asString());assertEquals(roll,detail.get("events").get(0).get("roll"));assertEquals(0,p.read(base(c)+"/characters/"+a).get("version").asLong());
        tree(player(c).post(path,body),403);tree(p.post(path+"/"+id+"/events",event(1,"VERDICT","","HIT",List.of(),"","No player verdict")),403);
    }
    @Test void manualApplicationCorrectionAndRetriesKeepLaterChangesAndOriginalEvents()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"Hero"),b=attackCard(dm,c,1,"Enemy"),path=base(c)+"/actions";
        String id=tree(p.post(path,declaration(a,b)),200).get("id").asString(),ep=path+"/"+id+"/events";
        tree(dm.post(ep,event(0,"VERDICT","","HIT",List.of(),"","PRIVATE AC reason")),200);
        var apply=event(1,"APPLY","","",List.of(change(b,0,18,0)),"","PRIVATE damage source");var applied=tree(dm.post(ep,apply),200);assertEquals(applied,tree(dm.post(ep,apply),200));assertEquals(18,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());
        var original=dm.read(path+"/"+id).get("events").get(1);
        tree(dm.post(base(c)+"/characters/"+b+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"resourceId","hp","delta",-2,"reason","Later independent operation")),200);
        tree(dm.post(ep,event(2,"CORRECT","","",List.of(change(b,1,19,0)),original.get("id").asString(),"Stale correction")),409);
        tree(dm.post(ep,event(2,"CORRECT","","",List.of(change(b,2,17,0)),original.get("id").asString(),"Fix damage while preserving later loss")),200);
        var detail=dm.read(path+"/"+id);assertEquals(original,detail.get("events").get(1));assertEquals(3,detail.get("events").size());assertEquals(17,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());
        var pub=p.read(path+"/"+id).toString();for(String secret:List.of("PRIVATE","hp","resources","before","reason"))assertFalse(pub.contains(secret),pub);
    }
    @Test void hiddenTargetsDarkRollsAndRevocationAreCheckedBeforeReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=attackCard(p,c,0,"Hero"),b=attackCard(dm,c,1,"SECRET TARGET"),path=base(c)+"/actions";var body=declaration(a,b);
        String id=tree(p.post(path,body),200).get("id").asString();var dark=tree(dm.post(base(c)+"/rolls",roll(0,a,"attack:sword","","DM")),200);
        tree(dm.post(path+"/"+id+"/events",event(0,"LINK_ROLL",dark.get("id").asString(),"",List.of(),"","private")),200);
        assertEquals(0,p.read(path+"/"+id).get("events").size());assertFalse(p.read(path).toString().contains(dark.get("id").asString()));
        tree(dm.post(base(c)+"/characters/"+b+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"controllers",List.of(),"hidden",true,"shareFull",false,"approveImport",true,"reason","Hide target")),200);
        assertEquals(0,p.read(path).get("actions").size());tree(p.get(path+"/"+id),404);tree(p.get(path+"?before="+id),404);tree(p.post(path,body),404);
    }
}
