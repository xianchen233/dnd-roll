# A03e Chinese module controls: executed evidence

2026-10-25, Asia/Shanghai. Same `feat/accounts-and-invitations` branch and open
draft PR #1, no merge/release. Entry `9c17675d96eed3b3b55936dcf06b0d0b23cbcf34`
was clean after fetch and matched origin/PR. The GitHub connector had a transport
failure; the existing authenticated REST checker independently downloaded all six
complete logs again. `D:/workspace/dnd/.tools/a03e-entry-ci.log` confirms push
40095551240 / PR40095555043 completed/success: 130 unit + 175 real IT, 249 frontend,
production build, 116 contracts, 52 browser and every prior restart/isolated-restore
and capacity assertion. This is entry evidence, not the new HEAD's result.

## Actual RED → GREEN

Minimal empty component scaffolds allowed focused behavior tests to execute:

- `npm test -- --run src/module-selection.test.tsx`: all 8 failed for missing
  explicit selection controls (`a03e-selection-red.log`), then 8 passed with
  per-path decisions, server impact, immutable retry and revocation isolation
  (`a03e-selection-green.log`). The original A03a comparison stays read-only.
- `src/module-live-ui.test.tsx`: all 8 failed (`a03e-live-red.log`), then all 8
  passed (`a03e-live-green.log`). Current source version, separate text/structure
  gates, all exact confirmations, blockers and original migration retries tested.
- `src/module-operations-ui.test.tsx`: all 11 failed (`a03e-operations-red.log`),
  then the focused combined suite passed after typed editing, ordering, saving,
  review and explicit execution. One test helper initially queried the async list
  before it appeared; changed to `findByRole`, without increasing any timeout.
  Ambiguous text queries for hashes/actors were scoped to the visible confirmation.
- One actual additional RED (`a03e-confirmation-label-red.log`) required Chinese
  explanations of structural confirmation codes. The original server strings are
  still sent exactly; the new explanations do not substitute or infer consent.
- One actual logic RED (`a03e-dependency-red.log`) exposed over-blocking when the
  incoming candidate had missing dependencies that KEEP decisions could exclude.
  Review now lets the server validate the assembled result; missing dependencies
  in that impact still disable application. Same command passed 11 selection
  tests (`a03e-dependency-green.log`).

Expanded audits passed when first run; they are not claimed as extra product REDs.
They cover failed confirming reads after successful writes, Blob URL disposal,
late successful writes after denial, independent observation of parallel denials,
target version adoption only by explicit edit/save, duplicate targets, 20-step
limit, and retained drafts on a known save conflict. A repeated-action 20-step
test initially exceeded the existing 5s test limit in the full concurrent suite;
it now loads a real 20-step saved definition to test the same boundary directly.
No production behavior, assertion, retry or timeout was relaxed.

The first build caught invalid Testing Library `exact` options in new test helpers;
removed those unsupported options. Two external runtime preparation errors
(server binary location and a duplicated `frontend/` path) were fixed before any
acceptance claim. These are preparation errors, not product behavior REDs.

## Local checks

Evidence paths below are outside Git, under `D:/workspace/dnd/.tools/`:

- `a03e-backend-full.log`: Maven `-Pintegration verify`, independent PG 55433 /
  `dnd_a03e_test`, **130 unit + 175 real IT**, zero failures/errors/skips. All
  old migration and lifecycle tests retained. No backend or migration source was
  changed; existing OpenAPI paths/request schemas remain identical.
- `a03e-frontend-final.log`: **283 passed**, including all old 249 plus 34 new
  behavior tests. `a03e-build-final.log`: TypeScript/Vite production build passed;
  the existing >500kB chunk warning remains visible.
- `node scripts/check-contracts.mjs`: **116 operations** retained. This focused
  checker is not described as a general OpenAPI validator. `git diff --check`
  and syntax checks for both new evidence scripts passed.
- Production Chrome uses independent Java 18083 / static HTTP+WS proxy 18084,
  PG 55433 / `dnd_a03e`, private `a03e-assets` and `a03e-backups`; no Vite source
  server, HTTP/STOMP mock data, browser retry or timeout increase. All original
  52 and 2 new scenarios passed (`a03e-browser.log`). The two new scenarios
  separately passed again with real API snapshot/receipt capture enabled.

