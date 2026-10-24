package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.AccountsService;
import io.dndroll.persistence.AccountBootstrap;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=", "BOOTSTRAP_USERNAME=p01_owner", "BOOTSTRAP_PASSWORD=integration-owner-password"})
class AccountsIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccountsService service;
    @Autowired PasswordEncoder passwords;
    @Autowired AccountBootstrap bootstrap;
    final JsonMapper json = JsonMapper.builder().build();
    final Set<UUID> campaigns = new HashSet<>();
    final Set<String> users = new HashSet<>();
    @DynamicPropertySource static void database(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", () -> FirstSliceIT.required("TEST_DATABASE_URL"));
        props.add("spring.datasource.username", () -> FirstSliceIT.required("TEST_DATABASE_USERNAME"));
        props.add("spring.datasource.password", () -> FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
    }
    class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String csrf;
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(String path, Object body) throws Exception { return post(path, json.writeValueAsString(body), true); }
        HttpResponse<String> post(String path, String body, boolean csrfHeader) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
            if (csrfHeader) request.header("X-CSRF-TOKEN", csrf);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        Browser() throws Exception { csrf(); }
        void csrf() throws Exception { csrf = tree(get("/api/auth/csrf"), 200).get("token").asString(); }
        void login(String name, String password) throws Exception { tree(post("/api/auth/login", Map.of("username", name, "password", password)), 200); csrf(); }
        JsonNode getTree(String path) throws Exception { return tree(get(path), 200); }
    }
    Browser owner() throws Exception { var browser = new Browser(); browser.login("p01_owner", "integration-owner-password"); return browser; }
    String user() {
        String name = "p01_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12); users.add(name);
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)", UUID.randomUUID(), name, passwords.encode("integration-user-password")); return name;
    }
    Browser account(String name) throws Exception { var b = new Browser(); b.login(name, "integration-user-password"); return b; }
    JsonNode tree(HttpResponse<String> response, int status) throws Exception { assertEquals(status, response.statusCode(), response.body()); return json.readTree(response.body()); }
    void error(HttpResponse<String> response, int status, String code) throws Exception { assertEquals(code, tree(response, status).get("code").asString()); }
    String base(UUID id) { return "/api/campaigns/" + id; }
    Map<String, Object> command(UUID request, long version) { return Map.of("requestId", request, "expectedVersion", version); }
    Map<String, Object> command(long version) { return command(UUID.randomUUID(), version); }
    UUID create(Browser b) throws Exception {
        var created = tree(b.post("/api/campaigns", Map.of("requestId", UUID.randomUUID(), "name", "P01 test campaign")), 201);
        var id = UUID.fromString(created.get("id").asString()); campaigns.add(id); return id;
    }
    JsonNode invite(Browser owner, UUID id) throws Exception {
        long version = owner.getTree(base(id) + "/members").get("campaignVersion").asLong();
        return tree(owner.post(base(id) + "/invitations", command(version)), 201);
    }
    @AfterEach void cleanup() {
        // Dedicated P01 users/campaigns never share the demo fixture used by M1 tests.
        for (var id : campaigns) {
            jdbc.update("update campaign set current_scene_id=null where id=?", id);
            jdbc.update("delete from token_controller where token_id in (select t.id from token t join scene s on s.id=t.scene_id where s.campaign_id=?)", id);
            jdbc.update("delete from movement_receipt where campaign_id=?", id);
            jdbc.update("delete from token where scene_id in (select id from scene where campaign_id=?)", id);
            jdbc.update("delete from scene where campaign_id=?", id);
            jdbc.update("delete from campaign_invitation where campaign_id=?", id);
            jdbc.update("delete from campaign_member where campaign_id=?", id);
            jdbc.update("delete from campaign where id=?", id);
        }
        jdbc.update("delete from account_command_receipt where actor='p01_owner'");
        for (var name : users) { jdbc.update("delete from account_command_receipt where actor=?", name); jdbc.update("delete from app_user where username=?", name); }
    }
    @Test void bootstrapWithoutDemoIsInsertOnlyAndCreationHasDurableConcurrentReceipts() throws Exception {
        var owner = owner();
        var hash = jdbc.queryForObject("select password_hash from app_user where username='p01_owner'", String.class);
        bootstrap.run(new DefaultApplicationArguments());
        assertEquals(hash, jdbc.queryForObject("select password_hash from app_user where username='p01_owner'", String.class));
        assertEquals(0, owner.getTree("/api/campaigns").size());
        UUID request = UUID.randomUUID(); var body = Map.of("requestId", request, "name", "  P01 durable creation  ");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> owner.post("/api/campaigns", body)); var b = executor.submit(() -> owner.post("/api/campaigns", body));
            var left = tree(a.get(15, TimeUnit.SECONDS), 201); var right = tree(b.get(15, TimeUnit.SECONDS), 201);
            assertEquals(left, right); var id = UUID.fromString(left.get("id").asString()); campaigns.add(id);
            assertEquals("P01 durable creation", left.get("name").asString()); assertEquals("DM", left.get("role").asString());
            var scene = owner.getTree(base(id) + "/scene"); assertEquals(24, scene.get("width").asInt()); assertEquals(16, scene.get("height").asInt()); assertEquals(48, scene.get("gridSize").asInt()); assertEquals(0, scene.get("tokens").size());
            var freshService = new AccountsService(new io.dndroll.persistence.JdbcAccountsStore(jdbc, new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()))), passwords::encode, Clock.systemUTC(), () -> { throw new AssertionError("Replay generated a secret"); }, new AccountsService.Events() { public void changed(UUID campaign) {} public void removed(String user) {} });
            assertEquals(id, freshService.create("p01_owner", request, "P01 durable creation").id());
        }
        error(owner.post("/api/campaigns", Map.of("requestId", request, "name", "different")), 409, "IDEMPOTENCY_CONFLICT");
    }
    @Test void anonymousRegistrationRequiresCsrfAndPreservesSecretsOnlyInOneResponse() throws Exception {
        var owner = owner(); var id = create(owner); UUID request = UUID.randomUUID();
        var issued = tree(owner.post(base(id) + "/invitations", command(request, 0)), 201);
        String token = issued.get("token").asString(); assertEquals(32, Base64.getUrlDecoder().decode(token).length);
        var retry = tree(owner.post(base(id) + "/invitations", command(request, 0)), 200);
        assertTrue(retry.get("token").isNull()); assertEquals(issued.get("id"), retry.get("id")); assertEquals(issued.get("expiresAt"), retry.get("expiresAt"));
        var storedHash = jdbc.queryForObject("select token_hash from campaign_invitation where id=?", String.class, UUID.fromString(issued.get("id").asString()));
        assertEquals(AccountsService.hash(token), storedHash);
        assertFalse(jdbc.queryForObject("select row_to_json(r)::text from account_command_receipt r where actor='p01_owner' and request_id=?", String.class, request).contains(token));
        var anonymous = new Browser(); assertEquals(403, anonymous.post("/api/invitations/preview", json.writeValueAsString(Map.of("token", token)), false).statusCode());
        var preview = tree(anonymous.post("/api/invitations/preview", Map.of("token", token)), 200); assertEquals(3, preview.size()); assertEquals(id.toString(), preview.get("campaignId").asString());
        String name = "p01_" + UUID.randomUUID().toString().replace("-", "").substring(0,12); users.add(name);
        assertEquals(403, anonymous.post("/api/invitations/register", json.writeValueAsString(Map.of("token", token, "username", name, "password", " valid-password ")), false).statusCode());
        var registered = tree(anonymous.post("/api/invitations/register", Map.of("token", token, "username", name, "password", " valid-password ")), 201);
        assertEquals(name, registered.get("username").asString()); assertEquals(401, anonymous.get("/api/auth/me").statusCode());
        anonymous.login(name, " valid-password "); tree(anonymous.post("/api/invitations/accept", Map.of("token", token)), 200);
        assertEquals(1, anonymous.getTree("/api/campaigns").size());
        assertEquals(2, anonymous.getTree(base(id) + "/members").get("members").size());
        error(anonymous.post("/api/invitations/register", Map.of("token", token, "username", "newuser", "password", "valid-password")), 400, "INVALID_INPUT");
        error(anonymous.get(base(id) + "/invitations"), 403, "FORBIDDEN");
        var listing = owner.getTree(base(id) + "/invitations"); String listJson = listing.toString(); assertFalse(listJson.contains(token)); assertFalse(listJson.contains(storedHash)); assertEquals("USED", listing.get("invitations").get(0).get("status").asString());
        assertEquals(2, listing.get("campaignVersion").asLong());
    }
    @Test void invalidExpiredRevokedAndConsumedInvitationsHavePreciseErrors() throws Exception {
        var owner = owner(); var id = create(owner); var anon = new Browser();
        error(anon.post("/api/invitations/preview", Map.of("token", "bad")), 400, "INVALID_INPUT");
        String unknown = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        error(anon.post("/api/invitations/preview", Map.of("token", unknown)), 404, "INVITE_NOT_FOUND");
        var expired = invite(owner, id); var expiredId = UUID.fromString(expired.get("id").asString());
        jdbc.update("update campaign_invitation set expires_at=? where id=?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)), expiredId);
        error(anon.post("/api/invitations/preview", Map.of("token", expired.get("token").asString())), 410, "INVITE_EXPIRED");
        var player = account(user()); error(player.post("/api/invitations/accept", Map.of("token", expired.get("token").asString())), 410, "INVITE_EXPIRED");
        var revoked = invite(owner, id); var revokeRequest = UUID.randomUUID();
        assertEquals(204, owner.post(base(id) + "/invitations/" + revoked.get("id").asString() + "/revoke", command(revokeRequest, 2)).statusCode());
        assertEquals(204, owner.post(base(id) + "/invitations/" + revoked.get("id").asString() + "/revoke", command(revokeRequest, 2)).statusCode());
        error(anon.post("/api/invitations/preview", Map.of("token", revoked.get("token").asString())), 409, "INVITE_UNAVAILABLE");
        error(player.post("/api/invitations/accept", Map.of("token", revoked.get("token").asString())), 409, "INVITE_UNAVAILABLE");
        var used = invite(owner, id); tree(owner.post("/api/invitations/accept", Map.of("token", used.get("token").asString())), 200);
        assertEquals("DM", owner.getTree(base(id) + "/members").get("members").get(0).get("role").asString());
        error(player.post("/api/invitations/accept", Map.of("token", used.get("token").asString())), 409, "INVITE_UNAVAILABLE");
        error(anon.post("/api/invitations/preview", Map.of("token", used.get("token").asString())), 409, "INVITE_UNAVAILABLE");
        var statuses = owner.getTree(base(id) + "/invitations").get("invitations").toString(); assertTrue(statuses.contains("EXPIRED")); assertTrue(statuses.contains("REVOKED")); assertTrue(statuses.contains("USED"));
    }
    @Test void existingUsernameAndForcedRegistrationFailureRollBackAccountClaimAndVersion() throws Exception {
        var owner = owner(); var id = create(owner); var issued = invite(owner, id); var anon = new Browser(); String existing = user();
        String hash = jdbc.queryForObject("select password_hash from app_user where username=?", String.class, existing);
        error(anon.post("/api/invitations/register", Map.of("token", issued.get("token").asString(), "username", existing, "password", "changed-password")), 409, "USERNAME_TAKEN");
        assertEquals(hash, jdbc.queryForObject("select password_hash from app_user where username=?", String.class, existing));
        assertEquals(1, owner.getTree(base(id) + "/members").get("campaignVersion").asLong());
        tree(anon.post("/api/invitations/preview", Map.of("token", issued.get("token").asString())), 200);
        String name = "p01_" + UUID.randomUUID().toString().replace("-", "").substring(0,12); users.add(name);
        jdbc.execute("create function p01_fail_claim() returns trigger language plpgsql as $$ begin raise exception 'forced invitation claim failure'; end $$");
        jdbc.execute("create trigger p01_fail_claim before update of used_by on campaign_invitation for each row execute function p01_fail_claim()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> service.register(issued.get("token").asString(), name, "valid-password"));
            assertEquals(0, jdbc.queryForObject("select count(*) from app_user where username=?", Integer.class, name));
            assertEquals(1, owner.getTree(base(id) + "/members").get("campaignVersion").asLong());
            tree(anon.post("/api/invitations/preview", Map.of("token", issued.get("token").asString())), 200);
        } finally { jdbc.execute("drop trigger p01_fail_claim on campaign_invitation"); jdbc.execute("drop function p01_fail_claim()"); }
        tree(anon.post("/api/invitations/register", Map.of("token", issued.get("token").asString(), "username", name, "password", "valid-password")), 201);
    }
    @Test void concurrentAcceptanceHasOneWinnerAndConcurrentIssueReturnsOneSecret() throws Exception {
        var owner = owner(); var id = create(owner); UUID request = UUID.randomUUID(); String token;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> owner.post(base(id) + "/invitations", command(request, 0)));
            var b = executor.submit(() -> owner.post(base(id) + "/invitations", command(request, 0)));
            var left = a.get(15, TimeUnit.SECONDS); var right = b.get(15, TimeUnit.SECONDS);
            assertEquals(Set.of(200,201), Set.of(left.statusCode(), right.statusCode()));
            token = json.readTree(left.statusCode() == 201 ? left.body() : right.body()).get("token").asString();
        }
        var alice = account(user()); var bob = account(user());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> alice.post("/api/invitations/accept", Map.of("token", token)));
            var b = executor.submit(() -> bob.post("/api/invitations/accept", Map.of("token", token)));
            assertEquals(Set.of(200,409), Set.of(a.get(15, TimeUnit.SECONDS).statusCode(), b.get(15, TimeUnit.SECONDS).statusCode()));
        }
        assertEquals(2, owner.getTree(base(id) + "/members").get("members").size()); assertEquals(2, owner.getTree(base(id) + "/members").get("campaignVersion").asLong());
    }
    @Test void managementScopesVersionsStrictJsonAndIdempotentPayloadsAreEnforced() throws Exception {
        var owner = owner(); var id = create(owner); var other = create(owner); var issued = invite(owner, id);
        error(owner.post(base(id) + "/invitations", command(0)), 409, "VERSION_CONFLICT");
        error(owner.post(base(id) + "/invitations", "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":1.5}", true), 400, "INVALID_INPUT");
        error(owner.post(base(id) + "/invitations", "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":\"1\"}", true), 400, "INVALID_INPUT");
        error(owner.post(base(id) + "/invitations", Map.of("requestId", UUID.randomUUID())), 400, "INVALID_INPUT");
        error(owner.post(base(id) + "/invitations", command(-1)), 400, "INVALID_INPUT");
        error(owner.post(base(other) + "/invitations/" + issued.get("id").asString() + "/revoke", command(0)), 404, "NOT_FOUND");
        error(owner.post(base(id) + "/members/p01_owner/remove", command(1)), 409, "DM_REMOVAL_DENIED");
        String playerName = user(); var player = account(playerName); tree(player.post("/api/invitations/accept", Map.of("token", issued.get("token").asString())), 200);
        error(player.post(base(id) + "/invitations", command(2)), 403, "FORBIDDEN");
        error(player.post(base(id) + "/members/p01_owner/remove", command(2)), 403, "FORBIDDEN");
        error(player.get(base(other) + "/members"), 403, "FORBIDDEN");
        UUID removeRequest = UUID.randomUUID();
        assertEquals(204, owner.post(base(id) + "/members/" + playerName + "/remove", command(removeRequest, 2)).statusCode());
        assertEquals(204, owner.post(base(id) + "/members/" + playerName + "/remove", command(removeRequest, 2)).statusCode());
        error(owner.post(base(id) + "/members/someone/remove", command(removeRequest, 2)), 409, "IDEMPOTENCY_CONFLICT");
        var fresh = invite(owner, id); tree(player.post("/api/invitations/accept", Map.of("token", fresh.get("token").asString())), 200);
        assertEquals(204, owner.post(base(id) + "/members/" + playerName + "/remove", command(removeRequest, 2)).statusCode());
        assertEquals(200, player.get(base(id) + "/scene").statusCode(), "An old removal receipt must not remove a newly granted membership");
    }
    @Test void removalClosesRealSocketDeletesOnlyCampaignGrantsAndRejectsOldInviteAndMoveReceipts() throws Exception {
        var owner = owner(); var id = create(owner); var other = create(owner); String name = user(); var player = account(name);
        var issued = invite(owner, id); var otherInvite = invite(owner, other);
        tree(player.post("/api/invitations/accept", Map.of("token", issued.get("token").asString())), 200);
        tree(player.post("/api/invitations/accept", Map.of("token", otherInvite.get("token").asString())), 200);
        UUID token = token(id, name); UUID otherToken = token(other, name); UUID moveRequest = UUID.randomUUID();
        var move = Map.of("requestId", moveRequest, "expectedVersion", 0, "x", 3, "y", 4);
        tree(player.post(base(id) + "/tokens/" + token + "/moves", move), 200);
        var frames = new LinkedBlockingQueue<String>(); var closed = new CountDownLatch(1);
        var socket = player.client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port + "/ws"), new WebSocket.Listener() {
            StringBuilder fragments = new StringBuilder();
            public void onOpen(WebSocket ws) { ws.request(1); }
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) { fragments.append(data); if (last) { frames.add(fragments.toString()); fragments.setLength(0); } ws.request(1); return null; }
            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) { closed.countDown(); return null; }
        }).get(5, TimeUnit.SECONDS);
        try {
            socket.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nX-CSRF-TOKEN:" + player.csrf + "\n\n\0", true).join();
            String connected = frames.poll(5, TimeUnit.SECONDS); assertNotNull(connected); assertTrue(connected.startsWith("CONNECTED"));
            assertEquals(204, owner.post(base(id) + "/members/" + name + "/remove", command(2)).statusCode());
            assertTrue(closed.await(5, TimeUnit.SECONDS), "Removed member's established socket must close");
            error(player.get(base(id) + "/scene"), 403, "FORBIDDEN");
            error(player.post(base(id) + "/tokens/" + token + "/moves", move), 403, "FORBIDDEN");
            error(player.post("/api/invitations/accept", Map.of("token", issued.get("token").asString())), 409, "INVITE_UNAVAILABLE");
            assertEquals(200, player.get("/api/auth/me").statusCode()); assertEquals(200, player.get(base(other) + "/scene").statusCode());
            assertEquals(0, jdbc.queryForObject("select count(*) from token_controller where token_id=? and username=?", Integer.class, token, name));
            assertEquals(1, jdbc.queryForObject("select count(*) from token_controller where token_id=? and username=?", Integer.class, otherToken, name));
            bootstrap.run(new DefaultApplicationArguments());
            var loggedInAgain = account(name); error(loggedInAgain.get(base(id) + "/scene"), 403, "FORBIDDEN");
        } finally { socket.abort(); }
    }
    @Test void failedRemovalReceiptRestoresMembershipGrantsAndVersion() throws Exception {
        var owner = owner(); var id = create(owner); String name = user(); var player = account(name);
        var issued = invite(owner, id); tree(player.post("/api/invitations/accept", Map.of("token", issued.get("token").asString())), 200);
        UUID token = token(id, name); UUID request = UUID.randomUUID();
        jdbc.execute("create function p01_fail_receipt() returns trigger language plpgsql as $$ begin raise exception 'forced management receipt failure'; end $$");
        jdbc.execute("create trigger p01_fail_receipt before insert on account_command_receipt for each row execute function p01_fail_receipt()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> service.remove(id, name, "p01_owner", request, 2));
            assertEquals(200, player.get(base(id) + "/scene").statusCode());
            assertEquals(1, jdbc.queryForObject("select count(*) from token_controller where token_id=? and username=?", Integer.class, token, name));
            assertEquals(2, owner.getTree(base(id) + "/members").get("campaignVersion").asLong());
            assertEquals(0, jdbc.queryForObject("select count(*) from account_command_receipt where request_id=?", Integer.class, request));
        } finally { jdbc.execute("drop trigger p01_fail_receipt on account_command_receipt"); jdbc.execute("drop function p01_fail_receipt()"); }
    }
    @Test void actorReceiptLockAllowsForeignKeyChecksDuringExistingDmAcceptance() throws Exception {
        var owner = owner(); var id = create(owner); var issued = invite(owner, id);
        var transaction = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var lock = executor.submit(() -> transaction.execute(status -> {
                jdbc.query("select id from app_user where username='p01_owner' for no key update", (rs, n) -> rs.getObject(1, UUID.class));
                locked.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); } catch (InterruptedException error) { throw new RuntimeException(error); }
                return null;
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            try {
                var acceptance = executor.submit(() -> owner.post("/api/invitations/accept", Map.of("token", issued.get("token").asString())));
                tree(acceptance.get(5, TimeUnit.SECONDS), 200);
            } finally { release.countDown(); }
            lock.get(5, TimeUnit.SECONDS);
        }
    }
    UUID token(UUID campaign, String username) {
        UUID id = UUID.randomUUID(); UUID scene = jdbc.queryForObject("select id from scene where campaign_id=?", UUID.class, campaign);
        jdbc.update("insert into token(id,scene_id,name,x,y) values (?,?,'P01 controlled token',1,1)", id, scene);
        jdbc.update("insert into token_controller(token_id,username) values (?,?)", id, username); return id;
    }
}

