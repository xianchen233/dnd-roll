package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DiceMigrationIT {
    @Test void v9AndV10PreserveAllV8ColumnsRowsAndChecksums(){
        String schema="p05_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("8").load().migrate();
            UUID c=UUID.randomUUID(),character=UUID.randomUUID(),entry=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Existing',9)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_dm','DM')",c);jdbc.update("insert into character_card(id,campaign_id,sheet,version) values (?,?,?::jsonb,3)",character,c,"{\"notes\":\"PRIVATE OLD DATA\"}");jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,'old_dm')",c,character);jdbc.update("insert into campaign_content_entry(id,campaign_id,data) values (?,?,'{\"old\":true}')",entry,c);jdbc.update("insert into campaign_content_settings(campaign_id,mode,approved_ids) values (?,'REVIEW',?::jsonb)",c,"[\""+entry+"\"]");jdbc.update("insert into content_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values ('old_dm',?,?,'old',?,9)",UUID.randomUUID(),c,entry);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("10").load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=8 order by version"));assertEquals(23,columns.size());
            assertEquals(0,jdbc.queryForObject("select count(*) from dice_roll",Integer.class));assertEquals(0,jdbc.queryForObject("select count(*) from campaign_initiative",Integer.class));
            UUID foreign=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Foreign')",foreign);assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into dice_roll(id,campaign_id,character_id,visibility,data) values (?,?,?,'PUBLIC','{}')",UUID.randomUUID(),foreign,character));
        }finally{if(!schema.matches("p05_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
