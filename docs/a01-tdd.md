# A01 模组包：实际开发与验收

2026-10-22（Asia/Shanghai），沿用 `feat/accounts-and-invitations` / 草稿 PR #1。
本包契约 [module-packages.md](../contracts/module-packages.md)。不合并、不发布。
所有本机运行时、合成包、凭据及日志在仓库外 `D:/workspace/dnd/.tools`；凭据不入日志。

## 开工独立核验

先读 AGENTS、continuation、verification、product-design、implementation-plan、
CONTEXT、work-packages 和 contracts/README，以及 P02 素材、P04 内容与 P09 备份契约。
fetch 后本地/origin/PR HEAD 同为 **682af77d14dc1468b9f163bc8f4307b41602781c**，
工作区干净，PR open/draft/main。连接器核对元数据和 PR run，既有本机脚本重新获取
push39643825879 / PR39643836476 的六份完整日志并逐项断言：102unit+118IT、
227前端/构建、87契约、47浏览器、43原PASS、容器重启与隔离恢复、完整团和容量。
`a01-entry-ci-verified.log` 是本轮重新取得的证据，不沿用交接文字作为绿灯。

## 实际 TDD 与补充边界

| 步骤 | 实际 RED | 最小实现与 GREEN |
| --- | --- | --- |
| ZIP/JSON/身份/图片/引用 | `a01-archive-red-actual.log`：7项中4 assertion fail、3尚未实现异常；无跳过 | `a01-archive-green.log`：7通过。严格字段/类型/重复JSON、版本、路径及额外文件、长度/哈希、PNG真实解码、重复身份/悬空引用、空可选部分 |
| 授权预览与持久导入 | `a01-http-red-behavior.log`：2项预期200/实际404，无环境错误 | `a01-http-green.log`：2真实PG/HTTP通过。精确依赖、惰性预览、默认复用/明确副本、原ZIP、旧版明确确认 |
| 中文工作区 | `a01-frontend-red.log`：6项全部失败（空组件没有入口/请求） | `a01-frontend-green.log`：6通过。校验、确认、原File/body重试、切团/撤权清理、并行拒绝、下载URL清理 |
| 契约路由 | `a01-contract-red-actual.log`：缺A01 GET路由 | 新增4操作后91操作通过；后续追加严格命令/格式/ref/私密/事务断言也通过 |
| 已解析依赖的版本展示 | `a01-dependency-display-red.log`：7项中新增1失败，原6通过 | 同一预览中展示所有精确依赖的ID/修订/规则；最终234前端通过 |

在首轮HTTP GREEN之后补充的三个HTTP用例及迁移用例为**直接GREEN边界覆盖**，
不是伪造新的先失败证据。`a01-boundaries-green.log`：7归档unit、5 HTTP IT和1迁移IT。
包含同actor/request并发单效、双DM同版本单胜、跨团request冲突、失去DM身份后
旧回执/下载拒绝、猜ID/跨团隔离、CSRF/重复multipart/类型/额外字段/坏ZIP原子拒绝。
真实数据库触发器分别在receipt插入时与延迟commit时失败，包bytes、库行、版本和
回执全部回滚；移除注入后同request成功。V17→V18逐表原列/行及17 checksum保持，
模块回执同团FK拒绝外团引用。完整旧回归均保留。

## 如实保留的准备错误

- 首次 `a01-archive-red.log` 未进入行为断言：JUnit系统Temp目录AccessDenied。
  指定既有外置 `java.io.tmpdir` 后才得到上述真实RED。
- 首次HTTP命令 `a01-http-red.log` 被PowerShell拆分 `-Dit.test`；修正引号后的
  `a01-http-red-actual.log` 又因PG默认5432、测试配置55432而连接拒绝。
  核对实际pg-data进程、停止并以55432/huge_pages=off重启后才记录真实404 RED。
- 首次契约命令从frontend目录执行，找不到script；首次build发现测试helper误用
  getByRole的exact类型参数；首次外置OpenAPI生成器少一个闭括号。均已修复，
  不算产品RED。生成时对原87操作/全部旧schema做deepEqual，并保留原契约源文本。
