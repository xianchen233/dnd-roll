# TDD evidence

2026-10-18, Java 25.0.4.1 and Maven 3.9.16 on Windows.

RED: `mvn -Dmaven.repo.local=D:/workspace/dnd/.tools/m2 -f backend/pom.xml test` executed before the movement/application/STOMP policy implementations. Result: **12 tests, 5 failures, 7 errors, 0 skipped; BUILD FAILURE** at 18:34 +08:00. Expected causes: deliberate `UnsupportedOperationException("TDD red")` movement/application stubs and an allow-all STOMP policy. Dependencies and tests compiled successfully; this was a behavioral failure.

GREEN: same command with `-B -ntp`, at 18:36 +08:00: **12 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. All 13 production sources compiled. Unit tests use no Spring context, database, Docker or network.

Real PostgreSQL integration results will be recorded after execution.

Strict JSON follow-up RED at 18:39 +08:00: **13 tests, 1 failure, 0 errors**. The new fractional-coordinate test reproduced Jackson silently accepting `x:1.5` as an integer. Disabled `ACCEPT_FLOAT_AS_INT` for both coordinates and versions; HTTP negative cases also cover fractions and absent fields.

Strict JSON GREEN at 18:39 +08:00: **13 tests passed**. Session revocation RED at 18:40 +08:00: **15 tests, 1 failure**, proving that invalidating a session did not yet close its sockets. Added an HTTP session lifecycle listener and transport registry; the real STOMP suite also tests logout closing an established socket.

Session revocation GREEN at 18:41 +08:00: **15 tests passed**. The coordinator then ran the scalar coercion follow-up RED at 18:59:04 +08:00: **16 tests, 1 failure, 0 errors, 0 skipped** (`stringCoordinatesAndVersionsAreRejected`, string numbers were accepted). Disabled `MapperFeature.ALLOW_COERCION_OF_SCALARS`; final GREEN is pending coordinator execution.

The coordinator ran real PostgreSQL integration RED at 19:03:09 +08:00: **9 tests, 2 failures, 0 errors, 0 skipped**. The movement test reproduced a committed move leaving scene version at 0; the restart test reproduced demo initialization restoring revoked control. All four actual WebSocket tests passed. An earlier run failed on access to the host's default Java temporary directory; that environment failure is not counted as behavioral RED. The coordinator set `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp` for this Windows environment and reran successfully to obtain the behavioral failures.

Minimal fixes: increment the scene snapshot version in the same transaction as the move and receipt; only seed scene data/memberships/controllers when the campaign insertion creates a new campaign.

Final coordinator GREEN, 19:06 +08:00: the official Maven 3.9.16 only-script Wrapper (distribution SHA-256 checked) executed `scripts/check.ps1 -Integration`: **16 unit tests and 9 real PostgreSQL 18.6 integration tests passed, 0 failures/errors/skipped; BUILD SUCCESS**, producing the executable Spring Boot jar. HTTP checks include strict JSON, membership/control, hidden filtering, conflicting commands, identical concurrent retries and transaction rollback; four real WebSocket tests cover connection/CSRF, private invalidations, forbidden sends/subscriptions, logout and anonymous rejection. This resolves the scalar coercion, scene version and restart authorization regressions above.

At 19:12 +08:00 the coordinator also ran an actual packaged-server process restart: commit a move, stop/restart Java against the same PostgreSQL database, authenticate again, retry the original stale-version request. The original receipt, coordinates and token/scene versions survived; replay did not apply the move twice. This was a local smoke check, separate from the committed Maven test suite. See `../docs/verification.md` for reproducible steps and remaining Docker verification.

## P01 accounts and invitations

2026-10-18 (Asia/Shanghai), Java 25.0.4.1, Maven 3.9.16. All commands used the existing local Maven cache and a writable external Java temporary directory; credentials were read from the external test-database configuration into environment variables and never printed.

