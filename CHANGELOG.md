# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Released versions will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Changes remain under Unreleased until a version is actually released; Git commits
and successful CI runs do not by themselves constitute a release.

## [Unreleased]

### Added

- Add independent 5R character sheets with all six field groups, multiple
  characters per player, sourced server calculations, reasoned DM overrides,
  immutable edit history and revocation-safe character control.
- Add separate HP/temp HP and bounded resource counters, explicit condition
  round ticks and DM-confirmed expiry reminders, independent token references,
  character copying and a versioned structured transfer format. Player imports
  require target DM review; copying pending imports keeps that review requirement.
- Add a character workspace with grouped field editors, calculation explanations,
  resource buttons, histories, DM permissions and import/export preview. V7 adds
  durable sheets/history/receipts and same-campaign controls/placements; P03 full
  acceptance is still in progress.
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
