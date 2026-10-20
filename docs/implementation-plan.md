# 实施路线与分工

## P07 已完成；下一包 P08 尚未开工（2026-10-21）

继续 feat/accounts-and-invitations / [草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，
不合并、不发布。最新HEAD与工作区以Git实际状态为准；完成状态文档HEAD亦独立核验。

P07代码 **5d96a72ee441741f702fb7cfd1641016a6cd8d8f** 的
[push39485707929](https://github.com/xianchen233/dnd-roll/actions/runs/39485707929) 与
[PR39485715062](https://github.com/xianchen233/dnd-roll/actions/runs/39485715062)
均completed/success，frontend/backend/browser六job全绿，PR open/draft/main。
逐份重新获取完整日志并严格核对：两组均84unit+108真实IT、211前端/生产构建、75契约，
标准Compose完整39浏览器以及九组seed→实际db/backend容器重启→九组新会话verify全PASS。
外置p07-job-118845439558/118845439977/118845440055及118845464786/118845465315/
118845465645，汇总p07-ci-5d96a72ee441741f702fb7cfd1641016a6cd8d8f.json和
p07-code-ci-verified.log。首次严格检查在PR尚in_progress时退出，保留p07-code-ci-pending.log；
待完成后重新获取全部六份并通过，不将该未完成检查或旧SHA结果当作新检查成功。

本地独立无库84unit、最终84+108完整后端、211前端/构建、75契约、39生产Chrome、
V12全部旧表原列/行/12checksum，以及九组实际Java+PG停止/同库同素材根启动/新会话
verify亦全部通过。来源/身份/编辑/图片SHA/场次/引用/搜索/完整导出/原回执和当前隐藏
过滤均已验证。实际RED、准备错误和修复保留在下文与两端TDD，不伪造先失败证据。

**P01–P07均完成；整个M2、P08–P10/A仍未完成。** 契约contracts/timeline.md。
机器人沿用P03/P05/P06逻辑；时间线不引入新骰子/数值/资源执行；不接QQ或导入QQ历史。
记录/图片/编辑长期保留；Markdown/JSON记录导出不是P09数据库+素材一致部署备份。

下一可执行步骤：按AGENTS读交接、verification、work-packages、产品/路线/CONTEXT与
必读契约，独立核对最新HEAD（含完成文档）的push/PR Checks；再进入 **P08 团队笔记
及DM资料**，先固定共享/私密、当前权限下查询/搜索/附件/导出契约，实际RED→最小实现→
GREEN→完整验收。P01–P07不重复开发，保留P09/P10/A、容量/触屏/TLS及异机恢复门槛。

日志/运行时/私有证据均在仓库外D:/workspace/dnd/.tools，凭据仅内存。最终JAR
p07-acceptance.jar；start-p07-backend.ps1/run-p07-restart.ps1可复用，新seed须新前缀
（wx拒覆盖）。p07-final九组JSON、p07-restart-{seed,verify}.log与进程前后身份保留。
最后按CommandLine/data核对停止Java/production preview/PG，55432/8080/5173无监听，
p07-stopped-processes.json留证。不要复用旧PID；PG启动huge_pages=off且分步操作。
以下为本包开发/验收与此前交接历史，不作为当前下一步。


## 历史：P07 本地完整验收通过；代码双 CI 当时待核验（2026-10-21）

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


## 历史：P07 开发/验收过程（2026-10-21）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。P01–P06保持完成。
开工独立 fetch：本地/origin/PR HEAD 23dfa2169baf65282d5687720993ff5340811b7a，clean；
认证 API 核对 push39478445935 / PR39478453446 六 job success。重新获取六份完整日志：
81unit+100IT、201前端/构建、65契约、两组35浏览器、八组实际容器重启均PASS；
PR open/draft/main。连接器临时传输失败后使用现有 Git credential 内存认证，不打印秘密。

P07契约 contracts/timeline.md，已固定a消息身份/历史、b图片/引用、c统一检索/场次/
导出、d中文入口与共用指令、e完整验收的有序切片。当前已实现后端和初版前端，
**P07e/P07/M2尚未完成，不以聚焦GREEN关闭。**

实际TDD：p07-messages-red.log 两条真实HTTP缺/messages 404失败→green两条通过。
p07-images-query-red.log 六条中新图片/私密引用/场次三条失败→green-2六条通过。
第一轮green仍有引用暗骰触发UnexpectedRollbackException：嵌套服务的404标记事务
rollback-only；改为当前可见性预检后才投影，第二轮完整六条通过（保留前次失败）。
p07-contract-red.log 缺10路由RED→75操作GREEN；仍须追加新schema/边界检查。
p07-frontend-red.log 七项缺入口失败，另有未消费deferred拒绝准备错误；实现后6过1败，
最后一条mock让新旧scope共用同一旧响应，修正mock单次响应后green-2七条通过。
构建最初误在仓库根运行（无package.json），第二轮发现测试helper使用不支持exact类型；
已修正，待最终构建。上述准备/类型问题不计行为RED。

所有日志/运行时/私有证据在仓库外 D:/workspace/dnd/.tools，凭据只注入内存。
PG已启动（huge_pages=off）；最新本地进程/端口应现场核对，不能沿用旧PID。
当前全量后端 p07-backend-full-initial.log 正在运行。下一步：完成前端入口/更广边界与
契约检查、旧库迁移保留测试、全量前后端/生产浏览器、九组实际Java/PG重启，再提交/
推送同分支并独立核验精确代码SHA的push/PR CI；完成后才更新关闭状态。
P08–P10/A及容量/触屏/TLS/一致备份异机恢复门槛全部保留。以下为此前历史状态。


状态：2026-10-18，用户已授权进入代码实现，并指定 `https://github.com/xianchen233/dnd-roll.git`。按 TDD 执行用户所说的“ttd”。产品目标见 [设计方案](product-design.md)，术语见 [领域词汇](../CONTEXT.md)。

续接入口：[当前交接状态](continuation.md) → [实际验收结果](verification.md) → [完整工作包与依赖](work-packages.md)。用户特别要求：任务超出聊天上下文后，必须能凭仓库文件继续推进。

## 本轮当前状态（2026-10-21）

**P01–P06均已完成；下一包P07未开工，M2及P07–P10/A仍未完成。**
P06代码d23c592的push39477351204/PR39477361622六job/完整日志独立核验全绿：
81unit+100真实IT、201前端/构建、65契约、标准Compose35浏览器及八组真实容器重启；
本地无库81、完整同等验收、28旧表全部原列/行/10checksum及八组Java/PG重启均通过。
契约contracts/combat.md、依据docs/research/combat-5r.md，实际失败/修复见verification。
完成文档新HEAD另核对，不合并、不发布；下面保留P06开发时的历史状态。
P06契约`contracts/combat.md`，来源`docs/research/combat-5r.md`。最终81unit+100IT、
独立无库81、201前端/构建、65契约、完整35真实浏览器、28旧表/10checksum及八组
实际Java/PG重启已通过；精确代码SHA push及PR CI仍待完成，不提前关闭。实际RED、
准备失败和后续步骤见verification。
契约 `contracts/dice-initiative.md`，5R依据 `docs/research/dice-initiative-5r.md`。
P05代码f416e62的push39468342643/PR39468347675六job全成功：77unit+91真实IT、
195前端/构建、58契约、标准Compose31浏览器及七组实际重启通过；V8→V10保留23
旧表原列/行/八checksum。实际RED/GREEN/失败修复、边界和下一步见continuation/
verification。只提供投骰与先攻准备；回合/伤害仍P06，不合并、不发布。

以下为P04完成时的历史证据：
P04契约 `contracts/content.md`；a7ef388的push39460581794/PR39460588799六job
全部成功：70unit+82真实IT、186前端/构建、50契约、标准Compose27浏览器及六组实际
重启通过。本地无库单测、V8旧数据迁移与Java/PG重启也通过；实际RED/GREEN、限制与
证据见verification/两端TDD。原分支/草稿PR #1不合并、不发布。交接文档新HEAD另核对。

## 工作阶段

| 阶段 | 工作 | 完成条件 |
| --- | --- | --- |
| M0 整体框架 | 独立 Git 仓库；frontend/backend/contracts/deploy/docs/CI；固定依赖与环境示例；模块和接口边界 | 前端可构建，Java 单元测试无需数据库运行；配置中无密码；CI 定义完整 |
| M1 最小可用纵向切片 | 最小失败单元测试 → 实现移动规则；会话登录、CSRF、战役成员、可控棋子、隐藏过滤、版本冲突、请求幂等、PostgreSQL、实时失效通知与重连快照 | 单元/组件/真实 PostgreSQL 集成测试通过；双会话可读取移动后的共同状态，玩家收不到隐藏单位；可用 Compose 启动 |
| M2 首版业务 | 邀请和成员管理、场景与素材编辑、角色卡、规则检索及填卡、服务器掷骰、5R 先攻与手动战斗、RP/附件/日志、更正、备份恢复 | 设计方案中的第一阶段验收场景逐项可重现，一次完整测试团与迁移恢复演练通过 |
| M3 模组与自动化 | 模组包、独立定制与战役实例、选择性更新、手动触发操作、限定支持矩阵内的自动结算、移动预算 | 模板与进行中状态隔离；自动化可解释、可确认、可更正，重试不重复消耗 |
| M4 发布验收 | 权限回归、真实浏览器/双客户端测试、断网与重启、备份恢复、容量测量、部署运维说明 | 自动检查与人工验收有证据，已知限制明确，达到相应发布阶段的范围 |

以下为P03完成时的历史基线（当前P04结果见上）：

**M0/M1、P01、整个P02及P03a–e/整个P03均已完成本地与远端完整验收。**
P03代码f66c4e7的push39452554961/PR39452563178六个job全部success：65unit+75真实
PG/HTTP/WS/迁移、177前端/构建、45契约、标准Compose23真实浏览器和五组实际容器
重启均通过；本地独立无数据库单测、真实Java+PG重启及16旧表原列/原行/六checksum
自然升级比较也通过。原全部测试保留，详细失败闭环与证据见verification和两端TDD。
继续原分支/草稿PR #1，不合并、不发布。下一包P04规则检索与填卡尚未开工；整个M2、
P04–P10/A仍未完成，检索、投骰、战斗、容量/触屏/TLS/完整备份与异机恢复不冒称完成。

## 当前分工

2026-10-21：P01–P07的本地及精确代码SHA双CI门槛全部通过，当前下一包P08团队笔记及DM资料。契约与目录责任延续下表；不重复开发已完成包。完整证据见verification/continuation，M2及P08–P10/A仍未完成。完成文档HEAD亦独立核验。

| 负责方 | 目录与责任 | 依赖 |
| --- | --- | --- |
| 协调者 | contracts、docs、deploy、scripts、CI、Git；工具准备、最终代码审查、集成验收 | 契约先行，汇总各模块测试证据 |
| 后端代理 | backend；领域、应用、JDBC/Flyway、HTTP、会话安全、STOMP | 遵循 contracts；测试数据库由协调者准备 |
| 前端代理 | frontend；React/PixiJS、API 客户端、状态同步、中文界面、组件测试 | 遵循同一契约；不发送自报权限或最终骰值 |

契约变动先协调，禁止分别实现两套不兼容字段；各代理只改自己目录，协调者统一提交。

## M1 测试顺序与验收

1. 建立框架与测试运行器。首先针对移动权限、坐标/版本或客户端状态写最小行为测试，执行并观察预期失败。
2. 实现纯领域行为。用相同测试确认通过，再重构；记录具体命令、结果和测试数量，环境失败不算行为 RED。
3. 接入会话安全、HTTP 与 PostgreSQL。集成测试运行真实迁移与真实事务，覆盖登录、CSRF、隐藏过滤、未授权移动、版本冲突与幂等重试。
4. 接入前端和 STOMP。确认结果才更新已保存状态；失败保留已确认快照；重连重新读取。实时消息仅作失效提示。
5. 两个不同会话进入同一战役：DM 能看到隐藏敌人，玩家不能；玩家移动获授权棋子，另一会话读取同一位置；重复请求不再次移动，过期版本被拒绝；重新建立会话和查询仍读到持久化结果。
6. 运行前端测试/类型检查/构建、后端单元与集成测试、仓库与契约检查。Docker、浏览器或网络受限的检查明确列为未执行，不能当作已通过。

## 执行状态

- M0框架与M1最小切片已实现；目录、契约、前后端、真实PostgreSQL迁移、会话及实时同步已落地。
- 最终本地检查：16后端unit、9真实数据库/WS集成、14前端测试、1真实双浏览器流程、构建及打包进程重启回放均通过。证据见 [阶段验收记录](verification.md)。
- c57c35a已推送，[远端CI](https://github.com/xianchen233/dnd-roll/actions/runs/39124712714)全通过，含Compose构建/启动/真实浏览器；Git终端认证已解决。
- 当前分支feat/accounts-and-invitations，P01正式账号/邀请/成员管理已实现。30后端unit、18真实PG/HTTP/WS、最终36前端测试、2真实Chrome流程与实际Java重启检查通过；初始代码3c12f8d的[远端CI](https://github.com/xianchen233/dnd-roll/actions/runs/39126498540)三个job全部success。后续PR暴露的刷新竞态已通过RED/GREEN修复并推送dabef40，当前交付门槛为[PR #1 Checks](https://github.com/xianchen233/dnd-roll/pull/1/checks)中最新SHA全通过；草稿供审阅，尚未合并或发布。
- P02a 已按 contracts/scenes.md 契约与实际 RED/GREEN 实现 DM 空场景创建、共享当前场景切换、持久回执/版本、旧场景保存、V3 数据保留及前端跨场景响应隔离。37 后端 unit、28 真实 PG/HTTP/WS、前端测试/构建、3 真实浏览器和实际 Java 重启通过；最终审查已修复晚到撤权边界，59 前端测试/构建通过；代码 8836abb 的 push 与 PR CI 三个 job 全部 success，证据见 verification.md。当时下一子包为P02b，后续P02b/c结果见下文；P02整体仍未完成。
- 后续任务不能以“预留接口”为由标记完成。规则相关实现开始前使用工作区 `dnd-rule-lookup` 核对 5R 依据；当前移动仅为探索网格操作，不承诺战斗移动规则。
- P02b私有素材/地图配置已按contracts/assets.md完成实际RED/GREEN、52unit+41真实PG/HTTP/WS全量、90前端测试/构建、24HTTP契约、4真实Chrome、V3→V4数据比较及Java+PG实际重启。最终修复素材确认竞态和提交确认丢失文件保护，代码1bfac83的push/PR全部CI success，含标准Compose私有卷与数据库/后端重启。P02c接续已完成本地56unit+50IT、112前端测试/构建、27契约、7真实Chrome、V4→V5全旧列/行比较和Java+PG实际重启，5160c0f的push39141337705/PR39141340870全部CI success，含七流程及实际容器重启；然后P02d/e，P02整体尚未完成。
