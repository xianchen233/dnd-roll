package io.dndroll.web;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SessionSocketsTest {
    @Test void invalidatingOneSessionClosesAllItsSocketsExactlyOnce() {
        var sockets = new SessionSockets(); var one = new AtomicInteger(); var two = new AtomicInteger(); var other = new AtomicInteger();
        sockets.register("session", "one", one::incrementAndGet); sockets.register("session", "two", two::incrementAndGet);
        sockets.register("other", "three", other::incrementAndGet);
        sockets.close("session"); sockets.close("session");
        assertEquals(1, one.get()); assertEquals(1, two.get()); assertEquals(0, other.get());
    }
    @Test void alreadyClosedSocketsAreRemoved() {
        var sockets = new SessionSockets(); var closed = new AtomicInteger();
        sockets.register("session", "one", closed::incrementAndGet); sockets.remove("session", "one"); sockets.close("session");
        assertEquals(0, closed.get());
    }
}
