package io.dndroll.application;

import io.dndroll.domain.AccountPolicy;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;

/** Campaign scene commands share the campaign management version, not scene versions. */
public final class SceneManagementService {
    public record Entry(UUID id, String name) {}
    public record Listing(long campaignVersion, UUID currentSceneId, List<Entry> scenes) {}
    public record Result(UUID id, String name, long campaignVersion) {}
    public record Receipt(String kind, UUID campaignId, UUID target, String name, long expectedVersion, Result result) {}
    public interface Store {
        <T> T actorLocked(String actor, Supplier<T> work);
        <T> T locked(UUID campaignId, Supplier<T> work);
        Role role(UUID campaignId, String actor);
        Listing listing(UUID campaignId);
        Entry entry(UUID campaignId, UUID sceneId);
        Receipt sceneReceipt(String actor, UUID requestId);
        void sceneReceipt(String actor, UUID requestId, Receipt receipt);
        void createScene(UUID campaignId, Entry entry);
        void activateScene(UUID campaignId, UUID sceneId);
        long advanceCampaign(UUID campaignId);
    }
    private final Store store;
    private final SceneService.Events events;
    public SceneManagementService(Store store, SceneService.Events events) { this.store = store; this.events = events; }
    public Listing list(UUID campaignId, String actor) {
        return store.locked(campaignId, () -> { AccountPolicy.dm(store.role(campaignId, actor)); return store.listing(campaignId); });
    }
    public Result create(UUID campaignId, String actor, UUID requestId, long expectedVersion, String name) {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty() || clean.codePointCount(0, clean.length()) > 100) throw invalid();
        return command("CREATE", campaignId, null, actor, requestId, expectedVersion, clean);
    }
    public Result activate(UUID campaignId, UUID sceneId, String actor, UUID requestId, long expectedVersion) {
        if (sceneId == null) throw invalid();
        return command("ACTIVATE", campaignId, sceneId, actor, requestId, expectedVersion, "");
    }
    private Result command(String kind, UUID campaignId, UUID target, String actor, UUID requestId, long expectedVersion, String name) {
        if (requestId == null || expectedVersion < 0) throw invalid();
        // Actor lock serializes globally keyed scene receipts, even across campaigns.
        return store.actorLocked(actor, () -> store.locked(campaignId, () -> {
            AccountPolicy.dm(store.role(campaignId, actor));
            var receipt = store.sceneReceipt(actor, requestId);
            if (receipt != null) {
                if (!receipt.kind().equals(kind) || !receipt.campaignId().equals(campaignId) || !Objects.equals(receipt.target(), target)
                    || !receipt.name().equals(name) || receipt.expectedVersion() != expectedVersion)
                    throw new DomainException("IDEMPOTENCY_CONFLICT", 409, "Request ID was already used with different parameters");
                return receipt.result();
            }
            Entry entry;
            if (kind.equals("ACTIVATE")) {
                entry = store.entry(campaignId, target);
                if (entry == null) throw new DomainException("NOT_FOUND", 404, "Scene not found");
            } else entry = new Entry(UUID.randomUUID(), name);
            AccountPolicy.version(store.listing(campaignId).campaignVersion(), expectedVersion);
            if (kind.equals("CREATE")) store.createScene(campaignId, entry);
            else store.activateScene(campaignId, target);
            var result = new Result(entry.id(), entry.name(), store.advanceCampaign(campaignId));
            store.sceneReceipt(actor, requestId, new Receipt(kind, campaignId, target, name, expectedVersion, result));
            events.changed(campaignId);
            return result;
        }));
    }
    private static DomainException invalid() { return new DomainException("INVALID_INPUT", 400, "Invalid scene command"); }
}