The first browser run passed selective updates, recovery downloads and live
text/structural changes, but its final logout step attempted to click a refresh
button already removed by authorization handling. It now asserts the actual
401 and disposed private panel directly; both scenarios then passed. Fault
injection calls the actual server before dropping a response, or delays an actual
response across campaign switch. It never fabricates a successful API response.

Screenshots cover desktop combinations and desktop/390px structural review;
both were visually inspected and no document overflow was found. A narrow browser
viewport is not a physical phone test.

## Real process restart and final CI gate

`a03e-process-{before,after}.json` records verified own Java/web/PG identities.
After capturing browser-created state and the original operation seed, actually
stopped those processes and PG, then restarted the same isolated database and
asset roots. Fresh-session `a03e-restart-verify.log` and
`a03e-controls-restart-verify-final.log` passed. These compare exact definitions,
target/card/scene/map bytes, recovery ZIP hashes, private/shared notes, history,
progress, actor evidence and original receipts. Replaying all captured original
commands cannot repeat reveals, publication, random rolls or start a stopped
combat, nor overwrite later damage/hiding/positions. This is local process
restart evidence, not a new local isolated-volume restore.

CI captures both actual browser-created domains via `E2E_A03E_CAPTURE_FILE`.
`check-controls-restart.mjs` requires exactly two complete captures and verifies
them after real database/backend restart and again after separate-project/volume
restore. All old persistence/restore/capacity gates remain present.

The final pushed HEAD must independently run the external
`a03e-check-ci.mjs`: exact local/origin/PR head, open draft/base main, both run
statuses completed/success and six freshly downloaded complete logs. It requires
130/175 backend, 283 frontend/build, 116 contracts, 54 browsers, all previous
assertions and **two CONTROLS VERIFIED** lines per browser job. The resulting
`a03e-ci-{SHA}.json`, six `a03e-job-*.log` and `a03e-head-ci-final.log` are the
final remote evidence. Pending or entry-HEAD success is never substituted.

The first pushed implementation HEAD `1db4702` failed before creating any job
(push40119506867). Its new job-level `env` incorrectly referenced `runner.temp`,
which is unavailable at that workflow key, as confirmed against the
[GitHub context availability table](https://docs.github.com/en/actions/reference/workflows-and-actions/contexts).
The browser run now receives the capture file via shell `$RUNNER_TEMP`; restart
and restore steps use the same path directly. No test/assertion/retry/timeout was
changed. The configuration repair requires new-HEAD push/PR checks; the failed
run and successful local tests are not described as remote success.

The repaired code HEAD **e680c36f98c447d74fbac0185845e4cc52a5f79d** subsequently
passed [push40119684682](https://github.com/xianchen233/dnd-roll/actions/runs/40119684682)
and [PR40119689443](https://github.com/xianchen233/dnd-roll/actions/runs/40119689443),
both completed/success. The independent checker re-downloaded all six complete
logs and passed every required assertion: 130 unit/175 real IT, 283 frontend and
build, 116 contracts, 54 production browsers, all previous restart/isolated-restore/
capacity gates and two CONTROLS VERIFIED lines in each browser job. Exact UI-created
domains and original receipts survived real containers and a separate restore
project/volume. Evidence: `a03e-ci-e680c36f98c447d74fbac0185845e4cc52a5f79d.json`,
six `a03e-job-*.log`, `a03e-code-ci-final.log`. This handoff-only commit's new HEAD
still requires independent checks; code-HEAD evidence is not substituted for it.

The `dnd-roll-local` project, its containers/data and earlier source fence/restore
targets were not operated. Acceptance services are stopped by verified identity
after use, with all data retained. Cloud/TLS, physical cross-host recovery and
real phones stay deferred. Portable template operation definitions, attached
material publication, automatic conditions/effects and A04–A06/M3 remain outside
this implemented bounded A03 slice. After stopping the independent PG and clearing
DB environment variables, all 130 offline unit tests passed again
(`a03e-unit-pg-stopped.log`).
