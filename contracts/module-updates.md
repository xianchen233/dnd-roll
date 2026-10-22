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
