package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.CookieManager;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import io.dndroll.application.SceneService;
import io.dndroll.domain.Movement.Command;
import io.dndroll.persistence.DemoBootstrap;
import org.springframework.boot.DefaultApplicationArguments;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=demo", "demo.dm.username=it-dm", "demo.dm.password=integration-dm-secret",
    "demo.player.username=it-player", "demo.player.password=integration-player-secret"})
class FirstSliceIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired SceneService service;
    @Autowired DemoBootstrap bootstrap;
    final JsonMapper json = JsonMapper.builder().build();
    @DynamicPropertySource static void database(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", () -> required("TEST_DATABASE_URL"));
        props.add("spring.datasource.username", () -> required("TEST_DATABASE_USERNAME"));
        props.add("spring.datasource.password", () -> required("TEST_DATABASE_PASSWORD"));
    }
    static String required(String key) {
        var value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Integration tests require " + key + " and a real PostgreSQL database");
        return value;
    }
    class Browser {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String csrf;
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(String path, String body, boolean withCsrf) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
            if (withCsrf) request.header("X-CSRF-TOKEN", csrf);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        void csrf() throws Exception { csrf = json.readTree(get("/api/auth/csrf").body()).get("token").asString(); }
        void login(String username, String password) throws Exception {
            csrf(); assertEquals(200, post("/api/auth/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", true).statusCode()); csrf();
        }
        JsonNode tree(String path) throws Exception { var response = get(path); assertEquals(200, response.statusCode()); return json.readTree(response.body()); }
    }
    @Test void loginCsrfVisibilityPermissionsAndDurableIdempotentMovement() throws Exception {
        Browser anonymous = new Browser();
        assertEquals(401, anonymous.get("/api/auth/me").statusCode());
        assertEquals(403, anonymous.post("/api/auth/login", "{}", false).statusCode());
        anonymous.csrf(); assertEquals(401, anonymous.post("/api/auth/login", "{\"username\":\"it-player\",\"password\":\"wrong\"}", true).statusCode());
        Browser dm = new Browser(); dm.login("it-dm", "integration-dm-secret");
        Browser player = new Browser(); player.login("it-player", "integration-player-secret");
        String campaign = player.tree("/api/campaigns").get(0).get("id").asString();
        String scenePath = "/api/campaigns/" + campaign + "/scene";
        var dmScene = dm.tree(scenePath); var playerScene = player.tree(scenePath);
        assertEquals(2, dmScene.get("tokens").size()); assertEquals(1, playerScene.get("tokens").size());
        JsonNode hero = playerScene.get("tokens").get(0);
        JsonNode hidden = null;
        for (JsonNode t : dmScene.get("tokens")) if (!t.get("id").asString().equals(hero.get("id").asString())) hidden = t;
        assertNotNull(hidden);
        String hiddenPath = "/api/campaigns/" + campaign + "/tokens/" + hidden.get("id").asString() + "/moves";
        String hiddenCommand = command(UUID.randomUUID().toString(), hidden.get("version").asLong(), 1, 1);
        assertEquals(404, player.post(hiddenPath, hiddenCommand, true).statusCode());
        String token = hero.get("id").asString(); String movePath = "/api/campaigns/" + campaign + "/tokens/" + token + "/moves";
        long version = hero.get("version").asLong(); String requestId = UUID.randomUUID().toString();
        long sceneVersion = playerScene.get("version").asLong();
        String body = command(requestId, version, 4, 5);
        assertEquals(403, player.post(movePath, body, false).statusCode());
        var first = player.post(movePath, body, true); assertEquals(200, first.statusCode());
        assertEquals(sceneVersion + 1, player.tree(scenePath).get("version").asLong(), "A committed move advances the scene snapshot version");
        assertEquals(400, player.post(movePath, "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":" + (version + 1) + ",\"x\":1.5,\"y\":2}", true).statusCode());
        assertEquals(400, player.post(movePath, "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":" + (version + 1) + ".5,\"x\":1,\"y\":2}", true).statusCode());
        assertEquals(400, player.post(movePath, "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":" + (version + 1) + ",\"y\":2}", true).statusCode());
        assertEquals(first.body(), player.post(movePath, body, true).body());
        assertEquals(sceneVersion + 1, player.tree(scenePath).get("version").asLong(), "Idempotent retries must not advance scene version");
        jdbc.update("delete from token_controller where token_id=? and username='it-player'", UUID.fromString(token));
        try {
            assertFalse(player.tree(scenePath).get("tokens").get(0).get("canMove").asBoolean());
            assertEquals(403, player.post(movePath, command(UUID.randomUUID().toString(), version + 1, 2, 2), true).statusCode());
            assertEquals(403, player.post(movePath, body, true).statusCode());
        } finally { jdbc.update("insert into token_controller(token_id,username) values (?,'it-player')", UUID.fromString(token)); }
        assertEquals(409, player.post(movePath, command(requestId, version, 8, 8), true).statusCode());
        assertEquals(409, player.post(movePath, command(UUID.randomUUID().toString(), version, 8, 8), true).statusCode());
        assertEquals(400, player.post(movePath, command(UUID.randomUUID().toString(), version + 1, 24, 0), true).statusCode());
        assertEquals(version + 1, jdbc.queryForObject("select version from token where id=?", Long.class, UUID.fromString(token)));
        assertEquals(1, jdbc.queryForObject("select count(*) from movement_receipt where request_id=?", Integer.class, UUID.fromString(requestId)));
        Browser reconnected = new Browser(); reconnected.login("it-player", "integration-player-secret");
        assertEquals(4, reconnected.tree(scenePath).get("tokens").get(0).get("x").asInt());
        assertEquals(403, player.get("/api/campaigns/" + UUID.randomUUID() + "/scene").statusCode());
        assertEquals(204, player.post("/api/auth/logout", "{}", true).statusCode());
        assertEquals(401, player.get("/api/auth/me").statusCode());
    }
    @Test void concurrentIdenticalRetriesCommitOnce() throws Exception {
        Browser player = new Browser(); player.login("it-player", "integration-player-secret");
        String campaign = player.tree("/api/campaigns").get(0).get("id").asString();
        var token = player.tree("/api/campaigns/" + campaign + "/scene").get("tokens").get(0);
        String path = "/api/campaigns/" + campaign + "/tokens/" + token.get("id").asString() + "/moves";
        String requestId = UUID.randomUUID().toString(); long before = token.get("version").asLong();
        String body = command(requestId, before, 3, 4);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> player.post(path, body, true)); var b = executor.submit(() -> player.post(path, body, true));
            var left = a.get(10, TimeUnit.SECONDS); var right = b.get(10, TimeUnit.SECONDS);
            assertEquals(200, left.statusCode()); assertEquals(200, right.statusCode()); assertEquals(left.body(), right.body());
        }
        assertEquals(before + 1, player.tree("/api/campaigns/" + campaign + "/scene").get("tokens").get(0).get("version").asLong());
    }
    @Test void concurrentDifferentCommandsForOneVersionHaveOneWinner() throws Exception {
        Browser player = new Browser(); player.login("it-player", "integration-player-secret");
        String campaign = player.tree("/api/campaigns").get(0).get("id").asString();
        var token = player.tree("/api/campaigns/" + campaign + "/scene").get("tokens").get(0);
        String path = "/api/campaigns/" + campaign + "/tokens/" + token.get("id").asString() + "/moves";
        long before = token.get("version").asLong();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> player.post(path, command(UUID.randomUUID().toString(), before, 7, 7), true));
            var b = executor.submit(() -> player.post(path, command(UUID.randomUUID().toString(), before, 8, 8), true));
            var statuses = new ArrayList<>(List.of(a.get(10, TimeUnit.SECONDS).statusCode(), b.get(10, TimeUnit.SECONDS).statusCode()));
            Collections.sort(statuses); assertEquals(List.of(200, 409), statuses);
        }
        assertEquals(before + 1, player.tree("/api/campaigns/" + campaign + "/scene").get("tokens").get(0).get("version").asLong());
    }
    @Test void receiptFailureRollsBackTokenMutation() throws Exception {
        Browser player = new Browser(); player.login("it-player", "integration-player-secret");
        UUID campaign = UUID.fromString(player.tree("/api/campaigns").get(0).get("id").asString());
        var token = player.tree("/api/campaigns/" + campaign + "/scene").get("tokens").get(0);
        UUID tokenId = UUID.fromString(token.get("id").asString()); long before = token.get("version").asLong();
        UUID requestId = UUID.randomUUID();
        jdbc.execute("create function it_fail_receipt() returns trigger language plpgsql as $$ begin raise exception 'forced receipt failure'; end $$");
        jdbc.execute("create trigger it_fail_receipt before insert on movement_receipt for each row execute function it_fail_receipt()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> service.move(campaign, tokenId, "it-player", new Command(requestId, before, 9, 9)));
            assertEquals(before, jdbc.queryForObject("select version from token where id=?", Long.class, tokenId));
            assertEquals(0, jdbc.queryForObject("select count(*) from movement_receipt where request_id=?", Integer.class, requestId));
        } finally {
            jdbc.execute("drop trigger it_fail_receipt on movement_receipt"); jdbc.execute("drop function it_fail_receipt()");
        }
    }
    @Test void restartingDemoDoesNotRestoreRevokedMembershipOrControl() throws Exception {
        Browser player = new Browser(); player.login("it-player", "integration-player-secret");
        UUID campaign = UUID.fromString(player.tree("/api/campaigns").get(0).get("id").asString());
        UUID token = UUID.fromString(player.tree("/api/campaigns/" + campaign + "/scene").get("tokens").get(0).get("id").asString());
        jdbc.update("delete from token_controller where token_id=? and username='it-player'", token);
        jdbc.update("delete from campaign_member where campaign_id=? and username='it-player'", campaign);
        try {
            bootstrap.run(new DefaultApplicationArguments(new String[0]));
            assertEquals(0, jdbc.queryForObject("select count(*) from token_controller where token_id=? and username='it-player'", Integer.class, token), "Restart must preserve revoked control");
            assertEquals(0, jdbc.queryForObject("select count(*) from campaign_member where campaign_id=? and username='it-player'", Integer.class, campaign), "Restart must preserve revoked membership");
        } finally {
            jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,'it-player','PLAYER') on conflict do nothing", campaign);
            jdbc.update("insert into token_controller(token_id,username) values (?,'it-player') on conflict do nothing", token);
        }
    }
    String command(String requestId, long version, int x, int y) { return "{\"requestId\":\"" + requestId + "\",\"expectedVersion\":" + version + ",\"x\":" + x + ",\"y\":" + y + "}"; }
}
