package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** An isolated real PostgreSQL schema exercises exactly the released V2 -> V3 path. */
class SceneMigrationIT {
    @Test void v3BackfillsCurrentPointerAndPreservesEveryExistingSceneTokenControlAndReceipt() {
        String schema = "p02_migration_" + UUID.randomUUID().toString().replace("-", "");
        var source = new DriverManagerDataSource(required("TEST_DATABASE_URL"), required("TEST_DATABASE_USERNAME"), required("TEST_DATABASE_PASSWORD"));
        var props = new Properties(); props.setProperty("currentSchema", schema); source.setConnectionProperties(props);
        var jdbc = new JdbcTemplate(source);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("2").load().migrate();
            UUID campaign = UUID.randomUUID(), scene = UUID.randomUUID(), token = UUID.randomUUID(), request = UUID.randomUUID();
            jdbc.update("insert into app_user(id,username,password_hash) values (?,'old_dm','old_hash')", UUID.randomUUID());
            jdbc.update("insert into campaign(id,name,version) values (?,'Old campaign',7)", campaign);
            jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'old_dm','DM')", campaign);
            jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size,version) values (?,?,'Old map',30,20,64,11)", scene, campaign);
            jdbc.update("insert into token(id,scene_id,name,x,y,version,hidden) values (?,?,'Secret old enemy',12,8,5,true)", token, scene);
            jdbc.update("insert into token_controller(token_id,username) values (?,'old_dm')", token);
            jdbc.update("insert into movement_receipt(campaign_id,actor,request_id,token_id,expected_version,requested_x,requested_y,result_name,result_x,result_y,result_version,result_can_move) values (?,'old_dm',?,?,4,12,8,'Secret old enemy',12,8,5,true)", campaign, request, token);
            jdbc.update("insert into account_command_receipt(actor,request_id,kind,target,expected_version,name,result_id,result_version) values ('old_dm',?,'CREATE','',0,'Old campaign',?,0)", UUID.randomUUID(), campaign);
            var before = preserved(jdbc);
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
            assertEquals(before, preserved(jdbc));
            assertEquals(scene, jdbc.queryForObject("select current_scene_id from campaign where id=?", UUID.class, campaign));
            UUID second = UUID.randomUUID(); jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Second',24,16,48)", second, campaign);
            UUID foreignCampaign = UUID.randomUUID(), foreignScene = UUID.randomUUID();
            jdbc.update("insert into campaign(id,name) values (?,'Foreign')", foreignCampaign);
            jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'Foreign scene',24,16,48)", foreignScene, foreignCampaign);
            assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("update campaign set current_scene_id=? where id=?", foreignScene, campaign));
            assertEquals(scene, jdbc.queryForObject("select current_scene_id from campaign where id=?", UUID.class, campaign));
            assertEquals(3, jdbc.queryForObject("select count(*) from flyway_schema_history where success and version is not null", Integer.class));
        } finally {
            // Generated test schema contains no caller-supplied SQL identifier.
            if (!schema.matches("p02_migration_[a-f0-9]{32}")) throw new AssertionError("Unexpected schema");
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
    static Map<String, Object> preserved(JdbcTemplate jdbc) {
        var rows = new LinkedHashMap<String, Object>();
        for (var table : List.of("app_user", "campaign_member", "scene", "token", "token_controller", "movement_receipt", "account_command_receipt"))
            rows.put(table, jdbc.queryForList("select row_to_json(t)::text from " + table + " t order by 1", String.class));
        rows.put("campaign", jdbc.queryForList("select id,name,version from campaign order by id"));
        return rows;
    }
    static String required(String key) {
        var value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Real PostgreSQL integration requires " + key);
        return value;
    }
}
