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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p05_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class DiceIT {
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
    Browser dm()throws Exception{var b=new Browser();b.login("p05_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","ZZZ Dice test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="p05_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
    String base(UUID c){return "/api/campaigns/"+c;}
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return r.body().isEmpty()?null:json.readTree(r.body());}
    JsonNode create(Browser b,UUID c,long v,String name)throws Exception{return tree(b.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",CharacterSheet.empty(name))),200);}
    Map<String,Object> roll(long version,String character,String key,String expression,String visibility){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"characterId",character,"key",key,"expression",expression,"visibility",visibility,"advantage",List.of(),"disadvantage",List.of());}
    @AfterEach void cleanup(){
        for(UUID c:campaigns){if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='dice_roll' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from dice_roll where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}
        jdbc.update("delete from account_command_receipt where actor='p05_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }
    @Test void rollIsServerOwnedDurableSnapshotAndCommandReusesSameReceipt()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var card=create(p,c,0,"First hero");String id=card.get("id").asString();String path=base(c)+"/rolls";
        var body=roll(0,id,"ability:DEX","","PUBLIC");var first=tree(p.post(path,body),200);
        assertEquals(first,tree(p.post(path,body),200));assertEquals("First hero",first.get("name").asString());assertEquals(p.username,first.get("actor").asString());assertEquals(1,first.get("result").get("dice").size());assertTrue(first.get("result").get("total").asInt()>=1);
        assertEquals(0,p.read(base(c)+"/characters/"+id).get("version").asLong());
        var command=Map.of("requestId",body.get("requestId"),"expectedVersion",0,"characterId",id,"text","/check ability:DEX");assertEquals(first,tree(p.post(base(c)+"/roll-commands",command),200));
        var sheet=json.readTree(json.writeValueAsString(CharacterSheet.empty("Renamed")));((tools.jackson.databind.node.ObjectNode)sheet.get("abilities")).put("DEX",18);
        tree(p.post(base(c)+"/characters/"+id,Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet,"reason","Upgrade")),200);
        assertEquals(first,p.read(path+"/"+first.get("id").asString()));assertEquals(first,tree(p.post(path,body),200));
        tree(p.post(path,roll(0,id,"ability:DEX","","PUBLIC")),409);
        var fresh=tree(p.post(path,roll(1,id,"ability:DEX","","PUBLIC")),200);assertEquals(4,fresh.get("calculation").get("value").asInt());
    }
    @Test void darkRollAndHiddenSourceNeverEnterPlayerListsDetailsOrReplays()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var other=player(c);var card=create(p,c,0,"SECRET HERO");String id=card.get("id").asString(),path=base(c)+"/rolls";
        var body=roll(0,id,"EXPRESSION","2d6+3","PUBLIC");var pub=tree(p.post(path,body),200);
        tree(other.post(path,body),403);tree(p.post(path,roll(0,id,"initiative","","DM")),403);
        var secret=tree(dm.post(path,roll(1,"","EXPRESSION","d20","DM")),200);tree(p.get(path+"/"+secret.get("id").asString()),404);tree(p.get(path+"?before="+secret.get("id").asString()),404);
        assertEquals(1,p.read(path).get("rolls").size());assertEquals(2,dm.read(path).get("rolls").size());
        tree(dm.post(base(c)+"/characters/"+id+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"controllers",List.of(p.username),"hidden",true,"shareFull",false,"approveImport",true,"reason","Hide")),200);
        assertFalse(p.read(path).toString().contains("SECRET"));assertEquals(0,p.read(path).get("rolls").size());tree(p.get(path+"/"+pub.get("id").asString()),404);tree(p.post(path,body),404);
        tree(dm.post(path,roll(1,id,"initiative","","PUBLIC")),403);
        jdbc.update("delete from character_controller where campaign_id=? and username=?",c,p.username);jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,p.username);tree(p.get(path),403);tree(p.post(path,body),403);
        tree(player(campaign(dm)).get(path),403);tree(new Browser().get(path),401);
    }
    @Test void strictInputsConcurrentRetryAndCommitFailureAreAtomic()throws Exception{
        var dm=dm();var c=campaign(dm);String path=base(c)+"/rolls";var body=roll(0,"","EXPRESSION","2d6+3","PUBLIC");
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->dm.post(path,body));var b=pool.submit(()->dm.post(path,body));assertEquals(tree(a.get(),200),tree(b.get(),200));}finally{pool.shutdownNow();}
        assertEquals(1,dm.read(path).get("rolls").size());var bad=new HashMap<>(body);bad.put("expression","3d6");tree(dm.post(path,bad),409);
        bad=new HashMap<>(body);bad.put("total",20);tree(dm.post(path,bad),400);bad=new HashMap<>(body);bad.remove("expectedVersion");tree(dm.post(path,bad),400);
        tree(dm.raw(path,json.writeValueAsString(body),false),403);tree(dm.post(path,roll(0,"","EXPRESSION","d6*100","PUBLIC")),400);
        for(String text:List.of("/eval 1","/roll d20 unknown","/check ac","/roll d20 dm dm"))tree(dm.post(base(c)+"/roll-commands",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"characterId","","text",text)),400);
        String suffix=UUID.randomUUID().toString().replace("-","");jdbc.execute("create function p05_fail_"+suffix+"() returns trigger language plpgsql as $$ begin raise exception 'receipt commit failure'; end $$");
        jdbc.execute("create constraint trigger p05_fail_"+suffix+" after insert on dice_command_receipt deferrable initially deferred for each row execute function p05_fail_"+suffix+"()");
        try{tree(dm.post(path,roll(0,"","EXPRESSION","d20","PUBLIC")),500);assertEquals(1,dm.read(path).get("rolls").size());}finally{jdbc.execute("drop trigger p05_fail_"+suffix+" on dice_command_receipt");jdbc.execute("drop function p05_fail_"+suffix+"()");}
    }
    Map<String,Object> participant(String id,long version,String kind,String group,boolean surprised){return Map.of("characterId",id,"expectedVersion",version,"kind",kind,"group",group,"surprised",surprised,"advantage",List.of(),"disadvantage",List.of());}
    @Test void initiativeGroupsSurpriseIndependentCharactersAndConfirmedPublicProjection()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String a=create(p,c,0,"PC One").get("id").asString(),b=create(p,c,1,"PC Two").get("id").asString(),x=create(dm,c,2,"HIDDEN MONSTER").get("id").asString(),y=create(dm,c,3,"MONSTER Two").get("id").asString();String path=base(c)+"/initiative";
        tree(dm.post(base(c)+"/characters/"+x+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"controllers",List.of(),"hidden",true,"shareFull",false,"approveImport",true,"reason","Hidden enemy")),200);
        var roster=List.of(participant(a,0,"PC","",false),participant(b,0,"PC","",false),participant(x,1,"MONSTER","identical goblins",true),participant(y,0,"MONSTER","identical goblins",true));
        var command=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",roster,"reason","Initial encounter, DM declares identical creatures");
        var ack=tree(dm.post(path,command),200);assertEquals(1,ack.get("version").asLong());assertEquals(ack,tree(dm.post(path,command),200));
        assertEquals("EMPTY",p.read(path).get("status").asString());assertEquals(0,p.read(path).get("entries").size());var draft=dm.read(path);assertEquals("DRAFT",draft.get("status").asString());assertEquals(4,draft.get("entries").size());
        JsonNode one=null,two=null;for(var entry:draft.get("entries")){if(entry.get("characterId").asString().equals(x))one=entry;if(entry.get("characterId").asString().equals(y))two=entry;}
        assertNotNull(one);assertNotNull(two);assertEquals(one.get("result"),two.get("result"));assertEquals("DISADVANTAGE",one.get("result").get("mode").asString());assertEquals(2,one.get("result").get("dice").size());
        tree(p.post(path,command),403);var order=List.of(b,x,a,y);var confirm=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",order,"reason","Players chose PC tie order; DM confirmed monster ties and exceptional adjustment");
        var confirmed=tree(dm.post(path+"/confirm",confirm),200);assertEquals(2,confirmed.get("version").asLong());assertEquals(confirmed,tree(dm.post(path+"/confirm",confirm),200));
        var pub=p.read(path);assertEquals("CONFIRMED",pub.get("status").asString());assertEquals(3,pub.get("entries").size());assertFalse(pub.toString().contains(x));assertFalse(pub.toString().contains("HIDDEN"));assertFalse(pub.has("version"));
        assertEquals(List.of("characterId","name","total"),new ArrayList<>(pub.get("entries").get(0).propertyNames()));assertEquals(b,pub.get("entries").get(0).get("characterId").asString());
        tree(p.get(path+"/history"),403);assertEquals(2,dm.read(path+"/history").size());assertEquals(0,p.read(base(c)+"/rolls").get("rolls").size());
        var again=new HashMap<>(command);again.put("requestId",UUID.randomUUID());again.put("expectedVersion",2);tree(dm.post(path,again),200);assertEquals(ack,tree(dm.post(path,command),200));assertEquals("DRAFT",dm.read(path).get("status").asString());assertEquals(3,dm.read(path).get("version").asLong());
    }
    @Test void initiativeRejectsInvalidGroupsStaleSourcesAndConcurrentVersionsWithoutPartialHistory()throws Exception{
        var dm=dm();var c=campaign(dm);String a=create(dm,c,0,"A").get("id").asString(),b=create(dm,c,1,"B").get("id").asString(),path=base(c)+"/initiative";
        var invalid=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",List.of(participant(a,0,"PC","illegal",false)),"reason","Bad group");tree(dm.post(path,invalid),400);
        invalid=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",List.of(participant(a,0,"MONSTER","group",false),participant(b,0,"MONSTER","group",true)),"reason","Mixed surprise");tree(dm.post(path,invalid),400);
        invalid=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",List.of(participant(a,1,"PC","",false)),"reason","Stale card");tree(dm.post(path,invalid),409);assertEquals(0,dm.read(path+"/history").size());
        var roster=List.of(participant(a,0,"PC","",false),participant(b,0,"PC","",false));var first=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",roster,"reason","Start");var second=new HashMap<>(first);second.put("requestId",UUID.randomUUID());
        var pool=Executors.newFixedThreadPool(2);try{var x=pool.submit(()->dm.post(path,first));var y=pool.submit(()->dm.post(path,second));var statuses=new ArrayList<>(List.of(x.get().statusCode(),y.get().statusCode()));Collections.sort(statuses);assertEquals(List.of(200,409),statuses);}finally{pool.shutdownNow();}
        assertEquals(1,dm.read(path+"/history").size());tree(dm.post(path+"/confirm",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"order",List.of(a,a),"reason","Bad permutation")),400);assertEquals(1,dm.read(path).get("version").asLong());
    }
    @Test void visiblePaginationFiltersPrivateRowsBeforeTheLimitAndAllResponsesAreNoStore()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c)+"/rolls";
        for(int n=0;n<55;n++){tree(p.post(path,roll(0,"","EXPRESSION",Integer.toString(n),"PUBLIC")),200);if(n%5==0)tree(dm.post(path,roll(0,"","EXPRESSION","d20","DM")),200);}
        var page=p.read(path);assertEquals(50,page.get("rolls").size());assertEquals(2,page.size());assertFalse(page.toString().contains("ordinal"));assertFalse(page.toString().contains("DM\""));
        String cursor=page.get("next").asString();assertFalse(cursor.isEmpty());var older=p.read(path+"?before="+cursor);assertEquals(5,older.get("rolls").size());assertEquals("",older.get("next").asString());
        var ids=new HashSet<String>();for(var row:page.get("rolls"))ids.add(row.get("id").asString());for(var row:older.get("rolls"))assertTrue(ids.add(row.get("id").asString()));assertEquals(55,ids.size());
        for(var response:List.of(p.get(path),p.get(path+"/"+UUID.randomUUID()),p.get(path+"?before=bad"),new Browser().get(path))){assertEquals("no-store, private",response.headers().firstValue("Cache-Control").orElse(""));assertEquals("nosniff",response.headers().firstValue("X-Content-Type-Options").orElse(""));}
    }
    @Test void removedControlPreventsRollReplayWithoutHidingItsAlreadyPublicSnapshot()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String id=create(p,c,0,"Public hero").get("id").asString(),path=base(c)+"/rolls";var command=roll(0,id,"initiative","","PUBLIC");var result=tree(p.post(path,command),200);
        tree(dm.post(base(c)+"/characters/"+id+"/access",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"controllers",List.of(),"hidden",false,"shareFull",false,"approveImport",true,"reason","Revoke control only")),200);
        tree(p.post(path,command),403);assertEquals(result,p.read(path+"/"+result.get("id").asString()));
    }
    @Test void initiativeReceiptCommitFailureRollsBackAndDowngradeDeniesOldReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);String id=create(dm,c,0,"Hero").get("id").asString(),path=base(c)+"/initiative";var command=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"participants",List.of(participant(id,0,"PC","",false)),"reason","Rollback test");String suffix=c.toString().replace("-","");
        jdbc.execute("create function p05_init_"+suffix+"() returns trigger language plpgsql as $$ begin raise exception 'injected receipt commit failure'; end $$");jdbc.execute("create constraint trigger p05_init_"+suffix+" after insert on initiative_command_receipt deferrable initially deferred for each row execute function p05_init_"+suffix+"()");
        try{tree(dm.post(path,command),500);assertEquals(0,dm.read(path).get("version").asLong());assertEquals(0,dm.read(path+"/history").size());}finally{jdbc.execute("drop trigger p05_init_"+suffix+" on initiative_command_receipt");jdbc.execute("drop function p05_init_"+suffix+"()");}
        var ack=tree(dm.post(path,command),200);assertEquals(ack,tree(dm.post(path,command),200));assertEquals(1,dm.read(path+"/history").size());
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.post(path,command),403);tree(dm.get(path+"/history"),403);assertEquals("EMPTY",dm.read(path).get("status").asString());
    }
}
