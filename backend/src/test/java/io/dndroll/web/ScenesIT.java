package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import io.dndroll.application.SceneManagementService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=", "BOOTSTRAP_USERNAME=p02_owner", "BOOTSTRAP_PASSWORD=integration-owner-password"})
class ScenesIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired SceneManagementService service;
    @Autowired PasswordEncoder passwords;
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
        Browser() throws Exception { refreshCsrf(); }
        void refreshCsrf() throws Exception { csrf = tree(get("/api/auth/csrf"), 200).get("token").asString(); }
        void login(String name) throws Exception { tree(post("/api/auth/login", Map.of("username", name, "password", "integration-owner-password")), 200); refreshCsrf(); }
        JsonNode read(String path) throws Exception { return tree(get(path), 200); }
    }
    Browser owner() throws Exception { var browser = new Browser(); browser.login("p02_owner"); return browser; }
    Browser member(UUID campaign, String role) throws Exception {
        String name = "p02_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12); users.add(name);
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?)", UUID.randomUUID(), name, passwords.encode("integration-owner-password"));
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?)", campaign, name, role);
        var browser = new Browser(); browser.login(name); return browser;
    }
    JsonNode tree(HttpResponse<String> response, int status) throws Exception { assertEquals(status, response.statusCode(), response.body()); return json.readTree(response.body()); }
    void error(HttpResponse<String> response, int status, String code) throws Exception { assertEquals(code, tree(response, status).get("code").asString()); }
    String base(UUID id) { return "/api/campaigns/" + id; }
    Map<String, Object> command(UUID request, long version) { return Map.of("requestId", request, "expectedVersion", version); }
    Map<String, Object> command(long version) { return command(UUID.randomUUID(), version); }
    Map<String, Object> createBody(UUID request, long version, String name) { return Map.of("requestId", request, "expectedVersion", version, "name", name); }
    UUID campaign(Browser b) throws Exception {
        var created = tree(b.post("/api/campaigns", Map.of("requestId", UUID.randomUUID(), "name", "P02 test campaign")), 201);
        var id = UUID.fromString(created.get("id").asString()); campaigns.add(id); return id;
    }
    @AfterEach void cleanup() {
        for (var id : campaigns) {
            jdbc.update("update campaign set current_scene_id=null where id=?", id);
            jdbc.update("delete from scene_command_receipt where campaign_id=?", id);
            jdbc.update("delete from token_controller where token_id in (select t.id from token t join scene s on s.id=t.scene_id where s.campaign_id=?)", id);
            jdbc.update("delete from movement_receipt where campaign_id=?", id);
            jdbc.update("delete from token where scene_id in (select id from scene where campaign_id=?)", id);
            jdbc.update("delete from scene where campaign_id=?", id);
            jdbc.update("delete from campaign_invitation where campaign_id=?", id);
            jdbc.update("delete from campaign_member where campaign_id=?", id);
            jdbc.update("delete from campaign where id=?", id);
        }
        jdbc.update("delete from account_command_receipt where actor='p02_owner'");
        for (var name : users) { jdbc.update("delete from scene_command_receipt where actor=?", name); jdbc.update("delete from app_user where username=?", name); }
    }
    @Test void dmCreatesEmptySceneWithoutSwitchingAndRetriesOnce() throws Exception {
        var dm = owner(); var id = campaign(dm); var current = dm.read(base(id) + "/scene");
        var request = UUID.randomUUID(); var body = createBody(request, 0, "  Secret preparation  ");
        var created = tree(dm.post(base(id) + "/scenes", body), 200);
        assertEquals("Secret preparation", created.get("name").asString());
        assertEquals(1, created.get("campaignVersion").asLong());
        assertEquals(created, tree(dm.post(base(id) + "/scenes", body), 200));
        var list = dm.read(base(id) + "/scenes"); assertEquals(2, list.get("scenes").size());
        assertEquals(current.get("id"), list.get("currentSceneId"));
        assertEquals(current, dm.read(base(id) + "/scene"));
    }
    @Test void authorizationCsrfAndStrictFieldsProtectEverySceneRoute() throws Exception {
        var dm = owner(); var id = campaign(dm); var player = member(id, "PLAYER"); var outsider = owner();
        var current = dm.read(base(id) + "/scene").get("id").asString();
        var body = createBody(UUID.randomUUID(), 0, "Secret name");
        assertEquals(401, new Browser().get(base(id) + "/scenes").statusCode());
        assertEquals(403, dm.post(base(id) + "/scenes", json.writeValueAsString(body), false).statusCode());
        assertEquals(403, dm.post(base(id) + "/scenes/" + current + "/activate", json.writeValueAsString(command(0)), false).statusCode());
        error(player.get(base(id) + "/scenes"), 403, "FORBIDDEN");
        error(player.post(base(id) + "/scenes", body), 403, "FORBIDDEN");
        error(player.post(base(id) + "/scenes/" + current + "/activate", command(0)), 403, "FORBIDDEN");
        error(outsider.get(base(UUID.randomUUID()) + "/scenes"), 403, "FORBIDDEN");
        for (var value : List.of("-1", "0.5", "\"0\"", "null"))
            error(dm.post(base(id) + "/scenes", "{\"requestId\":\"" + UUID.randomUUID() + "\",\"name\":\"Name\",\"expectedVersion\":" + value + "}", true), 400, "INVALID_INPUT");
        for (var name : List.of(" ", "😀".repeat(101))) error(dm.post(base(id) + "/scenes", createBody(UUID.randomUUID(), 0, name)), 400, "INVALID_INPUT");
        error(dm.post(base(id) + "/scenes", Map.of("name", "Name", "expectedVersion", 0)), 400, "INVALID_INPUT");
        tree(dm.post(base(id) + "/scenes", createBody(UUID.randomUUID(), 0, "😀".repeat(100))), 200);
    }
    @Test void activationPreservesOldSceneAndScopesMovementIncludingReceiptReplay() throws Exception {
        var dm = owner(); var id = campaign(dm); var player = member(id, "PLAYER");
        var original = dm.read(base(id) + "/scene"); UUID first = UUID.fromString(original.get("id").asString());
        UUID token = UUID.randomUUID(); jdbc.update("insert into token(id,scene_id,name,x,y) values (?,?,'Visible hero',1,1)", token, first);
        String playerName = users.iterator().next(); jdbc.update("insert into token_controller(token_id,username) values (?,?)", token, playerName);
        UUID moveRequest = UUID.randomUUID(); var move = Map.of("requestId", moveRequest, "expectedVersion", 0, "x", 7, "y", 8);
        String movePath = base(id) + "/tokens/" + token + "/moves";
        var moved = tree(player.post(movePath, move), 200);
        var saved = player.read(base(id) + "/scene"); assertEquals(1, saved.get("version").asLong());
        var created = tree(dm.post(base(id) + "/scenes", createBody(UUID.randomUUID(), 0, "Unpublished dungeon")), 200);
        UUID second = UUID.fromString(created.get("id").asString()); UUID request = UUID.randomUUID();
        var activated = tree(dm.post(base(id) + "/scenes/" + second + "/activate", command(request, 1)), 200);
        var empty = player.read(base(id) + "/scene"); assertEquals(second.toString(), empty.get("id").asString());
        assertEquals(0, empty.get("tokens").size()); assertEquals(0, empty.get("version").asLong());
        assertEquals(24, empty.get("width").asInt()); assertEquals(16, empty.get("height").asInt()); assertEquals(48, empty.get("gridSize").asInt());
        error(player.post(movePath, move), 404, "NOT_FOUND");
        error(player.post(movePath, Map.of("requestId", moveRequest, "expectedVersion", 0, "x", 9, "y", 9)), 404, "NOT_FOUND");
        error(player.post(movePath, Map.of("requestId", UUID.randomUUID(), "expectedVersion", 1, "x", 9, "y", 9)), 404, "NOT_FOUND");
        assertEquals(404, player.get(base(id) + "/scenes/" + first).statusCode());
        tree(dm.post(base(id) + "/scenes/" + first + "/activate", command(2)), 200);
        assertEquals(saved, player.read(base(id) + "/scene"));
        assertEquals(moved, tree(player.post(movePath, move), 200));
        assertEquals(activated, tree(dm.post(base(id) + "/scenes/" + second + "/activate", command(request, 1)), 200));
        assertEquals(first.toString(), player.read(base(id) + "/scene").get("id").asString(), "Historical activation receipt cannot switch back");
        assertEquals(3, dm.read(base(id) + "/scenes").get("campaignVersion").asLong());
        assertEquals(1, jdbc.queryForObject("select version from scene where id=?", Long.class, first));
        assertEquals(0, jdbc.queryForObject("select version from scene where id=?", Long.class, second));
    }
    @Test void staleVersionsFingerprintNamespacesAndCampaignTargetsAreEnforced() throws Exception {
        var dm = owner(); var id = campaign(dm); var other = campaign(dm);
        UUID request = UUID.randomUUID(); var body = createBody(request, 0, "Second");
        var created = tree(dm.post(base(id) + "/scenes", body), 200); String scene = created.get("id").asString();
        error(dm.post(base(id) + "/scenes", createBody(UUID.randomUUID(), 0, "Stale")), 409, "VERSION_CONFLICT");
        error(dm.post(base(id) + "/scenes", createBody(request, 0, "Changed")), 409, "IDEMPOTENCY_CONFLICT");
        error(dm.post(base(id) + "/scenes/" + scene + "/activate", command(request, 0)), 409, "IDEMPOTENCY_CONFLICT");
        error(dm.post(base(other) + "/scenes", body), 409, "IDEMPOTENCY_CONFLICT");
        error(dm.post(base(other) + "/scenes/" + scene + "/activate", command(0)), 404, "NOT_FOUND");
        error(dm.post(base(id) + "/scenes/" + UUID.randomUUID() + "/activate", command(1)), 404, "NOT_FOUND");
        // P01 and scene commands intentionally use different receipt namespaces.
        tree(dm.post(base(id) + "/invitations", command(request, 1)), 201);
        error(dm.post(base(id) + "/scenes/" + scene + "/activate", command(1)), 409, "VERSION_CONFLICT");
        tree(dm.post(base(id) + "/scenes/" + scene + "/activate", command(2)), 200);
    }
    @Test void currentAuthorizationIsRequiredEvenForHistoricalSceneReceipts() throws Exception {
        var dm = owner(); var id = campaign(dm); UUID request = UUID.randomUUID(); var body = createBody(request, 0, "Preparation");
        var created = tree(dm.post(base(id) + "/scenes", body), 200);
        var activationRequest = UUID.randomUUID(); String activate = base(id) + "/scenes/" + created.get("id").asString() + "/activate";
        tree(dm.post(activate, command(activationRequest, 1)), 200);
        try {
            jdbc.update("update campaign_member set role='PLAYER' where campaign_id=? and username='p02_owner'", id);
            error(dm.post(base(id) + "/scenes", body), 403, "FORBIDDEN");
            error(dm.post(activate, command(activationRequest, 1)), 403, "FORBIDDEN");
            jdbc.update("delete from campaign_member where campaign_id=? and username='p02_owner'", id);
            error(dm.post(base(id) + "/scenes", body), 403, "FORBIDDEN");
            error(dm.get(base(id) + "/scene"), 403, "FORBIDDEN");
        } finally { jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'p02_owner','DM') on conflict (campaign_id,username) do update set role='DM'", id); }
    }
    @Test void concurrentRetriesAndDifferentDmCommandsHaveOneCommittedEffect() throws Exception {
        var dm = owner(); var id = campaign(dm); var secondDm = member(id, "DM");
        var body = createBody(UUID.randomUUID(), 0, "Concurrent");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> dm.post(base(id) + "/scenes", body)); var b = executor.submit(() -> dm.post(base(id) + "/scenes", body));
            assertEquals(tree(a.get(15, TimeUnit.SECONDS), 200), tree(b.get(15, TimeUnit.SECONDS), 200));
        }
        String current = dm.read(base(id) + "/scenes").get("currentSceneId").asString();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> dm.post(base(id) + "/scenes/" + current + "/activate", command(1)));
            var b = executor.submit(() -> secondDm.post(base(id) + "/scenes", createBody(UUID.randomUUID(), 1, "Other command")));
            var statuses = new ArrayList<>(List.of(a.get(15, TimeUnit.SECONDS).statusCode(), b.get(15, TimeUnit.SECONDS).statusCode()));
            Collections.sort(statuses); assertEquals(List.of(200, 409), statuses);
        }
        assertEquals(2, dm.read(base(id) + "/scenes").get("campaignVersion").asLong());
    }
    @Test void globalActorReceiptLockMakesCrossCampaignRaceConflictInsteadOfServerError() throws Exception {
        var dm = owner(); var first = campaign(dm); var second = campaign(dm);
        var body = createBody(UUID.randomUUID(), 0, "Cross campaign retry");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> dm.post(base(first) + "/scenes", body)); var b = executor.submit(() -> dm.post(base(second) + "/scenes", body));
            var statuses = new ArrayList<>(List.of(a.get(15, TimeUnit.SECONDS).statusCode(), b.get(15, TimeUnit.SECONDS).statusCode()));
            Collections.sort(statuses); assertEquals(List.of(200, 409), statuses);
        }
        assertEquals(1, dm.read(base(first) + "/scenes").get("campaignVersion").asLong() + dm.read(base(second) + "/scenes").get("campaignVersion").asLong());
    }
    @Test void receiptFailureRollsBackCreateActivationAndManagementVersion() throws Exception {
        var dm = owner(); var id = campaign(dm);
        var created = service.create(id, "p02_owner", UUID.randomUUID(), 0, "Second");
        var before = dm.read(base(id) + "/scenes"); UUID createRequest = UUID.randomUUID(), activateRequest = UUID.randomUUID();
        jdbc.execute("create function p02_fail_receipt() returns trigger language plpgsql as $$ begin raise exception 'forced scene receipt failure'; end $$");
        jdbc.execute("create trigger p02_fail_receipt before insert on scene_command_receipt for each row execute function p02_fail_receipt()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> service.create(id, "p02_owner", createRequest, 1, "Must roll back"));
            assertEquals(before, dm.read(base(id) + "/scenes"));
            assertThrows(org.springframework.dao.DataAccessException.class, () -> service.activate(id, created.id(), "p02_owner", activateRequest, 1));
            assertEquals(before, dm.read(base(id) + "/scenes"));
            assertEquals(0, jdbc.queryForObject("select count(*) from scene_command_receipt where request_id in (?,?)", Integer.class, createRequest, activateRequest));
        } finally { jdbc.execute("drop trigger p02_fail_receipt on scene_command_receipt"); jdbc.execute("drop function p02_fail_receipt()"); }
        service.activate(id, created.id(), "p02_owner", activateRequest, 1);
        assertEquals(created.id().toString(), dm.read(base(id) + "/scene").get("id").asString());
    }
}
