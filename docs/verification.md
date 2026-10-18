# 阶段验收记录

日期：2026-10-18（Asia/Shanghai）。状态：M0/M1、P01 本地与远端 CI 通过；P02a 契约先行，实际结果另记于文末。

## 测试与构建

| 检查 | 实际命令 / 环境 | 结果 |
| --- | --- | --- |
| 契约 | `node scripts/check-contracts.mjs` | 7个HTTP操作、CSRF/会话、移动/幂等、快照与无棋子数据事件检查通过 |
| 后端单元 | Maven Wrapper3.9.16，Java25，`scripts/check.ps1 -Integration` | 16通过；无需Spring服务或数据库的unit |
| 后端集成 | 同一检查入口，真实PostgreSQL18.6 | 9通过、0跳过：HTTP5、原生WebSocket4；事务回滚、并发单胜和同请求并发幂等、撤权后重启保持均覆盖 |
| 前端 | `npm test` | 最终14通过，4个文件；API/状态/组件/实时替身检查 |
| 类型与产物 | `npm run build` | TypeScript及Vite通过；539kB主包提示保留为后续优化 |
| 真实浏览器 | `npm run test:e2e`，本机Chrome，两独立context，真实后端与PG | 1个多步骤场景通过；没有mock HTTP或STOMP |
| YAML静态检查 | 解析CI/Compose，核对3个job/3个服务、数据库无外部端口、PG18卷路径 | 通过；此项不能证明容器已运行 |
| 打包进程重启 | 移动并确认 → 停止Java → 同库重启 → 新会话重放原requestId | 原回执、位置、token/scene版本保持，不重复应用 |

统一检查使用官方only-script Maven Wrapper，Maven zip SHA-256已锁定。Windows本机临时目录权限问题通过将TEMP/TMP和java.io.tmpdir指向工作区外层 `.tools/tmp` 解决；环境故障没有记为TDD行为RED。

## 真实浏览器验收步骤

1. 使用环境注入的两个测试账号登录，DM与玩家各自独立Cookie会话。
2. DM看到Adventurer和Hidden enemy，玩家列表与HTTP快照仅含Adventurer。
3. 检查真实canvas完整置于面板内、宽高比例匹配24×16逻辑网格；客户端实时连接成功。
4. 玩家用坐标表单移动，确认服务器已保存；DM实际接到SCENE_CHANGED帧并自动更新位置。
5. 玩家点击缩放画布的(22,14)格，双方显示正确逻辑坐标；刷新仍读取相同位置，玩家仍无隐藏单位。
6. 保存DM/玩家截图，退出双方账号，认证接口变为401；没有非预期浏览器控制台/脚本异常。

截图由Playwright生成在 `frontend/test-results/tabletop-two-real-sessions-c8daf-ceive-realtime-invalidation/`，未纳入Git。复现命令及四个必填账号环境变量见前端README。

## TDD

领域与安全测试记录见 `../backend/TDD.md`；前端测试记录见 `../frontend/TDD.md`。只把实际预期断言失败计为 RED；安装、网络或编译器故障单独记录。

## 当前范围

M0/M1交付仓库框架与最小纵向切片；完整首版业务、模组与自动化见实施路线。demo 是显式启用的开发入口，账号密码从环境提供。正式的邀请与账号管理在P01推进，场景作者入口为P02。

## 尚未验证与后续工作

- 本机无Docker：没有在此机器执行镜像构建、Compose启动或容器重启。远端CI的browser job会通过标准Compose部署运行同一真实浏览器测试；必须查看实际CI结果才能标为通过。
- 外层TLS证书及反向代理链只完成配置与说明，仍需部署环境验证HTTPS和WSS。未部署到公网。
- 尚未进行完整测试团、手机触控、性能/容量测量、完整备份与异机恢复。这些属于后续业务/发布阶段，未因本轮验收而标为完成。
- 首版完整业务与模组/自动化仍待实现，按 `work-packages.md` 的P/A工作包继续。
- 原先CLI认证阻塞已在用户完成GCM授权后解除；基线已推送，远端CI实际结果见下文。新的P01提交仍须单独验收。

## 远端CI首次执行

