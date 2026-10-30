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
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p04_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ContentIT {
    @LocalServerPort int port; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder passwords; @Autowired SimpUserRegistry registry;
    final JsonMapper json=JsonMapper.builder().build(); final Set<UUID> campaigns=new HashSet<>(); final Set<String> users=new HashSet<>();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf,username;
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return raw(path,json.writeValueAsString(body),true);}
        HttpResponse<String> raw(String path,String body,boolean csrfHeader)throws Exception{var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");if(csrfHeader)r.header("X-CSRF-TOKEN",csrf);return client.send(r.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
        Browser()throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        void login(String n)throws Exception{username=n;tree(post("/api/auth/login",Map.of("username",n,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    Browser dm()throws Exception{var b=new Browser();b.login("p04_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Characters test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="p04_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
    String base(UUID c){return "/api/campaigns/"+c+"/characters";}
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return json.readTree(r.body());}
    Map<String,Object> createBody(long v,String name){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",CharacterSheet.empty(name));}
    JsonNode create(Browser b,UUID c,long v,String name)throws Exception{return tree(b.post(base(c),createBody(v,name)),200);}
    Map<String,Object> edit(JsonNode detail,String reason){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",detail.get("version").asLong(),"sheet",detail.get("sheet"),"reason",reason);}
    Map<String,Object> access(long v,List<String> controls,boolean hidden,boolean share){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"controllers",controls,"hidden",hidden,"shareFull",share,"approveImport",true,"reason","DM access ruling");}
    @AfterEach void cleanup(){
        // Only this fixture's campaigns, children cascade under V7; preserve earlier fixtures/data.
        for(UUID c:campaigns){
            if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='character_card' and table_schema=current_schema()",Integer.class)>0){jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);}
            jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from token_command_receipt where campaign_id=?",c);jdbc.update("delete from scene_command_receipt where campaign_id=?",c);jdbc.update("delete from token_controller where campaign_id=?",c);jdbc.update("delete from movement_receipt where campaign_id=?",c);jdbc.update("delete from token where campaign_id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_invitation where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);
        }
        jdbc.update("delete from account_command_receipt where actor='p04_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }
    @Test void searchHasLicensedVersionedContentAndFilters()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);
        var r=p.read("/api/campaigns/"+c+"/content?q=Dagger&rules=5R&type=ATTACK&admitted=true");
        assertEquals(1,r.get("entries").size());var e=r.get("entries").get(0);
        assertEquals("匕首",e.get("name").asString());assertEquals("5R",e.get("source").get("rules").asString());
        assertEquals("CC-BY-4.0",e.get("license").asString());assertTrue(e.get("attribution").asString().contains("Wizards"));
        assertEquals(0,r.get("automated").asInt());assertTrue(r.get("structured").asInt()>0);
        assertEquals("REVIEW",r.get("mode").asString());
        tree(p.get("/api/campaigns/"+c+"/content?rules=2015"),400);
        tree(new Browser().get("/api/campaigns/"+c+"/content"),401);
        tree(player(campaign(dm)).get("/api/campaigns/"+c+"/content"),403);
    }    Map<String,Object> custom(String name,String rules){return Map.of("name",name,"aliases",List.of("echo"),"type","FEATURE","source",new CharacterSheet.Source("本团自制","2026-10",""+rules,"原创"),"description","测试能力；人工裁定","spellLevel",0,"dice","","damageType","","rights","我有权与本团分享此原创内容");}
    String catalog(UUID c){return "/api/campaigns/"+c+"/content";}
    JsonNode publish(Browser b,UUID c,long version,String name,String rules)throws Exception{return tree(b.post(catalog(c)+"/custom",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"entry",custom(name,rules))),200);}
    JsonNode settings(Browser b,UUID c,long version,String mode,List<String> ids)throws Exception{return tree(b.post(catalog(c)+"/settings",Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"mode",mode,"approvedIds",ids)),200);}
    Map<String,Object> preview(long v,String entry){return Map.of("expectedVersion",v,"entryId",entry,"ability","STR","castingId","");}
    Map<String,Object> fill(JsonNode preview){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",preview.get("expectedVersion").asLong(),"policyVersion",preview.get("policyVersion").asLong(),"entryId",preview.get("entryId").asString(),"ability","STR","castingId","","reason","预览后明确加入");}
    @Test void admissionDoesNotGrantCharacterControlOrSilentlyRewriteSnapshots()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var stranger=player(c);var card=create(p,c,0,"Hero");String path=base(c)+"/"+card.get("id").asString();
        var entry=publish(p,c,1,"回声","2014");String id=entry.get("id").asString();
        tree(p.post(path+"/content-preview",preview(0,id)),403);tree(p.post(catalog(c)+"/settings",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"mode","FREE","approvedIds",List.of())),403);
        settings(dm,c,2,"FREE",List.of());tree(stranger.post(path+"/content-preview",preview(0,id)),403);
        var pr=tree(p.post(path+"/content-preview",preview(0,id)),200);assertEquals(0,p.read(path).get("version").asLong());assertEquals(0,p.read(path).get("sheet").get("features").size());
        var body=fill(pr);var ack=tree(p.post(path+"/content",body),200);assertEquals(1,ack.get("version").asLong());assertEquals(ack,tree(p.post(path+"/content",body),200));
        var before=p.read(path).get("sheet");assertEquals("2014",before.get("features").get(0).get("source").get("rules").asString());assertEquals(2,p.read(path+"/history").size());
        settings(dm,c,3,"REVIEW",List.of());tree(p.post(path+"/content",body),403);assertEquals(before,p.read(path).get("sheet"));
        settings(dm,c,4,"REVIEW",List.of(id));var changed=new HashMap<>(body);changed.put("reason","changed");tree(p.post(path+"/content",changed),409);assertEquals(ack,tree(p.post(path+"/content",body),200));
        tree(p.post(path+"/content-preview",preview(0,id)),409);
        tree(dm.post(path+"/access",access(1,List.of(p.username),true,false)),200);tree(p.post(path+"/content",body),404);
    }
    @Test void customRevisionsAreIndependentAndCommandsAreStrictAndAtomic()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var cmd=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"entry",custom("同名","CUSTOM"));
        var first=tree(p.post(catalog(c)+"/custom",cmd),200);assertEquals(first,tree(p.post(catalog(c)+"/custom",cmd),200));
        var changed=new HashMap<String,Object>(cmd);changed.put("entry",custom("changed","CUSTOM"));tree(p.post(catalog(c)+"/custom",changed),409);
        var second=publish(dm,c,1,"同名","5R");assertNotEquals(first.get("id"),second.get("id"));
        var dmCatalog=dm.read(catalog(c));for(var e:dmCatalog.get("entries"))if(e.get("id").asString().equals(first.get("id").asString()))assertFalse(e.get("admitted").asBoolean(),"DM ability to fill must not mislabel campaign admission");
        var results=p.read(catalog(c)+"?q="+URLEncoder.encode("同名",java.nio.charset.StandardCharsets.UTF_8));assertEquals(2,results.get("entries").size());assertEquals(0,results.get("entries").get(0).get("source").get("rules").asString().equals("2014")?1:0);
        tree(p.post(catalog(c)+"/custom",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"entry",custom("stale","CUSTOM"))),409);
        var extra=new HashMap<String,Object>(cmd);extra.put("actor",p.username);tree(p.post(catalog(c)+"/custom",extra),400);
        var malformed=new HashMap<String,Object>(custom("bad","CUSTOM"));malformed.put("spellLevel",1.5);tree(p.post(catalog(c)+"/custom",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"entry",malformed)),400);
        tree(p.raw(catalog(c)+"/custom",json.writeValueAsString(cmd),false),403);
        var foreign=campaign(dm);tree(dm.post(catalog(foreign)+"/settings",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"mode","REVIEW","approvedIds",List.of(first.get("id").asString()))),400);
        assertEquals(2,p.read(catalog(c)).get("campaignVersion").asLong());
    }
    @Test void concurrentFillHasOneEffectAndPolicyConflictDoesNotLeaveHistory()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var ack=create(p,c,0,"Concurrent");String path=base(c)+"/"+ack.get("id").asString();
        String id=p.read(catalog(c)+"?q=Dagger&type=ATTACK").get("entries").get(0).get("id").asString();var pr=tree(p.post(path+"/content-preview",preview(0,id)),200);var body=fill(pr);
        settings(dm,c,1,"FREE",List.of());tree(p.post(path+"/content",body),409);assertEquals(1,p.read(path+"/history").size());
        pr=tree(p.post(path+"/content-preview",preview(0,id)),200);var same=fill(pr);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->p.post(path+"/content",same));var b=pool.submit(()->p.post(path+"/content",same));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}
        var d=p.read(path);assertEquals(1,d.get("sheet").get("attacks").size());assertFalse(d.get("sheet").get("attacks").get(0).get("proficient").asBoolean());assertEquals("",d.get("sheet").get("attacks").get(0).get("mastery").asString());assertEquals(2,p.read(path+"/history").size());
    }
    @Test void receiptCommitFailureRollsBackEntryPolicyAndVersion()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String suffix=c.toString().replace("-","");String fn="p04_fail_"+suffix;String trigger="p04_trigger_"+suffix;
        var command=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"entry",custom("Rollback fixture","CUSTOM"));
        jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if NEW.campaign_id='"+c+"'::uuid then raise exception 'injected commit failure'; end if; return NEW; end $$");
        jdbc.execute("create constraint trigger "+trigger+" after insert on content_command_receipt deferrable initially deferred for each row execute function "+fn+"()");
        try{tree(p.post(catalog(c)+"/custom",command),500);assertEquals(0,p.read(catalog(c)).get("campaignVersion").asLong());assertEquals(0,jdbc.queryForObject("select count(*) from campaign_content_entry where campaign_id=?",Integer.class,c));}
        finally{jdbc.execute("drop trigger "+trigger+" on content_command_receipt");jdbc.execute("drop function "+fn+"()");}
        var result=tree(p.post(catalog(c)+"/custom",command),200);assertEquals(1,result.get("version").asLong());assertEquals(result,tree(p.post(catalog(c)+"/custom",command),200));
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,p.username);tree(p.post(catalog(c)+"/custom",command),403);tree(p.get(catalog(c)),403);
    }
    @Test void distinctSameVersionFillCommandsHaveOneWinner()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var ack=create(p,c,0,"Race");String path=base(c)+"/"+ack.get("id").asString();String id=p.read(catalog(c)+"?q=Skilled").get("entries").get(0).get("id").asString();var pr=tree(p.post(path+"/content-preview",preview(0,id)),200);var a=fill(pr);var b=fill(pr);var pool=Executors.newFixedThreadPool(2);
        try{var first=pool.submit(()->p.post(path+"/content",a));var second=pool.submit(()->p.post(path+"/content",b));var codes=new ArrayList<>(List.of(first.get().statusCode(),second.get().statusCode()));Collections.sort(codes);assertEquals(List.of(200,409),codes);}finally{pool.shutdownNow();}
        assertEquals(1,p.read(path).get("version").asLong());assertEquals(1,p.read(path).get("sheet").get("features").size());assertEquals(2,p.read(path+"/history").size());
    }

}
