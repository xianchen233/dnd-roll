package io.dndroll.web;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
class AccountSocketsTest {
    @Test void closesAllSessionsOfRemovedAccountWhileKeepingOtherUsers() {
        var sockets = new SessionSockets();
        var removed = new AtomicInteger(); var other = new AtomicInteger();
        sockets.register("a", "socket-a", "alice", removed::incrementAndGet);
        sockets.register("b", "socket-b", "alice", removed::incrementAndGet);
        sockets.register("c", "socket-c", "bob", other::incrementAndGet);
        sockets.closeUser("alice"); sockets.closeUser("alice");
        assertEquals(2, removed.get()); assertEquals(0, other.get());
        sockets.close("c"); assertEquals(1, other.get());
    }
}
