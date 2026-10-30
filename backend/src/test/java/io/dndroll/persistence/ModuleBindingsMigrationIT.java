package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ModuleBindingsMigrationIT {
 @Test void preservesAllV25ColumnsRowsChecksumsAndScopesBindingEvidenceToItsCampaign(){
  String schema="a03h_bind_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
  try{
   Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("25").load().migrate();UUID c=UUID.randomUUID(),op=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_binding_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Existing live campaign',7)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_binding_dm','DM')",c);jdbc.update("insert into module_instance(campaign_id,source,document,mapping,progress) values (?,'{}','{}','{}','{\"chapter\":\"played\"}')",c);jdbc.update("insert into module_operation(campaign_id,id,definition) values (?,?, '{\"version\":9,\"name\":\"local definition\"}')",c,op);jdbc.update("insert into module_operation_execution(campaign_id,operation_id,id,at,evidence) values (?,?,?,now(),'{\"reason\":\"immutable old execution\"}')",c,op,UUID.randomUUID());
   var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
   Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(25,checksums.size());assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=25 order by version"));
   UUID node=UUID.randomUUID(),other=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Other target')",other);jdbc.update("insert into module_instance(campaign_id,source,document,mapping,progress) values (?,'{}','{}','{}','{}')",other);jdbc.update("insert into module_operation_binding(campaign_id,node_id,operation_id,data) values (?,?,?,'{}')",c,node,UUID.randomUUID());
   assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into module_operation_binding_history(campaign_id,node_id,version,evidence) values (?,?,1,'{}')",other,node));
   assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into module_operation_binding_receipt(campaign_id,node_id,actor,request_id,fingerprint,result) values (?,?,'old_binding_dm',?,?,'{}')",other,node,UUID.randomUUID(),"a".repeat(64)));
   assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into module_operation_binding(campaign_id,node_id,operation_id,data) values (?,?,?,'{}')",UUID.randomUUID(),node,UUID.randomUUID()));
   jdbc.update("delete from campaign where id=?",other);assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));
  }finally{if(!schema.matches("a03h_bind_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
 }
}
