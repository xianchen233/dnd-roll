package io.dndroll.persistence;

import io.dndroll.application.AccountsService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcAccountsStore implements Store {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public JdbcAccountsStore(JdbcTemplate jdbc, TransactionTemplate transactions) { this.jdbc = jdbc; this.transactions = transactions; }
    public <T> T actorLocked(String actor, Supplier<T> work) {
        return transactions.execute(status -> {
            // Serialize receipt keys while allowing FK KEY SHARE checks by invitation acceptance.
            jdbc.query("select id from app_user where username=? for no key update", (rs, n) -> rs.getObject(1, UUID.class), actor);
            return work.get();
        });
    }
    public <T> T campaignLocked(UUID id, Supplier<T> work) {
        return transactions.execute(status -> {
            var rows = jdbc.query("select id from campaign where id=? for update", (rs, n) -> rs.getObject(1, UUID.class), id);
            if (rows.isEmpty()) throw new DomainException("NOT_FOUND", 404, "Campaign not found");
            return work.get();
        });
    }
    public Campaign campaign(UUID id) {
        return jdbc.queryForObject("select id,name,version from campaign where id=?", (rs, n) -> new Campaign(rs.getObject(1, UUID.class), rs.getString(2), rs.getLong(3)), id);
    }
    public Role role(UUID id, String username) {
        var rows = jdbc.query("select role from campaign_member where campaign_id=? and username=?", (rs, n) -> Role.valueOf(rs.getString(1)), id, username);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public List<Member> members(UUID id) {
        return jdbc.query("select username,role from campaign_member where campaign_id=? order by username", (rs, n) -> new Member(rs.getString(1), Role.valueOf(rs.getString(2))), id);
    }
    private Invite mapInvite(ResultSet rs, int ignored) throws SQLException {
        return new Invite(rs.getObject("id", UUID.class), rs.getObject("campaign_id", UUID.class), rs.getString("token_hash"), rs.getString("created_by"), rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("revoked"), rs.getString("used_by"));
    }
    public List<Invite> invitations(UUID id) { return jdbc.query("select * from campaign_invitation where campaign_id=? order by expires_at,id", this::mapInvite, id); }
    public Invite inviteByHash(String hash) {
        var rows = jdbc.query("select * from campaign_invitation where token_hash=?", this::mapInvite, hash);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Invite invitation(UUID campaignId, UUID id) {
        var rows = jdbc.query("select * from campaign_invitation where campaign_id=? and id=?", this::mapInvite, campaignId, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Receipt receipt(String actor, UUID requestId) {
        var rows = jdbc.query("select * from account_command_receipt where actor=? and request_id=?", (rs, n) -> {
            var expires = rs.getTimestamp("expires_at");
            return new Receipt(rs.getString("kind"), rs.getString("target"), rs.getLong("expected_version"), rs.getString("name"), rs.getObject("result_id", UUID.class), expires == null ? null : expires.toInstant(), rs.getLong("result_version"));
        }, actor, requestId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void receipt(String actor, UUID requestId, Receipt r) {
        jdbc.update("insert into account_command_receipt(actor,request_id,kind,target,expected_version,name,result_id,expires_at,result_version) values (?,?,?,?,?,?,?,?,?)",
            actor, requestId, r.kind(), r.target(), r.expectedVersion(), r.name(), r.resultId(), r.expiresAt() == null ? null : Timestamp.from(r.expiresAt()), r.resultVersion());
    }
    public void create(Campaign c, String actor) {
        jdbc.update("insert into campaign(id,name) values (?,?)", c.id(), c.name());
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'DM')", c.id(), actor);
        UUID scene = UUID.randomUUID();
        jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'First scene',24,16,48)", scene, c.id());
        jdbc.update("update campaign set current_scene_id=? where id=?", scene, c.id());
    }
    public void invitation(Invite i) {
        jdbc.update("insert into campaign_invitation(id,campaign_id,token_hash,created_by,expires_at) values (?,?,?,?,?)", i.id(), i.campaignId(), i.tokenHash(), i.createdBy(), Timestamp.from(i.expiresAt()));
    }
    public void revoke(UUID id) { jdbc.update("update campaign_invitation set revoked=true where id=?", id); }
    public void claim(UUID id, String actor) { jdbc.update("update campaign_invitation set used_by=? where id=?", actor, id); }
    public void join(UUID id, String actor) { jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'PLAYER') on conflict do nothing", id, actor); }
    public boolean insertUser(String username, String passwordHash) {
        return jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?) on conflict (username) do nothing", UUID.randomUUID(), username, passwordHash) == 1;
    }
    public void remove(UUID id, String username) {
        jdbc.update("delete from token_controller tc using token t,scene s where tc.token_id=t.id and t.scene_id=s.id and s.campaign_id=? and tc.username=?", id, username);
        jdbc.update("delete from campaign_member where campaign_id=? and username=? and role='PLAYER'", id, username);
    }
    public long advance(UUID id) {
        jdbc.update("update campaign set version=version+1 where id=?", id);
        return campaign(id).version();
    }
}

