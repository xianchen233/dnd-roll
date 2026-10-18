package io.dndroll.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import io.dndroll.domain.Movement.Role;

public final class AccountPolicy {
    private AccountPolicy() {}
    public static void credentials(String username, String password) {
        if (username == null || !username.matches("[a-z][a-z0-9_-]{2,31}") || password == null
            || password.codePointCount(0, password.length()) < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new DomainException("INVALID_INPUT", 400, "Username or password does not meet account requirements");
    }
    public static void dm(Role role) {
        if (role != Role.DM) throw new DomainException("FORBIDDEN", 403, "Campaign DM required");
    }
    public static void version(long current, long expected) {
        if (current != expected) throw new DomainException("VERSION_CONFLICT", 409, "Campaign changed; refresh before retrying");
    }
    public static void active(Instant expiresAt, boolean revoked, String usedBy, Instant now) {
        if (revoked || usedBy != null) throw new DomainException("INVITE_UNAVAILABLE", 409, "Invitation is unavailable");
        if (!expiresAt.isAfter(now)) throw new DomainException("INVITE_EXPIRED", 410, "Invitation has expired");
    }
}
