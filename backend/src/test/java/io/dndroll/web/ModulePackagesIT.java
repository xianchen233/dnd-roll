package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import io.dndroll.modules.ModuleArchive;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=","BOOTSTRAP_USERNAME=a01_owner","BOOTSTRAP_PASSWORD=integration-owner-password"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ModulePackagesIT {
    @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;
    final JsonMapper json=JsonMapper.builder().build();final Set<UUID> campaigns=new HashSet<>();final Set<String> users=new HashSet<>();
    final String module="10000000-0000-4000-8000-000000000001";
    @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String csrf,username;
        Browser()throws Exception{csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
        void login(String name)throws Exception{username=name;tree(post("/api/auth/login",Map.of("username",name,"password","integration-owner-password")),200);csrf=tree(get("/api/auth/csrf"),200).get("token").asString();}
        JsonNode read(String path)throws Exception{return tree(get(path),200);}
    }
    JsonNode tree(HttpResponse<String> r,int status){assertEquals(status,r.statusCode(),r.body());return json.readTree(r.body());}
    Browser dm()throws Exception{var b=new Browser();b.login("a01_owner");return b;}
    UUID campaign(Browser dm)throws Exception{UUID c=UUID.fromString(tree(dm.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","Module import test")),201).get("id").asString());campaigns.add(c);return c;}
    Browser member(UUID c,String role)throws Exception{String name="a01_"+UUID.randomUUID().toString().replace("-","").substring(0,12);users.add(name);jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),name,passwords.encode("integration-owner-password"));jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?)",c,name,role);var b=new Browser();b.login(name);return b;}
    String base(UUID c){return "/api/campaigns/"+c+"/module-packages";}
    Map<String,Object> command(long version,boolean copy,boolean legacy){return Map.of("requestId",UUID.randomUUID(),"expectedVersion",version,"createCopy",copy,"confirmLegacy",legacy);}
    byte[] archive(String version,String rules,List<?> dependencies,String name)throws Exception{
        byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of(Map.of("id",module,"kind","DOCUMENT","name","Secret preparation","text","<script>inert</script>","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()))));
        var manifest=Map.of("format","dnd-roll.module","formatVersion",1,"moduleId",module,"moduleVersion",version,"name",name,"rules",rules,"rights","Synthetic original fixture","dependencies",dependencies,"content",Map.of("size",content.length,"sha256",ModuleArchive.hash(content)),"assets",List.of());
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){for(var e:Map.of("manifest.json",json.writeValueAsBytes(manifest),"content.json",content).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}return bytes.toByteArray();
    }
    byte[] archive()throws Exception{return archive("1.0.0","5R",List.of(),"Synthetic module");}
    HttpResponse<String> upload(Browser b,UUID c,String suffix,Object command,byte[] bytes,boolean csrf,String extra)throws Exception{
        String boundary="a01-"+UUID.randomUUID();var out=new ByteArrayOutputStream();
        if(command!=null)out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"command\"\r\nContent-Type: application/json\r\n\r\n"+json.writeValueAsString(command)+"\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"../../ignored.zip\"\r\nContent-Type: application/zip\r\n\r\n").getBytes());out.write(bytes);out.write("\r\n".getBytes());
        if(!extra.isEmpty())out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+extra+"\"\r\n\r\nextra\r\n").getBytes());
        out.write(("--"+boundary+"--\r\n").getBytes());var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+suffix)).header("Content-Type","multipart/form-data; boundary="+boundary);if(csrf)request.header("X-CSRF-TOKEN",b.csrf);
        return b.client.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> upload(Browser b,UUID c,Object command,byte[] bytes)throws Exception{return upload(b,c,"",command,bytes,true,"");}
    @AfterEach void cleanup(){for(UUID c:campaigns){jdbc.update("update campaign set current_scene_id=null where id=?",c);jdbc.update("delete from scene where campaign_id=?",c);jdbc.update("delete from campaign_member where campaign_id=?",c);jdbc.update("delete from campaign where id=?",c);}jdbc.update("delete from account_command_receipt where actor='a01_owner'");for(String name:users)jdbc.update("delete from app_user where username=?",name);}
    @Test void previewIsInertVersionedAndMakesExactMissingDependenciesVisible()throws Exception{
        var dm=dm();var c=campaign(dm);var player=member(c,"PLAYER");var catalog=dm.read("/api/campaigns/"+c+"/content").get("entries").get(0);String id=catalog.get("id").asString();var source=catalog.get("source");
        var deps=List.of(Map.of("id",id,"version",source.get("revision").asString(),"rules",source.get("rules").asString()),Map.of("id",module,"version","missing-2","rules","2014"));
        byte[] bytes=archive("1.0.0","5R",deps,"Secret module");var before=dm.read("/api/campaigns/"+c+"/scene");
        var preview=tree(upload(dm,c,"/preview",null,bytes,true,""),200);assertEquals(1,preview.get("missingDependencies").size());assertEquals(module,preview.get("missingDependencies").get(0).get("id").asString());assertEquals(1,preview.get("nodes").size());assertTrue(preview.get("existingId").isNull());
        assertEquals(0,dm.read(base(c)).get("campaignVersion").asLong());assertEquals(before,dm.read("/api/campaigns/"+c+"/scene"));assertEquals(0,dm.read(base(c)).get("packages").size());
        tree(upload(player,c,"/preview",null,bytes,true,""),403);var denied=player.get(base(c));tree(denied,403);assertEquals("no-store, private",denied.headers().firstValue("Cache-Control").orElse(""));tree(new Browser().get(base(c)),401);
        var wrong=archive("1.0.0","5R",List.of(Map.of("id",id,"version","wrong-revision","rules",source.get("rules").asString())),"Wrong version");assertEquals(1,tree(upload(dm,c,"/preview",null,wrong,true,""),200).get("missingDependencies").size());
    }
    @Test void importHasIndependentLibraryIdentityExactDuplicateAndExplicitCopyWithoutPlayableEffects()throws Exception{
        var dm=dm();var c=campaign(dm);byte[] bytes=archive();var body=command(0,false,false);var before=dm.read("/api/campaigns/"+c+"/scene");
        var ack=tree(upload(dm,c,body,bytes),200);assertEquals(1,ack.get("version").asLong());assertEquals(ack,tree(upload(dm,c,body,bytes),200));
        assertEquals(ack,tree(upload(dm,c,command(1,false,false),bytes),200));var copy=tree(upload(dm,c,command(1,true,false),bytes),200);assertNotEquals(ack.get("id"),copy.get("id"));assertEquals(2,copy.get("version").asLong());
        assertEquals(2,dm.read(base(c)).get("packages").size());assertEquals(before,dm.read("/api/campaigns/"+c+"/scene"));
        var download=dm.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+base(c)+"/"+ack.get("id").asString()+"/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,download.statusCode());assertArrayEquals(bytes,download.body());assertEquals("no-store, private",download.headers().firstValue("Cache-Control").orElse(""));
        tree(upload(dm,c,command(2,true,false),archive("1.0.0","5R",List.of(),"Changed same version")),409);
        var legacy=archive("2.0.0","2014",List.of(),"Legacy");tree(upload(dm,c,command(2,false,false),legacy),400);tree(upload(dm,c,command(2,false,true),legacy),200);
    }
    @Test void receiptRacesVersionRacesAndCurrentAuthorizationPrecedeHistoricalResults()throws Exception{
        var dm=dm();var c=campaign(dm);var other=member(c,"DM");byte[] bytes=archive();var body=command(0,false,false);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->upload(dm,c,body,bytes));var b=pool.submit(()->upload(dm,c,body,bytes));assertEquals(tree(a.get(),200),tree(b.get(),200));
            var first=pool.submit(()->upload(dm,c,command(1,true,false),bytes));var second=pool.submit(()->upload(other,c,command(1,true,false),bytes));
            assertEquals(List.of(200,409),java.util.stream.Stream.of(first.get().statusCode(),second.get().statusCode()).sorted().toList());
        }
        assertEquals(2,dm.read(base(c)).get("packages").size());
        var changed=new HashMap<>(body);changed.put("createCopy",true);tree(upload(dm,c,changed,bytes),409);
        var foreign=campaign(dm);tree(upload(dm,foreign,body,bytes),409);assertEquals(0,dm.read(base(foreign)).get("campaignVersion").asLong());
        var ack=tree(upload(other,c,command(2,false,false),bytes),200);String path=base(c)+"/"+ack.get("id").asString()+"/content";
        var replay=command(2,false,false);tree(upload(other,c,replay,bytes),200);jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username=?",c,other.username);
        tree(upload(other,c,replay,bytes),403);tree(other.get(path),403);tree(member(foreign,"DM").get(path),403);
        tree(dm.get(base(foreign)+"/"+ack.get("id").asString()+"/content"),404);
        assertFalse(dm.read("/api/campaigns/"+c+"/scene").toString().contains(module));
    }
    @Test void invalidMultipartCsrfStrictCommandAndCorruptArchiveHaveNoEffects()throws Exception{
        var dm=dm();var c=campaign(dm);byte[] bytes=archive();var body=command(0,false,false);
        tree(upload(dm,c,"",body,bytes,false,""),403);
        for(String extra:List.of("extra","command","file"))tree(upload(dm,c,"",body,bytes,true,extra),400);
        tree(upload(dm,c,"/preview",null,bytes,true,"file"),400);
        for(var patch:List.of(Map.of("actor",(Object)"forged"),Map.of("expectedVersion",(Object)"0"),Map.of("expectedVersion",(Object)0.5),Map.of("createCopy",(Object)"false"))){var malformed=new HashMap<>(body);malformed.putAll(patch);tree(upload(dm,c,malformed,bytes),400);}
        tree(upload(dm,c,body,new byte[]{80,75,0}),400);assertEquals(0,dm.read(base(c)).get("packages").size());assertEquals(0,dm.read(base(c)).get("campaignVersion").asLong());assertEquals(0,jdbc.queryForObject("select count(*) from module_command_receipt where campaign_id=?",Integer.class,c));
    }
    @Test void receiptInsertAndDeferredCommitFailureRollBackBytesVersionAndLibrary()throws Exception{
        var dm=dm();var c=campaign(dm);byte[] bytes=archive();var body=command(0,false,false);
        // Trigger applies only to this request's campaign, never another test's rows.
        String fn="a01_fail_"+c.toString().replace("-","");
        jdbc.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin if new.campaign_id='"+c+"'::uuid then raise exception 'injected module receipt failure'; end if; return new; end $$");
        try{
            jdbc.execute("create trigger "+fn+" before insert on module_command_receipt for each row execute function "+fn+"()");tree(upload(dm,c,body,bytes),500);
            assertEquals(0,dm.read(base(c)).get("campaignVersion").asLong());assertEquals(0,dm.read(base(c)).get("packages").size());
            jdbc.execute("drop trigger "+fn+" on module_command_receipt");
            jdbc.execute("create constraint trigger "+fn+" after insert on module_command_receipt deferrable initially deferred for each row execute function "+fn+"()");tree(upload(dm,c,body,bytes),500);
            assertEquals(0,dm.read(base(c)).get("campaignVersion").asLong());assertEquals(0,dm.read(base(c)).get("packages").size());assertEquals(0,jdbc.queryForObject("select count(*) from module_command_receipt where campaign_id=?",Integer.class,c));
        }finally{jdbc.execute("drop trigger if exists "+fn+" on module_command_receipt");jdbc.execute("drop function "+fn+"()");}
        tree(upload(dm,c,body,bytes),200);assertEquals(1,dm.read(base(c)).get("packages").size());
    }
}
