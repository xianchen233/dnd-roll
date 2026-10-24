package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ModuleMigrationIT {
    @Test void preservesAllV17ColumnsRowsAndChecksumsAndRejectsForeignPackageReceipts(){
        String schema="a01_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var props=new Properties();props.setProperty("currentSchema",schema);ds.setConnectionProperties(props);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("17").load().migrate();UUID c=UUID.randomUUID();jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_module_dm','hash')",UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Old campaign',7)",c);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_module_dm','DM')",c);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(17,checksums.size());assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version::int<=17 order by version"));
            UUID pkg=UUID.randomUUID(),foreign=UUID.randomUUID();jdbc.update("insert into campaign(id,name) values (?,'Foreign')",foreign);byte[] archive={80,75,3,4};jdbc.update("insert into module_package(id,campaign_id,module_id,module_version,sha256,manifest,archive) values (?,?,?,'1.0.0',?,'{}',?)",pkg,c,UUID.randomUUID(),"a".repeat(64),archive);
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into module_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values ('old_module_dm',?,?,?, ?,0)",UUID.randomUUID(),foreign,"b".repeat(64),pkg));
            assertArrayEquals(archive,jdbc.queryForObject("select archive from module_package where id=?",byte[].class,pkg));jdbc.update("delete from module_package where id=?",pkg);jdbc.update("delete from campaign where id=?",foreign);assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));
        }finally{if(!schema.matches("a01_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
