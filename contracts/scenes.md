# P02a: multiple scenes and the campaign's current scene

This slice adds empty scenes only. Images, grid configuration, token authoring and
fog remain P02b–d. Existing scene snapshots and movement bodies remain compatible.

## DM scene management

- `GET /api/campaigns/{campaignId}/scenes` → `{ campaignVersion, currentSceneId, scenes: [{ id, name }] }`. DM only. Inactive names/IDs are preparation data and never enter player payloads.
- `POST /api/campaigns/{campaignId}/scenes` body `{ requestId, expectedVersion, name }` → `{ id, name, campaignVersion }` (200). Name is trimmed, 1–100 Unicode code points. Creates an empty 24×16 grid, gridSize 48, version 0; does **not** switch the group.
- `POST /api/campaigns/{campaignId}/scenes/{sceneId}/activate` body `{ requestId, expectedVersion }` → `{ id, name, campaignVersion }` (200). Target must belong to this campaign; other-campaign/unknown targets return 404. Switches the whole group without modifying any scene or token state. Activating the already current scene succeeds and still advances campaignVersion once.

All routes require authenticated identity and current DM membership. Every POST
requires CSRF. PLAYER and nonmembers receive 403, including on receipt replay.
expectedVersion is the campaign management version shared with P01, a nonnegative
integer. Stale versions return 409 VERSION_CONFLICT. Actor comes from the session.

Scene commands use a durable receipt namespace keyed by `(sessionActor, requestId)`
shared between create/activate, separate from account and movement receipts.
Fingerprint includes command kind, campaignId, target sceneId (activate), trimmed
name (create), and expectedVersion. After current authorization, identical retries
return the original result before checking stale versions, without creating,
activating again or emitting another event. Changed fingerprints return 409
IDEMPOTENCY_CONFLICT. A replayed activation receipt is historical confirmation;
clients must reread the current snapshot/list, never treat its id as current.
Commands, campaign version and receipts commit atomically. Parallel commands with
the same expectedVersion have one winner; identical concurrent retries have one effect.

## Shared snapshot, movement and realtime

`GET /api/campaigns/{campaignId}/scene` always returns the DM-selected current
scene, with existing per-member token filtering. Players cannot fetch inactive
scenes or enumerate their metadata. No scene-by-ID read route is introduced.
Movement applies only to tokens in the current scene, including receipt replay:
an inactive/foreign token returns 404 without leaking its data. Returning to that
scene preserves its positions, token/scene versions, controls and durable receipts.
Only that scene's version advances on a move.

After successful scene creation/activation, send the existing after-commit private
`SCENE_CHANGED` invalidation containing only type and campaignId. Reconnect and
periodic reads recover a missed switch. Clients clear old selection and any failed
movement retry when a different scene arrives; an old in-flight movement response
must not overwrite or confirm the new scene. DM commands retain requestId/body on
ambiguous transport/5xx failures, support explicit retry, and refresh list/version
on 409. A 401/403 clears map and management data, including delayed successes.

## V3 migration and initialization

Preserve V1/V2 checksums/data. Remove the single-scene uniqueness constraint, add
a campaign current-scene pointer backfilled to each existing scene, and enforce
that the referenced scene belongs to that campaign. Initial P01/demo creation must
set the pointer transactionally; demo restart never resets an existing pointer.
No existing tokens, controls, receipts or scene versions are rewritten.

Acceptance covers unit RED/GREEN, real PG/HTTP authorization, version/retry/race
and rollback tests, V2→V3 preservation, both existing browser flows plus actual
DM/player switching and returning to a moved scene, and a packaged process restart.
