# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Released versions will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Changes remain under Unreleased until a version is actually released; Git commits
and successful CI runs do not by themselves constitute a release.

## [Unreleased]

### Fixed

- Return exact historical portable-binding acknowledgements after retirement
  without reactivating operations or rewriting history. Current target-DM access
  still precedes replay; fresh binds remain blocked and changed retries conflict.

- Keep portable-binding reads, previews and authorization failures private and
  uncached; send both binding commands through the authenticated CSRF POST client.

- Recheck the authoritative scene after movement-control denial, so revoking one
  token keeps the campaign and visible uncontrollable token available while
  clearing private movement evidence. Session loss still clears access immediately.

- Return the same unavailable movement capability for unlinked tokens and tokens
  whose linked card is hidden or awaiting review, without disclosing private
  character association through a distinct response status.

- Correct the migration-preview response contract to retain all confirmations
  when a review exceeds the application limit. Migration requests remain capped
  at 500 exact items; oversized reviews remain blocked before writes.

- Validate newly attached or changed migration map assets before writes, including
  currently detached files and broken links. Ignore document-node list order when
  checking published-material changes while continuing to block actual edits.

- Allow consecutive structural migrations after all mapped scenes retire. Recover
  fallback/ad-hoc scenes, linked and characterless tokens and independent live map
  images with explicit confirmations, preserving names, current scene and DM-only
  control. Reject unsafe ad-hoc token bounds or incomplete recovery before writes.

- Preserve unchanged live placement coordinates and token hiding during structural
  migrations. Evaluate scene bounds using the confirmed resulting positions and
  retirements, allowing safe shrink-and-move updates while blocking stranded tokens.

- Preserve retired combat identities and additional live character cards in
  structural migration recovery copies, with explicit confirmations, independent
  IDs and DM-only control. Validate recovery capacity before applying migration.

### Added

- Add A24d offline review of typed A08-A23 JSON rebinding and persisted plan-hash
  regeneration using original algorithms and historical source gates. Preserve explicit
  aliases, separate ledger versions, canonical owners, local keys and free text;
  keep source receipts as provenance and reject unsupported evidence or collisions.
  Reports remain BLOCKED/non-executable; campaign writing, authority, files and
  module graphs remain follow-up work.

- A24c offline typed A08-A23 evidence graph inspection with strict shape, provenance, historical version, alias and canonical annotation checks; reports remain blocked and non-executable.
- Adapt the October 26 UI concepts into the live React/Pixi application: portal login, obsidian/parchment materials and five local color preferences. Concept sources and existing game flows are preserved.


- Add an offline, non-executable campaign identity proposal with 76-table scope
  and column policies, namespace and reference checks, preserved timeline aliases,
  source-only receipts and CREATE_NEW private reports. Audit and planning share
  one read-only repeatable-read snapshot; JSON rebinding and a writer remain pending.

- Add an offline M3 recovery audit with an explicit 76-table schema catalog,
  complete archive row and immutable-asset checks, and CREATE_NEW private reports.
  Campaign remapping returns BLOCKED until a typed writer is implemented.

- Add independent direct healing of unchanged original A18 instability: preview
  then explicitly restore capped real HP, reset death counts and remove only the
  original 0HP Unconscious record, keeping temporary HP/resources/helpers/dice.
- Add V34 private treatment history bound to the source A18 damage and resulting
  shared history versions. Preserve pending unapplied new Help checks verbatim;
  the healed parent version prevents their stale continuation without rerolling.

- Add independent DM preview and explicit healing of the unchanged original A21
  instability result. Cap restored real HP, reset death counts and remove only
  the original 0HP Unconscious record while keeping temporary HP, resources,
  helpers and historical dice. Preserve the original A20 two-record boundary.
- Add V33 private healing history linked to the exact A21 damage source and both
  result-history versions; close both ledgers as HEALED without replacing evidence.

