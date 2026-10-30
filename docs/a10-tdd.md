# A10：普通怪物死亡记录的实际 TDD

2026-10-26（Asia/Shanghai，客户端日期；执行日志宿主时间已跨到27日）。
先按 AGENTS 读取最新交接、验收、设计、计划、CONTEXT、契约及 M3 余项并检查 Git。
A07/A08/A09 不重复；原分支 `feat/accounts-and-invitations`，本地和 ls-remote
均为 `c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。GitHub 连接器首次网络失败，
随后只读复核 PR #1 为 open/draft、同一 HEAD。未改 PR 元数据。
107份入口未提交文件逐字节备份在仓库外 `D:/workspace/dnd/.tools/a10-entry/`，
含 SHA-256 清单；保留云补充、所有历史及原数据。

先[5R来源与支持矩阵](research/monster-death-5r.md)，再
[契约](../contracts/monster-death-effects.md)、实际失败、最小实现与边界检查。
独立 MONSTER-DEATH-V1 要求 DM 明确普通怪物即死处理及无未处理例外；从原伤害
推导 HP=0、tempHP=0，并在独立 APPLY 中同事务记录死亡注记。原 A08/A09 的0HP
回退与确认语义保持。死亡注记复用状态容器，不声称是5R condition，不执行死者
全部规则或自动删棋子/退出先攻/禁止行动；复活和标记移除由DM独立处理。无迁移。

## 已实际执行

日志均在 `D:/workspace/dnd/.tools/`，不入库，不输出凭据。

| 检查 | 实际结果 |
| --- | --- |
| 契约 RED | `node scripts/check-contracts.mjs` 缺 monster-death-effect 路由，a10-contract-red.log |
| 前端 RED | `npm test -- --maxWorkers=2 src/attack-previews.test.tsx`：修正夹具后1新增失败/15旧通过，缺“0生命值处理方式”，a10-frontend-red-fixed.log |
| HTTP RED | `../.tools/a10-test.ps1 -Prepare -Log a10-http-red.log`：原182单测通过，新增1 HTTP期望200实际404，0错误/跳过 |
| 初次实现 | 前端16聚焦通过/构建通过，契约138通过；HTTP业务已走到应用、历史与更正重试，最后撤权夹具触发FK错误，不能记整轮通过 |
| 边界 GREEN | `../.tools/a10-test.ps1 -Log a10-http-boundaries.log`：185单测+5真实PG/HTTP通过，立即/延迟提交失败全行回滚、同key并发、双DM竞争、双预览仅单效、权限/原版本/hash/严格字段/人工回退 |
| 前端全量 | `npm test -- --maxWorkers=2`：37文件342通过，a10-frontend-full.log；生产构建a10-build-final.log通过，大chunk提示保留 |
| 契约最终 | a10-contract-final.log：138操作，严格请求/候选/人工联合/缓存/角色及generic拒绝新kind；独立结构比较只增加2路径、3schema和1事件kind，旧路径/其他schema/根策略相等 |

## 准备与测试夹具失败

- 首次生成测试的 Python 默认GBK读取UTF-8失败，尚未产生新测试；同批前端旧15
  通过及旧136契约通过不是 A10 GREEN。改用 `python -X utf8` 后才生成实际测试。
- 初次前端新增用例漏 `setup()`，ReferenceError 不是产品RED；补夹具后实际缺
  中文combobox失败，记录在 a10-frontend-red-fixed.log。
- a10-http-green.log 中撤权夹具直接删除仍被 character_controller 引用的成员，
  FK拒绝（0断言失败/1错误）。改为降为PLAYER，再完整执行5项通过，没有改业务断言。

## 验收边界

本轮专用 PostgreSQL 18464，测试库/浏览器库均新建，生产验收Java18129/网页18130。
不操作 dnd-roll-local、其数据、旧fence/恢复目标。沿用 A09 验证用受限 JVM 参数，
仅用于本轮验收，不更改产品部署配置，不把它当作容量结论。
完整后端、生产Chrome、真实停启/待确认继续及停库离线复验结果如下。
完整P09独立恢复（需包含A08/A09/A10）/容量/精确交付HEAD日志及云端真机均后置。
没有暂存/提交/推送/合并/发布，不以旧HEAD CI替代未提交代码验证。


## 最终验收结果

- `../.tools/a10-test.ps1 -Full -Log a10-backend-full.log`：185 unit + 246完整真实
  PG/HTTP/WS IT，0失败/错误/跳过，6m26s。原241项与新增5项在同一完整轮次通过。
- `../.tools/a10-browser.ps1`：最终JAR、生产静态前端和真实Chrome下完整64项通过
  （3.4m），零retry。新流程明确选择/审阅、只读不写、私密拒绝、单独确认、丢失
  已提交APPLY响应后原body重试、HP/注记单效、人工修正后旧回执不重建标记均通过。
- 已查看 `.tools/a10-playwright-results/monster-death-effects-expl-d58fd-th-private-durable-evidence/`
  下 a10-dm-plan.png / a10-dm-390.png；布局可读，无横向溢出。390px只属浏览器布局，
  不是真实手机。旧test-results未清理，本轮截图/HTML报告均在新的仓库外目录。
- 运行与最终构建JAR SHA-256相等：
  `557df611cb19cfcfe842b9a0b6bfecba4db0b08fd74ed10563465b0663bb669d`。
- a10-before-restart.log 仅重启前基线核对，不计停启证据。随后 a10-stop.ps1 按
  PID/命令行核对Java/网页/PG身份，真正停止三者并确认18464/18129/18130无监听；
  a10-native-stopped.log、a10-native-stopped-processes.json保留。PG同目录同端口
  pg_ctl启动，a10-restart.ps1重启同JAR/静态网页，a10-restarted-ports.json保留。
- `a10-verify.ps1 -Log a10-restart-verify.log`：新会话逐项比较全部卡/历史、骰、
  行动、预览证据/hash、原回执和保存的待应用确认一致。
  `a10-verify.ps1 -Mode resume -Log a10-restart-resume.log`：继续持久的确认方案，
  HP/tempHP归0与单个死亡注记同一次卡版本写入；精确重试单效，来源/资源/骰保持。
  所有旧回执不覆盖新值，原证据保持；续跑快照在a10-browser-evidence.json.resumed.json。
- 最后 a10-stop.ps1 再按身份停止三服务，a10-stopped.log；独立数据库/素材/捕获
  保留。没有操作其他实例、旧恢复目标或fence。停库并清全部DATABASE/TEST_DATABASE
  环境后运行 `a10-test.ps1 -Offline -Log a10-offline-unit.log`：185通过，0失败/错误/跳过。

没有执行完整P09独立恢复或容量，不以旧HEAD容器/CI绿灯代替当前未提交代码；
M3其余效果与最终恢复/交付、云端和真机仍按 m3-remaining.md 保留。


最终保留核对：107份入口文件全部存在，90份仍逐字节相等，17份为本轮明确的共享
实现/契约/API/测试与交接更新；五份历史交接（含云补充）保持为逐字节后缀。
本轮还修改原干净的CombatService以在同一前后像中原子写入注记，旧调用指纹不变。
根 `git diff --check` 仅有入口已有 continuation/verification 尾部空行告警，未改写
旧历史去消除告警，不冒称根检查全绿。本轮代码/契约/前端文件的聚焦diff-check通过。
最终本地及ls-remote仍c034aa4，暂存区空；未提交、推送、合并、发布或修改PR元数据。
实际新规则来源、支持范围、失败及最终证据均已落盘。下一按 m3-remaining.md 选择
另一具体效果，先核对来源/例外矩阵，再契约及实际RED；不重复A07/A08/A09/A10。
