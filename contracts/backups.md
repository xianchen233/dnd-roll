# P09: deployment backup, restore and migration

Status: functionality and local/isolated-container acceptance passed (code f6c9130,
push39633129367 / PR39633137675, six full logs independently verified). Physical
cloud deployment/recovery remains deferred by the user as recorded below.
P01–P08 remain complete. An export of campaign records or a
restart of the original database is not a deployment restore. No merge/release.

Ordered slices: a write admission/draining and versioned archive validation;
b real PostgreSQL logical dump plus all committed immutable files; c persistent
manual/daily jobs, operator management/download and automatic retention; d offline
restore into a separate empty database and new asset root, migration write fence;
e original regressions, real restored-state/receipt/access checks and exact-SHA CI.
Behavior changes use executed focused failures; supplemental direct-GREEN tests
are identified in docs/p09-tdd.md. Slice e passed for local/container acceptance;
do not claim the deferred physical other-host gate or P10/M2 is complete.

## Authority and consistency

Deployment operations require the authenticated account explicitly configured by
the operator, independently of campaign DM membership. Disabled by default. Never
expose another campaign through its DM's backup permission. CSRF, versions and
durable request identity apply to management commands; authorize before replay.
All backup responses/errors are private/no-store. Never expose passwords, JDBC
URLs, storage roots, storage keys or dump contents in status, events or errors.

Pause admission of new state-changing requests before waiting for admitted ones
to finish (including transaction commit and immutable file publication). Reject
new writes with 503, Retry-After and WRITE_PAUSED; do not queue/replay them. Reads
remain authorized normally. A timeout releases a temporary pause without taking
an inconsistent backup. Backup/restore never modifies game versions or receipts.
Migration uses a durable fence, retained across source restarts until the operator
explicitly resumes; restored instances do not inherit source session cookies.
Do not run two writable instances for the same campaign during migration.

## Archive format v1

ZIP containing exactly manifest.json, database.dump (PostgreSQL custom logical
archive, no owners/ACL) and assets/<opaque UUID> for every committed map, RP and
note image, including loose committed uploads and all historical revisions.
Include app/schema/migration checksums, database major, creation instant, complete
table row counts/digests, file lengths/SHA-256 and recovery configuration guidance.
No credentials, sessions, generated player maps or host absolute paths. Store
outside source/public roots. Hash/copy streams, publish only complete validated
archives atomically. Failed/staging files must never be downloadable as completed.

Validate format/version, exact unique canonical entry names, bounded manifest,
declared/actual lengths and hashes, complete referenced files and migration
compatibility before restore. Reject traversal, absolute/backslash paths, duplicate
or extra/missing entries, malformed IDs/hashes, symbolic links in source files,
unsupported/newer database major or schema. Checksums detect corruption, not
authenticity; only restore operator-trusted deployment archives.

## Jobs, restore and acceptance

`GET /api/admin/backups` returns version, retention, paused, retentionFailed and persistent jobs.
Jobs expose id, actor, expectedVersion, kind MANUAL|AUTOMATIC|MIGRATION, state
RUNNING|COMPLETE|FAILED|INTERRUPTED|EXPIRED, at, finishedAt and bytes only.
`POST /api/admin/backups` and `/migration` require exactly
`{requestId: UUID, expectedVersion: nonnegative integer}` and return `{id,version}`.
Current deployment operator before receipt replay; changed endpoint/version under
the same ID is 409; an identical retry confirms the same task even after failure,
never starts it again. Fresh stale version or overlapping task is 409.
Migration persists a source write-fence before queueing; success/failure/restart
never removes it. `GET /api/admin/backups/{id}/content` streams only COMPLETE ZIPs
after current operator authorization; unknown404, unavailable409/503. Download is
an authenticated attachment with no-store/private and nosniff.

Daily automatic backup defaults to retaining seven successful automatic archives.
Manual archives are retained until the operator removes/exports them. Rotate only
completed backup files, never database rows or the live asset tree. Failures and
interrupted jobs remain visible after restart; failed runs do not rotate a good
backup. No external scheduled task is created by this development work.

Offline restore refuses a nonempty target database, an existing target asset root
or the source database. Validate all material before database restore; use an
isolated target, no automatic traffic switch. Check migrations, all table digests,
references and every file after restore. Failure leaves the source untouched and
the target quarantined. New deployment settings/passwords/TLS are supplied locally;
do not restore environment credentials. Resume target writes only after verification
and confirmation that source writes remain stopped.

Acceptance includes drain/rejection/timeout/concurrent retry/failure tests without
a database; actual PostgreSQL dump and separate-database/new-root restore with
saved maps, RP/note old images/history, private dice, active turns, pending manual
decisions, versions and original receipts; re-login/access/retry checks on target;
corrupt/missing/version-mismatched archives and nonempty target rejection; daily
retention/failure visibility; production management/download and player/DM denial.
Physical other-host transfer, capacity/touch/TLS remain explicit unpassed gates
unless actually exercised. P10 and A work are not implied complete.

User sequencing (2026-10-22): the cloud server exists but has no prepared runtime.
After overall functionality is complete, prepare its environment and jointly test
deployment and physical other-host recovery. Keep this final gate pending; do not
perform cloud deployment during P09 or substitute local/container isolation for it.