- Add a separate DM preview and explicit first bounded damage application for
  the unchanged original A19 restabilized outcome. Absorb temporary HP once,
  remove only the new Stable record, retain original 0HP Unconscious and record
  one death failure, or two for an adjudicated critical without doubling damage.
- Add V32 private damage history bound to both exact result-history versions.
  Mark A19 UNSTABLE and shared lineage RESTABILIZATION_UNSTABLE while preserving
  the original checks/completions and keeping subsequent treatment manual.

- Add a separate DM preview and explicit healing confirmation for an unchanged
  original A19 restabilized outcome. Cap real HP at its maximum and remove exactly
  the new Stable and original 0HP Unconscious records; retain temporary HP,
  resources, historical helpers and dice without rerolling or resource execution.
- Add V31 private restabilization-healing history bound to both exact ledger
  result versions; mark both lineages HEALED while preserving original evidence.

- Add a new dedicated Help check for unchanged original A18 instability, with
  failed checks retained, independent preview and explicit restabilization. Reset
  both death-save counts while keeping 0HP, temporary HP and the original
  Unconscious record; add a new Stable record without replacing prior evidence.
- Add V30 independent restabilization state/history bound to the exact A18 damage
  version. Mark shared lineage RESTABILIZED and preserve old receipts; new checks
  cannot be supplied by generic/old dice or pre-seeded ordinary roll requests.

- Add an explicit DM preview and confirmation for first bounded damage to an
  unchanged original A16 stable target. Consume temporary HP first, remove only
  the Stable record, retain 0HP Unconscious and record one death failure, or two
  for an adjudicated critical without doubling the final damage amount again.
- Add V29 immutable private damage history linked to the exact stabilization
  history version. Preserve original evidence and receipts; mark UNSTABLE and
  keep subsequent treatment manual. Zero or potentially fatal damage is not applied.

- Add explicit DM-adjudicated healing for unchanged original A16 stabilization
  outcomes. Preview then confirm real HP recovery capped at maximum, remove only
  the original Stable/0HP Unconscious record pair and preserve temporary HP,
  resources, helper and server check. Historical receipts never overwrite play.
- Add V28 immutable private healing history linked to the exact stabilization
  history version. Preserve original A16 evidence, record HEALED completion and
  permit a separately reviewed new Help attempt in a later ordinary 0HP episode.

- Add a DM-only ordinary 0HP stabilization flow: save a dedicated server Medicine
  check, preview, then explicitly apply source records while retaining 0HP and
  unconsciousness, keeping temporary HP and resetting death-save counts. Failed
  checks persist; exact retries do not reroll or overwrite subsequent play.
- Add V27 stabilization state, immutable history and receipts without modifying
  earlier migrations. Bind the latest attempt to both current character snapshots
  and rederive each application under version, authorization and transaction gates.

- Add an internal 5R ordinary-zero-HP stabilization calculator and source/support
  matrix. Successful bound-check plans keep 0 HP and temporary HP, reset death-save
  counters and describe Stable plus Unconscious records. The dedicated DM
  stabilization flow now uses this calculator.

- Add a reviewed first-aid action for unchanged A11/A12 knockout records. A
  dedicated server Wisdom (Medicine) check is saved, including failed attempts;
  a bound successful result can then be previewed and explicitly applied without
  healing. Private history retains the roll/DC/result. Exact retries cannot
  reroll or erase later play, generic rolls cannot pre-seed the check, and
  competing recovery requests have one effect. Action availability, contact,
  unsupported exceptions, 0 HP stabilization and remaining Prone stay with the DM.


- Add an explicit reviewed completion of the original A11/A12 knockout Short Rest.
  The server checks unchanged provenance, continuous game-time duration and no
  interruption, then atomically removes the original record without changing HP
  or resources. Private history retains the reviewed duration. Exact retries and
  competing healing/rest requests have one effect; rest timing, resource recovery,
  interrupted/restarted rests, first aid and remaining Prone stay with the DM.