- **19:58:24 RED**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dtest=AccountPolicyTest' test`: 3 tests, 3 assertion failures against empty policy methods (strict credentials, DM/version enforcement, expiry and single-use policy). A preceding incorrect working-directory path produced no test and is not counted as behavioral RED.
- **20:01:40 policy GREEN / socket RED**: the 3 policy tests passed; `AccountSocketsTest` failed with expected 2 closed account sockets, actual 0 against its empty implementation. Implemented account-to-session tracking and closure of all sessions after membership removal commits.
- **20:03:12 GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' test`: 28 tests passed, 0 failures/errors/skips. Includes 8 database-free application tests for one-time secrets, receipts, authorization before replay, role preservation, consumed-invite rejection after removal, rejoining after removal, expiry/revocation and registration conflict.
- **20:05:54 GREEN**: same unit command: 30 tests passed after 2 bootstrap configuration/insert-only tests. No Spring context, PostgreSQL, Docker or network required by these unit tests.
- **20:07:17 real PostgreSQL RED**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dit.test=AccountsIT' -Pintegration verify`: 30 unit tests passed; 7 P01 integration tests ran, 6 passed and 1 failed. Flyway upgraded the pre-existing V1 database to V2 successfully. Failure reproduced first invitation response preserving nanoseconds while PostgreSQL receipts preserve microseconds, so retry metadata differed. Invitation creation now normalizes its injectable Clock instant to database microsecond precision before computing the seven-day expiry.

All P01 integration fixtures use dedicated accounts and campaigns, with cleanup; they do not remove or restore permissions in the M1 demo fixtures. Additional real HTTP/PostgreSQL/WebSocket cases cover registration rollback on claim failure, removal rollback on receipt failure, concurrent creation/secret issuance/single-use claims, strict JSON versions, cross-campaign grants, socket closure and replay after revocation. Final full-suite result follows after execution.

**20:08:35 full-suite environment failure**: 30 unit tests, 9 P01 integration tests and 5 M1 HTTP tests passed; the final 4 M1 WebSocket tests could not create their Spring context because three cached default Hikari pools exhausted this host's PostgreSQL connection limit. This is not counted as behavioral RED. Added `src/test/resources/application.properties` to limit each test pool to 3 connections; production pool settings are unchanged.

