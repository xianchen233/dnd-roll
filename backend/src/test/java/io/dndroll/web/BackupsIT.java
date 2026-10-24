package io.dndroll.web;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=", "BOOTSTRAP_USERNAME=p09_operator", "BOOTSTRAP_PASSWORD=integration-operator-password",
    "backups.operator=p09_operator", "backups.daily-enabled=false"})
@org.springframework.test.annotation.DirtiesContext
class BackupsIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired io.dndroll.backup.WriteAdmission admission;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    static final Path root;
    static { try { root=Files.createTempDirectory("p09-backups-it-"); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url",()->FirstSliceIT.required("TEST_DATABASE_URL"));
        p.add("spring.datasource.username",()->FirstSliceIT.required("TEST_DATABASE_USERNAME"));
        p.add("spring.datasource.password",()->FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
        p.add("backups.storage-root",()->root.toString());
    }
    final JsonMapper json=JsonMapper.builder().build();
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();String token;
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body,boolean csrf)throws Exception {
            var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");
            if(csrf)r.header("X-CSRF-TOKEN",token);
            return client.send(r.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        void login()throws Exception {login("p09_operator");}
        void login(String username)throws Exception {token=json.readTree(get("/api/auth/csrf").body()).get("token").asString();assertEquals(200,post("/api/auth/login",Map.of("username",username,"password","integration-operator-password"),true).statusCode());token=json.readTree(get("/api/auth/csrf").body()).get("token").asString();}
    }
    @Test void onlyExplicitDeploymentOperatorCanReadStatusAndCsrfRemainsRequired()throws Exception {
        var anon=new Browser();var denied=anon.get("/api/admin/backups");assertEquals(401,denied.statusCode());assertEquals("no-store, private",denied.headers().firstValue("Cache-Control").orElse(""));
        var operator=new Browser();operator.login();var status=operator.get("/api/admin/backups");assertEquals(200,status.statusCode(),status.body());
        var tree=json.readTree(status.body());assertTrue(tree.get("jobs").isArray());assertEquals(7,tree.get("retention").asInt());assertFalse(tree.get("paused").asBoolean());
        assertEquals(403,operator.post("/api/admin/backups",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0),false).statusCode());
        assertFalse(status.body().contains(root.toString()));assertFalse(status.body().contains("integration-operator-password"));
    }
    @Test void manualJobIsDurableIdempotentAndDownloadIsAValidatedDatabaseArchive()throws Exception {
        var b=new Browser();b.login();
        long version=json.readTree(b.get("/api/admin/backups").body()).get("version").asLong();
        var command=Map.of("requestId",UUID.randomUUID(),"expectedVersion",version);
        var response=b.post("/api/admin/backups",command,true);assertEquals(200,response.statusCode(),response.body());
        var ack=json.readTree(response.body());String id=ack.get("id").asString();
        assertEquals(ack,json.readTree(b.post("/api/admin/backups",command,true).body()));
        assertEquals(409,b.post("/api/admin/backups",Map.of("requestId",command.get("requestId"),"expectedVersion",version+1),true).statusCode());
        long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(30);String state="";
        while(System.nanoTime()<until){var status=json.readTree(b.get("/api/admin/backups").body());for(var job:status.get("jobs"))if(job.get("id").asString().equals(id))state=job.get("state").asString();if(Set.of("COMPLETE","FAILED").contains(state))break;Thread.sleep(50);}
        assertEquals("COMPLETE",state);
        var download=b.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/backups/"+id+"/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,download.statusCode());assertTrue(download.body().length>1000);
        Path saved=Files.write(root.resolve("download-"+id+".zip"),download.body());
        try(var zip=new java.util.zip.ZipFile(saved.toFile())) {assertNotNull(zip.getEntry("database.dump"));assertNotNull(zip.getEntry("manifest.json"));}
        String database="p09_restore_"+UUID.randomUUID().toString().replace("-","");
        String source=FirstSliceIT.required("TEST_DATABASE_URL"),target=source.substring(0,source.lastIndexOf('/')+1)+database;
        String username=FirstSliceIT.required("TEST_DATABASE_USERNAME"),password=FirstSliceIT.required("TEST_DATABASE_PASSWORD");
        jdbc.execute("create database \""+database+"\"");
        try {
            Path restored=root.resolve("restored-assets-"+id);
            String restoreClient=System.getenv().getOrDefault("BACKUP_PG_RESTORE","pg_restore");
            Path corrupt=Files.write(root.resolve("corrupt-"+id+".zip"),new byte[]{80,75,0});
            assertThrows(io.dndroll.backup.BackupArchive.InvalidArchive.class,()->io.dndroll.backup.RestoreCommand.restore(corrupt,target,username,password,restored,restoreClient));
            assertFalse(Files.exists(restored),"corrupt material cannot publish an asset root");
            io.dndroll.backup.RestoreCommand.restore(saved,target,username,password,restored,restoreClient);
            assertTrue(Files.isDirectory(restored));
            try(var connection=java.sql.DriverManager.getConnection(target,username,password);var statement=connection.createStatement()){
                try(var rows=statement.executeQuery("select count(*) from app_user where username='p09_operator'")){rows.next();assertEquals(1,rows.getInt(1));}
            }
            var occupied=assertThrows(java.io.IOException.class,()->io.dndroll.backup.RestoreCommand.restore(saved,target,username,password,root.resolve("another-root"),restoreClient));
            assertEquals("Target database must be empty and isolated",occupied.getMessage());
            var existingRoot=assertThrows(java.io.IOException.class,()->io.dndroll.backup.RestoreCommand.restore(saved,target,username,password,restored,restoreClient));
            assertEquals("Target asset root must be new",existingRoot.getMessage());
        } finally {jdbc.execute("drop database \""+database+"\"");}
        assertEquals(ack,json.readTree(b.post("/api/admin/backups",command,true).body()),"completed request never creates another archive");
    }
    @Test void pauseRejectsWritesAfterSecurityAndAllowsAuthorizedReads()throws Exception {
        var b=new Browser();b.login();
        try(var pause=admission.pause(java.time.Duration.ZERO)) {
            var r=b.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","must not create"),true);
            assertEquals(503,r.statusCode());assertTrue(r.body().contains("WRITE_PAUSED"));assertEquals("5",r.headers().firstValue("Retry-After").orElse(""));
            assertEquals(200,b.get("/api/auth/me").statusCode());
            assertEquals(403,new Browser().post("/api/campaigns",Map.of(),false).statusCode(),"CSRF denial precedes admission");
            var fresh=new Browser();fresh.login();assertEquals(200,fresh.get("/api/admin/backups").statusCode(),"operator can re-login to inspect/download while source writes stay stopped");
        }
        assertFalse(admission.paused());
    }
    @Test void owningACampaignNeverGrantsGlobalBackupStatusCreationOrDownload()throws Exception {
        String username="p09_dm_"+UUID.randomUUID().toString().substring(0,8);UUID campaign=null;
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)",UUID.randomUUID(),username,passwords.encode("integration-operator-password"));
        try {
            var b=new Browser();b.login(username);
            var created=b.post("/api/campaigns",Map.of("requestId",UUID.randomUUID(),"name","ZZZ Other DM"),true);assertEquals(201,created.statusCode());campaign=UUID.fromString(json.readTree(created.body()).get("id").asString());
            assertEquals(403,b.get("/api/admin/backups").statusCode());
            assertEquals(403,b.post("/api/admin/backups",Map.of("requestId",UUID.randomUUID(),"expectedVersion",0),true).statusCode());
            assertEquals(403,b.get("/api/admin/backups/"+UUID.randomUUID()+"/content").statusCode());
        } finally {
            if(campaign!=null){jdbc.update("update campaign set current_scene_id=null where id=?",campaign);jdbc.update("delete from scene where campaign_id=?",campaign);jdbc.update("delete from campaign_member where campaign_id=?",campaign);jdbc.update("delete from campaign where id=?",campaign);}
            jdbc.update("delete from account_command_receipt where actor=?",username);jdbc.update("delete from app_user where username=?",username);
        }
    }
}