- Add an independent reviewed healing workflow for unchanged A11/A12 knockout
  records. The server caps actual HP recovery and atomically removes the original
  annotation, preserving evidence and full character history. Full HP, temporary
  HP, modified/recreated records and unsupported states stay manual. Exact retries
  cannot heal twice or erase later corrections; resources, rest, first aid and
  remaining Prone effects require separate DM handling.


- Add a separate reviewed ordinary dagger melee knockout profile for Strength or
  Dexterity, with explicit thrown/manual fallback, dedicated confirmation and
  atomic HP/unconscious annotation application. Club behavior remains unchanged;
  rest, wake-up, extra attacks and unconscious consequences remain DM-managed.

- An explicit 5R ordinary-club melee knockout plan, separate DM confirmation
  and atomic application of 1 HP plus a sourced unconscious record. Original
  damage, ammunition and monster-death profiles keep their boundaries. Rest,
  first aid, healing-based removal and unconscious consequences remain manual.

- Add explicit DM ordinary-monster death review and separate confirmation. APPLY
  atomically records zero HP/temp HP and a sourced death annotation with one card
  version and complete combat history. Existing damage/ammunition zero-HP fallbacks
  stay unchanged; knockout, resurrection and other linked effects remain manual.

- Add an explicit ordinary-shortbow arrow-counter profile: a DM selects and reviews
  one source counter, then confirms server-derived one-arrow consumption and target
  damage before separate atomic application. Misses also spend one arrow; insufficient
  ammunition and unsupported effects fall back to manual adjudication. Other resources,
  inventory, recovery, old confirmation profiles and private/public boundaries retain
  their existing meaning.

- Add a current-DM server damage plan for ordinary SRD 5.2.1 weapon previews,
  consuming temporary HP before HP while leaving positive HP. Explicit damage
  confirmation and separate atomic application preserve resources, conditions,
  private dice, linked corrections and original receipts; unsupported zero-HP,
  condition and death-save cases require manual adjudication.

- Add read-only 5R range hints for two currently visible single-cell tokens on a
  reviewed 5-foot grid. Chinese controls compare declared reach or normal/long
  range without rolling dice or applying attacks. Current visibility and scene/
  token versions are rechecked; hidden endpoints stay private and unsupported
  calibration/context requires a manual decision.

- Add explicit per-node KEEP/INCOMING/RETIRE migration review for portable
  operations. Preserve local edits, retired identities, execution histories and
  immutable before-images; source adoption requires a separate current binding.
  Create independent recovery campaigns with fresh unbound operation and target
  mappings. Chinese controls preserve exact uncertain requests and require every
  impact confirmation. The original text/structural gates keep their blockers.

- Reserve independent unbound operation identities for each new portable v3
  campaign. Let the current target DM preview actual mapped target versions and
  explicitly save a binding with an immutable before-image and atomic receipt.
  Binding never reveals, publishes, rolls, confirms initiative or starts combat;
  ordinary impact review and execution remain separate. Pending identities reserve
  capacity, local edits are visible, and exact retries cannot overwrite them.
- Add V26 binding/history/receipt tables, authenticated Chinese binding controls,
  strict HTTP contracts and restart/full-restore evidence for pending bindings.
  Later selective source updates, retirement and independent recovery mappings
  remain pending behind the existing live migration gates.

- Add module format v3 for inert portable manual combinations with strict stable
  trigger, unit, placement and material references. Existing v1/v2 archives remain
  unchanged; customization edits, save-as, selective comparison and recovery ZIPs
  retain the versioned definitions. Nonempty portable definitions explicitly block
  campaign launch and live migration until per-instance binding is implemented.

- Add explicit DM publication of a private module material with all ordered images
  into independent TEAM note/image copies. Reviews validate actual image bytes;
  combination transactions retain original private sources and clean only new
  public files on rollback. Exact retries publish once, with current DM access
  checked before historical receipts. Existing text-only steps keep their scope.

