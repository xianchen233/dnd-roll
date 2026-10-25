# A06 declared movement — executed evidence

2026-10-25, Asia/Shanghai. Existing `feat/accounts-and-invitations`, open draft
PR #1; no merge/release. Entry clean after fetch: local, origin and PR exactly
`8a2a6be4521347f4b249801ad46e748c724f06cf`. Independently downloaded six complete
logs with the existing strict checker: push40159268831 / PR40159271974 both
completed/success, 153 unit +185 IT, 291 frontend/build, 121 contracts, 55 browsers
and all prior restart/isolated-restore/capacity assertions. New entry evidence:
outside Git `D:/workspace/dnd/.tools/a06-entry-ci.log`. Not new-HEAD evidence.

Read AGENTS, continuation/verification, product/implementation plans, CONTEXT,
contracts and ordered backlog. Used dnd-rule-lookup A locate-only and B full SRD
movement page, then official SRD5.2.1 movement/glossary. Wrote
[contract](../contracts/movement-paths.md) and [evidence](research/movement-5r.md)
before behavior. Advisory declared 5-foot paths only; no hidden obstacle queries,
collision/effect/special-speed automation or compatibility inference.

## Actual RED / GREEN and preparation errors

- `a01-maven.ps1 -UnitOnly -Unit MovementPathTest -Log a06-path-red.log`:
  six executed tests, 3 failures/3 errors from the pending runtime calculator.
  Implemented loop/diagonal counting, additive terrain/mode costs, bounds and
  explicit manual cost. All six pass in subsequent complete unit runs.
- `a06-runtime.ps1 -TestBackend -IT MovementPathsIT`: four actual HTTP 404
  failures before controller wiring (`a06-http-red.log`). Domain/application
  code already existed at this point; this proves missing HTTP behavior, not
  a claim that all service code was written after a failing service unit test.
  Added controller/persistence/transaction integration. Initial GREEN attempts
  had fixture cleanup errors (scene/token/controller/character receipt FK order),
  not new product RED. Logs retained; corrected cleanup before counting passes.
- Frontend placeholder runtime: `npm test -- --run
  src/movement-workspace.test.tsx` actually failed four assertions
  (`a06-frontend-red.log`). Implemented explicit cells, preview/commit, exact
  uncertain retry, read-only recovery and scope/authorization cleanup. All four
  then passed (`a06-frontend-green.log`). Later tests add encoded API routes,
  unlinked token local recovery and stale drafts that never silently rebase.
- Contract checker actually failed `A06 missing get` before the OpenAPI additions
  (`a06-contract-red.log`), then passed 125 operations. Strict request fields,
  unknown modes, bounds and read-only preview policy are exercised without
  deleting prior checks or changing prior operation schemas.
- Additional real HTTP RED: private movement success lacked no-store headers
  (`a06-private-headers-red.log`, alongside the above cleanup error). Extended
  the pre-security header filter; success and anonymous failures then pass.
- Additional real RED: new combat reused the non-first character's previous
  spent budget, expected 0/actual 5 (`a06-new-combat-red.log`). START now
  initializes all participants; own later turn entry resets that character,
  GRANT/ORDER/same-current SWITCH do not reset. Focused 11 HTTP +1 migration pass.
- Additional real RED: malformed negative versions were reported as 409 stale
  drafts rather than 400 invalid inputs (`a06-context-red.log`). Added bounded
  context/UUID validation. Final full regression includes this test.
- Real production Chrome first fixture used campaign version 0 after invitation
  changed it (409); fixed to read the real campaign version. Next attempt used
  exact wrapping-label lookup for a select whose option text joined the label;
  gave the select an explicit accessible name, covered by a component assertion.
  One run accidentally used the stale dist after a test-only TypeScript options
  error; recorded as preparation failure. No retries/timeouts/business assertions
  were relaxed. `a06-browser-focused.log` subsequently passes the real loop,
  server preview, intentionally dropped committed response, exact single-effect
  retry and private final-value DM budget correction.
- Full production browser regression then found a real compatibility failure:
  55 pass/one failure because the unchanged tabletop console assertion saw 404s
  for ordinary unlinked tokens (`a06-browser-unlinked-red.log`). A focused HTTP
  test actually expected200/received404 (`a06-unlinked-http-red.log`); a component
  test also failed at runtime on the new unavailable capability shape. GET now
  returns only `{available:false}` for a currently controlled visible unlinked
  token; hidden/missing objects still404. Typed client clears only local movement
  state. No console assertions, mocks, retries or timeouts were weakened.

## Current evidence and remaining gates

Independent PG55436, new dnd_a06_test and dnd_a06 databases; production Java18091 /
web18092 with separate asset/backup roots. No dnd-roll-local process/data or older
source fence/restore target was operated. All fixtures are synthetic.

Final full regression: **159 offline unit +199 real IT**, zero failures/errors/
skips (`a06-backend-full.log`); includes 13 A06 HTTP tests +1 V24→V25 migration
test retaining every prior table/column/row and all24 checksums, including pending
A05 evidence/receipts. Earlier full197/198 runs are intermediate evidence only.
**299 frontend tests, production build, 125 contracts** pass. Independent parsed
comparison preserves every old path/schema/component/top-level policy
(`a06-contract-preservation.log`). Bundle size warning remains visible.
**56 real production Chrome flows pass**, zero retries (`a06-browser.log`),
including all unchanged original console/visibility/realtime assertions.
Viewed desktop/390px screenshots; these are emulation, not real phones.

Actual Java/web/PG stop/start with the final JAR preserves exact HTTP and real
browser-created paths, budgets, DM histories and receipts, plus original A03e
controls and A05 attacks in fresh sessions (`a06-restart-complete.log`). Two
Windows orchestration attempts stalled waiting for the long-lived PG descendant
(native redirection / Start-Process -Wait); the identified acceptance wrapper was
stopped and startup/verification completed in separate calls. The PG actually
stopped and restarted; this preparation issue is not product RED or data loss.

A full migration archive was downloaded with a verified durable source fence,
then this acceptance source was stopped. Restore into **independent PG55437,
new database/assets and HTTP18093/18094** verifies every database row, migration,
reference and immutable file (`a06-restore.log`). Fresh sessions retain all four
HTTP/browser movement captures and delivered A03e/A05 browser records; original
receipts replay without overwriting later state (`a06-restore-verify.log`). An
actually restored ledger then accepts its original current-context draft once,
preserving DM allowance and single-effect retry. All acceptance data/fences are
retained; services are stopped after identity checks. Offline159 units are
rechecked with both acceptance PG clusters stopped (`a06-unit-pg-stopped.log`).

Existing CI restart/isolated-restore/capacity assertions are retained; A06 adds
six MOVEMENT VERIFIED and one MOVEMENT RESUMED VERIFIED line per browser job.
The delivered HEAD must be independently checked with outside-Git
`a06-check-ci.mjs`: all six newly downloaded complete logs, 159/199 backend,
299 frontend/build,125 contracts,56 browsers, every original assertion and the new
movement markers. Evidence: `a06-ci-{HEAD}.json`, six `a06-job-*.log` and
`a06-head-ci-final.log`. Entry/intermediate SHA results cannot substitute.

Concurrent cloud-deployment entries appeared in continuation/verification during
this work. They are preserved in the working tree and excluded from this A06
commit. This task did not operate cloud deployment, private credentials, the
other checkout, dnd-roll-local or any prior source fence/restore target.
Cloud/TLS/physical host/real phone, A03 portable combinations/attached publication,
and other M3 remainder stay open independently of this bounded A06 slice.
