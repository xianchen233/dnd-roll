package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TokenMigrationIT {
    @Test void v5PreservesEveryV4ColumnAndChecksumAndEnforcesSameCampaignMembership() {
        String schema="p02c_migration_"+UUID.randomUUID().toString().replace("-","");
        var source=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));
        var props=new Properties();props.setProperty("currentSchema",schema);source.setConnectionProperties(props);var jdbc=new JdbcTemplate(source);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("4").load().migrate();
            UUID campaign=UUID.randomUUID(),scene=UUID.randomUUID(),token=UUID.randomUUID(),other=UUID.randomUUID(),otherScene=UUID.randomUUID(),otherToken=UUID.randomUUID(),asset=UUID.randomUUID();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'legacy_dm','hash'),(?,'legacy_player','hash')",UUID.randomUUID(),UUID.randomUUID());
            jdbc.update("insert into campaign(id,name,version) values (?,'Legacy',8),(?,'Other',0)",campaign,other);
            jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'legacy_dm','DM'),(?,'legacy_player','PLAYER'),(?,'legacy_dm','DM')",campaign,campaign,other);
            jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size,version) values (?,?,'Legacy',30,20,64,12),(?,?,'Other',24,16,48,0)",scene,campaign,otherScene,other);
            jdbc.update("update campaign set current_scene_id=? where id=?",scene,campaign);
            jdbc.update("insert into token(id,scene_id,name,x,y,version,hidden) values (?,?,'Old hidden',12,8,5,true),(?,?,'Other',0,0,0,false)",token,scene,otherToken,otherScene);
            jdbc.update("insert into token_controller(token_id,username) values (?,'legacy_dm'),(?,'legacy_player')",token,token);
            jdbc.update("insert into movement_receipt(campaign_id,actor,request_id,token_id,expected_version,requested_x,requested_y,result_name,result_x,result_y,result_version,result_can_move) values (?,'legacy_player',?,?,4,12,8,'Old hidden',12,8,5,true)",campaign,UUID.randomUUID(),token);
            jdbc.update("insert into account_command_receipt(actor,request_id,kind,target,expected_version,name,result_id,result_version) values ('legacy_dm',?,'CREATE','',0,'Legacy',?,0)",UUID.randomUUID(),campaign);
            jdbc.update("insert into campaign_invitation(id,campaign_id,token_hash,created_by,expires_at) values (?,?,'legacy-hash','legacy_dm',CURRENT_TIMESTAMP+interval '7 days')",UUID.randomUUID(),campaign);
            jdbc.update("insert into scene_command_receipt(actor,request_id,kind,campaign_id,target,name,expected_version,result_id,result_name,result_version) values ('legacy_dm',?,'ACTIVATE',?,?,'',7,?,'Legacy',8)",UUID.randomUUID(),campaign,scene,scene);
            jdbc.update("insert into campaign_asset(id,campaign_id,storage_key,sha256,media_type,byte_size,width,height) values (?,?,?,?,'image/png',100,10,10)",asset,campaign,UUID.randomUUID().toString(),"a".repeat(64));
            jdbc.update("insert into scene_map(campaign_id,scene_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,48,1,2,5)",campaign,scene,asset);
            jdbc.update("insert into asset_command_receipt(actor,request_id,campaign_id,fingerprint,response_json) values ('legacy_dm',?,?,'legacy','{}')",UUID.randomUUID(),campaign);
            var columns=new LinkedHashMap<String,List<String>>();
            for(String table:List.of("app_user","campaign","campaign_member","scene","token","token_controller","movement_receipt","account_command_receipt","campaign_invitation","scene_command_receipt","campaign_asset","scene_map","asset_command_receipt"))
                columns.put(table,jdbc.queryForList("select column_name from information_schema.columns where table_schema=? and table_name=? order by ordinal_position",String.class,schema,table));
            var before=snapshot(jdbc,columns);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("5").load().migrate();
            assertEquals(before,snapshot(jdbc,columns));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version in ('1','2','3','4') order by version"));
            assertEquals(5,jdbc.queryForObject("select count(*) from flyway_schema_history where success and version is not null",Integer.class));
            assertEquals(List.of(campaign,campaign),jdbc.queryForList("select campaign_id from token_controller order by username",UUID.class));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into token_controller(token_id,username,campaign_id) values (?,'legacy_player',?)",otherToken,other));
            // Explicit wrong ownership cannot be rewritten by the backward-compatible default triggers.
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into token_controller(token_id,username,campaign_id) values (?,'legacy_dm',?)",otherToken,campaign));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y) values (?,?,?,'Foreign',0,0)",UUID.randomUUID(),otherScene,campaign));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from campaign_member where campaign_id=? and username='legacy_player'",campaign));
            jdbc.update("insert into token_controller(token_id,username) values (?,'legacy_dm')",otherToken);
            assertEquals(other,jdbc.queryForObject("select campaign_id from token_controller where token_id=?",UUID.class,otherToken));
        } finally {
            if(!schema.matches("p02c_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");
        }
    }
    static Map<String,Object> snapshot(JdbcTemplate jdbc,Map<String,List<String>> columns){
        var rows=new LinkedHashMap<String,Object>();
        columns.forEach((table,names)->rows.put(table,jdbc.queryForList("select row_to_json(t)::text from (select "+String.join(",",names)+" from "+table+") t order by 1",String.class)));
        return rows;
    }
}