- Declared 5R movement paths with per-cell modes/terrain, authoritative budget
  previews, character-owned spent distance across placements, advisory overage
  warnings and private DM cost/final-budget decisions. Durable journals and
  idempotent receipts commit with the position and budget; legacy unmeasured
  moves make the ledger incomplete until DM review.

- Persist DM-only SRD 5.2.1 weapon previews with trusted catalog/card snapshots,
  server attack and independent critical-damage dice, explanations and an evidence
  hash. Unsupported or unreviewed cases remain explicit manual adjudication.
- Separate final-value confirmation from atomic P06 application. Preserve exact
  receipts, immutable evidence and linked current-value corrections across later
  edits; publish chosen summaries independently without exposing dark dice or HP.
- Add Chinese preview, confirmation, application, correction and summary controls,
  with locked exact retries and private-state isolation across campaign/API changes.
- Add V24 preview/receipt storage and restart/isolated-restore acceptance for both
  HTTP-created pending previews and records created through the real browser.

- Add a bounded 5R SRD 5.2.1 weapon attack candidate calculator with immutable
  input/evidence, natural 1/20, sourced advantage/disadvantage, critical dice plans
  and single-type immunity/resistance/vulnerability. Unreviewed contexts, unknown
  versions and unsupported effects require manual adjudication. This internal
  calculator does not apply HP, resources or effects; A05 remains separate.

- Add Chinese per-path template selection, authoritative impact review, live text
  updates and individually confirmed structural migration with recovery access.
  Add typed live combination editing, ordering, target selection, explicit version
  adoption, save/review/trigger and immutable execution evidence. Uncertain requests
  retain their exact identities/bodies through refresh; campaign changes and
  authorization failures dispose private state and pending commands.
- Verify browser-created module updates, recovery ZIPs/maps/cards and operation
  receipts after actual restart and isolated restoration, retaining all prior
  browser, persistence and capacity checks.

- Add DM-editable live module operation combinations with strict typed steps and
  limits, server-bound impact review, atomic execution through existing token,
  character, note, initiative and turn services, and immutable actor evidence.
  Current authorization precedes durable receipt replay; retries preserve later
  play. Text publication explicitly blocks attached notes; candidate generation
  leaves ordering/confirmation to the existing initiative workflow.

- Preserve all V22 rows, columns and migration checksums while adding V23 manual
  definition, receipt and execution tables. Extend restart and isolated backup
  restoration acceptance with saved manual combinations and historical retries.

- Add a separate live structural migration gate with exact runtime impact
  confirmations, current DM checks, atomic independent playable recovery campaigns
  and durable receipts. Retire removed scenes/placements while preserving references,
  combat identities, damaged resources, conditions, progress and historical results.
  V22 retains all previous rows and migrations. Published materials, policy and
  manifest changes and invalid live links remain explicit blockers.

- Add a separate, hash-bound live-instance preparation-text update gate with
  current DM checks in both source and target, atomic before-instance evidence
  and durable single-effect receipts. V21 preserves existing play and records;
  structural changes are reported and blocked pending explicit migration support.

- Add current-DM selective customization updates with explicit KEEP/INCOMING
  decisions, server-recomputed hash-bound impact, validated image/dependency
  closure, atomic durable receipts and downloadable before-image recovery ZIPs.
  V20 retains effective archives and immutable update provenance. Existing live
  campaigns remain independent; selection UI and structural live-instance updates are pending.

- Add a current-DM-only three-way template comparison against the immutable
  baseline and saved customization. Stable identities survive list reordering;
  field edits and delete/edit conflicts show all three values in Chinese UI.
  Preview checks version, module identity and rules without writing live state.

- Add independent module customizations with immutable source/editing baselines,
  versioned full-definition editing, v2 typed scenes/cards/placements and save-as
  template ZIPs. New campaigns receive fresh assets, units, notes and permissions;
  ongoing HP/resources, story progress and historical records stay independent.
