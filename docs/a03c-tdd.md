# A03c live preparation text gate — partial milestone

2026-10-23. Existing branch and draft PR #1 retained. A03a/A03b were not
redeveloped. This gate accepts preparation node name/text into the instance's
definition only. Runtime scene/card/note names and bodies remain independent.
Structural changes are reported as blockers and cannot be confirmed through this
API. Actual structural migration, scene removal/combatant replacement impact
confirmation and recoverable structural copies remain unfinished A03c work.

## Entry and actual TDD

- Independently fetched origin and PR metadata at
  `c105a5da8239d49aefe8507131a54c6570af6322`. Freshly downloaded all six full
  logs for push39821053960 / PR39821061499. Both completed/success, each with
  126 unit + 143 IT, 248 frontend/build, 105 operations, 52 production browser,
  original restart, isolated restore, complete campaign and capacity evidence.
  External evidence: `.tools/a03c-entry-ci.log` and downloaded job logs.
- First Maven command used an incorrect relative helper path and never ran;
  this is a preparation error, not RED. Corrected to the absolute helper path.
- Added actual HTTP test `liveDefinitionUpdatePreservesPlayAndRequiresFreshImpact`.
  Ran Maven integration verify restricted to that method: **expected 200, actual
  404** for `/module-instance/update-impact` (`a03c-http-red.log`). Existing 126
  unit tests passed in that command. Implemented locked planning/application,
  durable evidence, receipt and V21: all **18 lifecycle IT passed** in
  `a03c-http-green.log` (original 17 preserved).
- Added direct-GREEN boundary coverage for structural blockers, immediate and
  deferred-commit receipt failure rollback, concurrent exact retry, changed-body
  conflicts and target-role revocation. Initial full 145 IT had one test setup
  error: deleting a member still controlling a token violated the original FK.
  Changed setup to demote DM to PLAYER. No production change or relaxed assertion.
  Focused **19 lifecycle + 1 V20→V21 migration IT passed**, 126 unit passed:
  `a03c-boundaries-green.log`.
- A subsequent full run overlapped with the packaged acceptance backend and
  exhausted local PostgreSQL connections (`FATAL: sorry, too many clients
  already`, `a03c-backend-final.log`). It is a failed run, not a pass. Stopped
  the matching acceptance Java process and serialized subsequent verification;
  no assertion, timeout or retry was relaxed.
- V20 migration fixture contains old package/copy/instance/progress, lifecycle
  receipts and effective/before archives. Every old table column/row and all 20
  checksums remain equal. New evidence uses a campaign/instance FK.
- Contract check initially rejected three newly implemented but undocumented
  operations. After adding README entries, **108 operations passed**. All prior
  paths/operations and schemas independently structurally compared with entry
  HEAD and unchanged. This is contract synchronization, not a claimed product RED.
- Python shim was unavailable during a source-edit preparation command; used
  installed Node instead. No Python installation or external download performed.

## Boundaries and verification

Source and target current-DM checks precede replay, under source→target campaign
locks within the actor transaction. Instance and source customization versions
and complete impact hash must match. Evidence includes actor, request, hash and
full before-instance. Mapping, progress and original source provenance survive;
only accepted definition/customization version and instance version change.
No runtime service is invoked and no past result is recalculated.

Local frontend **248 tests** and TypeScript/production build passed; existing
bundle warning remains (`a03c-frontend.log`, `a03c-build.log`). No Chinese UI was
added; that remains A03e. Typed client methods expose the new routes.

Packaged lifecycle seed passed against the existing restored target with a
dedicated `a03c-backups` root, preserving damaged HP/custom resources, RP, notes,
maps, history, independent party and progress snapshots. Initial run used the
source backup root and correctly returned 503 under its write fence; that fence
was retained. Fresh seed used the separate target backup root and passed
(`a03c-restart-seed-ready.log`). Actual restart/verify is recorded below when run.

Actual Java and PG processes were stopped after checking command lines, PG was
restarted on the same cluster and packaged Java restarted on the same target and
asset root. Fresh-session lifecycle verify passed (`a03c-restart-verify-final.log`),
including original live update receipt, immutable before-instance and all damaged
resources/progress/records/maps/notes snapshots. An initial verify attempted before
Java readiness returned ECONNREFUSED; after readiness, verification passed. It is
not counted as a behavior RED. Acceptance Java was then stopped before the final
standalone full Maven run, avoiding the earlier connection contention.

The existing CI lifecycle script now also checks live evidence and original
receipts across actual container restart and separate-project/new-volume restore.
Exact final HEAD push/PR results must be freshly downloaded, not inferred from
entry SHA. External evidence uses `.tools/a03c-*`; credentials/runtime stay outside
the repository. Cloud/TLS/physical cross-host recovery and real phones deferred.

Final standalone full Maven integration verify: **126 unit + 146 real IT**,
zero failures/errors/skips (`a03c-backend-serial-final.log`). This supersedes the
two failed full runs above without concealing them. The final backend was not
running alongside packaged acceptance. Frontend 248/build and 108 contracts pass.
No local production browser run is claimed for this slice; the exact-HEAD CI
browser job executes the original 52 flows plus restart/restore script evidence.
Final CI index is external `a03c-ci-{HEAD}.json`, `a03c-job-{jobId}.log` and
`a03c-head-ci-final.log`; all six logs must satisfy the strict checker before
reporting success. A03c structural migration and A03d/e remain pending regardless.

After matching and stopping the local PG cluster and clearing database environment,
offline **126 unit passed** (`a03c-unit-pg-stopped.log`). One prior stop command used
a wrong relative path; its unit pass while PG was running is retained separately as
`a03c-unit-pre-stop.log`, not counted as stopped-PG evidence. Final listeners
8080/8081/5173/55432 are absent; source fence and restored target/assets remain.
