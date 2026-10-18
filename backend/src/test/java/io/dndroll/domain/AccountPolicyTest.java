package io.dndroll.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import io.dndroll.domain.Movement.Role;
class AccountPolicyTest {
    @Test void rejectsInvalidCredentialsWithoutTrimmingOrUtf8Truncation() {
        assertCode("INVALID_INPUT", () -> AccountPolicy.credentials("Abc", "long-password"));
        assertCode("INVALID_INPUT", () -> AccountPolicy.credentials("abc", "short"));
        assertCode("INVALID_INPUT", () -> AccountPolicy.credentials("abc", "密".repeat(25)));
        assertDoesNotThrow(() -> AccountPolicy.credentials("alice_01", " secret-password "));
        assertDoesNotThrow(() -> AccountPolicy.credentials("abc", "密".repeat(24)));
    }
    @Test void requiresDmAndCurrentVersion() {
        assertCode("FORBIDDEN", () -> AccountPolicy.dm(Role.PLAYER));
        assertCode("FORBIDDEN", () -> AccountPolicy.dm(null));
        assertCode("VERSION_CONFLICT", () -> AccountPolicy.version(3, 2));
        assertDoesNotThrow(() -> AccountPolicy.dm(Role.DM));
    }
    @Test void expiryAtExactInstantAndConsumedOrRevokedCannotBeClaimed() {
        var now = Instant.parse("2026-10-18T00:00:00Z");
        assertCode("INVITE_EXPIRED", () -> AccountPolicy.active(now, false, null, now));
        assertCode("INVITE_UNAVAILABLE", () -> AccountPolicy.active(now.plusSeconds(10), true, null, now));
        assertCode("INVITE_UNAVAILABLE", () -> AccountPolicy.active(now.plusSeconds(10), false, "alice", now));
        assertDoesNotThrow(() -> AccountPolicy.active(now.plusSeconds(1), false, null, now));
    }
    static void assertCode(String code, Runnable action) { assertEquals(code, assertThrows(DomainException.class, action::run).code()); }
}
