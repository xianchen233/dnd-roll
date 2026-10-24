# A03c destination-map and blocker audit

2026-10-24 (Asia/Shanghai). Continue `feat/accounts-and-invitations` and draft
PR #1 without merge/release. Cloud/TLS, physical cross-host recovery and real
phones remain deferred. Existing A03a/b and previous A03c slices are retained.

Entry `git fetch origin` confirmed local/origin/PR HEAD
`8ddbb0db2c83179d230baebe17ab7d77fbcc4911`. Freshly downloaded six complete
logs for push40074564212 / PR40074567963: both completed/success; each 126 unit,
158 real IT, 248 frontend/build, 111 contracts, 52 production browsers and every
old restart/isolated-restore/capacity assertion, including four UNMAPPED VERIFIED
lines per browser job. External evidence: `.tools/a03c-boundary-entry-ci.log`.
This is entry evidence, never substituted for a later HEAD.

Actual commands ran the external `.tools/a01-maven.ps1` from `backend/`, with
real PostgreSQL on 55432 and independent test schemas. HTTP tests use random
ports. The new packaged acceptance service uses 18081, an independent database
`dnd_a03c_boundary` and separate assets/backups. No command stops, recreates or
resets the `dnd-roll-local` project, its 8080 service or its volumes. The existing
uncommitted local-preview handoff paragraphs are retained.

- `-Log a03c-boundary-red.log -IT
  'ModuleLifecycleIT#migrationRejectsUnavailableDestinationMapBeforeRecoveryWrites+migrationIgnoresDocumentOrderingButStillBlocksMaterialEdits'`:
  **2 tests, 2 assertion failures, 0 errors/skips**. A detached live map left
  its template asset unread by recovery, so preview allowed reattaching a missing
  file. Merely reordering two DOCUMENT nodes incorrectly blocked migration.
- Added destination-map link and file validation before any writes. Validate
  actual decoded bytes against both immutable template and live metadata; report
  `/map-assets/{id}`. Compare published DOCUMENT nodes by stable identity.
- First focused follow-up (`a03c-boundary-green.log`) had **1 pass / 1 failure**:
  notes were intact, but the test compared their wrapper's campaignVersion too.
  Assert unchanged note contents and the required campaignVersion increment.
  This assertion correction is not another product RED.
- Expanded first lifecycle run (`a03c-boundary-lifecycle.log`): **34 tests,
  0 failures / 1 setup error**. A no-image campaign has no storage directory yet;
  the new helper walked it unconditionally. Use the existing empty-safe file
  inventory. The source was corrected while that already-compiled run executed;
  its error remains in the log and is not claimed as passing.
- Additional direct boundaries cover corrupt bytes, dimensions/hash mismatch,
  missing metadata/mapping, file loss between review and apply, identical-byte
  repair and exact retry; null/changed/retired placement links, retired scene and
  missing card mappings; actual material edits, admission-policy changes and
  upstream manifest rights adopted through A03b. Every blocker checks unchanged
  runtime/instance/evidence/receipt/campaign count and no new files. These are
  direct regression checks, not claimed as separately observed REDs.

No route/schema/migration change. Contract checker remains 111 operations.
Final full `-Pintegration verify` (`a03c-boundary-full.log`): **126 unit +
163 real IT**, zero failures/errors/skips, including all **35 lifecycle IT**.
Frontend **248 passed**, production build and **111 contracts** passed. Actual
Java/PG stop and restart on the independent database/assets, followed by new-session
lifecycle `verify`, passed all old migration/update/recovery assertions
(`a03c-boundary-restart-{seed,verify}.log`). This local run is an actual process
restart, not a new independent-volume restore or production-browser claim.

Test setup initially tried a `createdb.exe` absent from the minimal PG runtime;
the existing psql client created the new database instead. No product RED is
claimed for that setup error. Process identities before/after restart are retained
externally. The tested Java/PG are stopped by verified identity afterward; data,
original source fence and previous restored databases/assets are retained.
After stopping PG and clearing all database environment variables, offline
`-UnitOnly` passed **126 unit** (`a03c-boundary-unit-pg-stopped.log`).

Read-only Docker inspection found the existing `dnd-roll-local` web/backend/db
already exited at **16:29:19–20 Asia/Shanghai**, before this resumed session;
8080 had no listener. This session did not start/stop/rebuild that project or
touch its data. Earlier running-instance handoff paragraphs are historical.

Exact new-HEAD push/PR verification uses `.tools/a03c-boundary-check-ci.mjs`,
requiring 163 IT and every previous assertion. Fresh full logs and results go to
`a03c-boundary-head-ci-final.log`, `a03c-boundary-ci-{HEAD}.json` and six
`a03c-boundary-job-{id}.log` files. Pending checks do not mean success.

A03c remains bounded: invalid links, unsafe bounds/recovery capacity/images,
published-material/policy and manifest changes still block. A03d manual
combinations, A03e Chinese controls/final acceptance and A04–A06/M3 remain open.