- Add a Chinese customization editor and independent story-progress workspace,
  exact uncertain-command retry, explicit template identities and campaign entry.
  V19 stores copies, pinned instance provenance/mappings and durable receipts;
  campaign creation and image cleanup honor immediate and deferred rollback.
- Add a current-DM-only inert module library with ZIP identity/version manifests,
  bounded strict JSON/image/hash/reference validation, exact missing-dependency
  previews, explicit legacy confirmation and independent copies. Chinese import
  and original ZIP download support durable exact retries; V18 commits library
  bytes/version/receipts atomically and deployment backups retain all package data.
- Add phone workspace navigation for character/resources, dice, RP/logs and notes,
  with touch-sized controls, keyboard tab navigation and mounted draft/uncertain
  request preservation across workspaces. Campaign changes and access denial
  still dispose private state.
- Add a continuous DM/two-player acceptance campaign, 360/390px touch-emulated
  checks, whole-party restart/recovery comparisons and a bounded nine-session
  capacity workload with real map/history/backup measurements.
- Add deployment-operator backup management, persistent manual/daily jobs, seven
  successful automatic archives by default, authenticated ZIP downloads and
  visible interruption, generation and retention failures. Backup rotation never
  deletes live images or game histories.
- Add write admission/draining, consistent PostgreSQL 18 logical archives with
  immutable map/RP/note images, strict versioned manifests and offline restore
  into an empty isolated database and new asset root. Migration backups retain a
  durable source write fence across failure/restart; restore compares all table
  rows/digests, migrations, image references and file hashes before activation.
- Add collaborative team notes and DM-only preparation, immutable visibility,
  versioned edits with actual editor history, persistent PNG/JPEG images, literal
  search, pagination and complete Markdown/JSON record exports with image manifests.
- Add a Chinese notes workspace with current-version editing, exact uncertain
  retries and permission-aware image/history/export downloads. V16/V17 add notes,
  durable receipts and same-campaign/same-visibility attachment references.
- Add RP/OOC identity snapshots, immutable message revisions, durable PNG/JPEG
  attachments, permission-aware replies and a unified message/dice/action timeline
  with paged search, session archives and Markdown/JSON record exports.
- Add a Chinese timeline workspace and shared bot shortcuts through existing
  dice/action/turn services. P07 local and exact-code-SHA push/PR acceptance
  passed; consistent backup/restore remains later work.

- Add manual combat actions linking independent characters, saved attack sources
  and immutable server rolls, DM hit/miss judgement, explicit versioned HP/temp HP/
  resource application and linked corrections that preserve subsequent play.
- Add character turns from confirmed 5R initiative, DM skip/switch/reorder/stop
  and temporary movement grants. Exploration remains free; out-of-turn dice and
  actions remain available. Duration/effect resolution remains explicit DM work.
- Add a Chinese combat workspace, exact uncertain-request retries and filtered
  journals, with complete P06 local and exact-SHA push/PR acceptance. No release
  is made; consistent backup/restore remains later work.

- Add server-owned persistent dice with bounded expressions, every raw die and
  kept/discarded result, saved calculation/name/actor sources, DM-only rolls and
  one versioned, idempotent service for buttons and text commands. Rolls never
  spend character resources or infer combat outcomes.
- Add 5R initiative preparation for independent characters, explicit identical
  monster groups, sourced surprise disadvantage, candidate review and reasoned
  order confirmation/history. Hidden participants leave no player rows or ranks;
  V9/V10 preserve prior data and add durable rolls, initiative and receipts.
- Add a Chinese dice/initiative workspace with precise uncertain-request retry,
  reconnect/permission recovery, visible-record pagination and expandable dice
  details. Manual combat acceptance is tracked separately under P06.
- Add a searchable, attributed selection of ten SRD 5.2.1 entries, immutable
  campaign custom entries and explicit 5R/2014/custom source identities. Search,
  structured fill and execution coverage are reported separately.
