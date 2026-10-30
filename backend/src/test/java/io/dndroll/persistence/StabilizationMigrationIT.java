package io.dndroll.persistence;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StabilizationMigrationIT {
 @Test void preservesEveryV26ColumnRowAndMigrationChecksum(){
  String schema="a16_migration_"+UUID.randomUUID().toString().replace("-","");
  var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var properties=new Properties();properties.setProperty("currentSchema",schema);ds.setConnectionProperties(properties);var jdbc=new JdbcTemplate(ds);
  try{
   Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("26").load().migrate();var c=UUID.randomUUID();var id=UUID.randomUUID();
   jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_stabilization_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Existing campaign',8)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_stabilization_dm','DM')",c);
   jdbc.update("insert into character_card(id,campaign_id,version,sheet) values (?,?,7,'{\"notes\":\"existing zero HP record\",\"combat\":{\"hp\":0,\"deathSuccesses\":1,\"deathFailures\":2}}')",id,c);
   var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));
   var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");assertEquals(26,checksums.size());
   Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=26 order by version"));
   assertEquals(3,jdbc.queryForObject("select count(*) from information_schema.tables where table_schema=? and table_name in ('stabilization_state','stabilization_history','stabilization_receipt')",Integer.class,schema));
   assertEquals(0,jdbc.queryForObject("select count(*) from stabilization_state",Integer.class));
   assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into stabilization_history(campaign_id,target_id,version,snapshot) values (?,?,1,'{}')",c,id));
  }finally{if(!schema.matches("a16_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
 }
}
