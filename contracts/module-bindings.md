# A03h: independent live identities and explicit portable binding

Portable v3 import/customization/save-as remain inert. Creating a new 5R campaign
now reserves a fresh operation UUID per stable TRIGGER under `mapping.operations`
and persists an UNBOUND binding. Two campaigns never share operation IDs, target
identities, versions, receipts or binding history. Initial reservation is part of
the existing campaign/files/instance transaction; it never creates executable
steps, consumes dice, reveals units, publishes notes or starts combat.

## Current-target binding

Routes under `/api/campaigns/{campaignId}/module-operation-bindings`, current target
DM only, CSRF on POST, no-store/private/nosniff. Source-library membership is not
required to continue an independent instance. Current target DM/instance/binding
authorization precedes every receipt replay. Binding IDs are stable node UUIDs.

- GET base returns `{instanceVersion,campaignVersion,bindings:[{binding,operation,
  locallyModified}]}`. Binding is `{nodeId,operationId,version,name,source,boundVersion}`;
  version starts at 1, boundVersion 0 means UNBOUND. Operation is nullable current
  runtime definition. A later manual edit is shown as local modification; reads
  never adopt it, reset it or choose target versions.
- POST `/{nodeId}/preview` is read-only, exact `{expectedVersion,operationVersion,
  instanceVersion,campaignVersion}`. Operation version is 0 if no runtime definition
  exists. It resolves all stable references through the current *target* mapping,
  including placement scene/card links, then reads actual token/card/note versions.
  Missing/retired/foreign/relinked targets block; there is no name-based fallback.
  Candidate runtime save (with a server-owned placeholder request UUID) must fit
  the complete 64 KiB P03d save envelope, not merely the portable definition limit.
  Response `{binding,operationVersion,instanceVersion,campaignVersion,candidate,
  targets,impactHash}` shows the complete proposed runtime steps and snapshots.
  Current exact local dependencies and the original operation review constraints
  are checked without writes or entropy. Hash binds binding, complete instance,
  previous local definition, candidate and all reviewed target snapshots.
- POST `/{nodeId}/bind` is exact `{requestId,expectedVersion,operationVersion,
  instanceVersion,campaignVersion,impactHash,reason}` (reason 1..1000). Explicit DM
  confirmation recomputes the preview before saving through ModuleOperations.
  Binding/history/child save receipt and outer receipt commit together. It returns
  `{nodeId,operationId,version,operationVersion}`. No step executes. A later trigger
  still needs the ordinary fresh operation impact and separate execution command.
- GET `/{nodeId}/history` returns immutable DM-only binding events, original source,
  previous local definition, reviewed candidate/targets/hash, actor/time and reason.

REVEAL_TOKEN independently resolves stable placement and verifies its current scene
and character link. Card revelation and token revelation stay separate. Publishing
resolves the current DM note, including ordered current images under A03f; template
text/images are not substituted for live note edits. PREPARE resolves current cards
and the current initiative version; binding neither draws nor confirms candidates.
START can bind only the current already CONFIRMED initiative with idle combat;
EMPTY/DRAFT/active context returns 409 MODULE_BINDING_NOT_READY. No source-group
confirmation or implicit tie ordering is imported. Unsupported source dependencies
or context block the whole preview and fresh bind.

Actor→campaign locks, exact object/instance/campaign/binding/runtime versions and
hashes serialize fresh writes. Identical retries return the immutable original
ack after subsequent edits without rebinding or overwriting; changed body/route/
node/campaign with the same key conflicts. Immediate/deferred failure rolls back
binding, runtime definition, all histories and child/outer receipts. Fresh bind
after a local edit requires reviewing that exact current definition/version and
an explicit reason; it never silently treats template values as current values.

Pending portable identities reserve the existing 100-operation campaign capacity;
the same operation ID counts once when it becomes bound. A manual operation cannot
occupy a reserved UUID before explicit binding. V1/v2 campaigns keep their original
payloads and behavior. V26 adds binding/history/receipt tables and retains all
V1–V25 columns, rows and checksums. P09 full backups include these tables and all
existing immutable files; restoring does not bind or execute pending operations.

## Explicit selected migration extension

[Selected operation migration](module-operation-migrations.md) adds per-node
KEEP/INCOMING/RETIRE, exact local edit review, immutable before-images and fresh
UNBOUND recovery identities. The original two live gates retain their blocker;
only the separate selected route may apply the reviewed result. Retired bindings
remain readable through target-DM history; their runtime rows are not deleted.
Changed/reactivated sources retain their operation UUID but cannot freshly execute
until rebound. Local executable edits remain in original immutable evidence;
recovery carries accepted inert sources, not original runtime versions or events.
A normal P09 full restore retains campaign IDs and is verified separately.

No commit, push, merge, publish, dnd-roll-local operation, cloud or physical-phone
validation is part of this work.
