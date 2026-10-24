# TDD evidence

## A02 actual TDD (2026-10-22)

Executed HTTP/archive/strict JSON/private-header/rollback failures and fixes,
typed v2 boundaries, V18 preservation and full regression evidence are recorded
in [A02 evidence](../docs/a02-tdd.md). Tests use a real isolated PostgreSQL schema;
unit tests do not require a database or network. Source package/copy baselines and
independent campaign state/receipts are verified across restart and isolated restore.

## P09 actual TDD (2026-10-21–22)

Detailed executed RED/GREEN, environment failures, full checks and real isolated
database/assets recovery are recorded in [P09 evidence](../docs/p09-tdd.md).
All original tests remain. Code f6c9130 push39633129367/PR39633137675 six full logs
were independently verified:102unit+118real IT,225frontend/build,87contracts,
44browsers, ten actual restart groups and separate-project/fresh-volume recovery
with pending decisions and ten fresh-session checks pass. Documentation HEAD is
checked separately; physical cloud recovery remains deferred by the user.

## P07 最终完成

代码5d96a72的push39485707929/PR39485715062六份完整日志已重新读取并严格核对全通过：84unit+108IT、211前端/构建、75契约、两组39浏览器和九组实际容器重启。独立无库84unit及本地全量、九组实际Java/PG重启亦通过。原始失败与当时pending状态保留如下，最新入口见docs/continuation.md。完成文档HEAD另独立核验，不合并、不发布。


## P07 本地完整验收通过；精确代码 SHA 的双 CI 待核验（2026-10-21）

