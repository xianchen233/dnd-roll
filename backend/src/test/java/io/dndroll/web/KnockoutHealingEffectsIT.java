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
@org.springframework.context.annotation.Import(KnockoutHealingEffectsIT.EntropyConfig.class)
class KnockoutHealingEffectsIT {
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
    Map<String,Object> healing(Browser dm,UUID c,JsonNode p,int amount)throws Exception{return new LinkedHashMap<>(Map.of("expectedVersion",p.get("version").asLong(),"previewHash",p.get("hash").asString(),"targetVersion",dm.read(base(c)+"/characters/"+target(p)).get("version").asLong(),"amount",amount,"reviewed",true));}
    Map<String,Object> healBody(Map<String,Object> review,JsonNode plan){var out=new LinkedHashMap<>(review);out.put("requestId",UUID.randomUUID());out.put("planHash",plan.get("hash").asString());out.put("reason","Adjudicated healing; resource paid independently; remains prone");return out;}
    JsonNode healPlan(Browser dm,UUID c,JsonNode p,Map<String,Object> body)throws Exception{return tree(dm.post(root(c,p)+"/knockout-healing-effect",body),200);}
    @Test void bothOriginsPreviewWithoutWritingThenHealCappedHpAndRemoveOnlyTheirRecord()throws Exception{
        var dm=dm();for(boolean dagger:List.of(false,true)){
            var c=campaign(dm);var pr=knocked(dm,c,dagger,20);var player=player(c);var body=healing(dm,c,pr,100000);var before=snapshot(c);
            var response=dm.post(root(c,pr)+"/knockout-healing-effect",body);var plan=tree(response,200);
            assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals("CANDIDATE",plan.get("status").asString());assertEquals(20,plan.get("hp").asInt());assertEquals(19,plan.get("healed").asInt());assertEquals(before,snapshot(c));
            var command=healBody(body,plan);var ack=tree(dm.post(root(c,pr)+"/knockout-healings",command),200);assertEquals(3,ack.get("version").asLong());
            var card=dm.read(base(c)+"/characters/"+target(pr));assertEquals(3,card.get("version").asLong());assertEquals(20,card.get("sheet").get("combat").get("hp").asInt());assertEquals(0,card.get("sheet").get("conditions").size());
            var now=dm.read(root(c,pr));assertEquals(pr.get("evidence"),now.get("evidence"));assertEquals(pr.get("hash"),now.get("hash"));assertEquals("HEAL_KNOCKOUT",now.get("events").get(2).get("kind").asString());assertEquals(20,now.get("events").get(2).get("changes").get(0).get("hp").asInt());
            assertEquals(1,jdbc.queryForObject("select count(*) from character_history where character_id=? and kind='KNOCKOUT_HEALING'",Integer.class,UUID.fromString(target(pr))));assertEquals(0,player.read(base(c)+"/attack-results").size());
            before=snapshot(c);assertEquals(ack,tree(dm.post(root(c,pr)+"/knockout-healings",command),200));assertEquals(before,snapshot(c));
        }
    }
    @Test void permissionsCsrfStrictBodiesAndGenericEventCannotBypassReview()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var review=healing(dm,c,pr,4);var body=healBody(review,healPlan(dm,c,pr,review));var player=player(c);var before=snapshot(c);String path=root(c,pr)+"/knockout-healings";
        tree(player.post(path,body),403);tree(player.post(root(c,pr)+"/knockout-healing-effect",review),403);tree(dm.raw(path,json.writeValueAsString(body),false),403);
        for(String key:List.of("hp","actor","conditionId","changes")){var bad=new HashMap<>(body);bad.put(key,1);tree(dm.post(path,bad),400);}
        for(Object amount:List.of(0,-1,100001,1.5,"4")){var bad=new HashMap<>(body);bad.put("amount",amount);tree(dm.post(path,bad),400);}
        var missing=new HashMap<>(body);missing.remove("reviewed");tree(dm.post(path,missing),400);missing.put("reviewed",null);tree(dm.post(path,missing),400);
        var raw=json.writeValueAsString(body);tree(dm.raw(path,raw.substring(0,raw.length()-1)+",\"amount\":4}",true),400);
        tree(dm.post(root(c,pr)+"/events",command(pr,"HEAL_KNOCKOUT",List.of(),"","")),400);
        var wrong=new HashMap<>(body);wrong.put("planHash","0".repeat(64));tree(dm.post(path,wrong),409);assertEquals(before,snapshot(c));
    }
    @Test void manualPlansRejectFullHpUnreviewedChangedOrRecreatedAnnotations()throws Exception{
        var dm=dm();for(String mode:List.of("full","unreviewed","edited","recreated","temp","other","zero")){
            var c=campaign(dm);var pr=knocked(dm,c,true,mode.equals("full")?1:20);var old=healing(dm,c,pr,3);
            if(!Set.of("full","unreviewed").contains(mode)){
                var card=dm.read(base(c)+"/characters/"+target(pr));var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();
                if(mode.equals("temp"))((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("tempHp",5);
                if(mode.equals("zero"))((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("hp",0);
                if(mode.equals("other"))((tools.jackson.databind.node.ObjectNode)sheet.get("conditions").get(0)).put("name","Other unconsciousness");
                if(mode.equals("recreated")){
                    var cleared=sheet.deepCopy();cleared.set("conditions",json.valueToTree(List.of()));
                    tree(dm.post(base(c)+"/characters/"+target(pr),Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"sheet",cleared,"reason","Remove")),200);
                }
                tree(dm.post(base(c)+"/characters/"+target(pr),Map.of("requestId",UUID.randomUUID(),"expectedVersion",mode.equals("recreated")?3:2,"sheet",sheet,"reason","Later edit")),200);
                tree(dm.post(root(c,pr)+"/knockout-healing-effect",old),409);
            }
            var review=healing(dm,c,pr,3);if(mode.equals("unreviewed"))review.put("reviewed",false);var before=snapshot(c);var plan=healPlan(dm,c,pr,review);
            assertEquals("MANUAL_REQUIRED",plan.get("status").asString(),mode);assertTrue(plan.get("hp").isNull());assertTrue(plan.get("conditionId").isNull());tree(dm.post(root(c,pr)+"/knockout-healings",healBody(review,plan)),409);assertEquals(before,snapshot(c));
        }
    }
    @Test void receiptSurvivesLaterPlayButPermissionRevocationAndChangedBodyDoNot()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,false,20);var review=healing(dm,c,pr,4);var body=healBody(review,healPlan(dm,c,pr,review));String path=root(c,pr)+"/knockout-healings";
        var original=dm.read(base(c)+"/characters/"+target(pr)).get("sheet");var ack=tree(dm.post(path,body),200);
        tree(dm.post(base(c)+"/characters/"+target(pr),Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"sheet",original,"reason","Independent correction")),200);
        var before=snapshot(c);assertEquals(ack,tree(dm.post(path,body),200));var different=new HashMap<>(body);different.put("amount",5);tree(dm.post(path,different),409);
        tree(dm.post(root(c,pr)+"/dagger-knockout-confirmations",Map.of("requestId",body.get("requestId"),"expectedVersion",2,"previewHash",pr.get("hash").asString(),"treatment","KNOCK_OUT_MELEE","reviewed",true,"reason",body.get("reason"))),409);
        assertEquals(before,snapshot(c));jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path,body),403);assertEquals(before,snapshot(c));
    }
    @Test void immediateDeferredFailureAndSameKeyConcurrencyAreAtomic()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var review=healing(dm,c,pr,4);var body=healBody(review,healPlan(dm,c,pr,review));String path=root(c,pr)+"/knockout-healings";
        for(boolean deferred:List.of(true,false)){
            var before=snapshot(c);String fn="a13_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'atomic healing failure'; end $$");
            jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");
            try{tree(dm.post(path,body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}
        }
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(path,body));var b=pool.submit(()->dm.post(path,body));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}
        assertEquals(3,dm.read(base(c)+"/characters/"+target(pr)).get("version").asLong());var next=new HashMap<>(body);next.put("requestId",UUID.randomUUID());tree(dm.post(path,next),409);
    }
    @Test void twoDmsRaceOneAppliesAndSourceActivityDoesNotBlockTargetHealing()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,false,20);var other=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,other.username);
        var source=pr.get("evidence").get("source");tree(dm.post(base(c)+"/characters/"+source.get("id").asString()+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",source.get("version").asLong(),"resourceId","hp","delta",-1,"reason","Later source play")),200);
        var review=healing(dm,c,pr,4);var body=healBody(review,healPlan(dm,c,pr,review));var second=new HashMap<>(body);second.put("requestId",UUID.randomUUID());String path=root(c,pr)+"/knockout-healings";
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(path,body));var b=pool.submit(()->other.post(path,second));assertEquals(Set.of(200,409),Set.of(a.get().statusCode(),b.get().statusCode()));}finally{pool.shutdownNow();}
        assertEquals(5,dm.read(base(c)+"/characters/"+target(pr)).get("sheet").get("combat").get("hp").asInt());
    }
}
