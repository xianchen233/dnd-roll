package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MovementMigrationIT {
    @Test void preservesEveryV24ColumnRowAndChecksumAndRejectsForeignBudgets(){
        String schema="a06_migration_"+UUID.randomUUID().toString().replace("-","");
        var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("24").load().migrate();
            UUID c=UUID.randomUUID(),character=UUID.randomUUID(),action=UUID.randomUUID(),roll=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'a06_old_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Existing campaign',9)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'a06_old_dm','DM')",c);jdbc.update("insert into character_card(id,campaign_id,sheet,version) values (?,?,?::jsonb,3)",character,c,"{\"notes\":\"OLD PRIVATE DATA\"}");jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,'a06_old_dm')",c,character);
            jdbc.update("insert into dice_roll(id,campaign_id,character_id,visibility,data) values (?,?,?,'DM','{\"raw\":20,\"at\":\"2026-10-25T00:00:00Z\"}')",roll,c,character);jdbc.update("insert into combat_action(id,campaign_id,character_id,target_id,visibility,data) values (?,?,?,?,'DM','{\"pending\":true,\"at\":\"2026-10-25T00:00:00Z\"}')",action,c,character,character);
            jdbc.update("insert into module_instance(campaign_id,source,document,mapping,progress) values (?,'{}','{}','{}','{\"played\":true}')",c);UUID operation=UUID.randomUUID();jdbc.update("insert into module_operation(campaign_id,id,definition) values (?,?,'{\"preserved\":true}')",c,operation);jdbc.update("insert into module_operation_execution(campaign_id,operation_id,id,at,evidence) values (?,?,?,now(),'{\"original\":true}')",c,operation,UUID.randomUUID());
            jdbc.update("insert into attack_preview(id,campaign_id,character_id,target_id,data) values (?,?,?,?,'{\"pending\":true,\"hash\":\"original\",\"dice\":[20,6]}')",UUID.randomUUID(),c,character,character);
            jdbc.update("insert into attack_preview_receipt(actor,request_id,campaign_id,fingerprint,result) values ('a06_old_dm',?,?,'original','{\"version\":7}')",UUID.randomUUID(),c);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(24,checksums.size());assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=24 order by version"));assertEquals(0,jdbc.queryForObject("select count(*) from movement_budget",Integer.class));assertEquals(0,jdbc.queryForObject("select count(*) from movement_journal",Integer.class));
            UUID foreign=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Foreign')",foreign);assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into movement_budget(campaign_id,character_id,data) values (?, ?, '{}')",foreign,character));
            jdbc.update("insert into attack_preview(id,campaign_id,character_id,target_id,data) values (?,?,?,?,'{}')",UUID.randomUUID(),c,character,character);
        }finally{if(!schema.matches("a06_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
