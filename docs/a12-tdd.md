# A12：普通匕首近战击昏实际 TDD

2026-10-27（Asia/Shanghai）。按AGENTS读取最新交接、验收、设计、实施、CONTEXT、
契约与M3余项。A07–A11不重复实现。原分支feat/accounts-and-invitations、HEAD及
ls-remote均c034aa48fe1176f8e4e0711cf22031d04b5f6ebd；连接器只读核对PR #1为open draft。
第一次连接器参数校验失败，改正repository_full_name后成功，未修改PR元数据。
124份入口文件逐字节备份于仓库外 `.tools/a12-entry/`，含SHA-256清单。

先[5R来源/矩阵](research/dagger-knockout-5r.md)，再[契约](../contracts/dagger-knockout-effects.md)
与实际RED。独立普通匕首STR/DEX、明确近战/投掷区分和例外审阅；单独确认再APPLY。
原A11短棒入口/配置保持，其他旧门保持，无新迁移。所有日志位于仓库外 `.tools/`。

| 检查 | 实际结果 |
| --- | --- |
| a12-contract-red.log | 实际缺少A12路由断言失败 |
| a12-ui-red.log | 5新增测试失败，缺少匕首控件及保存后说明 |
| a12-http-red.log | 188unit通过；9新增真实HTTP测试均因路由404失败，0错误/跳过 |
| a12-http-green.log | 188unit+9真实HTTP通过，0失败/错误/跳过 |
| a12-ui-green.log | A11旧控件与新增控件合计29通过；首次构建通过 |
| a12-contract-final.log | 142操作通过；包含THROWN仅新配置接受、旧配置仍拒绝 |

中途契约复验因README尚未列出新增两路径而失败，补齐后通过；不是后端失败。
首次生成控件出现重复“匕首”字样，自审修正后进行全前端/构建验证。
9项HTTP涵盖独立来源/属性门、STR与DEX、投掷人工、旧短棒入口仍拒绝匕首、
跨入口同key冲突、当前DM/CSRF/严格字段、源/目标/行动/hash/预览版本、同key并发、
双DM及双预览竞争、立即/延迟失败全行回滚、数值更正不醒来、旧回执不重建已移除记录。

本轮使用独立PG18468/Java18133/网页18134及全新数据目录；未操作dnd-roll-local、
其数据或旧fence。完整P09独立恢复/容量/云端/真机后置，不宣称M3完成。
全量回归、生产浏览器与真正重启结果待下续；无暂存/提交/推送/合并/发布。

## 首轮生产浏览器失败与修复

`a12-browser.log`：65通过/1失败，零retry。新增匕首流程本身通过；旧A11流程的
`getByLabel('近战击昏处理')`同时匹配原短棒和新增“匕首近战击昏处理”，导致
Playwright strict-mode失败。保留首轮trace/截图及捕获，不改旧测试或重试配置。
将新增控件改名“匕首使用方式”，并独立成“普通匕首近战击昏”分组，保留旧控件
及A11测试原样。随后重跑完整前端/构建与完整66生产Chrome；结果见下节。
首轮数据/截图/捕获保留在a12-playwright-results与a12-browser-final-evidence.json；
最终使用独立a12-final-playwright-results及a12-browser-verified-evidence.json。

分组调整首次漏闭合条件表达式，a12-ui-group-syntax.log记录8个测试文件转换失败、
30文件256测试通过；a12-build-group-syntax.log记录TS2657。补齐两个独立条件分组后
再跑完整前端与构建；这次语法失败保留，不算GREEN。未改A11代码含义或旧测试。

第二轮a12-browser-final.log：65通过/1失败，两个击昏流程均通过。既有角色卡
lost-resource-response测试在enter时找不到战役选择器；a12-second-browser-trace.log
从trace定位静态getTextureBatchBindGroup-CDnZS9mH.js请求返回网络失败
`net::ERR_NO_BUFFER_SPACE`，不是角色资源行为断言失败。保留第二轮完整trace、截图、
捕获；不改产品部署配置、测试断言、超时或自动重试。第三轮完整验收使用独立
a12-browser-accepted.log、a12-accepted-playwright-results、a12-browser-accepted-evidence.json。

## 最终本地验收与停机

- `../.tools/a12-test.ps1 -Full -Log a12-backend-full.log`：188unit+263完整真实
  PG/HTTP/WS IT，0失败/错误/跳过，5m41s。包含9项新增HTTP及全部旧254项。
  新DEX用例采用DEX30/STR10，实际保存重击伤害14，验证使用敏捷的伤害结果。
- a12-frontend-final.log：38文件353通过；a12-build-final.log生产构建通过，
  798.10kB主chunk警告保持。a12-contract-final.log：142操作通过。
- `../.tools/a12-browser-accepted.ps1`：a12-browser-accepted.log，最终JAR下完整66生产Chrome通过，
  零retry。新增流程明确投掷不能确认、确认不改卡、实际丢失提交后的APPLY响应再
  精确重试仅一次HP/昏迷效果、旧回执不覆盖后续独立更正；私密证据不进入玩家。
  已查看a12-dm-plan.png与a12-dm-390.png，控件可读，无可见横向溢出；不是真机。
- a12-native-stopped.log/进程身份文件证明真正停止Java18133、网页18134及PG18468；
  同数据/同端口重启，a12-restart-verify.log新会话比较卡/历史/骰/行动/预览/原回执。
  a12-restart-resume.log继续保存的待应用方案，目标HP/记录只生效一次，来源、资源、
  原骰与证据保持；保存a12-browser-accepted-evidence.json及.resumed.json。
- 最终a12-stopped.log：按身份停止三服务，独立端口无监听，数据与截图/证据保留。
  `../.tools/a12-test.ps1 -Offline -Log a12-offline-unit.log`：清数据库环境后188
  离线单测再次通过，0失败/错误/跳过。
- a12-preservation-audit.log：124入口文件全部保留且备份hash核对，OpenAPI仅新增
  2路径/3schema/1事件kind，其余旧契约结构相等；六份入口/设计文档原字节为完整
  后缀保留，包含云补充。原CI配置保持。本包代码/契约diff-check通过；根检查仍有
  入口既存continuation/verification尾部空行告警，未改写历史消除告警。

本轮没有提交/推送/合并/发布或新HEAD远端CI；没有运行完整P09独立恢复、容量、
云端或真实手机。M3不关闭，最终恢复必须加入A08–A12捕获与继续。无新迁移。

验收运行JAR与backend/target JAR的SHA-256均为
`7822bffe68a8574176d996029072a10a9be0f3ae6e837385433502f134494ef8`。
最终保留132份未提交文件，HEAD仍c034aa4，暂存区空；全部124入口文件继续存在。