- Add independent DM review/free content admission, server character-fill previews
  and versioned, retry-safe fill with retained sources/history. V8 persists custom
  content, admission settings and receipts; catalog changes never rewrite sheets.
- Add Chinese catalog filters, provenance/licensing details, explicit preview and
  reusable campaign custom entries. Filling does not grant proficiency/mastery,
  apply choices/effects, spend resources or execute dice.
- Add independent 5R character sheets with all six field groups, multiple
  characters per player, sourced server calculations, reasoned DM overrides,
  immutable edit history and revocation-safe character control.
- Add separate HP/temp HP and bounded resource counters, explicit condition
  round ticks and DM-confirmed expiry reminders, independent token references,
  character copying and a versioned structured transfer format. Player imports
  require target DM review; copying pending imports keeps that review requirement.
- Add a character workspace with grouped field editors, calculation explanations,
  resource buttons, histories, DM permissions and import/export preview. V7 adds
  durable sheets/history/receipts and same-campaign controls/placements, with
  complete character acceptance and all previous regressions retained.
- Add integrated P02 acceptance for two-scene fog/calibration and independent
  token visibility/control, with a real-service restart check retaining both
  scenes, original/safe image hashes, versions and historical command receipts.
- Add real two-client browser acceptance for offline scene/fog recovery,
  control/member revocation and committed-but-lost fog responses. Verify exact
  retries preserve later cover and require a successful fresh read to synchronize.
- Add server-rendered current-map PNGs and DM manual cell reveal/cover with
  durable fog receipts and a V6 migration preserving earlier data. Maps start
  fully covered; map replacement, calibration and detach reset revealed cells.
- Add native Pixi pan, cursor-centered zoom/reset, snapped token drag previews,
  two-point geometric measurement and DM rectangular reveal/cover drafts, while
  retaining numeric movement and visibility controls as independent behaviors.
- Add decoded secret-pixel checks and a real-service restart acceptance script
  for fog, image versions and historical receipts, alongside existing checks.
- Add DM token creation and complete configuration of names, grid positions,
  visibility and multiple current-member controllers, with a dedicated authoring
  panel, API and durable token command receipts. Keep shared movement payloads
  and secret-free scene invalidations compatible.
- Add a V5 migration that backfills token/control campaign ownership, enforces
  same-campaign membership foreign keys and preserves earlier data and receipts.
- Add real HTTP acceptance for token configuration, control revocation and
  historical receipts across backend/PostgreSQL restart, alongside asset checks.
- Establish the React/TypeScript/PixiJS frontend and Java/Spring Boot backend with
  PostgreSQL/Flyway migrations, versioned HTTP/WebSocket contracts and Docker Compose.
- Add session login/logout, campaign membership filtering, a demo campaign and
  permission-controlled exploration movement on a responsive grid.
- Persist movement receipts and object versions so retries are idempotent and
  competing edits are rejected. Synchronize clients through private STOMP
  invalidations, authoritative snapshots and reconnect refreshes.
- Add unit, real PostgreSQL/HTTP/WebSocket and two-browser tests, build workflows,
  TDD evidence, an implementation backlog and resumable handoff records.
- Add an insert-only bootstrap account, campaign creation with an empty grid,
  invitation-only account registration, existing-account acceptance and DM member
  and invitation management. Add a V2 migration for campaign management versions,
  hashed invitations and durable management command receipts.
- Add DM creation and activation of multiple empty scenes, with one shared current
  scene for the whole campaign. Preserve inactive scene positions, versions and
  control grants, and persist scene command receipts with the campaign version.
- Add a V3 migration that preserves existing scenes and backfills each campaign's
  current scene, with a foreign key enforcing campaign ownership.
- Add durable private PNG/JPEG assets with stable IDs, authenticated downloads and
  a DM preparation panel for upload, preview, download and scene map calibration.
