# A03: selective updates and manual operations

Status: in progress. Ordered slices below are independent acceptance gates;
A03 is not complete until all gates have actual evidence.

## Three-way preparation preview (A03a)

POST `/api/campaigns/{campaignId}/module-customizations/{id}/update-preview`
accepts exactly `{expectedVersion,packageId}`. This is a read-only POST with
CSRF, current DM authorization and no-store/private. The customization version
must still match; foreign copies/packages are 404 after authorization. New
package must have the same moduleId and rules family as the original source,
and a different moduleVersion. Version strings are identities, not ordering.

The server compares immutable original baseline, saved current customization,
and candidate template. Arrays of nodes/scenes/units/placements are matched by
stable UUID (nodeId for scene/unit). Object fields are compared recursively;
reference lists and other arrays are atomic values. Reordering identity arrays
is not a modification. Add/delete vs edit is a conflict at that object identity.
Changes contain `{path,baseline,current,incoming,conflict}`. Absent values are
JSON null; actual nullable map values remain distinguished by their path.
Unchanged fields are omitted. A conflict means both sides changed differently
from baseline. No field is chosen or written by preview. A03a compares definition
fields only: it does not yet diff manifest asset bytes, dependency declarations or
rights. Empty changes mean identical definition fields, not identical packages or
readiness to apply. Manifest/asset/dependency closure belongs to A03b's impact gate.

Response `{version,packageId,sha256,changes,missingDependencies}` pins the
candidate's immutable library identity/hash and current customization version.
Only current DMs see these definitions or dependency details. Repeated previews
have no receipts, version increments, events or runtime side effects. Existing
instances, resource values, turns, progress, RP/dice/combat/history remain intact.
Preview is not permission or a promise that a later apply will still succeed.

## Customization selection and atomic application (A03b)

Current-DM-only routes under `/api/campaigns/{campaignId}/module-customizations/{id}`:

- POST `/update-options` accepts the A03a request and includes definition changes
  plus `/manifest/rights`, stable-ID assets and exact dependencies. Original
  source/baseline remain immutable; current metadata comes from the last applied
  effective archive. Package display name/version are provenance, not selections.
- POST `/update-impact` accepts exactly `{expectedVersion,packageId,selections}`.
  Up to 500 changed paths are supported per application (strict command limit).
  Each selection is `{path,choice:KEEP|INCOMING}`. Every change, including conflicts
  and local-only edits, needs exactly one decision. Duplicate/unknown/missing
  paths and other choices reject. No default adoption or implicit conflict fix.
  Server assembles the document, rights, old/new asset bytes and dependencies,
  validates all internal references and actual image hashes, and re-verifies the
  complete bounded ZIP. Missing exact catalog dependencies remain visible.
  Response adds `{impactHash,document,manifest}` to the A03a result shape.
  The hash binds campaign/copy/version, original source hash, effective archive,
  incoming archive hash, exact selections, assembled ZIP and dependency readiness.
  Both preparation POSTs are read-only with no receipts or version changes.
- POST `/updates` adds exactly `{requestId,sha256,impactHash}` to the impact body.
  Current DM and campaign/copy scope precede durable receipt replay. Same actor,
  request and exact payload return the original acknowledgement; changed payload
  conflicts. Fresh stale versions, changed impact/hash and unresolved dependencies
  reject. Server recomputes everything in the same locked transaction as apply.
  Document/version, effective ZIP, immutable actor/choices/provenance/before-image
  and receipt commit together, including deferred-commit failure rollback.
- GET `/updates` returns ordered evidence including the complete previous copy.
- GET `/updates/{version}/content` downloads a validated ZIP assembled from that
  before-image document and retained image bytes, under a stable recovery module
  identity. It can be imported as a separate template/copy; recovery does not
  overwrite the current copy or undo live play. Unknown/foreign targets are 404
  after authorization. History and download enforce current DM on every read.

V20 adds effective-archive and immutable-update tables; original V1–V19 files,
source package, baseline, existing instances and histories remain unchanged.
Save-as and new campaign creation use the assembled definitions/assets/rights.
Instances retain original source package/hash provenance plus customization
version and the effective manifest used at creation. Existing live instances
are never updated by A03b. BYTEA and JSON evidence are included in P09 archives.
No-store/private, nosniff and CSRF follow the existing module boundary.
The TypeScript client exposes these routes; Chinese selection/uncertain-retry UI
remains A03e work. Actual checks and limitations: `docs/a03b-tdd.md`.

