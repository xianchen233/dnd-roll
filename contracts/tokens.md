# P02c: token authoring and shared control

DMs create and configure map tokens independently of future character sheets.
Existing shared snapshots, movement requests/responses and realtime events keep
their exact shapes. No character resources, combat rules or deletion are added.

## DM authoring API

- `GET /api/campaigns/{campaignId}/scenes/{sceneId}/tokens` →
  `{ sceneId, sceneVersion, tokens: [{ id, name, x, y, version, hidden, controllers }] }`.
- `POST /api/campaigns/{campaignId}/scenes/{sceneId}/tokens` body
  `{ requestId, expectedVersion, name, x, y, hidden, controllers }` →
  `{ sceneId, sceneVersion, token: { id, name, x, y, version, hidden, controllers } }`.
  expectedVersion is the target **scene** version shared with maps/movement.
  Create a stable UUID token, version 0; advance the scene version once.
- `POST /api/campaigns/{campaignId}/scenes/{sceneId}/tokens/{tokenId}` with the
  same body/response shape replaces the complete configuration. expectedVersion
  is the **token** version shared with movement. Advance both token and scene
  version once, even for unchanged values. Controls are replaced atomically.

All endpoints require authenticated current DM membership; PLAYER/nonmembers
receive 403 before target/receipt lookup. Every POST requires CSRF. Unknown or
foreign scene/token targets return generic 404. DMs may prepare inactive scenes;
authoring does not activate them. The token must belong to the specified scene.
Requests reject unknown or missing fields, nulls, invalid UUIDs, noninteger or
negative versions/coordinates, and out-of-grid positions with 400 INVALID_INPUT.
Names are stripped and must contain 1–100 Unicode code points. controllers is a
required array of distinct exact usernames (0–100 entries, each 1–80 characters),
each a current member of this campaign (DM or PLAYER); invalid membership returns
generic 400 without identifying an account. Order has no meaning: canonicalize
controllers by sorting for responses/fingerprints. An empty array revokes all
explicit control; current DMs retain their existing implicit control.

## Authorization, versions and durable retries

Use a separate `token-commands` namespace keyed by `(sessionActor, requestId)`
across create/update and campaigns. Fingerprint includes kind, campaignId,
sceneId, tokenId (update), expectedVersion, stripped name, x/y, hidden and the
canonical controller set. Actor comes only from the session. Acquire actor then
campaign locks, consistent with existing account/scene/asset commands.

Under the campaign lock, check current DM authorization and target ownership
before replay. An identical retry returns the original result before stale
version or controller-membership validation, with no new mutation/event. Changed
fingerprints return 409 IDEMPOTENCY_CONFLICT. Replayed results are historical:
they never recreate a token, restore earlier names/positions/visibility/controls
or regrant removed members. Clients reread authoritative state after success.
Fresh commands validate every controller's current membership and then expected
version; stale versions return 409 VERSION_CONFLICT. Token/configuration,
controllers, scene version and receipt commit atomically. Same-request concurrent
retries have one effect; concurrent commands on the same version have one winner.

Movement keeps its existing token version and durable receipts, and checks
**current** membership, current-scene visibility and current control before
returning any historical movement receipt. Revoked controllers receive 403;
hidden/inactive/foreign tokens receive generic 404 for players. Removing a member
deletes their controls campaign-wide. Rejoining does not restore those controls,
and replaying an old authoring receipt cannot restore them. Membership removal
must advance affected token and scene versions so a stale authoring form cannot
silently regrant control after that member rejoins.

Hidden tokens are omitted entirely from player snapshots. No hidden flag,
controller list, hidden identity/name/position/count enters shared token views,
errors or events. A successful authoring command emits only the existing
after-commit `{ type: 'SCENE_CHANGED', campaignId }` member invalidation.

## Client and migration acceptance

Only DMs see the authoring form/list, including explicit hidden and multi-select
member controls. Wait for authoritative tokens/members before enabling submit.
Do not mark a receipt as current state: refresh the token list and shared scene.
On 409 discard the command and refresh versions. On ambiguous transport/5xx keep
the exact body/requestId for explicit retry and disable conflicting edits.
Campaign/scene changes and 401/403 clear private state/pending operations; delayed
successes cannot restore them. Any same-scope authorization denial invalidates
concurrent successes. Revocation/hidden changes refresh the shared scene so old
selected/pending movement cannot restore a removed token or control.

V5 preserves all V1–V4 rows/checksums and adds durable token receipts plus
same-campaign membership integrity for controls (backfill existing controls).
Validate RED/GREEN without DB, real PG/HTTP/WS authorization, hidden fields,
retries/races/rollback, migration data preservation and process restart. Keep all
earlier tests and add a real DM/player browser flow for creation, shared control,
hiding/revealing and revocation. P02d/e and later packages remain outstanding.
