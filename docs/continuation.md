# 继续工作的入口

## P03 当前执行中（2026-10-20，本会话，优先于下方历史交接）

末尾补强已通过全部本地门槛：create/copy/import历史回执须另复核结果角色的当前
可见性，实际404/200 RED后修复；65unit+75IT完整GREEN，独立无数据库65unit、
177前端/构建、45契约、新JAR生产Chrome23条（1.0m）、重新seed后的五组实际Java+PG
重启均通过。最后源码日志为p03-backend-final、p03-unit-no-db-final、
p03-browser-final-after-receipts、p03-final-restart-{seed,verify}及p03-final-process-evidence。
服务再次核对身份后停止，三端口无监听，数据保留。仅剩统一提交推送并核验最终SHA
的push/PR CI，P03/M2尚未完成。下方74IT/第一次重启为本轮更早历史，不替代本段。

最新：本地完整65无数据库unit、65unit+74真实PG/HTTP/WS/迁移、177前端/构建、
45契约、最终生产Chrome23条（58.0秒）及五组实际Java+PG重启全部通过；16张旧表
全列/全行/六checksum自然升级一致。完整失败闭环、重启wrapper管道卡住后的分步
核验及证据见verification最上段。Java/production preview/PG再次核对身份后已停止，
55432/8080/5173无监听，数据及私有素材保留。下一步统一提交推送并独立核对最终SHA
的push/PR全CI，之后才关闭P03；**P03及整个M2当前仍未完成**。下文“正重跑/待本地
浏览器”等为本轮早期记录，已由本段及verification最新结果更新，不重复开发。

起点 b1f2c5f00d015e9efc50340e99db4584b3287056，干净，fetch后本地/origin/PR一致。
独立核对 push39439094794 / PR39439269275 六job completed/success，PR #1 open/draft。
已读AGENTS、continuation、verification、work-packages、产品/路线/CONTEXT及全部必读契约。
已使用工作区dnd-rule-lookup：A仅locate，B SRD5.2定位+整页回读，见
`docs/research/character-sheet-5r.md`，未读/分发商业规则正文。

P03契约为`contracts/characters.md`，按P03a身份字段历史→b来源计算→c资源状态→d传输
与棋子引用→e完整验收执行。已写V7、CharacterSheet/Service/Controller/JdbcStore、角色
授权撤销与实际操作者历史；六组前端字段、精确重试、来源历史、DM权限/计时/传输入口。
P01/P02保持完成，不重复；**P03及整个M2仍未完成**。

实际证据：4计算/校验断言RED；HTTP创建入口预期200实际404，扩展7场景同一前置RED；
65unit+7IT首轮GREEN。补充待审导入复制绕过（true实际false）和NUL输入400实际500
真实RED，修复后65unit+9IT通过。前端5项缺入口实际RED，171全量/构建通过；畸形
导入预览not.toThrow断言RED，修复后新6项GREEN。契约缺14操作RED→45操作GREEN。
新增迁移/多棋子/回滚/原生WS覆盖首次通过不冒充RED。全量首次74IT中旧TokensIT八项
因本机PG连接上限无法加载context；新类DirtiesContext释放池后正重跑全量。无凭据输出。

下一步：查看`.tools/p03-backend-full-2.log`，完成前端最终全量/构建及新6条实际浏览器
（frontend/e2e/characters.spec.ts），补真实V6→V7旧库逐列快照与第五组角色重启脚本，
原四组/17浏览器全保留运行。实际浏览器/重启/最终SHA双CI尚未执行，不得标通过。
当前PG已核对身份后启动127.0.0.1:55432（pg-data）；Java/preview尚未启动。
重启前仍重新核对端口和进程身份，不复用PID。本会话尚未提交；沿原分支/草稿PR统一
提交推送，不合并/发布。所有日志为外层`.tools/p03-*`，凭据仅内存。

