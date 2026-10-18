package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AssetMigrationIT {
    @Test void unchangedV3RowsAndChecksumsSurviveV4AndBothMapReferencesEnforceCampaignOwnership() {
        String schema="p02b_migration_"+UUID.randomUUID().toString().replace("-","");
        var source=new DriverManagerDataSource(SceneMigrationIT.required("TEST_DATABASE_URL"),SceneMigrationIT.required("TEST_DATABASE_USERNAME"),SceneMigrationIT.required("TEST_DATABASE_PASSWORD"));
        var props=new Properties();props.setProperty("currentSchema",schema);source.setConnectionProperties(props);var jdbc=new JdbcTemplate(source);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("3").load().migrate();
            UUID campaign=UUID.randomUUID(),scene=UUID.randomUUID(),token=UUID.randomUUID(),foreign=UUID.randomUUID(),foreignScene=UUID.randomUUID(),asset=UUID.randomUUID();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'legacy_dm','legacy_hash')",UUID.randomUUID());
            jdbc.update("insert into campaign(id,name,version) values (?,'Legacy',8),(?,'Other',0)",campaign,foreign);
            jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'legacy_dm','DM')",campaign);
            jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size,version) values (?,?,'Kept',30,20,64,12),(?,?,'Other',24,16,48,0)",scene,campaign,foreignScene,foreign);
            jdbc.update("update campaign set current_scene_id=? where id=?",scene,campaign);
            jdbc.update("insert into token(id,scene_id,name,x,y,version,hidden) values (?,?,'Hidden old',12,8,5,true)",token,scene);
            jdbc.update("insert into token_controller(token_id,username) values (?,'legacy_dm')",token);
            jdbc.update("insert into movement_receipt(campaign_id,actor,request_id,token_id,expected_version,requested_x,requested_y,result_name,result_x,result_y,result_version,result_can_move) values (?,'legacy_dm',?,?,4,12,8,'Hidden old',12,8,5,true)",campaign,UUID.randomUUID(),token);
            jdbc.update("insert into account_command_receipt(actor,request_id,kind,target,expected_version,name,result_id,result_version) values ('legacy_dm',?,'CREATE','',0,'Legacy',?,0)",UUID.randomUUID(),campaign);
            jdbc.update("insert into scene_command_receipt(actor,request_id,kind,campaign_id,target,name,expected_version,result_id,result_name,result_version) values ('legacy_dm',?,'ACTIVATE',?,?,'',7,?,'Kept',8)",UUID.randomUUID(),campaign,scene,scene);
            var before=preserved(jdbc);var checksums=jdbc.queryForList("select version,checksum from flyway_schema_history where version is not null order by version");
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("4").load().migrate();
            assertEquals(before,preserved(jdbc));assertEquals(checksums,jdbc.queryForList("select version,checksum from flyway_schema_history where version in ('1','2','3') order by version"));
            assertEquals(4,jdbc.queryForObject("select count(*) from flyway_schema_history where success and version is not null",Integer.class));
            jdbc.update("insert into campaign_asset(id,campaign_id,storage_key,sha256,media_type,byte_size,width,height) values (?,?,?,?,'image/png',100,10,10)",asset,campaign,UUID.randomUUID().toString(),"a".repeat(64));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into scene_map(campaign_id,scene_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,48,0,0,5)",foreign,foreignScene,asset));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("insert into scene_map(campaign_id,scene_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,48,0,0,5)",campaign,foreignScene,asset));
            jdbc.update("insert into scene_map(campaign_id,scene_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,48,0,0,5)",campaign,scene,asset);
            assertEquals(1,jdbc.queryForObject("select count(*) from scene_map",Integer.class));assertEquals(before,preserved(jdbc));
        } finally {if(!schema.matches("p02b_migration_[a-f0-9]{32}"))throw new AssertionError("Unexpected schema");jdbc.execute("drop schema if exists "+schema+" cascade");}
    }
    static Map<String,Object> preserved(JdbcTemplate jdbc) {
        var result=new LinkedHashMap<String,Object>();
        for(var table:List.of("app_user","campaign","campaign_member","scene","token","token_controller","movement_receipt","account_command_receipt","campaign_invitation","scene_command_receipt"))
            result.put(table,jdbc.queryForList("select row_to_json(t)::text from "+table+" t order by 1",String.class));
        return result;
    }
}
