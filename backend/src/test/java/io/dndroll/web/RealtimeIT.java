package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=demo", "demo.dm.username=it-dm", "demo.dm.password=integration-dm-secret",
    "demo.player.username=it-player", "demo.player.password=integration-player-secret"})
class RealtimeIT {
    @LocalServerPort int port;
    @Autowired SimpUserRegistry registry;
    final JsonMapper json = JsonMapper.builder().build();
    @DynamicPropertySource static void database(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", () -> FirstSliceIT.required("TEST_DATABASE_URL"));
        props.add("spring.datasource.username", () -> FirstSliceIT.required("TEST_DATABASE_USERNAME"));
        props.add("spring.datasource.password", () -> FirstSliceIT.required("TEST_DATABASE_PASSWORD"));
    }
    class Session implements AutoCloseable {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String csrf;
        WebSocket socket;
        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        CountDownLatch closed = new CountDownLatch(1);
        StringBuilder fragments = new StringBuilder();
        String get(String path) throws Exception { var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(), HttpResponse.BodyHandlers.ofString()); assertEquals(200, response.statusCode()); return response.body(); }
        HttpResponse<String> post(String path, String body) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json").header("X-CSRF-TOKEN", csrf).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()); }
        Session(String username, String password) throws Exception {
            csrf = json.readTree(get("/api/auth/csrf")).get("token").asString();
            assertEquals(200, post("/api/auth/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}").statusCode());
            csrf = json.readTree(get("/api/auth/csrf")).get("token").asString();
        }
        void connect(boolean validCsrf) throws Exception {
            socket = client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port + "/ws"), new WebSocket.Listener() {
                public void onOpen(WebSocket ws) { ws.request(1); }
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    fragments.append(data); if (last) { frames.add(fragments.toString()); fragments.setLength(0); }
                    ws.request(1); return null;
                }
                public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) { closed.countDown(); return null; }
            }).get(5, TimeUnit.SECONDS);
            send("CONNECT\naccept-version:1.2\nhost:localhost\nX-CSRF-TOKEN:" + (validCsrf ? csrf : "wrong") + "\n\n\0");
        }
        void send(String frame) { socket.sendText(frame, true).join(); }
        String next() throws Exception { String value = frames.poll(5, TimeUnit.SECONDS); assertNotNull(value, "Expected a STOMP frame"); return value; }
        public void close() { if (socket != null) socket.abort(); }
    }
    @Test void authenticatedUserQueuesReceiveOnlySafeCampaignInvalidations() throws Exception {
        try (Session dm = new Session("it-dm", "integration-dm-secret"); Session player = new Session("it-player", "integration-player-secret")) {
            dm.connect(true); player.connect(true); assertTrue(dm.next().startsWith("CONNECTED")); assertTrue(player.next().startsWith("CONNECTED"));
            dm.send("SUBSCRIBE\nid:dm-scene\ndestination:/user/queue/scene-changes\n\n\0");
            player.send("SUBSCRIBE\nid:player-scene\ndestination:/user/queue/scene-changes\n\n\0");
            awaitSubscription("it-dm"); awaitSubscription("it-player");
            String campaign = json.readTree(dm.get("/api/campaigns")).get(0).get("id").asString();
            var tokens = json.readTree(dm.get("/api/campaigns/" + campaign + "/scene")).get("tokens");
            var hidden = tokens.get(0).get("name").asString().equals("Hidden enemy") ? tokens.get(0) : tokens.get(1);
            var response = dm.post("/api/campaigns/" + campaign + "/tokens/" + hidden.get("id").asString() + "/moves",
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":" + hidden.get("version").asLong() + ",\"x\":10,\"y\":7}");
            assertEquals(200, response.statusCode());
            for (Session session : List.of(dm, player)) {
                String frame = session.next(); assertTrue(frame.startsWith("MESSAGE"));
                String body = frame.substring(frame.indexOf("\n\n") + 2).replace("\0", "");
                var event = json.readTree(body); assertEquals(2, event.size());
                assertEquals("SCENE_CHANGED", event.get("type").asString()); assertEquals(campaign, event.get("campaignId").asString());
                assertFalse(frame.contains(hidden.get("id").asString())); assertFalse(frame.contains("Hidden enemy"));
            }
        }
    }
    @Test void invalidCsrfArbitrarySubscriptionsAndClientSendAreRejected() throws Exception {
        try (Session session = new Session("it-player", "integration-player-secret")) {
            session.connect(false); assertTrue(session.next().startsWith("ERROR"));
        }
        for (String forbidden : List.of("SUBSCRIBE\nid:bad\ndestination:/topic/anything\n\n\0", "SEND\ndestination:/app/move\n\n{}\0")) {
            try (Session session = new Session("it-player", "integration-player-secret")) {
                session.connect(true); assertTrue(session.next().startsWith("CONNECTED")); session.send(forbidden); assertTrue(session.next().startsWith("ERROR"));
            }
        }
    }
    @Test void logoutClosesExistingRealtimeSession() throws Exception {
        try (Session session = new Session("it-player", "integration-player-secret")) {
            session.connect(true); assertTrue(session.next().startsWith("CONNECTED"));
            assertEquals(204, session.post("/api/auth/logout", "{}").statusCode());
            assertTrue(session.closed.await(3, TimeUnit.SECONDS), "Logged-out sessions must lose existing sockets");
        }
    }
    @Test void anonymousWebsocketHandshakeIsRejected() {
        var error = assertThrows(ExecutionException.class, () -> HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/ws"), new WebSocket.Listener() {}).get(5, TimeUnit.SECONDS));
        assertInstanceOf(WebSocketHandshakeException.class, error.getCause());
        assertEquals(401, ((WebSocketHandshakeException) error.getCause()).getResponse().statusCode());
    }
    void awaitSubscription(String username) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < until) {
            var user = registry.getUser(username);
            if (user != null && user.getSessions().stream().anyMatch(s -> !s.getSubscriptions().isEmpty())) return;
            Thread.sleep(20);
        }
        fail("STOMP subscription did not register");
    }
}