更新：2026-10-20（Asia/Shanghai）。最新分支、HEAD 和工作区以 `git status --short --branch` / `git log -5 --oneline` 为准。当前沿用 `feat/accounts-and-invitations` 和 [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，未合并、未发布。

## 当前入口：P02e / P02 已完成，下一工作包 P03 尚未开工

2026-10-20 用户明确要求：**下一包交给下一会话，本会话不启动 P03**。
交接前独立核验 `863a68abb4addf0dcfc7664a0c04cc204c97c31f` 工作区干净、与 origin
一致；其 [push39437833963](https://github.com/xianchen233/dnd-roll/actions/runs/39437833963)
与 [PR39437844561](https://github.com/xianchen233/dnd-roll/actions/runs/39437844561) 均
completed/success，六个 job 全绿，PR 仍 open/draft。本次仅补交接文档，不改行为、
不重复本地测试；后续交接文档新 HEAD 的 Checks 留待下一会话开工独立核验，
不能把863a68a绿灯当作新SHA结果。55432/8080/5173再次确认无监听。

最终验收提交 `853944d36d59d31b833c509f30db84b356449a5a` 的
[push39436631381](https://github.com/xianchen233/dnd-roll/actions/runs/39436631381) 与
[PR39436640191](https://github.com/xianchen233/dnd-roll/actions/runs/39436640191) 均
completed/success，frontend/backend/browser 六 job 全绿，PR #1 open/draft、base main。
最新日志确认 **61unit + 61真实PG/HTTP/WS/迁移、166前端测试/构建、31契约、17真实
浏览器**，两组标准Compose均通过17条及四组实际db/backend重启seed/verify。
本地完整生产preview17条46.4秒、Java+PG实际重启四组也通过。原六个E2E文件/13条
及全部旧测试保留，生产源码/迁移/OpenAPI未改；新验收不伪造RED。具体证据、初次
测试准备失败和三项证据加强见verification及两端TDD。

P02e 契约 `contracts/p02-acceptance.md`；新增 `scripts/check-p02-restart.mjs` 和
`frontend/e2e/acceptance.spec.ts`。实际双场景fog/校准/单位独立、真实offline加原生
socket断开/新CONNECTED、撤权先提交再上线、fog提交后丢响应/原body重试/失败读恢复
均通过。已更新CHANGELOG Unreleased、部署及验收。**P01/P02a–e与整个P02完成**；
角色卡、整个M2、P04–P10/A、容量/触屏/TLS及完整备份/异机恢复仍未完成。

下一可执行步骤：先读AGENTS/本页/verification/work-packages及必读契约，独立核对
Git和最新HEAD Checks（含本次完成状态文档提交），再开始 **P03 完整半自动角色卡**。
先核对5R规则来源（使用工作区dnd-rule-lookup），固定一人多角、独立资源、基础计算
及来源、编辑历史、DM覆盖、状态到期提醒、复制与结构化导入导出的契约和最小切片；
按契约→实际行为RED→最小实现→完整验收，不把既有棋子配置当角色卡，不重复P01/P02。
继续现有分支/草稿PR #1，不合并/发布，不因P02完成标整个M2完成。

工具/日志/证据在外层 `.tools` 的 `p02e-*`；`run-p02e-restart.ps1` 注入已有测试
凭据调用原三组加新组合脚本，`start-p02e-backend.ps1`只启动当前打包Java，
`update-p02e-pr.mjs`沿用GCM维护原PR。凭据只在内存，不打印/不提交。验收
Java/production preview/PG 已逐一核实CommandLine/data后停止，55432/8080/5173
无监听，数据库/私有素材/截图及证据保留；不能复用旧PID。

## P02e 开工及首次验收历史

2026-10-20 从 `3c834b4` 干净工作区开工，fetch 后与 origin 一致；该 SHA 的
push39289292710 / PR39289302976 六个 job 全部 success，PR #1 仍 open/draft。
先写 `contracts/p02-acceptance.md`，只组合既有功能，保留全部旧测试。
本轮 61 无数据库 unit、166 前端测试/生产构建、31 契约以及最终后端
61 unit + 61 真实 PG/HTTP/WS/迁移已通过；新增真实 HTTP 跨场景组合首次即通过，
不伪造 RED。协调者完整真实 Chrome 生产 preview **17 passed（40.7s）**，
原六个 E2E 文件及13流程未改；真实双端离线/撤权/提交后响应丢失精确重试通过。
四组（原素材/棋子/地图 + 新两场景组合）实际 Java+PG 停止/同库同素材根重启，
seed/verify 全 PASS；详情、首两轮测试准备失败及边界见 verification。
首次验收提交 `5ac7491` 的 push39435183782 / PR39435191314 六 job 全 success，
标准Compose17流程及四组实际容器重启全 PASS。其后只读审查加强了三项证据：
移除实际204/名单无成员后才上线；捕获校准POST并读回offsetX=1；真实浏览器offline
加原生socket关闭，要求实际close及新socket的服务端CONNECTED。新四条18.4秒通过，
无生产行为变化；完整17条再次 **46.4秒通过**，加强提交的远端CI仍独立核实。
**P02e/P02仍未完成**。
下一步提交推送加强版本并核实该 SHA 的 push/PR 全量CI，全部通过后才更新完成状态。
外置 `.tools/run-p02e-restart.ps1` 注入已有测试凭据调用四组脚本，证据/日志以
`p02e-`前缀在同目录。新仓库脚本 `scripts/check-p02-restart.mjs`，验收契约
`contracts/p02-acceptance.md`；无新 API/迁移/生产行为。验收 Java/production preview/PG
已逐一核实实际 CommandLine/data 目录后停止，55432/8080/5173 无监听，数据与素材保留。

## P02d 已完成的历史交接

2026-10-19 本次从 `0bacdab` 开工，干净且与 origin 一致；其 push39143471892 /
PR39143474706 均 frontend/backend/browser success 后才实施 P02d。先写
`contracts/map-visibility.md`，实际缺路由/缓存头 RED，额外注入不安全全图输出使
真实 HTTP 秘密像素断言失败；恢复后实现服务器安全 PNG、手动 fog、V6 和原生 Pixi。
地图默认全遮盖；换图、校准或拆离原子清 fog，单位隐藏独立。旧公开像素无法撤回。
派生 PNG 重新编码、显式内存流、无磁盘/HTTP 派生缓存，URL 绑定当前场景和版本。

本地最终 **61 unit + 60 真实 PG/HTTP/WS/迁移、166 前端测试/构建、31 HTTP 契约、
13 真实 Chrome 生产构建（30.3s）**通过。原全部测试保留，旧 7 E2E 未改；14 张 V5 旧表
全部列/行与五个 checksum 自然升级一致。Java+PG 实际停止/同库同素材根启动，
新会话地图/素材/棋子三个 verify 全通过。详见 verification.md 与两端 TDD.md。

代码 `7f539cf7640f54f07087bffbd1e9bc8b012a79ed` 的
[push39287462278](https://github.com/xianchen233/dnd-roll/actions/runs/39287462278) 与
[PR39287472000](https://github.com/xianchen233/dnd-roll/actions/runs/39287472000) 均
completed/success，frontend/backend/browser 全绿。两组标准 Compose 分别13 passed
（push1.5m、PR1.1m）；素材/棋子/地图三个seed、实际db/backend重启和三个verify均PASS。
地图verify重新登录校验fog/version/回执及掩盖PNG哈希，旧URL、历史揭露、撤权和退出
不能取得秘密像素。本段是该代码SHA证据；接续时仍独立核对当前HEAD，含文档提交。

d593092 的刷新吞手势/草稿编辑问题已有实际RED/GREEN；bbdd54b组合流程超时已拆独立场景。88afda9两组
前后端通过、browser均12过1失败：push旧素材切图没有激活POST，PR平移/缩放断言
全部通过但最后截图耗尽总时限；两份trace已取得。场景preflight被背景读取静默吞掉
有4项实际RED，修复保留原版本、授权拒绝及409。静态Pixi改按需绘制，安全清图同步
重绘，有2项实际RED/GREEN；测距/视图用例仅打开所需玩家UI，其他仍双端。166项/
构建及完整13条本地复验通过；旧5个E2E文件不改，60秒/条、零retry。详见verification和
frontend/TDD。下一可执行步骤：核对Git与当前HEAD Checks后，按work-packages的P02e
制定独立整体验收组合，实际验证跨场景fog保存、校准重置、双端撤权/重连/失败恢复及
进程重启；保留全部既有测试、独立回执与单位隐藏边界，不重复开发P02a/b/c/d。
**P02e、整个 P02 及后续工作包仍未完成**；不合并、不发布。

本次外置工具：`P02dSnapshot.java`、`compare-p02d-snapshots.mjs`、
`p02d-before-v6.json` / `p02d-after-v6.json`、`run-p02d-browser.ps1`；
三个新重启证据为 `p02d-map-restart-evidence.json`、`p02d-asset-restart-evidence.json`、
`p02d-token-restart-evidence.json`。复用此前账号、DB、JDK、Maven、私有素材根；
仓库新增 `scripts/check-map-restart.mjs`。本次验收Java/Vite/PostgreSQL已按实际命令行
和data目录核对后停止，55432/8080/5173无监听，数据库/素材保留；不能复用旧PID。

## 原 P02c 交接基线（历史记录）

2026-10-19交接前再次核验：`a3e7f7ba9c2b66349e38df8f2c84ae087c9b60c0`工作区干净、与origin一致；[push39142300219](https://github.com/xianchen233/dnd-roll/actions/runs/39142300219)与[PR39142302856](https://github.com/xianchen233/dnd-roll/actions/runs/39142302856)均completed/success，frontend/backend/browser全绿。这是P02c最终交接HEAD的独立证据，不只引用代码5160c0f。以下本次仅补交接文档；其新HEAD Checks由下一会话开工时独立核对，不能将a3e7f7b绿灯当作新文档SHA结果。

下一会话按AGENTS先读本文、verification、work-packages，再读product-design、implementation-plan、CONTEXT和contracts/README及scenes/assets/tokens契约。不重复P01/P02a/b/c；保留56后端unit、50真实PG/HTTP/WS、112前端测试、27契约操作和7真实浏览器全部既有覆盖。

P02d先固定玩家安全图像/手动揭露的契约，再实际运行秘密像素泄漏的失败测试：玩家不能取得DM原图或遮盖区域原始像素，已公开内容再次遮盖不能声称从玩家缓存中撤回。覆盖直接URL、猜ID、跨团、撤权、历史回执、重连/切图及派生图缓存失效；客户端遮罩不能作为秘密边界，单位隐藏仍独立。随后实现服务端安全图像交付、DM手动揭露/遮盖及原生Pixi平移/缩放/网格校准/吸附/测距，按工作包先协调契约再分工，IT数据库串行使用。

继续维护CHANGELOG Unreleased和实际RED/GREEN/验收/交接；沿用当前分支与草稿PR #1，不合并、不发布。P02d完成后才进入P02e整体验收，不能提前标整个P02完成。本机Java/Vite/PG已停止，凭据与运行时均在外层.tools；重启前核对端口和进程身份，不依赖旧代理或本聊天历史。

## P02a/b/c 历史状态与当时下一步

- **P01 已完成，禁止重复开发。** P02a 开工起点 `d9a0393` 工作区干净、与 origin 一致；其 [push CI](https://github.com/xianchen233/dnd-roll/actions/runs/39127654306) 与 [PR CI](https://github.com/xianchen233/dnd-roll/actions/runs/39127657531) 均 completed/success，核对后才开工 P02a。
- **P02a 已完成并通过本地及远端完整 CI。** 代码提交 `8836abbcca9a28a2e130770cb9839e0136f51fcf` 的 [push CI](https://github.com/xianchen233/dnd-roll/actions/runs/39129943816) 与 [PR CI](https://github.com/xianchen233/dnd-roll/actions/runs/39129946834) 均 frontend/backend/browser success，含标准 Compose 部署和全部三条真实浏览器流程。 契约先写 `contracts/scenes.md`，实际 RED/GREEN 见两端 TDD.md 与 verification.md。37 后端 unit、28 真实 PG/HTTP/WS、59 前端测试、构建、19 HTTP 操作契约、3 条真实 Chrome 流程、V2→V3 旧数据比较及真实 Java 进程重启全部通过。
- DM 创建多个空场景（创建不切图）、明确切换全团当前场景；玩家不能列出未激活场景。旧场景位置/版本/控制权保存；历史激活回执只返回原结果，不再次切图。服务器仅接受当前场景的移动/移动重放，前端隔离旧场景在途响应和重试。
- V3 保留 V1/V2，回填当前场景指针并通过复合 FK 限制归属；P01/demo 在同一事务设置指针，demo 重启不重置选择。scene-command 回执独立命名空间，全局 actor→campaign 锁；与 P01 共用 campaignVersion。
- **P02b 已完成本地与远端完整 CI 验收。** 代码 `1bfac83d55eb3ccab18778a30e5f5ba54f4e4289` 的 [push run39135326549](https://github.com/xianchen233/dnd-roll/actions/runs/39135326549) 和 [PR run39135329930](https://github.com/xianchen233/dnd-roll/actions/runs/39135329930) 均 frontend/backend/browser success，含四条真实浏览器和标准Compose的数据库/后端重启。开工起点 `be96f2c` clean、与origin一致，旧SHA CI先全绿才开工。先写 `contracts/assets.md` 和实际RED，再实现DM私有PNG/JPEG素材与地图引用/校准、V4同团FK、独立素材回执、Compose私有卷。共享逻辑网格和玩家快照不含原图或地图素材字段，玩家图片揭露仍为P02d。
- 本地证据：最终52 unit + 41真实PG/HTTP/WS全量、最终90前端测试与构建、24 HTTP契约、4条真实Chrome全部通过；V3旧库自然升级比较、Java与PostgreSQL实际停止/同库同素材根重启的SHA-256/引用/版本/回执检查通过。初次新增E2E精确label定位失败已修复，原三条流程未放宽；详情见 verification.md 和两端 TDD.md。
- 最终审查修复：同步确认由POST确认后启动且最新有效的权威读完成，旧成功/失败不能覆盖；测试动作等实际可用，不放宽业务断言/timeout、不加retry。提交确认丢失时先取得新campaign锁等待旧事务结束，再核对asset/storageKey；确认未提交才清文件，已提交或无法核对则保留，通过原requestId回执恢复。模拟故障的范围及实际RED/GREEN见两端TDD和verification。
- **P02c 已完成本地与远端完整验收。** 代码`5160c0fdbb98a9bb189193f1ec8735d004ffb978`的[push39141337705](https://github.com/xianchen233/dnd-roll/actions/runs/39141337705)和[PR39141340870](https://github.com/xianchen233/dnd-roll/actions/runs/39141340870)均frontend/backend/browser success，含标准Compose七流程和素材/棋子实际数据库/后端重启。开工2967c37干净且与origin一致，其push/PR全部success后才开工。先写`contracts/tokens.md`和实际RED，再实现DM创建/完整编辑名称、位置、隐藏与多控制者，移动协议不变。56unit+50真实PG/HTTP/WS、112前端测试/构建、27HTTP契约、7真实Chrome通过；13表全部旧列/行及V1–V4 checksum自然升级一致，Java+PG本机真实重启亦通过。首次长流程CI超时和第二次checkbox交互竞争均已实际重现/修复，最终新SHA关闭，见verification及两端TDD。
- P02c独立token回执使用actor→campaign锁，DM/目标归属先于回放；历史配置只返回原确认、不恢复控制。V5回填同团键并限制成员FK；移除成员推进受影响token/scene版本，重加入不恢复控制。前端撤控/隐藏清除旧选择与在途移动，作者草稿保留原版本，任一并发拒绝清私有状态，最新post-ack读完成确认。
- 下一可执行步骤：先核对Git与PR #1当前HEAD及Checks（交接文档提交也独立核对），再进入 **P02d 地图交互及手动遮盖**，先契约及秘密像素不可经URL/缓存取得的实际失败测试。P02c不用重复开发；P02d/e、整个P02及后续包仍未完成。

## 用户授权与开发约定

- 仓库 `https://github.com/xianchen233/dnd-roll.git`，本地 `D:/workspace/dnd/dnd-roll`。已授权持续编码、按工作包分工、TDD、阶段验收和统一 Git 提交推送；不合并或发布草稿 PR。
- 开始读 AGENTS.md、本文、verification.md、work-packages.md，改行为前读 product-design.md、implementation-plan.md、CONTEXT.md、contracts/README.md 和对应行为契约。未完成工作不得静默删除。
- “契约 → 聚焦失败测试并实际运行 → 最小实现 → 集成验收”。只把实际断言失败算 RED；环境/编译失败单列。保留 M1/P01 全部功能与测试。
- 本轮按 work-packages 显式分工完成 backend/**、frontend/**、openapi/checker；协调者拥有其他契约、docs/deploy、实际验收和 Git。代理可用性不是接续前提，禁止并发改同一 IT 数据库。
- 服务器校验身份、成员、控制权、对象版本和幂等；秘密不进入玩家快照、错误或事件。所有新变更在根 CHANGELOG 的 Unreleased 同步记录，不伪造发布。
- 规则基线 5R；P02a 只有场景与探索坐标，无 D&D 机制实现。本轮无需规则检索；开始规则相关行为时使用 dnd-rule-lookup。

## 真实验收与边界

详情见 `docs/verification.md`：M1/P01 两条真实浏览器原样保留，新增 P02a 双端移动、创建不激活、玩家 403、真实 STOMP 切图、返回完整快照/版本保持。已人工查看 DM/player 截图，画布完整、DM 管理可用、玩家无隐藏单位或管理区。

V2 浏览器库升级前后逐项比较 5 个战役/场景、2 个棋子、controls 与旧回执数量一致。独立自动迁移 IT 另外逐行比较全部原表内容并验证跨团指针 FK。真实 Java 进程停止/同库重启后，当前指针、创建/切换/移动回执、旧场景位置与版本保持；demo 不重置选择，历史切换重放不重新切图。

本机无Docker；P02c的5160c0f两次远端CI已实际通过容器构建、私有素材卷、代理、七条浏览器及数据库/后端重启，后续交接提交仍独立核对Checks。公网TLS、触屏、容量、完整素材备份和异机恢复未验收；遮盖P02d、整体验收P02e及P03–P10/A继续见work-packages.md。约572kB主chunk提示保留为后续优化，不放宽检查。

## GitHub 与 CI

- Git Credential Manager 已授权，CLI fetch/push 可用，不要再次要求用户登录。PR #1 仍 open draft、base main；继续更新这条 PR，不创建重复 P01 PR。
- P02b续接时GitHub连接器可读PR，但更新PR返回integration 403，部分日志读取出现transport error；已沿用现有GCM凭据更新同一PR，无需用户再登录。只读脚本 `.tools/check-p02-ci.mjs --jobs` 按当前HEAD核验Actions，`.tools/read-p02-job.mjs <jobId>`只输出筛选证据，`.tools/update-p02b-pr.mjs`读取外置body更新PR；凭据仅在内存，禁止打印或落盘。
- CI 包含 frontend、真实 PG backend、标准 Compose browser；每次按目标 SHA 核验。旧 P01 全绿不代替新 P02a 检查。

## 本机辅助环境（均在应用仓库外，不提交）

- 外层工具目录 `D:/workspace/dnd/.tools`：JDK `java/jdk-25.0.4.1+1`、Maven `apache-maven-3.9.16/bin/mvn.cmd`、缓存 `m2`。
- Java 命令设置 JAVA_HOME；TEMP/TMP 及 `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp` 避免系统临时目录权限问题。Maven `-D` 参数用完整引号防止 PowerShell 拆分。
- PG data `pg-data`、原生 bin `postgres/bin`，回环 127.0.0.1:55432、huge_pages=off。`test-database.json` 与 `browser-database.json` 分别为 IT/browser 凭据，只注入环境，禁止打印。测试专用 Hikari pool 3，生产配置未变。
- `browser-credentials.json` 为测试账号；Chrome `C:/Program Files/Google/Chrome/Application/chrome.exe`。E2E 需四个 E2E_* 账号环境变量，无默认密码。
- `start-p01-browser.ps1` 可启动当前打包 jar/Vite；`smoke-p02a-restart.ps1` 为本轮实际进程重启验收，`P02Snapshot.java`/`p02a-before-v3.json`/`p02a-after-v3.json` 为迁移比较。均在 `.tools`，不提交数据/运行时。
- P02b 素材根 `.tools/browser-assets`，实际双进程重启脚本 `.tools/restart-p02b-services.ps1`；配合仓库 `scripts/check-asset-restart.mjs seed|verify <证据文件>`，本轮证据 `.tools/p02b-restart-evidence.json`。V3→V4快照为 `p02b-before-v4.json` / `p02b-after-v4.json`；Java单文件快照程序的JDBC driver取 `.tools/m2/org/postgresql/postgresql/**/postgresql-*.jar`，不要误用外层 `postgres.jar` 归档。PG通过 `pg_ctl start -D ... -l ... -o '-h 127.0.0.1 -p 55432 -c huge_pages=off' -w` 启动；直接管理员启动postgres会被拒绝。
- P02c沿用同一浏览器库/素材根和双进程重启脚本，新增仓库`check-token-restart.mjs`；两份新证据为`.tools/p02c-token-restart-evidence.json`与`p02c-asset-restart-evidence.json`。`P02cSnapshot.java`、`compare-p02c-snapshots.mjs`和`p02c-before-v5.json`/`p02c-after-v5.json`为13表全部原列/行及迁移checksum比较。外置`update-p02c-pr.mjs`更新同一草稿，body在`p02c-pr-body.md`；凭据仍只在内存。全量日志为`p02c-full-verify.log`、`p02c-frontend-final.log`、`p02c-browser.log`，无运行时或数据纳入Git。
- PID 文件只供定位，停止前必须核对实际进程 CommandLine，不能假设旧 PID 仍属于本任务。本轮 Java/Vite/PG 已按 CommandLine/数据目录核对后停止，测试数据保留；续接仍须核实实际进程和端口。