2026-10-18，用户通过Desktop发布79cf832后，运行 [Build and test #1](https://github.com/xianchen233/dnd-roll/actions/runs/39124170015)：frontend与backend通过；browser在后端Docker构建时失败，还未执行浏览器步骤。Temurin构建镜像没有unzip，only-script Wrapper将ZIP替换为tar.gz，导致与ZIP的固定SHA-256不符。已为构建阶段补unzip，保留校验，需推送并等待新CI结果。此项为真实部署RED，不是业务单元测试失败。

[Build and test #2](https://github.com/xianchen233/dnd-roll/actions/runs/39124457926)（d903d40）：frontend/backend仍通过，前后端镜像均成功构建、PostgreSQL健康；紧接容器启动的curl遇到连接重置(56)，原有retry未涵盖此错误，browser尚未执行。就绪检查改为重试所有暂时错误、单次5秒、总重试120秒；没有跳过就绪检查或放宽业务测试断言。

[Build and test #3](https://github.com/xianchen233/dnd-roll/actions/runs/39124712714)（c57c35a）：**frontend、backend、browser全部success**。标准Docker Compose的前后端镜像构建、PostgreSQL启动、代理HTTP/WS及真实双浏览器流程均有远端运行证据。至此M0/M1的容器检查门槛完成；公网TLS和完整产品验收仍另行进行。用户确认GCM终端授权后CLI推送已成功。

## 当前P01

在上述CI通过后开工，契约位于contracts/accounts.md，分支feat/accounts-and-invitations。以下为P01本地实际结果，远端CI另行记录。

| 检查 | 实际结果 |
| --- | --- |
| 机器契约 | `node scripts/check-contracts.mjs`：16个HTTP操作通过；保留M1并增加账号、版本、秘密字段及fragment约束；聚焦检查器不冒充完整OpenAPI validator |
| Java unit | 20:10:41，全量Maven `-Pintegration verify`中30通过，0失败/错误/跳过，无数据库的单元层包含bootstrap、policy、application及socket registry |
| 真实PG/HTTP/WS | 同次verify共18通过，0失败/错误/跳过：AccountsIT9、FirstSliceIT5、RealtimeIT4；V1既有数据库成功升级V2 |
| 前端 | 20:06:21，`npm test`共31通过；`npm run build` TypeScript/Vite通过，约550kB主chunk提示仍保留 |
| 真实Chrome | 20:11，`npm run test:e2e`两条通过（13.6秒）：原M1双会话回归和新P01三会话账号/邀请/移除流程，无HTTP/STOMP mock |
| 打包进程重启 | 20:12，停止Java、同库重启、新会话认证后，创建战役回执不重复建团；邀请回执保持id/到期/version且token:null；被移除玩家仍403，旧邀请仍409；未使用邀请仍能接受，旧移除回执不删除新授予成员 |
| 静态审查 | 邀请明文不进入表、回执、列表或后端URL；管理授权先于回执；跨战役控制权删除带campaign scope；成员删除和回执故障均原子回滚；`git diff --check`通过 |

TDD实际发现并修复：邀请首次响应纳秒与PG微秒精度不一致；邀请消费后preview失败阻断恢复；Unicode密码字符计数不同；较早403被忽略、在途旧快照恢复撤权地图。证据见两端TDD.md。全量IT曾遇本机PG连接上限，测试专用Hikari池缩至3后18项全通过；环境失败未算业务RED。

真实P01浏览器步骤：DM创建空战役→分享`#invite=`链接→第三个独立context注册固定账号并自动正常登录→选择新战役→DM刷新成员并移除→真实socket关闭、客户端地图清空、后续场景403→账号仍保持有效且重新登录也不恢复成员。原M1截图已重新生成并人工查看，画布完整、管理区及权限单位列表正常。

进程重启为本机外置smoke脚本，非Maven自动测试；复现时用独立数据库执行上表命令顺序，不把在同一Spring context内调用bootstrap视为进程重启。P02场景与素材、P03角色卡以及M2其余工作包未完成。

P01代码提交 **3c12f8d4c0fe0687e3c3af09af40a87ab60065f4** 的[远端CI](https://github.com/xianchen233/dnd-roll/actions/runs/39126498540)：**frontend、backend、browser全部success**，含标准Compose构建/启动、真实PG与两条真实浏览器流程。[草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)已创建并附到聊天，未合并或发布。后续文档交接提交不改变上述代码测试证据；应继续核对PR当前提交状态。

### PR CI暴露的并发回归

dbe61cf的push run39126867019三个job通过，但[PR run39126870907](https://github.com/xianchen233/dnd-roll/actions/runs/39126870907)在M1浏览器移动确认处失败。真实STOMP刷新替代移动后的直接refresh时，位置已保存/同步，界面却停留在“场景刷新失败”。这是真实状态竞争，不能以另一个绿色run或自动retry关闭。

20:25:19先补deferred RED（两种响应顺序2失败），20:26:32补恢复边界再RED；最小修复记录POST成功时refresh序号，只让其后最新有效快照完成同步确认，旧/撤权响应继续隔离。20:26:50 `npm test`最终**36通过**，TypeScript/Vite构建通过，原M1/P01测试保留。

本机复验中，P01所有邀请/移除/403断言先通过，但退出后脚本立即导航导致重新登录表单超时；trace定位于8.34秒开始的账号fill。补上等待退出完成后的“进入冒险”按钮可见再导航。20:29再次 `npm run test:e2e`：**2通过（11.8秒）**，E2E业务断言未放宽、未配置retry。后端源码未改，不重复冒称执行新的Java检查。最新提交的远端结果以[PR Checks](https://github.com/xianchen233/dnd-roll/pull/1/checks)为准，交付必须检查当前SHA。

## P02a 开工核对

2026-10-18：HEAD `d9a03935920a1e75013e3de24a31370e33a87849`，Git clean，与 origin 一致。通过已认证 GitHub API 核对 PR #1 open/draft/base main，当前 SHA 的 [push run39127654306](https://github.com/xianchen233/dnd-roll/actions/runs/39127654306) 和 [pull_request run39127657531](https://github.com/xianchen233/dnd-roll/actions/runs/39127657531) 均 completed/success。P01 门槛满足后才开始 P02a 契约与 RED。P02a 检查尚未执行，不复用 P01 绿灯标记新功能通过。

### P02a 契约 TDD

先完成 `contracts/scenes.md` 后扩展检查器，实际 `node scripts/check-contracts.mjs` exit 1：documented/actual 操作比较缺少 GET scenes、POST scenes、POST activate（check-contracts.mjs:79）。随后补 OpenAPI，实际同命令 exit 0：19 个 HTTP 操作通过，保留全部 16 个旧操作。覆盖 DM-only、共享 campaignVersion、独立持久 scene-command 回执命名空间、授权先于回放、历史切换回执、严格字段及 Unicode 名称边界、无秘密元数据事件/玩家快照。此检查仍是聚焦契约检查器，不冒充通用 OpenAPI validator。

### P02a 本地最终验收

2026-10-18（Asia/Shanghai），所有旧 M1/P01 测试保留；未开始 P02b。

| 检查 | 实际结果 |
| --- | --- |
| 后端 | 20:55:38 `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify`：37 无数据库 unit + 28 真实 PG/HTTP/WS，0 失败/错误/跳过；AccountsIT9、FirstSliceIT6、RealtimeIT4、ScenesIT8、SceneMigrationIT1 |
| 前端 | 协调者20:56:49 `npm test`：6 文件53通过；`npm run build` TypeScript/Vite通过，主chunk约556kB提示仍保留 |
| 契约 | `node scripts/check-contracts.mjs`：19 HTTP 操作，保留全部旧16操作；命令/快照字段兼容、DM-only、共享版本/幂等与秘密边界检查通过 |
| 真实浏览器 | 20:56–57 `npm run test:e2e`：本机 Chrome、真实后端/PG/Vite，3通过（14.8秒），原 M1/P01 两流程原样保留，无 HTTP/STOMP mock、0 retry |
| V2→V3 实际升级 | 20:56 打包服务对旧 browser 库自然升级，Flyway验证1/2/3；升级前后5个campaign/scene、2个token、所有controls及旧receipt数量逐项相同；自动迁移IT另逐行验证原表及同团FK |
| 真实进程重启 | 20:58 外置 smoke-p02a-restart.ps1 实际停止Java、同库重启、新会话：当前scene、创建/激活回执、旧scene/token位置与版本、move回执保持；旧activate回放不再切图、inactive move回放404、demo不复位 |
| 视觉与静态检查 | 已人工查看真实DM/player PNG，画布完整、DM场景/成员管理正常、玩家无管理区和隐藏单位；`git diff --check`通过 |

第三条真实浏览器流程：玩家移动原demo棋子 → DM创建空备团场景而双端仍在旧场景 → 玩家不请求备团列表、直接列表/activate均403 → DM切图并观察真实私有STOMP frame（无场景名或秘密单位）→ 玩家新空场景/清选择 → DM切回 → 完整玩家快照与原保存位置、token/scene版本一致，DM仍可见隐藏敌人。finally只在需要时恢复旧current，避免破坏原M1复跑。截图在忽略目录 `frontend/test-results/scenes-DM-creates-a-prepar-f5e8f-o-the-preserved-moved-scene/`。

前端 TDD 包含初始11项预期失败，追加409刷新跨scene和POST成功但快照503的实际RED；采用独立scene generation，保留campaign epoch与任一当前epoch401/403全局清除，原P01实时刷新竞争修复保留。切换命令回执仅提示已确认，地图是否同步取决于真实新快照，不以历史回执宣称同步完成。

本地没有执行Docker/Compose或公网TLS；本轮新提交的远端CI尚待核实。迁移与重启检查只针对测试数据库，不等于P09完整备份/异机恢复。

### P02a 最终审查修复与复验

最终只读 QA 发现 `CampaignManagement.load()` 的 `Promise.all` 先网络/503、后另一请求401/403时，后到撤权未被观察。21:01:40 focused RED 实际4失败/14通过；每个请求独立观察授权失败再聚合，不等待另一挂起请求。新增6项deferred回归，晚到地图/备团/管理成功均不得恢复数据。21:02:04 全量前端最终 **59通过**，构建通过。21:03协调者再运行真实 Chrome `npm run test:e2e`，**3通过**，含原两流程与P02a，没有mock/retry或放宽断言。后端生产代码未改，沿用本轮37unit/28IT及真实重启证据，不声称重跑了新的Java检查。
