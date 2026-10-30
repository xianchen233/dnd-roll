package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CharacterMigrationIT {
    @Test void v7PreservesEveryExistingColumnRowAndChecksumAndEnforcesCampaignReferences(){
        String schema="p03_migration_"+UUID.randomUUID().toString().replace("-","");var ds=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));var p=new Properties();p.setProperty("currentSchema",schema);ds.setConnectionProperties(p);var jdbc=new JdbcTemplate(ds);
        try{
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("6").load().migrate();
            UUID c=UUID.randomUUID(),other=UUID.randomUUID(),scene=UUID.randomUUID(),otherScene=UUID.randomUUID(),token=UUID.randomUUID(),otherToken=UUID.randomUUID(),character=UUID.randomUUID();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_dm','hash'),(?,'old_player','hash')",UUID.randomUUID(),UUID.randomUUID());jdbc.update("insert into campaign(id,name,version) values (?,'Old',7),(?,'Other',2)",c,other);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_dm','DM'),(?,'old_player','PLAYER'),(?,'old_dm','DM')",c,c,other);
            jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size,version) values (?,?,'Old',24,16,48,9),(?,?,'Other',24,16,48,0)",scene,c,otherScene,other);jdbc.update("update campaign set current_scene_id=? where id=?",scene,c);jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,version,hidden) values (?,?,?,'Legacy hidden',3,4,8,true),(?,?,?,'Other',0,0,0,false)",token,scene,c,otherToken,otherScene,other);jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,'old_player')",token,c);
            jdbc.update("insert into scene_revealed_cell(campaign_id,scene_id,x,y) values (?,?,1,2)",c,scene);jdbc.update("insert into fog_command_receipt(actor,request_id,campaign_id,payload) values ('old_dm',?,?,'{}')",UUID.randomUUID(),c);
            var columns=new LinkedHashMap<String,List<String>>();for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname=? and tablename<>'flyway_schema_history' order by tablename",String.class,schema))columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));
            var before=FogMigrationIT.snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("7").load().migrate();assertEquals(before,FogMigrationIT.snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version in ('1','2','3','4','5','6') order by version"));assertEquals(0,jdbc.queryForObject("select count(*) from character_card",Integer.class));assertNull(jdbc.queryForObject("select character_id from token where id=?",UUID.class,token));
            jdbc.update("insert into character_card(id,campaign_id,sheet) values (?,?,?::jsonb)",character,c,"{}");jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,'old_player')",c,character);
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,'old_player')",other,character));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update token set character_id=? where id=?",character,otherToken));jdbc.update("update token set character_id=? where id=?",character,token);assertEquals(character,jdbc.queryForObject("select character_id from token where id=?",UUID.class,token));
        }finally{if(!schema.matches("p03_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
}
