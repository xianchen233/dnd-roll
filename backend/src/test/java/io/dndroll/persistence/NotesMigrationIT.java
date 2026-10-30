package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class NotesMigrationIT {
    @Test void preservesEveryV15ColumnRowChecksumAndEnforcesCampaignVisibilityReferences(){
        String schema="p08_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("15").load().migrate();UUID c=UUID.randomUUID(),message=UUID.randomUUID(),image=UUID.randomUUID();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_note_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Old campaign',7)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_note_dm','DM')",c);
            jdbc.update("insert into rp_message(id,campaign_id,kind,data) values (?,?,'OOC','{\"text\":\"Old RP\"}')",message,c);jdbc.update("insert into timeline_entry(id,campaign_id,kind,message_id,at) values (?,?,'OOC',?,now())",message,c,message);jdbc.update("insert into rp_message_history(message_id,version,data) values (?,0,'{\"text\":\"Old RP\"}')",message);
            jdbc.update("insert into rp_attachment(id,campaign_id,actor,storage_key,sha256,data) values (?,?,'old_note_dm',?,?,'{}')",image,c,UUID.randomUUID(),"0".repeat(64));jdbc.update("insert into rp_attachment_reference(campaign_id,message_id,version,attachment_id) values (?,?,0,?)",c,message,image);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=15 order by version"));assertEquals(15,checksums.size());
            UUID note=UUID.randomUUID(),privateImage=UUID.randomUUID(),foreign=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Foreign')",foreign);jdbc.update("insert into campaign_note(id,campaign_id,visibility,data) values (?,?,'TEAM','{\"visibility\":\"TEAM\"}')",note,c);jdbc.update("insert into note_history(note_id,version,data) values (?,0,'{}')",note);jdbc.update("insert into note_attachment(id,campaign_id,actor,visibility,storage_key,sha256,data) values (?,?,'old_note_dm','DM',?,?,'{}')",privateImage,c,UUID.randomUUID(),"1".repeat(64));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into note_attachment_reference(campaign_id,note_id,version,attachment_id,visibility) values (?,?,0,?,'TEAM')",c,note,privateImage));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into note_attachment_reference(campaign_id,note_id,version,attachment_id,visibility) values (?,?,0,?,'DM')",foreign,note,privateImage));
            UUID shared=UUID.randomUUID();jdbc.update("insert into note_attachment(id,campaign_id,actor,visibility,storage_key,sha256,data) values (?,?,'old_note_dm','TEAM',?,?,'{}')",shared,c,UUID.randomUUID(),"2".repeat(64));jdbc.update("insert into note_attachment_reference(campaign_id,note_id,version,attachment_id,visibility) values (?,?,0,?,'TEAM')",c,note,shared);assertEquals(1,jdbc.queryForObject("select count(*) from note_attachment_reference",Integer.class));jdbc.update("delete from campaign where id=?",foreign);assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));
        }finally{if(!schema.matches("p08_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
