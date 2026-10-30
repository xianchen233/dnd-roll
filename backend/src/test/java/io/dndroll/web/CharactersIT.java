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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p03_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class CharactersIT {
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
    Browser dm()throws Exception{var b=new Browser();b.login("p03_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Characters test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="p03_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
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
        jdbc.update("delete from account_command_receipt where actor='p03_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }
    @Test void multipleCharactersKeepPrivateIndependentSheetsAndHistory()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var other=player(c);
        var first=create(p,c,0,"First");var second=create(p,c,1,"Second");String path=base(c)+"/"+first.get("id").asString();
        var d=p.read(path);assertTrue(d.get("canEdit").asBoolean());assertEquals(18,d.get("sheet").get("skills").size());assertEquals(6,d.get("sheet").get("saves").size());
        var sheet=(tools.jackson.databind.node.ObjectNode)d.get("sheet").deepCopy();sheet.put("notes","private story");((tools.jackson.databind.node.ObjectNode)sheet.get("combat")).put("maxHp",15).put("hp",7);
        var body=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet,"reason","Character setup");
        tree(p.post(path,body),200);assertEquals(7,p.read(path).get("sheet").get("combat").get("hp").asInt());assertEquals(1,p.read(base(c)+"/"+second.get("id").asString()).get("sheet").get("combat").get("hp").asInt());
        var summary=other.read(base(c));assertEquals(2,summary.get("characters").size());assertFalse(summary.toString().contains("private story"));assertFalse(summary.toString().contains("maxHp"));assertFalse(summary.toString().contains("controllers"));
        tree(other.get(path),403);tree(other.get(path+"/history"),403);assertEquals(2,dm.read(path+"/history").size());assertEquals(p.username,dm.read(path+"/history").get(1).get("actor").asString());
        tree(p.post(base(c),createBody(0,"Stale")),409);
        tree(p.post(path,body),200);assertEquals(2,dm.read(path+"/history").size());
    }
    @Test void serverCalculationsAndDmOverridesCannotBeForgedByPlayers()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var ack=create(p,c,0,"Mage");String path=base(c)+"/"+ack.get("id").asString();var d=p.read(path);
        var s=(tools.jackson.databind.node.ObjectNode)d.get("sheet").deepCopy();((tools.jackson.databind.node.ObjectNode)s.get("abilities")).put("DEX",9).put("INT",18);
        s.set("overrides",json.readTree("[{\"target\":\"initiative\",\"value\":9,\"reason\":\"DM ruling\"}]"));var b=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",s,"reason","ruling");
        tree(p.post(path,b),403);tree(dm.post(path,b),200);var calc=p.read(path).get("calculations").get("initiative");assertEquals(-1,calc.get("baseValue").asInt());assertEquals(9,calc.get("value").asInt());assertEquals("DM ruling",calc.get("overrideReason").asString());
        d=p.read(path);tree(p.post(path,edit(d,"keep DM override")),200);s=(tools.jackson.databind.node.ObjectNode)d.get("sheet").deepCopy();s.set("overrides",json.createArrayNode());tree(p.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"sheet",s,"reason","remove override")),403);
    }
    @Test void accessAndRemovalRecheckAuthorizationBeforeHistoricalReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var q=player(c);var ack=create(p,c,0,"Secret");String path=base(c)+"/"+ack.get("id").asString();
        var original=edit(p.read(path),"original");tree(p.post(path,original),200);tree(dm.post(path+"/access",access(1,List.of(p.username,q.username),false,false)),200);tree(q.get(path),200);
        tree(dm.post(path+"/access",access(2,List.of(q.username),false,true)),200);tree(p.get(path),200);tree(p.get(path+"/history"),403);tree(p.post(path,original),403);
        tree(dm.post(path+"/access",access(3,List.of(q.username),true,false)),200);assertEquals(0,p.read(base(c)).get("characters").size());tree(q.get(path),404);tree(q.get(path+"/export"),404);tree(q.post(path+"/copy",Map.of("requestId",UUID.randomUUID(),"expectedVersion",4,"name","Stolen")),404);
        tree(dm.post(path+"/access",access(4,List.of(q.username),false,false)),200);
        tree(dm.post("/api/campaigns/"+c+"/members/"+q.username+"/remove",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1)),204);tree(q.get(path),403);
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,q.username);tree(q.get(path),403);assertEquals(6,dm.read(path).get("version").asLong());
        var history=dm.read(path+"/history");assertEquals(dm.username,history.get(history.size()-1).get("actor").asString());
        tree(dm.post(path+"/access",access(5,List.of(q.username),false,false)),409);
    }
    @Test void resourceChangesAreBoundedIdempotentAndExpiryRequiresDmConfirmation()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var ack=create(p,c,0,"Resources");String path=base(c)+"/"+ack.get("id").asString();var d=p.read(path);var s=(tools.jackson.databind.node.ObjectNode)d.get("sheet").deepCopy();
        s.set("resources",json.valueToTree(List.of(new CharacterSheet.Resource("slots","Slots","SPELL_SLOT",2,2,"Long rest",CharacterSheet.manual()))));
        s.set("conditions",json.valueToTree(List.of(new CharacterSheet.Condition("poison","Poison","!","manual effect",CharacterSheet.manual(),1,java.time.Instant.parse("2020-01-01T00:00:00Z")))));
        tree(p.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",s,"reason","prepare")),200);
        assertEquals(0,p.read(path).get("expiredConditionIds").size());assertEquals("poison",dm.read(path).get("expiredConditionIds").get(0).asString());
        var consume=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"resourceId","slots","delta",-1,"reason","Cast manually");var used=tree(p.post(path+"/resources",consume),200);assertEquals(used,tree(p.post(path+"/resources",consume),200));assertEquals(1,p.read(path).get("sheet").get("resources").get(0).get("current").asInt());
        tree(p.post(path+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"resourceId","slots","delta",2,"reason","overfill")),400);
        tree(p.post(path+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"resourceId","tempHp","delta",5,"reason","cannot stack")),400);
        d=p.read(path);s=(tools.jackson.databind.node.ObjectNode)d.get("sheet").deepCopy();s.set("conditions",json.createArrayNode());tree(p.post(path,Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"sheet",s,"reason","remove expired")),403);
        var tick=new LinkedHashMap<String,Object>();tick.put("requestId",UUID.randomUUID());tick.put("expectedVersion",2);tick.put("action","TICK");tick.put("conditionId",null);tick.put("reason","DM advances round");tree(p.post(path+"/conditions",tick),403);tree(dm.post(path+"/conditions",tick),200);tree(dm.post(path+"/conditions",tick),200);
        assertEquals(0,p.read(path).get("sheet").get("conditions").get(0).get("remainingRounds").asInt());assertEquals(1,p.read(path).get("sheet").get("conditions").size());tree(dm.post(path+"/conditions",Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"action","REMOVE","conditionId","poison","reason","Confirmed expired")),200);assertEquals(0,p.read(path).get("sheet").get("conditions").size());
    }
    @Test void copyAndStructuredTransferReassignOwnershipAndRequireTargetReview()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var q=player(c);var a=create(p,c,0,"Original");String path=base(c)+"/"+a.get("id").asString();
        var b=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","Copy");var copy=tree(p.post(path+"/copy",b),200);assertEquals(copy,tree(p.post(path+"/copy",b),200));assertNotEquals(a.get("id"),copy.get("id"));
        var exported=p.read(path+"/export");assertEquals("dnd-roll.character",exported.get("format").asString());assertFalse(exported.toString().contains("campaignId"));assertFalse(exported.toString().contains("controllers"));
        var target=campaign(dm);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",target,p.username);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",target,q.username);
        var imported=tree(p.post(base(target)+"/import",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"document",exported)),200);String ip=base(target)+"/"+imported.get("id").asString();assertNotEquals(a.get("id"),imported.get("id"));assertEquals(0,q.read(base(target)).get("characters").size());
        assertTrue(dm.read(ip+"/access").get("importReview").asBoolean());tree(dm.post(ip+"/access",access(0,List.of(p.username),false,false)),200);assertEquals(1,q.read(base(target)).get("characters").size());tree(q.get(ip),403);
        tree(p.post(ip+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"resourceId","hp","delta",-1,"reason","independent")),200);assertEquals(1,p.read(path).get("sheet").get("combat").get("hp").asInt());
        var malformed=(tools.jackson.databind.node.ObjectNode)exported.deepCopy();malformed.put("controllers",p.username);tree(p.post(base(target)+"/import",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"document",malformed)),400);assertEquals(1,p.read(base(target)).get("characters").size());
    }
    @Test void concurrentRetriesHaveOneEffectAndSameVersionHasOneWinner()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var body=createBody(0,"Once");var pool=Executors.newFixedThreadPool(2);
        try{var f=pool.submit(()->p.post(base(c),body));var g=pool.submit(()->p.post(base(c),body));var one=tree(f.get(),200);assertEquals(one,tree(g.get(),200));String path=base(c)+"/"+one.get("id").asString();
            var d=p.read(path);var b1=edit(d,"one");var b2=edit(d,"two");var r1=pool.submit(()->p.post(path,b1));var r2=pool.submit(()->p.post(path,b2));assertEquals(List.of(200,409),java.util.stream.Stream.of(r1.get().statusCode(),r2.get().statusCode()).sorted().toList());assertEquals(2,p.read(path+"/history").size());
            var changed=new LinkedHashMap<>(body);changed.put("sheet",CharacterSheet.empty("Different"));tree(p.post(base(c),changed),409);
        }finally{pool.shutdownNow();}
    }
    @Test void strictNestedJsonCsrfAndForeignTargetsFailWithoutLeaking()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var c2=campaign(dm);var a=create(p,c,0,"Private");String path=base(c)+"/"+a.get("id").asString();tree(dm.get(base(c2)+"/"+a.get("id").asString()),404);
        var b=createBody(1,"Bad");tree(p.raw(base(c),json.writeValueAsString(b),false),403);
        var root=(tools.jackson.databind.node.ObjectNode)json.valueToTree(b);var s=(tools.jackson.databind.node.ObjectNode)root.get("sheet");s.put("actor","forged");tree(p.post(base(c),root),400);s.remove("actor");((tools.jackson.databind.node.ObjectNode)s.get("combat")).remove("hp");tree(p.post(base(c),root),400);
        ((tools.jackson.databind.node.ObjectNode)s.get("combat")).putNull("hp");tree(p.post(base(c),root),400);((tools.jackson.databind.node.ObjectNode)s.get("combat")).put("hp",0.5);tree(p.post(base(c),root),400);
        assertEquals(1,p.read(base(c)).get("characters").size());assertTrue(p.get(path).headers().firstValue("Cache-Control").orElse("").contains("no-store"));
    }
    @Test void copyingUnreviewedImportCannotBypassTargetDmReview()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var q=player(c);var doc=new io.dndroll.application.CharacterService.Document("dnd-roll.character",1,"5R",CharacterSheet.empty("Pending"));
        var imported=tree(p.post(base(c)+"/import",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"document",doc)),200);
        var copy=tree(p.post(base(c)+"/"+imported.get("id").asString()+"/copy",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","Pending copy")),200);
        assertTrue(dm.read(base(c)+"/"+copy.get("id").asString()+"/access").get("importReview").asBoolean());assertEquals(0,q.read(base(c)).get("characters").size());
    }
    @Test void unicodeStorageAndSerializedSizeFailuresAreDeterministicBadRequests()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var body=(tools.jackson.databind.node.ObjectNode)json.valueToTree(createBody(0,"Valid"));var sheet=(tools.jackson.databind.node.ObjectNode)body.get("sheet");sheet.put("notes","not storable \0");tree(p.post(base(c),body),400);
        var source=CharacterSheet.manual();var features=new ArrayList<CharacterSheet.Feature>();for(int n=0;n<150;n++)features.add(new CharacterSheet.Feature("f"+n,"f","FEATURE","","汉".repeat(5000),source));sheet.put("notes","");sheet.set("features",json.valueToTree(features));tree(p.post(base(c),body),400);assertEquals(0,p.read(base(c)).get("characters").size());
    }
    @Test void multipleTokenPlacementsReferenceOneResourceOwnerWithoutChangingOldPayloads()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var ack=create(p,c,0,"One owner");String path=base(c)+"/"+ack.get("id").asString();var scene=dm.read("/api/campaigns/"+c+"/scene");String firstScene=scene.get("id").asString();
        var tokenBody=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","One token","x",1,"y",1,"hidden",false,"controllers",List.of(p.username));var t1=tree(dm.post("/api/campaigns/"+c+"/scenes/"+firstScene+"/tokens",tokenBody),200).get("token");
        var s2=tree(dm.post("/api/campaigns/"+c+"/scenes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"name","Second")),200);String secondScene=s2.get("id").asString();var t2=tree(dm.post("/api/campaigns/"+c+"/scenes/"+secondScene+"/tokens",tokenBody),409);
        var secondBody=new LinkedHashMap<>(tokenBody);secondBody.put("requestId",UUID.randomUUID());t2=tree(dm.post("/api/campaigns/"+c+"/scenes/"+secondScene+"/tokens",secondBody),200).get("token");
        var link=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"tokenId",t1.get("id").asString(),"tokenVersion",0,"reason","place same character");tree(p.post(path+"/placements",link),403);tree(dm.post(path+"/placements",link),200);tree(dm.post(path+"/placements",link),200);
        tree(dm.post(path+"/placements",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"tokenId",t2.get("id").asString(),"tokenVersion",0,"reason","second scene")),200);assertEquals(2,dm.read(path+"/placements").size());
        tree(p.post(path+"/resources",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2,"resourceId","hp","delta",-1,"reason","one resource owner")),200);
        var moved=tree(p.post("/api/campaigns/"+c+"/tokens/"+t1.get("id").asString()+"/moves",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"x",4,"y",5)),200);assertEquals(6,moved.size());assertFalse(moved.has("characterId"));assertEquals(0,p.read(path).get("sheet").get("combat").get("hp").asInt());assertEquals(1,dm.read(base(c)).get("characters").size());
    }
    @Test void immediateAndCommitTimeReceiptFailuresRollBackSheetHistoryVersionsAndReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var ack=create(p,c,0,"Atomic");String path=base(c)+"/"+ack.get("id").asString();
        for(boolean deferred:List.of(false,true)){
            String function="p03_fail_"+UUID.randomUUID().toString().replace("-","");var before=p.read(path);var command=edit(before,"must be atomic");var oldHistory=p.read(path+"/history");
            jdbc.execute("create function "+function+"() returns trigger language plpgsql as $$ begin if NEW.campaign_id='"+c+"'::uuid then raise exception 'injected receipt failure'; end if; return NEW; end $$");
            try{
                jdbc.execute((deferred?"create constraint trigger ":"create trigger ")+function+(deferred?" after":" before")+" insert on character_command_receipt "+(deferred?"deferrable initially deferred ":"")+"for each row execute function "+function+"()");
                tree(p.post(path,command),500);assertEquals(before,p.read(path));assertEquals(oldHistory,p.read(path+"/history"));assertEquals(0,jdbc.queryForObject("select count(*) from character_command_receipt where actor=? and request_id=?",Integer.class,p.username,command.get("requestId")));
            }finally{jdbc.execute("drop trigger if exists "+function+" on character_command_receipt");jdbc.execute("drop function "+function+"()");}
            tree(p.post(path,command),200);assertEquals(before.get("version").asLong()+1,p.read(path).get("version").asLong());
        }
    }
    @Test void realWebsocketCharacterInvalidationContainsNoSheetOrHiddenIdentity()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var frames=new LinkedBlockingQueue<String>();var fragments=new StringBuilder();
        var socket=p.client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:"+port+"/ws"),new WebSocket.Listener(){public void onOpen(WebSocket ws){ws.request(1);}public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last){fragments.append(data);if(last){frames.add(fragments.toString());fragments.setLength(0);}ws.request(1);return null;}}).get(5,TimeUnit.SECONDS);
        try{
            socket.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nX-CSRF-TOKEN:"+p.csrf+"\n\n\0",true).join();assertTrue(frames.poll(5,TimeUnit.SECONDS).startsWith("CONNECTED"));socket.sendText("SUBSCRIBE\nid:characters\ndestination:/user/queue/scene-changes\n\n\0",true).join();
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<deadline&&registry.findSubscriptions(s->s.getSession().getUser().getName().equals(p.username)).isEmpty())Thread.sleep(20);assertFalse(registry.findSubscriptions(s->s.getSession().getUser().getName().equals(p.username)).isEmpty());
            var ack=create(dm,c,0,"SECRET_CHARACTER");String path=base(c)+"/"+ack.get("id").asString();tree(dm.post(path+"/access",access(0,List.of(),true,false)),200);
            for(int n=0;n<2;n++){String f=frames.poll(5,TimeUnit.SECONDS);assertNotNull(f);var event=json.readTree(f.substring(f.indexOf("\n\n")+2).replace("\0",""));assertEquals(2,event.size());assertEquals("SCENE_CHANGED",event.get("type").asString());assertEquals(c.toString(),event.get("campaignId").asString());assertFalse(f.contains("SECRET_CHARACTER"));assertFalse(f.contains(ack.get("id").asString()));}
            assertEquals(0,p.read(base(c)).get("characters").size());tree(p.get(path+"/history"),404);tree(p.get(path+"/export"),404);
        }finally{socket.abort();}
    }
    @Test void oldCreateCopyAndImportReceiptsCannotReturnAnIdentityNowHiddenFromTheActor()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var create=createBody(0,"Originally mine");var first=tree(p.post(base(c),create),200);String firstPath=base(c)+"/"+first.get("id").asString();
        tree(dm.post(firstPath+"/access",access(0,List.of(p.username),true,false)),200);tree(p.post(base(c),create),404);
        var source=create(p,c,1,"Copy source");String sourcePath=base(c)+"/"+source.get("id").asString();var copy=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","My copy");var copied=tree(p.post(sourcePath+"/copy",copy),200);tree(dm.post(base(c)+"/"+copied.get("id").asString()+"/access",access(0,List.of(p.username),true,false)),200);tree(p.post(sourcePath+"/copy",copy),404);
        var document=p.read(sourcePath+"/export");var importing=Map.of("requestId",UUID.randomUUID(),"expectedVersion",3,"document",document);var imported=tree(p.post(base(c)+"/import",importing),200);tree(dm.post(base(c)+"/"+imported.get("id").asString()+"/access",access(0,List.of(p.username),true,false)),200);tree(p.post(base(c)+"/import",importing),404);
    }
}
