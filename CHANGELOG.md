# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Released versions will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Changes remain under Unreleased until a version is actually released; Git commits
and successful CI runs do not by themselves constitute a release.

## [Unreleased]

### Added

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
  restart. Player image revelation and Pixi map interaction remain P02d.

### Changed

- Default Compose setup to a fixed bootstrap account. Keep sample accounts and
  the original playable campaign available through the optional demo profile.

### Fixed

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

### Security

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
