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
from baseline. No field is chosen or written by preview.

Response `{version,packageId,sha256,changes,missingDependencies}` pins the
candidate's immutable library identity/hash and current customization version.
Only current DMs see these definitions or dependency details. Repeated previews
have no receipts, version increments, events or runtime side effects. Existing
instances, resource values, turns, progress, RP/dice/combat/history remain intact.
Preview is not permission or a promise that a later apply will still succeed.

## Remaining ordered slices

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
