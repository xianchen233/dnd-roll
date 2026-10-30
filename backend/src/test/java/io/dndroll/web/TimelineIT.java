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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p07_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class TimelineIT {
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
    Browser dm()throws Exception{var b=new Browser();b.login("p07_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","ZZZ Dice test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="p07_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
    String base(UUID c){return "/api/campaigns/"+c;}
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return r.body().isEmpty()?null:json.readTree(r.body());}
    JsonNode create(Browser b,UUID c,long v,String name)throws Exception{return tree(b.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",CharacterSheet.empty(name))),200);}
    Map<String,Object> roll(long version,String character,String key,String expression,String visibility){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"characterId",character,"key",key,"expression",expression,"visibility",visibility,"advantage",List.of(),"disadvantage",List.of());}
    @AfterEach void cleanup(){
        for(UUID c:campaigns){if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='combat_action' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from combat_action where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='dice_roll' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from dice_roll where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}
        jdbc.update("delete from account_command_receipt where actor='p07_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }

    Map<String,Object> message(long v,String character,String text,String reply){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"characterId",character,"kind",character.isEmpty()?"OOC":"RP","text",text,"replyTo",reply,"attachments",List.of());}
    Map<String,Object> edit(long v,String text){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"text",text,"attachments",List.of(),"reason","Correct spelling");}
    byte[] png()throws Exception{var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    HttpResponse<String> upload(Browser b,UUID c,Object command,byte[] bytes,String mime)throws Exception{
        String boundary="p07Multipart";var out=new java.io.ByteArrayOutputStream();out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"command\"\r\nContent-Type: application/json\r\n\r\n"+json.writeValueAsString(command)+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"ignored.png\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(bytes);out.write(("\r\n--"+boundary+"--\r\n").getBytes());
        return b.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+"/attachments")).header("X-CSRF-TOKEN",b.csrf).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void imagesArePrivateUntilReferencedAndOlderEditsKeepTheirFiles()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);var other=player(c);String hero=create(p,c,0,"Image Hero").get("id").asString();var command=Map.of("requestId",UUID.randomUUID(),"expectedVersion",1);var image=tree(upload(p,c,command,png(),"image/png"),200);String imageId=image.get("id").asString(),content=base(c)+"/attachments/"+imageId+"/content";
        assertEquals(image,tree(upload(p,c,command,png(),"image/png"),200));tree(other.get(content),404);assertEquals(200,p.get(content).statusCode());assertFalse(image.toString().contains("storage"));
        var body=new HashMap<>(message(0,hero,"Image message",""));body.put("attachments",List.of(imageId));String id=tree(p.post(base(c)+"/messages",body),200).get("id").asString();assertEquals(200,other.get(content).statusCode());
        tree(p.post(base(c)+"/messages/"+id,edit(0,"Image removed from current revision")),200);assertEquals(200,other.get(content).statusCode());assertEquals(imageId,other.read(base(c)+"/messages/"+id+"/history").get(0).get("attachments").get(0).get("id").asString());
        jdbc.update("update character_card set hidden=true where id=?",UUID.fromString(hero));tree(p.get(content),404);tree(other.get(content),404);assertEquals(200,dm.get(content).statusCode());
        tree(upload(p,c,Map.of("requestId",UUID.randomUUID(),"expectedVersion",1),"<svg/>".getBytes(),"image/svg+xml"),400);tree(player(campaign(dm)).get(content),403);
    }
    @Test void privateReferencesSearchAndExportsUseFilteredSourceProjections()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c),hero=create(p,c,0,"SECRET NAME").get("id").asString();
        String dark=tree(dm.post(path+"/rolls",roll(0,hero,"EXPRESSION","1d20+9876","DM")),200).get("id").asString();
        String reply=tree(dm.post(path+"/messages",message(1,"","Public discussion",dark)),200).get("id").asString();assertTrue(p.read(path+"/timeline/"+reply).get("reply").isNull());assertFalse(p.read(path+"/messages/"+reply+"/history").toString().contains(dark));tree(p.post(path+"/messages",message(1,"","Try private quote",dark)),404);
        assertEquals(0,p.read(path+"/timeline?q=9876").get("entries").size());assertFalse(p.read(path+"/timeline/export?format=json").toString().contains(dark));assertTrue(dm.read(path+"/timeline/export?format=json").toString().contains(dark));
        String publicRoll=tree(p.post(path+"/rolls",roll(0,hero,"EXPRESSION","1d6","PUBLIC")),200).get("id").asString();String publicReply=tree(p.post(path+"/messages",message(1,"","Visible quoted roll",publicRoll)),200).get("id").asString();assertEquals(publicRoll,p.read(path+"/timeline/"+publicReply).get("reply").get("id").asString());
        jdbc.update("update character_card set hidden=true where id=?",UUID.fromString(hero));assertTrue(p.read(path+"/timeline/"+publicReply).get("reply").isNull());tree(p.get(path+"/timeline?before="+publicRoll),404);assertEquals(0,p.read(path+"/timeline?q=SECRET%20NAME").get("entries").size());assertFalse(p.read(path+"/timeline/export?format=json").toString().contains("SECRET NAME"));
    }
    @Test void paginationExportAndSessionArchiveNeverTruncateHistory()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c);var start=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"kind","START","name","First night");var session=tree(dm.post(path+"/timeline/sessions",start),200);assertEquals(session,tree(dm.post(path+"/timeline/sessions",start),200));
        for(int i=0;i<52;i++)tree(p.post(path+"/messages",message(1,"","Entry "+i,"")),200);
        var first=p.read(path+"/timeline");assertEquals(50,first.get("entries").size());String cursor=first.get("next").asString();assertFalse(cursor.isEmpty());var second=p.read(path+"/timeline?before="+cursor);assertEquals(2,second.get("entries").size());assertEquals("",second.get("next").asString());
        var exported=p.read(path+"/timeline/export?format=json");assertEquals(52,exported.get("entries").size());assertEquals("Entry 0",exported.get("entries").get(0).get("data").get("text").asString());assertEquals(52,p.read(path+"/timeline/export?format=json&sessionId="+session.get("id").asString()).get("entries").size());
        var markdown=p.get(path+"/timeline/export?format=markdown");assertEquals(200,markdown.statusCode());assertTrue(markdown.body().contains("Entry 51"));assertTrue(markdown.body().contains("Entry 0"));tree(p.get(path+"/timeline?from=not-a-date"),400);tree(p.get(path+"/timeline?kind=PRIVATE"),400);
        tree(p.post(path+"/timeline/sessions",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"kind","ARCHIVE","name","")),403);tree(dm.post(path+"/timeline/sessions",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"kind","ARCHIVE","name","")),200);assertEquals(52,p.read(path+"/timeline/export?format=json").get("entries").size());assertTrue(p.read(path+"/timeline/sessions").get("sessions").get(0).get("archived").asBoolean());
    }
    @Test void sameRequestRacesCommitRollbackAndConcurrentEditsHaveOneEffect()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c)+"/messages";var body=message(0,"","Once","");var pool=Executors.newFixedThreadPool(2);String id;
        try{var a=pool.submit(()->p.post(path,body));var b=pool.submit(()->p.post(path,body));var ack=tree(a.get(),200);assertEquals(ack,tree(b.get(),200));id=ack.get("id").asString();var a2=pool.submit(()->p.post(path+"/"+id,edit(0,"One")));var b2=pool.submit(()->p.post(path+"/"+id,edit(0,"Two")));assertEquals(List.of(200,409),java.util.stream.Stream.of(a2.get().statusCode(),b2.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}
        var changed=new HashMap<>(body);changed.put("text","Changed");tree(p.post(path,changed),409);assertEquals(2,p.read(path+"/"+id+"/history").size());
        String suffix=UUID.randomUUID().toString().replace("-","");jdbc.execute("create function p07_fail_"+suffix+"() returns trigger language plpgsql as $$ begin raise exception 'receipt failure'; end $$");jdbc.execute("create constraint trigger p07_fail_"+suffix+" after insert on timeline_command_receipt deferrable initially deferred for each row execute function p07_fail_"+suffix+"()");
        try{tree(p.post(path+"/"+id,edit(1,"Should roll back")),500);assertEquals(2,p.read(path+"/"+id+"/history").size());assertEquals(1,p.read(base(c)+"/timeline/"+id).get("data").get("version").asLong());}finally{jdbc.execute("drop trigger p07_fail_"+suffix+" on timeline_command_receipt");jdbc.execute("drop function p07_fail_"+suffix+"()");}
    }
    @Test void actionTimelineUsesExistingPrivateEventProjectionAndCurrentTargetVisibility()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String root=base(c);var sheet=(tools.jackson.databind.node.ObjectNode)json.valueToTree(CharacterSheet.empty("Public attacker"));sheet.set("attacks",json.valueToTree(List.of(new CharacterSheet.Attack("sword","Sword","STR",true,0,0,"1d6","slashing","",CharacterSheet.manual()))));
        String hero=tree(p.post(root+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"sheet",sheet)),200).get("id").asString(),target=create(dm,c,1,"Target name").get("id").asString();var declaration=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"characterId",hero,"targetId",target,"attackId","sword","visibility","PUBLIC");String action=tree(p.post(root+"/actions",declaration),200).get("id").asString();
        tree(dm.post(root+"/actions/"+action+"/events",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"kind","VERDICT","rollId","","verdict","HIT","changes",List.of(),"correctionOf","","reason","PRIVATE_HIT_REASON")),200);
        String dark=tree(dm.post(root+"/rolls",roll(0,hero,"attack:sword","","DM")),200).get("id").asString();tree(dm.post(root+"/actions/"+action+"/events",Map.of("requestId",UUID.randomUUID(),"expectedVersion",1,"kind","LINK_ROLL","rollId",dark,"verdict","","changes",List.of(),"correctionOf","","reason","PRIVATE_LINK_REASON")),200);
        String publicData=p.read(root+"/timeline/export?format=json").toString();assertFalse(publicData.contains(dark));assertFalse(publicData.contains("PRIVATE_"));assertFalse(publicData.contains("privateOnly"));assertTrue(p.read(root+"/timeline/"+action).get("data").has("mutationVersion"));assertEquals(0,p.read(root+"/timeline?q=PRIVATE_HIT_REASON").get("entries").size());
        String reply=tree(dm.post(root+"/messages",message(2,"","Reply to action",action)),200).get("id").asString();jdbc.update("update character_card set hidden=true where id=?",UUID.fromString(target));assertTrue(p.read(root+"/timeline/"+reply).get("reply").isNull());tree(p.get(root+"/timeline/"+action),404);tree(p.get(root+"/timeline?before="+action),404);assertEquals(0,p.read(root+"/timeline?q=Target%20name").get("entries").size());assertFalse(p.read(root+"/timeline/export?format=json").toString().contains(target));
    }
    @Test void messagesSnapshotIdentityKeepEditsAndReauthorizeReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String character=create(p,c,0,"Original Hero").get("id").asString(),path=base(c)+"/messages";
        var body=message(0,character,"We enter the hall","");var ack=tree(p.post(path,body),200);String id=ack.get("id").asString();assertEquals(ack,tree(p.post(path,body),200));
        var original=dm.read(base(c)+"/timeline/"+id);assertEquals("Original Hero",original.get("data").get("name").asString());assertEquals(p.username,original.get("data").get("actor").asString());
        var revision=edit(0,"We enter the great hall");tree(p.post(path+"/"+id,revision),200);tree(p.post(path+"/"+id,revision),200);
        var history=p.read(path+"/"+id+"/history");assertEquals(2,history.size());assertEquals("We enter the hall",history.get(0).get("text").asString());assertEquals("We enter the great hall",history.get(1).get("text").asString());
        tree(dm.post(path+"/"+id,edit(1,"Impersonation")),403);tree(p.post(path+"/"+id,edit(0,"Stale")),409);
        jdbc.update("update character_card set sheet=jsonb_set(sheet,'{name}','\"New Hero\"'),version=version+1 where id=?",UUID.fromString(character));
        assertEquals("Original Hero",p.read(base(c)+"/timeline/"+id).get("data").get("name").asString());
        jdbc.update("delete from character_controller where character_id=?",UUID.fromString(character));tree(p.post(path,body),403);tree(p.post(path+"/"+id,revision),403);
        jdbc.update("update character_card set hidden=true where id=?",UUID.fromString(character));tree(p.get(base(c)+"/timeline/"+id),404);tree(p.get(path+"/"+id+"/history"),404);assertEquals(0,p.read(base(c)+"/timeline").get("entries").size());
    }
    @Test void strictJsonCrossCampaignCsrfAndSearchRespectCurrentVisibility()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c)+"/messages";var body=message(0,"","Plain <script>text</script> %_","");String id=tree(p.post(path,body),200).get("id").asString();
        var forged=new HashMap<>(body);forged.put("actor","dm");tree(p.post(path,forged),400);forged=new HashMap<>(body);forged.remove("expectedVersion");tree(p.post(path,forged),400);tree(p.raw(path,json.writeValueAsString(body),false),403);tree(new Browser().get(base(c)+"/timeline"),401);
        tree(player(campaign(dm)).get(base(c)+"/timeline/"+id),403);tree(p.post(path,message(0,"","Foreign reply",UUID.randomUUID().toString())),404);
        assertEquals(1,p.read(base(c)+"/timeline?q=%25_").get("entries").size());assertEquals("no-store, private",p.get(base(c)+"/timeline").headers().firstValue("Cache-Control").orElse(""));
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,p.username);tree(p.post(path,body),403);tree(p.get(base(c)+"/timeline"),403);
    }
}
