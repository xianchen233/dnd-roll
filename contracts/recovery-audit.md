# A24a M3 recovery inventory and read-only remap readiness

This is the first bounded recovery audit, not a campaign-copy writer. Existing
P09 same-ID deployment restoration and A03h definition-only recovery retain their
contracts. No new HTTP route, role permission, database migration or traffic switch.

`RecoveryAuditCommand` runs offline with exactly four arguments: trusted P09 archive,
restored asset root, output JSON path, mode `SAME_ID` or `CAMPAIGN_REMAP`.
Connection settings use `RECOVERY_AUDIT_DATABASE_URL`, `_USERNAME`, `_PASSWORD`;
no JDBC URL/password/host path or game payload in output/errors. Run only on an
isolated restored database. Never use an audit to resume or remove a source fence.

Use a read-only repeatable-read transaction, UTC, current compiled Flyway checksum
compatibility and the explicit schema catalog. Compare all tables (including empty
ones), columns/defaults/nullability/constraints, row counts/digests, and all committed
immutable asset lengths/hashes with the archive. Unknown/missing/changed relations
or schema signatures and omitted/extra/different manifest tables block. The catalog
explicitly lists 13 equivalent CHECK renderings across 12 tables produced by native
PostgreSQL restore (array text cast versus per-element text cast). Only these exact
reviewed strings match; changed values, removed CHECKs, columns and foreign keys
still block. There is no general SQL or free-text rewrite. Missing,
changed or symbolic-link assets block. Output is CREATE_NEW; never overwrite a
report. No database mutation, event, game version or receipt changes.
Exit codes: 0 only for VERIFIED, 2 for a written BLOCKED report, 1 for failed audit
or invalid input. A remap-blocked plan must not look like a successful restore.

Report contains version/mode/status, complete table policies/counts, blockers,
and verified asset count only. SAME_ID can return VERIFIED only after every check.
CAMPAIGN_REMAP always returns BLOCKED with REMAP_WRITER_NOT_IMPLEMENTED plus
occupied-table requirements. It must never claim that global string replacement,
fresh UUID generation, or a definition recovery can continue saved A08–A23 effects.
Classify identity/access/receipt/typed evidence/opaque archive/asset/runtime tables
explicitly. Unclassified schema blocks instead of silently skipping it.

ID remapping still needs typed field paths and namespaces, exact card/turn/roll
and version maps, canonical annotation IDs, regenerated source-bound plan hashes,
new-target authority review, and a deliberate old-receipt policy. Free text and
pinned module/content identities must never be changed by substring replacement.
Source receipts stay source-scoped; a future writer cannot execute an old source
command in a new campaign merely because a card was copied. Existing A03h creates
fresh UNBOUND definitions and deliberately excludes live history/execution.

This slice's local acceptance includes actual inventory RED/GREEN, real PostgreSQL
schema/data/file mismatch checks, and one native P09 migration archive containing
real A08–A23 evidence from production-browser fixtures. Restore it through the
actual offline RestoreCommand, re-login, compare saved evidence/old receipts and
resume supported pending effects only after the new synthetic source is stopped.
Campaign-ID remap application, complete P09/M3 closure, capacity, delivery-HEAD CI,
cloud and physical-phone checks remain explicitly pending.
