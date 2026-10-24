package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.AccountsService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AccountsServiceTest {
    static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
    final Memory store = new Memory();
    int secretNumber;
    final List<String> removed = new ArrayList<>();
    final AccountsService service = new AccountsService(store, password -> "encoded:" + password,
        Clock.fixed(NOW, ZoneOffset.UTC), () -> Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32 - 0]).substring(0, 42) + "AEIMQUYcgkosw048".charAt(secretNumber++),
        new Events() { public void changed(UUID id) {} public void removed(String username) { removed.add(username); } });
    final UUID campaign = service.create("owner", UUID.randomUUID(), "  First  ").id();
    @Test void createRetriesNormalizeNameAndEnforcePayloadAndCurrentAuthority() {
        var request = UUID.randomUUID(); var created = service.create("owner", request, "  Second  ");
        assertEquals(created, service.create("owner", request, "Second"));
        code("IDEMPOTENCY_CONFLICT", () -> service.create("owner", request, "Third"));
        store.remove(created.id(), "owner");
        code("FORBIDDEN", () -> service.create("owner", request, "Second"));
    }
    @Test void invitationReturnsSecretOnceAndStoresOnlyHashAndMetadata() {
        var request = UUID.randomUUID(); var issued = service.issue(campaign, "owner", request, 0);
        assertEquals(NOW.plus(Duration.ofDays(7)), issued.expiresAt());
        assertEquals(32, Base64.getUrlDecoder().decode(issued.token()).length);
        var retry = service.issue(campaign, "owner", request, 0);
        assertNull(retry.token()); assertEquals(issued.id(), retry.id()); assertEquals(1, store.campaign(campaign).version());
        assertEquals(AccountsService.hash(issued.token()), store.invitation(campaign, issued.id()).tokenHash());
        assertFalse(store.receipt("owner", request).toString().contains(issued.token()));
        code("IDEMPOTENCY_CONFLICT", () -> service.issue(campaign, "owner", request, 1));
        store.remove(campaign, "owner");
        code("FORBIDDEN", () -> service.issue(campaign, "owner", request, 0));
    }
    @Test void managementRequiresDmAndCurrentVersionBeforeAnyMutation() {
        store.join(campaign, "player");
        code("FORBIDDEN", () -> service.issue(campaign, "player", UUID.randomUUID(), 0));
        code("VERSION_CONFLICT", () -> service.issue(campaign, "owner", UUID.randomUUID(), 4));
        assertEquals(0, store.invitations(campaign).size());
        assertEquals(2, service.members(campaign, "player").members().size());
        code("FORBIDDEN", () -> service.invitations(campaign, "player"));
        code("FORBIDDEN", () -> service.members(campaign, "outsider"));
    }
    @Test void acceptanceIsSingleUseAndRemovedActorCannotRecoverMembership() {
        var invite = service.issue(campaign, "owner", UUID.randomUUID(), 0);
        assertEquals(campaign, service.preview(invite.token()).campaignId());
        assertEquals(campaign, service.accept(invite.token(), "alice").campaignId());
        assertEquals(Role.PLAYER, store.role(campaign, "alice"));
        service.accept(invite.token(), "alice"); assertEquals(2, store.campaign(campaign).version());
        code("INVITE_UNAVAILABLE", () -> service.accept(invite.token(), "bob"));
        service.remove(campaign, "alice", "owner", UUID.randomUUID(), 2);
        code("INVITE_UNAVAILABLE", () -> service.accept(invite.token(), "alice"));
        assertNull(store.role(campaign, "alice")); assertEquals(List.of("alice"), removed);
    }
    @Test void acceptingAsExistingDmNeverDowngradesRole() {
        var invite = service.issue(campaign, "owner", UUID.randomUUID(), 0);
        service.accept(invite.token(), "owner");
        assertEquals(Role.DM, store.role(campaign, "owner"));
        assertEquals("USED", service.invitations(campaign, "owner").invitations().getFirst().status());
    }
    @Test void expiredRevokedAndMalformedInvitesCannotBePreviewedOrClaimed() {
        var issued = service.issue(campaign, "owner", UUID.randomUUID(), 0);
        var invite = store.invitation(campaign, issued.id());
        store.invites.put(invite.id(), new Invite(invite.id(), campaign, invite.tokenHash(), "owner", NOW, false, null));
        code("INVITE_EXPIRED", () -> service.preview(issued.token()));
        code("INVITE_EXPIRED", () -> service.accept(issued.token(), "alice"));
        assertEquals("EXPIRED", service.invitations(campaign, "owner").invitations().getFirst().status());
        code("INVALID_INPUT", () -> service.preview("bad"));
        code("INVITE_NOT_FOUND", () -> service.preview(Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]).substring(0,42) + "Q"));
        service.revoke(campaign, invite.id(), "owner", UUID.randomUUID(), 1);
        code("INVITE_UNAVAILABLE", () -> service.accept(issued.token(), "alice"));
    }
    @Test void removeAndRevokeReplaysCannotRepeatSideEffectsOrAffectNewMembership() {
        store.join(campaign, "alice"); var removeRequest = UUID.randomUUID();
        service.remove(campaign, "alice", "owner", removeRequest, 0);
        store.join(campaign, "alice"); service.remove(campaign, "alice", "owner", removeRequest, 0);
        assertEquals(Role.PLAYER, store.role(campaign, "alice"));
        code("IDEMPOTENCY_CONFLICT", () -> service.remove(campaign, "bob", "owner", removeRequest, 0));
        code("DM_REMOVAL_DENIED", () -> service.remove(campaign, "owner", "owner", UUID.randomUUID(), 1));
        var invite = service.issue(campaign, "owner", UUID.randomUUID(), 1); var revokeRequest = UUID.randomUUID();
        service.revoke(campaign, invite.id(), "owner", revokeRequest, 2);
        service.revoke(campaign, invite.id(), "owner", revokeRequest, 2);
        service.revoke(campaign, invite.id(), "owner", UUID.randomUUID(), 3);
        assertEquals(3, store.campaign(campaign).version());
        code("NOT_FOUND", () -> service.revoke(campaign, UUID.randomUUID(), "owner", UUID.randomUUID(), 3));
    }
    @Test void registrationRequiresStrictCredentialsAndUsernameConflictDoesNotClaimInvite() {
        var invite = service.issue(campaign, "owner", UUID.randomUUID(), 0);
        store.users.add("taken");
        code("USERNAME_TAKEN", () -> service.register(invite.token(), "taken", " valid-password "));
        assertNull(store.invitation(campaign, invite.id()).usedBy());
        code("INVALID_INPUT", () -> service.register(invite.token(), "abc", "too-short"));
        var registered = service.register(invite.token(), "alice", " valid-password ");
        assertEquals(new Registered("alice", campaign), registered);
        assertEquals("encoded: valid-password ", store.passwordHash);
        assertEquals(Role.PLAYER, store.role(campaign, "alice"));
    }
    static void code(String code, Runnable work) { assertEquals(code, assertThrows(DomainException.class, work::run).code()); }
    static class Memory implements Store {
        final Map<UUID, Campaign> campaigns = new HashMap<>();
        final Map<UUID, Map<String, Role>> roles = new HashMap<>();
        final Map<UUID, Invite> invites = new HashMap<>();
        final Map<String, Receipt> receipts = new HashMap<>();
        final Set<String> users = new HashSet<>(); String passwordHash;
        public <T> T actorLocked(String actor, Supplier<T> work) { return work.get(); }
        public <T> T campaignLocked(UUID id, Supplier<T> work) { return work.get(); }
        public Campaign campaign(UUID id) { return campaigns.get(id); }
        public Role role(UUID id, String actor) { return roles.get(id).get(actor); }
        public List<Member> members(UUID id) { return roles.get(id).entrySet().stream().map(e -> new Member(e.getKey(), e.getValue())).toList(); }
        public List<Invite> invitations(UUID id) { return invites.values().stream().filter(i -> i.campaignId().equals(id)).toList(); }
        public Invite inviteByHash(String hash) { return invites.values().stream().filter(i -> i.tokenHash().equals(hash)).findFirst().orElse(null); }
        public Invite invitation(UUID campaignId, UUID invitationId) { var i = invites.get(invitationId); return i != null && i.campaignId().equals(campaignId) ? i : null; }
        public Receipt receipt(String actor, UUID requestId) { return receipts.get(actor + requestId); }
        public void receipt(String actor, UUID requestId, Receipt receipt) { receipts.put(actor + requestId, receipt); }
        public void create(Campaign c, String actor) { campaigns.put(c.id(), c); roles.put(c.id(), new HashMap<>(Map.of(actor, Role.DM))); }
        public void invitation(Invite i) { invites.put(i.id(), i); }
        public void revoke(UUID id) { var i = invites.get(id); invites.put(id, new Invite(id, i.campaignId(), i.tokenHash(), i.createdBy(), i.expiresAt(), true, null)); }
        public void claim(UUID id, String actor) { var i = invites.get(id); invites.put(id, new Invite(id, i.campaignId(), i.tokenHash(), i.createdBy(), i.expiresAt(), false, actor)); }
        public void join(UUID id, String actor) { roles.get(id).putIfAbsent(actor, Role.PLAYER); }
        public boolean insertUser(String username, String hash) { passwordHash = hash; return users.add(username); }
        public void remove(UUID id, String actor) { roles.get(id).remove(actor); }
        public long advance(UUID id) { var c = campaigns.get(id); campaigns.put(id, new Campaign(id, c.name(), c.version() + 1)); return c.version() + 1; }
    }
}
