# A03h: selected operation migration and independent recovery

The original text and structural routes retain their `/portable-operations`
blocker. These additional current-DM, CSRF, private/no-store routes under
`/api/campaigns/{campaignId}/module-instance` review operations explicitly:

- POST `/operation-options`: exact `{expectedVersion,customizationVersion}`;
  returns both versions, campaignVersion and each stable node's current binding,
  current runtime definition, incoming portable definition and incoming name.
- POST `/operation-migration-impact`: exact `{expectedVersion,customizationVersion,
  campaignVersion,selections}`. Each selection is exactly
  `{nodeId,choice,bindingVersion,operationVersion}`. Every identity in the union
  of retained bindings (including retired ones) and incoming definitions must
  occur exactly once, at most 200. No implicit default. Choices are KEEP,
  INCOMING and RETIRE. KEEP preserves the current accepted source and runtime
  edits; for an absent/retired binding it leaves the identity inactive. INCOMING
  requires an incoming definition and adopts its source/name, preserving an
  existing operation UUID. Changed/reactivated sources become UNBOUND; previous
  executable rows remain immutable review evidence and cannot execute until a
  separate current-target binding. RETIRE removes the active definition/mapping,
  retains its UUID, source, runtime row, binding history, executions and receipts,
  and blocks fresh save/impact/execute/bind. Exact historical receipts may replay
  under current authorization without effects. Retirement reserves capacity.
  This includes the original successful binding receipt: retrying its exact
  request after retirement returns the saved acknowledgement without activating
  the identity, changing a definition or adding history. A new bind remains
  blocked, and a changed request under that key conflicts. Current target-DM
  authorization still precedes replay, including after source-library access
  has been revoked.
- POST `/operation-migrations` adds exactly `{requestId,impactHash,confirmations}`
  to the selected review. It recomputes the complete reviewed result under the
  existing actor, source, target locks. All instance/source/campaign/binding and
  runtime versions must match; local edits after preview reject without writes.
  Every returned confirmation must be echoed exactly once. Current DM in both
  source and target, and recovery access on replay, precede durable receipts.

Impact includes the selected document, the complete choices and existing structural
impact (including full runtime before-image, binding rows/history and executions).
Invalid assembled references, bounds, dependencies, unavailable image bytes,
capacity or unsupported material/policy changes block before any write. KEEP does
not invent a replacement target when the selected structure removes its target.
Selection may retain a TRIGGER node independently of the incoming node list; all
other references must resolve in the fully validated document.

Application, recovery campaign/files/instance, binding changes/history, structural
writes, immutable migration before-image and exact receipt are one transaction.
No selection binds, draws dice, publishes, reveals, confirms initiative or starts
combat. Original operations and execution evidence stay in the original campaign.
Recovery preserves active accepted portable sources in a v3 document and reserves
fresh UNBOUND operation IDs and freshly mapped targets. It never copies runtime
target versions, receipts, executions or initiative confirmation. Local runtime
edits remain in the immutable original before-image for explicit subsequent review;
they are not silently converted into portable instructions. Retired identities
are not resurrected. Ordinary same-identity P09 restore is a separate check.

Cloud, real phones, commit, push, merge, publish and dnd-roll-local remain excluded.
