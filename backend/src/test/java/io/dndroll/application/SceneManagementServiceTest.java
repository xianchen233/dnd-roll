package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.SceneManagementService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SceneManagementServiceTest {
    final UUID campaign = UUID.randomUUID(), original = UUID.randomUUID();
    final Memory store = new Memory();
    int events;
    final SceneManagementService service = new SceneManagementService(store, id -> events++);
    class Memory implements Store {
        long version;
        UUID current = original;
        Role role = Role.DM;
        Map<UUID, Entry> scenes = new HashMap<>(Map.of(original, new Entry(original, "Original")));
        Map<String, Receipt> receipts = new HashMap<>();
        public <T> T actorLocked(String actor, Supplier<T> work) { return work.get(); }
        public <T> T locked(UUID id, Supplier<T> work) { return work.get(); }
        public Role role(UUID id, String actor) { return role; }
        public Listing listing(UUID id) { return new Listing(version, current, List.copyOf(scenes.values())); }
        public Entry entry(UUID id, UUID scene) { return scenes.get(scene); }
        public Receipt sceneReceipt(String actor, UUID request) { return receipts.get(actor + request); }
        public void sceneReceipt(String actor, UUID request, Receipt receipt) { receipts.put(actor + request, receipt); }
        public void createScene(UUID id, Entry entry) { scenes.put(entry.id(), entry); }
        public void activateScene(UUID id, UUID scene) { current = scene; }
        public long advanceCampaign(UUID id) { return ++version; }
    }
    @Test void createTrimsUnicodeNameAndRetryPreservesOriginalResultWithoutSwitchOrEvent() {
        var request = UUID.randomUUID();
        var result = service.create(campaign, "dm", request, 0, "  " + "😀".repeat(100) + "  ");
        assertEquals(result, service.create(campaign, "dm", request, 0, "😀".repeat(100)));
        assertEquals(original, store.current); assertEquals(2, store.scenes.size()); assertEquals(1, events);
        assertEquals(1, result.campaignVersion());
    }
    @Test void replayedActivationDoesNotReactivateHistoricalScene() {
        var created = service.create(campaign, "dm", UUID.randomUUID(), 0, "Second");
        var request = UUID.randomUUID();
        var activated = service.activate(campaign, created.id(), "dm", request, 1);
        service.activate(campaign, original, "dm", UUID.randomUUID(), 2);
        assertEquals(activated, service.activate(campaign, created.id(), "dm", request, 1));
        assertEquals(original, store.current); assertEquals(3, store.version); assertEquals(3, events);
    }
    @Test void roleIsRecheckedBeforeReceiptAndPreparationListing() {
        var request = UUID.randomUUID(); service.create(campaign, "dm", request, 0, "Secret");
        for (var role : Arrays.asList(Role.PLAYER, null)) {
            store.role = role;
            assertCode("FORBIDDEN", () -> service.create(campaign, "dm", request, 0, "Secret"));
            assertCode("FORBIDDEN", () -> service.list(campaign, "dm"));
            assertCode("FORBIDDEN", () -> service.activate(campaign, original, "dm", UUID.randomUUID(), 1));
        }
        assertEquals(1, events);
    }
    @Test void receiptFingerprintCoversCampaignKindTargetNameAndVersion() {
        var request = UUID.randomUUID(); service.create(campaign, "dm", request, 0, "Second");
        assertCode("IDEMPOTENCY_CONFLICT", () -> service.create(UUID.randomUUID(), "dm", request, 0, "Second"));
        assertCode("IDEMPOTENCY_CONFLICT", () -> service.activate(campaign, original, "dm", request, 0));
        assertCode("IDEMPOTENCY_CONFLICT", () -> service.create(campaign, "dm", request, 0, "Third"));
        assertCode("IDEMPOTENCY_CONFLICT", () -> service.create(campaign, "dm", request, 1, "Second"));
        var activateRequest = UUID.randomUUID(); service.activate(campaign, original, "dm", activateRequest, 1);
        assertCode("IDEMPOTENCY_CONFLICT", () -> service.activate(campaign, UUID.randomUUID(), "dm", activateRequest, 1));
        assertEquals(2, events);
    }
    @Test void invalidInputStaleVersionAndUnknownSceneDoNotMutate() {
        for (String name : Arrays.asList(null, "  ", "😀".repeat(101)))
            assertCode("INVALID_INPUT", () -> service.create(campaign, "dm", UUID.randomUUID(), 0, name));
        assertCode("INVALID_INPUT", () -> service.create(campaign, "dm", null, 0, "Name"));
        assertCode("INVALID_INPUT", () -> service.activate(campaign, original, "dm", UUID.randomUUID(), -1));
        assertCode("VERSION_CONFLICT", () -> service.create(campaign, "dm", UUID.randomUUID(), 1, "Name"));
        assertCode("NOT_FOUND", () -> service.activate(campaign, UUID.randomUUID(), "dm", UUID.randomUUID(), 0));
        assertEquals(0, store.version); assertEquals(1, store.scenes.size()); assertEquals(0, events);
    }
    @Test void activatingCurrentSceneAdvancesManagementVersionExactlyOnce() {
        var request = UUID.randomUUID();
        var result = service.activate(campaign, original, "dm", request, 0);
        assertEquals(result, service.activate(campaign, original, "dm", request, 0));
        assertEquals(1, store.version); assertEquals(1, events);
    }
    static void assertCode(String code, Runnable action) { assertEquals(code, assertThrows(DomainException.class, action::run).code()); }
}
