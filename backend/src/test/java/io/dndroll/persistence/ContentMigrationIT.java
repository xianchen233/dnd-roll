package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ContentMigrationIT {
    @Test void v8PreservesEveryV7TableColumnRowAndChecksum(){
        String schema="p04_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var p=new Properties();p.setProperty("currentSchema",schema);ds.setConnectionProperties(p);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("7").load().migrate();
            UUID c=UUID.randomUUID(),character=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Existing',9)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_dm','DM')",c);jdbc.update("insert into character_card(id,campaign_id,sheet,version) values (?,?,?::jsonb,3)",character,c,"{\"notes\":\"OLD PRIVATE DATA\"}");jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,'old_dm')",c,character);jdbc.update("insert into character_history(character_id,version,actor,kind,reason,at,after_state) values (?,3,'old_dm','EDIT','existing',CURRENT_TIMESTAMP,'{}')",character);jdbc.update("insert into character_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values ('old_dm',?,?,'old',?,3)",UUID.randomUUID(),c,character);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));
            var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("8").load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version<>'8' and version is not null order by version"));assertEquals(0,jdbc.queryForObject("select count(*) from campaign_content_entry",Integer.class));assertEquals(0,jdbc.queryForObject("select count(*) from campaign_content_settings",Integer.class));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into campaign_content_settings(campaign_id,mode,approved_ids) values (?,'UNKNOWN','[]')",c));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into campaign_content_entry(id,campaign_id,data) values (?,?,'{}')",UUID.randomUUID(),UUID.randomUUID()));
        }finally{if(!schema.matches("p04_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
