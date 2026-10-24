# A03d: editable manual operation combinations

Operations belong to an independent live module campaign, not its immutable
template or source library. Only current target-campaign DMs can read, edit,
review, execute or read evidence. Source-library membership is not required.
Every POST requires CSRF. Definitions never run on import, edit or startup.

`GET /api/campaigns/{campaignId}/module-operations`
`POST /api/campaigns/{campaignId}/module-operations/{id}`
`POST /api/campaigns/{campaignId}/module-operations/{id}/impact`
`POST /api/campaigns/{campaignId}/module-operations/{id}/executions`
`GET /api/campaigns/{campaignId}/module-operations/{id}/executions`

The client selects a fresh UUID for a new definition. Save is exactly
`{requestId,expectedVersion,name,steps}`; version zero means create if absent,
otherwise compare the existing version. First save returns version 1. Subsequent
saves replace the complete definition and increment its version. Max 100
definitions per campaign, 1..20 steps, name 1..100 code points, complete save
JSON <=64 KiB. Unknown/missing/null/extra fields and duplicate targets are rejected.
Definitions use runtime identities in this campaign and are not exported into
portable templates. Stale/incomplete target configuration can be saved as
preparation; review/execution must resolve every target and version.

Each step is one strict discriminated shape:

- `{kind:"REVEAL_TOKEN",sceneId,tokenId,expectedVersion}` reveals that token,
  preserving position/name/controllers and its character link. Character hiding
  is independent and requires its own step.
- `{kind:"REVEAL_CHARACTER",characterId,expectedVersion}` reveals that card,
  preserving its sheet/resources/controllers/share policy. Pending import review
  is blocked; this action cannot approve content.
- `{kind:"PUBLISH_TEXT",noteId,expectedVersion}` copies the current DM-only note
  title and literal text into a new TEAM note through P08. Original note/history
  stays private. Notes with attachments are explicitly unsupported and block;
  images must be published separately using existing authorized authoring.
- `{kind:"PREPARE_ENCOUNTER",expectedVersion,participants,reason}` delegates the
  existing P05 candidate generation, with its exact participant fields, groups,
  source versions and bounds. It leaves DRAFT candidates for explicit ordering
  and confirmation through P05; it does not start combat.
- `{kind:"START_ENCOUNTER",expectedVersion,initiativeVersion,reason}` starts P06
  from that exact already CONFIRMED initiative version, only while combat is idle.

At most one encounter step per combination, so generation cannot silently confirm
random results or start an encounter. Duplicate token/card/note targets reject.
No scripts, conditions, resource consumption, attacks, automatic ties, map reveal
or arbitrary requests. Unsupported work remains explicit preparation.

Review body is exactly `{expectedVersion,campaignVersion}`. Response includes
`{id,version,campaignVersion,impactHash,definition,targets}`. Review validates the
entire combination with existing service validation before writes or entropy:
same campaign/active scene and token, exact object versions, note visibility,
participant/card/group constraints, confirmed initiative and idle combat. Exact
module dependencies must still resolve locally using the instance dependency map.
The server hash binds definition, campaign management version, instance and all
reviewed target snapshots. Changing any of these requires another review.

Execution is exactly `{requestId,expectedVersion,campaignVersion,impactHash}`.
Actor lock precedes campaign lock. Check current DM and module instance before
receipt replay; exact same request returns the original immutable execution
without reapplying, even after definition edits or later play. Changed command
body, requested definition ID, campaign or route with the same request ID returns
409. Fresh requests
recompute review/hash before any mutation. All original service writes, histories,
versions, child receipts and combination evidence commit in one transaction;
immediate/deferred failure rolls everything back. Events publish after commit
and contain only the existing permission-filtered invalidation. No partial
combination is visible. Concurrent identical requests have one effect; stale
competitors cannot override newer object versions.

Saved evidence contains execution UUID, definition snapshot, impact/target
snapshots, actor/time/request ID and ordered original-service results. It is
immutable and DM-only; source DM prose/versions/targets never enter player
payloads. Published TEAM text is the explicit visibility change. Original cards,
RP/history/progress/template/instance versions otherwise remain intact. Evidence
and receipts are included in ordinary P09 database backup/restore.

A03e owns Chinese editing/review/trigger controls and final integrated browser,
restart/restore acceptance. A03/A04–A06/M3 remain open. Cloud and real phones are
deferred; same branch/draft PR #1, no merge/release.
