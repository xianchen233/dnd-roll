package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ModuleLifecycleMigrationIT {
    @Test void preservesEveryV18ColumnRowAndChecksumIncludingOriginalModuleReceipts(){
        String schema="a02_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("18").load().migrate();UUID c=UUID.randomUUID(),pkg=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_lifecycle_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Old template library',7)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_lifecycle_dm','DM')",c);byte[] bytes={80,75,3,4};jdbc.update("insert into module_package(id,campaign_id,module_id,module_version,sha256,manifest,archive) values (?,?,?,'1.0.0',?,'{}',?)",pkg,c,UUID.randomUUID(),"a".repeat(64),bytes);jdbc.update("insert into module_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values ('old_lifecycle_dm',?,?,?, ?,7)",UUID.randomUUID(),c,"b".repeat(64),pkg);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(18,checksums.size());assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=18 order by version"));
            UUID foreign=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Foreign')",foreign);assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into module_customization(id,campaign_id,source_package_id,source_sha256,name,baseline,document) values (?,?,?,?,'forged','{}','{}')",UUID.randomUUID(),foreign,pkg,"a".repeat(64)));jdbc.update("delete from campaign where id=?",foreign);assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));
        }finally{if(!schema.matches("a02_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
