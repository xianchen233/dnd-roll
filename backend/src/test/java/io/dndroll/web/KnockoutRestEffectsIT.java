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
@org.springframework.context.annotation.Import(KnockoutRestEffectsIT.EntropyConfig.class)
class KnockoutRestEffectsIT {
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

    Map<String,Object> rest(Browser dm,UUID c,JsonNode p)throws Exception{return new LinkedHashMap<>(Map.of("expectedVersion",p.get("version").asLong(),"previewHash",p.get("hash").asString(),"targetVersion",dm.read(base(c)+"/characters/"+target(p)).get("version").asLong(),"minutes",60,"interrupted",false,"reviewed",true));}
    JsonNode plan(Browser dm,UUID c,JsonNode p,Map<String,Object> review)throws Exception{return tree(dm.post(root(c,p)+"/knockout-rest-effect",review),200);}
    Map<String,Object> complete(Map<String,Object> review,JsonNode plan){var b=new LinkedHashMap<>(review);b.put("requestId",UUID.randomUUID());b.put("planHash",plan.get("hash").asString());b.put("reason","Original continuous Short Rest completed; no interruptions; still prone");return b;}
    @Test void bothOriginsIncludingFullHpEndTheirRestWithoutAnyNumericalRecovery()throws Exception{
        var dm=dm();for(boolean dagger:List.of(false,true)){
            var c=campaign(dm);var pr=knocked(dm,c,dagger,dagger?1:20);var review=rest(dm,c,pr);String path=root(c,pr);var before=dm.read(base(c)+"/characters/"+target(pr));var snapshot=snapshot(c);
            var response=dm.post(path+"/knockout-rest-effect",review);var plan=tree(response,200);assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals("CANDIDATE",plan.get("status").asString());assertEquals(1,plan.get("hp").asInt());assertEquals(snapshot,snapshot(c));
            var rolls=dm.read(base(c)+"/rolls");var actions=dm.read(base(c)+"/actions");var command=complete(review,plan);var ack=tree(dm.post(path+"/knockout-rest-completions",command),200);
            var after=dm.read(base(c)+"/characters/"+target(pr));assertEquals(3,after.get("version").asLong());var expected=(tools.jackson.databind.node.ObjectNode)before.get("sheet").deepCopy();expected.set("conditions",json.valueToTree(List.of()));assertEquals(expected,after.get("sheet"));
            var retained=dm.read(path).get("events").get(2).get("summary").asString();assertTrue(retained.contains("60分钟"),"Rest history must preserve the adjudicated duration");assertTrue(retained.contains("未中断"),"Rest history must preserve the interruption review");
            assertEquals(rolls,dm.read(base(c)+"/rolls"));assertEquals(actions,dm.read(base(c)+"/actions"));var updated=dm.read(path);assertEquals(pr.get("hash"),updated.get("hash"));assertEquals(pr.get("evidence"),updated.get("evidence"));assertEquals("END_KNOCKOUT_REST",updated.get("events").get(2).get("kind").asString());assertEquals(1,jdbc.queryForObject("select count(*) from character_history where character_id=? and kind='KNOCKOUT_REST'",Integer.class,UUID.fromString(target(pr))));
            snapshot=snapshot(c);assertEquals(ack,tree(dm.post(path+"/knockout-rest-completions",command),200));assertEquals(snapshot,snapshot(c));
        }
    }
    @Test void incompleteInterruptedAndUnreviewedRestsNeverWrite()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var before=snapshot(c);
        for(String mode:List.of("short","interrupted","unreviewed")){
            var review=rest(dm,c,pr);review.put(mode.equals("short")?"minutes":mode.equals("interrupted")?"interrupted":"reviewed",mode.equals("short")?59:mode.equals("interrupted"));var plan=plan(dm,c,pr,review);
            assertEquals("MANUAL_REQUIRED",plan.get("status").asString());assertTrue(plan.get("conditionId").isNull());tree(dm.post(root(c,pr)+"/knockout-rest-completions",complete(review,plan)),409);assertEquals(before,snapshot(c));
        }
    }
    @Test void strictCsrfPermissionAndHashBindingsCannotBeBypassed()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,false,20);var review=rest(dm,c,pr);var body=complete(review,plan(dm,c,pr,review));String path=root(c,pr)+"/knockout-rest-completions";var player=player(c);var before=snapshot(c);
        tree(player.post(path,body),403);tree(player.post(root(c,pr)+"/knockout-rest-effect",review),403);tree(dm.raw(path,json.writeValueAsString(body),false),403);
        for(String key:List.of("hp","conditionId","actor","changes","targetId")){var bad=new HashMap<>(body);bad.put(key,1);tree(dm.post(path,bad),400);}
        for(Object minutes:List.of(-1,100001,60.5,"60")){var bad=new HashMap<>(body);bad.put("minutes",minutes);tree(dm.post(path,bad),400);}
        var bad=new HashMap<>(body);bad.remove("interrupted");tree(dm.post(path,bad),400);bad.put("interrupted",null);tree(dm.post(path,bad),400);
        var raw=json.writeValueAsString(body);tree(dm.raw(path,raw.substring(0,raw.length()-1)+",\"minutes\":60}",true),400);bad=new HashMap<>(body);bad.put("planHash","0".repeat(64));tree(dm.post(path,bad),409);
        bad=new HashMap<>(body);bad.put("minutes",61);tree(dm.post(path,bad),409);tree(dm.post(root(c,pr)+"/events",command(pr,"END_KNOCKOUT_REST",List.of(),"","")),400);assertEquals(before,snapshot(c));
    }
    @Test void cardEditsRecreatedRecordsAndGenericResourceWritesKeepManualBoundary()throws Exception{
        var dm=dm();for(String mode:List.of("edit","recreate","resource","condition","zero","temp")){
            var c=campaign(dm);var pr=knocked(dm,c,true,20);var old=rest(dm,c,pr);var card=dm.read(base(c)+"/characters/"+target(pr));var sheet=(tools.jackson.databind.node.ObjectNode)card.get("sheet").deepCopy();String path=base(c)+"/characters/"+target(pr);
            if(mode.equals("resource")){tree(dm.post(path+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"resourceId","hp","delta",1,"reason","Generic healing")),200);assertEquals(sheet.get("conditions"),dm.read(path).get("sheet").get("conditions"));}
            else{
                if(mode.equals("recreate")){var clear=sheet.deepCopy();clear.set("conditions",json.valueToTree(List.of()));tree(dm.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"sheet",clear,"reason","Remove")),200);}
                if(mode.equals("condition"))((tools.jackson.databind.node.ObjectNode)sheet.get("conditions").get(0)).put("name","Other unconsciousness");
                if(mode.equals("zero"))((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("hp",0);
                if(mode.equals("temp"))((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("tempHp",5);
                tree(dm.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",mode.equals("recreate")?3:2,"sheet",sheet,"reason","Later card edit")),200);
            }
            tree(dm.post(root(c,pr)+"/knockout-rest-effect",old),409);var review=rest(dm,c,pr);var before=snapshot(c);var plan=plan(dm,c,pr,review);assertEquals("MANUAL_REQUIRED",plan.get("status").asString(),mode);tree(dm.post(root(c,pr)+"/knockout-rest-completions",complete(review,plan)),409);assertEquals(before,snapshot(c));
        }
    }
    @Test void previewVersionAndObjectIdentityAreBoundWhileLaterSourceActivityIsAllowed()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,false,20);var source=pr.get("evidence").get("source");tree(dm.post(base(c)+"/characters/"+source.get("id").asString()+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",source.get("version").asLong(),"resourceId","hp","delta",-1,"reason","Later source play")),200);
        var review=rest(dm,c,pr);assertEquals("CANDIDATE",plan(dm,c,pr,review).get("status").asString());var body=complete(review,plan(dm,c,pr,review));var updated=send(dm,c,pr,"PUBLISH",List.of(),"","Reviewed public summary");var before=snapshot(c);tree(dm.post(root(c,pr)+"/knockout-rest-completions",body),409);assertEquals(before,snapshot(c));
        var otherCampaign=campaign(dm);tree(dm.post(previews(otherCampaign)+"/"+pr.get("id").asString()+"/knockout-rest-effect",review),404);
        review=rest(dm,c,updated);tree(dm.post(root(c,pr)+"/knockout-rest-completions",complete(review,plan(dm,c,updated,review))),200);
    }
    @Test void oldReceiptsCannotRemoveRestoredRecordsAndRevocationStillWins()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var review=rest(dm,c,pr);var body=complete(review,plan(dm,c,pr,review));String path=root(c,pr)+"/knockout-rest-completions";var original=dm.read(base(c)+"/characters/"+target(pr));var ack=tree(dm.post(path,body),200);
        tree(dm.post(base(c)+"/characters/"+target(pr),Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"sheet",original.get("sheet"),"reason","Explicit later correction")),200);var before=snapshot(c);assertEquals(ack,tree(dm.post(path,body),200));var bad=new HashMap<>(body);bad.put("reason","Changed");tree(dm.post(path,bad),409);assertEquals(before,snapshot(c));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path,body),403);assertEquals(before,snapshot(c));
    }
    @Test void immediateDeferredRollbackAndSameKeyConcurrencyHaveExactlyOneEffect()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,false,20);var review=rest(dm,c,pr);var body=complete(review,plan(dm,c,pr,review));String path=root(c,pr)+"/knockout-rest-completions";
        for(boolean deferred:List.of(false,true)){var before=snapshot(c);String fn="a14_fail_"+UUID.randomUUID().toString().replace("-","");jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'atomic rest failure'; end $$");jdbc.execute("create "+(deferred?"constraint ":"")+"trigger "+fn+" after insert on attack_preview_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+fn+"()");try{tree(dm.post(path,body),500);assertEquals(before,snapshot(c));}finally{jdbc.execute("drop trigger "+fn+" on attack_preview_receipt");jdbc.execute("drop function "+fn+"()");}}
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(path,body));var b=pool.submit(()->dm.post(path,body));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}assertEquals(3,dm.read(base(c)+"/characters/"+target(pr)).get("version").asLong());var fresh=new HashMap<>(body);fresh.put("requestId",UUID.randomUUID());tree(dm.post(path,fresh),409);
    }
    @Test void twoDmRestAndHealingRaceShareTheSameTargetVersion()throws Exception{
        var dm=dm();var c=campaign(dm);var pr=knocked(dm,c,true,20);var other=player(c);jdbc.update("update campaign_member set role='DM' where campaign_id=? and username=?",c,other.username);
        var review=rest(dm,c,pr);var body=complete(review,plan(dm,c,pr,review));var h=new LinkedHashMap<>(review);h.remove("minutes");h.remove("interrupted");h.put("amount",4);var healing=tree(dm.post(root(c,pr)+"/knockout-healing-effect",h),200);h.put("requestId",UUID.randomUUID());h.put("planHash",healing.get("hash").asString());h.put("reason","Reviewed healing instead");
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(root(c,pr)+"/knockout-rest-completions",body));var b=pool.submit(()->other.post(root(c,pr)+"/knockout-healings",h));assertEquals(Set.of(200,409),Set.of(a.get().statusCode(),b.get().statusCode()));}finally{pool.shutdownNow();}
        var card=dm.read(base(c)+"/characters/"+target(pr));assertEquals(3,card.get("version").asLong());assertEquals(0,card.get("sheet").get("conditions").size());assertEquals(3,dm.read(root(c,pr)).get("version").asLong());
    }
}
