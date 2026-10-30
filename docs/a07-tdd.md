# A07：可见棋子范围提示的实际 TDD 与本地验收

2026-10-26（Asia/Shanghai）。从 `m3-remaining.md` 的范围提示缺口选择此独立包。
A03h 和退役绑定回执补审不重做。原分支 `feat/accounts-and-invitations`，入口
fetch 后本地/origin/草稿 PR #1 均 `c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。
PR 为 open/draft；只读核对并沿用。没有暂存、提交、推送、合并、发布或改PR元数据。
入口70份改动逐字节备份至仓库外 `.tools/a07-entry/`，含哈希清单。

先用 dnd-rule-lookup 的 A locate-only 与 B SRD 全页回读，并核对官方 SRD 5.2.1；
[支持矩阵及来源](research/range-hints-5r.md) 先于 [契约](../contracts/range-hints.md)
与实现。只计算单格、同平面、经明确审阅的5尺方格距离，并比较声明的触及或普通/
最大射程。超出支持范围保留人工裁定；扩展自动化条目为0。

## 实际 RED → GREEN

日志均在 `D:/workspace/dnd/.tools/`，未入库；不打印凭据。

| 实际顺序 | 命令/日志与结果 |
| --- | --- |
| HTTP RED，00:30–00:31 | `a07-test.ps1 -Log a07-http-red.log`：171旧unit通过；6真实HTTP测试实际失败，缺路由200→404及缺私密响应头，不是编译错误 |
| 前端 RED，00:31 | `npm test -- --maxWorkers=2 src/range-hints.test.tsx`：7失败；6缺中文控件、1缺API方法，日志 `a07-frontend-red.log` |
| 契约 RED，00:32 | `node scripts/check-contracts.mjs`：`A07 range hint route missing`，`a07-contract-red.log` |
| HTTP GREEN，00:33 | 同聚焦命令：171unit +6真实HTTP通过，`a07-http-green.log` |
| 前端 GREEN，00:33 | 同聚焦命令7通过，`a07-frontend-green.log`；随后补同域旧请求迟到401清除新结果，第8项也通过（`a07-frontend-extra.log`） |
| 最终后端全量，00:40–00:46 | `a07r-test.ps1 -Prepare -Full -Log a07r-backend-full.log`：175unit +230完整真实PG/HTTP/WS IT，零失败/错误/跳过；包括新4unit和7HTTP边界 |
| 最终前端全量 | `npm test -- --maxWorkers=2`：37文件327通过，`a07-frontend-full-final.log`；生产构建通过，既有大chunk提示保留 |
| 最终契约 | `node scripts/check-contracts.mjs`：133 HTTP操作通过，`a07-contract-green.log`；旧路径/schema/components/策略逐项结构比较完全一致，只新增1路由、2schema |
| 生产 Chrome | 新流程先单独1通过；第一次全量60通过/1测试监测误判失败，修正测试后完整61通过，零retry，`a07-browser-full-final.log` |
| 实际重启 | Java18115、网页18116、PG55450均实际停止再启，新会话 `check-range-restart.mjs verify` 通过；`a07-native-restart.log`、`a07-restart-verify.log`及`a07-range-evidence.json` |
| 真正停库后的离线复验 | 核对进程命令行后停止两套独立PG及Java/网页，清除所有DATABASE/TEST_DATABASE环境，`a07r-test.ps1 -Offline -Log a07-offline-unit.log`：175通过，零失败/错误/跳过 |

服务器只读范围查询持有既有战役锁：当前成员、两个当前场景可见端点先于版本与
校准；无需移动控制、角色卡或回合授权。当前场景身份/版本、两个棋子版本必须
匹配。隐藏/缺失/跨场景/跨团端点统一404；撤权403、陈旧409、严格输入400与
匿名/无CSRF响应均保持私密响应头。公开可见端点测量不等于规则上的可攻击。
HTTP逐次比较public全部表的完整行，重复预览与人工回退都无状态变更。
没有新迁移、历史、回执、骰子、资源或广播；未改A04/A05执行语义。

前端只列当前过滤场景中的端点。编辑、场景/棋子/版本/角色/API/战役变更清掉
结果并隔离晚到成功；同域旧请求迟到401/403仍撤销新提示，旧域拒绝不污染新域。
404/409提示刷新，不把旧结果显示成当前。没有执行/应用按钮。
真实浏览器使用两可见且无控制权的棋子，确认两次查询只有本站两条预览POST、
场景/骰子/战斗完全不变；玩家不能列出或猜测隐藏目标，DM可测私密端点。
实际DM隐藏后，玩家通过现有实时刷新清掉目标和旧提示。已查看桌面/390px截图，
文件位于忽略的 `frontend/test-results/range-hints-*/a07-*.png`；并非真实手机。

## 环境与测试辅助错误（不计产品 RED）

- 初次独立PG55448绑定报Permission denied；保留初始化数据，改55450成功。
- 第一次完整后端229项中125项为连接阶段 `BindException: Address already in use`，
  零业务断言失败，日志 `a07-backend-full.log` 保留。复验用新独立PG18448/新库及
  `-Djava.net.preferIPv4Stack=true`，包含后补第7HTTP的完整230项全绿。
  两项环境变化同时使用，不推定其中一项是已经证明的唯一根因。
- 首次全量浏览器监测了所有POST，误计外部注入脚本的 `dp.onGetFields` 请求。
  改为监测本站所有 `/api/` POST；仍精确断言只有两条范围预览，不排除任何本站
  状态写入。未改产品实现、业务期望、timeout或retry。首次日志/trace另存
  `a07-browser-full.log` / `a07-browser-first-failure/`。
- 重启辅助脚本整段输出重定向在PG成功启动后挂起。按命令行核对并停止该辅助
  shell；`a07-start-resume.ps1`继续启动相同应用并实际新会话核验通过。不是服务
  重启失败或模拟重启；重启前后加载JAR与最终构建JAR逐字节相同。
- 探查个别文件时的工作目录/文件名错误已修正，不算RED。根 `git diff --check`
  入口已有continuation/verification空行EOF告警，保留原文，没有为消警告覆盖交接。

## 结束状态与未完成门槛

PG55450/18448、Java18115、网页18116均已停止，无监听；数据/证据保留。没有
启停、重置、读写 `dnd-roll-local` 或其数据，没有操作旧fence/恢复目标。
两份云部署补充及此前全部未提交A03f/g/h业务代码/测试保持，交接仅在前面加
本轮入口。没有新提交的精确HEAD远端CI结论，不借用c034aa4绿灯。

A07此只读单格范围切片已本地验收；M3整体仍未关闭。自动效果、公开自动应用
策略、复杂范围/多格/3D/范围效果继续保留。下一按剩余清单推进A04/A05效果扩展：
先逐项确定5R/扩展来源与支持矩阵，再契约和实际RED；公开自动应用另立版本化
策略及私密边界。M3最终完整独立恢复/继续操作、容量、精确交付HEAD远端日志仍
保留；本轮没有重跑完整P09独立恢复，不把原A03h证据冒称为本轮新结果。
云端、物理异机与真实手机验证后置；提交/推送需用户另行授权。