继续 feat/accounts-and-invitations / [草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，
不合并、不发布。开工HEAD 23dfa21的push39478445935/PR39478453446已独立核验六job/
完整日志通过；P01–P06完成且所有原回归保留。

契约 contracts/timeline.md。P07a文字/身份快照/不可变更正历史、b持久PNG/JPEG与
权限化引用、c共用RP/OOC/骰子/行动时间线、检索/分页/场次/完整Markdown及JSON导出、
d中文工作区/既有P03/P05/P06共用bot入口已实现。无独立骰子/数值/资源逻辑；不接QQ。

最终本地结果：p07-backend-final.log **84unit+108真实PG/HTTP/WS/迁移零失败/错误/
跳过**；p07-unit-no-db.log独立清除数据库环境后84unit；前端p07-frontend-full.log
211项，生产构建通过；p07-contract-final.log75操作，原65操作/全部旧schema逐项
结构比较相同。V12所有旧表原列/行/12checksum保持，当前到V15且同团引用FK通过。
p07-browser-full.log完整39生产Chrome流程通过1.8m，原35及60秒/条/零retry保留。
已复看深色timeline-dm.png。完整RED/GREEN/原始准备错误/失败修复见下文及两端TDD。

九组p07-final-{asset,token,map,p02,character,content,dice,combat,timeline}-restart-evidence.json
在实际Java+PG停止/同库同素材根启动后，新会话verify全PASS；p07-restart-{seed,verify}.log，
p07-process-before.json/p07-process-after.json显示不同Java/PG进程ID及真实CommandLine。
最终JAR .tools/p07-acceptance.jar；外置start-p07-backend.ps1/run-p07-restart.ps1可复用，
新seed须新前缀（wx禁止覆盖）。所有运行时/私有证据在D:/workspace/dnd/.tools，凭据仅内存。
最后按实际身份停止Java/production preview/PG，三端口无监听（p07-stopped-processes.json）。

**P07e/P07/M2尚未完成**：下一步提交/推送原分支、更新原草稿PR，核对精确代码SHA
的push/PR六job、标准Compose39流程及九组实际容器重启日志。全部通过后才关闭P07；
完成状态文档新HEAD也独立核验。P08–P10/A与容量/触屏/TLS/一致备份异机恢复继续保留。
以下为本包开发与此前交接历史，不作为最新运行状态。


## P07 追加验收与真实失败记录（2026-10-21）

- 首次完整后端 p07-backend-full-initial.log：81unit通过，106IT中1error；旧
  CombatMigrationIT合法最小骰子JSON没有at，V14回填的NOT NULL拒绝。保留旧断言，
  回填缺失时刻用epoch标识未知旧时间；所有原始数据不改写。仅对隔离dnd_roll_test库
  的未提交V14运行Flyway repair，对账日志p07-test-checksum-reconcile.log明确只修V14。
  V1–V12源码/checksum均不变。p07-backend-full-2.log随后81+107全部通过。
- TimelineMigrationIT比较V12所有旧表原列/行和12checksum，并验证源记录回填、
  场次捕获和跨团FK。V13消息/V14图片场次源捕获/V15引用约束均为本包新迁移。
- 三条TimelineImagesTest为文件提交不确定性/回滚清理/当前权限追加覆盖（直接GREEN，
  不伪造RED）。p07-unit-no-db.log明确清除TEST_DATABASE_*后84unit零失败/错误/跳过。
- 前端边界p07-frontend-boundaries-red.log实际1失败9通过：确认读撤销发言控制权时
  epoch变化导致busy未清理；修复后p07-frontend-full.log完整211项通过、构建通过。
  包含P01–P06原201，追加身份/精确File重试/编辑历史/指令/并发撤权/URL生命周期等。
- 首次浏览器p07-browser-first.log在Java未就绪时四项初始化502，非产品RED。
  就绪后p07-browser-ready.log 3过1败：新离线用例发送后立即读到空列表，补权威持久
  记录就绪等待。p07-browser-fixed.log仍3过1败：全页img定位匹配原Pixi地图的img角色；
  改为精确消息图片accessible name（包括断网前实际图片加载等待），不削弱权限断言。
  p07-browser-final.log四条通过12秒。上述原失败trace/日志均保留，不计组件RED。
- 人工查看完整timeline-dm.png发现浅色背景/全局浅字对比不足，调整本工作区深色配色
  并重新构建、复看截图。p07-browser-full.log最终完整39条通过1.8m，原35保留，
  60秒每条/零retry。覆盖RP图片/编辑历史/改名快照/引用/筛选/真实下载，P05/P06共用
  bot入口，私密引用/导出，以及实际offline、原生socket关闭与新CONNECTED隐藏恢复。
- p07-contract-final.log为75操作/严格字段/图片引用/无私密版本/保留策略检查；Node
  对HEAD原65操作和全部旧schema逐项deepEqual。检查器仍非通用OpenAPI validator。

九组p07-final重启seed均PASS，p07-restart-seed.log与对应JSON在仓库外.tools。
当前验收Java已按实际CommandLine核对停止，PG保留供最终后端84+108全量；最终日志
p07-backend-final.log仍在运行。随后实际停止/同库重启PG、启动最终JAR、九组新会话
verify，核对进程身份再停止。代码SHA双CI未推送核验前P07e/P07/M2均保持未完成。


## P07 2026-10-21

真实HTTP RED/GREEN及失败修复：见 docs/verification.md 当前P07段。
命令为外置 run-p04-tests.ps1 -Log <p07日志名> -Tests TimelineIT，内部真实执行
mvn -Dmaven.repo.local=D:/workspace/dnd/.tools/m2 -B -ntp -Pintegration verify -Dit.test=TimelineIT。
每轮均运行原81无数据库unit；消息RED 2失败→GREEN 2通过；图片/引用/查询RED
6中3失败→第一次GREEN 1失败（嵌套404回滚）→green-2六条全过。并发同键、不同版本
编辑及延迟commit失败回滚也已执行。所有日志外置；全量/迁移/重启/CI仍待验收。


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
# P03 当前证据（2026-10-20，未完成）

先固定contracts/characters.md，按本机SRD5.2整页核对公式。22:40:20
`mvn -Dtest=CharacterSheetTest test`（附外置m2参数）4项实际断言RED：计算缺值、
无效数据未抛领域错误；实现后65unit GREEN。日志外层.tools/p03-calculations-red.log。
HTTP创建预期200实际404 RED（22:44:11）；扩展7场景在相同前置RED；22:50:26
65unit+7真实PG/HTTP GREEN。构造器兼容的初次编译错误不算行为RED。
23:05:11待审核导入复制可绕过审核true实际false、NUL输入应400实际500两项RED；
修复保持待审标志、拒绝不可存储Unicode并按JSONB字节/分隔符限制大小，23:06:47
65unit+9IT GREEN。来源、技能、豁免、攻击/施法、回执/权限等见CharacterSheetTest/
CharactersIT；后加迁移、多棋子、回滚、WS首次通过是覆盖不伪造RED。
全量首次旧TokensIT因PG连接上限8个context错误，新增类DirtiesContext释放连接池后
重跑，未通过前不标完整验收。日志p03-backend-full-{initial,2}.log。
## P03 本地完整验收（2026-10-20）

最终独立清除TEST_DATABASE_*的65unit通过；完整`-Pintegration verify`为65unit+74IT
通过，零失败/错误/跳过（p03-backend-full-2.log）。新CharactersIT结束后关闭其context
释放连接池，原TokensIT八项完整通过，没有跳过；原FogMigrationIT明确target6，保留
其原V6断言，新增CharacterMigrationIT验证V6→V7，不减覆盖。浏览器旧库16表原列/
原行/六checksum自然升级一致；实际Java+PG停止并在同库/素材根重启，原四组加角色
第五组seed/verify全PASS。角色全字段、来源/覆盖/资源/历史/两场景引用/目标审核及
撤权后历史回执均保持；详细步骤、wrapper管道故障、日志与当前CI门槛见verification。
末尾创建/复制/导入回执“结果角色后被隐藏”的真实HTTP预期404实际200 RED
（p03-hidden-receipt-red.log）；同团锁内另复核receipt.result.id的当前可见性后，
完整65unit+75IT通过（p03-backend-final.log），0失败/错误/跳过。独立无数据库65unit
再通过。新JAR真实Chrome23条及重新seed后五组实际Java+PG重启全PASS，实际进程ID
前后不同，见verification最上段/p03-final-process-evidence.json。P03最终SHA双CI待核验。
P03最终代码f66c4e731f174c81be0ee7ab9724db30c0827446的push39452554961和
PR39452563178六job全success；完整日志确认65unit+75IT、177前端/构建、45契约、
标准Compose23浏览器与五组实际db/backend重启全PASS。至此P03完成；整个M2及P04–P10/A
未完成。上文pending/失败均为历史真实时点，由最终证据关闭，未伪造RED或跳过检查。
完成状态文档后续HEAD仍独立核对，见docs/verification.md与continuation.md。


## P04 实际 RED/GREEN（2026-10-21，Asia/Shanghai）

- 开工 d19d40a 本地/origin/PR 一致、clean；独立认证读取 push39454060276 /
  PR39454069467 的六个 job completed/success，PR #1 open/draft/main。
- 00:21:21 聚焦真实 HTTP：新检索预期200实际404，1 assertion RED；
  p04-http-red.log。00:22:44 扩展四场景4 assertion RED，均停在缺入口前置，
  不声称后续断言也已执行；p04-http-all-red.log。
- 00:25:20 同四场景 GREEN；65unit+4ContentIT，通过版本/许可搜索、独立准入、
  预览无变更、单次填入、精确重试、旧策略冲突、双请求并发与隐藏后旧回执拒绝。
- 第一轮完整69unit+80真实IT通过（含4纯填卡单测及V7→V8全表原列/行/checksum）。
  独立清TEST_DATABASE_*的69unit亦通过。证据 p04-backend-full.log / p04-unit-no-db.log。
- 00:38:07 开放署名随角色导出丢失的新增单测实际 RED（5项中1失败），
  p04-export-attribution-red.log；最小修复在Source.detail附原署名与修改说明。
- DM隐式权限不能把未批准条目显示成全团已准入：真实HTTP实际false/true RED，
  p04-admission-label-actual-red.log；目录改为报告团策略，执行端单独允许DM覆盖。
- 最后增加真实deferred receipt commit故障回滚、自定义发布撤权后回执拒绝、
  两个不同同版本填卡命令单胜；这些是新增覆盖，不虚构初次失败。

环境准备失败单列：首次pg_ctl漏huge_pages=off致1314，未进入HTTP；修正后另取得
实际404 RED。署名RED尚未修复时的首轮准入测试被unit挡住，不算准入RED；后一次
运行中JAR被Windows占用导致repackage失败，也不是行为RED。停止已核实进程后重跑。
最终计数/完整浏览器/实际重启与精确SHA双CI以docs/verification.md为准。

最终Source署名与准入修复后完整70unit+82IT通过；独立清库环境70unit通过。
V8迁移保留全部20张旧表原列/行和七checksum。最终日志p04-backend-final.log /
p04-unit-no-db-final.log；最后新增两项真实HTTP并发/提交故障亦通过。

P04代码a7ef388的push39460581794/PR39460588799六job全success，逐份日志确认
70unit+82IT、186前端/构建、50契约、两组标准Compose27浏览器及六组实际db/backend
重启全PASS；P04至此完成。完成状态文档后续HEAD另核对，不将旧SHA冒称新SHA结果。
# P05 — 2026-10-21 (Asia/Shanghai), in progress

- 01:11 `mvn -Dtest=DiceTest test` with external JDK25/Maven cache: 4 tests,
  3 assertion failures + empty-result access error against the compileable stub;
  `.tools/p05-dice-red.log`. Minimum bounded parser/entropy implementation then
  same command 4 passed, `.tools/p05-dice-green.log` (01:11:50).
- 01:13 `-Pintegration -Dit.test=DiceIT verify`: 74 units passed; three new real
  HTTP tests failed expected200/actual404 at missing `/rolls`. Later assertions
  were not yet executed. `.tools/p05-http-red.log`.
- First implementation rerun: all three behavior tests passed their assertions,
  but two fixture teardowns failed because the new durable roll FK prevented
  deleting their source cards. Fixed fixture cleanup to remove only its own rolls
  before cards; this run is NOT a full pass. `.tools/p05-http-green.log`.
- Initiative tests added before implementation: p05-initiative-red.log original
  3 HTTP passed/new 2 missing-route failures; then p05-initiative-green.log all 5
  passed. Additional deterministic entropy tests verify one group draw, stable
  candidate ties and complete validation before drawing; added as regressions.
- First complete p05-backend-full-2.log: 77 units + 88 real integration passed.
  Three additional HTTP regressions cover visible-before-limit pagination,
  control-only revocation, initiative commit rollback and DM downgrade; focused
  p05-backend-focused-2.log 77 units + 8 DiceIT passed.
- p05-backend-final.log: 77 units passed, 30/91 IT context startup errors from
  local PG `too many clients` while the acceptance Java and cached test pools ran
  together. Kept failure, stopped verified acceptance Java, no assertion/timeout
  weakening: p05-backend-delivered.log final **77 units + 91 real IT passed**, no
  failure/error/skip. Independent no-DB 77 units passed (p05-unit-no-db.log).
- DiceMigrationIT preserves every original column/row of all 23 V8 tables and
  all eight checksums, validates same-campaign roll source FK. Seven groups around
  actual packaged-Java/PostgreSQL stop/restart passed with new sessions;
  p05-restart-{seed,verify}.log and p05-process-evidence.json outside the repo.
- Delivered code f416e62 push39468342643/PR39468347675 all six jobs succeeded;
  each backend log 77 units + 91 real IT, zero failure/error/skip. Both standard
  Compose 31-browser suites and seven actual db/backend restart groups passed.
  P05 complete; completion-document HEAD remains independently checked.

## P06 manual combat (2026-10-21; acceptance in progress)

- Read AGENTS/continuation/verification/product/plan/CONTEXT/required contracts;
  independently fetched and verified entry d4e1068 against local/origin/PR #1,
  push39469353602 and PR39469359789 plus all six logs. P01–P05 remain complete.
- Contract first: contracts/combat.md, rules evidence docs/research/combat-5r.md.
  p06-actions-red.log: 3 HTTP assertions failed because /actions returned404;
  p06-actions-green.log: 3 passed. p06-turns-red.log: prior3 passed/new turn
  assertion failed on404; p06-turns-green.log: all4 passed.
- p06-private-version-red.log: assertion failed on player endVersion1→2 after
  private DM grant. Replace global counter with opaque random compare token;
  grants preserve token. p06-private-version-green.log:5 real HTTP passed.
- Added real commit-time deferred trigger rollback, same-request concurrent
  application, same-version correction race, multi-character atomic changes,
  current-version correction after later HP loss, source/target hiding, CSRF,
  foreign campaign/identity/revoked receipts and dual token/character movement
  controls. These are executed regression additions, not invented RED evidence.
- p06-private-headers-red.log: expected no-store, private vs Spring default.
  Extend earliest filter to combat/actions (including anonymous failure);
  p06-private-headers-green.log:8 real HTTP passed.
- Initial complete p06-backend-full.log:80 units +100 real IT, zero failure/error/
  skip. CombatMigrationIT preserves all28 V10 tables' old columns/rows and all10
  migration checksums; same-campaign source/target FK enforced. Final no-DB/full
  tests, actual eight-group Java/PG restart and exact-SHA dual CI still pending.
- Restart seed first failed on a synthetic template with no attacks; original
  seven seed groups passed. Add a synthetic saved attack, then new combat seed
  passed. No implementation RED claimed for fixture preparation.
- Final audit added entirely-hidden combat status/hints/exploration regression.
  Two custom focused launches omitted writable TEMP and failed Mockito agent
  initialization (4 errors each), not behavioral evidence. With existing runner,
  replayed the pre-fix projection/event/movement boundary: actual81 tests/1
  assertion failure (expected IDLE vs ACTIVE), p06-hidden-combat-assert-red.log;
  restore fixed projection, private-only hints and exploration behavior and rerun.
- Copying fixed source preserved its older timestamp; two subsequent runs used
  stale compiled class and still failed. Touch source forced actual recompilation;
  then valid immutable empty List.of exposed contains(null) NPE. Replace that
  check with anyMatch(Objects::isNull). p06-unit-delivered.log finally81 database-
  free units passed, zero failure/error/skip. Final complete result still pending.
- p06-backend-final-2.log: final81 units +100 real PostgreSQL/HTTP/WS/migration,
  zero failure/error/skip. New final packaged JAR prepared. Eight new delivered
  restart seeds passed. Actual stop/restart/verify and exact-SHA dual CI pending.
- Eight delivered seed→actual packaged-Java and PostgreSQL stop/start→fresh-session
  verify groups all passed. Complete action/roll/source/actor/change/correction,
  sheet and turn/history/receipt snapshots deepEqual. Current hiding denies old
  replay. Logs p06-delivered-restart-{seed,verify}.log and process/evidence JSON
  outside repo. Exact delivered-SHA push/PR CI still pending.
- Code d23c592 push39477351204/PR39477361622 all six jobs passed; both backend
  logs81 units+100 real IT, zero failure/error/skip; both standard Compose35
  browsers and all eight actual db/backend restart groups passed. Six complete
  logs independently fetched/checked. P06 complete; completion-document HEAD
  still requires its own exact-SHA checks.
# P08 actual TDD (2026-10-21)

- Entry fetch: local/origin/PR HEAD 1a4f0df identical, clean, open/draft/main.
  Fresh authenticated API and full six job logs verified push39486986615 and
  PR39486993922: 84unit+108IT/211frontend/75contracts/39browsers/9 restart groups.
  Evidence outside repo: p08-entry-ci-verified.log. Checks connector failed twice;
  existing Git credentials used only in memory for REST API (no source-control CI CLI).
- First p08-notes-red.log was environment failure (Tomcat tempDir denied), not RED.
  Configured external java.io.tmpdir; initial PG launch lacked port 55432, corrected
  by identity-checked stop/start. p08-notes-red-2.log: 2 assertion failures, /notes
  expected200 actual404. Minimal persistent notes/history/current auth/receipts and
  exports: p08-notes-green.log 2/2 pass.
- p08-images-red.log: 5 executed, 4pass/1assertion failure, missing note-attachments
  expected200 actual404. Add distinct immutable TEAM/DM images and same-visibility
  references: p08-images-green.log 5/5 pass. Tests include shared editors/history,
  current DM loss, image ownership, old-image retention, literal query, >50 complete
  export, strict JSON/CSRF, private cursors/foreign IDs, concurrent create/edit and
  deferred receipt-failure rollback. Original failures retained.
- Added database-free image commit-acknowledgment/rollback/unknown outcome tests and
  V15 all-table old-column/row/15-checksum migration checks. Full checks in progress;
  no claim of P08 completion until browser, real restart and delivered SHA dual CI.
- First complete run:87 units passed,114 IT with one migration-fixture failure:
  the test inserted a foreign campaign after its baseline, then compared that new
  row to the old snapshot. Remove that fixture before final comparison; all old
  columns/rows and15 checksums remain asserted. Focused migration then passes.
- p08-unit-no-db.log:87 units pass with all TEST_DATABASE_* and DATABASE_* removed.
  p08-backend-final.log:87 units+114 real PostgreSQL/HTTP/WS/migration tests,
  zero failures/errors/skips, BUILD SUCCESS. V15 original data preserved.
- p08-contract-red.log missing route RED; after documenting routes,
  p08-contract-final.log83 operations pass. Independent deepEqual confirms every
  original75 operation and old schema retained against entry HEAD1a4f0df.
- Ten seed groups then actual Java/PG stop, closed-port confirmation, new process
  identities, same data/assets and ten fresh-session verify groups all pass.
  Notes/history/visibility/imageSHA/exports and original receipts deepEqual;
  current removal/rejoin/private cursor/search/download boundaries hold. Evidence
  p08-restart-{seed,verify}.log/process JSON outside repo. Delivered SHA dual CI pending.

- Delivered code 1450e5db875c49e29d6446183d9f3851a32ff757 push39615748242/PR39615751296 six jobs all success.
  Six complete logs independently fetched and verified:87 units+114 real IT,
  221 frontend/build,83 contracts, both standard Compose43 browser suites and
  ten actual db/backend restart groups all pass. P08 complete; completion-document
  HEAD independently checked, P09/P10/A and M2 remain pending.
