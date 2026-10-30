# A03f 附图资料公开：实际执行证据

2026-10-25（Asia/Shanghai）。沿用 `feat/accounts-and-invitations` / open draft
[PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，没有提交、推送、合并或发布。
按 AGENTS 先读交接/验收、设计/实施、CONTEXT、契约和工作包。没有游戏规则计算
变更；不重复 A06。剩余范围与顺序见 [M3 待办](m3-remaining.md)。

开工 fetch 后本地/origin/PR 为 `c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。
工作区原有两处并发修改：`docs/continuation.md`、`docs/verification.md` 的云部署
补充及末尾空行；已经另存入口副本并逐字保留，未提交。独立重新取回 push40170704932
与 PR40170708329 的六份完整日志，严格核验全部旧门槛：159unit+199IT、300前端/
构建、125契约、56生产Chrome、所有原重启/隔离恢复/容量及六 MOVEMENT/一 RESUMED。
证据在仓库外 `D:/workspace/dnd/.tools/m3-entry-ci.log`、对应 a06-ci-c034aa4…json
和六份新取 job 日志。这是入口 HEAD 的证据，不是当前未提交代码的远端 CI。

先写 [附图契约](../contracts/module-materials.md)，增加显式 PUBLISH_MATERIAL，
原 PUBLISH_TEXT 继续遇附图拒绝。新步骤复制当前标题/原文字和最多四张有序图片为
独立 TEAM 笔记/图片 ID/存储键；不复制私密修订、理由或原图片身份。另一名当前 DM
也可发布同团资料。所有来源实际字节、SHA、类型、长度和尺寸在完整组合写入前
核验并绑定预览。公开记录及所有其他步骤/历史/child回执/证据同事务；回滚清新文件，
未知提交结果新事务加战役锁核对，已提交或无法确定则保留不可猜测文件。

## 实际 RED、准备失败与 GREEN

所有命令在指定子目录执行，运行时、凭据与完整日志均在仓库外 `.tools`。

- 后端 `mvn -Dmaven.repo.local=D:/workspace/dnd/.tools/m2 -B -ntp
  -Dtest=ModuleOperationsTest test`：新步骤解析实际报 INVALID_MODULE，5项中1 error，
  `a03f-unit-red.log`。实现后全量包含5项解析器和5项 NoteImages 单测并通过。
- 前端 `npm test -- --run src/module-operations-ui.test.tsx`：新按钮不存在，
  1失败/14通过，`a03f-ui-red.log`；实现后15通过；补跨旧/新步骤去重、附图精确
  重试/API切域后最终该文件17项，包含在303全量中。
- `node scripts/check-contracts.mjs`：实际不能接受新步骤，`a03f-contract-red.log`；
  只增补新的严格分支后125操作通过，缺失/null/多余字段仍拒绝。
- 真实 HTTP 首跑测试代码使用复合 var 声明，编译失败（`a03f-http-red.log`）；
  修正后旧 NoteImages Mockito 因 TEMP 下创建 agent 被拒绝而失败
  （`a03f-http-red-behavior.log`）。两者是准备失败，不算行为 RED。
  将 TEMP/TMP 设为已有 `.tools/tmp`，用不涉及 mock 的旧归档单测运行聚焦 IT：
  `-Pintegration -Dtest=ModuleArchiveTest -Dit.test=ModuleOperationsIT#illustratedPublication* verify`，
  保存新操作预期200实际400，1实际failure，`a03f-http-red-runtime.log`。
  最小实现后原8+新3共11项 ModuleOperationsIT 通过；附图四张顺序/真实bytes、
  另一DM、撤权、源私密/公开清单、源缺失/坏图/替换/版本变化、跨团、即时及延迟
  提交回滚逐表/全部文件比较、并发同请求单效均有执行。没有降低旧断言或增加重试。
- 新纯单测边界和额外HTTP边界直接运行 GREEN，未冒称它们各自先有产品 RED。

## 当前未提交代码的完整本地验证

| 检查 | 实际结果与外部日志 |
| --- | --- |
| 后端全量 | `a03f-runtime.ps1 -TestBackend`：162unit +202真实PG/HTTP/WS/迁移，0失败/错误/跳过；`a03f-backend-full.log`。独立新PG55438/dnd_a03f_test，HTTP使用随机端口 |
| 前端全量 | 首轮默认worker过多发生本机内存分配失败/5 worker异常，251/303已执行，不能算全通过；`a03f-frontend-full.log`。保留全部用例/断言，以 `npm test -- --maxWorkers=2` 完整重跑，303通过/0错误；`a03f-frontend-final.log` |
| 生产构建 | `npm run build` TypeScript/Vite通过；`a03f-build.log`；既有大chunk提示未放宽 |
| 契约/兼容 | 125操作通过；独立按解析树比较：只移除新步骤分支后与HEAD全部path/schema/顶层策略完全相同；V1–V25无改动；`a03f-contract-compatibility.log` |
| 生产Chrome | 新流程聚焦1通过，随后完整57通过、0重试；`a03f-browser-focused.log` / `a03f-browser.log`。真实Java18095/web18096/PG55438，玩家独立会话；真实提交响应丢弃后原body重试，不重复公开；玩家实际显示16px图片并下载比对SHA，私密接口未被玩家UI请求 |
| 视觉 | 已查看桌面和390px布局PNG，保留 `.tools/a03f-material-{desktop,mobile-layout}.png`；这不是真实手机验收 |
| 实际服务重启 | `a03f-restart.ps1`：按CommandLine确认并真实停Java/web/PG再启，新会话三份HTTP/浏览器附图快照、原回执和旧A03e/A05/A06控件捕获逐字段相同；最终JAR哈希相同；`a03f-native-restart.log` |
| 完整独立恢复 | 完整migration归档后停止本轮验收来源，在新PG55439/dnd_a03f_restored、独立素材、Java18097/web18098离线恢复；全部DB行/迁移/引用/文件匹配，三份附图及旧控件/攻击/移动快照和回执通过；`a03f-restore.log` |
| 恢复后继续 | HTTP捕获的原版本/原hash待公开组合成功一次、原请求重试单效；补强后再用浏览器捕获的另一待公开组合验证恢复后新公开图片SHA及私密ID不复用；`a03f-restore-resume-final.log`。没有重新预览或重基旧草稿 |
| 停库离线复验 | 按CommandLine/数据目录核验后停止本轮全部验收进程，六个独立端口无监听；清除数据库环境后 `mvn -o -B -ntp test` 再次162unit通过/0失败错误跳过，`a03f-unit-pg-stopped.log` / `a03f-stopped.log` |

CI已保留所有原重启、独立项目/卷恢复与容量断言，追加HTTP和浏览器附图capture的
重启/恢复比较、恢复后的原草稿继续执行。因用户要求不擅自提交，此次没有新SHA，
这些新增CI步骤尚未在远端运行；不能拿c034aa4的绿灯关闭A03f的远端交付门槛。

当前状态与停止验收服务后的离线复验见交接顶部。数据、备份和本轮来源fence保留。
从未启停、重置或使用 dnd-roll-local 的数据，也未操作旧source fence/恢复目标。
两份云部署补充没有进入暂存区；本轮不做云端或真实手机验证。M3整体未关闭，
下一执行包为便携组合的版本化格式/稳定引用契约与真实RED，随后实例绑定/选择
更新/恢复闭环；自动效果/公开自动应用及范围的支持缺口仍在待办中。
