# A11：普通短棒近战击昏记录的实际 TDD

2026-10-27（Asia/Shanghai）。按AGENTS读取最新交接、验收、设计、计划、CONTEXT、
契约及M3余项；A07/A08/A09/A10不重复。原分支feat/accounts-and-invitations、
HEAD/ls-remote/PR #1均c034aa48fe1176f8e4e0711cf22031d04b5f6ebd，PR仍open draft。
连接器首次网络失败、第二次只读复核成功；没有修改PR元数据。116份入口未提交文件
逐字节备份于仓库外 `.tools/a11-entry/`，含SHA-256清单，云补充与全部历史保留。

先[5R来源及支持矩阵](research/knockout-effects-5r.md)，再[契约](../contracts/knockout-effects.md)
与实际失败。仅受信短棒STR、DM明确近战击昏及例外审阅；伤害足以降至0时改留1HP，
独立APPLY原子记录生命与昏迷注记。无新迁移。短休/急救/治疗解除、昏迷关联规则
仍人工，中文界面和状态说明均明示；不假装完整状态执行引擎。A08/A09/A10旧门保持。

## 实际执行记录

日志位于仓库外 `D:/workspace/dnd/.tools/`；不打印凭据。

| 检查 | 结果 |
| --- | --- |
| 契约RED | `node scripts/check-contracts.mjs` 缺A11路由，a11-contract-red.log |
| UI RED | `npm test -- --maxWorkers=2 src/attack-previews.test.tsx`，1新增失败/19旧通过，缺“近战击昏处理”，a11-frontend-red.log |
| HTTP RED | `../.tools/a11-test.ps1 -Prepare -Log a11-http-red.log`：185unit通过，5新HTTP均404失败、无错误/跳过 |
| 第一后端复验 | a11-http-green.log：188unit中0断言失败/1清理错误；既有BackupJobsTest临时目录DirectoryNotEmpty，未进入HTTP |
| 第二后端复验 | a11-http-boundaries.log：同为188unit、另一备份用例清理DirectoryNotEmpty；并非A11 GREEN。两处目录稍后检查已不存在，原因未确认，不改产品/旧测试掩盖 |
| 独立临时目录复验 | 将本包TEMP/TMP/java.io.tmpdir改为新`.tools/a11-tmp`，a11-http-isolated-tmp.log：188unit+8真实PG/HTTP通过、0失败/错误/跳过 |
| 首轮前端/构建 | a11-frontend-full.log：347通过；a11-build.log生产构建通过，大chunk提示保留 |
| 保存后说明RED | 自审发现新确认被错误显示为死亡说明；补测试后a11-saved-guidance-red.log实际1失败/23通过，再修复为独立击昏/解除说明 |
| 契约首轮GREEN | a11-contract-green.log：140操作通过，包含新严格请求与候选/人工联合 |

另一次辅助Python单行替换因PowerShell引号转义SyntaxError未执行，改用apply_patch；
不计产品RED。完整后端、最终前端/构建、生产Chrome、真实停启与继续结果下续。

独立PG18466、Java18131、网页18132；不操作dnd-roll-local、其数据、旧fence或恢复目标。
沿用受限验收JVM参数，非容量结论。完整P09独立恢复需含A08/A09/A10/A11，仍为后续；
不提交/推送/合并/发布，不以旧HEAD CI替代新代码结果。云端和真实手机后置。

## 全量回归及首轮浏览器失败

- `../.tools/a11-test.ps1 -Full -Log a11-backend-full.log`：188unit +254完整真实
  PG/HTTP/WS IT，0失败/错误/跳过，6m41s；8项新HTTP与全部旧246项同轮通过。
  覆盖近战来源/属性拒绝、准确HP/tempHP边界、严格字段/重复键、当前DM/CSRF、
  确认与APPLY立即/延迟失败全行回滚、同key并发、双DM/双预览竞争、确认后改卡、
  旧回执不重建已移除记录及数值更正不擅自解除昏迷。
- a11-frontend-final.log：37文件348通过；a11-build-final.log生产构建通过，
  794.75kB主chunk提示保持。a11-contract-final.log：140操作通过；独立结构核对
  a11-preservation-audit.log确认仅增2路径/3schema/1事件kind，其余原契约相等。
- 首轮完整Chrome `a11-browser.log`：64通过/1失败，零自动retry。A11流程本身通过。
  既有combat.spec.ts在进入DM页面等战役选择器时失败；保留完整trace和截图。
  `a11-first-browser-trace.log`定位DM的getTextureBatchBindGroup静态JS请求失败，
  浏览器控制台为 `net::ERR_NO_BUFFER_SPACE`，该页面没有后续auth/me/campaigns读取。
  不能称首轮全绿；未改产品/旧测试/超时/重试，等待后端全量结束后串行重跑完整Chrome。
- 首轮A11桌面/390px截图已查看，文字/控件可读，无可见横向溢出；390px不是物理手机。
  `a11-before-restart.log`为重启前快照基线核对，不计真实重启证据。该脚本输出已改为
  提示进程重启由独立证据确认，避免仅凭快照脚本输出推断已重启。
- 运行JAR与最终target JAR SHA-256均为
  `b22b6fa8c0873c8cf39844b0b3b19b4398c98697c3ea1df12eca1b125e561648`。


## 最终本地验收与停机

- `../.tools/a11-browser-rerun.ps1`：a11-browser-final.log，完整65生产Chrome通过，
  零retry；没有改业务断言/超时。后端全量完成后串行运行，原combat失败流程及
  A11均通过，最终捕获a11-browser-final-evidence.json保留。
- a11-stop.ps1核对PID/命令行后真正停Java18131、网页18132及PG18466，
  a11-native-stopped.log与a11-native-stopped-processes.json保存停机身份；同目录同
  端口重新启动PG，再a11-restart.ps1启动同JAR/静态网页，a11-restarted-ports.json。
- a11-restart-verify.log：新会话比较全部卡/历史/骰/行动/预览/hash/原回执/待应用
  方案一致；a11-restart-resume.log：继续持久方案，目标HP=1/tempHP=0和单条昏迷
  记录同一次版本写入，重试单效，来源/资源/骰不变，旧回执不覆盖新值。
  继续后的快照保存为a11-browser-final-evidence.json.resumed.json。
- 最终a11-stopped.log：按身份停止三服务，18466/18131/18132均无监听，数据/素材/
  截图/捕获保留；清除所有DATABASE/TEST_DATABASE环境后
  `../.tools/a11-test.ps1 -Offline -Log a11-offline-unit.log`再次188通过、0失败/错误/跳过。
- 本轮代码/契约/前端聚焦diff-check通过。根diff-check仍仅报告入口已有
  continuation/verification尾部空行；未改写历史来消除告警，不称根检查全绿。

116份入口文件全部保留，最终逐字节核对日志a11-preservation-audit.log；6份交接/
设计文档仅增加当前说明，旧内容（含云补充）作为逐字节后缀保留。未提交、推送、
合并、发布或修改PR元数据，HEAD仍c034aa4，暂存区空。没有运行完整P09独立恢复/
容量/新HEAD远端CI/云端或真机；这些门槛继续保留，M3不关闭。
