## A03h 补审：退役后原绑定回执（2026-10-25）

先按AGENTS.md读交接/验收/设计/领域词汇/契约/工作包并检查Git，fetch和GitHub连接器
只读确认本地/origin/PR #1仍c034aa48fe1176f8e4e0711cf22031d04b5f6ebd、open draft。
70份既有未提交文件原字节备份到仓库外.tools/a03ha-entry；未使用旧测试库、恢复
目标或dnd-roll-local。独立PG55446使用新目录、新凭据及dnd_a03ha_test数据库；
浏览器另用dnd_a03ha_browser/独立素材，Java18111/网页18112。

契约module-operation-migrations.md原已要求退役保留精确历史回执，本轮补充明确
包括原绑定ack。ModuleBindings.bind原在读回执之前调用活动target检查，退役后
原成功请求也404。先加真实HTTP测试并执行
外.tools/a03ha-test.ps1 -Prepare -Log a03ha-receipt-red.log：1项失败/0错误/0跳过，
明确expected200/actual404，不是测试准备错误。最小修复仅将回执查找移至当前目标
DM/实例授权之后、活动映射检查之前；新请求仍必须有活动映射。随后原命令（去掉
-Prepare，日志a03ha-receipt-green.log）1项通过。新增测试对每次旧回执、变更body409、
新请求404、源库撤权仍读和目标撤权403逐次比较所有数据库行；不复活、不重绑、
不改定义/历史/回执。原先绑定/版本/并发/原子失败测试全部保留。

实际完整回归（日志均在仓库外.tools）：
- a03ha-test.ps1 -Full -Log a03ha-backend-full.log：171单测＋223完整真实PG/HTTP/WS IT，
  零失败/错误/跳过。覆盖之前完整220＋单独2项及本次新增1项。
- npm test -- --maxWorkers=2：319通过，a03ha-frontend.log；npm run build生产构建通过，
  a03ha-build.log，原778.51kB主chunk提示保持。
- node scripts/check-contracts.mjs：132操作通过，a03ha-contract.log。
- a03ha-browser.ps1：最终JAR＋生产静态页面＋真实Chrome，module-bindings.spec.ts和
  module-operation-migrations.spec.ts共2条通过、0重试，a03ha-browser.log。
  先前60条全量浏览器/真实服务重启/完整P09恢复证据保留，本轮未重新执行它们。
- 核对新验收进程身份后停止Java18111/网页18112和本轮PG55446，三端口无监听，
  新数据/素材保留；清除DATABASE与TEST_DATABASE环境后
  a03ha-test.ps1 -Offline -Log a03ha-offline-unit.log：171单测通过。

无新迁移、前端或CI修改；CHANGELOG同步更新。入口文件与两份云部署补充保留，
保护核验a03ha-final-review.json；暂存区为空、无提交/推送/合并/发布。旧HEAD远端
CI不能代表未提交代码。A03h本地有界完成，M3余项不删除，云端和真实手机仍后置。
以下完整保留入口A03h执行证据。

# A03h 初始实例绑定：执行证据与剩余边界

## 2026-10-25 后续选择迁移、退役与独立恢复映射

原分支/草稿PR #1、HEAD/origin/PR仍c034aa4；开工fetch和连接器只读核对。
入口全部未提交文件逐字节保存在仓库外 `.tools/a03h-selective-entry/manifest.json`。
以下是本轮新增结果；下方初始绑定的剩余项描述属于当时阶段。

先写 `contracts/module-operation-migrations.md`，另加三个严格接口：
`operation-options`、`operation-migration-impact`、`operation-migrations`。
每个既有/退役/新增nodeId必须明确KEEP/INCOMING/RETIRE，并回传绑定和本团操作版本。
KEEP保留本团编辑；INCOMING保留已有UUID，来源变更/重新启用/明确替换本团编辑时
回到未绑定状态；RETIRE保存tombstone映射，保留定义、执行历史、绑定历史和回执，
禁止新保存/预览/执行，精确旧回执只返回原结果。退役身份继续占用100身份容量。
源/目标当前DM、原实例/副本/战役/绑定/操作版本和完整hash先核验；每条确认均须
精确回传。恢复团、文件、结构、绑定、不可变前像/选择/完整实况和回执同事务。

