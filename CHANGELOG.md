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

### Fixed

- Bind native browser fetch correctly so real-browser login succeeds.
- Fit the complete Pixi canvas inside its panel while preserving logical grid
  coordinates, including clicks near the right edge.
- Increment scene versions on committed moves and preserve both versions on retries.
- Install unzip in the backend build image so Maven Wrapper downloads the ZIP
  matching its pinned SHA-256 instead of silently selecting a tar archive.
- Retry transient connection resets while waiting for the Compose application to
  become ready, with a bounded timeout before running browser tests.

### Security

- Enforce CSRF, membership, visibility and token control on the server; exclude
  hidden token data from player snapshots and realtime invalidations.
- Reject fractional and string-coerced movement coordinates and versions.
- Materialize the WebSocket CSRF token during the HTTP handshake and close existing
  sockets when their HTTP session is invalidated.
- Preserve revoked membership and token control when demo initialization runs again.

[Unreleased]: https://github.com/xianchen233/dnd-roll/commits/main/
