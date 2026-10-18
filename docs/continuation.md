# 继续工作的入口

更新：2026-10-18（Asia/Shanghai）。最新分支、HEAD 和工作区以 `git status --short --branch` / `git log -5 --oneline` 为准。当前沿用 `feat/accounts-and-invitations` 和 [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，未合并、未发布。

## 本轮状态与下一步

- **P01 已完成，禁止重复开发。** 本轮起点 `d9a0393` 工作区干净、与 origin 一致；其 [push CI](https://github.com/xianchen233/dnd-roll/actions/runs/39127654306) 与 [PR CI](https://github.com/xianchen233/dnd-roll/actions/runs/39127657531) 均 completed/success，核对后才开工 P02a。
- **P02a 已完成并通过本地及远端完整 CI。** 代码提交 `8836abbcca9a28a2e130770cb9839e0136f51fcf` 的 [push CI](https://github.com/xianchen233/dnd-roll/actions/runs/39129943816) 与 [PR CI](https://github.com/xianchen233/dnd-roll/actions/runs/39129946834) 均 frontend/backend/browser success，含标准 Compose 部署和全部三条真实浏览器流程。 契约先写 `contracts/scenes.md`，实际 RED/GREEN 见两端 TDD.md 与 verification.md。37 后端 unit、28 真实 PG/HTTP/WS、59 前端测试、构建、19 HTTP 操作契约、3 条真实 Chrome 流程、V2→V3 旧数据比较及真实 Java 进程重启全部通过。
- DM 创建多个空场景（创建不切图）、明确切换全团当前场景；玩家不能列出未激活场景。旧场景位置/版本/控制权保存；历史激活回执只返回原结果，不再次切图。服务器仅接受当前场景的移动/移动重放，前端隔离旧场景在途响应和重试。
- V3 保留 V1/V2，回填当前场景指针并通过复合 FK 限制归属；P01/demo 在同一事务设置指针，demo 重启不重置选择。scene-command 回执独立命名空间，全局 actor→campaign 锁；与 P01 共用 campaignVersion。
- **P02b 已完成本地验收，远端 CI 待本轮推送后核实。** 本轮起点 `be96f2c` 工作区干净、与 origin 一致；其 push run39130339805 / PR run39130343257 三个 job 全 success。先写 `contracts/assets.md` 和实际 RED，再实现 DM 私有 PNG/JPEG 素材与地图引用/校准、V4 同团 FK、独立素材回执、Compose 私有卷。共享逻辑网格和玩家快照不含原图或地图素材字段，玩家图片揭露仍为 P02d。
- 本地证据：最终52 unit + 41真实PG/HTTP/WS全量、最终90前端测试与构建、24 HTTP契约、4条真实Chrome全部通过；V3旧库自然升级比较、Java与PostgreSQL实际停止/同库同素材根重启的SHA-256/引用/版本/回执检查通过。初次新增E2E精确label定位失败已修复，原三条流程未放宽；详情见 verification.md 和两端 TDD.md。
- 下一可执行步骤：先核验本轮提交的 push/PR CI（含Compose文件+数据库重启），全部通过后再进入 **P02c 棋子作者与共享控制**。先读契约，再RED验证名称/位置/隐藏/多控制者、撤回控制和移除成员后的旧回执不得恢复权限，保留移动版本/幂等。P02c/d/e、整个P02及后续包仍未完成。

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

本机无 Docker；8836abb 的两次远端 CI 已实际通过容器构建、Compose、代理及三条浏览器流程。本轮 P02b 新增容器卷/重启须另核新 SHA。公网 TLS、触屏、容量、完整素材备份和异机恢复未验收；P02b 本地已验收，棋子作者 P02c、遮盖 P02d、整体验收 P02e 及 P03–P10/A 工作包继续见 work-packages.md。约 564 kB 主 chunk 提示保留为后续优化，不放宽检查。

## GitHub 与 CI

- Git Credential Manager 已授权，CLI fetch/push 可用，不要再次要求用户登录。PR #1 仍 open draft、base main；继续更新这条 PR，不创建重复 P01 PR。
- P02b 续接时 GitHub 连接器已成功读取 PR #1；本机 Git 凭据的只读辅助脚本 `.tools/check-p02-ci.mjs --jobs` 可按当前 HEAD 核验 Actions，凭据仅在内存，禁止打印或落盘。
- CI 包含 frontend、真实 PG backend、标准 Compose browser；每次按目标 SHA 核验。旧 P01 全绿不代替新 P02a 检查。

## 本机辅助环境（均在应用仓库外，不提交）

- 外层工具目录 `D:/workspace/dnd/.tools`：JDK `java/jdk-25.0.4.1+1`、Maven `apache-maven-3.9.16/bin/mvn.cmd`、缓存 `m2`。
- Java 命令设置 JAVA_HOME；TEMP/TMP 及 `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp` 避免系统临时目录权限问题。Maven `-D` 参数用完整引号防止 PowerShell 拆分。
- PG data `pg-data`、原生 bin `postgres/bin`，回环 127.0.0.1:55432、huge_pages=off。`test-database.json` 与 `browser-database.json` 分别为 IT/browser 凭据，只注入环境，禁止打印。测试专用 Hikari pool 3，生产配置未变。
- `browser-credentials.json` 为测试账号；Chrome `C:/Program Files/Google/Chrome/Application/chrome.exe`。E2E 需四个 E2E_* 账号环境变量，无默认密码。
- `start-p01-browser.ps1` 可启动当前打包 jar/Vite；`smoke-p02a-restart.ps1` 为本轮实际进程重启验收，`P02Snapshot.java`/`p02a-before-v3.json`/`p02a-after-v3.json` 为迁移比较。均在 `.tools`，不提交数据/运行时。
- P02b 素材根 `.tools/browser-assets`，实际双进程重启脚本 `.tools/restart-p02b-services.ps1`；配合仓库 `scripts/check-asset-restart.mjs seed|verify <证据文件>`，本轮证据 `.tools/p02b-restart-evidence.json`。V3→V4快照为 `p02b-before-v4.json` / `p02b-after-v4.json`；Java单文件快照程序的JDBC driver取 `.tools/m2/org/postgresql/postgresql/**/postgresql-*.jar`，不要误用外层 `postgres.jar` 归档。PG通过 `pg_ctl start -D ... -l ... -o '-h 127.0.0.1 -p 55432 -c huge_pages=off' -w` 启动；直接管理员启动postgres会被拒绝。
- PID 文件只供定位，停止前必须核对实际进程 CommandLine，不能假设旧 PID 仍属于本任务。本轮 Java/Vite/PG 已按 CommandLine/数据目录核对后停止，测试数据保留；续接仍须核实实际进程和端口。
