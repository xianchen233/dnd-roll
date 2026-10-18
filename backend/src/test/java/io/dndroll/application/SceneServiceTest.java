package io.dndroll.application;

import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.*;
import io.dndroll.application.SceneService.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SceneServiceTest {
    final UUID campaign = UUID.randomUUID();
    final UUID tokenId = UUID.randomUUID();
    final MemoryStore store = new MemoryStore();
    int events;
    final SceneService service = new SceneService(store, id -> events++);
    final Command first = new Command(UUID.randomUUID(), 0, 4, 5);
    class MemoryStore implements Store {
        Token token = new Token(tokenId, "Hero", 2, 3, 0, false, Set.of("player"));
        Map<String, Receipt> receipts = new HashMap<>();
        public List<CampaignView> campaigns(String actor) { return List.of(); }
        public Role role(UUID id, String actor) { return actor.equals("outsider") ? null : Role.PLAYER; }
        public Scene scene(UUID id) { return new Scene(UUID.randomUUID(), campaign, "Scene", 24, 16, 48, 0, List.of(token)); }
        public Receipt receipt(UUID id, String actor, UUID requestId) { return receipts.get(actor + requestId); }
        public void save(UUID id, String actor, Token token, Command command, TokenView response) {
            this.token = token; receipts.put(actor + command.requestId(), new Receipt(token.id(), command, response));
        }
        public synchronized <T> T locked(UUID id, Supplier<T> action) { return action.get(); }
    }
    @Test void identicalRetryReturnsOriginalResponseEvenAfterFurtherMoves() {
        var result = service.move(campaign, tokenId, "player", first);
        service.move(campaign, tokenId, "player", new Command(UUID.randomUUID(), 1, 8, 8));
        assertEquals(result, service.move(campaign, tokenId, "player", first));
        assertEquals(2, store.token.version()); assertEquals(2, events);
    }
    @Test void reusingRequestKeyWithDifferentPayloadConflicts() {
        service.move(campaign, tokenId, "player", first);
        var error = assertThrows(DomainException.class, () -> service.move(campaign, tokenId, "player", new Command(first.requestId(), 0, 9, 9)));
        assertEquals("IDEMPOTENCY_CONFLICT", error.code()); assertEquals(1, store.token.version());
    }
    @Test void requestKeyCannotBeReusedForAnotherToken() {
        service.move(campaign, tokenId, "player", first);
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(DomainException.class, () -> service.move(campaign, UUID.randomUUID(), "player", first)).code());
    }
    @Test void membershipIsCheckedBeforeSnapshotOrMutation() {
        assertEquals("FORBIDDEN", assertThrows(DomainException.class, () -> service.snapshot(campaign, "outsider")).code());
        assertEquals("FORBIDDEN", assertThrows(DomainException.class, () -> service.move(campaign, tokenId, "outsider", first)).code());
        assertEquals(0, store.token.version()); assertEquals(0, events);
    }
}
