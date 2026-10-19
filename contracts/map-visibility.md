# P02d: safe map delivery and manual visibility

Existing snapshots, movement and secret-free events keep their exact shapes.
Unit hiding/control remains independent: revealing a map cell never reveals a
hidden token, and visible units remain visible even on covered map cells.

## Shared current-map view

- `GET /api/campaigns/{campaignId}/scene/map-view` →
  `{ sceneId, sceneVersion, map }`. map is null or `{ distancePerCell }`.
  Authenticated current members only (403 otherwise). The scene is always the
  campaign's current scene. No asset ID, source dimensions, calibration offsets,
  storage key, original URL or fog authoring data enters this response.
- `GET /api/campaigns/{campaignId}/scene/map-image?sceneId=UUID&version=N` →
  `image/png` bytes. Both query parameters are required; invalid values give 400.
  Reauthorize current membership first, then current scene (404 if different),
  exact current scene version (409 VERSION_CONFLICT), and attached map (404).
  No historical image versions or role-selecting parameters are supported.

Image output is exactly the logical scene width × gridSize by height × gridSize.
For output pixel (px,py), the source coordinate is
`(offsetX + floor(px * map.gridSize / scene.gridSize),
offsetY + floor(py * map.gridSize / scene.gridSize))`. Out-of-source pixels are
opaque black. Nearest-neighbor sampling is deliberate: interpolation must never
blend a covered source cell into a revealed one. Player pixels in unrevealed
logical cells are always opaque black (RGB 0, alpha 255); only explicitly revealed
cells sample the original. Flatten source alpha onto black and encode a fresh PNG
with no source metadata. DMs receive the full calibrated image at the same route.
No player request ever receives original bytes, even when all cells are revealed.

All map-view/image and fog responses, including malformed, anonymous, revoked,
missing and stale failures, carry `Cache-Control: no-store, private` and
`X-Content-Type-Options: nosniff`. No ETag, Last-Modified, 304, Range/206, redirect,
public URL, disk derivative or reusable cache capability. Every image is generated
under the same campaign lock as authorization/current map/fog/version reads;
there is no derivative cache. Existing original-content routes remain DM-only,
with no-store even for guessed/foreign IDs. Missing source files give generic 503
ASSET_UNAVAILABLE. Existing private storage is never served as a static directory.

Already delivered public pixels may have been saved by a player. Re-covering
changes future responses and invalidates old URLs; it cannot retract prior copies.
An image read authorized before a concurrent change can finish delivering its
then-current public result; changes take effect for reads authorized afterward.

## DM fog authoring

- `GET /api/campaigns/{campaignId}/scenes/{sceneId}/fog` →
  `{ sceneId, sceneVersion, revealed: [{ x, y }] }`.
- `POST /api/campaigns/{campaignId}/scenes/{sceneId}/fog` body
  `{ requestId, expectedVersion, revealed: [{ x, y }] }` → same result.
  This replaces the complete revealed cell set (empty means fully covered).

Both are current-DM-only before target lookup/replay (PLAYER/nonmember 403).
Foreign/missing scene 404; inactive scenes may be prepared without activation.
POST requires CSRF, all fields, exact JSON keys, nonnegative integer versions,
distinct in-bounds integer cells, no nulls, and at most width×height entries.
Canonical order is y then x. Fog may be prepared without a map; every successful
map assignment/calibration/detach resets fog to fully covered atomically, advances
the existing scene version once, and preserves token visibility/control.
V6 introduces fog and its receipts without changing V1–V5 checksums or old rows.

Fog expectedVersion is the scene version shared with moves/maps/token authoring.
Use a separate durable fog-command namespace `(sessionActor, requestId)` and
actor→campaign locks. Fingerprint includes campaign/scene/expectedVersion and
canonical revealed set. Current DM and target ownership precede receipt lookup.
Identical retries return original results before stale versions, with no mutation
or new event; changed fingerprints give 409 IDEMPOTENCY_CONFLICT. Fresh stale
versions give 409 VERSION_CONFLICT. State/version/receipt commit atomically;
identical concurrent retries have one effect, same-version commands one winner.
Historical receipts never restore old fog. Emit only existing after-commit
SCENE_CHANGED invalidations. Never put pixels, fog, token secrets or image URLs
in events/errors/receipts beyond the DM fog confirmation above.

## Client interaction and acceptance

Native Pixi supports pointer pan, cursor-centered wheel zoom/reset, selected-token
drag preview and integer-cell snapping, click movement, and two-point straight-line
Euclidean measurement × distancePerCell (a geometric aid, no D&D movement rule).
The existing numeric movement fallback remains. Calibration uses the existing DM
map fields and affects the server image; no client original image is needed for
play. DM rectangular reveal/cover gestures and a coordinate form edit the same
cell set, with explicit save/confirmation and server versions.

Fetch current map-view and matching image with `cache: 'no-store'`. On scene or
version invalidation remove the displayed image before requesting another; never
keep an old revealed image visible while loading a covered replacement. Revoke
object URLs/destroy textures on replacement, unmount, campaign/scene switch and
401/403; late responses cannot restore disposed state. Observe any concurrent
authorization denial, including image downloads. Reconnect/periodic snapshot reads
also reread map state. Version mismatch refreshes authoritative state; uncertain
POSTs retain exact body/requestId for explicit retry and disable conflicting edits.
Receipt success alone does not claim current fog is synchronized: a latest
post-ack read must succeed. Fog drafts retain their original expectedVersion.

First execute actual assertion REDs proving the missing delivery/fog behavior,
then implement. Acceptance decodes returned PNGs to compare secret/public pixels,
checks original/guessed/cross-campaign/revoked URLs, stale and conditional requests,
historical retries, races/rollback, migration preservation, secret-free STOMP and
real DM/player Pixi gestures. Preserve every previous test. P02e and the whole P02
remain incomplete after this slice; broader restart/disconnection acceptance is P02e.
