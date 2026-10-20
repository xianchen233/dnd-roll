package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TimelineMigrationIT {
    @Test void preservesEveryV12ColumnRowChecksumAndBackfillsSourcesWithSameCampaignIntegrity(){
        String schema="p07_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("12").load().migrate();UUID c=UUID.randomUUID(),character=UUID.randomUUID(),roll=UUID.randomUUID(),legacy=UUID.randomUUID(),action=UUID.randomUUID();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_rp_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Old campaign',7)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_rp_dm','DM')",c);jdbc.update("insert into character_card(id,campaign_id,sheet) values (?,?,?::jsonb)",character,c,"{\"name\":\"Old character\",\"notes\":\"PRIVATE\"}");
            jdbc.update("insert into dice_roll(id,campaign_id,character_id,visibility,data) values (?,?,?,'DM','{\"at\":\"2026-10-20T00:00:00Z\",\"raw\":17}')",roll,c,character);jdbc.update("insert into dice_roll(id,campaign_id,visibility,data) values (?,?,'PUBLIC','{\"raw\":9}')",legacy,c);jdbc.update("insert into combat_action(id,campaign_id,character_id,target_id,visibility,data) values (?,?,?,?,'PUBLIC','{\"at\":\"2026-10-20T01:00:00Z\"}')",action,c,character,character);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=12 order by version"));assertEquals(12,checksums.size());assertEquals(3,jdbc.queryForObject("select count(*) from timeline_entry",Integer.class));assertEquals(java.time.Instant.EPOCH,jdbc.queryForObject("select at from timeline_entry where id=?",java.sql.Timestamp.class,legacy).toInstant());
            UUID foreign=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Foreign')",foreign);assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into timeline_entry(id,campaign_id,kind,roll_id,at) values (?,?,'DICE',?,now())",UUID.randomUUID(),foreign,roll));
            UUID session=UUID.randomUUID(),next=UUID.randomUUID();jdbc.update("insert into timeline_session(id,campaign_id,name) values (?,?,'New session')",session,c);jdbc.update("insert into dice_roll(id,campaign_id,visibility,data) values (?,?,'PUBLIC','{\"at\":\"2026-10-21T00:00:00Z\"}')",next,c);assertEquals(session,jdbc.queryForObject("select session_id from timeline_entry where id=?",UUID.class,next));assertEquals(4,jdbc.queryForObject("select count(*) from timeline_entry",Integer.class));
        }finally{if(!schema.matches("p07_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
