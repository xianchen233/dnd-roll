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
@org.springframework.context.annotation.Import(KnockoutFirstAidEffectsIT.EntropyConfig.class)
class KnockoutFirstAidEffectsIT {
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
        for(UUID c:campaigns){if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='attack_preview' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from attack_preview where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='combat_action' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from combat_action where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='dice_roll' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from dice_roll where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}
        jdbc.update("delete from account_command_receipt where actor='a05_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
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

    String previews(UUID c){return base(c)+"/attack-previews";}
    String catalogCard(Browser dm,UUID c,long v,String name)throws Exception{
        var catalog=dm.read(base(c)+"/content");var entries=catalog.get("entries");JsonNode entry=null;for(var e:entries)if(e.get("id").asString().equals("e1086d6d-e523-53d9-93b9-7aa24c718e39")){entry=e;break;}
        assertNotNull(entry);String id=create(dm,c,v,name).get("id").asString();
        tree(dm.post(base(c)+"/characters/"+id+"/content",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"policyVersion",v+1,"entryId",entry.get("id").asString(),"ability","STR","castingId","","reason","Trusted catalog")),200);return id;
    }
    Map<String,Object> previewBody(Browser dm,UUID c,String a,String b)throws Exception{
        var s=dm.read(base(c)+"/characters/"+a);var t=dm.read(base(c)+"/characters/"+b);var out=new LinkedHashMap<String,Object>();
        out.put("requestId",UUID.randomUUID());out.put("characterId",a);out.put("sourceVersion",s.get("version").asLong());out.put("targetId",b);out.put("targetVersion",t.get("version").asLong());out.put("attackId",s.get("sheet").get("attacks").get(0).get("id").asString());out.put("reviewed",true);
        for(String key:List.of("exceptions","advantage","disadvantage","immunities","resistances","vulnerabilities"))out.put(key,List.of());return out;
    }
    Map<String,Object> command(JsonNode p,String kind,List<?> changes,String correction,String summary){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",p.get("version").asLong(),"kind",kind,"previewHash",p.get("hash").asString(),"changes",changes,"correctionOf",correction,"reason","Private DM reason","summary",summary);}
    JsonNode preview(Browser dm,UUID c,Map<String,Object> body)throws Exception{String id=tree(dm.post(previews(c),body),200).get("id").asString();return dm.read(previews(c)+"/"+id);}
    JsonNode send(Browser dm,UUID c,JsonNode p,String kind,List<?> changes,String correction,String summary)throws Exception{tree(dm.post(previews(c)+"/"+p.get("id").asString()+"/events",command(p,kind,changes,correction,summary)),200);return dm.read(previews(c)+"/"+p.get("id").asString());}


    String root(UUID c,JsonNode p){return previews(c)+"/"+p.get("id").asString();}
    Map<String,Object> review(JsonNode p){return Map.of("expectedVersion",p.get("version").asLong(),"previewHash",p.get("hash").asString(),"treatment","KNOCK_OUT_MELEE","reviewed",true);}
    Map<String,Object> confirm(JsonNode p){var v=new LinkedHashMap<>(review(p));v.put("requestId",UUID.randomUUID());v.put("reason","Club melee knockout; no immunity or linked exception; manually manage rest and wake-up");return v;}
    String snapshot(UUID c){var out=new LinkedHashMap<String,Object>();for(String t:List.of("attack_preview","attack_preview_receipt","combat_action","combat_command_receipt","dice_roll","dice_command_receipt","character_card","timeline_entry"))out.put(t,jdbc.queryForList("select to_jsonb(t)::text as row from "+t+" t where campaign_id=? order by to_jsonb(t)::text",c));out.put("history",jdbc.queryForList("select to_jsonb(h)::text from character_history h join character_card c on c.id=h.character_id where c.campaign_id=? order by h.character_id,h.version",c));return json.writeValueAsString(out);}

    JsonNode knocked(Browser dm,UUID c,boolean dagger,int max)throws Exception{
        String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Target").get("id").asString();
        if(!dagger)tree(dm.post(base(c)+"/characters/"+a+"/content",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"policyVersion",2,"entryId","916bf8b1-a24e-5dea-be38-2caee47dad54","ability","STR","castingId","","reason","Club")),200);
        var card=dm.read(base(c)+"/characters/"+b);var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("hp",Math.min(3,max)).put("maxHp",max).put("tempHp",1);
        tree(dm.post(base(c)+"/characters/"+b,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet,"reason","Fixture")),200);
        var body=previewBody(dm,c,a,b);if(!dagger)body.put("attackId","content:916bf8b1-a24e-5dea-be38-2caee47dad54");
        var pr=preview(dm,c,body);tree(dm.post(root(c,pr)+(dagger?"/dagger-knockout-confirmations":"/knockout-confirmations"),confirm(pr)),200);
        return send(dm,c,dm.read(root(c,pr)),"APPLY",List.of(),"","");
    }
    String target(JsonNode p){return p.get("evidence").get("target").get("id").asString();}


    String helper(JsonNode p){return p.get("evidence").get("source").get("id").asString();}
    Map<String,Object> aid(Browser dm,UUID c,JsonNode p)throws Exception{return new LinkedHashMap<>(Map.of("requestId",UUID.randomUUID(),"expectedVersion",p.get("version").asLong(),"previewHash",p.get("hash").asString(),"targetVersion",dm.read(base(c)+"/characters/"+target(p)).get("version").asLong(),"helperId",helper(p),"helperVersion",dm.read(base(c)+"/characters/"+helper(p)).get("version").asLong(),"reviewed",true,"reason","Reviewed available aid action, legal contact and no unsupported exceptions"));}
    JsonNode attempt(Browser dm,UUID c,JsonNode p)throws Exception{tree(dm.post(root(c,p)+"/knockout-first-aid-rolls",aid(dm,c,p)),200);return dm.read(root(c,p));}
    Map<String,Object> aidReview(Browser dm,UUID c,JsonNode p)throws Exception{var b=aid(dm,c,p);b.remove("requestId");b.remove("reason");b.put("rollId",p.get("events").get(p.get("events").size()-1).get("id").asString());return b;}
    JsonNode plan(Browser dm,UUID c,JsonNode p,Map<String,Object> review)throws Exception{return tree(dm.post(root(c,p)+"/knockout-first-aid-effect",review),200);}
    Map<String,Object> complete(Map<String,Object> review,JsonNode plan){var b=new LinkedHashMap<>(review);b.put("requestId",UUID.randomUUID());b.put("planHash",plan.get("hash").asString());b.put("reason","Confirm successful bound first aid; remains prone");return b;}
    void bonus(Browser dm,UUID c,JsonNode p,int value)throws Exception{var card=dm.read(base(c)+"/characters/"+helper(p));var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();for(var skill:sheet.get("skills"))if(skill.get("id").asString().equals("medicine"))((tools.jackson.databind.node.ObjectNode)skill).put("bonus",value);tree(dm.post(base(c)+"/characters/"+helper(p),Map.of("requestId",UUID.randomUUID(),"expectedVersion",card.get("version").asLong(),"sheet",sheet,"reason","Saved flat Medicine bonus")),200);}
    @Test void bothOriginsBindOneServerCheckThenExplicitlyRemoveWithoutHealing()throws Exception{
        var dm=dm();for(boolean dagger:List.of(false,true)){
            var c=campaign(dm);var original=knocked(dm,c,dagger,dagger?1:20);var target=dm.read(base(c)+"/characters/"+target(original));var helper=dm.read(base(c)+"/characters/"+helper(original));var actions=dm.read(base(c)+"/actions");var body=aid(dm,c,original);String path=root(c,original);var ack=tree(dm.post(path+"/knockout-first-aid-rolls",body),200);var pr=dm.read(path);assertEquals(3,pr.get("version").asLong());assertEquals(target,dm.read(base(c)+"/characters/"+target(original)));
            var event=pr.get("events").get(2);assertEquals("ROLL_KNOCKOUT_FIRST_AID",event.get("kind").asString());var roll=dm.read(base(c)+"/rolls/"+event.get("id").asString());assertEquals("skill:medicine",roll.get("key").asString());assertEquals("DM",roll.get("visibility").asString());assertEquals(20,roll.get("result").get("total").asInt());assertTrue(event.get("summary").asString().contains("DC10"));assertEquals(2,event.get("changes").size());
            var snapshot=snapshot(c);assertEquals(ack,tree(dm.post(path+"/knockout-first-aid-rolls",body),200));var review=aidReview(dm,c,pr);var response=dm.post(path+"/knockout-first-aid-effect",review);var plan=tree(response,200);assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals("CANDIDATE",plan.get("status").asString());assertEquals(10,plan.get("dc").asInt());assertEquals(snapshot,snapshot(c));
            var command=complete(review,plan);var applied=tree(dm.post(path+"/knockout-first-aid-completions",command),200);var after=dm.read(base(c)+"/characters/"+target(original));assertEquals(3,after.get("version").asLong());var expected=(tools.jackson.databind.node.ObjectNode)target.get("sheet").deepCopy();expected.set("conditions",json.valueToTree(List.of()));assertEquals(expected,after.get("sheet"));assertEquals(helper,dm.read(base(c)+"/characters/"+helper(original)));assertEquals(actions,dm.read(base(c)+"/actions"));var end=dm.read(path);assertEquals(original.get("evidence"),end.get("evidence"));assertEquals(original.get("hash"),end.get("hash"));assertEquals("END_KNOCKOUT_FIRST_AID",end.get("events").get(3).get("kind").asString());assertTrue(end.get("events").get(3).get("summary").asString().contains(roll.get("id").asString()));assertEquals(1,jdbc.queryForObject("select count(*) from character_history where character_id=? and kind='KNOCKOUT_FIRST_AID'",Integer.class,UUID.fromString(target(original))));snapshot=snapshot(c);assertEquals(applied,tree(dm.post(path+"/knockout-first-aid-completions",command),200));assertEquals(snapshot,snapshot(c));
        }
    }
    @Test void failedCheckIsDurableReadsNeverRerollAndOnlyLatestExplicitAttemptQualifies()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);entropy.d20=9;var body=aid(dm,c,pr);var ack=tree(dm.post(root(c,pr)+"/knockout-first-aid-rolls",body),200);pr=dm.read(root(c,pr));var review=aidReview(dm,c,pr);String oldRoll=review.get("rollId").toString();var before=snapshot(c);var plan=plan(dm,c,pr,review);assertEquals("MANUAL_REQUIRED",plan.get("status").asString());assertEquals(9,plan.get("total").asInt());assertTrue(plan.get("conditionId").isNull());tree(dm.post(root(c,pr)+"/knockout-first-aid-completions",complete(review,plan)),409);assertEquals(plan,plan(dm,c,pr,review));assertEquals(ack,tree(dm.post(root(c,pr)+"/knockout-first-aid-rolls",body),200));assertEquals(before,snapshot(c));
        entropy.d20=10;pr=attempt(dm,c,pr);review=aidReview(dm,c,pr);var old=new HashMap<>(review);old.put("rollId",oldRoll);assertEquals("MANUAL_REQUIRED",plan(dm,c,pr,old).get("status").asString());assertEquals("CANDIDATE",plan(dm,c,pr,review).get("status").asString());assertEquals(4,pr.get("events").size());
    }
    @Test void skillTotalsRatherThanNaturalAttackRulesDetermineSuccess()throws Exception{
        var dm=dm();for(boolean naturalOne:List.of(true,false)){entropy.d20=20;var c=campaign(dm);var pr=knocked(dm,c,true,20);bonus(dm,c,pr,naturalOne?9:-11);entropy.d20=naturalOne?1:20;pr=attempt(dm,c,pr);var review=aidReview(dm,c,pr);var plan=plan(dm,c,pr,review);assertEquals(naturalOne?10:9,plan.get("total").asInt());assertEquals(naturalOne?"CANDIDATE":"MANUAL_REQUIRED",plan.get("status").asString());}
    }
    @Test void csrfPermissionsStrictFieldsAndUnboundDiceCannotBypassDedicatedAttempt()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,false,20);var body=aid(dm,c,pr);var player=player(c);var before=snapshot(c);String path=root(c,pr);
        tree(player.post(path+"/knockout-first-aid-rolls",body),403);tree(dm.raw(path+"/knockout-first-aid-rolls",json.writeValueAsString(body),false),403);
        for(String key:List.of("total","dc","rollId","hp","actor")){var bad=new HashMap<>(body);bad.put(key,20);tree(dm.post(path+"/knockout-first-aid-rolls",bad),400);}var raw=json.writeValueAsString(body);tree(dm.raw(path+"/knockout-first-aid-rolls",raw.substring(0,raw.length()-1)+",\"reviewed\":true}",true),400);var bad=new HashMap<>(body);bad.put("reviewed",false);tree(dm.post(path+"/knockout-first-aid-rolls",bad),409);assertEquals(before,snapshot(c));
        var unrelated=tree(dm.post(base(c)+"/rolls",roll(dm.read(base(c)+"/characters/"+helper(pr)).get("version").asLong(),helper(pr),"skill:medicine","","DM")),200);Map<String,Object> review=new LinkedHashMap<>(body);review.remove("requestId");review.remove("reason");review.put("rollId",unrelated.get("id").asString());assertEquals("MANUAL_REQUIRED",plan(dm,c,pr,review).get("status").asString());pr=attempt(dm,c,pr);review=aidReview(dm,c,pr);var plan=plan(dm,c,pr,review);var command=complete(review,plan);before=snapshot(c);tree(player.post(path+"/knockout-first-aid-effect",review),403);tree(player.post(path+"/knockout-first-aid-completions",command),403);tree(dm.post(path+"/events",command(pr,"ROLL_KNOCKOUT_FIRST_AID",List.of(),"","")),400);tree(dm.post(path+"/events",command(pr,"END_KNOCKOUT_FIRST_AID",List.of(),"","")),400);bad=new HashMap<>(command);bad.put("planHash","0".repeat(64));tree(dm.post(path+"/knockout-first-aid-completions",bad),409);assertEquals(before,snapshot(c));assertEquals(0,player.read(base(c)+"/rolls").get("rolls").size());assertEquals(0,player.read(base(c)+"/attack-results").size());
    }
    @Test void changedOrUnsupportedHelperAndTargetPreserveManualBoundary()throws Exception{
        var dm=dm();for(String mode:List.of("helper","target","self","wis","modifier")){
            var c=campaign(dm);var pr=knocked(dm,c,true,20);var reviewBody=aid(dm,c,pr);if(mode.equals("self")){reviewBody.put("helperId",target(pr));reviewBody.put("helperVersion",2);var before=snapshot(c);tree(dm.post(root(c,pr)+"/knockout-first-aid-rolls",reviewBody),409);assertEquals(before,snapshot(c));continue;}
            if(Set.of("wis","modifier").contains(mode)){var card=dm.read(base(c)+"/characters/"+helper(pr));var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();if(mode.equals("wis")){for(var skill:sheet.get("skills"))if(skill.get("id").asString().equals("medicine"))((tools.jackson.databind.node.ObjectNode)skill).put("ability","INT");}else sheet.set("modifiers",json.valueToTree(List.of(new CharacterSheet.Modifier("skill:medicine",1,CharacterSheet.manual()))));tree(dm.post(base(c)+"/characters/"+helper(pr),Map.of("requestId",UUID.randomUUID(),"expectedVersion",card.get("version").asLong(),"sheet",sheet,"reason","Unsupported helper")),200);var before=snapshot(c);tree(dm.post(root(c,pr)+"/knockout-first-aid-rolls",aid(dm,c,pr)),409);assertEquals(before,snapshot(c));continue;}
            pr=attempt(dm,c,pr);var old=aidReview(dm,c,pr);String id=mode.equals("helper")?helper(pr):target(pr);var card=dm.read(base(c)+"/characters/"+id);tree(dm.post(base(c)+"/characters/"+id,Map.of("requestId",UUID.randomUUID(),"expectedVersion",card.get("version").asLong(),"sheet",card.get("sheet"),"reason","Later same-values edit")),200);tree(dm.post(root(c,pr)+"/knockout-first-aid-effect",old),409);var current=aidReview(dm,c,pr);assertEquals("MANUAL_REQUIRED",plan(dm,c,pr,current).get("status").asString());
        }
    }
    @Test void bothMutationsRollbackAtImmediateDeferredCommitAndSameKeyConcurrency()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);
        for(boolean roll:List.of(true,false)){String path=root(c,pr)+(roll?"/knockout-first-aid-rolls":"/knockout-first-aid-completions");var review=roll?null:aidReview(dm,c,pr);var body=roll?aid(dm,c,pr):complete(review,plan(dm,c,pr,review));
            for(boolean deferred:List.of(false,true)){var before=snapshot(c);String fn="a15_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'atomic aid failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");try{tree(dm.post(path,body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}}
            var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(path,body));var b=pool.submit(()->dm.post(path,body));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}var before=snapshot(c);var bad=new HashMap<>(body);bad.put("reason","Changed");tree(dm.post(path,bad),409);assertEquals(before,snapshot(c));pr=dm.read(root(c,pr));
        }
        assertEquals(3,dm.read(base(c)+"/characters/"+target(pr)).get("version").asLong());
    }
    @Test void oldReceiptsPreserveLaterRestoredRecordAndCurrentRevocationWins()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var original=dm.read(base(c)+"/characters/"+target(pr));var rollBody=aid(dm,c,pr);String path=root(c,pr);var rollAck=tree(dm.post(path+"/knockout-first-aid-rolls",rollBody),200);pr=dm.read(path);var review=aidReview(dm,c,pr);var body=complete(review,plan(dm,c,pr,review));var ack=tree(dm.post(path+"/knockout-first-aid-completions",body),200);tree(dm.post(base(c)+"/characters/"+target(pr),Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"sheet",original.get("sheet"),"reason","Later explicit restoration")),200);var before=snapshot(c);assertEquals(ack,tree(dm.post(path+"/knockout-first-aid-completions",body),200));assertEquals(rollAck,tree(dm.post(path+"/knockout-first-aid-rolls",rollBody),200));assertEquals(before,snapshot(c));jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path+"/knockout-first-aid-completions",body),403);tree(dm.post(path+"/knockout-first-aid-rolls",rollBody),403);assertEquals(before,snapshot(c));
    }
    @Test void genericReceiptCannotPreseedDedicatedCheckAndDifferentKeysHaveOneAttempt()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var body=aid(dm,c,pr);var planted=new LinkedHashMap<>(roll(((Number)body.get("helperVersion")).longValue(),helper(pr),"skill:medicine","","DM"));planted.put("requestId",UUID.nameUUIDFromBytes(("A05:"+body.get("requestId")+":first-aid").getBytes(java.nio.charset.StandardCharsets.UTF_8)));tree(dm.post(base(c)+"/rolls",planted),200);var before=snapshot(c);tree(dm.post(root(c,pr)+"/knockout-first-aid-rolls",body),409);assertEquals(before,snapshot(c));
        var one=aid(dm,c,pr);var two=aid(dm,c,pr);String path=root(c,pr)+"/knockout-first-aid-rolls";var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(path,one));var b=pool.submit(()->dm.post(path,two));assertEquals(Set.of(200,409),Set.of(a.get().statusCode(),b.get().statusCode()));}finally{pool.shutdownNow();}assertEquals(3,dm.read(root(c,pr)).get("events").size());var foreign=campaign(dm);tree(dm.post(previews(foreign)+"/"+pr.get("id").asString()+"/knockout-first-aid-rolls",aid(dm,c,pr)),404);
    }
    @Test void twoDmAidAndRestRaceHaveOnlyOneTargetEffect()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=attempt(dm,c,knocked(dm,c,true,20));var other=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,other.username);var review=aidReview(dm,c,pr);var body=complete(review,plan(dm,c,pr,review));var rest=new LinkedHashMap<>(review);for(String key:List.of("helperId","helperVersion","rollId"))rest.remove(key);rest.put("minutes",60);rest.put("interrupted",false);var restPlan=tree(dm.post(root(c,pr)+"/knockout-rest-effect",rest),200);rest.put("requestId",UUID.randomUUID());rest.put("planHash",restPlan.get("hash").asString());rest.put("reason","Reviewed rest instead");var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(root(c,pr)+"/knockout-first-aid-completions",body));var b=pool.submit(()->other.post(root(c,pr)+"/knockout-rest-completions",rest));assertEquals(Set.of(200,409),Set.of(a.get().statusCode(),b.get().statusCode()));}finally{pool.shutdownNow();}assertEquals(3,dm.read(base(c)+"/characters/"+target(pr)).get("version").asLong());assertEquals(0,dm.read(base(c)+"/characters/"+target(pr)).get("sheet").get("conditions").size());
    }
}
