package io.dndroll.persistence;

import io.dndroll.application.AssetService.*;
import io.dndroll.application.AssetService;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcAssetStore implements AssetService.Store {
    private final JdbcTemplate jdbc;
    private final JdbcSceneStore scenes;
    private final JsonMapper json;
    public JdbcAssetStore(JdbcTemplate jdbc, JdbcSceneStore scenes, JsonMapper json) { this.jdbc = jdbc; this.scenes = scenes; this.json = json; }
    public <T> T actorLocked(String actor, Supplier<T> work) { return scenes.actorLocked(actor, work); }
    public <T> T locked(UUID campaign, Supplier<T> work) { return scenes.locked(campaign, work); }
    public Role role(UUID campaign, String actor) { return scenes.role(campaign, actor); }
    private static Asset metadata(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Asset(rs.getObject("id", UUID.class), rs.getString("media_type"), rs.getLong("byte_size"), rs.getInt("width"), rs.getInt("height"));
    }
    public Listing assets(UUID campaign) {
        var entries = jdbc.query("select * from campaign_asset where campaign_id=? order by id", (rs, n) -> metadata(rs), campaign);
        return new Listing(jdbc.queryForObject("select version from campaign where id=?", Long.class, campaign), entries);
    }
    public Stored asset(UUID campaign, UUID id) {
        var rows = jdbc.query("select * from campaign_asset where campaign_id=? and id=?", (rs, n) -> new Stored(metadata(rs), rs.getString("storage_key"), rs.getString("sha256")), campaign, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void insertAsset(UUID campaign, Stored stored) {
        var a = stored.asset();
        jdbc.update("insert into campaign_asset(id,campaign_id,storage_key,sha256,media_type,byte_size,width,height) values (?,?,?,?,?,?,?,?)",
            a.id(), campaign, stored.storageKey(), stored.sha256(), a.mediaType(), a.byteSize(), a.width(), a.height());
    }
    public long advanceCampaign(UUID campaign) { return scenes.advanceCampaign(campaign); }
    public MapResult map(UUID campaign, UUID scene) {
        var rows = jdbc.query("select s.id,s.version,m.asset_id,m.grid_size,m.offset_x,m.offset_y,m.distance_per_cell from scene s left join scene_map m on m.scene_id=s.id where s.campaign_id=? and s.id=?", (rs, n) -> {
            UUID asset = rs.getObject("asset_id", UUID.class);
            return new MapResult(scene, rs.getLong("version"), asset == null ? null : new MapConfig(asset, rs.getInt("grid_size"), rs.getInt("offset_x"), rs.getInt("offset_y"), rs.getDouble("distance_per_cell")));
        }, campaign, scene);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public MapResult setMap(UUID campaign, UUID scene, MapConfig map) {
        jdbc.update("delete from scene_revealed_cell where campaign_id=? and scene_id=?", campaign, scene);
        if (map == null) jdbc.update("delete from scene_map where campaign_id=? and scene_id=?", campaign, scene);
        else jdbc.update("insert into scene_map(campaign_id,scene_id,asset_id,grid_size,offset_x,offset_y,distance_per_cell) values (?,?,?,?,?,?,?) on conflict (scene_id) do update set asset_id=excluded.asset_id,grid_size=excluded.grid_size,offset_x=excluded.offset_x,offset_y=excluded.offset_y,distance_per_cell=excluded.distance_per_cell",
            campaign, scene, map.assetId(), map.gridSize(), map.offsetX(), map.offsetY(), map.distancePerCell());
        jdbc.update("update scene set version=version+1 where campaign_id=? and id=?", campaign, scene);
        return map(campaign, scene);
    }
    public Receipt assetReceipt(String actor, UUID request) {
        var rows = jdbc.query("select response_json from asset_command_receipt where actor=? and request_id=?", (rs, n) -> json.readValue(rs.getString(1), Receipt.class), actor, request);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void assetReceipt(String actor, UUID request, Receipt receipt) {
        String campaign = receipt.fingerprint().split("\\|", 3)[1];
        jdbc.update("insert into asset_command_receipt(actor,request_id,campaign_id,fingerprint,response_json) values (?,?,?,?,?)", actor, request, UUID.fromString(campaign), receipt.fingerprint(), json.writeValueAsString(receipt));
    }
}
