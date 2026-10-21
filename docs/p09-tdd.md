# P09 actual TDD and recovery evidence

2026-10-21–22, Asia/Shanghai. Logs/private synthetic fixtures/runtime downloads
are outside the repository in `D:/workspace/dnd/.tools`. Credentials stay in process
memory; neither dumps nor game data are committed. P01–P08 are not redeveloped.

## Entry and actual failures

- AGENTS, continuation/verification, product/plan/CONTEXT and contracts read first.
  Fetch confirmed clean local/origin/PR HEAD `63ad3af430384bb1c11c958dc670cf3c7cbb08d4`.
  PR #1 remained open/draft/main. Fresh REST and all six full logs independently
  verified push39618022510/PR39618030841:87unit+114IT,221 frontend/build,83 contracts,
  both43-browser Compose suites and ten real container restart groups.
  `p09-start-ci-verified.log` is fresh evidence, not the prior agent's assertion.
- `p09-admission-red.log`:2 executed assertion failures (admission never closed,
  timeout never rejected). Minimum implementation then2 GREEN, including draining
  until the admitted request/commit ends, refusing new writes and competing pauses,
  timeout/interruption recovery and old double-close not resuming a later pause.
- First archive run failed to create the default temporary directory: environment
  failure, not behavioral RED. Explicit external java.io.tmpdir then
  `p09-archive-red-2.log`:6 run,3 assertion failures for path/extra/missing entries,
  corruption and incompatible versions. `p09-foundation-green.log`:6 GREEN.
  Writer-first test raised InvalidArchive on valid writing in
  `p09-archive-writer-red.log`; later5 archive tests passed, retaining old/live files
  and refusing overwritten archives/changed bytes. No simulated failure is called
  an executed integration result.
- Real HTTP `p09-http-red.log` first lacked anonymous private/no-store headers;
  fixing headers exposed actual404 in `p09-http-route-red.log`, then status GREEN.
  Manual generation then405 in `p09-manual-job-red.log`. First implementation
  correctly reported FAILED because the local embedded PostgreSQL package lacked
  pg_dump. Matching PostgreSQL18.6 clients were obtained outside source.
- `p09-restore-red.log`:real dump/download/receipts passed, then the offline restore
  entry point raised UnsupportedOperation. First implementation failed because
  an empty target already has public schema; remove only that proven-empty target
  schema (no CASCADE/--clean). `p09-restore-green-2.log`:4 real PG/HTTP tests pass.
  Further negative assertions reject corrupt archives before publication, existing
  asset roots and nonempty databases with the specific guard error.
- `p09-migration-fence-red.log`:migration reopened the source (expected paused,
  actual false). Durable fence implementation then11 focused tests GREEN in
  `p09-jobs-fence-green.log`. Automatic seven-file retention, manual/live-history
  preservation, interrupted recovery and failed-request identity were additional
  coverage initially GREEN; these are not presented as prior RED runs.
- `p09-rotation-failure-red.log`:a failed unlink incorrectly made a valid new
  archive FAILED. `p09-rotation-failure-green.log`:5 GREEN. Expiry is persisted
  before unlinking; generation stays COMPLETE, retention failure is separately
  persisted/visible, and a later successful task retries unlinking expired files.
- `p09-manifest-types-red.log`:1 actual assertion failure for coerced string/
  fractional archive versions/lengths. Strict recursive JSON shape/type checking
  then passes6 archive tests. `p09-registry-lock-red.log`:1 failure because a
  second live process could rewrite the registry; an exclusive file lock fixes it.
- `p09-automatic-storage-red.log`:expected one visible failed automatic attempt,
  actual zero after registry storage failure. It now remains FAILED in current
  status, with best-effort persistence and a fixed server log category; no secret
  filesystem/connection detail is returned. All7 job tests subsequently pass.
- `p09-paused-login-red.log`:a fresh operator login received503 while fenced.
  Authentication/session operations now remain available after security/CSRF;
  game writes still503. `p09-paused-login-green.log`:102unit+4 real backup IT pass.
