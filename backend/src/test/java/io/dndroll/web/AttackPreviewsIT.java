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
@org.springframework.context.annotation.Import(AttackPreviewsIT.EntropyConfig.class)
class AttackPreviewsIT {
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
    @Test void damageEffectIsReadOnlyThenConfirmedAndAppliedExactlyOnce()throws Exception{
        var dm=dm();var c=campaign(dm);var player=player(c);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));
        String root=previews(c)+"/"+p.get("id").asString(),path=root+"/events";var before=snapshot(c);
        var plan=tree(dm.get(root+"/damage-effect"),200);assertEquals("CANDIDATE",plan.get("status").asString());assertEquals(20,plan.get("hp").asInt());assertEquals(1,plan.get("tempHp").asInt());assertEquals(4,plan.get("absorbed").asInt());assertEquals(before,snapshot(c));
        var response=player.get(root+"/damage-effect");tree(response,403);assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElseThrow());tree(new Browser().get(root+"/damage-effect"),401);
        var bad=new HashMap<>(command(p,"CONFIRM_DAMAGE",List.of(change(b,0,1,0)),"",""));tree(dm.post(path,bad),400);assertEquals(before,snapshot(c));
        var confirm=command(p,"CONFIRM_DAMAGE",List.of(),"","");var confirmAck=tree(dm.post(path,confirm),200);p=dm.read(root);var event=p.get("events").get(0);assertEquals("CONFIRM_DAMAGE",event.get("kind").asString());assertEquals(20,event.get("changes").get(0).get("hp").asInt());assertEquals(1,event.get("changes").get(0).get("tempHp").asInt());assertEquals(0,event.get("changes").get(0).get("resources").size());assertEquals(5,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("tempHp").asInt());
        var apply=command(p,"APPLY",List.of(),"","");var applyAck=tree(dm.post(path,apply),200);assertEquals(1,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("tempHp").asInt());var applied=snapshot(c);
        assertEquals(confirmAck,tree(dm.post(path,confirm),200));assertEquals(applyAck,tree(dm.post(path,apply),200));assertEquals(applied,snapshot(c));assertEquals(0,player.read(base(c)+"/attack-results").size());
        tree(dm.get(root+"/damage-effect"),409);jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path,confirm),403);tree(dm.post(path,apply),403);
    }
    @Test void damageEffectCrossesTempBufferAndRejectsLethalOrStalePlansWithoutWrites()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");
        tree(dm.post(base(c)+"/characters/"+b+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"resourceId","tempHp","delta",-3,"reason","Buffer")),200);
        var p=preview(dm,c,previewBody(dm,c,a,b));String root=previews(c)+"/"+p.get("id").asString();var plan=dm.read(root+"/damage-effect");assertEquals(18,plan.get("hp").asInt());assertEquals(0,plan.get("tempHp").asInt());assertEquals(2,plan.get("hpLoss").asInt());
        tree(dm.post(base(c)+"/characters/"+b+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"resourceId","hp","delta",-19,"reason","Later play")),200);
        var before=snapshot(c);tree(dm.get(root+"/damage-effect"),409);tree(dm.post(root+"/events",command(p,"CONFIRM_DAMAGE",List.of(),"","")),409);assertEquals(before,snapshot(c));
        p=preview(dm,c,previewBody(dm,c,a,b));root=previews(c)+"/"+p.get("id").asString();before=snapshot(c);plan=dm.read(root+"/damage-effect");assertEquals("MANUAL_REQUIRED",plan.get("status").asString());for(String field:List.of("hp","tempHp","absorbed","hpLoss"))assertTrue(plan.get(field).isNull());tree(dm.post(root+"/events",command(p,"CONFIRM_DAMAGE",List.of(),"","")),409);assertEquals(before,snapshot(c));
    }
    @Test void automaticDamageConfirmationAndApplyKeepRacesAndCommitFailuresAtomic()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));String root=previews(c)+"/"+p.get("id").asString(),path=root+"/events";
        for(String kind:List.of("CONFIRM_DAMAGE","APPLY")){
            var body=command(p,kind,List.of(),"","");
            for(boolean deferred:List.of(false,true)){
                var before=snapshot(c);String fn="a08_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'automatic damage atomic failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");
                try{tree(dm.post(path,body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}
            }
            var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(path,body));var two=pool.submit(()->dm.post(path,body));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
            var before=snapshot(c);var different=new HashMap<>(body);different.put("reason","Changed review");tree(dm.post(path,different),409);assertEquals(before,snapshot(c));p=dm.read(root);
        }
        assertEquals(1,dm.read(base(c)+"/characters/"+b).get("version").asLong());assertEquals(1,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("tempHp").asInt());
    }
    @Test void automaticEffectsRejectExistingConditionsDeathRecordsAndIndependentActionDecisions()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");
        for(String mode:List.of("condition","death","action","source")){
            var target=dm.read(base(c)+"/characters/"+b);var sheet=(tools.jackson.databind.node.ObjectNode)target.get("sheet").deepCopy();sheet.set("conditions",json.valueToTree(mode.equals("condition")?List.of(new CharacterSheet.Condition("expired","Reviewed","","",CharacterSheet.manual(),0,null)):List.of()));((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("deathFailures",mode.equals("death")?1:0);
            tree(dm.post(base(c)+"/characters/"+b,Map.of("requestId",UUID.randomUUID(),"expectedVersion",target.get("version").asLong(),"sheet",sheet,"reason","Explicit test condition")),200);
            var p=preview(dm,c,previewBody(dm,c,a,b));String root=previews(c)+"/"+p.get("id").asString();
            if(mode.equals("action")){String actionPath=base(c)+"/actions/"+p.get("evidence").get("actionId").asString();tree(dm.post(actionPath+"/events",event(2,"VERDICT","","MISS",List.of(),"","Independent verdict")),200);}
            if(mode.equals("source")){var source=dm.read(base(c)+"/characters/"+a);tree(dm.post(base(c)+"/characters/"+a+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",source.get("version").asLong(),"resourceId","hp","delta",-1,"reason","Source changed")),200);}
            var before=snapshot(c);if(Set.of("action","source").contains(mode))tree(dm.get(root+"/damage-effect"),409);else assertEquals("MANUAL_REQUIRED",dm.read(root+"/damage-effect").get("status").asString());tree(dm.post(root+"/events",command(p,"CONFIRM_DAMAGE",List.of(),"","")),409);assertEquals(before,snapshot(c));
        }
    }
    @Test void durablePreviewConfirmAtomicApplyCorrectionAndSeparateSafePublication()throws Exception{
        var dm=dm();var c=campaign(dm);var player=player(c);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Enemy");var body=previewBody(dm,c,a,b);var p=preview(dm,c,body);String path=previews(c)+"/"+p.get("id").asString()+"/events";
        assertEquals("PREVIEW",p.get("status").asString());assertEquals(20,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());assertEquals(0,player.read(base(c)+"/attack-results").size());
        var original=p.get("evidence");assertEquals("DM",original.get("attackRoll").get("visibility").asString());var createAck=tree(dm.post(previews(c),body),200);assertEquals(0,createAck.get("version").asLong());
        tree(dm.post(path,command(p,"APPLY",List.of(),"","")),409);
        p=send(dm,c,p,"CONFIRM",List.of(change(b,0,16,0)),"","");assertEquals("CONFIRMED",p.get("status").asString());assertEquals(20,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());
        var apply=command(p,"APPLY",List.of(),"","");var ack=tree(dm.post(path,apply),200);p=dm.read(previews(c)+"/"+p.get("id").asString());assertEquals("APPLIED",p.get("status").asString());assertEquals(16,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());
        assertEquals(ack,tree(dm.post(path,apply),200));assertEquals(original,p.get("evidence"));String event=p.get("events").get(1).get("id").asString();
        tree(dm.post(base(c)+"/characters/"+b+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"resourceId","hp","delta",-2,"reason","Later damage")),200);
        tree(dm.post(path,command(p,"CORRECT",List.of(change(b,1,17,0)),event,"")),409);
        p=send(dm,c,p,"CORRECT",List.of(change(b,2,15,0)),event,"");assertEquals(15,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());assertEquals(ack,tree(dm.post(path,apply),200));assertEquals(15,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());
        assertEquals(0,player.read(base(c)+"/attack-results").size());p=send(dm,c,p,"PUBLISH",List.of(),"","A declared result");var visible=player.read(base(c)+"/attack-results");assertEquals(1,visible.size());assertEquals(Set.of("id","name","targetName","summary","actor","at"),visible.get(0).propertyNames());
        for(String secret:List.of("Private","dice","damage","hash","reason","hp","version"))assertFalse(visible.toString().contains(secret),visible.toString());assertEquals(0,player.read(base(c)+"/rolls").get("rolls").size());
        tree(player.get(previews(c)),403);tree(player.get(previews(c)+"/"+p.get("id").asString()),403);assertEquals("no-store, private",player.get(previews(c)).headers().firstValue("Cache-Control").orElse(""));
        jdbc.update("update character_card set hidden=true where id=?",UUID.fromString(b));assertEquals(0,player.read(base(c)+"/attack-results").size());
    }
    @Test void currentDmStrictInputCrossCampaignHashAndVersionBoundaries()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var body=previewBody(dm,c,a,b);
        tree(p.post(previews(c),body),403);tree(new Browser().get(previews(c)),401);tree(dm.raw(previews(c),json.writeValueAsString(body),false),403);
        for(String field:body.keySet()){var bad=new HashMap<>(body);bad.remove(field);tree(dm.post(previews(c),bad),400);}
        for(String field:List.of("actor","dice","armorClass","total","sheet")){var bad=new HashMap<>(body);bad.put(field,20);tree(dm.post(previews(c),bad),400);}
        var foreign=new HashMap<>(body);foreign.put("targetId",attackCard(dm,campaign(dm),0,"Foreign"));tree(dm.post(previews(c),foreign),404);
        var stale=new HashMap<>(body);stale.put("targetVersion",1);tree(dm.post(previews(c),stale),409);var preview=preview(dm,c,body);var cmd=command(preview,"CONFIRM",List.of(change(b,0,18,0)),"","");var bad=new HashMap<>(cmd);bad.put("previewHash","0".repeat(64));tree(dm.post(previews(c)+"/"+preview.get("id").asString()+"/events",bad),409);
        jdbc.update("update character_card set version=version+1 where id=?",UUID.fromString(a));tree(dm.post(previews(c)+"/"+preview.get("id").asString()+"/events",cmd),409);
        assertEquals(0,tree(dm.post(previews(c),body),200).get("version").asLong());jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username='a05_owner'",c);tree(dm.post(previews(c),body),403);tree(dm.get(previews(c)+"/"+preview.get("id").asString()),403);
    }
    @Test void racesAndImmediateOrDeferredFailureKeepEveryRowAtomic()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var body=previewBody(dm,c,a,b);var p=preview(dm,c,body);p=send(dm,c,p,"CONFIRM",List.of(change(a,1,0,0),change(b,0,17,0)),"","");String path=previews(c)+"/"+p.get("id").asString()+"/events";var apply=command(p,"APPLY",List.of(),"","");
        for(boolean deferred:List.of(false,true)){
            var before=snapshot(c);String suffix=UUID.randomUUID().toString().replace("-","");String fn="a05_fail_"+suffix;
            jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'atomic preview failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");
            try{tree(dm.post(path,apply),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}
        }
        var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(path,apply));var two=pool.submit(()->dm.post(path,apply));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
        assertEquals(2,dm.read(base(c)+"/characters/"+a).get("version").asLong());assertEquals(1,dm.read(base(c)+"/characters/"+b).get("version").asLong());
        var changed=new HashMap<>(apply);changed.put("reason","Different");tree(dm.post(path,changed),409);var current=dm.read(previews(c)+"/"+p.get("id").asString());String original=current.get("events").get(1).get("id").asString();
        var second=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,second.username);
        var oneCmd=command(current,"CORRECT",List.of(change(b,1,18,0)),original,"");var twoCmd=command(current,"CORRECT",List.of(change(b,1,19,0)),original,"");var pool2=Executors.newFixedThreadPool(2);try{var one=pool2.submit(()->dm.post(path,oneCmd));var two=pool2.submit(()->second.post(path,twoCmd));assertEquals(List.of(200,409),java.util.stream.Stream.of(one.get().statusCode(),two.get().statusCode()).sorted().toList());}finally{pool2.shutdownNow();}
    }
    String snapshot(UUID c){var out=new LinkedHashMap<String,Object>();for(String t:List.of("attack_preview","attack_preview_receipt","combat_action","combat_command_receipt","dice_roll","dice_command_receipt","character_card","timeline_entry"))out.put(t,jdbc.queryForList("select to_jsonb(t)::text as row from "+t+" t where campaign_id=? order by to_jsonb(t)::text",c));out.put("history",jdbc.queryForList("select to_jsonb(h)::text from character_history h join character_card c on c.id=h.character_id where c.campaign_id=? order by h.character_id,h.version",c));return json.writeValueAsString(out);}
    @Test void manualFallbackHasNoRollsOrActionAndCannotBeConfirmed()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var body=previewBody(dm,c,a,b);body.put("reviewed",false);var p=preview(dm,c,body);assertEquals("MANUAL_REQUIRED",p.get("status").asString());assertTrue(p.get("evidence").get("resolution").isNull());assertEquals(0,dm.read(base(c)+"/rolls").get("rolls").size());assertEquals(0,dm.read(base(c)+"/actions").get("actions").size());tree(dm.post(previews(c)+"/"+p.get("id").asString()+"/events",command(p,"CONFIRM",List.of(change(b,0,19,0)),"","")),409);
    }
    @Test void longReviewedFactorRetainsFullEvidenceWithinExistingDiceLabelBounds()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var body=previewBody(dm,c,a,b);String reason="审阅依据".repeat(100);
        body.put("advantage",List.of(Map.of("reason",reason,"source",new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","Explicit review"))));
        var p=preview(dm,c,body);assertEquals(reason,p.get("evidence").get("review").get("advantage").get(0).get("reason").asString());assertEquals("PREVIEW",p.get("status").asString());assertEquals(2,p.get("evidence").get("attackRoll").get("result").get("dice").size());
    }
    @Test void failedPreviewCreationRollsBackEveryChildAndOldCreateReceiptsCannotReroll()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var body=previewBody(dm,c,a,b);
        for(boolean deferred:List.of(false,true)){
            var before=snapshot(c);String fn="a05_create_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'create atomic failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");
            try{tree(dm.post(previews(c),body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}
        }
        var pool=Executors.newFixedThreadPool(2);try{var one=pool.submit(()->dm.post(previews(c),body));var two=pool.submit(()->dm.post(previews(c),body));assertEquals(tree(one.get(),200),tree(two.get(),200));}finally{pool.shutdownNow();}
        var before=snapshot(c);tree(dm.post(previews(c),body),200);assertEquals(before,snapshot(c));var bad=new HashMap<>(body);bad.put("reviewed",false);tree(dm.post(previews(c),bad),409);assertEquals(before,snapshot(c));
    }
    @Test void realPersistedNaturalMissHasNoDamageRollAndCriticalUsesIndependentDoubleDice()throws Exception{
        var dm=dm();var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");entropy.d20=1;var miss=preview(dm,c,previewBody(dm,c,a,b));var me=miss.get("evidence");assertEquals("MISS",me.get("resolution").get("review").get("candidate").get("verdict").asString());assertTrue(me.get("damageRoll").isNull());assertEquals(0,me.get("resolution").get("dice").size());assertEquals(1,dm.read(base(c)+"/actions/"+me.get("actionId").asString()).get("events").size());
        entropy.d20=20;var critical=preview(dm,c,previewBody(dm,c,a,b));var ce=critical.get("evidence");var candidate=ce.get("resolution").get("review").get("candidate");assertEquals("CRITICAL",candidate.get("verdict").asString());int expected=candidate.get("damageCount").asInt();assertEquals(2,expected);assertEquals(expected,ce.get("damageRoll").get("result").get("dice").size());assertEquals(expected,ce.get("resolution").get("dice").size());assertEquals(2,dm.read(base(c)+"/actions/"+ce.get("actionId").asString()).get("events").size());assertEquals(20,dm.read(base(c)+"/characters/"+b).get("sheet").get("combat").get("hp").asInt());
    }
    @Test void sourceOrTargetChangesAfterConfirmationRequireNewPreviewAndDoNotRebaseOldReceipts()throws Exception{
        var dm=dm();for(boolean sourceChanged:List.of(true,false)){var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));String path=previews(c)+"/"+p.get("id").asString()+"/events";var confirm=command(p,"CONFIRM",List.of(change(b,0,18,0)),"","");var ack=tree(dm.post(path,confirm),200);p=dm.read(previews(c)+"/"+p.get("id").asString());String changed=sourceChanged?a:b;long version=sourceChanged?1:0;
            tree(dm.post(base(c)+"/characters/"+changed+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"resourceId","hp","delta",-1,"reason","Later change")),200);var before=snapshot(c);tree(dm.post(path,command(p,"APPLY",List.of(),"","")),409);assertEquals(ack,tree(dm.post(path,confirm),200));assertEquals(before,snapshot(c));assertEquals("CONFIRMED",dm.read(previews(c)+"/"+p.get("id").asString()).get("status").asString());
        }
    }
    @Test void independentP06DecisionInvalidatesPreviewBeforeConfirmationAndApplication()throws Exception{
        var dm=dm();for(boolean afterConfirm:List.of(false,true)){var c=campaign(dm);String a=catalogCard(dm,c,0,"Hero"),b=attackCard(dm,c,1,"Target");var p=preview(dm,c,previewBody(dm,c,a,b));if(afterConfirm)p=send(dm,c,p,"CONFIRM",List.of(change(b,0,18,0)),"","");
            String actionPath=base(c)+"/actions/"+p.get("evidence").get("actionId").asString();var action=dm.read(actionPath);tree(dm.post(actionPath+"/events",event(action.get("version").asLong(),"VERDICT","","MISS",List.of(),"","Independent DM adjudication")),200);
            var before=snapshot(c);tree(dm.post(previews(c)+"/"+p.get("id").asString()+"/events",command(p,afterConfirm?"APPLY":"CONFIRM",afterConfirm?List.of():List.of(change(b,0,18,0)),"","")),409);assertEquals(before,snapshot(c));
        }
    }
}
