package io.dndroll.persistence;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
class StabilizationHealingMigrationIT {
 @Test void preservesAllV27RowsColumnsAndChecksumsAndReferencesExactHistoryVersion(){
  String schema="a17_migration_"+UUID.randomUUID().toString().replace("-","");
  var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var properties=new Properties();properties.setProperty("currentSchema",schema);ds.setConnectionProperties(properties);var jdbc=new JdbcTemplate(ds);
  try{
   Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("27").load().migrate();var c=UUID.randomUUID();var id=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_a17_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Existing',8)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_a17_dm','DM')",c);jdbc.update("insert into character_card(id,campaign_id,version,sheet) values (?,?,1,'{\"old\":true}')",id,c);
   jdbc.update("insert into stabilization_state(campaign_id,target_id,version,snapshot) values (?,?,2,'{\"oldAttempt\":true}')",c,id);jdbc.update("insert into stabilization_history(campaign_id,target_id,version,snapshot) values (?,?,2,'{\"oldAttempt\":true}')",c,id);jdbc.update("insert into stabilization_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values ('old_a17_dm',?,?,'old',?,2)",UUID.randomUUID(),c,id);
   var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");assertEquals(27,checksums.size());
   Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=27 order by version"));assertEquals(0,jdbc.queryForObject("select count(*) from stabilization_healing_history",Integer.class));
   assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into stabilization_healing_history(campaign_id,target_id,version,snapshot) values (?,?,3,'{}')",c,id));jdbc.update("insert into stabilization_healing_history(campaign_id,target_id,version,snapshot) values (?,?,2,'{}')",c,id);assertEquals(1,jdbc.queryForObject("select count(*) from stabilization_healing_history",Integer.class));
  }finally{if(!schema.matches("a17_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
 }
}
