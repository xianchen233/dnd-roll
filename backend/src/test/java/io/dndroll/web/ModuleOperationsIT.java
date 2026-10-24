package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=a03d_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ModuleOperationsIT {
    @LocalServerPort int port; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder passwords;
    final JsonMapper json=JsonMapper.builder().build();
    static final String schema="a03d_http_"+UUID.randomUUID().toString().replace("-","");
    static final org.springframework.jdbc.datasource.DriverManagerDataSource admin=new org.springframework.jdbc.datasource.DriverManagerDataSource(FirstSliceIT.required("TEST_DATABASE_URL"),FirstSliceIT.required("TEST_DATABASE_USERNAME"),FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){new JdbcTemplate(admin).execute("create schema "+schema);String url=FirstSliceIT.required("TEST_DATABASE_URL");p.add("spring.datasource.url",()->url+(url.contains("?")?"&":"?")+"currentSchema="+schema);p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));p.add("assets.storage-root",()->System.getProperty("java.io.tmpdir")+"/"+schema);}
    @AfterAll static void removeSchema(){new JdbcTemplate(admin).execute("drop schema "+schema+" cascade");}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf,username;
        Browser()throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return raw(path,json.writeValueAsString(body),true);}
        HttpResponse<String> raw(String path,String body,boolean token)throws Exception{var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");if(token)r.header("X-CSRF-TOKEN",csrf);return client.send(r.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
        void login(String name)throws Exception{username=name;tree(post("/api/auth/login",Map.of("username",name,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return json.readTree(r.body());}
    Browser dm()throws Exception{var b=new Browser();b.login("a03d_owner");return b;}
    Browser member(UUID c,String role)throws Exception{String name="a03d_"+UUID.randomUUID().toString().replace("-","");jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),name,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?)",c,name,role);var b=new Browser();b.login(name);return b;}
    String base(UUID c){return "/api/campaigns/"+c;}
    String ops(UUID c){return base(c)+"/module-operations";}
    // A synthetic independent instance: no source-library access and no dependencies.
    UUID campaign(Browser dm)throws Exception{
        UUID c=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Manual operations")),201).get("id").asString());
        var manifest=Map.of("format","dnd-roll.module","formatVersion",2,"moduleId",UUID.randomUUID(),"moduleVersion","1.0.0","name","Synthetic","rules","5R","rights","Synthetic original","dependencies",List.of(),"content",Map.of("size",0,"sha256","a".repeat(64)),"assets",List.of());
        var source=Map.of("campaignId",UUID.randomUUID(),"packageId",UUID.randomUUID(),"sha256","a".repeat(64),"manifest",manifest,"customizationId",UUID.randomUUID(),"customizationVersion",0);
        var document=Map.of("nodes",List.of(),"play",Map.of("scenes",List.of(),"units",List.of(),"placements",List.of(),"admissionMode","REVIEW","approvedDependencyIds",List.of()));
        jdbc.update("insert into module_instance(campaign_id,source,document,mapping,progress) values (?,?::jsonb,?::jsonb,'{}','{}')",c,json.writeValueAsString(source),json.writeValueAsString(document));return c;
    }
    Map<String,Object> save(long version,List<?> steps){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"name","公开线索与守卫","steps",steps);}
    Map<String,Object> reveal(UUID scene,UUID token,long version){return Map.of("kind","REVEAL_TOKEN","sceneId",scene,"tokenId",token,"expectedVersion",version);}
    UUID scene(Browser dm,UUID c)throws Exception{return UUID.fromString(dm.read(base(c)+"/scene").get("id").asString());}
    UUID token(Browser dm,UUID c,UUID s)throws Exception{return UUID.fromString(tree(dm.post(base(c)+"/scenes/"+s+"/tokens",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","SECRET GUARD","x",2,"y",3,"hidden",true,"controllers",List.of(dm.username))),200).get("token").get("id").asString());}
    Map<String,Object> execute(Browser dm,UUID c,UUID id,long version)throws Exception{long cv=dm.read(base(c)+"/scenes").get("campaignVersion").asLong();var review=tree(dm.post(ops(c)+"/"+id+"/impact",Map.of("expectedVersion",version,"campaignVersion",cv)),200);return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"campaignVersion",cv,"impactHash",review.get("impactHash").asString());}
    @Test void editableCombinationUsesOriginalServicesAndExactRetryPreservesLaterPlay()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s),id=UUID.randomUUID();
        UUID note=UUID.fromString(tree(dm.post(base(c)+"/notes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","DM","title","Secret clue","text","Literal <script>inert</script>","attachments",List.of())),200).get("id").asString());
        var steps=List.of(reveal(s,t,0),Map.of("kind","PUBLISH_TEXT","noteId",note,"expectedVersion",0));var body=save(0,steps);
        assertEquals(1,tree(dm.post(ops(c)+"/"+id,body),200).get("version").asLong());assertEquals(1,dm.read(ops(c)).size());
        var player=member(c,"PLAYER");assertTrue(player.read(base(c)+"/scene").get("tokens").isEmpty());assertTrue(player.read(base(c)+"/notes").get("notes").isEmpty());
        var command=execute(dm,c,id,1);var result=tree(dm.post(ops(c)+"/"+id+"/executions",command),200);
        assertEquals(dm.username,result.get("actor").asString());assertEquals(2,result.get("results").size());assertEquals(1,player.read(base(c)+"/scene").get("tokens").size());
        var published=player.read(base(c)+"/notes").get("notes");assertEquals(1,published.size());assertEquals("Literal <script>inert</script>",published.get(0).get("text").asString());assertEquals("DM",dm.read(base(c)+"/notes/"+note).get("visibility").asString());
        tree(dm.post(base(c)+"/scenes/"+s+"/tokens/"+t,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","Later hidden","x",7,"y",8,"hidden",true,"controllers",List.of(dm.username))),200);
        tree(dm.post(ops(c)+"/"+id,save(1,List.of(reveal(s,t,2)))),200);
        assertEquals(result,tree(dm.post(ops(c)+"/"+id+"/executions",command),200));assertTrue(player.read(base(c)+"/scene").get("tokens").isEmpty());assertEquals(1,dm.read(ops(c)+"/"+id+"/executions").size());assertEquals(1,player.read(base(c)+"/notes").get("notes").size());
        var changed=new HashMap<>(command);changed.put("impactHash","b".repeat(64));tree(dm.post(ops(c)+"/"+id+"/executions",changed),409);
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(ops(c)+"/"+id+"/executions",command),403);tree(dm.get(ops(c)+"/"+id+"/executions"),403);
    }
    @Test void permissionsStrictFieldsAndStaleTargetsAreRejectedBeforeAnyWrite()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s),id=UUID.randomUUID();var body=save(0,List.of(reveal(s,t,0)));String path=ops(c)+"/"+id;
        var player=member(c,"PLAYER");var foreign=member(campaign(dm),"DM");
        tree(new Browser().get(ops(c)),401);tree(player.get(ops(c)),403);tree(foreign.get(ops(c)),403);tree(player.post(path,body),403);tree(dm.raw(path,json.writeValueAsString(body),false),403);
        for(String key:body.keySet()){var invalid=new HashMap<>(body);invalid.remove(key);tree(dm.post(path,invalid),400);invalid.put(key,null);tree(dm.post(path,invalid),400);}
        var invalid=new HashMap<>(body);invalid.put("actor","forged");tree(dm.post(path,invalid),400);
        for(var step:List.of(Map.of("kind","SCRIPT","code","anything"),Map.of("kind","REVEAL_TOKEN","sceneId",s,"tokenId",t,"expectedVersion",0.5),Map.of("kind","REVEAL_TOKEN","sceneId",s,"tokenId",t,"expectedVersion",0,"hidden",false))){tree(dm.post(path,save(0,List.of(step))),400);}
        tree(dm.post(path,save(0,List.of(reveal(s,t,0),reveal(s,t,0)))),400);
        var saved=tree(dm.post(path,body),200);assertEquals(saved,tree(dm.post(path,body),200));var edited=new HashMap<>(body);edited.put("name","different");tree(dm.post(path,edited),409);
        var command=execute(dm,c,id,1);for(String suffix:List.of("/impact","/executions"))tree(player.post(path+suffix,suffix.equals("/impact")?Map.of("expectedVersion",1,"campaignVersion",0):command),403);
        tree(dm.post(base(c)+"/scenes/"+s+"/tokens/"+t,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","Still secret","x",4,"y",5,"hidden",true,"controllers",List.of(dm.username))),200);
        tree(dm.post(path+"/executions",command),409);assertTrue(dm.read(path+"/executions").isEmpty());assertTrue(player.read(base(c)+"/scene").get("tokens").isEmpty());
        UUID other=campaign(dm);tree(dm.post(path,save(1,List.of(reveal(scene(dm,other),t,0)))),200);tree(dm.post(path+"/impact",Map.of("expectedVersion",2,"campaignVersion",0)),404);assertTrue(dm.read(path+"/executions").isEmpty());
        assertEquals("no-store, private",player.get(ops(c)).headers().firstValue("Cache-Control").orElse(""));
    }
    Map<String,Object> state(UUID c){var out=new LinkedHashMap<String,Object>();for(String table:List.of("token","scene","campaign_note","note_history","token_command_receipt","note_command_receipt","module_operation_execution","module_operation_receipt","character_card","character_controller","character_command_receipt","character_history","campaign_initiative","initiative_history","initiative_command_receipt","campaign_combat","combat_turn_history","combat_command_receipt")){
        // Every row in these synthetic isolated schemas is stable while a command runs.
        out.put(table,jdbc.queryForList("select to_jsonb(t)::text value from "+table+" t order by to_jsonb(t)::text"));
    }return out;}
    @Test void immediateAndDeferredCommitFailuresRollBackAllOriginalServiceWritesAndReceipts()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s),id=UUID.randomUUID();UUID n=UUID.fromString(tree(dm.post(base(c)+"/notes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","DM","title","Private","text","Publish once","attachments",List.of())),200).get("id").asString());
        tree(dm.post(ops(c)+"/"+id,save(0,List.of(reveal(s,t,0),Map.of("kind","PUBLISH_TEXT","noteId",n,"expectedVersion",0)))),200);var command=execute(dm,c,id,1);failAtReceipt(dm,c,id,command);
        var result=tree(dm.post(ops(c)+"/"+id+"/executions",command),200);assertEquals(result,tree(dm.post(ops(c)+"/"+id+"/executions",command),200));assertEquals(1,dm.read(ops(c)+"/"+id+"/executions").size());
        UUID next=campaign(dm),card=card(dm,next),prepare=UUID.randomUUID();
        tree(dm.post(ops(next)+"/"+prepare,save(0,List.of(Map.of("kind","REVEAL_CHARACTER","characterId",card,"expectedVersion",0),Map.of("kind","PREPARE_ENCOUNTER","expectedVersion",0,"participants",List.of(participant(card,0)),"reason","Atomic preparation")))),200);
        var generation=execute(dm,next,prepare,1);failAtReceipt(dm,next,prepare,generation);tree(dm.post(ops(next)+"/"+prepare+"/executions",generation),200);
        tree(dm.post(base(next)+"/initiative/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",List.of(card),"reason","Explicit confirmation")),200);
        UUID start=UUID.randomUUID();tree(dm.post(ops(next)+"/"+start,save(0,List.of(Map.of("kind","REVEAL_CHARACTER","characterId",card,"expectedVersion",1),Map.of("kind","START_ENCOUNTER","expectedVersion",0,"initiativeVersion",2,"reason","Atomic start")))),200);var begin=execute(dm,next,start,1);failAtReceipt(dm,next,start,begin);tree(dm.post(ops(next)+"/"+start+"/executions",begin),200);
    }
    void failAtReceipt(Browser dm,UUID c,UUID id,Map<String,Object> command)throws Exception{
        var before=state(c);
        for(boolean deferred:List.of(false,true)){
            String suffix=UUID.randomUUID().toString().replace("-","");jdbc.execute("create function a03d_fail_"+suffix+"() returns trigger language plpgsql as $$ begin raise exception 'injected combination failure'; end $$");
            jdbc.execute("create "+(deferred?"constraint ":"")+"trigger a03d_fail_"+suffix+" after insert on module_operation_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function a03d_fail_"+suffix+"()");
            try{tree(dm.post(ops(c)+"/"+id+"/executions",command),500);assertEquals(before,state(c));}finally{jdbc.execute("drop trigger a03d_fail_"+suffix+" on module_operation_receipt");jdbc.execute("drop function a03d_fail_"+suffix+"()");}
        }
    }
    @Test void concurrentRetriesHaveOneEffectAndFreshStaleCompetitorsCannotOverride()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s),id=UUID.randomUUID();String path=ops(c)+"/"+id;tree(dm.post(path,save(0,List.of(reveal(s,t,0)))),200);var command=execute(dm,c,id,1);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->dm.post(path+"/executions",command));var b=pool.submit(()->dm.post(path+"/executions",command));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}
        assertEquals(1,dm.read(path+"/executions").size());assertEquals(1,jdbc.queryForObject("select version from token where id=?",Integer.class,t));
        tree(dm.post(path,save(1,List.of(reveal(s,t,1)))),200);var first=execute(dm,c,id,2);var second=new HashMap<>(first);second.put("requestId",UUID.randomUUID());var dm2=member(c,"DM");pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->dm.post(path+"/executions",first));var b=pool.submit(()->dm2.post(path+"/executions",second));assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get().statusCode(),b.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}
        assertEquals(2,dm.read(path+"/executions").size());assertEquals(2,jdbc.queryForObject("select version from token where id=?",Integer.class,t));
        var other=campaign(dm);UUID otherId=UUID.randomUUID();tree(dm.post(ops(other)+"/"+otherId,save(0,List.of(reveal(s,t,0)))),200);tree(dm.post(ops(other)+"/"+otherId+"/executions",command),409);
    }
    UUID card(Browser dm,UUID c)throws Exception{return UUID.fromString(tree(dm.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",io.dndroll.domain.CharacterSheet.empty("Secret creature"))),200).get("id").asString());}
    Map<String,Object> participant(UUID card,long version){return Map.of("characterId",card,"expectedVersion",version,"kind","MONSTER","group","","surprised",false,"advantage",List.of(),"disadvantage",List.of());}
    @Test void attachedPrivateMaterialIsExplicitlyBlockedWithoutStrippingImagesOrRevealingEarlierTargets()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s),id=UUID.randomUUID();var image=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",image);
        String boundary="a03dMultipart";var out=new java.io.ByteArrayOutputStream();var cmd=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","DM");
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"command\"\r\nContent-Type: application/json\r\n\r\n"+json.writeValueAsString(cmd)+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"synthetic.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(image.toByteArray());out.write(("\r\n--"+boundary+"--\r\n").getBytes());
        var response=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+"/note-attachments")).header("X-CSRF-TOKEN",dm.csrf).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());String attachment=tree(response,200).get("id").asString();
        String note=tree(dm.post(base(c)+"/notes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","DM","title","Private illustrated material","text","Must not silently lose image","attachments",List.of(attachment))),200).get("id").asString();
        tree(dm.post(ops(c)+"/"+id,save(0,List.of(reveal(s,t,0),Map.of("kind","PUBLISH_TEXT","noteId",note,"expectedVersion",0)))),200);var before=state(c);
        assertEquals("MODULE_OPERATION_UNSUPPORTED",tree(dm.post(ops(c)+"/"+id+"/impact",Map.of("expectedVersion",1,"campaignVersion",0)),409).get("code").asString());assertEquals(before,state(c));var p=member(c,"PLAYER");assertTrue(p.read(base(c)+"/scene").get("tokens").isEmpty());assertTrue(p.read(base(c)+"/notes").get("notes").isEmpty());tree(p.get(base(c)+"/note-attachments/"+attachment+"/content"),404);
    }
    @Test void allTargetsAndExactLocalDependenciesMustValidateBeforeTheFirstReveal()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s),id=UUID.randomUUID();String path=ops(c)+"/"+id;
        UUID n=UUID.fromString(tree(dm.post(base(c)+"/notes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","TEAM","title","Already public","text","Public","attachments",List.of())),200).get("id").asString());
        tree(dm.post(path,save(0,List.of(reveal(s,t,0),Map.of("kind","PUBLISH_TEXT","noteId",n,"expectedVersion",0)))),200);var before=state(c);
        tree(dm.post(path+"/impact",Map.of("expectedVersion",1,"campaignVersion",0)),409);tree(dm.post(path+"/executions",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"campaignVersion",0,"impactHash","a".repeat(64))),409);assertEquals(before,state(c));
        tree(dm.post(path,save(1,List.of(reveal(s,t,0)))),200);var command=execute(dm,c,id,2);
        var source=(tools.jackson.databind.node.ObjectNode)dm.read(base(c)+"/module-instance").get("source");((tools.jackson.databind.node.ObjectNode)source.get("manifest")).set("dependencies",json.valueToTree(List.of(Map.of("id",UUID.randomUUID(),"version","1","rules","5R"))));
        jdbc.update("update module_instance set source=?::jsonb where campaign_id=?",source.toString(),c);before=state(c);tree(dm.post(path+"/executions",command),409);assertEquals(before,state(c));
        ((tools.jackson.databind.node.ObjectNode)source.get("manifest")).set("dependencies",json.valueToTree(List.of()));jdbc.update("update module_instance set source=?::jsonb where campaign_id=?",source.toString(),c);
        tree(dm.post(base(c)+"/scenes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","New room")),200);tree(dm.post(path+"/executions",command),409);assertTrue(dm.read(path+"/executions").isEmpty());
    }
    @Test void definitionCapacitySupportsOneHundredAndNewOrRetiredTargetsStayPreparation()throws Exception{
        var dm=dm();UUID c=campaign(dm),s=scene(dm,c),t=token(dm,c,s);String definition=json.writeValueAsString(Map.of("id",UUID.randomUUID(),"version",1,"name","Synthetic capacity","steps",List.of(reveal(s,t,0))));
        for(int n=0;n<99;n++)jdbc.update("insert into module_operation(campaign_id,id,definition) values (?,?,?::jsonb)",c,UUID.randomUUID(),definition);
        UUID id=UUID.randomUUID();tree(dm.post(ops(c)+"/"+id,save(0,List.of(reveal(s,t,0)))),200);assertEquals(100,dm.read(ops(c)).size());tree(dm.post(ops(c)+"/"+UUID.randomUUID(),save(0,List.of(reveal(s,t,0)))),400);tree(dm.post(ops(c)+"/"+id,save(1,List.of(reveal(s,t,0)))),200);
        jdbc.update("update token set module_retired=true where id=?",t);tree(dm.post(ops(c)+"/"+id+"/impact",Map.of("expectedVersion",2,"campaignVersion",0)),404);assertTrue(dm.read(ops(c)+"/"+id+"/executions").isEmpty());
        UUID normal=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Not a module instance")),201).get("id").asString());tree(dm.get(ops(normal)),404);
    }
    @Test void revealAndPrepareComposeWithoutChangingResourcesAndStartRequiresExplicitConfirmedOrder()throws Exception{
        var dm=dm();UUID c=campaign(dm),card=card(dm,c),id=UUID.randomUUID();tree(dm.post(base(c)+"/characters/"+card+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"controllers",List.of(dm.username),"hidden",true,"shareFull",false,"approveImport",false,"reason","Hide")),200);
        var original=dm.read(base(c)+"/characters/"+card).get("sheet");
        tree(dm.post(ops(c)+"/"+id,save(0,List.of(Map.of("kind","REVEAL_CHARACTER","characterId",card,"expectedVersion",1),Map.of("kind","PREPARE_ENCOUNTER","expectedVersion",0,"participants",List.of(participant(card,1)),"reason","Prepare explicitly")))),200);
        var command=execute(dm,c,id,1);assertEquals("EMPTY",dm.read(base(c)+"/initiative").get("status").asString());var result=tree(dm.post(ops(c)+"/"+id+"/executions",command),200);
        var draft=dm.read(base(c)+"/initiative");assertEquals("DRAFT",draft.get("status").asString());assertEquals("IDLE",dm.read(base(c)+"/combat").get("status").asString());assertEquals(original,dm.read(base(c)+"/characters/"+card).get("sheet"));assertEquals(result,tree(dm.post(ops(c)+"/"+id+"/executions",command),200));assertEquals(draft,dm.read(base(c)+"/initiative"));
        UUID start=UUID.randomUUID();tree(dm.post(ops(c)+"/"+start,save(0,List.of(Map.of("kind","START_ENCOUNTER","expectedVersion",0,"initiativeVersion",1,"reason","Start confirmed")))),200);tree(dm.post(ops(c)+"/"+start+"/impact",Map.of("expectedVersion",1,"campaignVersion",1)),400);
        tree(dm.post(base(c)+"/initiative/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",List.of(card),"reason","DM explicitly confirms order")),200);
        tree(dm.post(ops(c)+"/"+start+"/impact",Map.of("expectedVersion",1,"campaignVersion",1)),409);tree(dm.post(ops(c)+"/"+start,save(1,List.of(Map.of("kind","START_ENCOUNTER","expectedVersion",0,"initiativeVersion",2,"reason","Start confirmed")))),200);
        var begin=execute(dm,c,start,2);
        tree(dm.post(base(c)+"/characters/"+card+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"controllers",List.of(dm.username),"hidden",false,"shareFull",true,"approveImport",false,"reason","Changed after review")),200);
        tree(dm.post(ops(c)+"/"+start+"/executions",begin),409);assertEquals("IDLE",dm.read(base(c)+"/combat").get("status").asString());
        begin=execute(dm,c,start,2);var started=tree(dm.post(ops(c)+"/"+start+"/executions",begin),200);assertEquals("ACTIVE",dm.read(base(c)+"/combat").get("status").asString());assertEquals(1,dm.read(base(c)+"/combat").get("round").asInt());assertEquals(started,tree(dm.post(ops(c)+"/"+start+"/executions",begin),200));assertEquals(original,dm.read(base(c)+"/characters/"+card).get("sheet"));
    }
}
