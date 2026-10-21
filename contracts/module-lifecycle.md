# A02: templates, customizations and independent campaigns

Source packages remain immutable A01 library records. A customization has its own
UUID and version, immutable source package ID/SHA and editing baseline, and a
current editable version-2 document. Creating another copy never shares mutable
state. Scope is the source campaign's current DMs, including reads and receipt
replays. Players never receive preparation data. All POSTs require CSRF.

`dnd-roll.module` formatVersion 2 keeps the v1 manifest, bounded ZIP and node
validation. content.json is exactly `{nodes,play}`. `play` is exactly
`{scenes,units,placements,admissionMode,approvedDependencyIds}`. All arrays may be
empty. Node descriptions, links, images and exact dependencies keep v1 semantics.
Scene specs `{nodeId,width,height,gridSize,map}` reference SCENE nodes; dimensions
1..200, grid 8..512. Nullable map is `{assetId,gridSize,offsetX,offsetY,distancePerCell}`
and uses the existing P02 calibration bounds. Unit specs `{nodeId,sheet,hidden,
shareFull}` reference UNIT nodes and a validated full P03 sheet. Placements
`{id,sceneId,unitId,x,y,hidden}` have independent stable UUIDs, reference these specs,
and must fit the scene. A unit may have multiple placements sharing one card.
No usernames, runtime IDs, controllers, logs or active turns enter a template.
Admission mode is REVIEW/FREE; approved IDs must be declared dependencies.
At most 50 scenes, 100 units, 500 placements; serialized content <=1 MiB.
DOCUMENT nodes become DM/TEAM notes (max four images each); other node kinds remain
inert preparation. Encounter execution and operation combinations belong to A03.
V1 opens as a v2 customization with empty play; no gameplay is guessed from prose.
V1 archive validation retains its original full content-byte limit. The added
editing wrapper does not reduce that allowance; subsequent v2 edits/exports must
fit the explicit v2 content limit.

Routes under `/api/campaigns/{campaignId}/module-customizations`:

- GET: `{campaignVersion,customizations:[{id,name,version,sourcePackageId}]}`.
- POST: `{requestId,expectedVersion,packageId,name}` creates an independent editable
  copy; expectedVersion is the source campaign management version.
- GET `/{id}`: `{id,name,version,sourcePackageId,sourceSha256,baseline,document,
  missingDependencies}`. Baseline is immutable v2 content; current dependencies
  are resolved again by exact ID/revision/rules.
- POST `/{id}`: `{requestId,expectedVersion,name,document}` replaces the complete
  editable content at the customization version. No runtime state is changed.
- POST `/{id}/templates`: `{requestId,expectedVersion,moduleId,moduleVersion,name}`
  saves a new immutable v2 library ZIP with edited document, original images,
  rights/rules/dependencies. New identity or unused version required. Returns
  `{id,version}`; version is campaign management version. No implicit overwrite.
- POST `/{id}/campaigns`: `{requestId,expectedVersion,name}` creates a **new**
  campaign, current actor as its sole DM. No existing target can be overwritten.
  Returns `{id,version:0}`; expectedVersion is the customization version.

Every command uses actor lock then source campaign lock, validates current DM
before historical replay, canonical fingerprint and a durable receipt. Same
request/body returns the original result before stale-version checks; changed
body/route/campaign returns 409. Campaign-result replay also requires current DM
in the created campaign. Stale fresh commands return 409. Maximum 100 copies per
library campaign. Source baseline is pinned at creation; saving or importing a
newer template never updates copies or campaigns (selective update is A03).

Instantiation revalidates format, exact dependencies and 5R gameplay readiness.
Unresolved dependencies or non-5R rules cannot instantiate; they can be edited and
exported as inert preparation. Source custom catalog dependencies are copied into
the new campaign with fresh IDs and explicit source-to-runtime mappings. All scene,
asset, card, placement and note IDs are fresh; control is assigned only to the new
DM. Maps start fully covered; visibility cannot reveal hidden units. New campaign
has no source RP/dice/combat/history/turn/session/progress. Card initial-state
history records the module creation. Current runtime data uses the existing P02–P08
APIs; there is no parallel resource implementation.

GET `/api/campaigns/{campaignId}/module-instance` (current target DM) returns pinned
source identities, customized content, stable-to-runtime mapping, version and
story progress. POST at the same route `{requestId,expectedVersion,progress}` sets
story progress (UUID node keys; literal strings <=2000 each, max 500), with its own
version/receipt. Template save never captures runtime HP/progress/records. No source
membership is needed to continue an already created independent campaign.

Customization, new campaign, assets, mappings, initial histories, version and receipt
commit atomically. File publication registers rollback cleanup with the outermost
transaction. Both immediate insert and deferred commit failure must leave no partial
campaign/files/copy/version/receipt. P09 archives include new tables and files.
Download/export uses A01 original ZIP routes, current authorization and no-store.
UI retains unsaved document and original uncertain command across refreshes, offers
exact retry, and clears private content on scope change or loss of permission.

Ordered TDD: two campaigns/resources/progress and save-as isolation; v2 archive
validation; authorized copy/edit/save/instantiate/progress and rollback/races;
Chinese editor and retry isolation; contracts, migration preservation, production
browser, real restart/isolated backup restore, exact-HEAD push/PR logs. Record actual
RED/GREEN in docs/a02-tdd.md. Cloud and physical phone checks remain deferred.
