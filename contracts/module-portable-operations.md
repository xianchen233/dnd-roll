# A03g: portable manual operation definitions, module format v3

This slice exchanges and edits inert definitions. Runtime binding, operation IDs,
target versions, live selective migration and execution belong to A03h. A package
import, preview, edit, download, save-as or process startup never executes a step.

## Versioned boundary and compatibility

Manifest `formatVersion:3` retains every existing manifest field and ZIP bound.
Its content is exactly `{nodes,play,operations}`. `play` retains the exact v2
shape and bounds. `operations` is exactly `{formatVersion:1,definitions:[...]}`;
the inner version names this stable-reference instruction vocabulary. Unknown,
missing, null, duplicate JSON keys and coerced scalars reject at every level.
V1 content remains exactly `{nodes}`, v2 exactly `{nodes,play}`. They do not gain
optional fields or operations. V1 still promotes to the existing v2 editing
document without guessing actions; its original full content-byte allowance holds.

Customization editing accepts either the old exact v2 document or this explicit
v3 shape, including when its immutable source was v1/v2. The user explicitly
enables the portable format; no read or refresh upgrades an old document.
Save-as chooses manifest v3 iff the edited document has the versioned operations
envelope (even when empty), otherwise v2. New v3 sources/baselines, revisions and
downloads preserve their complete format and stable identities. No database
migration, new route, or change to old live-operation commands is needed.

## Definitions and references

A definition is exactly `{nodeId,steps}`. `nodeId` is a unique canonical UUID of
a DM TRIGGER node; its existing node name/text describe the combination. At most
100 definitions, 1..20 ordered steps/definition, complete UTF-8 serialized
definition <=64 KiB, total content <=1 MiB. Empty definitions array is valid.
The node, scene, unit and placement uniqueness rules and content/image/dependency
limits remain unchanged. IDs are identities, never names or array positions.

Each strict step is one of:

- `{kind:"REVEAL_TOKEN",placementId}`: references an existing typed placement;
  that placement fixes scene/unit identity. No runtime scene/token ID or version.
- `{kind:"REVEAL_CHARACTER",unitId}`: references an existing typed UNIT spec.
- `{kind:"PUBLISH_TEXT",documentId}`: references a DM DOCUMENT node with no images.
- `{kind:"PUBLISH_MATERIAL",documentId}`: references a DM DOCUMENT node, preserving
  its declared ordered image links; the existing four-image limit applies.
- `{kind:"PREPARE_ENCOUNTER",participants,reason}`: participants are exactly
  `{unitId,kind,group,surprised,advantage,disadvantage}`. `unitId` references a typed
  UNIT; all other fields keep existing P05 participant/reason bounds. These are
  declared preparation facts, not generated rolls or confirmations.
- `{kind:"START_ENCOUNTER",reason}`: an inert intent requiring a separately
  reviewed current confirmed initiative/combat context in A03h. No source-group
  initiativeVersion, combat version, order, turn or confirmation may be imported.

Duplicate placement/unit/document targets reject within a definition. TEXT and
MATERIAL share the document-target identity. A unit revelation and a placement
revelation are independent; the same unit can also be a participant. At most one
PREPARE/START step per definition. P05 validates participant kinds/groups/source
strings/surprise and distinct units; no rule effect or dice is inferred from prose.
All internal references resolve by exact stable identity and kind. Missing external
dependencies keep existing inert-template diagnostics; they are never substituted.

Unknown kinds, scripts, HTTP requests, conditions, effects and source runtime
fields (`expectedVersion`, actor/controllers, tokenId/characterId/noteId, requestId,
initiativeVersion, receipts, rolls/history/progress) are rejected, not discarded.
Character *initial* resources remain legitimate v2 unit-sheet data; live resource
state cannot be captured by template save-as. Literal node text is never code.

## Lifecycle, updates and explicit A03h binding

Existing current-DM/CSRF/no-store authorization, actor→campaign locks, exact body
receipts, object versions, concurrency and rollback contracts govern all reuse of
the existing package/customization routes. Current authorization precedes replay.
An invalid edit/import leaves no rows/versions/receipts/files or partial template.
The immutable source/baseline and other customizations/live campaigns stay intact.

Template comparison uses definition `nodeId` at `/operations/definitions/{nodeId}`
when the three sides have envelopes; reordering definitions does not change their
identities, while step order is semantic. Adding/removing an envelope is an explicit
whole-envelope change. Every assembled selection is revalidated against the chosen
nodes/play/images/dependencies before application and recovery ZIP creation.

Import/edit/save-as never create live executable operations. New campaigns with
nonempty definitions reserve independent UNBOUND operation identities, with no
execution, entropy or runtime definition. [A03h binding](module-bindings.md) reviews
current mapped target versions and explicitly saves through ModuleOperations;
ordinary impact/trigger remains separate. Empty envelopes have nothing to bind.
Existing live text/structural migration previews carry a `/portable-operations`
blocker whenever nonempty portable definitions would require binding; neither gate
may silently omit them. Old exact receipts still replay under current authorization.
Chinese authoring explains the unbound state and offers current-target previews,
explicit binding reasons and exact uncertain-request retry. The separate
[selected migration route](module-operation-migrations.md) reviews per-node
source adoption/retirement and creates independent UNBOUND recovery mappings.

Acceptance: actual archive/parser/HTTP/UI/contract RED→GREEN, strict/bounds/reference
and old-format tests, authorized edit→save→download→reimport roundtrip, exact retry,
revocation/races/rollback, full regressions/production browser, real restart and
isolated complete restore. No commit, push, merge, publish, cloud/phone validation
or operation against dnd-roll-local is authorized by this slice.