- Add a V4 migration for same-campaign map references and durable asset command
  receipts. Preserve logical grids, existing scenes, tokens and earlier receipts.
- Add a dedicated Compose asset volume and a real HTTP acceptance script for
  checking file hashes, map references and receipts across application/database
  restart.

### Changed

- Render the static Pixi tabletop only when its image, units, drafts or viewport
  change. Immediately repaint image removal without waiting for a frame ticker.
- Default Compose setup to a fixed bootstrap account. Keep sample accounts and
  the original playable campaign available through the optional demo profile.

### Fixed

- Keep builtin module dependencies automatically admitted when creating a campaign;
  map only copied custom approvals so later P04 content-policy edits remain valid.
- Prevent cross-campaign transitions from pairing the next campaign with the previous scene when loading maps, fog and unit controls.

- Keep scene create/switch commands from disappearing when a background list read
  supersedes their preflight. Preserve the command's own expected version and
  report server conflicts explicitly, while newer reads own the displayed list.
- Preserve valid Pixi gestures across same-scene snapshot and safe-image refreshes
  while immediately discarding obsolete pixels. Cancel gestures when scene,
  selection, token version or control changes. Keep loaded fog drafts editable
  during background validation while saving waits for fresh state.
- Keep existing controller checkboxes editable during background authoring reads
  while submission waits for fresh data. Stop old save acknowledgments from
  reappearing after a new draft begins, preserving form position during editing.
- Bind native browser fetch correctly so real-browser login succeeds.
- Fit the complete Pixi canvas inside its panel while preserving logical grid
  coordinates, including clicks near the right edge.
- Increment scene versions on committed moves and preserve both versions on retries.
- Install unzip in the backend build image so Maven Wrapper downloads the ZIP
  matching its pinned SHA-256 instead of silently selecting a tar archive.
- Retry transient connection resets while waiting for the Compose application to
  become ready, with a bounded timeout before running browser tests.
- Match invitation expiry precision to PostgreSQL so retry metadata remains stable.
- Keep login and invitation acceptance available when a consumed invitation can no
  longer be previewed, allowing recovery after a lost registration response.
- Count Unicode password characters consistently in the browser and backend.
- Confirm a saved move when its direct refresh is superseded by a successful
  realtime refresh. Keep pre-acknowledgment and revoked snapshots from confirming
  the move, and recover confirmation after a transient refresh failure.
- Clear selected units and obsolete movement retries on a scene switch; ignore
  delayed movement results from the previous scene while retaining access-denial
  handling. Increment only the moved token's scene version.
- Confirm saved asset/map commands through the newest valid read started after
  acknowledgment, including superseding realtime refreshes. Ignore older errors
  and recover synchronization after a temporary read failure without reposting.
- Preserve asset files when a database commit acknowledgment is lost. Recheck
  matching metadata under a fresh campaign lock before cleanup; retain files
  when the outcome cannot be established so a committed receipt stays usable.

### Security

- Rebuild original A16/A18 provenance and verify the saved A18 plan hash for direct
  instability healing. Bind its full journal to new plans, authorize before old
  receipts, keep private declarations out of player history and roll back all
  treatment writes together; A17/A20/A22 source gates remain explicit.

- Verify the complete A16/A18/A19/A21 lineage and original A21 plan hash before
  instability healing, bind its full private journal to new plans and authorize
  before receipt replay. Roll back the card, both ledgers, histories, journal and
  receipt together; private declarations stay out of player character history.

- Reconstruct and bind A16/A18/A19 provenance, full target and three versions
  before restabilization damage. Authorize historical receipt replay, keep damage
  declarations private and roll back both ledgers/card/histories/journal/receipt
  together; old Help and healing entries cannot consume this new instability.

- Bind restabilization healing to the reconstructed A16/A18/A19 lineage, full
  target and all three current versions. Authorize before historical receipt
  replay, keep private healing declarations out of player history and roll back
  both ledgers, card, histories, journal and receipt together on failure.

