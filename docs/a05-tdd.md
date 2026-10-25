# A05 durable preview: executed evidence

2026-10-25, Asia/Shanghai. Existing `feat/accounts-and-invitations`, draft PR #1;
no merge/release. Entry clean after fetch, local/origin/PR exactly
`68a8a0652636f07c28c1549e83cb980bbc72d6c9`. Independently fetched PR metadata and
downloaded all six complete logs again using the strict A04 checker. Both
[push40154621542](https://github.com/xianchen233/dnd-roll/actions/runs/40154621542)
and [PR40154625315](https://github.com/xianchen233/dnd-roll/actions/runs/40154625315)
were completed/success: 148 unit, 175 real IT, 283 frontend/build, 116 contracts,
54 browsers and every original restart/isolated-restore/capacity assertion,
including two CONTROLS VERIFIED lines per browser job. Entry evidence outside
Git: `D:/workspace/dnd/.tools/a05-entry-ci.log`. It is not new-HEAD evidence.

Read continuation, verification, product/implementation plans, CONTEXT, contracts
and backlog before behavior. Wrote [A05 contract](../contracts/attack-previews.md)
first. Used `dnd-rule-lookup`: commercial A locate-only for attack rolls, B SRD 5.2
search then full Attack Rolls, Damage Rolls and Critical Hits pages. A04's explicit
5.2.1 matrix/formulas remain unchanged. Canonical bundled P04 attack identity,
licensing and exact source are checked separately from DM context review. Unknown
or altered entries/mastery and unsupported/unreviewed situations stay manual,
without rolls or partial candidates. Extensions remain unsupported.

## Actual RED / GREEN and preparation failures

- `a01-maven.ps1 -UnitOnly -Unit AttackPreviewServiceTest -Log a05-adapter-red-runtime.log`:
  four executed tests, one failure/three errors, methods still threw `A05 pending`.
  First invocation `a05-adapter-red.log` used two incorrect Resolution accessor
  names and did not compile; that is a preparation failure, not product RED.
  `a05-adapter-green.log`: four pass after implementing the adapter.
- Real PG/HTTP `a05-runtime.ps1 -TestBackend -IT AttackPreviewsIT`: four tests
  actually failed on missing routes (404), `a05-http-red.log`; after V24/store/
  controller/transaction wiring, `a05-http-green.log`: 152 unit + four IT pass.
- Contract checker `a05-contract-red.log` failed on missing A05 operations before
  the OpenAPI additions; `a05-contract-green.log` passes 121 operations. All old
  parsed paths and schemas were independently compared unchanged against entry.
  Final audit `a05-factor-contract-red.log` also actually rejected an A04-bounded
  factor source (unknown rules label / 1000-character detail) because it reused
  the narrower character-source schema. Added a dedicated factor source schema
  and matching client type; 1000 accepted, 1001 rejected, all 121 operations pass.
  After the initial push, the final response audit verified all real captured
  previews/public summaries, then caught an output-only bound: a valid
  1000-character exception plus its manual-reason prefix exceeds 1000. Executed
  `a05-reason-contract-red.log`, widened only new manual-reason response strings to
  2000 and reran 121 contracts and actual-response validation. No runtime behavior
  or input bound changed; the new HEAD must have its own full dual CI verification.
- `npm test -- --run src/attack-previews.test.tsx`: five actual failures against
  the pending UI (`a05-ui-red.log`), then five pass (`a05-ui-green.log`). Later
  audits add API encoding, old-domain late denial and current-value correction.
- `a05-immutable-red.log`: five adapter tests, one actual failure: changing the
  caller's factor list changed saved evidence. Defensively copy review lists and
  nested command resource lists. The subsequent full unit run passes 153.
- `a05-factor-red.log`: real HTTP expected 200, got 400 for a valid 400-character
  factor reason. Preserve full review evidence; bound P05 labels by code points
  without changing its existing 200-character limit. Subsequent focused run
  `a05-focused-green.log`: 153 unit + seven IT pass.
- First full integration run had 179 passing IT and a migration-fixture error:
  synthetic old rolls/actions omitted the `at` required by the existing timeline
  trigger. Fixed the fixture, preserving trigger and assertions; focused migration
  passed (`a05-migration-green.log`). Another command passed two unquoted IT names,
  yielding no matching tests after passing units (`a05-focused-command-preparation-error.log`).
- First browser attempts exposed test preparation issues: numeric `toHaveValue`,
  reload without reselecting the campaign, and screenshot of the desktop panel
  after switching to mobile layout. Their logs remain outside Git; none is called
  product RED. Focused A05 browser then passed with real HTTP/PG/server entropy.
- First full browser run: 54 pass, old P06 screenshot's `.combat-workspace` became
  ambiguous after a second workspace was added. Changed only that screenshot to
  the exact named manual-combat region, retaining every old behavior assertion,
  timeout and zero-retry setting. Final full rerun is recorded below.
- Final review added a real P06 cross-entry test. `a05-action-version-red.log`
  expected 409 but got 200 after an independent DM verdict changed the associated
  action without changing either card. CONFIRM/APPLY now also require its original
  linked-roll version, after receipt replay. The final nine HTTP tests cover both
  decision-before-confirm and decision-after-confirm; the full 153/185 run passes.
  Repeated the complete 55-browser/restart/independent restore/resume acceptance
  with the final implementation in a fresh acceptance database, preserving the
  earlier acceptance data and source fence.

## Acceptance boundaries

Preview creation saves immutable DM P05 rolls and a private P06 linked action,
full cards/catalog/review/calculations, reasons and hash. It changes no cards.
CONFIRM saves explicit final values; APPLY uses them only with both original card
versions and one transaction. CORRECT uses current versions, preserves later
independent edits and references immutable original events. PUBLISH is a separate
chosen-text summary; public DTOs contain no raw dice/AC/HP/resources/hash/private
reason/version. Current DM authorization precedes receipts; current visible
source/target filtering precedes public limits. Exact retries do not reroll or
reapply. Source/target changes require a new preview, never automatic rebase.

Real HTTP covers CSRF, anonymous/player/cross-campaign/revoked DM access, strict
missing/forged fields, hash and source/target versions, manual fallback, concurrent
same requests and competing DMs, later edits and old receipts, immediate/deferred
rollback of creation/application and all child rows, and full long-factor evidence.
Additional final audits cover real deterministic natural miss/critical storage,
independent critical dice and changes to either card after confirmation. Entropy
is injected only in the IT configuration; production still uses SecureRandom.
V24 migration compares every V23 table's original columns/rows and all 23 checksums,
then rejects cross-campaign character references. V1–V23 are unchanged.

UI tests cover locked exact retries retaining body/hash/versions, read failure
after ack without write retry, independently observed late 403, player-only public
reads and campaign/API/role scope isolation. The production-browser flow uses a
real committed APPLY whose response is deliberately dropped, proves exact retry
and single resource spend, reloads saved evidence, preserves later damage through
current-value correction and separately publishes a safe summary. Desktop and
390px screenshots are inspected; this is not physical-phone verification.

## Final local and exact-HEAD delivery evidence

- Full final `a05-runtime.ps1 -TestBackend`: **153 unit + 185 real IT**, zero
  failures/errors/skips, `a05-backend-full.log` (11:35:01). Includes all old 175 IT,
  nine A05 HTTP tests and one V23→V24 migration test. New source/target-after-confirm
  and deterministic natural miss/critical audits also passed. Production entropy
  remains unchanged. The prior full 153/182 and 153/184 passes is retained separately.
- `a05-frontend-final.log`: **291 frontend tests**, all pass; production build
  `a05-build-final.log` passed. Existing chunk-size warning remains unchanged.
  Contract checker passes **121 HTTP operations**, retaining all old 116.
- `a05-final-browser.log`: **55 production Chrome tests**, all pass, zero retries,
  real Java/PG/WebSocket and independently authenticated DM/player contexts.
  Final captures: `a05-final-browser.json`, `a05-final-controls.json`.
- On dedicated PG **55434/dnd_a05_final**, Java **18089** and production web **18090**,
  actually stopped Java/web/PG by verified process identity, restarted the same
  database/assets with the final jar, then logged in with fresh sessions.
  `a05-final-restart-verify.log` verifies both HTTP and browser-created A05 snapshots,
  original receipts and the existing browser-created A03e controls/recovery data.
- Created a complete P09 migration backup from this new acceptance instance,
  retaining its new source write fence (`a05-final-backup.log`). Stopped that source;
  restored into **separate PG 55435**, fresh `dnd_a05_restored_final`, new asset root and
  backend/web **18087/18088**. `a05-final-restore.log`: database, migrations, every row,
  references and immutable files match. `a05-final-restore-verify.log`: fresh sessions
  verify both A05 captures and original A03e controls. Then actually confirmed and
  applied a restored pending preview with its original hash/evidence/dice; exact
  retries changed HP once without adding a roll (`ATTACK RESUMED VERIFIED`).
- Acceptance processes were identity-checked and stopped; all synthetic databases,
  files and the new acceptance source fence are preserved. No previous source
  fence or restore destination was changed. Offline PG-stopped unit evidence is
  `a05-unit-pg-stopped-final.log`; no runtime tool/data/credential is committed.

Final results and independent restart/restore commands are recorded in
`docs/verification.md` and `docs/continuation.md`. Runtime binaries, credentials,
synthetic acceptance databases, logs and captures remain outside Git under
`D:/workspace/dnd/.tools/a05-*`. Final tests use PG 55434 and HTTP 18089/18090; restoration
uses separate PG/data/asset roots and ports. `dnd-roll-local`, its data, earlier
source fences and earlier restore targets are never operated.

CI retains all old checks and adds A05 HTTP/browser-created evidence verification
after actual container restart and separate-project/volume restore. The external
`a05-check-ci.mjs` requires the final counts, all original assertions, six new
ATTACK PREVIEWS VERIFIED lines and two original CONTROLS VERIFIED lines per browser
job, plus one ATTACK RESUMED VERIFIED after isolated restore. It independently verifies local/origin/PR SHA, open draft/base main, both
push/PR completed-success, and freshly downloads all six complete job logs.
The final index is `a05-ci-{HEAD}.json`, six `a05-job-*.log` and
`a05-head-ci-final.log`; absent/pending/failed evidence is not success, and entry
green cannot substitute for the new HEAD.

A03 portable combinations/attached-material publication, A06/M3, cloud/TLS,
physical cross-host and real-phone gates remain open. This bounded A05 adapter
does not claim full rule/effect automation or a release.
