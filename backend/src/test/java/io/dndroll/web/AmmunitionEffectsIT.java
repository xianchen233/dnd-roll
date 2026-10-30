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
@org.springframework.context.annotation.Import(AmmunitionEffectsIT.EntropyConfig.class)
class AmmunitionEffectsIT {
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
        var catalog=dm.read(base(c)+"/content");var entries=catalog.get("entries");JsonNode entry=null;for(var e:entries)if(e.get("type").asString().equals("ATTACK")){entry=e;break;}
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

    String archer(Browser dm,UUID c,int arrows)throws Exception{
        var sheet=(tools.jackson.databind.node.ObjectNode)json.valueToTree(CharacterSheet.empty("Archer"));((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("maxHp",20).put("hp",20);
        sheet.set("resources",json.valueToTree(List.of(new CharacterSheet.Resource("arrows","Explicit arrow counter","CUSTOM",arrows,100,"Manual",CharacterSheet.manual()),new CharacterSheet.Resource("other","Keep","FEATURE",7,7,"Manual",CharacterSheet.manual()))));
        String id=tree(dm.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet)),200).get("id").asString();
        tree(dm.post(base(c)+"/characters/"+id+"/content",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"policyVersion",1,"entryId","1fd208e1-f443-5090-b83c-363a460245aa","ability","DEX","castingId","","reason","Trusted shortbow")),200);return id;
    }
    String root(UUID c,JsonNode p){return previews(c)+"/"+p.get("id").asString();}
    Map<String,Object> review(JsonNode p,String resource,boolean reviewed){return Map.of("expectedVersion",p.get("version").asLong(),"previewHash",p.get("hash").asString(),"resourceId",resource,"reviewed",reviewed);}
    Map<String,Object> confirm(JsonNode p,String resource){var body=new LinkedHashMap<>(review(p,resource,true));body.put("requestId",UUID.randomUUID());body.put("reason","Reviewed ordinary arrows and all linked effects");return body;}
    String snapshot(UUID c){var out=new LinkedHashMap<String,Object>();for(String t:List.of("attack_preview","attack_preview_receipt","combat_action","combat_command_receipt","dice_roll","dice_command_receipt","character_card","timeline_entry"))out.put(t,jdbc.queryForList("select to_jsonb(t)::text as row from "+t+" t where campaign_id=? order by to_jsonb(t)::text",c));out.put("history",jdbc.queryForList("select to_jsonb(h)::text from character_history h join character_card c on c.id=h.character_id where c.campaign_id=? order by h.character_id,h.version",c));return json.writeValueAsString(out);}
    int arrows(Browser dm,UUID c,String a)throws Exception{return dm.read(base(c)+"/characters/"+a).get("sheet").get("resources").get(0).get("current").asInt();}
    @Test void readOnlyMappedPlanConfirmsThenAppliesBothCardsAndHistoricalRetriesNeverSpendTwice()throws Exception{
        var dm=dm();var c=campaign(dm);var player=player(c);String a=archer(dm,c,3),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);var before=snapshot(c);
        var response=dm.post(root+"/ammunition-effect",review(p,"arrows",true));var plan=tree(response,200);assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals("CANDIDATE",plan.get("status").asString());assertEquals(3,plan.get("before").asInt());assertEquals(2,plan.get("after").asInt());assertEquals(20,plan.get("hp").asInt());assertEquals(1,plan.get("tempHp").asInt());assertEquals(before,snapshot(c));
        tree(player.post(root+"/ammunition-effect",review(p,"arrows",true)),403);tree(player.post(root+"/ammunition-confirmations",confirm(p,"arrows")),403);tree(new Browser().get(root+"/damage-effect"),401);tree(dm.raw(root+"/ammunition-effect",json.writeValueAsString(review(p,"arrows",true)),false),403);
        var forged=new HashMap<>(confirm(p,"arrows"));forged.put("changes",List.of(change(a,1,0,0)));tree(dm.post(root+"/ammunition-confirmations",forged),400);tree(dm.post(root+"/events",command(p,"CONFIRM_AMMUNITION",List.of(),"","")),400);assertEquals(before,snapshot(c));
        var confirm=confirm(p,"arrows");var ack=tree(dm.post(root+"/ammunition-confirmations",confirm),200);p=dm.read(root);assertEquals("CONFIRMED",p.get("status").asString());var event=p.get("events").get(0);assertEquals("CONFIRM_AMMUNITION",event.get("kind").asString());assertEquals(2,event.get("changes").size());assertEquals(before.contains("CONFIRMED"),false);assertEquals(3,arrows(dm,c,a));
        var apply=command(p,"APPLY",List.of(),"","");var applied=tree(dm.post(root+"/events",apply),200);assertEquals(2,arrows(dm,c,a));var source=dm.read(base(c)+"/characters/"+a);assertEquals(20,source.get("sheet").get("combat").get("hp").asInt());assertEquals(7,source.get("sheet").get("resources").get(1).get("current").asInt());assertEquals(1,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("tempHp").asInt());assertEquals(0,player.read(base(c)+"/attack-results").size());assertEquals(0,player.read(base(c)+"/rolls").get("rolls").size());
        tree(dm.post(base(c)+"/characters/"+a+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"resourceId","arrows","delta",-1,"reason","Later shot")),200);before=snapshot(c);assertEquals(ack,tree(dm.post(root+"/ammunition-confirmations",confirm),200));assertEquals(applied,tree(dm.post(root+"/events",apply),200));assertEquals(before,snapshot(c));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(root+"/ammunition-confirmations",confirm),403);tree(dm.post(root+"/events",apply),403);assertEquals(before,snapshot(c));
    }
    @Test void insufficientUnreviewedMissingOrWrongCountersReturnNoPartialPlanOrWrites()throws Exception{
        var dm=dm();var c=campaign(dm);String a=archer(dm,c,0),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));var before=snapshot(c);
        for(String resource:List.of("arrows","other","missing")){var plan=tree(dm.post(root(c,p)+"/ammunition-effect",review(p,resource,true)),200);assertEquals("MANUAL_REQUIRED",plan.get("status").asString());for(String key:List.of("before","after","hp","tempHp"))assertTrue(plan.get(key).isNull());tree(dm.post(root(c,p)+"/ammunition-confirmations",confirm(p,resource)),409);assertEquals(before,snapshot(c));}
        var plan=tree(dm.post(root(c,p)+"/ammunition-effect",review(p,"arrows",false)),200);assertEquals("MANUAL_REQUIRED",plan.get("status").asString());var unreviewed=confirm(p,"arrows");unreviewed.put("reviewed",false);tree(dm.post(root(c,p)+"/ammunition-confirmations",unreviewed),409);assertEquals(before,snapshot(c));
    }
    @Test void missAndDamageImmunityStillConsumeExactlyOneArrow()throws Exception{
        var dm=dm();for(boolean miss:List.of(true,false)){var c=campaign(dm);String a=archer(dm,c,1),b=attackCard(dm,c,1,"Target");entropy.d20=miss?1:20;var body=previewBody(dm,c,a,b);if(!miss)body.put("immunities",List.of(Map.of("reason","Reviewed piercing immunity","source",new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","Immunity"))));var p=preview(dm,c,body);String root=root(c,p);assertEquals(0,p.get("evidence").get("resolution").get("damage").asInt());var plan=tree(dm.post(root+"/ammunition-effect",review(p,"arrows",true)),200);assertEquals(0,plan.get("after").asInt());assertEquals(5,plan.get("tempHp").asInt());tree(dm.post(root+"/ammunition-confirmations",confirm(p,"arrows")),200);p=dm.read(root);send(dm,c,p,"APPLY",List.of(),"","");assertEquals(0,arrows(dm,c,a));assertEquals(20,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());}
    }
    @Test void confirmationAndApplicationRollBackBothCardsOnImmediateOrDeferredFailuresAndRaceOnce()throws Exception{
        var dm=dm();var c=campaign(dm);String a=archer(dm,c,3),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);
        for(boolean confirmation:List.of(true,false)){
            String path=root+(confirmation?"/ammunition-confirmations":"/events");var body=confirmation?confirm(p,"arrows"):command(p,"APPLY",List.of(),"","");
            for(boolean deferred:List.of(false,true)){var before=snapshot(c);String fn="a09_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'atomic ammunition failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");try{tree(dm.post(path,body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}}
            var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(path,body));var two=pool.submit(()->dm.post(path,body));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
            var before=snapshot(c);var bad=new HashMap<>(body);bad.put("reason","Different review");tree(dm.post(path,bad),409);assertEquals(before,snapshot(c));p=dm.read(root);
        }
        assertEquals(2,arrows(dm,c,a));assertEquals(2,dm.read(base(c)+"/characters/"+a).get("version").asLong());assertEquals(1,dm.read(base(c)+"/characters/"+b).get("version").asLong());
        p=preview(dm,c,previewBody(dm,c,a,b));String competing=root(c,p)+"/ammunition-confirmations";var second=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,second.username);var oneBody=confirm(p,"arrows");var twoBody=confirm(p,"arrows");var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(competing,oneBody));var two=pool.submit(()->second.post(competing,twoBody));assertEquals(List.of(200,409),java.util.stream.Stream.of(one.get().statusCode(),two.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}assertEquals(2,arrows(dm,c,a));
    }
    @Test void staleSourceTargetActionPreviewOrHashAndCrossCampaignCannotConfirmOrRebase()throws Exception{
        var dm=dm();for(String mode:List.of("source","target","action","hash","preview","campaign")){var c=campaign(dm);String a=archer(dm,c,3),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);var review=new HashMap<>(review(p,"arrows",true));var body=confirm(p,"arrows");
            if(mode.equals("source")||mode.equals("target")){String id=mode.equals("source")?a:b;long version=mode.equals("source")?1:0;tree(dm.post(base(c)+"/characters/"+id+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"resourceId","hp","delta",-1,"reason","Later play")),200);}
            if(mode.equals("action")){String action=base(c)+"/actions/"+p.get("evidence").get("actionId").asString();tree(dm.post(action+"/events",event(2,"VERDICT","","MISS",List.of(),"","Independent decision")),200);}
            if(mode.equals("hash")){review.put("previewHash","b".repeat(64));body.put("previewHash","b".repeat(64));}
            if(mode.equals("preview")){review.put("expectedVersion",1);body.put("expectedVersion",1);}
            if(mode.equals("campaign"))root=previews(campaign(dm))+"/"+p.get("id").asString();
            var before=snapshot(c);int status=mode.equals("campaign")?404:409;tree(dm.post(root+"/ammunition-effect",review),status);tree(dm.post(root+"/ammunition-confirmations",body),status);assertEquals(before,snapshot(c));
        }
    }
    @Test void unsupportedWeaponAbilitySourceEffectsAndSameIdentityNeverProducePartialAmounts()throws Exception{
        var dm=dm();for(String mode:List.of("weapon","ability","condition","zero","death","same")){var c=campaign(dm);String a=archer(dm,c,3),b=attackCard(dm,c,1,"Target");
            if(!mode.equals("same")){var source=dm.read(base(c)+"/characters/"+a);var sheet=(tools.jackson.databind.node.ObjectNode)source.get("sheet").deepCopy();
                if(mode.equals("ability"))((tools.jackson.databind.node.ObjectNode)sheet.get("attacks").get(0)).put("ability","STR");
                if(mode.equals("condition"))sheet.set("conditions",json.valueToTree(List.of(new CharacterSheet.Condition("expired","Expired","","",CharacterSheet.manual(),0,null))));
                if(mode.equals("zero"))((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("hp",0);
                if(mode.equals("death"))((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("deathFailures",1);
                tree(dm.post(base(c)+"/characters/"+a,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"sheet",sheet,"reason","Source fixture")),200);
                if(mode.equals("weapon")){var entries=dm.read(base(c)+"/content").get("entries");String entry="";for(var e:entries)if(e.get("name").asString().equals("匕首"))entry=e.get("id").asString();tree(dm.post(base(c)+"/characters/"+a+"/content",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"policyVersion",2,"entryId",entry,"ability","STR","castingId","","reason","Other trusted weapon")),200);}
            }
            var body=previewBody(dm,c,a,mode.equals("same")?a:b);if(mode.equals("weapon"))body.put("attackId","content:e1086d6d-e523-53d9-93b9-7aa24c718e39");var p=preview(dm,c,body);var before=snapshot(c);var plan=tree(dm.post(root(c,p)+"/ammunition-effect",review(p,"arrows",true)),200);assertEquals("MANUAL_REQUIRED",plan.get("status").asString());for(String field:List.of("before","after","hp","tempHp"))assertTrue(plan.get(field).isNull());tree(dm.post(root(c,p)+"/ammunition-confirmations",confirm(p,"arrows")),409);assertEquals(before,snapshot(c));
        }
    }
    @Test void twoConfirmedPreviewsCompeteForTheLastArrowWithoutPartialTargetDamage()throws Exception{
        var dm=dm();var c=campaign(dm);String a=archer(dm,c,1),b=attackCard(dm,c,1,"Target");var one=preview(dm,c,previewBody(dm,c,a,b));var two=preview(dm,c,previewBody(dm,c,a,b));
        tree(dm.post(root(c,one)+"/ammunition-confirmations",confirm(one,"arrows")),200);tree(dm.post(root(c,two)+"/ammunition-confirmations",confirm(two,"arrows")),200);one=dm.read(root(c,one));two=dm.read(root(c,two));assertEquals(1,arrows(dm,c,a));
        String onePath=root(c,one)+"/events",twoPath=root(c,two)+"/events";var oneBody=command(one,"APPLY",List.of(),"","");var twoBody=command(two,"APPLY",List.of(),"","");var second=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,second.username);
        var pool=Executors.newFixedThreadPool(2);try{var first=pool.submit(()->dm.post(onePath,oneBody));var other=pool.submit(()->second.post(twoPath,twoBody));assertEquals(List.of(200,409),java.util.stream.Stream.of(first.get().statusCode(),other.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}
        assertEquals(0,arrows(dm,c,a));assertEquals(1,dm.read(base(c)+"/characters/"+b).get("version").asLong());assertEquals(1,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("tempHp").asInt());
    }
}
