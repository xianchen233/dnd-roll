# Deployment backup and migration operations

P09 implementation; actual acceptance and remaining gates are recorded in
`docs/verification.md`. This document does not constitute a release or a passed
physical other-host/TLS/capacity drill.

Set `BACKUP_OPERATOR_USERNAME` in `.env` to an existing fixed account. This grants
deployment-wide backup access, separately from all campaign roles; leave it blank
to disable management and automatic backups. The account still needs normal login
and CSRF. After login, open **部署备份管理**. Ordinary DMs/players receive no backup
status or bytes. Never put the backup directory on a public/static mount.

The standard image includes PostgreSQL 18 archive clients. For native Java runs,
install matching-major `pg_dump`/`pg_restore`, optionally specifying absolute paths
with `BACKUP_PG_DUMP` and `BACKUP_PG_RESTORE`. `BACKUP_STORAGE_ROOT` and
`ASSET_STORAGE_ROOT` must be private persistent directories. Compose supplies
separate `backup_data` and `asset_data` volumes. Preserve both with the database
volume; `down -v` destroys them.

## Manual and automatic backups

The application checks once a minute, starting one minute after startup, and
accepts at most one automatic task per UTC date while the operator is configured.
`BACKUPS_DAILY_ENABLED=false` disables only the automatic scheduler;
`BACKUPS_RETENTION` defaults to seven successful automatic archives. Manual and
migration archives are not rotated. No external scheduled task is required.

**生成手动备份** creates a durable request. Wait for **已完成** before downloading.
The task pauses new writes, drains admitted requests, holds PostgreSQL SHARE locks
over the application tables and archives the logical database and committed files.
No gameplay history or live image is deleted by rotation. Save downloads outside
the running host. Keep operator credentials and the matching application artifact
separately; neither environment passwords nor active sessions are in the archive.

503 WRITE_PAUSED with Retry-After means that a new write was rejected before its
mutation. An ambiguous task submission can be retried with the original request;
it confirms the existing task, including FAILED/INTERRUPTED, and never reruns it.
After addressing a failure, create a new task with a new request identity. Unknown
process outcomes remain INTERRUPTED after restart. Status/HTTP errors never expose
paths, connection strings or PostgreSQL stderr. Check installed clients, database
availability, private directory permissions/free space and server failure category.
If automatic rotation fails, the new backup remains downloadable and a separate
warning persists; correct storage permissions and let a later successful task retry.
Only one process may own a backup registry at once.

## Migration and isolated restore

1. Check **确认迁移期间旧实例持续停止写入**, then select **生成迁移备份并保持旧实例停写**.
   This writes a durable source fence before queueing. Success, failure and source
   restarts do not remove it. Wait for a completed download; stop the old backend.
   Never activate a second writable copy of the same campaign.
2. Copy the trusted ZIP and the matching application artifact to the destination.
   ZIP hashes detect corruption, not authenticity. PostgreSQL dumps can contain
   executable database functions; only restore archives from trusted operators.
3. Provision a separate empty PostgreSQL 18 database and a new private asset root.
   Supply new deployment environment credentials/TLS settings locally. Do not use
   `--clean`, overwrite an existing database or point at the old asset tree.
4. Run the offline entry point with `RESTORE_DATABASE_URL` (JDBC PostgreSQL URL),
   `RESTORE_DATABASE_USERNAME`, `RESTORE_DATABASE_PASSWORD` in the environment:

```powershell
java '-Dloader.main=io.dndroll.backup.RestoreCommand' -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher C:/private/backup.zip C:/private/new-assets
```

The command verifies exact archive names/JSON types, versions/migration checksums,
lengths and hashes before touching an empty target. It removes only that target's
proven-empty default `public` schema so the archive can recreate it, then restores
in a single PostgreSQL transaction. It compares every application table's row count
and digest, migrations, image metadata/references and actual image bytes. A failed
target stays quarantined; no automatic cleanup of user directories or traffic switch.
The final asset directory is published only after verification succeeds.

For Compose, use a new project name and fresh named volumes. Build the same source,
start only the new `db`, then run the offline entry point in a one-off backend
container with `--entrypoint java`, the three RESTORE_* environment variables and
a read-only bind of the downloaded ZIP. Restore into a **new child directory** of
the mounted asset volume, such as `/var/lib/dnd-roll/assets/restored`; the volume
root itself already exists. Set `ASSET_STORAGE_ROOT` to this child when starting
the destination backend. Do not start its HTTP backend before the restore succeeds.
The CI workflow contains an executable two-project example and ten fresh-session
business verification groups; its synthetic credentials are not deployment defaults.

5. With the source still stopped, start the target with the restored asset root.
   Re-login and check maps/fog, shared/private notes, old images/history, server dice,
   active turns, pending manual actions, versions and historical receipts. Retrying
   an old command must not repeat a roll/application or restore revoked control.
   Only then switch the entry point to the target. Recreate DNS/TLS separately.

Abandoning a migration is an explicit offline operator action: stop the old backend,
confirm the target has never accepted new progress, remove only the old backup
root's `write-fence` marker and restart the old backend. Do not remove the marker
while either instance is serving new progress. The archive does not copy the source
backup registry/fence or session cookies into the destination.

PostgreSQL's primary references: [pg_dump](https://www.postgresql.org/docs/18/app-pgdump.html),
[pg_restore](https://www.postgresql.org/docs/18/app-pgrestore.html).
