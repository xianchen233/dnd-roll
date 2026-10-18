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

[Unreleased]: https://github.com/xianchen233/dnd-roll/commits/main/