- `a01-browser-focus.log` 在首次Java尚未就绪时登录csrf为502；就绪后两流程通过。
  实际重启编排在PG新进程就绪后、启动Java前挂起；核对并停止该编排进程，分步
  启动Java。期间 `a01-browser-final-focus.log` 也曾过早启动而csrf502；待8080
  真实200及重启验证完成后，`a01-browser-final-ready.log` 两流程5.0s通过。
  两次准备失败及挂起日志保留，不冒称产品RED或通过。
- 外置截图复制曾从backend目录使用错误相对路径；随后用绝对路径补存，不影响测试。

## 已实际完成的本地验收

- `a01-backend-full.log`：**109无数据库unit+124真实PG/HTTP/WS/迁移**，全部
  零失败/错误/跳过，重新打包。原102+118全部保留。
- `a01-frontend-final.log`：**234通过**；生产构建通过；既有主chunk >500kB
  提示保持，未放宽阈值。最后仅补模组复选框/长文本样式并再次构建通过。
- 契约 **91 HTTP操作**，原87操作及所有旧schema的有效结构与开工HEAD相同。
  严格字段/缺失/null/伪造actor/非整数/旧版标记、格式版本、未知可执行字段、
  引用数组和multipart均有检查。仍是聚焦检查器，不冒充通用OpenAPI validator。
- `a01-browser-full.log`：**49生产Chrome流程通过（2.6m）**，原47保留，
  每条60秒/零retry不变。新增两流程覆盖明确旧版确认/缺失依赖/原图真实校验、
  相同包默认复用与显式两个副本、原ZIP真实浏览器下载SHA、丢导入响应后原命令/
  ZIP精确重试、玩家无入口/请求/WS秘密字段、会话失效清理。最终两流程5.0s再通过。
  已实际查看深色模组库PNG；没有截图伪装成执行证据。
- 实际核对旧Java/PG CommandLine，停止并确认8080/55432无监听，再以同数据库/
  素材根启动新PG/Java。`a01-process-before.json` / `a01-process-after.json`
  留存新旧身份；`a01-module-restart-{seed,verify}.log` 新会话逐字段、原ZIP/图片
  SHA、精确缺失依赖、两个库ID和原receipt/当前DM权限均通过。
  `a01-old-restart-{seed,verify}.log`原十组全部PASS；
  `a01-party-restart-verify.log`完整团公开/私密快照、第二轮、待确认行动、原骰子、
  历史和地图/RP图片SHA逐字段保持。
- `a01-backup-local.log`：实际手动PG逻辑备份下载；清单包含V18及模块库/回执
  行摘要，ZIP/image随BYTEA进入database.dump。该次为manual，旧helper最后一条
  固定输出中的“persistent source write fence verified”不代表本次建立了migration
  fence；不据此冒称隔离恢复/物理异机恢复通过。原源端fence始终保留。
- 最终按实际身份停止Java/production preview/PG，8080/8081/5173/55432无监听。
  停PG并清除数据库环境后，a01-unit-no-db.log离线109unit零失败/错误/跳过通过。

## 交付与下一步

代码 **2a9bb2a9c2455cda06944d7068b88854378972d6** 的push39651535455 /
PR39651544111均completed/success。独立下载六份完整日志后严格断言通过：每组
109+124零失败/错误/跳过、234/构建、91契约、49浏览器、原43PASS、4条MODULE VERIFIED、
完整团两次逐字段比较、恢复后3流程及9会话/100棋子/72消息容量errors=0。A01关闭。
外置a01-code-ci-verified.log、a01-ci-2a9bb2a9c2455cda06944d7068b88854378972d6.json
及a01-job-{jobId}.log保留。完成文档HEAD也必须重新取得同等完整证据。
CI已加入第十一组module seed/verify，
以及真实迁移归档→独立项目/全新卷恢复后的原ZIP/副本/依赖/回执/权限验证。
原十组、完整团、恢复后3流程和容量全部保留。

后续A02先固定模板原件、定制副本与独立战役状态及版本升级边界；当前v1描述/稳定
引用与图片不是完整可玩的战役。需要typed placement/resource/operation载荷时必须
明确修订交换格式，不在v1未知字段里执行。A03–A06、云环境/部署/TLS/物理异机恢复
与真实手机检查继续待执行，不以本轮结果关闭整体M3或发布门槛。
