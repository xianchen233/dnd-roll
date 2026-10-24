# A03c recovery archive and confirmation limit audit

2026-10-24 (Asia/Shanghai). Same `feat/accounts-and-invitations` branch and draft
PR #1; no merge/release. Cloud/TLS, physical cross-host recovery and real phones
remain deferred. Previous A03 slices and their assertions remain intact.

Entry fetch confirmed clean local/origin/PR HEAD
`5bac33f9e26d1ce9061461cc477623251c596c81`. Independently re-downloaded six complete
logs for push40080009608 / PR40080012457; both completed/success, each 126 unit,
163 real IT, 248 frontend/build, 111 contracts, 52 production browsers, all old
restart/isolated-restore/capacity assertions and four UNMAPPED VERIFIED lines.
External entry evidence: `.tools/a03c-limits-entry-ci.log`.

Tests were written before any proposed production change; the existing runtime
behavior passed, so no backend production code was changed and **no runtime RED
is claimed**. The subsequent contract audit found a real response mismatch:
OpenAPI capped preview confirmations at 500 even when HTTP correctly returned
501 complete required items with `/confirmation-limit`.

Added a focused contract assertion for a blocked 501-item response, while keeping
500-item application success and 501-item/duplicate application rejection.
`node scripts/check-contracts.mjs` actually failed at the response's `maxItems`
validation (`a03c-limits-contract-red.log`, exit 1). Removed that response-only
cap and documented the complete blocked review; request limits remain unchanged.
The same command then passed **111 operations** (`a03c-limits-contract-green.log`,
exit 0). An independent structural comparison against entry HEAD confirms that
every route, request and other schema is identical; only the preview confirmation
response cap/description changed (`a03c-limits-compatibility.log`).
This is a compatible response-contract correction, recorded in Changelog.
No database schema, route, backend behavior or TypeScript client type changed.

Actual commands used external `.tools/a01-maven.ps1` from `backend/`, with real
PostgreSQL on 55432, independent schemas and random HTTP ports:

- `-Log a03c-limits-first.log -IT
  'ModuleLifecycleIT#migrationAcceptsExactlyFiveHundredConfirmationsAndBlocksFiveHundredOne+migrationAppliesCombinedAssetCountLimitAndRecoversExactlyOneHundredTwentyEight+migrationBlocksCombinedExpandedAndCompressedArchiveSizesWithoutWrites'`:
  **126 unit + 3 real IT**, zero failures/errors/skips on first execution.
- Expanded the archive test to reduce over-limit images and actually migrate
  both large valid recoveries. Same focused command, log
  `a03c-limits-focused-final.log`: **126 unit + 3 real IT**, zero failures/errors/
  skips. Final shared-asset and source-revocation assertions are included in the
  subsequent full run, not retroactively claimed for this command.

Coverage includes:

- 499 and exactly 500 confirmations accepted in preview; 501 explicitly blocked.
  A truncated 500-item command cannot bypass a 501-item review. Missing, duplicate
  and unknown confirmations create no recovery, evidence, files or receipts.
  Exactly 500 applies once, persists its complete before-image and independent
  recovery, retries exactly, and still enforces player/source-DM revocation.
- 127 template image identities plus one independently authored live image fit
  128; two scenes sharing that live asset count it once. Another identity with
  identical bytes produces 129 and blocks before writes. Removing the extra
  fixture allows migration into an actual 128-image independent recovery.
- Four valid PNG maps plus the source image total exactly 32 MiB; manifest and
  content overhead make the complete archive exceed its expanded limit. All PNGs
  use valid CRC-checked ancillary chunks, real decoding and immutable hashes.
  Reducing the aggregate to about 31 MiB permits actual migration.
- Two independently identified 6 MiB high-entropy valid PNG maps remain below
  32 MiB expanded, but their ZIP exceeds 10 MiB and is blocked. One such image
  fits and migrates. Recovered metadata and actual file SHA-256 match originals;
  exact retries create no additional recovery. No assertion weakens archive limits.

Tests use independent schemas on the native test PG; they never use 8080 or the
`dnd-roll-local` project/data. Read-only inspection still finds that project's
containers exited at 16:29, before the earlier development session. Source fence,
previous restored databases/assets and local-preview data remain untouched.

Final full `-Pintegration verify` (`a03c-limits-full.log`): **126 unit + 166 real
IT**, zero failures/errors/skips; includes all **38 lifecycle IT** and the final
shared-asset/source-revocation assertions. This follow-up does not claim a new
local frontend/build/production-browser/process-restart/isolated-restore run;
backend production code is identical to the previously restarted 5bac33f. Fresh
new-HEAD CI must still independently execute every existing gate.
After verified PG shutdown and clearing database environment variables,
offline `-UnitOnly` passed **126 unit**, zero failures/errors/skips
(`a03c-limits-unit-pg-stopped.log`). Test data and synthetic file evidence remain.

Final full regression and exact new-HEAD CI evidence use external
`.tools/a03c-limits-*`. The strict CI verifier requires 166 real IT and every
previous frontend/browser/restart/isolated-restore/capacity assertion. Results go
to `a03c-limits-head-ci-final.log`, `a03c-limits-ci-{HEAD}.json` and six job logs.
Pending is not passing; entry results never replace new-HEAD evidence.

The A03c service limit audit is covered by executable checks. Intentional blockers
for invalid links, unsafe recovery/bounds, published-material/policy and manifest
changes remain. Next package is A03d editable manual combinations, then A03e
Chinese controls and final acceptance. A03/A04–A06/M3 are not declared complete.
