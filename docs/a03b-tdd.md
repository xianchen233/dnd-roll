# A03b actual TDD and acceptance

2026-10-23, Asia/Shanghai. Existing branch and draft PR #1; no merge/release.
A03a is retained. A03b implements customization selection/application APIs;
A03c live updates, A03d manual operations and A03e Chinese controls/final gates
remain separate unfinished work. Cloud and real phones remain deferred.

## Entry

Read AGENTS, continuation, verification, design/plan, CONTEXT, work packages,
contracts README and A01/A02/A03 contracts. Clean local/origin HEAD after fetch:
`48b086aa8a72266303d1f1f347c42d67dc5728f2`. Connector unavailable; independently
downloaded six complete GitHub API logs using existing Git authentication.
push39811174761 / PR39811184432 both completed/success, PR open/draft/main.
Strictly checked each 121 unit + 136 IT, 248 frontend/build, 100 operations,
52 browser, twelve restart groups, complete archive/isolated restore/capacity.
Evidence: external `.tools/a03b-entry-ci.log`; no older SHA substituted.

## Actual cycles

- Stub selection returned current document. Focused ModuleUpdateTest: 8 tests,
  **3 failures**, zero errors/skips (`a03b-selection-red.log`). First helper
  invocation had an incorrect relative path; that preparation error is not RED.
  Implement explicit exact path choices and stable-ID replacement: **8 passed**
  (`a03b-selection-green.log`). Added direct-GREEN image hash/rights and dangling
  reference boundaries: **10 passed** in subsequent runs.
- First HTTP command was stopped by still-failing unit tests, not HTTP RED
  (`a03b-http-red.log`). After unit GREEN, actual two HTTP tests both failed
  **404 vs 200/400** (`a03b-http-red-behavior.log`). Implemented read-only impact,
  locked recomputation/apply and V20 durable archives/evidence: complete existing
  ModuleLifecycleIT **13 passed** (`a03b-http-green.log`). Added concurrent retry,
  missing dependency/live resource preservation/recovery: **15 passed**
  (`a03b-http-boundaries.log`). Final real PNG/save-as and deferred-commit rollback
  plus V19 preservation: **17 lifecycle + 1 migration IT passed**
  (`a03b-final-boundaries.log`). These added cases were direct GREEN, not claimed RED.
- Contract operation assertion actually failed on missing update-options
  (`a03b-contract-red.log`). Added five operations and strict schemas; first
  schema run correctly rejected missing README routes. After documentation,
  **105 operations passed**; all previous 100 operations and all old schemas
  were independently structurally compared with entry HEAD and unchanged.

## Local evidence

- Full offline Maven integration verify: **126 unit + 140 real IT**, zero
  failures/errors/skips (`a03b-backend-full.log`). Subsequent final focused run
  contains the two new lifecycle cases and migration check: **18 IT passed**,
  so 143 distinct IT are exercised across the runs; the first full command did
  not contain 143. Final exact-SHA CI must independently run the full set.
- **248 frontend tests**, TypeScript/production build passed
  (`a03b-frontend.log`, `a03b-build.log`). Existing >500kB bundle warning remains.
  No selection UI added; typed client exposes all APIs including recovery URL.
- V19→V20 preserves every old table column/row and 19 migration checksums,
  including a populated customization, instance progress and lifecycle receipt.
- Real HTTP checks exact selection completeness, metadata/image choice, internal
  closure, exact unresolved dependencies, stale/tampered impact, current DM,
  single-effect concurrent/uncertain retry, immediate/deferred commit rollback,
  downloadable validated before ZIP and original source/baseline preservation.
  Existing live damaged HP, instance and RP snapshot remain identical after apply.
- Packaged acceptance uses the existing restored target/assets, retaining source
  write fence. Initial seed ran before backend readiness and failed ECONNREFUSED
  (`a03b-restart-seed.log`); fresh prefix after readiness passed update/lifecycle
  seed (`a03b-restart-seed-ready.log`). Actual Java and PG restart then fresh-session
  verification is recorded separately; never infer success from seeding alone.

The existing lifecycle restart/isolated restore script now also asserts update
choices, provenance/effective archive, recovery ZIP hash, unchanged live snapshots
and exact replay. Final push/PR logs and remaining checks are recorded in
verification/continuation. Local evidence/runtime remain outside the repository.

Final local production browser: first full 51 passed/1 failed while the old P02e
map module load reported net::ERR_NO_BUFFER_SPACE; failure trace preserved outside
repo. Original four P02e tests then passed (17.3s), followed by full **52 passed
(2.5m)**, original assertions/60s/zero retry unchanged. Evidence a03b-browser-full.log,
a03b-browser-failure-trace.zip, a03b-browser-p02e-recheck.log and
a03b-browser-full-recheck.log. No claim that the first full run passed.

Actual packaged Java/PG restart fresh-session verification passed
(a03b-restart-verify.log). Stopped Java/preview after matching command lines, then
pg_ctl stopped the matching cluster; no listeners 8080/8081/5173/55432.
Offline **126 unit passed with PG stopped and database environment removed**
(a03b-unit-pg-stopped.log). Restored target/assets and original fence retained.

Code 61305b3 push39818867630 passed with full 126+143/248/build/105/52 logs and
all original + new UPDATE VERIFIED restart/isolated restore assertions. PR run
39818876262 was still running at documentation update; strict checker rejected it.
Final documentation HEAD has its own push/PR checks: external a03b-head-ci-final.log,
a03b-ci-{HEAD}.json and six a03b-job-{jobId}.log. Never substitute code SHA for HEAD.
