# A09：短弓箭支与伤害联动的实际 TDD

2026-10-26（Asia/Shanghai）。按AGENTS读取最新交接/验收及Git状态，确认A07/A08
已完成，不重做。沿用feat/accounts-and-invitations / 草稿PR #1；本地、ls-remote和
GitHub连接器均核对HEAD仍c034aa48fe1176f8e4e0711cf22031d04b5f6ebd，PR open/draft。
99份入口未提交文件逐字节备份至仓库外`.tools/a09-entry/`，含SHA-256清单。
没有暂存/提交/推送/合并/发布或修改PR元数据；云部署补充保持。

先查证[5R来源与支持矩阵](research/ammunition-effects-5r.md)，再写
[契约](../contracts/ammunition-effects.md)和实际失败测试。只接受受信内置普通短弓、
DEX、DM明确映射的来源CUSTOM箭支计数器，固定每次攻击耗1，失手/免疫也耗。
来源状态/死亡/0HP、同身份、A08目标回退及未审阅例外均整体人工；不自动识别
资源名称、不改库存、不自动回收、不泛化到其他弹药/扩展。

## 实际RED/GREEN与命令

外置日志在`D:/workspace/dnd/.tools/`，不入库、不打印凭据。

| 顺序 | 实际结果 |
| --- | --- |
| 契约RED，23:20 | `node scripts/check-contracts.mjs`：缺A09 ammunition-effect路由，a09-contract-red.log |
| 前端RED，23:20 | `npm test -- --maxWorkers=2 src/attack-previews.test.tsx`：1新增失败/11旧通过，缺中文箭支映射控件，a09-frontend-red.log |
| HTTP RED，23:20 | `../.tools/a09-test.ps1 -Prepare -Log a09-http-red.log`：179旧unit通过，3新增真实HTTP预期200实际404，0错误/跳过 |
| HTTP GREEN，23:22 | 同聚焦命令：179unit+3真实HTTP通过，a09-http-green.log |
| 前端GREEN，23:24 | 聚焦12通过，a09-frontend-green.log；请求只有明确映射/审阅/hash/版本，不上传最终数值 |
| 补边界，23:28–29 | 182unit+6真实HTTP通过，a09-http-boundaries-fixed.log；立即/延迟失败回滚、同key并发、双DM同版本竞争、当前权限/陈旧/来源回退；随后追加最后一箭的双预览竞争至完整验收 |
| 前端全量 | `npm test -- --maxWorkers=2`：37文件337通过，a09-frontend-full.log；`npm run build`通过，a09-build.log，保留大chunk提示 |
| 契约 | 136操作通过，a09-contract-green.log；新严格请求/候选/人工联合，以及generic events不可接受CONFIRM_AMMUNITION的断言 |

来源/目标/行动版本和精确hash由服务器复核，读方案与确认都不修改卡。新的
CONFIRM_AMMUNITION事件保存服务器派生的两卡最终变化，独立APPLY原子写入来源
箭支、目标HP/tempHp、两卡历史、P06行动、预览事件及回执。原A08 DAMAGE-HP-V1和
手动路径含义保持，新kind只允许新确认端点生成。无数据库迁移或持久记录形状变更。
暗骰、公开摘要与更正继续独立；更正采用当前明确最终值，不自动返还旧箭支。

前端明确选择计数器与审阅，读取权威方案后另行确认/应用。映射/审阅/预览/revision
变更清掉方案，迟到成功隔离；当前域迟到403清除私密状态。不确定确认保留原route/
body/key并锁住竞争控件；ack后读失败仍只重读。相关聚焦/全量测试均实际执行。

## 辅助与环境失败（不计产品RED）

- 补充测试一处Java `var`复合声明导致testCompile失败，a09-http-boundaries.log。
  分开声明后实际执行6IT通过；没有把编译错误当业务RED。
- 首次生产Chrome生成/选取真实预览后，验收Java进程62096原生内存分配失败，
  Native memory allocation malloc约7MB失败，随后代理502；a09-browser.log为失败。
  崩溃日志/replay与截图/trace保留在仓库外a09-browser-first-failure/，没有提交这些
  生成文件。该时完整后端正在同机运行；不能仅据此证明唯一根因。
  后续待完整后端结束，给验收JVM明确内存上限并顺序复验；不改业务断言/timeout/retry。
