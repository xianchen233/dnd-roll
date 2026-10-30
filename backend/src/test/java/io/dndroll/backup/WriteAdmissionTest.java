package io.dndroll.backup;

import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WriteAdmissionTest {
    @Test void closesAdmissionBeforeDrainingAndWaitsForCommit() throws Exception {
        var gate = new WriteAdmission();
        var inFlight = gate.enter();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var drained = executor.submit(() -> gate.pause(Duration.ofSeconds(2)));
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (!gate.paused() && System.nanoTime() < until) Thread.onSpinWait();
            assertTrue(gate.paused());
            assertThrows(IllegalStateException.class, gate::enter);
            assertFalse(drained.isDone(), "cannot snapshot before in-flight commit");
            inFlight.close();
            try (var pause = drained.get(1, TimeUnit.SECONDS)) {
                assertThrows(IllegalStateException.class, gate::enter);
                assertThrows(IllegalStateException.class, () -> gate.pause(Duration.ZERO));
            }
            try (var admitted = gate.enter()) { assertFalse(gate.paused()); }
        } finally { inFlight.close(); }
    }

    @Test void timeoutAndInterruptionReleaseOnlyTheirOwnPause() throws Exception {
        var gate = new WriteAdmission();
        try (var active = gate.enter()) {
            assertThrows(IllegalStateException.class, () -> gate.pause(Duration.ofMillis(10)));
            assertFalse(gate.paused());
            Thread.currentThread().interrupt();
            try { assertThrows(InterruptedException.class, () -> gate.pause(Duration.ofSeconds(1))); }
            finally { Thread.interrupted(); }
            assertFalse(gate.paused());
        }
        var pause = gate.pause(Duration.ZERO);
        pause.close(); pause.close();
        try (var next = gate.pause(Duration.ZERO)) {
            pause.close();
            assertTrue(gate.paused(), "old double close cannot resume another backup");
        }
    }
}