**20:10:41 final GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify` against real PostgreSQL 18.6: **30 unit tests and 18 integration tests passed, 0 failures/errors/skipped; BUILD SUCCESS**, executable jar repackaged. Integration split: 9 P01 HTTP/PostgreSQL tests (one includes an actual native WebSocket), 5 original M1 HTTP tests and 4 original M1 native WebSocket tests. This verifies the microsecond receipt fix and preserves the complete prior slice. True packaged-process restart and browser acceptance are coordinated separately by the parent task; this suite does not claim to have run either.

## P02a multiple scenes and the campaign's current scene

2026-10-18 (Asia/Shanghai), same Java/Maven/cache/temp environment as P01. Read `contracts/scenes.md` before changing behavior. Credentials were injected from the external test database configuration without printing them. The backend agent held exclusive use of the IT database throughout these runs; the coordinator used a separate browser database.

- **20:49:51 HTTP RED**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dit.test=ScenesIT' -Pintegration verify`: all 30 existing unit tests passed; the new real HTTP create test failed with expected 200 / actual 404 on `/api/campaigns/{id}/scenes`. The original V2 database was unchanged, and the application and test compiled and started successfully.
- **20:50:13 unit RED**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dtest=SceneServiceTest' test`: 5 tests, 1 assertion failure. Once a token became inactive, a changed-fingerprint retry disclosed `IDEMPOTENCY_CONFLICT` instead of `NOT_FOUND`. Current-scene token visibility/control is now checked before reading or exposing movement receipt state. The existing different-token receipt test still verifies 409, using another visible controlled token; arbitrary inactive/foreign IDs now obey the new 404 contract.
- **20:51:54 intermediate GREEN**: full unit command passed 31 tests, including the inactive receipt fix. No Spring context, database, Docker or network is required by unit tests.
- **20:54:27 focused GREEN**: focused ScenesIT verify passed **37 unit + 8 real PostgreSQL/HTTP integration tests**, 0 failures/errors/skips. Flyway naturally upgraded the existing IT database from V2 to V3, validating all released migration checksums. Six additional pure in-memory management tests cover trim/code-point limits, authorization before receipt replay, shared command fingerprint namespace, stale/unknown targets, exact event counts and historical activation receipts that do not reactivate scenes.
- **20:55:38 full GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify`: **37 unit tests + 28 real PostgreSQL/HTTP/WebSocket integration tests passed**, 0 failures/errors/skips; executable Spring Boot jar repackaged. Integration split: AccountsIT 9, FirstSliceIT 6, RealtimeIT 4, ScenesIT 8, SceneMigrationIT 1. All previous M1/P01 tests remain.

ScenesIT covers DM creation without activation, shared activation/return to moved scenes, player/nonmember/anonymous rejection, CSRF and strict JSON versions, Unicode name limits, shared P01 campaignVersion, separate account/scene receipt namespaces, stale versions, historical retry behavior after role/membership revocation, inactive movement and inactive receipt replay (including changed fingerprints), identical concurrent retries, two-DM single-version races, cross-campaign actor/request races, and create/activate rollback on forced receipt insertion failure. Only the moved scene's version advances. Production scene commands use actor-then-campaign database locks so a globally keyed scene receipt cannot produce a unique-key server error when the same actor races across campaigns.

SceneMigrationIT creates a generated isolated real PostgreSQL schema, applies the unchanged V1/V2 migrations, seeds an old map, hidden token, controller, movement receipt and account receipt, then applies V3. Full old rows and versions compare equal afterward; current pointer backfill, multiple scenes and rejection of a pointer to another campaign are asserted. Test schema cleanup is scoped to its generated identifier. A separate FirstSliceIT verifies rerunning demo bootstrap preserves a DM-selected scene. Pointer initialization is transactional for both P01 and demo creation; nullable staging permits initial campaign insertion while the composite FK enforces campaign ownership for every nonnull pointer.

Actual browser switching, packaged-process restart and remote CI are coordinated separately and are not claimed by this Maven result. Images, map configuration, token authoring and fog remain P02b–d.

Coordinator acceptance: at 20:56–57 all 3 real Chrome E2E flows passed (14.8 s), retaining M1/P01 and adding shared scene switching. At 20:58 an actual packaged-Java stop/restart preserved current selection, scene/movement receipts and the inactive scene state; historical activation replay did not switch again. The existing browser V2 database also upgraded to V3 with a before/after comparison of all scenes/tokens/controls and prior receipt counts. See docs/verification.md; remote CI is tracked separately.

## P02b durable private assets and map calibration

2026-10-18 (Asia/Shanghai). Read `contracts/assets.md` before implementation. Same Java 25.0.4.1/Maven 3.9.16/cache/external temp setup as P02a. Unit commands do not use Spring, a database, Docker or network; real integration credentials are injected from the external test-database JSON without printing them. Only the backend agent uses the IT database during these runs.

- **21:22:03 image RED**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dtest=AssetImageTest' test -B -ntp`: **3 tests, 3 assertion failures, 0 errors/skips** against a compiling validator stub. Reproduced metadata width 1 instead of decoded width 3 and missing rejection of empty/malformed/MIME-mismatched/truncated/oversized images. Added PNG container CRC/end checks, JPEG start/end checks, decoded format/dimensions/pixel limits before full decode, rejection of decoder warnings and SHA-256 of original bytes.
- **21:24:12 real HTTP RED / image GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dit.test=AssetsIT#privateAssetListingStartsEmptyWithManagementVersion' -Pintegration verify -B -ntp`: **40 unit tests passed; 1 real PostgreSQL/HTTP test failed**, expected 200 / actual 404 on GET assets. Existing V3 schema started successfully and remained unchanged. This is a behavior assertion failure, not a compile/environment failure.
- **21:26:01 unit GREEN**: full `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' test -B -ntp`: **40 tests passed**, 0 failures/errors/skips after adding controller, V4 migration, JDBC asset store and private atomic filesystem adapter.
- **21:29:47 intermediate integration**: focused `'-Dit.test=AssetsIT' -Pintegration verify`: **46 unit tests passed; 10 real HTTP/PG tests ran, 9 passed and 1 failed**. V3 naturally upgraded to V4 with unchanged old migration checksums. The failing assertion incorrectly expected DataAccessException for a deliberately deferred constraint-trigger failure; PostgreSQL commit failures are TransactionSystemException. Corrected the test to assert the precise exception class for each immediate/deferred stage, retaining all database/reference/version/file rollback assertions. This test expectation correction is not claimed as a production-behavior RED.

