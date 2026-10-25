# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Released versions will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Changes remain under Unreleased until a version is actually released; Git commits
and successful CI runs do not by themselves constitute a release.

## [Unreleased]

### Fixed

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