恢复团保存已接受惰性源定义的v3封套，重新分配操作及全部实况目标身份并保持
UNBOUND，不复制原团版本、执行、回执或先攻确认。本团运行时编辑保留于原团和
不可变迁移证据供明确审阅，不静默转成便携指令。原文字/结构两门仍阻断便携定义，
只有新选择入口可应用。没有修改V1–V26迁移文件或新增数据库迁移。

实际失败与修复（日志均在仓库外 `.tools/`，没有覆盖RED文件）：

- `a03hs-route-red.log`：新增真实HTTP测试期望200，实际新 `operation-options` 404。
  接入后首轮500：新回执kind超过既有varchar(16)；缩短后又触发既有kind CHECK。
  两次实现错误保留于 `a03hs-receipt-kind-failure.log` / `a03hs-adoption-red.log`，
  不是被当成通过。最终复用已有MIGRATE存储类型、保持独立请求指纹，不改旧约束。
- `a03hs-local-edit-red.log`：来源内容未变但本团已编辑，明确INCOMING后仍boundVersion1，
  测试期望0失败。把显式替换本团编辑纳入待绑定条件，原2项真实IT通过。
- `a03hs-ui-red.log`：两个中文控件测试因缺少选择入口实际失败；接入逐项选择、
  全部确认、完整前像、精确不确定请求重试和撤权隔离后通过。
- `a03hs-contract-red.log`：README增加三接口后实际路径对比失败；补严格schema、
  只读POST/CSRF/当前双域DM/私有响应和约束样例，132操作通过。
- `a03hs-private-header-red.log`：新增玩家拒绝响应缺少private失败；修复过滤器
  对module-instance全部子路径的覆盖，后续完整220IT通过。
- 契约生成初次引用不存在的ModuleAck被检查器拒绝；改用既有CharacterAck并把
  候选文档明确引用v3 schema后通过。没有把该准备错误声称为业务RED。

实际验证：

- `a03hs-backend-220-full.log`：171单测＋220真实PG/HTTP/WS完整IT，零失败/错误/跳过。
  新5项覆盖保留/采用本团编辑、退役/历史执行回执/独立恢复绑定、过期与严格选择/
  撤权优先、立即/延迟全库行和文件回滚、同请求并发/双DM单胜、来源删除/再引入。
- `a03hs-remaining-boundaries-green.log`：另2项新增来源与结构删除的完整组装引用、
  退役身份继续保留容量边界通过。不是一次222IT全量运行；生产代码未因此再修改。
- `a03hs-frontend-final.log` / `a03hs-build-final.log`：319前端和生产构建通过；含
  新接口实际CSRF POST、无默认选择、精确重试、过期预览撤权及API/战役域隔离。
  原约778KiB主chunk提示保留。`a03hs-contract-final.log`：132契约检查通过。
- `a03hs-browser.log`：最终JAR下60条真实生产Chrome全部通过、零retry。新流程
  实际丢弃已经提交的迁移响应，比较两次完整body相同，只产生一个恢复团与迁移；
  原F/G/H/e/A05/A06浏览器流程保持。桌面/390px截图已查看，不是真实手机。
- `a03hs-http-evidence.log`：独立HTTP选择迁移/原本团编辑回执/恢复新映射证据通过。
  `a03hs-restart.log`：按进程身份真实停启Java18107/网页18108/PG55444，两个选择
  捕获与旧F/G/H/e/A05/A06全部快照、附图SHA和原回执逐字段比较通过，最终JAR匹配。
