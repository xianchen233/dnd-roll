package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.TokenService;
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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p02c_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
class TokensIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired TokenService service;
    @Autowired PasswordEncoder passwords;
    @Autowired SimpUserRegistry registry;
    final JsonMapper json=JsonMapper.builder().build();
    final Set<UUID> campaigns=new HashSet<>();final Set<String> users=new HashSet<>();
    @DynamicPropertySource static void database(DynamicPropertyRegistry props){
        props.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));
        props.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));
        props.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
    }
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf;String username;
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return raw(path,json.writeValueAsString(body),true);}
        HttpResponse<String> raw(String path,String body,boolean withCsrf)throws Exception{
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");
            if(withCsrf)request.header("X-CSRF-TOKEN",csrf);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
        Browser()throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        void login(String name)throws Exception{username=name;tree(post("/api/auth/login",Map.of("username",name,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    Browser owner()throws Exception{var b=new Browser();b.login("p02c_owner");return b;}
    Browser member(UUID campaign,String role)throws Exception{
        String name="p02c_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(name);
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),name,passwords.encode("integration-owner-password"));
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?)",campaign,name,role);
        var b=new Browser();b.login(name);return b;
    }
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Token authoring test")),201);var id=UUID.fromString(r.get("id").asString());campaigns.add(id);return id;}
    UUID scene(Browser dm,UUID campaign)throws Exception{return UUID.fromString(dm.read(base(campaign)+"/scene").get("id").asString());}
    String base(UUID c){return "/api/campaigns/"+c;}
    String path(UUID c,UUID s){return base(c)+"/scenes/"+s+"/tokens";}
    Map<String,Object> body(UUID request,long version,String name,boolean hidden,List<String> controls){return Map.of("requestId",request,"expectedVersion",version,"name",name,"x",3,"y",4,"hidden",hidden,"controllers",controls);}
    Map<String,Object> body(long version,String name,boolean hidden,List<String> controls){return body(UUID.randomUUID(),version,name,hidden,controls);}
    JsonNode tree(HttpResponse<String>r,int status)throws Exception{assertEquals(status,r.statusCode(),r.body());return json.readTree(r.body());}
    void error(HttpResponse<String>r,int status,String code)throws Exception{assertEquals(code,tree(r,status).get("code").asString());}
    @AfterEach void cleanup(){
        for(UUID id:campaigns){
            jdbc.update("update campaign set current_scene_id=null where id=?",id);
            jdbc.update("delete from token_command_receipt where campaign_id=?",id);
            jdbc.update("delete from scene_command_receipt where campaign_id=?",id);
            jdbc.update("delete from token_controller where campaign_id=?",id);
            jdbc.update("delete from movement_receipt where campaign_id=?",id);
            jdbc.update("delete from token where campaign_id=?",id);
            jdbc.update("delete from scene where campaign_id=?",id);
            jdbc.update("delete from campaign_invitation where campaign_id=?",id);
            jdbc.update("delete from campaign_member where campaign_id=?",id);
            jdbc.update("delete from campaign where id=?",id);
        }
        jdbc.update("delete from account_command_receipt where actor='p02c_owner'");
        for(String name:users){jdbc.update("delete from account_command_receipt where actor=?",name);jdbc.update("delete from app_user where username=?",name);}
    }
    @Test void createUpdateCanonicalControlsMovementAndHiddenViewsKeepExactSharedShape()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var a=member(c,"PLAYER");var b=member(c,"PLAYER");
        var request=UUID.randomUUID();var command=body(request,0,"  😀 Hero  ",false,List.of(b.username,a.username));
        var created=tree(dm.post(path(c,s),command),200);var token=created.get("token");var id=token.get("id").asString();
        assertEquals("😀 Hero",token.get("name").asString());assertEquals(0,token.get("version").asLong());assertEquals(1,created.get("sceneVersion").asLong());
        var sorted=new ArrayList<>(List.of(a.username,b.username));Collections.sort(sorted);assertEquals(sorted,json.convertValue(token.get("controllers"),List.class));
        assertEquals(created,tree(dm.post(path(c,s),body(request,0,"😀 Hero",false,List.of(a.username,b.username))),200));
        assertEquals(1,dm.read(path(c,s)).get("tokens").size());
        for(var player:List.of(a,b)){
            var visible=player.read(base(c)+"/scene").get("tokens").get(0);assertEquals(6,visible.size());assertTrue(visible.get("canMove").asBoolean());assertFalse(visible.has("hidden"));assertFalse(visible.has("controllers"));
        }
        var move=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",7,"y",8);
        var moved=tree(a.post(base(c)+"/tokens/"+id+"/moves",move),200);assertEquals(1,moved.get("version").asLong());
        error(dm.post(path(c,s)+"/"+id,body(0,"Stale",false,List.of(a.username))),409,"VERSION_CONFLICT");
        var hidden=tree(dm.post(path(c,s)+"/"+id,body(1,"Private surprise",true,List.of(a.username,b.username))),200);
        assertEquals(2,hidden.get("token").get("version").asLong());assertEquals(3,hidden.get("sceneVersion").asLong());
        for(var player:List.of(a,b)){
            var snapshot=player.read(base(c)+"/scene");assertEquals(0,snapshot.get("tokens").size());assertFalse(snapshot.toString().contains("Private surprise"));assertFalse(snapshot.toString().contains(id));
            var denied=player.post(base(c)+"/tokens/"+id+"/moves",move);error(denied,404,"NOT_FOUND");assertFalse(denied.body().contains("Private surprise"));assertFalse(denied.body().contains(id));
        }
        tree(dm.post(path(c,s)+"/"+id,body(2,"Revealed",false,List.of())),200);
        for(var player:List.of(a,b)){assertFalse(player.read(base(c)+"/scene").get("tokens").get(0).get("canMove").asBoolean());error(player.post(base(c)+"/tokens/"+id+"/moves",move),403,"FORBIDDEN");}
        assertTrue(dm.read(base(c)+"/scene").get("tokens").get(0).get("canMove").asBoolean());
    }
    @Test void memberRemovalInvalidatesAllControlledTokenAndSceneFormsAndNoReceiptRestoresGrants()throws Exception{
        var dm=owner();var c=campaign(dm);var first=scene(dm,c);var player=member(c,"PLAYER");var retained=member(c,"PLAYER");
        var second=tree(dm.post(base(c)+"/scenes",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"name","Prep")),200);
        var prep=UUID.fromString(second.get("id").asString());
        var originalBody=body(0,"Shared",false,List.of(player.username,retained.username));var firstResult=tree(dm.post(path(c,first),originalBody),200);var id=firstResult.get("token").get("id").asString();
        tree(dm.post(path(c,first),body(1,"Second same scene",false,List.of(player.username))),200);
        tree(dm.post(path(c,prep),body(0,"Inactive shared",false,List.of(player.username))),200);
        var move=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"x",6,"y",7);tree(player.post(base(c)+"/tokens/"+id+"/moves",move),200);
        var removal=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1);
        assertEquals(204,dm.post(base(c)+"/members/"+player.username+"/remove",removal).statusCode());
        error(player.post(base(c)+"/tokens/"+id+"/moves",move),403,"FORBIDDEN");
        assertEquals(4,dm.read(path(c,first)).get("sceneVersion").asLong());assertEquals(2,dm.read(path(c,prep)).get("sceneVersion").asLong());
        assertEquals(2,jdbc.queryForObject("select version from token where id=?",Long.class,UUID.fromString(id)));
        assertEquals(0,jdbc.queryForObject("select count(*) from token_controller where campaign_id=? and username=?",Integer.class,c,player.username));assertEquals(1,jdbc.queryForObject("select count(*) from token_controller where campaign_id=? and username=?",Integer.class,c,retained.username));
        // Rejoin through the real invitation API; old command result remains historical.
        var invite=tree(dm.post(base(c)+"/invitations",Map.of("requestId",UUID.randomUUID(),"expectedVersion",2)),201);
        tree(player.post("/api/invitations/accept",Map.of("token",invite.get("token").asString())),200);
        assertEquals(firstResult,tree(dm.post(path(c,first),originalBody),200));
        error(dm.post(path(c,first)+"/"+id,body(1,"Stale before removal",false,List.of(player.username))),409,"VERSION_CONFLICT");
        error(player.post(base(c)+"/tokens/"+id+"/moves",move),403,"FORBIDDEN");
        var before=dm.read(path(c,first));assertEquals(204,dm.post(base(c)+"/members/"+player.username+"/remove",removal).statusCode());assertEquals(before,dm.read(path(c,first)));
        assertEquals(200,player.get(base(c)+"/scene").statusCode());assertEquals(0,jdbc.queryForObject("select count(*) from token_controller where campaign_id=? and username=?",Integer.class,c,player.username));assertEquals(1,jdbc.queryForObject("select count(*) from token_controller where campaign_id=? and username=?",Integer.class,c,retained.username));
    }
    @Test void everyRouteRequiresCurrentDmCsrfAndCorrectOwnershipBeforeReceipt()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var player=member(c,"PLAYER");var other=campaign(dm);var foreignScene=scene(dm,other);
        var command=body(0,"Secret",true,List.of());var created=tree(dm.post(path(c,s),command),200);var id=created.get("token").get("id").asString();
        for(String p:List.of(path(c,s),path(c,s)+"/"+id)){
            error(player.post(p,command),403,"FORBIDDEN");assertEquals(403,dm.raw(p,json.writeValueAsString(command),false).statusCode());assertEquals(401,new Browser().post(p,command).statusCode());
        }
        error(player.get(path(c,s)),403,"FORBIDDEN");error(player.get(path(c,UUID.randomUUID())),403,"FORBIDDEN");
        assertEquals(401,new Browser().get(path(c,s)).statusCode());
        error(dm.get(path(c,foreignScene)),404,"NOT_FOUND");error(dm.post(path(other,foreignScene)+"/"+id,command),404,"NOT_FOUND");error(dm.post(path(c,s)+"/"+UUID.randomUUID(),command),404,"NOT_FOUND");
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username='p02c_owner'",c);
        error(dm.post(path(c,s),command),403,"FORBIDDEN");error(dm.post(path(c,s)+"/"+id,body(0,"Secret",true,List.of())),403,"FORBIDDEN");error(dm.get(path(c,s)),403,"FORBIDDEN");
        jdbc.update("update campaign_member set role='DM' where campaign_id=? and username='p02c_owner'",c);
        assertEquals(created,tree(dm.post(path(c,s),command),200));
    }
    @Test void strictJsonValidationAndControllerMembershipRevealNoAccountExistence()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var valid=body(0,"Valid",false,List.of());
        for(String field:valid.keySet()){
            var missing=new HashMap<>(valid);missing.remove(field);error(dm.post(path(c,s),missing),400,"INVALID_INPUT");
            var nil=new HashMap<>(valid);nil.put(field,null);error(dm.post(path(c,s),nil),400,"INVALID_INPUT");
        }
        for(var bad:List.of(Map.of("x",0.5),Map.of("x","3"),Map.of("x",24),Map.of("y",16),Map.of("x",-1),Map.of("expectedVersion",-1),Map.of("expectedVersion",0.1),Map.of("expectedVersion","0"),Map.of("hidden","false"),Map.of("controllers","alice"),Map.of("extra",true),Map.of("requestId","not-uuid"),Map.of("controllers",List.of(7)),Map.of("controllers",List.of("p02c_owner","p02c_owner")),Map.of("controllers",List.of("x".repeat(81))),Map.of("controllers",List.of("")),Map.of("controllers",Arrays.asList((String)null)),Map.of("controllers",Collections.nCopies(101,"p02c_owner")),Map.of("name"," "),Map.of("name","😀".repeat(101)))){
            var b=new HashMap<String,Object>(valid);b.putAll(bad);error(dm.post(path(c,s),b),400,"INVALID_INPUT");
        }
        var existing=member(c,"PLAYER");jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,existing.username);
        var unknown=dm.post(path(c,s),body(0,"Hero",false,List.of("no_such_account")));var nonmember=dm.post(path(c,s),body(0,"Hero",false,List.of(existing.username)));
        error(unknown,400,"INVALID_INPUT");error(nonmember,400,"INVALID_INPUT");assertEquals(unknown.body(),nonmember.body());
        // Legacy database usernames may use Unicode: length and sorting are code-point aware/exact.
        String unicode="😀".repeat(80);users.add(unicode);
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),unicode,"legacy-hash");
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,unicode);
        var accepted=tree(dm.post(path(c,s),body(0,"😀".repeat(100),false,List.of(unicode,"p02c_owner"))),200);
        assertEquals(accepted.get("token").get("controllers"),dm.read(path(c,s)).get("tokens").get(0).get("controllers"));
        assertEquals(1,dm.read(path(c,s)).get("tokens").size());
    }
    @Test void durableFingerprintCoversEveryFieldNamespaceAndInactivePreparation()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var command=body(0,"Hero",false,List.of());var result=tree(dm.post(path(c,s),command),200);String id=result.get("token").get("id").asString();
        for(var difference:List.of(Map.of("name","Changed"),Map.of("x",4),Map.of("y",5),Map.of("hidden",true),Map.of("expectedVersion",1),Map.of("controllers",List.of("p02c_owner")))){
            var changed=new HashMap<String,Object>(command);changed.putAll(difference);error(dm.post(path(c,s),changed),409,"IDEMPOTENCY_CONFLICT");
        }
        error(dm.post(path(c,s)+"/"+id,command),409,"IDEMPOTENCY_CONFLICT");
        var other=campaign(dm);var otherScene=scene(dm,other);error(dm.post(path(other,otherScene),command),409,"IDEMPOTENCY_CONFLICT");
        var request=command.get("requestId");var second=tree(dm.post(base(c)+"/scenes",Map.of("requestId",request,"expectedVersion",0,"name","Preparation")),200);
        var prep=UUID.fromString(second.get("id").asString());error(dm.post(path(c,prep),command),409,"IDEMPOTENCY_CONFLICT");
        tree(dm.post(path(c,prep),body(0,"Prepared hidden",true,List.of())),200);assertEquals(s.toString(),dm.read(base(c)+"/scene").get("id").asString());
        tree(dm.post(base(c)+"/invitations",Map.of("requestId",request,"expectedVersion",1)),201);
        assertEquals(2,dm.read(base(c)+"/members").get("campaignVersion").asLong());
        // A newly constructed service uses the persisted receipt, without generating a second token.
        var fresh=new TokenService(new io.dndroll.persistence.JdbcTokenStore(jdbc,new io.dndroll.persistence.JdbcSceneStore(jdbc,new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()))),json),ignored->fail("Replay emitted an event"));
        assertEquals(id,fresh.create(c,s,"p02c_owner",new TokenService.Command((UUID)request,0,"Hero",3,4,false,List.of())).token().id().toString());
    }
    @Test void concurrentRetriesVersionsAndGlobalCrossCampaignKeysHaveOneWinner()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var secondDm=member(c,"DM");var command=body(0,"Concurrent",false,List.of());JsonNode created;
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->dm.post(path(c,s),command));var b=pool.submit(()->dm.post(path(c,s),command));created=tree(a.get(15,TimeUnit.SECONDS),200);assertEquals(created,tree(b.get(15,TimeUnit.SECONDS),200));
        }
        var id=created.get("token").get("id").asString();
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->dm.post(path(c,s)+"/"+id,body(0,"First",false,List.of())));var b=pool.submit(()->secondDm.post(path(c,s)+"/"+id,body(0,"Second",false,List.of())));
            assertStatuses(a,b);
        }
        assertEquals(2,dm.read(path(c,s)).get("sceneVersion").asLong());assertEquals(1,dm.read(path(c,s)).get("tokens").size());
        var other=campaign(dm);var os=scene(dm,other);var cross=body(UUID.randomUUID(),0,"Cross campaign",false,List.of());var crossHere=new HashMap<String,Object>(cross);crossHere.put("expectedVersion",2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->dm.post(path(c,s),crossHere));var b=pool.submit(()->dm.post(path(other,os),cross));assertStatuses(a,b);
        }
        assertEquals(2,dm.read(path(c,s)).get("tokens").size()+dm.read(path(other,os)).get("tokens").size());
    }
    void assertStatuses(Future<HttpResponse<String>>a,Future<HttpResponse<String>>b)throws Exception{var statuses=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS).statusCode(),b.get(15,TimeUnit.SECONDS).statusCode()));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);}
    @Test void receiptFailureRollsBackConfigurationControlsVersionsAndMembershipRemoval()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var player=member(c,"PLAYER");var created=tree(dm.post(path(c,s),body(0,"Original",false,List.of(player.username))),200);var id=UUID.fromString(created.get("token").get("id").asString());var before=dm.read(path(c,s));
        jdbc.execute("create function p02c_fail_receipt() returns trigger language plpgsql as $$ begin raise exception 'forced token receipt failure'; end $$");
        jdbc.execute("create trigger p02c_fail_receipt before insert on token_command_receipt for each row execute function p02c_fail_receipt()");
        try{
            assertThrows(org.springframework.dao.DataAccessException.class,()->service.create(c,s,"p02c_owner",new TokenService.Command(UUID.randomUUID(),1,"Rollback",1,1,false,List.of())));
            assertThrows(org.springframework.dao.DataAccessException.class,()->service.update(c,s,id,"p02c_owner",new TokenService.Command(UUID.randomUUID(),0,"Rollback update",1,1,true,List.of())));
            assertEquals(before,dm.read(path(c,s)));assertEquals(1,jdbc.queryForObject("select count(*) from token_command_receipt where campaign_id=?",Integer.class,c));
        }finally{jdbc.execute("drop trigger p02c_fail_receipt on token_command_receipt");jdbc.execute("drop function p02c_fail_receipt()");}
        jdbc.execute("create function p02c_fail_remove() returns trigger language plpgsql as $$ begin if NEW.kind='REMOVE' then raise exception 'forced remove receipt failure'; end if; return NEW; end $$");
        jdbc.execute("create trigger p02c_fail_remove before insert on account_command_receipt for each row execute function p02c_fail_remove()");
        try{
            assertEquals(500,dm.post(base(c)+"/members/"+player.username+"/remove",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0)).statusCode());
            assertEquals(before,dm.read(path(c,s)));assertEquals(200,player.get(base(c)+"/scene").statusCode());
        }finally{jdbc.execute("drop trigger p02c_fail_remove on account_command_receipt");jdbc.execute("drop function p02c_fail_remove()");}
        tree(dm.post(path(c,s)+"/"+id,body(0,"Now succeeds",true,List.of())),200);
    }
    @Test void authoringInvalidationIsAfterCommitAndContainsOnlySafeCampaignIdentity()throws Exception{
        var dm=owner();var c=campaign(dm);var s=scene(dm,c);var player=member(c,"PLAYER");var frames=new LinkedBlockingQueue<String>();var fragments=new StringBuilder();
        var socket=player.client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:"+port+"/ws"),new WebSocket.Listener(){
            public void onOpen(WebSocket ws){ws.request(1);}public CompletionStage<?>onText(WebSocket ws,CharSequence text,boolean last){fragments.append(text);if(last){frames.add(fragments.toString());fragments.setLength(0);}ws.request(1);return null;}
        }).get(5,TimeUnit.SECONDS);
        try{
            socket.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nX-CSRF-TOKEN:"+player.csrf+"\n\n\0",true).join();assertTrue(Objects.requireNonNull(frames.poll(5,TimeUnit.SECONDS)).startsWith("CONNECTED"));
            socket.sendText("SUBSCRIBE\nid:token-changes\ndestination:/user/queue/scene-changes\n\n\0",true).join();
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);boolean subscribed=false;
            while(System.nanoTime()<until){var user=registry.getUser(player.username);if(user!=null&&user.getSessions().stream().anyMatch(x->!x.getSubscriptions().isEmpty())){subscribed=true;break;}Thread.sleep(20);}assertTrue(subscribed);
            var command=body(0,"Secret hidden creation",true,List.of(player.username));var created=tree(dm.post(path(c,s),command),200);
            String frame=frames.poll(5,TimeUnit.SECONDS);assertNotNull(frame);var event=json.readTree(frame.substring(frame.indexOf("\n\n")+2).replace("\0",""));
            assertEquals(2,event.size());assertEquals("SCENE_CHANGED",event.get("type").asString());assertEquals(c.toString(),event.get("campaignId").asString());
            assertFalse(frame.contains("Secret hidden creation"));assertFalse(frame.contains(created.get("token").get("id").asString()));assertFalse(frame.contains("controllers"));
            assertEquals(1,dm.read(path(c,s)).get("tokens").size());assertEquals(0,player.read(base(c)+"/scene").get("tokens").size());
            assertEquals(created,tree(dm.post(path(c,s),command),200));assertNull(frames.poll(250,TimeUnit.MILLISECONDS),"Receipt replay must not emit another event");
        }finally{socket.abort();}
    }
}
