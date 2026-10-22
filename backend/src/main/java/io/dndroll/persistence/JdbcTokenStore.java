package io.dndroll.persistence;

import io.dndroll.application.TokenService.*;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcTokenStore implements Store {
    private final JdbcTemplate jdbc;
    private final JdbcSceneStore scenes;
    private final JsonMapper json;
    public JdbcTokenStore(JdbcTemplate jdbc, JdbcSceneStore scenes, JsonMapper json) { this.jdbc = jdbc; this.scenes = scenes; this.json = json; }
    public <T> T actorLocked(String actor, Supplier<T> work) { return scenes.actorLocked(actor, work); }
    public <T> T locked(UUID campaign, Supplier<T> work) { return scenes.locked(campaign, work); }
    public Role role(UUID campaign, String actor) { return scenes.role(campaign, actor); }
    public Scene scene(UUID campaign, UUID scene) {
        var rows = jdbc.query("select width,height,version from scene where campaign_id=? and id=? and not module_retired", (rs,n) -> new Scene(scene,rs.getInt(1),rs.getInt(2),rs.getLong(3),List.of()),campaign,scene);
        if (rows.isEmpty()) return null;
        Map<UUID,List<String>> controls = new HashMap<>();
        jdbc.query("select token_id,username from token_controller where campaign_id=? and token_id in (select id from token where scene_id=?) order by username", rs -> {
            controls.computeIfAbsent(rs.getObject(1,UUID.class),ignored -> new ArrayList<>()).add(rs.getString(2));
        }, campaign, scene);
        var tokens = jdbc.query("select * from token where scene_id=? and not module_retired order by id", (rs,n) -> {
            UUID id = rs.getObject("id",UUID.class);
            return new Token(id,rs.getString("name"),rs.getInt("x"),rs.getInt("y"),rs.getLong("version"),rs.getBoolean("hidden"),controls.getOrDefault(id,List.of()).stream().sorted().toList());
        },scene);
        var s = rows.getFirst(); return new Scene(scene,s.width(),s.height(),s.version(),tokens);
    }
    public Receipt receipt(String actor, UUID request) {
        var rows = jdbc.query("select payload from token_command_receipt where actor=? and request_id=?",(rs,n) -> json.readValue(rs.getString(1),Receipt.class),actor,request);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void receipt(String actor, UUID request, Receipt receipt) {
        jdbc.update("insert into token_command_receipt(actor,request_id,campaign_id,payload) values (?,?,?,?)",actor,request,receipt.intent().campaignId(),json.writeValueAsString(receipt));
    }
    public void save(UUID campaign, UUID scene, Token token, boolean create) {
        if (create) jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,version,hidden) values (?,?,?,?,?,?,?,?)",token.id(),scene,campaign,token.name(),token.x(),token.y(),token.version(),token.hidden());
        else jdbc.update("update token set name=?,x=?,y=?,version=?,hidden=? where id=? and scene_id=?",token.name(),token.x(),token.y(),token.version(),token.hidden(),token.id(),scene);
        jdbc.update("delete from token_controller where token_id=?",token.id());
        for (String controller : token.controllers()) jdbc.update("insert into token_controller(token_id,username,campaign_id) values (?,?,?)",token.id(),controller,campaign);
    }
    public long advance(UUID scene) {
        jdbc.update("update scene set version=version+1 where id=?",scene);
        return jdbc.queryForObject("select version from scene where id=?",Long.class,scene);
    }
}
