package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StompPolicyTest {
    @Test void onlyAuthenticatedUserQueueSubscriptionsAreAllowed() {
        assertTrue(StompPolicy.allowed("SUBSCRIBE", true, "/user/queue/scene-changes"));
        assertFalse(StompPolicy.allowed("SUBSCRIBE", false, "/user/queue/scene-changes"));
        assertFalse(StompPolicy.allowed("SUBSCRIBE", true, "/topic/campaigns"));
        assertFalse(StompPolicy.allowed("SUBSCRIBE", true, "/queue/scene-changes"));
        assertFalse(StompPolicy.allowed("SUBSCRIBE", true, "/user/other/queue/scene-changes"));
    }
    @Test void clientsCannotSendOrConnectAnonymously() {
        assertFalse(StompPolicy.allowed("SEND", true, "/app/move"));
        assertFalse(StompPolicy.allowed("CONNECT", false, null));
        assertTrue(StompPolicy.allowed("CONNECT", true, null));
        assertTrue(StompPolicy.allowed("DISCONNECT", true, null));
    }
}
