# A03 actual TDD and acceptance

2026-10-22, Asia/Shanghai. A03 is in progress; this milestone implements A03a,
read-only three-way comparison. Selection/apply, live impact and manual operation
execution remain pending in [the ordered contract](../contracts/module-updates.md).

## Entry gate

Read AGENTS, continuation, verification, product design, implementation plan,
CONTEXT, contracts README, A01/A02 contracts and work packages before behavior edits.
Git clean on feat/accounts-and-invitations; fetch matched local/origin/PR HEAD
`d40f03f203efea554ceee7fa71cbd63e6664816b`. PR #1 open/draft/base main.
Independent GitHub API download of six complete logs verified push39669763972
and PR39669771205: each 116 unit + 133 real IT, 243 frontend/build, 99 contracts,
51 production browser, twelve restart groups and isolated restore/capacity.
The first two fresh list requests did not return the expected runs and failed
closed; a subsequent full re-fetch passed. Evidence outside repo:
`.tools/a03-entry-ci-verified.log` and refreshed job logs/JSON. No old SHA substituted.

## Actual cycles

1. `mvn -o -Dmaven.repo.local=... -Dtest=ModuleUpdateTest test`: initial fixture
   compilation error (compound var declaration) retained in a03-diff-red.log.
   Fixed fixture, reran: **5 tests, 4 failures**, zero errors/skips in
   a03-diff-red-behavior.log. Empty comparison failed fields/delete/add/scene cases.
   Implemented stable UUID indexing and recursive fields: **5 passed** in
   a03-diff-green.log. Identity reorder unchanged; delete/edit and competing adds
   conflict; identical edits do not conflict; reference lists are atomic.
2. Real PostgreSQL/HTTP `ModuleLifecycleIT#updatePreview*`: first command used the
   host default temp directory and failed before IT (a03-http-red.log); not RED.
   Set JAVA_TOOL_OPTIONS to the existing external writable temp directory, reran:
   **2 real HTTP tests failed, actual 404 vs expected 200/403**, zero errors/skips
   in a03-http-red-behavior.log. Implemented authorized versioned read-only route:
   **2 passed** in a03-http-green.log. Additional boundary test in
   a03-http-boundaries.log covers module/rules mismatch, exact missing dependency,
   CSRF and private headers. Preview creates no receipt and changes neither copy
   nor independently instantiated campaign/card/scene.
3. `npm test -- src/module-update.test.tsx`: stub component **5 failed** in
   a03-frontend-red.log. Implementation first **1 passed/4 failed**: literal JSON
   quotes and detail summary locator did not match. Display strings literally,
   expand the real details element in the test, retain original assertions.
   Full frontend **248 passed** in a03-frontend-full.log. Covers three values,
   late responses on scope/copy/version changes, current denial and read errors.
4. `node scripts/check-contracts.mjs`: missing read-only operation actual RED in
   a03-contract-red.log. Added route, exact request/result schemas and README.
   First schema preparation omitted path-level parameters; checker rejected it.
   Corrected structure: **100 operations**, all old paths/schemas structurally
   equal to entry HEAD. Focused checker is not a general OpenAPI validator.

## Local acceptance

- Full Maven offline integration verify: **121 unit + 135 real IT**, zero
  failures/errors/skips in a03-backend-full.log. New boundary IT is then run
  separately; do not claim that original full run contained it.
- Offline unit run with TEST_DATABASE_* removed: **121 passed** in
  a03-unit-no-db.log. PostgreSQL was running for simultaneous browser acceptance,
  but unit tests used no database configuration or database-dependent tests.
- Frontend 248; TypeScript/production Vite build passed (a03-build-final.log).
  Existing >500kB bundle warning remains.
- Real production Chrome new flow initially failed preparing damage at a nonexistent
  `/hp` endpoint (a03-browser-focus.log); corrected fixture to existing `/resources`.
  **1 passed, 2.6s** in a03-browser-focus-final.log, zero retry. DM starts independent
  party, damages HP, writes message, compares upstream conflict, opens all values,
  checks 1440/390px overflow and screenshots, then compares exact old card/message/
  copy/instance. Both screenshots inspected. Narrow browser layout is not a real
  phone validation. Full browser result is recorded in verification/continuation.

Full production Chrome: **52 passed (2.6m)**, original 51 retained, 60s/zero retry,
in a03-browser-full.log. Added identity/rules/missing-dependency/header/CSRF case:
**3 focused IT passed**, so 136 distinct IT have been run successfully across the
full 135 run and the final focused three. Stopped acceptance Java 42920 and preview
23732 only after matching actual command lines, then pg_ctl stopped the matching
pg-data cluster. No listeners remain at 8080/5173/55432. The source write fence and
restored target/assets remain. Unit verification with PG stopped and no database
environment is recorded separately in a03-unit-pg-stopped.log.

No migration or persistence write path added. No template update or manual trigger
has been applied. Final review clarified the empty-result message to mean equal
definition fields only; A03b still must compare manifest/images/dependencies and
rights before impact/apply. This wording correction is not a behavior RED.
A03b–e and A04–A06/M3 remain unfinished. Cloud/TLS/physical recovery
and real phones stay deferred; no merge or release.

2026-10-23: code 4f11cee exact-SHA push39808922365 and PR39808930410 both
completed/success. Re-downloaded all six full logs and strictly checked each group:
121 unit + 136 real IT; 248 frontend/build; 100 contracts; 52 browser; all original
restart/restore/campaign/capacity assertions passed. Final evidence a03-code-ci-final.log
and a03-ci-4f11ceeafa88cac270bc9827ca9276655116cc8f.json outside repo. A first check
rejected in_progress; final success was asserted only after completion. HEAD-filtered
run listing intermittently returned empty; direct known run IDs with SHA assertions
were used. Final documentation HEAD must independently pass the same checks.
