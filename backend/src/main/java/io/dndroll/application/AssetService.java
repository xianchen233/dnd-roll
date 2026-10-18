package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;

/** Private DM preparation. Asset metadata is deliberately separate from shared snapshots. */
public final class AssetService {
    public record Asset(UUID id, String mediaType, long byteSize, int width, int height) {}
    public record Stored(Asset asset, String storageKey, String sha256) {}
    public record Listing(long campaignVersion, List<Asset> assets) {}
    public record UploadResult(Asset asset, long campaignVersion) {}
    public record MapConfig(UUID assetId, Integer gridSize, Integer offsetX, Integer offsetY, Double distancePerCell) {}
    public record MapResult(UUID sceneId, long sceneVersion, MapConfig map) {}
    public record Receipt(String fingerprint, UploadResult upload, MapResult map) {}
    public record Content(String mediaType, byte[] bytes) {}
    public interface Store {
        <T> T actorLocked(String actor, Supplier<T> work);
        <T> T locked(UUID campaign, Supplier<T> work);
        Role role(UUID campaign, String actor);
        Listing assets(UUID campaign);
        Stored asset(UUID campaign, UUID id);
        void insertAsset(UUID campaign, Stored asset);
        long advanceCampaign(UUID campaign);
        MapResult map(UUID campaign, UUID scene);
        MapResult setMap(UUID campaign, UUID scene, MapConfig map);
        Receipt assetReceipt(String actor, UUID request);
        void assetReceipt(String actor, UUID request, Receipt receipt);
    }
    public interface Files {
        void publish(String key, byte[] bytes);
        byte[] read(String key);
        void discard(String key);
    }
    private final Store store;
    private final Files files;
    private final SceneService.Events events;
    public AssetService(Store store, Files files, SceneService.Events events) { this.store = store; this.files = files; this.events = events; }
    public Listing list(UUID campaign, String actor) {
        return store.locked(campaign, () -> { AccountPolicy.dm(store.role(campaign, actor)); return store.assets(campaign); });
    }
    public Content content(UUID campaign, UUID asset, String actor) {
        return store.locked(campaign, () -> {
            AccountPolicy.dm(store.role(campaign, actor));
            var stored = requireAsset(campaign, asset);
            return new Content(stored.asset().mediaType(), files.read(stored.storageKey()));
        });
    }
    public MapResult map(UUID campaign, UUID scene, String actor) {
        return store.locked(campaign, () -> { AccountPolicy.dm(store.role(campaign, actor)); return requireScene(campaign, scene); });
    }
    public UploadResult upload(UUID campaign, String actor, UUID request, long expectedVersion, byte[] bytes, String mediaType) {
        command(request, expectedVersion);
        // Validate only after current authorization, including on retries.
        Stored[] published = {null};
        try {
            return store.actorLocked(actor, () -> store.locked(campaign, () -> {
                AccountPolicy.dm(store.role(campaign, actor));
                var image = AssetImage.validate(bytes, mediaType);
                String fingerprint = "UPLOAD|" + campaign + "|" + expectedVersion + "|" + image.mediaType() + "|" + image.sha256();
                var previous = receipt(actor, request, fingerprint);
                if (previous != null) return previous.upload();
                AccountPolicy.version(store.assets(campaign).campaignVersion(), expectedVersion);
                var asset = new Asset(UUID.randomUUID(), image.mediaType(), image.bytes().length, image.width(), image.height());
                String key = UUID.randomUUID().toString();
                published[0] = new Stored(asset, key, image.sha256());
                files.publish(key, image.bytes());
                store.insertAsset(campaign, published[0]);
                var result = new UploadResult(asset, store.advanceCampaign(campaign));
                store.assetReceipt(actor, request, new Receipt(fingerprint, result, null));
                events.changed(campaign);
                return result;
            }));
        } catch (RuntimeException | Error failure) {
            if (published[0] != null) {
                try {
                    // A lost commit acknowledgment can throw after PostgreSQL committed.
                    // A fresh campaign lock waits for the original server transaction to
                    // finish; an unlocked MVCC read could still see an uncommitted absence.
                    boolean committed = store.locked(campaign, () -> {
                        var saved = store.asset(campaign, published[0].asset().id());
                        return saved != null && saved.storageKey().equals(published[0].storageKey());
                    });
                    if (!committed) files.discard(published[0].storageKey());
                } catch (RuntimeException | Error cleanup) {
                    // Unknown outcome: retain the opaque file and preserve the original
                    // failure. No committed metadata means it still cannot be downloaded.
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }
    public MapResult configure(UUID campaign, UUID scene, String actor, UUID request, long expectedVersion, MapConfig map) {
        command(request, expectedVersion);
        return store.actorLocked(actor, () -> store.locked(campaign, () -> {
            AccountPolicy.dm(store.role(campaign, actor));
            validMap(map);
            String fingerprint = "MAP|" + campaign + "|" + scene + "|" + expectedVersion + "|" + map;
            var previous = receipt(actor, request, fingerprint);
            if (previous != null) return previous.map();
            var current = requireScene(campaign, scene);
            if (map != null) requireAsset(campaign, map.assetId());
            AccountPolicy.version(current.sceneVersion(), expectedVersion);
            var result = store.setMap(campaign, scene, map);
            store.assetReceipt(actor, request, new Receipt(fingerprint, null, result));
            events.changed(campaign);
            return result;
        }));
    }
    private Receipt receipt(String actor, UUID request, String fingerprint) {
        var receipt = store.assetReceipt(actor, request);
        if (receipt != null && !receipt.fingerprint().equals(fingerprint))
            throw new DomainException("IDEMPOTENCY_CONFLICT", 409, "Request ID was already used with different parameters");
        return receipt;
    }
    private Stored requireAsset(UUID campaign, UUID asset) {
        var stored = store.asset(campaign, asset);
        if (stored == null) throw new DomainException("NOT_FOUND", 404, "Asset not found");
        return stored;
    }
    private MapResult requireScene(UUID campaign, UUID scene) {
        var result = store.map(campaign, scene);
        if (result == null) throw new DomainException("NOT_FOUND", 404, "Scene not found");
        return result;
    }
    private static void command(UUID request, long expectedVersion) { if (request == null || expectedVersion < 0) throw invalid(); }
    private static void validMap(MapConfig map) {
        if (map == null) return;
        if (map.assetId() == null || map.gridSize() == null || map.offsetX() == null || map.offsetY() == null || map.distancePerCell() == null
            || map.gridSize() < 8 || map.gridSize() > 512 || map.offsetX() < -8192 || map.offsetX() > 8192 || map.offsetY() < -8192 || map.offsetY() > 8192
            || !Double.isFinite(map.distancePerCell()) || map.distancePerCell() < 0.01 || map.distancePerCell() > 10000) throw invalid();
    }
    private static DomainException invalid() { return new DomainException("INVALID_INPUT", 400, "Invalid asset command"); }
}
