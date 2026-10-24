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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=p08_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class NotesIT {
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
    Browser dm()throws Exception{var b=new Browser();b.login("p08_owner");return b;}
    UUID campaign(Browser dm)throws Exception{var r=tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","ZZZ Dice test")),201);var c=UUID.fromString(r.get("id").asString());campaigns.add(c);return c;}
    Browser player(UUID c)throws Exception{String n="p08_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(n);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),n,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER')",c,n);var b=new Browser();b.login(n);return b;}
    String base(UUID c){return "/api/campaigns/"+c;}
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return r.body().isEmpty()?null:json.readTree(r.body());}
    JsonNode create(Browser b,UUID c,long v,String name)throws Exception{return tree(b.post(base(c)+"/characters",Map.of("requestId",UUID.randomUUID(),"expectedVersion",v,"sheet",CharacterSheet.empty(name))),200);}
    Map<String,Object> roll(long version,String character,String key,String expression,String visibility){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"characterId",character,"key",key,"expression",expression,"visibility",visibility,"advantage",List.of(),"disadvantage",List.of());}
    @AfterEach void cleanup(){
        for(UUID c:campaigns){if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='combat_action' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from combat_action where campaign_id=?",c);if(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='dice_roll' and table_schema=current_schema()",Integer.class)>0)jdbc.update("delete from dice_roll where campaign_id=?",c);jdbc.update("update token set character_id=null where campaign_id=?",c);jdbc.update("delete from character_command_receipt where campaign_id=?",c);jdbc.update("delete from character_card where campaign_id=?",c);jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}
        jdbc.update("delete from account_command_receipt where actor='p08_owner'");for(String u:users){jdbc.update("delete from account_command_receipt where actor=?",u);jdbc.update("delete from app_user where username=?",u);}
    }

    Map<String,Object> note(long version,String visibility,String title,String text){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"visibility",visibility,"title",title,"text",text,"attachments",List.of());}
    Map<String,Object> edit(long version,String title,String text){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"title",title,"text",text,"attachments",List.of(),"reason","Correct shared note");}
    @Test void sharedEditorsKeepImmutableHistoryAndExactReceipts()throws Exception{
        var dm=dm();var c=campaign(dm);var a=player(c);var b=player(c);String path=base(c)+"/notes";
        var body=note(0,"TEAM","Party plan","Enter the hall");var ack=tree(a.post(path,body),200);String id=ack.get("id").asString();assertEquals(ack,tree(a.post(path,body),200));
        var change=edit(0,"Party plan","Ask the guard first");var changed=tree(b.post(path+"/"+id,change),200);assertEquals(changed,tree(b.post(path+"/"+id,change),200));
        var current=a.read(path+"/"+id);assertEquals(1,current.get("version").asLong());assertEquals(b.username,current.get("actor").asString());
        var history=a.read(path+"/"+id+"/history");assertEquals(2,history.size());assertEquals(a.username,history.get(0).get("note").get("actor").asString());assertEquals("Enter the hall",history.get(0).get("note").get("text").asString());
        tree(a.post(path+"/"+id,edit(0,"Stale","No overwrite")),409);var conflict=new HashMap<>(body);conflict.put("text","Different");tree(a.post(path,conflict),409);
        assertEquals(0,a.read(path).get("campaignVersion").asLong());assertEquals(1,a.read(path).get("notes").size());
    }
    @Test void dmMaterialsNeverEnterPlayerQueriesHistoryExportCursorOrReplay()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c)+"/notes";var secret=note(0,"DM","SECRET_AMBUSH","Hidden passage");String id=tree(dm.post(path,secret),200).get("id").asString();
        tree(p.get(path+"/"+id),404);tree(p.get(path+"/"+id+"/history"),404);tree(p.get(path+"?before="+id),404);tree(p.post(path+"/"+id,edit(0,"Probe","Guess")),404);tree(p.post(path,note(0,"DM","Forged secret","No")),403);
        assertEquals(0,p.read(path+"?q=SECRET_AMBUSH").get("notes").size());assertEquals(0,p.read(path+"?visibility=DM").get("notes").size());
        assertFalse(p.read(path+"/export?format=json").toString().contains(id));assertFalse(p.get(path+"/export?format=markdown").body().contains("SECRET_AMBUSH"));assertTrue(dm.read(path+"/export?format=json").toString().contains(id));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(dm.get(path+"/"+id),404);tree(dm.post(path,secret),403);
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,p.username);tree(p.get(path),403);
    }
    @Test void imagesKeepVisibilityOwnershipHistoryAndCurrentDownloadAuthority()throws Exception{
        var dm=dm();var c=campaign(dm);var a=player(c);var b=player(c);String path=base(c)+"/notes";
        var command=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","TEAM");var image=tree(upload(a,c,command,png(),"image/png"),200);String imageId=image.get("id").asString(),content=base(c)+"/note-attachments/"+imageId+"/content";
        assertEquals(image,tree(upload(a,c,command,png(),"image/png"),200));assertEquals(200,a.get(content).statusCode());tree(b.get(content),404);tree(dm.get(content),404);
        var claim=new HashMap<>(note(0,"TEAM","Claim","Not mine"));claim.put("attachments",List.of(imageId));tree(b.post(path,claim),404);
        String id=tree(a.post(path,claim),200).get("id").asString();assertEquals(200,b.get(content).statusCode());
        var keep=new HashMap<>(edit(0,"Shared image","Other editor"));keep.put("attachments",List.of(imageId));tree(b.post(path+"/"+id,keep),200);
        tree(b.post(path+"/"+id,edit(1,"Image in history","Removed current image")),200);assertEquals(200,b.get(content).statusCode());assertEquals(imageId,b.read(path+"/"+id+"/history").get(0).get("note").get("attachments").get(0).get("id").asString());
        assertEquals(1,b.read(path+"/export?format=json").get("attachments").size());tree(b.post(path,claim),403);
        var privateCommand=Map.of("requestId",UUID.randomUUID(),"expectedVersion",0,"visibility","DM");var privateImage=tree(upload(dm,c,privateCommand,png(),"image/png"),200);String privateId=privateImage.get("id").asString();
        var secret=new HashMap<>(note(0,"DM","SECRET_IMAGE","DM preparation"));secret.put("attachments",List.of(privateId));tree(dm.post(path,secret),200);tree(a.get(base(c)+"/note-attachments/"+privateId+"/content"),404);
        var leak=new HashMap<>(note(0,"TEAM","Wrong visibility","No automatic publish"));leak.put("attachments",List.of(privateId));tree(dm.post(path,leak),400);
        tree(upload(a,c,privateCommand,png(),"image/png"),403);tree(upload(a,c,command,"<svg/>".getBytes(),"image/svg+xml"),400);
        assertEquals("no-store, private",b.get(content).headers().firstValue("Cache-Control").orElse(""));assertFalse(image.toString().contains("storage"));
        jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,dm.username);tree(upload(dm,c,privateCommand,png(),"image/png"),403);tree(dm.get(base(c)+"/note-attachments/"+privateId+"/content"),404);
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,a.username);tree(a.get(content),403);tree(upload(a,c,command,png(),"image/png"),403);
    }
    @Test void completeExportLiteralSearchAndPrivateCursorsDoNotLeakOrTruncate()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c)+"/notes";
        for(int i=0;i<52;i++)tree(p.post(path,note(0,"TEAM","Note "+i,i==0?"Plain <script>x</script> %_":"Text "+i)),200);
        String secret=tree(dm.post(path,note(0,"DM","PRIVATE_KEY","SECRET_TEXT")),200).get("id").asString();var page=p.read(path);assertEquals(50,page.get("notes").size());assertEquals(2,p.read(path+"?before="+page.get("next").asString()).get("notes").size());
        var all=p.read(path+"/export?format=json");assertEquals(52,all.get("notes").size());assertEquals(52,all.get("histories").size());assertEquals("Note 0",all.get("notes").get(0).get("title").asString());assertFalse(all.toString().contains(secret));
        assertEquals(1,p.read(path+"?q=%25_").get("notes").size());var md=p.get(path+"/export?format=markdown");assertEquals(200,md.statusCode());assertTrue(md.body().contains("Note 51"));assertTrue(md.body().contains("&lt;script&gt;"));assertFalse(md.body().contains("SECRET_TEXT"));
        tree(p.get(path+"?visibility=PRIVATE"),400);tree(p.get(path+"?q="+"x".repeat(101)),400);tree(p.get(path+"?before="+secret),404);
    }
    @Test void strictJsonCsrfForeignIdsRemovalRacesAndRollbackAreEnforced()throws Exception{
        var dm=dm();var c=campaign(dm);var p=player(c);String path=base(c)+"/notes";var body=note(0,"TEAM","Once","Persistent");var pool=Executors.newFixedThreadPool(2);String id;
        try{var a=pool.submit(()->p.post(path,body));var b=pool.submit(()->p.post(path,body));var ack=tree(a.get(),200);assertEquals(ack,tree(b.get(),200));id=ack.get("id").asString();var a2=pool.submit(()->p.post(path+"/"+id,edit(0,"A","First")));var b2=pool.submit(()->p.post(path+"/"+id,edit(0,"B","Second")));assertEquals(List.of(200,409),java.util.stream.Stream.of(a2.get().statusCode(),b2.get().statusCode()).sorted().toList());}finally{pool.shutdownNow();}
        assertEquals(2,p.read(path+"/"+id+"/history").size());var forged=new HashMap<>(body);forged.put("actor","dm");tree(p.post(path,forged),400);forged=new HashMap<>(body);forged.remove("expectedVersion");tree(p.post(path,forged),400);tree(p.raw(path,json.writeValueAsString(body),false),403);
        tree(new Browser().get(path),401);tree(player(campaign(dm)).get(path+"/"+id),403);tree(p.get(base(campaign(dm))+"/notes/"+id),403);assertEquals("no-store, private",new Browser().get(path).headers().firstValue("Cache-Control").orElse(""));
        String suffix=UUID.randomUUID().toString().replace("-","");jdbc.execute("create function p08_fail_"+suffix+"() returns trigger language plpgsql as $$ begin raise exception 'receipt failure'; end $$");jdbc.execute("create constraint trigger p08_fail_"+suffix+" after insert on note_command_receipt deferrable initially deferred for each row execute function p08_fail_"+suffix+"()");
        try{tree(p.post(path+"/"+id,edit(1,"Rollback","Never save")),500);assertEquals(2,p.read(path+"/"+id+"/history").size());assertEquals(1,p.read(path+"/"+id).get("version").asLong());}finally{jdbc.execute("drop trigger p08_fail_"+suffix+" on note_command_receipt");jdbc.execute("drop function p08_fail_"+suffix+"()");}
        jdbc.update("delete from campaign_member where campaign_id=? and username=?",c,p.username);tree(p.post(path,body),403);tree(p.get(path+"/"+id+"/history"),403);tree(p.get(path+"/export?format=json"),403);
    }
    byte[] png()throws Exception{var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    HttpResponse<String> upload(Browser b,UUID c,Object command,byte[] bytes,String mime)throws Exception{
        String boundary="p08Multipart";var out=new java.io.ByteArrayOutputStream();out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"command\"\r\nContent-Type: application/json\r\n\r\n"+json.writeValueAsString(command)+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"ignored.png\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(bytes);out.write(("\r\n--"+boundary+"--\r\n").getBytes());
        return b.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+"/note-attachments")).header("X-CSRF-TOKEN",b.csrf).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
}