Final complete-suite result is recorded below after execution. Actual browser and packaged Java + PostgreSQL restart acceptance are coordinated separately and are not represented by a Spring context or adapter re-creation.

**21:31:33 full GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp` against real PostgreSQL 18.6: **49 unit + 40 integration tests passed, 0 failures/errors/skips; BUILD SUCCESS**, executable jar repackaged. Integration split: original AccountsIT 9, FirstSliceIT 6, RealtimeIT 4, ScenesIT 8, SceneMigrationIT 1 plus AssetsIT 11 and AssetMigrationIT 1. Old migration test now explicitly targets its intended V3 endpoint, preserving all old assertions; the separate V3→V4 test checks full old rows and checksums plus both same-campaign map foreign keys.

P02b unit coverage includes decoded PNG/JPEG metadata/SHA, malformed/truncated/MIME/byte/dimension/pixel rejection, authorization before receipts, complete command fingerprints, historical map replay, actor→campaign order, one event per successful command, failed command file cleanup without deleting old files, opaque atomic storage and generic path-safe failures. Real HTTP/PG coverage includes all routes, CSRF/current role/membership/logout, scoped 404s, no-store on success/401/403/404/503, original download bytes and no ETag/304, strict JSON/map fields, upload limits, unchanged shared snapshots, inactive calibration without group switching, independent namespaces, upload/scene shared campaign versions, map/move shared scene versions, same-request concurrent retries, two-DM version races and global actor/request cross-campaign races. Immediate receipt-insert and deferred commit trigger failures assert rollback of metadata/references/receipts/versions and cleanup of only new files; old committed asset bytes remain intact.

**21:34:43 final focused GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dit.test=AssetsIT' -Pintegration verify -B -ntp`: **49 unit + 12 real PostgreSQL/HTTP tests passed**, 0 failures/errors/skips, executable jar repackaged. Added one HTTP test exercising extra and duplicate `command`/`file` multipart parts; all return 400 without metadata/version/receipt mutation. Production sources did not change after the 49 + 40 full-suite result; the full suite now contains 41 integration tests, with the added test executed in this focused run. IT database released before the coordinator's shared PostgreSQL process-restart acceptance.

Coordinator acceptance (2026-10-18 Asia/Shanghai): packaged browser database naturally upgraded V3→V4 with before/after comparison of old campaign/scene/token/controller data and receipt counts. Actual Java and PostgreSQL stop/restart at 21:36–37, same private asset root, then new sessions verified exact SHA-256, map references/calibration, versions and original upload/map receipts; historical replay after detach did not reattach. Player/foreign/logout downloads were denied. At 21:39–40 all four real Chrome flows passed (16.8s). See docs/verification.md; remote CI is checked separately for the new SHA.

### Lost commit acknowledgment follow-up

- **22:13:05 pure service RED**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dtest=AssetServiceTest' test -B -ntp`: **9 tests, 3 assertion failures, 0 errors/skips**. The in-memory transactional Store commits work then throws a simulated lost commit acknowledgment; the previous cleanup removed the committed file. Two additional cases reproduced deleting the file when the recovery database could not determine whether the transaction committed or rolled back. These tests compiled and ran without Spring, PostgreSQL, Docker or network.
- **22:14:09 focused GREEN**: same command: **9 tests passed**, 0 failures/errors/skips. Cleanup now runs only after the outer transaction ends and uses a fresh campaign row lock, so it waits for a still-finishing PostgreSQL commit before reading the candidate asset ID and independent storage key. Only a confirmed absent/different reference permits deleting the new file. A committed matching reference protects its file; a failed recovery lookup/lock/commit preserves the opaque file and the original exception (recovery failure is suppressed). The simulated committed case retries the identical request, obtains the original receipt without another effect/event and downloads the original bytes. The unknown rolled-back case retains no metadata/receipt and remains unavailable through every public lookup.