- `a03hs-restore.log`：真实migration ZIP/source fence并停止来源，恢复到独立新
  PG55445/数据库/素材/Java18109/网页18110；RestoreCommand验证全部数据库行、
  迁移、引用和不可变文件。新会话逐项验证上述8组捕获；恢复前两个原待绑定草稿
  各在原团及独立恢复团明确绑定一次、重试单效，先攻EMPTY/战斗IDLE。旧START草稿
  另行绑定后仍IDLE，再独立预览/明确触发才ACTIVE；便携作者编辑/另存仍能继续。

已按身份停止本轮全部服务；六端口无监听，两个新PG目录/数据、归档和来源写围栏
保留。没有启停/读写dnd-roll-local或操作旧来源fence/恢复目标。CI只追加新捕获、
重启/独立恢复/继续绑定断言，所有旧断言保留；本地没有Docker/远端新CI结论。
停库并清除所有数据库环境后，`mvn -o test`最终171通过：`a03hs-offline-unit.log`。
首次该复验未沿用专用临时目录，系统TEMP的AccessDenied令图片缓存/@TempDir失败
（5失败/66错误），实录 `a03hs-offline-temp-failure.log`。恢复项目既有TEMP/TMP及
java.io.tmpdir到 `.tools/tmp` 后原测试全通过，未修改断言或放宽权限/跳过检查。
没有暂存、提交、推送、合并或发布；两份云部署补充保留。云端/真实手机后置，
M3其他剩余项见 `m3-remaining.md`，不以A03h本地有界联验关闭整个M3。

最终保护核验 `.tools/a03hs-final-review.json`：35份未涉及本轮的入口改动逐字节
一致；四份交接/验收/实施/工作包的完整入口原文保留为精确raw后缀，含两份云部署
补充。全部旧101个OpenAPI path/236 schema及其他component逐项相同，仅绑定策略的
remaining说明按新增独立入口更新；所有旧CI断言保留。V1–V25按Git规范化换行比较、
V26与入口按原始字节比较均保持。最终fetch和PR只读复核仍open/draft/c034aa4，
暂存区为空。`git diff --check`仅原两份云部署文档既有EOF空行提示，未改写其尾部。

以下保留本轮之前的初始绑定执行证据。

2026-10-25；沿用 `feat/accounts-and-invitations` / 草稿 PR #1。
HEAD/origin/PR 为 `c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。
本轮不提交、推送、合并或发布；保留 A03f/A03g 未提交改动和两份云部署补充。
入口的 44 份既有改动逐字节保存在 Git 外 `.tools/a03h-entry/`，含 SHA-256 索引。
不操作 dnd-roll-local 及数据；使用 PG55442、Java18103、生产静态网页18104，
恢复目标 PG55443、Java18105、网页18106；云端与真实手机后置。

## 本轮实现的闭环

契约 [module-bindings.md](../contracts/module-bindings.md) 先规定版本、身份、当前
目标快照、显式确认和原子回执。v3 新团为每个稳定 TRIGGER 分配独立操作 UUID，
仅保存 UNBOUND 元数据和初始证据。绑定预览从本团当前映射解析棋子/角色/资料、
验证棋子场景及角色链接，采用当前版本并展示完整候选及目标。实际保存封套仍
受 64KiB 限制；待绑定身份计入 100 个组合容量，手动保存不能抢占其 UUID。

显式理由/hash/绑定及操作及实例及战役版本确认后，才经原 ModuleOperations
保存定义。绑定、原定义前像、历史、子保存回执、外层回执同事务提交。不会执行
步骤或掷骰。之后仍需原本团组合独立预览和明确触发。PREPARE 保留 DRAFT；START
须本团另行确认次序，绑定 START 也不启动战斗。当前目标 DM 授权先于旧回执重放；
源模板库撤权不撤销独立目标团。旧绑定回执不覆盖后来的本团编辑或实况。

V26 仅新增绑定/历史/回执三表。迁移测试在 V25 填入真实旧操作及执行记录，
比较全部既有表列/行和 25 个迁移 checksum，再验证跨团证据及回执 FK 拒绝。
中文控件展示完整候选、本团编辑前像和历史；未确认写入保留原 UUID/body 跨刷新
重试，已确认但刷新失败只恢复读取；API/战役切换和当前/迟到撤权清除私密状态。

## 实际 RED → GREEN

- 后端先执行 `ModuleLifecycleIT#bindingLaunchReservesIndependentIdentitiesAndOnlyExplicitCurrentPreviewCanSaveExecutableSteps`，
  当时 v3 launch 仍返回409，断言期望200，确实失败。随后新增 V26、保留身份和
  显式绑定，重跑171单测+该IT通过。最初 RED 输出见本会话17:01的执行结果；
  `a03h-backend-full.log` 是循环复用路径，已被后续绿色运行覆盖，不能冒称仍含 RED。
  该次 GREEN 保存在 `.tools/a03h-binding-first-green.log`。
