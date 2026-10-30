package io.dndroll.backup;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;

class RecoveryAuditIT {
    @TempDir Path root;
    static String required(String key){var value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalStateException("Missing test database");return value;}
    class Fixture implements AutoCloseable {
        final String source=required("TEST_DATABASE_URL"),username=required("TEST_DATABASE_USERNAME"),password=required("TEST_DATABASE_PASSWORD"),name="a24_audit_"+UUID.randomUUID().toString().replace("-",""),url=source.substring(0,source.lastIndexOf('/')+1)+name;
        final Path assets=root.resolve("assets"),archive=root.resolve("backup.zip");final String key=UUID.randomUUID().toString();final JdbcTemplate jdbc;final UUID campaign=UUID.randomUUID();
        Fixture()throws Exception{
            try(var c=DriverManager.getConnection(source,username,password);var s=c.createStatement()){s.execute("create database \""+name+"\"");}
            var data=new DriverManagerDataSource(url,username,password);jdbc=new JdbcTemplate(data);Flyway.configure().dataSource(data).load().migrate();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'PRIVATE_AUDIT_ACTOR','PRIVATE_AUDIT_HASH')",UUID.randomUUID());jdbc.update("insert into campaign(id,name) values (?,'PRIVATE_AUDIT_CAMPAIGN')",campaign);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'PRIVATE_AUDIT_ACTOR','DM')",campaign);
            Files.createDirectory(assets);byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jJXkAAAAASUVORK5CYII=");Files.write(assets.resolve(key),png);String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png));
            jdbc.update("insert into campaign_asset(id,campaign_id,storage_key,sha256,media_type,byte_size,width,height) values (?,?,?,?,'image/png',?,1,1)",UUID.randomUUID(),campaign,key,hash,png.length);
            new PostgresBackup(data,assets.toString(),required("BACKUP_PG_DUMP"),url,username,password).create(Files.createDirectory(root.resolve("work")),archive);
        }
        RecoveryAuditCommand.Result audit(RecoveryInventory.Mode mode)throws Exception{return RecoveryAuditCommand.audit(archive,url,username,password,assets,mode);}
        Map<String,BackupArchive.Table> snapshot()throws Exception{try(var c=DriverManager.getConnection(url,username,password)){return PostgresBackup.tables(c);}}
        public void close()throws Exception{if(!name.matches("a24_audit_[a-f0-9]{32}"))throw new AssertionError();try(var c=DriverManager.getConnection(source,username,password);var s=c.createStatement()){s.execute("drop database \""+name+"\" with (force)");}}
    }
    @Test void verifiesAllTablesAndFilesReadOnlyButNeverClaimsCampaignRemap()throws Exception{
        try(var f=new Fixture()){var before=f.snapshot();var same=assertDoesNotThrow(()->f.audit(RecoveryInventory.Mode.SAME_ID));assertEquals("VERIFIED",same.status());assertEquals(76,same.inventory().tables().size());assertEquals(1,same.assetsVerified());var remap=assertDoesNotThrow(()->f.audit(RecoveryInventory.Mode.CAMPAIGN_REMAP));assertEquals("BLOCKED",remap.status());assertTrue(remap.inventory().blockers().contains("REMAP_WRITER_NOT_IMPLEMENTED"));assertEquals(before,f.snapshot());var text=JsonMapper.builder().build().writeValueAsString(same);for(String secret:List.of("PRIVATE_AUDIT",f.password,f.url,root.toString()))assertFalse(text.contains(secret));}
    }
    @Test void alteredRowsAndUnreviewedColumnsAreBlockedInsteadOfOmitted()throws Exception{
        try(var f=new Fixture()){assertDoesNotThrow(()->f.audit(RecoveryInventory.Mode.SAME_ID));f.jdbc.update("update campaign set name='Changed private evidence' where id=?",f.campaign);var changed=f.snapshot();var report=f.audit(RecoveryInventory.Mode.SAME_ID);assertEquals("BLOCKED",report.status());assertTrue(report.inventory().blockers().contains("ROW_DIGEST_MISMATCH:campaign"));assertEquals(changed,f.snapshot());f.jdbc.execute("alter table stabilization_state add column unknown_origin jsonb");report=f.audit(RecoveryInventory.Mode.SAME_ID);assertTrue(report.inventory().blockers().contains("SCHEMA_SIGNATURE_MISMATCH:stabilization_state"));}
    }
    @Test void changedMissingAssetsAndOverwrittenReportAreRejected()throws Exception{
        try(var f=new Fixture()){var report=assertDoesNotThrow(()->f.audit(RecoveryInventory.Mode.SAME_ID));var output=root.resolve("report.json");RecoveryAuditCommand.write(report,output);var bytes=Files.readAllBytes(output);assertThrows(FileAlreadyExistsException.class,()->RecoveryAuditCommand.write(report,output));assertArrayEquals(bytes,Files.readAllBytes(output));Files.write(f.assets.resolve(f.key),new byte[]{1,2,3});assertThrows(java.io.IOException.class,()->f.audit(RecoveryInventory.Mode.SAME_ID));Files.delete(f.assets.resolve(f.key));assertThrows(java.io.IOException.class,()->f.audit(RecoveryInventory.Mode.SAME_ID));}
    }
    @Test void auditsActualNativeRestoreButDoesNotAcceptAlteredRestoredConstraints()throws Exception{
        try(var f=new Fixture()){
            String name="a24_restore_audit_"+UUID.randomUUID().toString().replace("-",""),url=f.source.substring(0,f.source.lastIndexOf('/')+1)+name;Path assets=root.resolve("restored-assets");
            try(var c=DriverManager.getConnection(f.source,f.username,f.password);var s=c.createStatement()){s.execute("create database \""+name+"\"");}
            try{
                RestoreCommand.restore(f.archive,url,f.username,f.password,assets,required("BACKUP_PG_RESTORE"));
                var report=RecoveryAuditCommand.audit(f.archive,url,f.username,f.password,assets,RecoveryInventory.Mode.SAME_ID);assertEquals("VERIFIED",report.status(),report.inventory().blockers().toString());
                var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,f.username,f.password));jdbc.execute("alter table campaign_member drop constraint campaign_member_role_check");jdbc.execute("alter table campaign_member add constraint campaign_member_role_check check (role in ('DM','PLAYER','ADMIN'))");
                report=RecoveryAuditCommand.audit(f.archive,url,f.username,f.password,assets,RecoveryInventory.Mode.SAME_ID);assertTrue(report.inventory().blockers().contains("SCHEMA_SIGNATURE_MISMATCH:campaign_member"));
            }finally{
                if(!name.matches("a24_restore_audit_[a-f0-9]{32}"))throw new AssertionError();try(var c=DriverManager.getConnection(f.source,f.username,f.password);var s=c.createStatement()){s.execute("drop database \""+name+"\" with (force)");}
            }
        }
    }
}