- Reconstruct the full original A16/A18 outcome and bind each new Help to the
  current target/helper snapshots, versions and authoritative server check.
  Check current DM access before receipts, keep private check/review/reasons in
  DM history and use safe character-history descriptions. Roll and apply failures
  roll back dice, card, both ledgers and receipts together.

- Bind stabilization damage to the complete original applied card, authoritative
  check, current versions and rederived plan. Keep reason and reviewed damage
  inputs private, use safe character-history descriptions and check current DM
  access before receipts. Immediate and deferred failures roll back all writes.

- Bind stabilization healing to the full original applied card, authoritative
  saved check, current versions and rederived plan. Check current DM access before
  receipts; keep requested healing amount, review and reason in DM-only history,
  using a safe result description in player-readable character history.

- Keep stabilization checks, reviews and operation reasons in DM-only evidence.
  Player-readable character history contains only a safe outcome description;
  authorization precedes receipt replay, and generic dice cannot pre-seed the
  dedicated stabilization request namespace.

- Replace covered map samples with opaque black on the server and freshly encode
  player PNGs without source metadata or transparent hidden RGB values. Keep
  original downloads DM-only and token hiding independent of map revelation.
- Bind image reads to current membership, scene and exact scene version under
  the campaign lock; reject historical URLs and disable HTTP and derivative
  caching. Re-covering affects future delivery; previously saved public pixels
  cannot be retracted.
- Clear displayed images before refresh and revoke object URLs and Pixi textures
  on invalidation, scope changes or access denial; isolate late image responses.
- Recheck current DM and target ownership before token receipt replay; historical
  authoring receipts never restore old configuration or control grants. Keep
  hidden flags and controller lists confined to DM authoring endpoints.
- Advance affected token and scene versions when removing a controller's campaign
  membership, preventing stale authoring forms from regranting control after
  rejoin. Current visibility and control still gate historical movement receipts.
- Clear obsolete movement selection/retries when control or visibility changes;
  isolate late private authoring results after scope changes or access denial.
- Enforce CSRF, membership, visibility and token control on the server; exclude
  hidden token data from player snapshots and realtime invalidations.
- Reject fractional and string-coerced movement coordinates and versions.
- Materialize the WebSocket CSRF token during the HTTP handshake and close existing
  sockets when their HTTP session is invalidated.
- Preserve revoked membership and token control when demo initialization runs again.
- Expire single-use invitations after seven days, return their secret only once,
  store only SHA-256 hashes and share links through URL fragments.
- Validate new credentials before hashing and create invited accounts, membership
  and invitation claims atomically. Preserve existing passwords and DM roles.
- Scope member removal and control revocation to its campaign, close the removed
  user's realtime connections after commit and prevent old invitation or command
  replays from granting access or removing a subsequently rejoined member.
- Clear scene data and pending commands after access denial and invalidate all
  in-flight snapshots so a delayed response cannot restore a revoked map.
- Restrict inactive scene metadata and scene management to current DMs, recheck
  authorization before scene receipt replay, and reject movement or movement
  replay for tokens outside the current scene. Clear DM panels on access denial.
- Observe authorization errors from each concurrent management read so an earlier
  network failure cannot conceal a later access denial or leave old maps visible.
- Restrict original assets and map authoring metadata to current campaign DMs,
  including direct downloads and command replay; keep asset IDs and original pixels
  out of player snapshots and events. Disable caching on content responses.
- Validate image format, decoded dimensions, pixel count and byte limits, ignore
  client filenames and keep server storage keys private. Roll back failed asset
  commands and clean newly published files on normal failure.
- Revoke private preview URLs and isolate delayed asset responses on scene/campaign
  changes or access denial; preserve exact upload bodies for uncertain retries.

[Unreleased]: https://github.com/xianchen233/dnd-roll/commits/main/