- 前端先执行新增生命周期绑定控件测试，缺少预览入口失败：
  `.tools/a03h-ui-red.log`。实现显式控件后11项通过：`a03h-ui-green.log`。
- 接口检查先加四条缺失路由断言并实际失败 `A03h missing get`：
  `.tools/a03h-contract-red.log`。补 strict schemas、四接口、CSRF/只读/回执策略，
  再运行129操作通过：`a03h-contract-green.log`。既有125路径、全部旧schema/component
  逐项深比较保留，仅便携 readiness 策略按新行为调整。
- 生产构建发现两条新API误用读取调用及测试类型错误。新增真实HTTP适配器测试，
  复现未走CSRF/POST：`a03h-api-transport-red.log`。修正为现有 `post()`，
  真实适配器及绑定UI16项通过：`a03h-api-transport-green.log`，生产构建通过。
- 独立真实HTTP脚本已保存/触发/确认并持久化原回执和待绑定 START 后，验证玩家
  403响应失败：Cache-Control 缺少private，见 `a03h-private-header-red.log`。
  把新路由加入安全链之前的 PrivateAssetHeaders，并加入HTTP拒绝响应断言。
  修复后全量214IT通过，最终运行时的旧捕获也在重启/恢复中重新核验通过。
- 慢绑定预览尚未返回时点击刷新，旧预览被丢弃但控件一直禁用；新增测试实际
  失败见 `a03h-slow-preview-red.log`。刷新同步解除 reviewing，314项前端回归通过。
  随后真实浏览器拦住旧预览响应，刷新后确认可以重新操作，再放回旧响应并验证
  不显示旧预览；完整丢失保存响应/原请求重试/单独触发流程仍通过。

## 已执行回归

- 七类绑定HTTP边界覆盖：独立团身份/当前实际版本/保存不执行/撤权；过期目标/
  实例/伪造字段/保留UUID/CSRF；立即及延迟提交失败全库行回滚、相同请求并发、
  两DM同版本竞争、本团编辑后旧回执不覆盖；PREPARE和START单独确认；缺映射/
  改绑/退役目标和源库撤权；100待绑定容量和开团立即/延迟失败回滚；便携定义
  合规但展开后的完整运行时保存封套超64KiB时拒绝且无任何写入或掷骰。
- 后端最终实现：171离线单测 +214项完整真实PG/HTTP/WS集成测试，零失败/错误/
  跳过，`a03h-backend-final-full.log`。最后补充的完整运行时封套边界单独执行，
  结果1项通过，另见 `a03h-runtime-envelope.log`；不能把不同命令称为一次215项全量运行。
- 前端314测试、生产构建通过；129契约操作通过。日志分别为
  `a03h-frontend-full.log`、`a03h-frontend-build.log`、`a03h-contract-green.log`。
