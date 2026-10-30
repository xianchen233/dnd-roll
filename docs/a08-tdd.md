# A08：普通伤害生命扣减的实际 TDD 与验收

2026-10-26（Asia/Shanghai）。先读取AGENTS、最新交接/验收、设计/计划、领域和契约。
沿用 `feat/accounts-and-invitations` / open draft PR #1；本地、ls-remote及恢复后的
GitHub连接器均核对HEAD为 `c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。
连接器首次传输失败后再次只读成功；没有更新PR元数据。入口83份未提交文件已在
仓库外 `D:/workspace/dnd/.tools/a08-entry/` 逐字节备份并保存SHA-256清单。
A07已完成，不重做；保留全部已有改动和云部署补充，不操作dnd-roll-local及数据。

先完成 [5R来源与支持矩阵](research/damage-effects-5r.md)，再写
[契约](../contracts/damage-effects.md)及实际失败测试。仅支持普通武器最终伤害先扣
临时HP、再扣HP且保持正HP；0HP/状态/死亡记录及未处理的关联效果转人工。
不是状态、专注、资源或全部扩展效果的自动执行。公开自动应用策略仍另包。

所有外置日志位于 `D:/workspace/dnd/.tools/`，不入库、不输出凭据。

| 顺序 | 实际命令与证据 |
| --- | --- |
| 契约RED，22:36 | `node scripts/check-contracts.mjs`：缺少A08 damage effect路由，`a08-contract-red.log` |
| 前端RED，22:36 | `npm test -- --maxWorkers=2 src/attack-previews.test.tsx`：1失败/旧7通过，缺中文预览控件，`a08-frontend-red.log` |
| HTTP RED，22:36–37 | `../.tools/a08-test.ps1 -Prepare -Log a08-http-red.log`：175旧unit通过，11项IT中2新增测试预期200实际404，0错误/跳过；不是编译失败 |
| HTTP GREEN，22:38 | 同聚焦命令：175unit+11真实PG/HTTP通过，`a08-http-green.log` |
| 前端GREEN，22:39 | 聚焦8通过，`a08-frontend-green.log`；包含新命令不接收客户端最终值 |
| 补边界单测 | 4个DamageEffect离线单测实际通过：遍历正HP/临时HP/伤害组合守恒、精确0HP边界、状态/死亡记录、输入范围、不可变理由；`a08-http-boundaries.log`中总179unit通过 |
| 前端全量，22:41–42 | `npm test -- --maxWorkers=2`：37文件332通过，`a08-frontend-full.log`；构建见`a08-build.log`，保留大chunk提示 |
| 契约 | `node scripts/check-contracts.mjs`：134操作，`a08-contract-final.log`；新增方案严格联合及命令枚举检查 |
| 首次生产Chrome，22:45 | 独立PG18460/browser库、Java18125、网页18126、最终产品代码JAR：新增流程1通过、零retry，`a08-browser.log` |

服务器从不可变A05证据的最终伤害推导，防御/重击不重复计算；GET只读且先校验
当前DM和原双方/行动版本。CONFIRM_DAMAGE要求明确审阅/理由、精确hash/版本和空changes，
保存服务器推导的目标最终值；单独APPLY才沿原P06事务写卡、历史、事件及回执。
旧手动CONFIRM、CORRECT、PUBLISH含义保持；无迁移，不改旧证据/hash。
`CONFIRM_DAMAGE`固定对应DAMAGE-HP-V1，未来不同规则配置必须另行版本化。

前端新增明确的方案读取及例外复核；revision、选择、API/团/角色切换清掉旧方案，
迟到成功隔离，当前域401/403清除私密状态；不确定写入锁住控件并保留原body/key，
确认成功但读取失败仍只重读。生产Chrome实际中断已提交APPLY的响应，精确重试单效；
伤害扣减/资源与状态保持/私密骰/独立更正/历史回执不覆盖后续编辑均有实际断言。

## 测试辅助错误（不计产品RED）

- API封装新测试起初遗漏既有GET选项中的`method: GET`与空headers，断言1失败；
  按既有封装实参修正测试，未改客户端传输实现。
- 补充HTTP夹具通过资源接口增加临时HP被现有规则400拒绝，13项IT中1失败。
  改用原HP扣减产生源版本变化，未放宽断言或允许资源接口增加临时HP。
- OpenAPI生成一度重新格式化；随后基于入口字节副本仅插入新route/schema及两处
  CONFIRM_DAMAGE枚举，解析结果相等，保留全部入口未提交契约内容。

## 最终实际验收与结束状态

- `../.tools/a08-test.ps1 -Full -Log a08-backend-full.log`：22:44–49完整179unit +
  234真实PG/HTTP/WS IT，0失败/错误/跳过；含修正夹具后的全部13项AttackPreviewsIT。
  新自动确认和应用两阶段各验证立即/延迟回执提交失败回滚、同key并发单效和
  变body冲突。状态/死亡/陈旧源与关联行动失败均比较完整相关表及历史保持不变。
- `npm test -- --maxWorkers=2`：332通过；最终`npm run build`再次通过，
  `a08-build-final.log`。全程保留原业务期望和timeout/retry。
- `node scripts/check-contracts.mjs`：134操作；独立解析入口备份逐项比较，除新
  damage-effect路由/DamageEffect schema和命令/事件的CONFIRM_DAMAGE枚举外，
  所有原路径、组件和顶层策略相等；没有迁移文件变更。
- `npx playwright test`：完整62生产Chrome通过（3.1m），零retry，
  `a08-browser-full.log`。加载JAR与最终构建JAR SHA-256相等。
  已查看`frontend/test-results/damage-effects-*/a08-dm-plan.png`及`a08-dm-390.png`。
- 按PID/命令行核对后真正停止Java18125、网页18126、PG18460，再启动同JAR/数据。
  新进程证据`a08-restarted-processes.json`。`check-damage-effects-restart.mjs verify`
  新会话精确比较浏览器生成的全部卡/历史、骰、行动、方案、原回执与更正；
  `resume`应用真正持久的待确认自动扣减，一次生效、原证据/hash/骰子保持、
  资源/状态保持，所有旧回执不覆盖新值；`a08-restart-verify.log`、
  `a08-restart-resume.log`、`a08-browser-evidence.json.resumed.json`。
- 最终再次按身份停止三服务，18460/18125/18126无监听、数据保留；
  `a08-stopped.log`。清除全部DATABASE/TEST_DATABASE环境后
  `../.tools/a08-test.ps1 -Offline -Log a08-offline-unit.log`：22:53再次179通过，
  0失败/错误/跳过；不依赖运行中的数据库。
- 入口83份文件全部保留，其中73份仍逐字节相等；其余仅明确的共享契约/API/
  检查器及交接/CHANGELOG更新。五份交接历史（含云部署补充）逐字节作为完整后缀
  保留。暂存区为空、HEAD不变。根`git diff --check`仍报告入口已有的
  continuation/verification尾部空行；未为消警告改写历史，也不称根检查全绿。

原分支/草稿PR #1/HEAD保持，全部入口文件和两份云部署补充保留；无暂存/提交/
推送/合并/发布，没有当前未提交代码的精确新HEAD远端CI。没有操作dnd-roll-local
及其数据、旧fence或恢复目标。本包不重跑完整P09独立恢复或容量，不冒称M3关闭；
云端、物理异机、真实手机后置。下一选择具体效果先查证再契约/实际RED，
公开自动应用策略另包；不能直接移除0HP/条件回退门。
