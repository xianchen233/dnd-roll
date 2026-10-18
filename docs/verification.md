# 阶段验收记录

日期：2026-10-18（Asia/Shanghai）。状态：M0/M1本地与远端CI通过；P01正在实现，结果单独列于文末。

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