Full real PostgreSQL regression result is recorded after execution below; immediate/deferred rollback tests retain all earlier file/reference/version assertions.

**22:15:51 full follow-up GREEN**: `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp` against real PostgreSQL 18.6: **52 unit + 41 integration tests passed**, 0 failures/errors/skips; BUILD SUCCESS and executable jar repackaged. All previous suites remain, including immediate receipt-insert failure, deferred commit failure, file-publication failure, old-file preservation, namespace/authorization/replay/concurrency and V3→V4 migration checks. PG was started with the existing external data directory for this exclusive run and stopped afterward. Simulated lost acknowledgment coverage is explicitly a pure transactional Store test; this run does not claim an actual network-disconnected commit experiment.

Final remote acceptance: code 1bfac83 passes all frontend/backend/browser jobs in push run39135326549 and PR run39135329930. Backend logs confirm 52 unit + 41 real PostgreSQL/HTTP/WebSocket tests, zero failures/errors/skips. Standard Compose runs all four real browser flows and seed → db/backend restart → verify with exact SHA, map/version/receipt preservation and authorization checks. See docs/verification.md; follow-up documentation commits require their own current-HEAD Checks.

## P02c token authoring and shared control — 2026-10-18

Final remote acceptance: code `5160c0fdbb98a9bb189193f1ec8735d004ffb978` passes frontend/backend/browser in push39141337705 and PR39141340870. Logs confirm 56 unit + 50 real PostgreSQL/HTTP/WS tests, no failures/errors/skips; standard Compose also passes seven browser cases and token/asset seed → actual database/backend container restart → new-session verify. Actual local migration/process-restart evidence remains in docs/verification.md. P02c is complete; P02d/e and the full P02 remain open. Later handoff-only commits require their own current Checks.

- Contract first: read the fixed `contracts/tokens.md`, preserving shared scene/movement/event shapes. Added four focused database-free application tests before behavior implementation.
- Actual RED: 22:44:36, `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' '-Dtest=TokenServiceTest' test -B -ntp` ended with **4 tests, 4 assertion failures**, each expecting a non-null authoring response while the service skeleton returned null. Initial command used a wrong working-directory-relative path and therefore found no tests; that setup failure is not counted as RED.
- Implementation: token creation/configuration has separate globally actor-keyed receipts, actor→campaign locks, canonical controllers, current DM/ownership before replay, fresh membership validation, token/scene versions shared with movement and atomic JDBC writes. Existing movement already checked current membership/current scene/visibility/control before receipt replay; retain it and add HTTP regressions.
- V5 retains released V1–V4 files, backfills token/controller campaign keys and enforces composite scene/token/member ownership. Member removal advances each affected scene once and every affected token, then deletes grants; receipt replay changes nothing. Existing migration tests now explicitly target their original V3/V4 boundary, retaining assertions.
- Actual GREEN: 22:46:28 and 22:50:11 full `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' test -B -ntp`: **56 unit tests passed**, no failures/errors/skips; all prior 52 preserved. Second run also compiled the new `TokensIT` and `TokenMigrationIT` classes. Unit tests require no PostgreSQL or network.
- Added eight real PG/HTTP/WS tests for authoring, multi-control, hide/reveal, current authorization before retries, member removal/rejoin and stale forms, strict fields, receipt namespace/fingerprints, inactive preparation, concurrent same-request/version/cross-campaign commands, transaction rollback and secret-free after-commit invalidations. Added isolated V4→V5 migration/data/checksum/FK test. Execution is serialized by the coordinator; these IT results remain pending until actually run. Real process restart/browser acceptance belongs to coordinator verification, not this unit result.
- Coordinator serialized real-PG execution: **22:52:16** focused `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration '-Dit.test=TokensIT,TokenMigrationIT' verify -B -ntp` passed **56 unit + 9 new IT**, zero failures/errors/skips. **22:54:39** full `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp` passed **56 unit + 50 real PG/HTTP/WS IT**, zero failures/errors/skips, with all prior 41 IT retained. Final full run includes Java-canonical controller sorting independent of PostgreSQL collation, 80-Unicode-code-point legacy username acceptance, null/empty controllers rejection and preservation of another controller during member removal. Target reports confirm `TokensIT` 8/8 and `TokenMigrationIT` 1/1. Database was never used concurrently by agents.
- Remaining coordinator acceptance at this point: existing V4 browser database natural upgrade comparison, five real-browser flows, actual Java/PostgreSQL restart and current-SHA remote CI. These are not implied by the successful Maven checks; final actual results belong in root verification/continuation records.

