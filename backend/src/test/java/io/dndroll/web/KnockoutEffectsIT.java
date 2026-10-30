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
@org.springframework.context.annotation.Import(KnockoutEffectsIT.EntropyConfig.class)
class KnockoutEffectsIT {
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
        var catalog=dm.read(base(c)+"/content");var entries=catalog.get("entries");JsonNode entry=null;for(var e:entries)if(e.get("id").asString().equals("916bf8b1-a24e-5dea-be38-2caee47dad54")){entry=e;break;}
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
    @Test void unsupportedWeaponOrAbilityNeverBecomesMeleeByItsNameOrDamageType()throws Exception{
        var dm=dm();for(String mode:List.of("bow","dex","dagger")){
            var c=campaign(dm);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Target").get("id").asString();
            if(mode.equals("dex")){
                var card=dm.read(base(c)+"/characters/"+a);var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();
                ((tools.jackson.databind.node.ObjectNode)sheet.get("attacks").get(0)).put("ability","DEX");
                tree(dm.post(base(c)+"/characters/"+a,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"sheet",sheet,"reason","Different ability")),200);
            }else{
                String entry=mode.equals("bow")?"1fd208e1-f443-5090-b83c-363a460245aa":"e1086d6d-e523-53d9-93b9-7aa24c718e39";
                tree(dm.post(base(c)+"/characters/"+a+"/content",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"policyVersion",2,"entryId",entry,"ability",mode.equals("bow")?"DEX":"STR","castingId","","reason","Other weapon")),200);
            }
            var body=previewBody(dm,c,a,b);if(!mode.equals("dex"))body.put("attackId","content:"+(mode.equals("bow")?"1fd208e1-f443-5090-b83c-363a460245aa":"e1086d6d-e523-53d9-93b9-7aa24c718e39"));
            var p=preview(dm,c,body);var before=snapshot(c);var plan=tree(dm.post(root(c,p)+"/knockout-effect",review(p)),200);
            assertEquals("MANUAL_REQUIRED",plan.get("status").asString());assertTrue(plan.get("reasons").toString().contains("SUPPORTED_MELEE_CLUB_REQUIRED"));assertTrue(plan.get("hp").isNull());
            tree(dm.post(root(c,p)+"/knockout-confirmations",confirm(p)),409);assertEquals(before,snapshot(c));
        }
    }
    @Test void temporaryHpAndActualHpLossAreRecordedTogetherAndNumericCorrectionDoesNotWakeTarget()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Target").get("id").asString();
        var card=dm.read(base(c)+"/characters/"+b);var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("hp",3).put("maxHp",20).put("tempHp",1);
        tree(dm.post(base(c)+"/characters/"+b,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet,"reason","Fixture")),200);
        var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);var plan=tree(dm.post(root+"/knockout-effect",review(p)),200);
        assertEquals(4,p.get("evidence").get("resolution").get("damage").asInt());assertEquals(1,plan.get("absorbed").asInt());assertEquals(2,plan.get("hpLoss").asInt());
        tree(dm.post(root+"/knockout-confirmations",confirm(p)),200);p=dm.read(root);var applied=send(dm,c,p,"APPLY",List.of(),"","");
        var after=dm.read(base(c)+"/characters/"+b);assertEquals(1,after.get("sheet").get("combat").get("hp").asInt());assertEquals(0,after.get("sheet").get("combat").get("tempHp").asInt());assertEquals(2,after.get("version").asLong());
        send(dm,c,applied,"CORRECT",List.of(change(b,2,5,0)),applied.get("events").get(1).get("id").asString(),"");
        var corrected=dm.read(base(c)+"/characters/"+b);assertEquals(5,corrected.get("sheet").get("combat").get("hp").asInt());assertEquals(after.get("sheet").get("conditions"),corrected.get("sheet").get("conditions"));
    }
    @Test void changedTargetAfterConfirmationBlocksApplyAndStrictBodiesDoNotWrite()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Target").get("id").asString();var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);var body=confirm(p);var before=snapshot(c);
        for(String extra:List.of("hp","actor","condition","changes")){var bad=new HashMap<>(body);bad.put(extra,0);tree(dm.post(root+"/knockout-confirmations",bad),400);}
        var raw=json.writeValueAsString(body);tree(dm.raw(root+"/knockout-confirmations",raw.substring(0,raw.length()-1)+",\"reviewed\":true}",true),400);
        tree(dm.raw(root+"/knockout-confirmations",raw,false),403);assertEquals(before,snapshot(c));
        var ack=tree(dm.post(root+"/knockout-confirmations",body),200);p=dm.read(root);
        tree(dm.post(base(c)+"/characters/"+b+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","hp","delta",-1,"reason","Later play")),200);
        before=snapshot(c);tree(dm.post(root+"/events",command(p,"APPLY",List.of(),"","")),409);assertEquals(ack,tree(dm.post(root+"/knockout-confirmations",body),200));assertEquals(before,snapshot(c));
    }
    @Test void explicitChoiceReadsThenConfirmsThenAppliesKnockoutAtomically()throws Exception{
        var dm=dm();var c=campaign(dm);var player=player(c);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Monster").get("id").asString();var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);var before=snapshot(c);
        var response=dm.post(root+"/knockout-effect",review(p));var plan=tree(response,200);assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals("CANDIDATE",plan.get("status").asString());assertEquals(1,plan.get("hp").asInt());assertEquals(0,plan.get("hpLoss").asInt());assertEquals(before,snapshot(c));
        assertEquals("MANUAL_REQUIRED",dm.read(root+"/damage-effect").get("status").asString());tree(player.post(root+"/knockout-effect",review(p)),403);tree(player.post(root+"/knockout-confirmations",confirm(p)),403);assertEquals(before,snapshot(c));
        var body=confirm(p);var ack=tree(dm.post(root+"/knockout-confirmations",body),200);assertEquals(1,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());p=dm.read(root);assertEquals("CONFIRM_KNOCKOUT",p.get("events").get(0).get("kind").asString());
        var apply=command(p,"APPLY",List.of(),"","");var applied=tree(dm.post(root+"/events",apply),200);var target=dm.read(base(c)+"/characters/"+b);assertEquals(1,target.get("version").asLong());assertEquals(1,target.get("sheet").get("combat").get("hp").asInt());assertEquals("昏迷（击昏记录）",target.get("sheet").get("conditions").get(0).get("name").asString());
        var action=dm.read(base(c)+"/actions/"+p.get("evidence").get("actionId").asString());var last=action.get("events").get(action.get("events").size()-1);assertEquals(target.get("sheet"),last.get("changes").get(0).get("after").get("sheet"));assertEquals(p.get("hash"),dm.read(root).get("hash"));assertEquals(0,player.read(base(c)+"/attack-results").size());
        var sheet=(tools.jackson.databind.node.ObjectNode)target.get("sheet").deepCopy();sheet.set("conditions",json.valueToTree(List.of()));((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("hp",1);tree(dm.post(base(c)+"/characters/"+b,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"sheet",sheet,"reason","Independent correction")),200);
        before=snapshot(c);assertEquals(ack,tree(dm.post(root+"/knockout-confirmations",body),200));assertEquals(applied,tree(dm.post(root+"/events",apply),200));assertEquals(before,snapshot(c));jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(root+"/knockout-confirmations",body),403);tree(dm.post(root+"/events",apply),403);assertEquals(before,snapshot(c));
    }
    @Test void rollbackAtImmediateAndDeferredCommitAndSameKeyConcurrency()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Monster").get("id").asString();var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);
        for(boolean confirmation:List.of(true,false)){
            String path=root+(confirmation?"/knockout-confirmations":"/events");var body=confirmation?confirm(p):command(p,"APPLY",List.of(),"","");
            for(boolean deferred:List.of(true,false)){var before=snapshot(c);String fn="a11_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'atomic death failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");try{tree(dm.post(path,body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}}
            var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(path,body));var two=pool.submit(()->dm.post(path,body));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
            var before=snapshot(c);var bad=new HashMap<>(body);bad.put("reason","Different treatment review");tree(dm.post(path,bad),409);assertEquals(before,snapshot(c));p=dm.read(root);
        }
        var card=dm.read(base(c)+"/characters/"+b);assertEquals(1,card.get("version").asLong());assertEquals(1,card.get("sheet").get("conditions").size());
    }
    @Test void staleSourceTargetActionHashAndPreviewAreRejectedWithoutWrites()throws Exception{
        var dm=dm();for(String mode:List.of("source","target","action","hash","preview","campaign")){var c=campaign(dm);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Monster").get("id").asString();var p=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,p);var read=new HashMap<>(review(p));var body=confirm(p);
            if(mode.equals("source")||mode.equals("target")){String id=mode.equals("source")?a:b;long version=mode.equals("source")?1:0;tree(dm.post(base(c)+"/characters/"+id+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"resourceId","hp","delta",-1,"reason","Later play")),200);}
            if(mode.equals("action")){String action=base(c)+"/actions/"+p.get("evidence").get("actionId").asString();tree(dm.post(action+"/events",event(2,"VERDICT","","MISS",List.of(),"","Independent decision")),200);}
            if(mode.equals("hash")){read.put("previewHash","b".repeat(64));body.put("previewHash","b".repeat(64));}if(mode.equals("preview")){read.put("expectedVersion",1);body.put("expectedVersion",1);}if(mode.equals("campaign"))root=previews(campaign(dm))+"/"+p.get("id").asString();
            var before=snapshot(c);int status=mode.equals("campaign")?404:409;tree(dm.post(root+"/knockout-effect",read),status);tree(dm.post(root+"/knockout-confirmations",body),status);assertEquals(before,snapshot(c));
        }
    }
    @Test void manualCasesAndStrictInputsNeverProducePartialPlansOrWrites()throws Exception{
        var dm=dm();for(String mode:List.of("manual","unreviewed","zero","condition","death","notLethal","same","miss")){var c=campaign(dm);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Monster").get("id").asString();
            var target=dm.read(base(c)+"/characters/"+b);var sheet=(tools.jackson.databind.node.ObjectNode)target.get("sheet").deepCopy();var combat=(tools.jackson.databind.node.ObjectNode)sheet.get("combat");
            if(mode.equals("zero"))combat.put("hp",0);if(mode.equals("notLethal"))combat.put("maxHp",100).put("hp",100);if(mode.equals("death"))combat.put("deathFailures",1);if(mode.equals("condition"))sheet.set("conditions",json.valueToTree(List.of(new CharacterSheet.Condition("expired","Expired","","",CharacterSheet.manual(),0,null))));
            tree(dm.post(base(c)+"/characters/"+b,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet,"reason","Fixture")),200);entropy.d20=mode.equals("miss")?1:20;var p=preview(dm,c,previewBody(dm,c,a,mode.equals("same")?a:b));String root=root(c,p);var body=confirm(p);var read=new HashMap<>(review(p));if(mode.equals("manual")){body.put("treatment","MANUAL");read.put("treatment","MANUAL");}if(mode.equals("unreviewed")){body.put("reviewed",false);read.put("reviewed",false);}
            var before=snapshot(c);var plan=tree(dm.post(root+"/knockout-effect",read),200);assertEquals("MANUAL_REQUIRED",plan.get("status").asString());for(String key:List.of("hp","tempHp","absorbed","hpLoss"))assertTrue(plan.get(key).isNull());tree(dm.post(root+"/knockout-confirmations",body),409);assertEquals(before,snapshot(c));
            for(String field:body.keySet()){var bad=new HashMap<>(body);bad.remove(field);tree(dm.post(root+"/knockout-confirmations",bad),400);bad.put(field,null);tree(dm.post(root+"/knockout-confirmations",bad),400);}var forged=new HashMap<>(body);forged.put("hp",0);tree(dm.post(root+"/knockout-confirmations",forged),400);tree(dm.post(root+"/events",command(p,"CONFIRM_KNOCKOUT",List.of(),"","")),400);assertEquals(before,snapshot(c));
        }
    }
    @Test void twoConfirmedPreviewsCannotKnockOutTwiceAndTwoDmsCannotConfirmSameVersion()throws Exception{
        var dm=dm();var c=campaign(dm);var second=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,second.username);String a=catalogCard(dm,c,0,"Source"),b=create(dm,c,1,"Monster").get("id").asString();var one=preview(dm,c,previewBody(dm,c,a,b));var two=preview(dm,c,previewBody(dm,c,a,b));String root=root(c,one);var firstBody=confirm(one);var secondBody=confirm(one);var pool=Executors.newFixedThreadPool(2);
        try{var x=pool.submit(()->dm.post(root+"/knockout-confirmations",firstBody));var y=pool.submit(()->second.post(root+"/knockout-confirmations",secondBody));assertEquals(List.of(200,409),java.util.stream.Stream.of(x.get().statusCode(),y.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}
        tree(dm.post(root(c,two)+"/knockout-confirmations",confirm(two)),200);one=dm.read(root);two=dm.read(root(c,two));String secondRoot=root(c,two);var xbody=command(one,"APPLY",List.of(),"","");var ybody=command(two,"APPLY",List.of(),"","");pool=Executors.newFixedThreadPool(2);
        try{var x=pool.submit(()->dm.post(root+"/events",xbody));var y=pool.submit(()->second.post(secondRoot+"/events",ybody));assertEquals(List.of(200,409),java.util.stream.Stream.of(x.get().statusCode(),y.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}
        assertEquals(1,dm.read(base(c)+"/characters/"+b).get("version").asLong());
    }

}