- 完整后端结束后，以Xmx384m、Metaspace256m复验新增生产Chrome单项1通过，
  a09-browser-fixed.log；随后启动全量时JVM27424再次原生内存分配失败，崩溃记录
  当前线程为C2 CompilerThread1，malloc约1.9MB失败。a09-browser-full.log为失败，
  后续502不是业务断言通过；a09-before-restart.log在登录CSRF读取阶段同为502，
  未执行到状态比较。第二份崩溃/replay与trace保留在a09-browser-second-failure/。
  后续使用2个活动处理器、SerialGC、C1编译、Xmx256m/Metaspace160m/CodeCache48m、
  512KiB线程栈及验收Tomcat最多32线程，顺序复验。以上仅验收运行时限制，未改
  产品/生产部署配置，多个因素同时改变，不声称证明唯一根因或完成容量验收。

## 最终实际验收

- `../.tools/a09-test.ps1 -Full -Log a09-backend-full.log`：完整182离线unit+241真实
  PG/HTTP/WS IT通过，0失败/错误/跳过。包含全部7项AmmunitionEffectsIT；两个不同
  预览都确认后争用最后一箭，APPLY仅一个成功，另一409，目标只变一次。
- 全量前端337通过及生产构建通过；136契约操作通过。独立解析入口JSON对比，
  仅新增2路由/3schema和1个事件kind，generic命令schema与其余路径/组件/策略相等。
  旧A08代码/证据/hash/确认语义保留；无迁移文件变更。
- 限定验收JVM后`npx playwright test`：完整63生产Chrome通过（3.4m），零retry，
  a09-browser-full-final.log。不是首次全绿；两次native内存失败及整轮63失败日志
  均保留。产品代码、测试业务断言、timeout和retry未因此改变。
  已查看ammunition-effects-*/a09-dm-plan.png和a09-dm-390.png，不是真实手机。
- 最终加载JAR与构建JAR SHA-256相等（7995372c04529bf37289fc7ac08708fa1ce879b8fee098eefac6fe74bfd940a9）。
  按PID/命令行核对，真正停Java18127/网页18128/PG18462，确认三端口无监听，再以
  同数据/同JAR和限定验收参数启动。a09-native-stopped.log、a09-restarted-processes.json
  为进程证据；a09-restart-verify.log新会话逐项核对全部卡/历史、骰、行动、事件、
  私密证据、映射及原回执，snapshot完全相等。
- `node scripts/check-ammunition-effects-restart.mjs resume <外置capture>`：真正持久
  的待确认方案应用一次，来源仅耗1箭、目标符合保存方案，其他资源/状态和骰子保持；
  原证据/hash保持，全部旧回执不覆盖新值。a09-restart-resume.log及
  a09-browser-evidence.json.resumed.json保留。预核对a09-before-restart-fixed.log也通过。

- 最终按PID/命令行再次停止三服务，18462/18127/18128无监听，独立数据/证据保留；
  a09-stopped.log。清除DATABASE/TEST_DATABASE环境后，以限定本机验收运行参数执行
  `../.tools/a09-offline-test.ps1 -Offline -Log a09-offline-unit.log`，23:50再次182离线
  单测通过，0失败/错误/跳过，数据库已真正停止。未操作其他运行实例。

A09只是此有界资源联动切片，M3整体仍不关闭。
最终独立保留核对：99份入口文件全部存在，80份仍逐字节相等；另外19份仅明确的
共享代码/契约/API/检查器及交接/CHANGELOG更新。五份完整旧交接（含云补充）作为
逐字节后缀保留。根`git diff --check`仍有入口已有的continuation/verification尾部
空行告警；没有为消警告改写历史，也不称根检查全绿。暂存区为空；最终再次核对
本地/origin/PR HEAD均c034aa4，PR仍open/draft。无新JVM崩溃产物留在仓库根。
没有新HEAD远端CI，不复用旧SHA绿灯。完整P09独立恢复/容量、精确交付HEAD日志、
云端与真实手机继续后置；不操作dnd-roll-local及其数据、旧fence/恢复目标。
