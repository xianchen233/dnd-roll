# P02b: durable DM assets and scene map configuration

P02b stores private PNG/JPEG maps and their calibration. All five operations are
authenticated, current-DM-only, including replay and direct content downloads.
PLAYER/nonmembers receive 403; after DM authorization, foreign/unknown asset or
scene IDs receive 404. Every POST requires CSRF. No public/static asset route,
signed URL, storage key, original filename or filesystem path is exposed.

## HTTP

- `GET /api/campaigns/{campaignId}/assets` → `{ campaignVersion, assets: [{ id, mediaType, byteSize, width, height }] }`.
- `POST /api/campaigns/{campaignId}/assets` → `{ asset: { id, mediaType, byteSize, width, height }, campaignVersion }` (200). Multipart body has exactly `command` (application/json `{ requestId, expectedVersion }`) and `file` (image/png or image/jpeg). Browser filenames are ignored. Maximum file size 10 MiB (10,485,760 bytes); nonempty, actual decoded format must match declared media type; dimensions 1–8192 each and at most 32,000,000 pixels. Reject SVG, malformed/truncated images, MIME mismatch and oversized input with 400 INVALID_INPUT (413 is permitted at the transport/proxy limit). Metadata comes from the validated image, never client claims.
- `GET /api/campaigns/{campaignId}/assets/{assetId}/content` → original validated bytes with matching Content-Type, `Content-Disposition: attachment`, `Cache-Control: no-store, private`, `X-Content-Type-Options: nosniff`. Failures also carry no-store. Reauthorize every read; session logout or membership revocation denies subsequent downloads. Missing/unreadable backing file returns generic 503 ASSET_UNAVAILABLE without internal paths. No ETag/304 or public cache capability.
- `GET /api/campaigns/{campaignId}/scenes/{sceneId}/map` → `{ sceneId, sceneVersion, map }`. `map` is null or `{ assetId, gridSize, offsetX, offsetY, distancePerCell }`. DM may configure an inactive scene without switching the group.
- `POST /api/campaigns/{campaignId}/scenes/{sceneId}/map` body `{ requestId, expectedVersion, map }` → same map response. `map: null` detaches the image; otherwise all five map fields are required, unknown fields forbidden. `gridSize` integer 8–512 image pixels per cell, offsets integers −8192–8192 image pixels, `distancePerCell` finite number 0.01–10000 (unit chosen by the group). Reference only an existing asset owned by the same campaign. Map gridSize is calibration, separate from the existing logical canvas gridSize; logical scene dimensions/positions/control are unchanged.

## Versions, retries and atomicity

Upload expectedVersion is the campaign management version shared with P01/P02a;
successful upload advances it once. Map expectedVersion is the target scene
version shared with movement; successful assignment/detach advances only that
scene version once. Stale versions give 409 VERSION_CONFLICT.

Both commands use a durable `asset-commands` receipt namespace keyed by
`(sessionActor, requestId)`, separate from account/scene/movement receipts. The
fingerprint includes kind, campaignId, sceneId (map), expectedVersion and either
the validated file SHA-256/media type (upload) or all map fields (map). Acquire
actor then campaign locks consistently with existing management operations.
Current DM authorization precedes replay. Identical concurrent retries have one
effect and return the original receipt before checking stale versions; changed
fingerprints give 409 IDEMPOTENCY_CONFLICT. A historical map receipt never
reattaches an earlier image; clients reread authoritative state after success.

Use stable UUID asset IDs and independent server-generated storage keys. V4
preserves V1–V3 data/checksums and enforces same-campaign map references in the
database. Write files into the configured private persistent root with atomic
publication before committing metadata/reference/versions/receipts. File I/O or
database/receipt failure must roll back valid database state and clean new files
on normal failure. A process crash may leave an unreferenced opaque file; it must
never be downloadable without committed metadata. Do not delete referenced or
previously committed assets on failed commands. No automatic asset deletion in
this slice; future backup/retention belongs to P09.

A lost database commit acknowledgment is an uncertain outcome, not proof of
rollback. Acquire a fresh campaign lock to wait for the original server transaction
to finish; an unlocked MVCC read alone cannot prove absence during an in-flight
commit. After the transaction ends, preserve a new file if matching committed
metadata exists; if metadata cannot be checked because the database is unavailable,
also preserve it for reconciliation. Only delete a new file after confirming its
metadata did not commit. Return the original failure, without claiming success;
the same requestId resolves through its durable receipt on retry. Unreferenced
files left by uncertain outcomes have no download authority, like crash orphans.

## Visibility and clients

Existing current-scene snapshots remain unchanged for both roles: no map/asset
IDs, original image dimensions or storage data. Events remain exactly
`{ type: "SCENE_CHANGED", campaignId }`, emitted after commit, never on replay.
Players receive no image bytes or map authoring metadata in P02b. Player-safe
derived images and Pixi calibration/interaction remain P02d.

The DM panel lists durable assets, uploads one, previews its authenticated
download and assigns/detaches it on the current scene with calibration fields.
Describe this as DM preparation; the shared play grid is unchanged in this slice.
Retain requestId and the exact file/body for explicit retry after network/5xx
uncertainty; clear on deterministic 4xx, refresh versions/state on 409. Revoke
object URLs and clear private previews/pending operations on unmount, scene or
campaign switch and 401/403; delayed successes cannot restore cleared data.
Observe authorization failures independently for concurrent reads. A confirmed
command with failed refresh must not claim the current map is synchronized.

Compose mounts a dedicated asset volume at ASSET_STORAGE_ROOT (default
`/var/lib/dnd-roll/assets` in Compose). A local default stays outside source
files and is Git-ignored. Acceptance includes focused executed RED/GREEN,
real PG/HTTP permission/replay/race/rollback checks, V3→V4 preservation, original
browser regressions plus real DM upload/configure/download and player denial,
and actual Java + PostgreSQL restart preserving file hashes and references.