## Live preparation text gate (A03c, partial)

POST `/api/campaigns/{campaignId}/module-instance/update-impact` accepts exactly
`{expectedVersion,customizationVersion}`. It compares the instance's accepted
definition to its original source customization, under source-then-target locks.
Current DM membership in both campaigns is required. The response contains
`{version,customizationVersion,impactHash,changes,blockers}`. It is read-only,
CSRF protected and private/no-store. Both versions must match. The hash binds
the complete instance, customization, effective manifest and dependency readiness.

POST `/module-instance/updates` adds `{requestId,impactHash}`. The server
recomputes impact under the same locks. Only node name/text changes are currently
supported, and update the instance's preparation definition only. Existing live
scene names, published notes and character names remain independent runtime data.
Any scene/unit/placement/policy/visibility/reference change, asset/dependency/rights
change or missing dependency appears as a blocker and rejects application with
409 `MODULE_LIVE_MIGRATION_REQUIRED`, even with an otherwise valid hash.
There is no destructive-operation confirmation bypass in this slice.

Instance version, accepted definition/customization version, immutable full
before-instance evidence and receipt commit atomically. Mapping, progress and
original package provenance remain intact. No character, resource, condition,
turn, scene, note or historical record is written or recalculated. Current DM
in both domains precedes receipt replay; identical retry returns the old ack,
changed payload conflicts. GET `/module-instance/updates` is target-DM-only and
returns ordered evidence `{version,actor,requestId,impactHash,before}`. Before
instances are retained as independent recovery evidence, not a live-play undo.
V21 adds the evidence table; all old migrations remain unchanged.

This text gate remains bounded. Structural impact confirmation and recoverable
migration use the separate gate below; Chinese controls remain A03e work. No scene
deletion or combatant replacement is enabled through the text-only endpoint.

## Structural migration gate (A03c)

Separate POST `/module-instance/migration-impact` accepts `LiveReview`.
POST `/module-instance/migrations` accepts exactly
`{requestId,expectedVersion,customizationVersion,impactHash,confirmations}`.
GET `/module-instance/migrations` returns immutable migration evidence including
`before`, runtime snapshot, actual actor and `recoveryCampaignId`.
Both-domain current DM, source→target locks, CSRF and durable exact retry follow
the text gate. The text gate remains restricted to its existing behavior.

Impact binds current runtime scene/map/fog/token/card and combat snapshots as well
as both definition versions. Every returned confirmation string must be echoed
exactly once; missing, duplicate, unknown or stale confirmations reject before
writes. Response includes complete `changes` with before/after definitions and
the bound DM-only `runtime` snapshot for concrete review. Confirmation describes changed scenes/maps, retained resources and removed
placements/units, including retained old combat identities. No combat participant
or past result is automatically replaced/recalculated. New units require explicit
existing initiative/combat operations to enter play.

Before migration, the transaction creates an independent DM-only recovery campaign
from the pre-migration live scene geometry, all live cards (including damaged HP,
resources and conditions) and placement positions. Its independent maps start fully
covered. Original records and combat state remain in the original campaign; the
recovery campaign is a playable structural copy, not a replay of historical events.
Full runtime snapshot remains in evidence. Recovery campaign, evidence, structural
writes, instance and receipt commit or roll back together. Exact retry requires
current DM access to the recovery campaign too.

Recovery also includes retired combat identities and additional live cards outside
the current template. Each additional card has an explicit `RECOVER_EXTRA_UNIT`
confirmation, independent runtime identity and only the migrating DM as controller.
They are not adopted into the original instance's template mapping. Recovery node
identities avoid collisions with existing nodes/placements. The complete recovery
definition is validated at preview time; limits (100 cards, 500 nodes and bounded
JSON) produce `/recovery-definition` blockers before writes. Recovery includes
ad-hoc scenes, linked and characterless tokens, live map calibration and validated
independent image bytes. Each extra scene/placement/map has an explicit
`RECOVER_EXTRA_SCENE`, `RECOVER_EXTRA_PLACEMENT` or `RECOVER_EXTRA_MAP`
confirmation. Original ad-hoc objects stay outside the template mapping and are
not modified. Token names, positions, hiding, nullable character links and the
current scene are preserved in recovery; all controllers belong only to the
migrating DM. Characterless tokens stay characterless and do not create cards.
The runtime snapshot also binds asset metadata; the hash binds the validated
recovery archive. Missing/corrupt bytes, invalid links, out-of-bounds tokens or
recovery limits reject before creating any copy or files. The existing bounded
archive limits apply to the combined recovery images, not only the template ZIP.
The recovery archive allows at most 128 image identities, 10 MiB of compressed
ZIP bytes, and 32 MiB expanded across all entries, including manifest/content
overhead. A valid template plus additional live maps may exceed these combined
limits and is blocked before writes. Shared live asset identities are counted
once; distinct identities remain distinct even if their image hashes match.
Original combat and historical events are never replayed into recovery.