## P02d (2026-10-19): safe map delivery and manual fog

Read AGENTS, continuation/verification/work-packages and required design/contracts,
including `contracts/map-visibility.md`, before changing production behavior.
22:17:58 baseline `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' test -B -ntp`:
56 unit tests pass without database environment variables.
22:20:21 `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2'
'-Dit.test=MapVisibilityIT' -Pintegration verify -B -ntp` against dedicated real
PostgreSQL: 56 unit pass; 2 new HTTP assertion failures, no compilation/context
errors. Player-safe map-view expected 200 but returned 404; anonymous private
response expected `no-store, private` but returned Spring's default cache header.
The missing route prevents the subsequent PNG pixel assertions from running;
this RED is explicitly a missing-delivery/header RED, not evidence of an existing
original-image leak. The test also specifies decoded black/revealed/secret pixels,
original/guessed URLs, stale URL invalidation, conditional and Range behavior.

22:22:47 first GREEN: original 56 unit + 2 real HTTP PNG tests pass.
22:26:02 expanded GREEN: 60 unit + 9 real PG/HTTP/WS/migration tests pass.
22:27:01 deliberately injected a temporary unsafe mutation bypassing `revealed`
in `SafeMapImage`, then executed only MovementTest +
`MapVisibilityIT#playerGetsOnlyFreshBlackPixelsAndSourceRoutesCannotBypassFog`.
The real HTTP PNG assertion fails: covered pixel expected opaque black
`0xff000000` (-16777216), got source color `0xff12ab34` (-15553740).
This is a fault-injection test proving the pixel assertion detects leakage, not
a claim the baseline had a working unsafe image route. A PowerShell `finally`
restored the safe implementation immediately after the Maven failure. Its shell
exit is 0 because of the restoring finally; Maven clearly reports BUILD FAILURE,
1 assertion failure, 0 errors/skips. Final safe rebuild remains required.

QA found Java ImageIO's default global disk caching. 22:27:56 and 22:28:34
`mvn ... '-Dtest=SafeMapImageTest#renderingNeverUsesImageIoDiskCacheEvenWhenGlobalCachingIsEnabled' test`
both produce 1 actual assertion failure (no errors/skips): under global cache=true
and an invalidated cache directory, render attempts FileCacheImageInputStream and
throws `Not a directory!`. The test decoder was made explicitly memory-only before
the second RED, ensuring it cannot cause the failing production render assertion.
Implementation now uses explicit MemoryCacheImageInputStream and
MemoryCacheImageOutputStream; it never changes ImageIO's process-wide cache flag.
22:29:50 the first full verify stopped in the new renderer unit suite (61 tests,
2 failures/2 errors). ImageIO.read(ImageInputStream) automatically closes its
input; enclosing try-with-resources then attempted a second close. Replaced that
convenience read with an explicit disposed ImageReader while retaining the owned
memory stream, and corrected the independent test decoder similarly. IT did not
run in that failed attempt. The final full verify below supersedes this attempt.

**22:31:55 final full GREEN:**
`mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp`
against the dedicated real PostgreSQL 18.6 database: **61 unit + 60 integration
tests passed, zero failures/errors/skips; BUILD SUCCESS**, executable safe jar
rebuilt after the fault injection was removed. The original 56 unit + 50 IT
remain intact. New coverage is 5 pure renderer tests, 9 MapVisibilityIT cases and
one isolated V5-to-V6 FogMigrationIT. Full external log is
`D:/workspace/dnd/.tools/p02d-backend-full-verify.log` (not committed).

