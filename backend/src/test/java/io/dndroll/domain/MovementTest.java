package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import io.dndroll.domain.Movement.*;

class MovementTest {
    final Token visible = new Token(UUID.randomUUID(), "Adventurer", 2, 3, 0, false, Set.of("player"));
    final Token hidden = new Token(UUID.randomUUID(), "Secret", 8, 9, 0, true, Set.of());
    Command command(long version, int x, int y) { return new Command(UUID.randomUUID(), version, x, y); }
    void rejects(String code, Runnable action) { assertEquals(code, assertThrows(DomainException.class, action::run).code()); }
    @Test void ownerMovesAndIncrementsOnlyTokenVersion() {
        var moved = Movement.move(visible, Role.PLAYER, "player", command(0, 4, 5), 24, 16);
        assertEquals(4, moved.x()); assertEquals(5, moved.y()); assertEquals(1, moved.version());
        assertTrue(Movement.view(moved, Role.PLAYER, "player").canMove());
    }
    @Test void hiddenTokensDoNotBecomePlayerPayloadsOrErrors() {
        assertNull(Movement.view(hidden, Role.PLAYER, "player"));
        rejects("NOT_FOUND", () -> Movement.move(hidden, Role.PLAYER, "player", command(0, 1, 1), 24, 16));
        assertNotNull(Movement.view(hidden, Role.DM, "dm"));
    }
    @Test void visibleButUncontrolledTokenIsForbidden() {
        assertFalse(Movement.view(visible, Role.PLAYER, "other").canMove());
        rejects("FORBIDDEN", () -> Movement.move(visible, Role.PLAYER, "other", command(0, 1, 1), 24, 16));
    }
    @Test void dmCanMoveHiddenToken() { assertEquals(1, Movement.move(hidden, Role.DM, "dm", command(0, 1, 1), 24, 16).version()); }
    @Test void staleVersionConflicts() { rejects("VERSION_CONFLICT", () -> Movement.move(visible, Role.PLAYER, "player", command(1, 1, 1), 24, 16)); }
    @Test void allFourGridEdgesAreChecked() {
        for (var point : new int[][]{{-1, 0}, {0, -1}, {24, 0}, {0, 16}})
            rejects("INVALID_INPUT", () -> Movement.move(visible, Role.PLAYER, "player", command(0, point[0], point[1]), 24, 16));
        assertEquals(23, Movement.move(visible, Role.PLAYER, "player", command(0, 23, 15), 24, 16).x());
    }
}
