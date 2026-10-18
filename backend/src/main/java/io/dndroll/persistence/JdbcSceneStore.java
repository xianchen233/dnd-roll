package io.dndroll.persistence;

import io.dndroll.application.SceneService.*;
import io.dndroll.application.SceneManagementService;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcSceneStore implements Store, SceneManagementService.Store {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public JdbcSceneStore(JdbcTemplate jdbc, TransactionTemplate transactions) { this.jdbc = jdbc; this.transactions = transactions; }
    public List<CampaignView> campaigns(String actor) {
        return jdbc.query("select c.id,c.name,m.role from campaign c join campaign_member m on m.campaign_id=c.id where m.username=? order by c.name,c.id",
            (rs, n) -> new CampaignView(rs.getObject("id", UUID.class), rs.getString("name"), Role.valueOf(rs.getString("role"))), actor);
    }
    public Role role(UUID id, String actor) {
        var roles = jdbc.query("select role from campaign_member where campaign_id=? and username=?", (rs, n) -> Role.valueOf(rs.getString(1)), id, actor);
        return roles.isEmpty() ? null : roles.getFirst();
    }
    public Scene scene(UUID campaignId) {
        var scenes = jdbc.query("select s.* from scene s join campaign c on c.id=s.campaign_id and c.current_scene_id=s.id where c.id=?", (rs, n) -> new Scene(rs.getObject("id", UUID.class), campaignId,
            rs.getString("name"), rs.getInt("width"), rs.getInt("height"), rs.getInt("grid_size"), rs.getLong("version"), List.of()), campaignId);
        if (scenes.isEmpty()) throw new DomainException("NOT_FOUND", 404, "Scene not found");
        var scene = scenes.getFirst();
        Map<UUID, Set<String>> controllers = new HashMap<>();
        jdbc.query("select tc.token_id,tc.username from token_controller tc join token t on t.id=tc.token_id where t.scene_id=?", rs -> {
            controllers.computeIfAbsent(rs.getObject(1, UUID.class), ignored -> new HashSet<>()).add(rs.getString(2));
        }, scene.id());
        var tokens = jdbc.query("select * from token where scene_id=? order by id", (rs, n) -> {
            UUID id = rs.getObject("id", UUID.class);
            return new Token(id, rs.getString("name"), rs.getInt("x"), rs.getInt("y"), rs.getLong("version"), rs.getBoolean("hidden"), Set.copyOf(controllers.getOrDefault(id, Set.of())));
        }, scene.id());
        return new Scene(scene.id(), scene.campaignId(), scene.name(), scene.width(), scene.height(), scene.gridSize(), scene.version(), tokens);
    }
    public Receipt receipt(UUID campaignId, String actor, UUID requestId) {
        var results = jdbc.query("select * from movement_receipt where campaign_id=? and actor=? and request_id=?", (rs, n) -> {
            UUID token = rs.getObject("token_id", UUID.class);
            return new Receipt(token, new Command(requestId, rs.getLong("expected_version"), rs.getInt("requested_x"), rs.getInt("requested_y")),
                new TokenView(token, rs.getString("result_name"), rs.getInt("result_x"), rs.getInt("result_y"), rs.getLong("result_version"), rs.getBoolean("result_can_move")));
        }, campaignId, actor, requestId);
        return results.isEmpty() ? null : results.getFirst();
    }
    public void save(UUID campaignId, String actor, Token token, Command command, TokenView response) {
        int updated = jdbc.update("update token set x=?,y=?,version=? where id=? and version=?", token.x(), token.y(), token.version(), token.id(), command.expectedVersion());
        if (updated != 1) throw new DomainException("VERSION_CONFLICT", 409, "The token changed; refresh the scene");
        jdbc.update("update scene set version=version+1 where id=(select scene_id from token where id=?) and campaign_id=?", token.id(), campaignId);
        jdbc.update("insert into movement_receipt(campaign_id,actor,request_id,token_id,expected_version,requested_x,requested_y,result_name,result_x,result_y,result_version,result_can_move) values (?,?,?,?,?,?,?,?,?,?,?,?)",
            campaignId, actor, command.requestId(), token.id(), command.expectedVersion(), command.x(), command.y(), response.name(), response.x(), response.y(), response.version(), response.canMove());
    }
    public <T> T locked(UUID campaignId, Supplier<T> action) {
        return transactions.execute(status -> {
            // One campaign lock serializes retries before either version or receipt is read.
            jdbc.query("select id from campaign where id=? for update", (rs, n) -> rs.getObject(1, UUID.class), campaignId);
            return action.get();
        });
    }
    public <T> T actorLocked(String actor, Supplier<T> action) {
        return transactions.execute(status -> {
            jdbc.query("select id from app_user where username=? for no key update", (rs, n) -> rs.getObject(1, UUID.class), actor);
            return action.get();
        });
    }
    public SceneManagementService.Listing listing(UUID campaignId) {
        var scenes = jdbc.query("select id,name from scene where campaign_id=? order by name,id",
            (rs, n) -> new SceneManagementService.Entry(rs.getObject(1, UUID.class), rs.getString(2)), campaignId);
        return jdbc.queryForObject("select version,current_scene_id from campaign where id=?", (rs, n) ->
            new SceneManagementService.Listing(rs.getLong(1), rs.getObject(2, UUID.class), scenes), campaignId);
    }
    public SceneManagementService.Entry entry(UUID campaignId, UUID sceneId) {
        var rows = jdbc.query("select id,name from scene where campaign_id=? and id=?",
            (rs, n) -> new SceneManagementService.Entry(rs.getObject(1, UUID.class), rs.getString(2)), campaignId, sceneId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public SceneManagementService.Receipt sceneReceipt(String actor, UUID requestId) {
        var rows = jdbc.query("select * from scene_command_receipt where actor=? and request_id=?", (rs, n) ->
            new SceneManagementService.Receipt(rs.getString("kind"), rs.getObject("campaign_id", UUID.class), rs.getObject("target", UUID.class),
                rs.getString("name"), rs.getLong("expected_version"), new SceneManagementService.Result(rs.getObject("result_id", UUID.class), rs.getString("result_name"), rs.getLong("result_version"))), actor, requestId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void sceneReceipt(String actor, UUID requestId, SceneManagementService.Receipt r) {
        jdbc.update("insert into scene_command_receipt(actor,request_id,kind,campaign_id,target,name,expected_version,result_id,result_name,result_version) values (?,?,?,?,?,?,?,?,?,?)",
            actor, requestId, r.kind(), r.campaignId(), r.target(), r.name(), r.expectedVersion(), r.result().id(), r.result().name(), r.result().campaignVersion());
    }
    public void createScene(UUID campaignId, SceneManagementService.Entry entry) {
        jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,?,24,16,48)", entry.id(), campaignId, entry.name());
    }
    public void activateScene(UUID campaignId, UUID sceneId) { jdbc.update("update campaign set current_scene_id=? where id=?", sceneId, campaignId); }
    public long advanceCampaign(UUID campaignId) {
        jdbc.update("update campaign set version=version+1 where id=?", campaignId);
        return jdbc.queryForObject("select version from campaign where id=?", Long.class, campaignId);
    }
}
