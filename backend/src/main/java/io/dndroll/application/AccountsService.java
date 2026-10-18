package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.*;

/** Account and membership commands; storage supplies atomic transactions and campaign locks. */
public final class AccountsService {
    public record Campaign(UUID id, String name, long version) {}
    public record Member(String username, Role role) {}
    public record Members(long campaignVersion, List<Member> members) {}
    public record Invite(UUID id, UUID campaignId, String tokenHash, String createdBy, Instant expiresAt, boolean revoked, String usedBy) {}
    public record InviteView(UUID id, String createdBy, Instant expiresAt, String status, String usedBy) {}
    public record Invitations(long campaignVersion, List<InviteView> invitations) {}
    public record Issued(UUID id, String token, Instant expiresAt, long campaignVersion) {}
    public record Preview(UUID campaignId, String campaignName, Instant expiresAt) {}
    public record Joined(UUID campaignId) {}
    public record Registered(String username, UUID campaignId) {}
    public record Created(UUID id, String name, Role role) {}
    public record Receipt(String kind, String target, long expectedVersion, String name, UUID resultId, Instant expiresAt, long resultVersion) {}
    public interface Store {
        <T> T actorLocked(String actor, Supplier<T> work);
        <T> T campaignLocked(UUID campaignId, Supplier<T> work);
        Campaign campaign(UUID id);
        Role role(UUID id, String username);
        List<Member> members(UUID id);
        List<Invite> invitations(UUID id);
        Invite inviteByHash(String hash);
        Invite invitation(UUID campaignId, UUID invitationId);
        Receipt receipt(String actor, UUID requestId);
        void receipt(String actor, UUID requestId, Receipt receipt);
        void create(Campaign campaign, String actor);
        void invitation(Invite invite);
        void revoke(UUID id);
        void claim(UUID id, String actor);
        void join(UUID campaignId, String actor);
        boolean insertUser(String username, String passwordHash);
        void remove(UUID campaignId, String actor);
        long advance(UUID campaignId);
    }
    public interface Events { void changed(UUID campaignId); void removed(String username); }
    private final Store store;
    private final Function<String, String> passwords;
    private final Clock clock;
    private final Supplier<String> secrets;
    private final Events events;
    public AccountsService(Store store, Function<String, String> passwords, Clock clock, Supplier<String> secrets, Events events) {
        this.store = store; this.passwords = passwords; this.clock = clock; this.secrets = secrets; this.events = events;
    }
    public Created create(String actor, UUID requestId, String name) {
        requiredRequest(requestId);
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty() || clean.codePointCount(0, clean.length()) > 100) throw invalid();
        return store.actorLocked(actor, () -> {
            var receipt = store.receipt(actor, requestId);
            if (receipt != null) {
                same(receipt, "CREATE", "", 0, clean);
                requireMember(receipt.resultId(), actor);
                return new Created(receipt.resultId(), receipt.name(), Role.DM);
            }
            var campaign = new Campaign(UUID.randomUUID(), clean, 0);
            store.create(campaign, actor);
            store.receipt(actor, requestId, new Receipt("CREATE", "", 0, clean, campaign.id(), null, 0));
            return new Created(campaign.id(), clean, Role.DM);
        });
    }
    public Members members(UUID id, String actor) {
        return store.campaignLocked(id, () -> { requireMember(id, actor); return new Members(store.campaign(id).version(), store.members(id)); });
    }
    public Invitations invitations(UUID id, String actor) {
        return store.campaignLocked(id, () -> {
            AccountPolicy.dm(store.role(id, actor));
            return new Invitations(store.campaign(id).version(), store.invitations(id).stream().map(i ->
                new InviteView(i.id(), i.createdBy(), i.expiresAt(), i.usedBy() != null ? "USED" : i.revoked() ? "REVOKED" : i.expiresAt().isAfter(clock.instant()) ? "ACTIVE" : "EXPIRED", i.usedBy())).toList());
        });
    }
    public Issued issue(UUID id, String actor, UUID requestId, long expectedVersion) {
        requiredRequest(requestId);
        return managedLocked(id, actor, () -> {
            AccountPolicy.dm(store.role(id, actor));
            var receipt = store.receipt(actor, requestId);
            if (receipt != null) {
                same(receipt, "INVITE", id.toString(), expectedVersion, "");
                return new Issued(receipt.resultId(), null, receipt.expiresAt(), receipt.resultVersion());
            }
            AccountPolicy.version(store.campaign(id).version(), expectedVersion);
            String token = secrets.get();
            // PostgreSQL stores microseconds; make the one-time response match durable replay metadata exactly.
            var expiresAt = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS).plus(Duration.ofDays(7));
            var invite = new Invite(UUID.randomUUID(), id, hash(token), actor, expiresAt, false, null);
            store.invitation(invite);
            long version = store.advance(id);
            store.receipt(actor, requestId, new Receipt("INVITE", id.toString(), expectedVersion, "", invite.id(), invite.expiresAt(), version));
            events.changed(id);
            return new Issued(invite.id(), token, invite.expiresAt(), version);
        });
    }
    public void revoke(UUID id, UUID invitationId, String actor, UUID requestId, long expectedVersion) {
        requiredRequest(requestId);
        managedLocked(id, actor, () -> {
            AccountPolicy.dm(store.role(id, actor));
            var invite = store.invitation(id, invitationId);
            if (invite == null) throw new DomainException("NOT_FOUND", 404, "Invitation not found");
            var receipt = store.receipt(actor, requestId);
            String target = id + "/" + invitationId;
            if (receipt != null) { same(receipt, "REVOKE", target, expectedVersion, ""); return null; }
            AccountPolicy.version(store.campaign(id).version(), expectedVersion);
            long version = store.campaign(id).version();
            if (!invite.revoked() && invite.usedBy() == null) { store.revoke(invitationId); version = store.advance(id); events.changed(id); }
            store.receipt(actor, requestId, new Receipt("REVOKE", target, expectedVersion, "", invitationId, null, version));
            return null;
        });
    }
    public Preview preview(String token) {
        var found = find(token);
        return store.campaignLocked(found.campaignId(), () -> {
            var invite = find(token); active(invite);
            var campaign = store.campaign(invite.campaignId());
            return new Preview(campaign.id(), campaign.name(), invite.expiresAt());
        });
    }
    public Joined accept(String token, String actor) {
        var found = find(token);
        return store.campaignLocked(found.campaignId(), () -> {
            var invite = find(token);
            if (actor.equals(invite.usedBy()) && store.role(invite.campaignId(), actor) != null) return new Joined(invite.campaignId());
            active(invite);
            store.join(invite.campaignId(), actor); // Insert-only: an existing DM never loses their role.
            store.claim(invite.id(), actor); store.advance(invite.campaignId()); events.changed(invite.campaignId());
            return new Joined(invite.campaignId());
        });
    }
    public Registered register(String token, String username, String password) {
        AccountPolicy.credentials(username, password);
        var found = find(token);
        return store.campaignLocked(found.campaignId(), () -> {
            var invite = find(token); active(invite);
            if (!store.insertUser(username, passwords.apply(password))) throw new DomainException("USERNAME_TAKEN", 409, "Username is already taken");
            store.join(invite.campaignId(), username); store.claim(invite.id(), username); store.advance(invite.campaignId()); events.changed(invite.campaignId());
            return new Registered(username, invite.campaignId());
        });
    }
    public void remove(UUID id, String username, String actor, UUID requestId, long expectedVersion) {
        requiredRequest(requestId);
        if (username == null || username.isBlank() || username.length() > 80) throw invalid();
        managedLocked(id, actor, () -> {
            AccountPolicy.dm(store.role(id, actor));
            var receipt = store.receipt(actor, requestId);
            String target = id + "/" + username;
            if (receipt != null) { same(receipt, "REMOVE", target, expectedVersion, ""); return null; }
            AccountPolicy.version(store.campaign(id).version(), expectedVersion);
            var role = store.role(id, username);
            if (role == Role.DM) throw new DomainException("DM_REMOVAL_DENIED", 409, "A DM cannot be removed");
            long version = store.campaign(id).version();
            if (role != null) { store.remove(id, username); version = store.advance(id); events.removed(username); events.changed(id); }
            store.receipt(actor, requestId, new Receipt("REMOVE", target, expectedVersion, "", id, null, version));
            return null;
        });
    }
    private <T> T managedLocked(UUID id, String actor, Supplier<T> work) { return store.actorLocked(actor, () -> store.campaignLocked(id, work)); }
    private void requireMember(UUID id, String actor) { if (store.role(id, actor) == null) throw new DomainException("FORBIDDEN", 403, "Campaign membership required"); }
    private Invite find(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}") || !Base64.getUrlEncoder().withoutPadding().encodeToString(Base64.getUrlDecoder().decode(token)).equals(token)) throw invalid();
        var invite = store.inviteByHash(hash(token));
        if (invite == null) throw new DomainException("INVITE_NOT_FOUND", 404, "Invitation not found");
        return invite;
    }
    private void active(Invite invite) { AccountPolicy.active(invite.expiresAt(), invite.revoked(), invite.usedBy(), clock.instant()); }
    private static void requiredRequest(UUID requestId) { if (requestId == null) throw invalid(); }
    private static DomainException invalid() { return new DomainException("INVALID_INPUT", 400, "Invalid account or campaign command"); }
    private static void same(Receipt receipt, String kind, String target, long version, String name) {
        if (!receipt.kind().equals(kind) || !receipt.target().equals(target) || receipt.expectedVersion() != version || !receipt.name().equals(name))
            throw new DomainException("IDEMPOTENCY_CONFLICT", 409, "Request ID already used with different parameters");
    }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}