- Frontend scaffold `p09-frontend-red.log`:4 executed failures for missing tasks,
  uncertain exact retry, denial/scope isolation and explicit migration choice.
  `p09-frontend-green.log`:4 GREEN. Final frontend225/build pass. New Chinese
  management shows persistent failures and authenticated streamed downloads;
  no campaign role implies deployment authority. A real screenshot was reviewed
  and checkbox layout corrected without changing business assertions.
- `p09-contract-red.log`:missing admin route assertion failure; now87 operations
  pass, with strict command shape/operator policy checks. Independent deepEqual
  against entry HEAD preserves all83 prior operations and every prior schema.

## Exact delivered-code CI

Code f6c9130c0bbdd7d6dd1aa9ddf235074bee827cb0 was pushed on the existing branch.
Push39633129367 and PR39633137675 completed/success. Independently downloaded all
six complete job logs and asserted each run's102unit+118 real IT with zero failures,
errors or skips,225frontend/build,87contracts,44production browsers,10actual
database/backend restart groups, and a second10group seed/verify across a real
migration archive into a separate dnd-roll-restored project with fresh volumes.
Each browser log has43PASS lines plus RESTORE VERIFIED, including the pending
manual decision/roll/resource invariants after target startup. First strict check
refused the still-in-progress PR run; re-fetch after completion passed. Evidence:
external p09-code-ci-first-check.log, p09-code-ci-verified.log, p09-job-{jobId}.log
and p09-ci-f6c9130c0bbdd7d6dd1aa9ddf235074bee827cb0.json. Final documentation HEAD
also requires independently checked push/PR logs. Existing PR #1 stays open/draft
against main. No merge/release. Cloud physical recovery remains deferred below.

## Full checks and actual isolated recovery

The complete backend initially passed101unit+118 real IT in
`p09-backend-final.log`. A later parallel run with the production-browser backend
hit PostgreSQL's100-connection limit:15 context errors, not behavior RED. Preserve
`p09-backend-delivered.log`; no connection limit or assertion was relaxed. Final
serial `p09-backend-final-serial.log` passes102unit+118IT, zero failures/errors/skips.
After identity-checked PostgreSQL shutdown and removal of all database environment
variables, `p09-unit-no-db.log` independently passes102unit as well.

`p09-browser-full.log`:44 production Chrome flows pass, all original43 retained,
60 seconds per test/zero retry. New browser creates/downloads a real ZIP, compares
download and HTTP SHA-256, persists task on reload and denies another account.
No test replaces actual backend/storage with mocks.

Ten existing seed scripts created dedicated acceptance state before migration;
`p09-restore-seed.log` has10 PASS. The archive was downloaded to
`p09-final-deployment.zip`. A currently unapplied manual decision was captured by
authorized read from the frozen source (`p09-final-pending.json`), not invented.
`p09-migration-backup.log` checks operator-only bytes, versions, exact replay and503
source writes. After an actual identity-checked Java stop/new process,
`p09-source-fence-restart.log` confirms fresh login, durable completed task and
continued write rejection. Then the source was stopped before target startup.

Standalone packaged RestoreCommand restored into a separate, newly created
database and `p09-restored-assets-final`, not the original DB/root. The first
PowerShell invocation parsed an unquoted -Dloader.main argument incorrectly before
Java entered the command: invocation error, not restore RED. Quoting it yielded
`p09-offline-restore-2.log`:RESTORE VERIFIED for all table counts/digests,17 migration
checksums, image references and302 immutable files. Source databases/files stayed
untouched. `p09-pending-verify.log` proves no automatic application/roll/resource
change at target startup. `p09-restore-verify.log` then has10 fresh-session PASS
for asset/token/map/P02/character/content/dice/combat/timeline/notes, original
receipts, histories, old images/exports and current hiding/revocation.

Both app processes and preview were identity-checked/stopped. Source fence and
both databases/assets/evidence are retained outside source. This is a real logical
restore into an isolated target on this machine; it is not evidence of physical
other-host transfer. CI's separate-project/fresh-volume restore is likewise reported
as isolated-container acceptance. P09 physical other-host gate, P10 capacity/touch/
TLS/full campaign and A work remain explicit. The user has a cloud server without
its runtime environment and explicitly defers environment setup, deployment and
physical other-host recovery to one joint drill after overall functionality is
complete. Do not start cloud deployment during P09. No external automation, merge or release.
