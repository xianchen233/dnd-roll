package io.dndroll.persistence;

import io.dndroll.application.MapVisibilityService;
import io.dndroll.application.MapVisibilityService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcMapVisibilityStore implements MapVisibilityService.Store {
    private final JdbcTemplate jdbc;private final JdbcSceneStore scenes;private final JdbcAssetStore assets;private final PrivateAssetFiles files;private final JsonMapper json;
    public JdbcMapVisibilityStore(JdbcTemplate jdbc,JdbcSceneStore scenes,JdbcAssetStore assets,PrivateAssetFiles files,JsonMapper json){this.jdbc=jdbc;this.scenes=scenes;this.assets=assets;this.files=files;this.json=json;}
    public <T>T actorLocked(String actor,Supplier<T> work){return scenes.actorLocked(actor,work);}
    public <T>T locked(UUID campaign,Supplier<T> work){return scenes.locked(campaign,work);}
    public Role role(UUID campaign,String actor){return scenes.role(campaign,actor);}
    public Scene current(UUID campaign){var ids=jdbc.queryForList("select current_scene_id from campaign where id=?",UUID.class,campaign);return ids.isEmpty()||ids.getFirst()==null?null:scene(campaign,ids.getFirst());}
    public Scene scene(UUID campaign,UUID id){
        var rows=jdbc.query("select width,height,grid_size,version from scene where campaign_id=? and id=?",(rs,n)->new Scene(id,rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getLong(4),null,List.of()),campaign,id);
        if(rows.isEmpty())return null;var scene=rows.getFirst();
        var fog=jdbc.query("select x,y from scene_revealed_cell where campaign_id=? and scene_id=? order by y,x",(rs,n)->new Cell(rs.getInt(1),rs.getInt(2)),campaign,id);
        return new Scene(id,scene.width(),scene.height(),scene.gridSize(),scene.version(),assets.map(campaign,id).map(),fog);
    }
    public byte[] original(UUID campaign,UUID id){var asset=assets.asset(campaign,id);if(asset==null)throw new DomainException("ASSET_UNAVAILABLE",503,"Asset is unavailable");return files.read(asset.storageKey());}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select payload from fog_command_receipt where actor=? and request_id=?",(rs,n)->json.readValue(rs.getString(1),Receipt.class),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(String actor,UUID request,Receipt receipt){jdbc.update("insert into fog_command_receipt(actor,request_id,campaign_id,payload) values (?,?,?,?)",actor,request,receipt.intent().campaignId(),json.writeValueAsString(receipt));}
    public long replace(UUID campaign,UUID scene,List<Cell> cells){
        jdbc.update("delete from scene_revealed_cell where campaign_id=? and scene_id=?",campaign,scene);
        jdbc.batchUpdate("insert into scene_revealed_cell(campaign_id,scene_id,x,y) values (?,?,?,?)",cells,cells.isEmpty()?1:cells.size(),(ps,c)->{ps.setObject(1,campaign);ps.setObject(2,scene);ps.setInt(3,c.x());ps.setInt(4,c.y());});
        jdbc.update("update scene set version=version+1 where campaign_id=? and id=?",campaign,scene);
        return jdbc.queryForObject("select version from scene where campaign_id=? and id=?",Long.class,campaign,scene);
    }
}