At most 500 exact confirmation strings may be required, including independent
recovery-object confirmations, every definition change and the mandatory recovery
confirmation. Exactly 500 is supported; more reports `/confirmation-limit`.
Sending only 500 items from a larger blocked review cannot bypass the blocker.
Missing, duplicate or unknown items reject without creating recovery/evidence/
files/receipts. An invalid oversized command may be rejected by strict input
validation before reaching migration; it never relaxes the review limit.

Deleting all mapped scenes creates a usable current fallback scene. That scene
is recovered as an independent ad-hoc scene on subsequent migrations, without
being adopted into the original mapping. Active ad-hoc tokens retained in a
retired scene cause that scene to be included in recovery too. Historical retired
template tokens are not resurrected. Recovery of these objects remains in the
same transaction and file rollback boundary as the mapped structure and receipt.

Stable scene/unit/placement mappings are preserved for retained objects. Removed
scenes and placements are retired from active navigation/rendering, retaining rows
and historical references. Removed character identities remain usable by existing
combat/history; replacement units receive independent IDs. Relinking a placement
resets its controllers to the migrating DM; old player grants are not inherited.
Retired unit identities remain explicit so later migration previews work.
Existing card combat,
resources, conditions, equipment, money and overrides are preserved. New definitions
update descriptive/static fields only. Scene shrink rejects if any retained live
token would be outside bounds after the confirmed migration. Bounds use the same
field-wise placement merge as application: unchanged coordinate axes retain their
live values, explicitly changed axes adopt the reviewed definition, and moving to
a different scene uses both destination coordinates. Removed placements do not
block a shrink. Unmapped tokens retained in a shrinking scene must also fit;
template placement changes cannot move these independently authored objects.
A change to visibility or unit identity alone never resets live
coordinates. Existing hidden tokens stay hidden, even when a placement definition
becomes public; revealing them requires the existing explicit live authoring API.
The independent recovery copy retains the actual pre-migration positions.
Map changes reset fog through the existing map store.
Runtime notes, policies, manifest rights/assets/dependencies changes are blockers;
they are not silently overwritten. Structural recovery does not undo later play.

New or changed destination maps also validate their existing template-to-live
asset link, actual PNG/JPEG bytes, hash, size, type and dimensions before writes,
even when the asset is currently detached from every live scene. Missing links or
unavailable/inconsistent files report `/map-assets/{templateAssetId}` blockers.
Apply rechecks these files after review; no recovery, evidence or receipt is written
when validation fails. Repairing the identical immutable bytes permits a fresh
validation. Published-material comparison matches document nodes by stable identity;
reordering the node list alone is allowed, while actual field/add/delete changes
remain `/published-materials` blockers and leave existing notes/history intact.

## Ordered slices

1. A03b: explicit per-item KEEP/INCOMING/conflict decisions, server-recomputed
   version/hash-bound impact preview, atomic apply with durable idempotency,
   recoverable before-image and provenance. Validate the assembled document,
   dependencies and old/new asset closure; never guess around dangling references.
2. A03c: separately preview/apply definitions to live instances using their stable
   mappings. Protect live HP/resources/conditions/turns/progress and all records.
   Scene deletion and combat unit replacement require explicit impact confirmation
   and a recoverable copy. Past rolls/decisions are never recalculated.
3. A03d: DM-editable, bounded typed manual operation combinations (reveal units,
   publish material, prepare/start encounter through existing services). Validate
   all targets/current versions/dependencies before any write. Atomic execution,
   immutable actor/action evidence, same-request retry without replay, revocation
   before receipt, concurrency and failure rollback. No arbitrary code or automatic
   conditions. Unsupported operations remain explicit preparation.
4. A03e: Chinese review/selection/impact/trigger controls, exact uncertain retry,
   scope/revocation isolation, contracts/migration preservation, production browser,
   actual restart/isolated restore and exact-HEAD push/PR full logs.

Cloud deployment/TLS, physical cross-host recovery and real phones stay deferred
by user instruction. Keep the existing branch and draft PR #1; no merge/release.