- 59条真实生产Chrome流程通过，无重试，`a03h-browser.log`。新增绑定流程真实
  丢弃已经提交的保存响应，比较两次完整 body 一致；确认没有执行后，再通过
  原组合控件单独预览和触发，产生公开资料/附图和DRAFT；明确确认后保存待绑定
  START预览。截图包含桌面及390宽布局；不是物理手机验收。
- 后续慢预览刷新修复重跑全部314前端测试、生产构建和增强后的绑定浏览器1项，
  `a03h-browser-focused.log`；59条全量在该小修复之前、最终后端JAR下已通过，
  没有把最后的单项复验冒称又一次全量浏览器运行。
- 最终JAR真实停止并重启 Java18103、网页18104、PG55442，五份HTTP/浏览器捕获
  及F/G/A03e/A05/A06旧证据在重启前后均逐项比较通过，`a03h-restart.log`。
- 真正创建P09 migration ZIP、持久化源写围栏并停止源服务，在独立空PG55443/
  数据库/目录恢复，验证全库行、迁移、引用和不可变文件一致；Java18105/网页
  18106以新会话比较五份捕获、原回执和待绑定预览通过，`a03h-restore.log`。
  随后对恢复前原START草稿确认绑定两次只生成一份定义/事件，战斗仍IDLE；
  另行原组合预览和显式触发，重试只产生一份ACTIVE执行证据。恢复后仍可编辑/
  另存便携模板，老定制回执不覆盖新版本。没有重放源团先攻确认。
- 独立服务六个端口全部停止，数据/ZIP/源围栏保留；取消数据库环境变量、两库
  停止后，`mvn -o test` 再次171项离线单测通过，`a03h-offline-unit.log`。
  部署日志外的第一次即时复验在后端未就绪时得到502（`a03h-http-green.log`）；
  等待就绪后原捕获完整通过（`a03h-http-green-ready.log`），未当成业务失败或跳过。
- 最终保护核验 `a03h-final-review.json`：24份未涉及本轮的入口改动逐字节一致；
  两份云部署补充及四份交接/工作包的全部入口原文作为原样后缀保留；所有旧
  API路径/schema/component、旧CI断言保留，V1–V25在Git规范化换行后无变化。
  最初按原始字节对比Git blob对V6既有CRLF报差异，确认仅工作区换行转换后调整
  核验方式，没有修改旧迁移；迁移IT的25个checksum与全库行比较均通过。
  `git diff --check`除两份云部署文件原有EOF空行提示外通过；未改写其尾部。

## 入口远端日志独立核验

重新通过 GitHub 连接器读取 PR 和两个 run 的六个作业，全部 completed/success。
重新下载六份完整日志到 `.tools/a03h-job-*.log`，逐项检查159单测+199IT、300前端/
生产构建、125契约、56浏览器，以及旧模块/攻击/移动重启、完整独立恢复、43条
PASS、9会话/100棋子/72消息零错误容量断言。入口 push40170704932、PR40170708329。
`a03h-ci-current-metadata.json`、`a03h-ci-log-review.json` 保存元数据及文件hash。
首次尝试单次保存所有日志被工具64000字节限额拒绝；改为小段逐字节写入后成功，
没有扩大权限或遗漏日志。入口CI仅覆盖已提交HEAD，不能替代未提交F/G/H的CI。

## 尚未关闭的 A03h / M3

本轮闭环是初始实例绑定。尚未实现后续源副本选择更新、单个组合退役，以及
复制实况到另一恢复团时重新生成操作/目标映射。两条原有 live migration 门
继续返回 `/portable-operations`，不会静默丢弃、覆盖本团编辑或引用原团版本。
下一步须先为每稳定 nodeId 定义 KEEP/INCOMING/退役选择、完整不可变前像、
本团编辑冲突、已执行历史保留和恢复映射，再写实际失败测试。普通P09完整恢复
保留同一团身份，不等同于上述独立恢复团的组合迁移。M3整体没有关闭。
