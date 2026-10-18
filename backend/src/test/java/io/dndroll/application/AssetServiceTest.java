package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.AssetService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AssetServiceTest {
    final UUID campaign = UUID.randomUUID(), scene = UUID.randomUUID();
    final Memory store = new Memory();
    int events;
    final AssetService service = new AssetService(store, store, id -> events++);
    final byte[] png = png();
    static byte[] png() {
        try { var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(3,2,BufferedImage.TYPE_INT_RGB), "png", out); return out.toByteArray(); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    class Memory implements Store, Files {
        long campaignVersion, sceneVersion;
        Role role = Role.DM;
        MapConfig map;
        boolean receiptFailure, fileFailure;
        List<String> locks = new ArrayList<>();
        Map<UUID, Stored> assets = new HashMap<>();
        Map<String, Receipt> receipts = new HashMap<>();
        Map<String, byte[]> files = new HashMap<>();
        public <T> T actorLocked(String actor, Supplier<T> work) { locks.add("actor"); return work.get(); }
        public <T> T locked(UUID id, Supplier<T> work) { locks.add("campaign"); return work.get(); }
        public Role role(UUID id, String actor) { return role; }
        public Listing assets(UUID id) { return new Listing(campaignVersion, assets.values().stream().map(Stored::asset).toList()); }
        public Stored asset(UUID id, UUID asset) { return campaign.equals(id) ? assets.get(asset) : null; }
        public void insertAsset(UUID id, Stored asset) { assets.put(asset.asset().id(), asset); }
        public long advanceCampaign(UUID id) { return ++campaignVersion; }
        public MapResult map(UUID id, UUID target) { return campaign.equals(id) && scene.equals(target) ? new MapResult(scene, sceneVersion, map) : null; }
        public MapResult setMap(UUID id, UUID target, MapConfig value) { map = value; sceneVersion++; return map(id, target); }
        public Receipt assetReceipt(String actor, UUID request) { return receipts.get(actor + request); }
        public void assetReceipt(String actor, UUID request, Receipt receipt) { if (receiptFailure) throw new IllegalStateException("forced receipt failure"); receipts.put(actor + request, receipt); }
        public void publish(String key, byte[] bytes) { files.put(key, bytes); if (fileFailure) throw new IllegalStateException("forced file failure"); }
        public byte[] read(String key) { return files.get(key); }
        public void discard(String key) { files.remove(key); }
    }
    UploadResult upload(UUID request, long expected) { return service.upload(campaign, "dm", request, expected, png, "image/png"); }
    MapConfig calibration(UUID asset) { return new MapConfig(asset, 48, -12, 13, 5.0); }
    @Test void uploadRetryUsesStableIdIndependentStorageKeyAndOneEvent() {
        var request = UUID.randomUUID(); var result = upload(request, 0);
        assertEquals(result, upload(request, 0)); assertEquals(1, events); assertEquals(1, store.files.size());
        var stored = store.assets.get(result.asset().id());
        assertNotEquals(result.asset().id().toString(), stored.storageKey()); assertEquals(64, stored.sha256().length());
        assertArrayEquals(png, service.content(campaign, result.asset().id(), "dm").bytes());
        assertEquals(1, store.campaignVersion); assertEquals(0, store.sceneVersion);
        assertEquals(List.of("actor", "campaign", "actor", "campaign", "campaign"), store.locks);
    }
    @Test void mapReceiptReturnsHistoryWithoutRestoringHistoricalMap() {
        var asset = upload(UUID.randomUUID(), 0).asset().id(); var request = UUID.randomUUID();
        var assigned = service.configure(campaign, scene, "dm", request, 0, calibration(asset));
        assertEquals(1, assigned.sceneVersion());
        var detached = service.configure(campaign, scene, "dm", UUID.randomUUID(), 1, null);
        assertNull(detached.map()); assertEquals(2, detached.sceneVersion());
        assertEquals(assigned, service.configure(campaign, scene, "dm", request, 0, calibration(asset)));
        assertNull(service.map(campaign, scene, "dm").map()); assertEquals(3, events); assertEquals(1, store.campaignVersion);
    }
    @Test void currentDmAuthorizationPrecedesEveryReadAndReceiptReplay() {
        var request = UUID.randomUUID(); var asset = upload(request, 0).asset().id();
        var mapRequest = UUID.randomUUID(); service.configure(campaign, scene, "dm", mapRequest, 0, calibration(asset));
        for (var role : Arrays.asList(Role.PLAYER, null)) {
            store.role = role;
            code("FORBIDDEN", () -> upload(request, 0)); code("FORBIDDEN", () -> service.list(campaign, "dm"));
            code("FORBIDDEN", () -> service.content(campaign, asset, "dm")); code("FORBIDDEN", () -> service.map(campaign, scene, "dm"));
            code("FORBIDDEN", () -> service.configure(campaign, scene, "dm", mapRequest, 0, calibration(asset)));
        }
        assertEquals(2, events);
    }
    @Test void fingerprintCoversKindCampaignVersionFileAndAllCalibrationFields() {
        var request = UUID.randomUUID(); var asset = upload(request, 0).asset().id();
        code("IDEMPOTENCY_CONFLICT", () -> upload(request, 1));
        code("IDEMPOTENCY_CONFLICT", () -> service.upload(UUID.randomUUID(), "dm", request, 0, png, "image/png"));
        code("IDEMPOTENCY_CONFLICT", () -> service.configure(campaign, scene, "dm", request, 0, null));
        var mapRequest = UUID.randomUUID(); service.configure(campaign, scene, "dm", mapRequest, 0, calibration(asset));
        for (var map : Arrays.asList(null, new MapConfig(asset,49,-12,13,5.0),new MapConfig(asset,48,-11,13,5.0),new MapConfig(asset,48,-12,12,5.0),new MapConfig(asset,48,-12,13,6.0),calibration(UUID.randomUUID())))
            code("IDEMPOTENCY_CONFLICT", () -> service.configure(campaign, scene, "dm", mapRequest, 0, map));
        code("IDEMPOTENCY_CONFLICT", () -> service.configure(campaign, UUID.randomUUID(), "dm", mapRequest, 0, calibration(asset)));
        code("IDEMPOTENCY_CONFLICT", () -> service.configure(campaign, scene, "dm", mapRequest, 1, calibration(asset)));
    }
    @Test void staleVersionsUnknownScopedReferencesAndInvalidCalibrationDoNotMutate() {
        code("VERSION_CONFLICT", () -> upload(UUID.randomUUID(), 1));
        code("NOT_FOUND", () -> service.map(campaign, UUID.randomUUID(), "dm"));
        code("NOT_FOUND", () -> service.content(campaign, UUID.randomUUID(), "dm"));
        code("NOT_FOUND", () -> service.configure(campaign, scene, "dm", UUID.randomUUID(), 0, calibration(UUID.randomUUID())));
        for (var map : List.of(new MapConfig(null,48,0,0,5.0),new MapConfig(UUID.randomUUID(),7,0,0,5.0),new MapConfig(UUID.randomUUID(),48,-8193,0,5.0),new MapConfig(UUID.randomUUID(),48,0,8193,5.0),new MapConfig(UUID.randomUUID(),48,0,0,Double.NaN),new MapConfig(UUID.randomUUID(),48,0,0,Double.POSITIVE_INFINITY),new MapConfig(UUID.randomUUID(),48,0,0,0.0)))
            code("INVALID_INPUT", () -> service.configure(campaign, scene, "dm", UUID.randomUUID(), 0, map));
        code("INVALID_INPUT", () -> upload(null, 0)); code("INVALID_INPUT", () -> upload(UUID.randomUUID(), -1));
        assertEquals(0, events); assertTrue(store.files.isEmpty()); assertTrue(store.receipts.isEmpty());
    }
    @Test void normalFileAndReceiptFailuresDiscardOnlyNewFiles() {
        var old = upload(UUID.randomUUID(), 0); String committed = store.assets.get(old.asset().id()).storageKey();
        store.fileFailure = true; assertThrows(IllegalStateException.class, () -> upload(UUID.randomUUID(), 1));
        assertEquals(Set.of(committed), store.files.keySet()); store.fileFailure = false;
        store.receiptFailure = true; assertThrows(IllegalStateException.class, () -> upload(UUID.randomUUID(), 1));
        assertEquals(Set.of(committed), store.files.keySet()); assertArrayEquals(png, store.files.get(committed));
        assertEquals(1, events);
    }
    static void code(String code, Runnable action) { assertEquals(code, assertThrows(DomainException.class, action::run).code()); }
}
