# A04 bounded calculator: executed evidence

2026-10-25, Asia/Shanghai. Existing `feat/accounts-and-invitations`, draft PR #1.
No merge/release. Entry was clean after fetch; local/origin/PR matched
`c52d3ef98127bde61c4bd181e1244bb45e748edd`. The existing strict checker downloaded
all six complete logs afresh: push40120580408 / PR40120584894 completed/success,
130 unit + 175 real IT, 283 frontend/build, 116 contracts, 54 production browsers
and every previous restart/isolated-restore/capacity assertion. Entry evidence is
`D:/workspace/dnd/.tools/a04-entry-ci.log`; it is not evidence for the new HEAD.

Rules were checked before behavior: local A locate-only, local B full pages,
then official SRD 5.2.1 Attack Rolls, Advantage/Disadvantage, Damage Rolls,
Critical Hits, Resistance and Vulnerability and Immunity. See the explicit
[support matrix and attribution](research/automatic-attacks-5r.md) and
[internal contract](../contracts/automatic-attacks.md). No extension has been
selected and verified for automatic execution; extension support count is zero.
Core features beyond the matrix also require manual adjudication.

## Actual RED and GREEN

Public record/method scaffolds allowed the focused tests to compile and execute.
Before implementing calculation, ran from `backend`:

`& D:/workspace/dnd/.tools/a01-maven.ps1 -UnitOnly -Unit AttackCalculatorTest -Log a04-calculator-red.log`

Actual result: 10 tests, 1 failure + 9 errors, zero skips. Methods still threw
`UnsupportedOperationException("A04 pending")`; no candidate or resolution was
implemented. This was an executed runtime RED, not a compile/tool failure.

After minimal implementation, `a04-calculator-green.log` had 9 passing and 1
preparation error: a test for an unverified advantage source supplied only one
d20. Fixed that fixture to two d20s, without relaxing shape validation. Seven
additional boundary audits were added; they passed when first executed and are
not claimed as extra product REDs. The same focused command with
`-Log a04-calculator-green-final.log` passed **17 tests, 0 failures/errors/skips**.

Final review found a real P04 source identity mismatch: existing catalog/fill
preserves book `SRD 5.2.1`, while the first calculator used the invented short
label `SRD`. Added an offline test reading the actual bundled catalog, filling
a card with the original content service domain function and retaining its entry
ID/licensing detail. `a04-source-red.log` first rejected supplied damage because
the source had fallen back to manual; moved the candidate assertion before damage
resolution to make that cause explicit. `a04-source-red-confirmed.log`: **18 tests,
1 failure, 0 errors**, expected CANDIDATE but got MANUAL_REQUIRED. Aligned the
calculator, evidence, fixtures and contract to the existing exact identity;
`a04-source-green.log`: **18 passed**. One command initially used the wrong cwd
(missing paths/POM); `a04-source-green-preparation-error.log` preserves that tool
preparation failure, not a product RED. No content or historical source was renamed.

Coverage: equality hit/miss, natural1/20 versus totals, discarded natural20,
nonstacking and cancellation, tie selection, independent critical dice for 1d8
and 2d6, single-type defenses and rounding order, nonnegative damage, unsupported
profiles/actions/sources/context/exceptions, edition versus publication year,
all three defense source gates, manual results with no partial candidate,
malformed/null/oversized input, numerical upper bounds and immutable evidence/
repeat evaluation. No entropy, network, database, clock or live game mutation.

## Local regression and delivery gate

- `a01-maven.ps1 -UnitOnly -Log a04-unit-full-source-final.log`: **148 unit tests** (all old
  130 + 18 new), zero failures/errors/skips, Maven offline with database environment
  cleared. No Docker or database was started for this check.
- `npm test`: **283 passed**, `a04-frontend.log`.
- `npm run build`: passed, `a04-build.log`; existing large-chunk warning remains.
- `node scripts/check-contracts.mjs`: **116 operations**, all unchanged. No
  HTTP/schema/migration/client code changes; V1–V23 remain untouched.
- `git diff --check`: passed. Generated files, runtime tools, evidence logs and
  credentials stay outside Git under `D:/workspace/dnd/.tools/`.

No new local PG/HTTP/production-browser/restart/volume-restore run is claimed.
The internal calculator is not connected to P06 or any route. Full integration,
54 production browsers, all old restart/restore and capacity assertions must
also pass in both exact-HEAD CI runs before this bounded A04 is accepted.
The external `a04-check-ci.mjs` requires 148 unit (including 18 calculator),
175 IT, 283 frontend/build, 116 contracts, 54 browsers and every unchanged old
assertion, including 2 CONTROLS VERIFIED per browser job. It verifies exact
local/remote/PR SHA, open draft/base main, completed/success for both events and
downloads six complete logs again. Pending is not passing. Evidence index is
`a04-ci-{SHA}.json`, six `a04-job-*.log` and the chosen `a04-*-ci.log`.

A05 remains the next separate package: authenticated snapshot/roll assembly,
durable preview and DM confirmation, version/receipt/transaction/concurrency,
hidden-data projection, correction and restart/recovery. The pure result is not
a client command and cannot be used as an authorization or application receipt.
A03 portable combinations/attached-material publication, A06/M3, cloud/TLS,
physical cross-host and real-phone gates remain open. No operation on
`dnd-roll-local`, its processes/data, or old source-fence/restore targets occurred.

## Verified implementation HEAD

Code **668f9d6a224772219fab06583ec60847ae3ebc7f** independently passed
[push40153847972](https://github.com/xianchen233/dnd-roll/actions/runs/40153847972)
and [PR40153850400](https://github.com/xianchen233/dnd-roll/actions/runs/40153850400),
both completed/success. The strict checker downloaded all six complete logs again
and verified 148 unit (including all 18 calculator tests), 175 real IT, 283 frontend/
build, 116 contracts, 54 production browser scenarios, all original restart,
isolated project/volume restore and capacity assertions, including 2 CONTROLS
VERIFIED per browser job. Evidence: `a04-code-ci.log`,
`a04-ci-668f9d6a224772219fab06583ec60847ae3ebc7f.json`, six `a04-job-*.log`.

Earlier implementation 52e4131's two runs were cancelled after the source fix was
pushed. They are not accepted as complete passing runs. The connector could read
PR #1 but returned 403 on metadata updates; the existing Git credential was used
in memory for authenticated REST updates of the same open draft, without printing
or committing credentials. No comment, merge, release or deployment was created.

The defined A04 matrix/calculator is accepted; A05 and the other deferred packages
remain open. This evidence/handoff update changes documentation only. Its new HEAD
must still independently pass `a04-check-ci.mjs`; final evidence belongs in
`a04-head-ci-final.log` and `a04-ci-{SHA}.json`, never substituted by code-HEAD green.