Renderer assertions decode every covered/public boundary pixel, check negative
offsets and unequal grid scaling, opaque-black alpha flattening, text metadata
removal even when fully revealed, and rendering with global caching enabled but
an unavailable cache directory. Real HTTP verifies no source metadata/URLs in
shared map-view, original and guessed source denial, membership/role revocation,
cross-campaign/unknown/inactive scene isolation, current scene-version binding,
no-store/nosniff failures, conditional/Range full 200, stale image 409, and absent
map image 404. Fog cases cover strict JSON/canonical cells/current DM before
replay, historic receipts without restoration, independent namespaces, single
effect concurrent retries, same-version and global actor-key races, inactive
preparation and atomic reset on all successful map changes. Immediate and
deferred receipt failures roll back fog/map/version/receipts. A latch-controlled
source read proves rendering holds the campaign lock while a concurrent cover
waits; afterward the old URL conflicts and new fog applies. Real native STOMP
contains only the unchanged two-field invalidation, and replay emits no event.
Hidden tokens and controllers are unchanged by reveal/cover/map reset.

FogMigrationIT compares every original column/row of all 14 V5 tables and the
V1–V5 migration checksums, starts existing maps fully covered, and rejects a
cross-campaign revealed-cell FK. Earlier TokenMigrationIT explicitly targets V5
as the other historical migration tests target their original boundary; all its
old assertions remain. Browser/process restart and current-SHA remote CI belong
to the coordinator's separate evidence. P02e and the whole P02 remain incomplete.

**22:33:24 database-free regression GREEN:** removed all TEST_DATABASE_* values
from the process environment and ran `mvn ... test -B -ntp`: **61 unit tests pass**,
zero failures/errors/skips, no Spring context/database/network required. External
log: `D:/workspace/dnd/.tools/p02d-backend-unit-no-db.log`.

## P02e integrated acceptance (2026-10-20)

The independent composition contract is `contracts/p02-acceptance.md`. No new
production behavior or migration was required. A tenth `MapVisibilityIT` case
combines two scenes with different fog, actual decoded public/secret PNG pixels,
hidden/public tokens, a confirmed player move, scene return, rejected and accepted
calibration, and historical map/fog/activation/token receipts. It verifies that
rejected calibration leaves fog intact, accepted calibration clears only its own
scene's fog, historical confirmations never restore earlier state, control
revocation rejects an old successful movement receipt, and membership removal
denies known original/derived image URLs to both the existing session and a fresh
login. All nine existing map cases are unchanged.

**21:48:37 focused acceptance PASS:**
`mvn -f backend/pom.xml '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2'
-Pintegration
'-Dit.test=MapVisibilityIT#integratedSceneReturnCalibrationAndRevocationPreserveIndependentFogAndHistoricalReceipts'
verify -B -ntp` ran **61 unit tests + 1 focused real PostgreSQL/HTTP test**, with
zero failures/errors/skips and BUILD SUCCESS (37.600s). The new composition
passed on its first execution against the existing behavior; this is added
acceptance coverage, not a claimed behavioral RED. External log:
`D:/workspace/dnd/.tools/p02e-backend-focused.log`.

**21:50:18 full regression PASS:**
`mvn -f backend/pom.xml '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2'
-Pintegration verify -B -ntp` completed in 1m15s: **61 unit + 61 real
PostgreSQL/HTTP/WebSocket/migration tests**, zero failures/errors/skips,
BUILD SUCCESS. This preserves the previous 61 units and 60 integration tests,
adds the single composition above and rebuilds the packaged application.
External log: `D:/workspace/dnd/.tools/p02e-backend-full.log`.
Browser, actual process restart and exact-SHA CI remain separate coordinator
gates; these backend results alone do not mark P02e/P02 complete.

The coordinator subsequently completed the full production-browser and four real
Java/PostgreSQL restart gates. Final acceptance SHA `853944d` passed both push
39436631381 and PR 39436640191, including 61 units + 61 ITs, 17 standard-Compose
browser flows and four actual container-restart verification groups. P02e/P02 is
complete; see docs/verification.md. Subsequent documentation HEADs still require
independent CI verification. PR #1 remains a draft, unmerged and unreleased.
