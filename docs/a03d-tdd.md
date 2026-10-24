# A03d bounded manual operation service TDD

2026-10-24 (Asia/Shanghai). Continue `feat/accounts-and-invitations` and open draft
PR #1; no merge/release. No local-preview project/process/data changes. Native
test PostgreSQL uses 55432 and random HTTP ports; packaged acceptance uses 18082,
its own database/assets/backups. Cloud/TLS/physical cross-host/real phones deferred.

Entry fetch confirmed clean local/origin/PR exact HEAD
`ee9039af0d051afd7f2f76936decb3a9699c46ca`. Independently downloaded all six full
logs for push40085533515 and PR40085536284 using the strict existing verifier;
both completed/success, each 126 unit + 166 real IT, 248 frontend/build,
111 contracts, 52 production browsers, every old restart/isolated restore assertion
and 9-session/100-token/72-message capacity with zero errors. Evidence is external
`.tools/a03d-entry-ci.log`; entry logs never substitute for new HEAD results.

Contract first: [manual operations](../contracts/module-operations.md). This
service slice adds live-campaign definitions, not portable template scripts.
1..20 typed steps, <=64 KiB complete save, max 100 definitions; distinct targets,
one encounter step. DM can save/edit preparation, explicitly review current
targets/dependencies and manually execute. Reveal token and card separately;
publish only image-free DM text as an independent TEAM note; generate DRAFT
initiative candidates or start an already explicitly CONFIRMED order.
No rule formulas changed: generation/START validation is factored from the
existing P05/P06 services and reused for read-only preflight.

Actual failures before implementation:

- `a01-maven.ps1 -Log a03d-http-red.log -IT ModuleOperationsIT`: all original
  126 unit passed, new real HTTP test failed 200 expected / 404 actual at save.
- `node scripts/check-contracts.mjs`: exit 1, `A03d missing get` before adding
  OpenAPI; external `a03d-contract-red.log`.
- `npm test -- --run src/module-operations-api.test.ts`: one failure,
  `moduleOperations is not a function`; `a03d-client-red.log`.

The first implementation attempt used an unavailable Windows `python` alias
for a source transformation; no transformation occurred. The resulting compile
failure (`a03d-http-green-first.log`) is setup/implementation work, not a behavior
RED. Applied the transformation with available Node and corrected package-private
missing-target references. Next command (`a03d-http-green-second.log`) hit a Windows
JUnit temp-directory deletion error in unchanged BackupJobsTest (126 unit, 1 error).
No assertions/retries/timeouts were relaxed. Focused rerun with `-Unit
ModuleBlueprintTest -IT ModuleOperationsIT` passed 4 unit + the new real HTTP test
(`a03d-http-green-third.log`).

Expanded real HTTP checks produced actual product REDs:

- `a03d-composition-red.log`: reveal-card then prepare failed 200 expected / 409
  actual because the original-service prepare call used the pre-reveal card
  version. Preflight continues to require original versions; execution adjusts
  only versions produced by its own already-executed reveal step.
- Same run: new private route returned default cache header instead of documented
  `no-store, private`; added route family to the existing early private-header
  filter. A rollback helper initially named two SQL tables incorrectly; that
  preparation error was corrected before claiming rollback evidence.
- `a03d-start-snapshot-red.log`: after a START review, changed a participant's
  access/version; execution incorrectly returned 200 instead of 409. Hash now
  includes each confirmed participant's current access/detail as well as exact
  initiative and idle combat state. Five real HTTP tests then passed.

`a03d-focused-green.log`: 4 database-free parser unit + 6 real IT (5 HTTP and
V22→V23 migration preservation), zero failures/errors/skips. Real PG triggers at
immediate receipt insertion and deferred commit prove original token/notes,
histories, child receipts and combination evidence all roll back. Concurrent
identical requests have one effect; different DMs racing stale target versions
have one winner. Player/foreign DM/CSRF/strict fields, revoked historical replay,
cross-campaign receipt collisions, edit-after-execution and later hide preservation
are covered. Parser tests cover 20/21 steps, exact shapes, unknown code, duplicate
targets, encounter limit and UTF-8 save byte limit.

Additional HTTP checks cover exact local dependency resolution, later invalid
targets before the first reveal, campaign-version changes, retired targets,
ordinary non-module campaigns and 100/101 definition capacity. Original tests
remain intact. First full command passed 130 unit + 174 real IT (7 new HTTP + 1
migration). Expanded failure snapshots to card/initiative/combat state and every
child receipt; added real PNG-attached DM material rejection. Final full command
(`a03d-full-final.log`) passed **130 unit + 175 real IT**, including 8 operation HTTP
and migration preservation, zero failures/errors/skips; all 38 lifecycle IT kept.
Immediate/deferred failures cover reveal/text, reveal/prepare and reveal/START.
After native PG shutdown and cleared DB environment, offline `-UnitOnly` passed
all **130 unit** (`a03d-unit-pg-stopped.log`).

Local frontend `a03d-frontend.log`: 249 passed; TypeScript/Vite production build
passed (`a03d-build.log`), existing chunk-size warning retained. Contract checker
passes 116 operations, including strict operation shapes and exact execution
bodies. Independent structural comparison preserves every old path/operation and
schema except additive ApiError codes. Original requests are unchanged.

`scripts/check-operations-restart.mjs` adds public-API acceptance on a genuinely
imported/instantiated module. It composes all five supported operations, checks
damaged sheets/progress/old RP and explicit candidate confirmation, then edits the
definition and independently hides/damages/stops combat. Fresh-session verification
compares saved definitions, actor/time/evidence, all original histories and receipts;
old retries cannot reroll, republish, reveal again or restart stopped combat.
Wired into original CI restart and separate-project/volume restoration groups;
local packaged acceptance actually passed seed, then verified Java/PG shutdown,
same independent database/assets restart and fresh-session verify on 18082
(`a03d-restart-seed.log`/`a03d-restart-verify.log`). This local result is process
restart only, not a new isolated-volume restore. Captured old/new process identities
in `a03d-process-{before,after}.json`. Stopped only identified acceptance Java/PG;
all data retained. Exact new-HEAD CI must independently run the isolated restore.

A03e Chinese definition/review/trigger controls and final integrated browser
acceptance remain next. Portable template operation definitions, image publication,
automatic conditions, effects and resource execution are not represented as shipped.
A03/A04–A06/M3 remain open. New-HEAD dual CI must re-download all six complete logs
and include all old gates plus OPERATIONS VERIFIED checks; pending is not passing.
