# A03g 便携组合格式：实际执行证据

2026-10-25（Asia/Shanghai），当前未提交切片。沿用
`feat/accounts-and-invitations` / open draft PR #1，HEAD仍为
`c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`；没有提交、推送、合并或发布。

按 AGENTS 先读交接、验收、产品/实施设计、CONTEXT、契约和工作包。fetch与PR
元数据复核确认本地/origin/PR一致；暂存区为空。入口已有A03f实现、测试、CI和
文档改动，两份云部署补充也在工作区；仓库外 `.tools/a03g-entry/` 保存入口副本
及哈希索引。A03f业务实现/测试逐字节保持，云补充和A03f交接原文完整保留。
不重复A03f/A06。入口c034aa4完整CI证据仍是旧HEAD证据，不能代替当前代码。

先写 [稳定引用契约](../contracts/module-portable-operations.md)：manifest v3
使用精确 `{nodes,play,operations}`，操作封套 `{formatVersion:1,definitions}`。
v1/v2及原play严格形状保持。六种typed步骤只引用DM TRIGGER、typed unit/placement
和DM资料节点；无运行时对象版本、账号/权限、回执、骰子或先攻确认。每份最多
100组合、每组合1..20步/64KiB UTF-8，总内容1MiB；所有引用/类型/重复目标和
原P05参战声明边界都重新核验。没有变更5R计算或查证新的游戏规则。

编辑、另存、下载、重导入和定制三方比较保留v3稳定定义。比较按定义nodeId，
重排定义不制造变更，步骤次序仍有意义；组装选择后校验完整引用闭包。A03h尚未
实现，因此非空定义开团明确409 MODULE_PORTABLE_BINDING_REQUIRED，两条实况
更新/迁移门都带 `/portable-operations` 阻断，不能静默丢弃定义。界面明确仅备团，
可显式启用v3、创建稳定TRIGGER并通过现有JSON编辑器填写严格步骤；保存不执行。

## 实际 RED / GREEN 与准备错误

完整日志、运行时、凭据和测试数据均在仓库外 `D:/workspace/dnd/.tools/`。

- 离线 `mvn -o -B -ntp -Dtest=ModulePortableTest test`（使用既有本地Maven仓库）：
  9项中5项因当前归档不接受v3而实际error，`a03g-portable-red.log`。其余拒绝性
  测试当时已通过，不能把它们冒称独立行为RED。实现后新9+旧归档10/blueprint4/
  更新10共33项通过，`a03g-portable-green.log`。覆盖严格封套、六种类型、稳定
  引用与wrong-kind、重复身份/跨文字附图目标、100/101组合、20/21步、UTF-8
  超限、P05参战声明、v1/v2形状、确定性v3导出/回读及按身份比较。
- 前端聚焦 `npm test -- --run src/module-lifecycle.test.tsx --maxWorkers=2`：
  新3项缺少入口实际失败、旧7项保持，`a03g-ui-red.log`。实现后10通过，
  `a03g-ui-green.log`；保留旧草稿、读写分离、原body精确重试与撤权/切域测试。
- 契约检查先在manifest v3被明确拒绝处实际失败，`a03g-contract-red.log`。
  首次补schema使用了聚焦检查器不支持的oneOf，导致拒绝性断言没有抛错，
  `a03g-contract-green.log`实际失败，不能称GREEN。改为既有检查器支持的anyOf，
  各分支仍由严格字段和kind常量互斥；125操作全部检查通过，
  `a03g-contract-green-final.log` / `a03g-contract-final.log`。
- 真实HTTP首跑测试代码有一处复合var声明编译错误，`a03g-http-red.log`，仅属
  准备失败。修正后 `-Pintegration -Dtest=ModuleArchiveTest
  -Dit.test=ModuleLifecycleIT#portableDefinition* verify` 实际2项预期200/实际400
  失败，`a03g-http-red-behavior.log`。实现后两项通过，再追加引用拒绝、事务
  回滚/并发和选择更新/恢复包边界，共5项通过，最终包含在43项生命周期IT中。
  后加边界直接GREEN，未捏造各项先有RED。

## 当前代码的完整本地验收

| 检查 | 实际结果和仓库外证据 |
| --- | --- |
| 后端全量 | `a03g-runtime.ps1 -TestBackend`，171unit+207真实PG/HTTP/WS/迁移，0失败/错误/跳过；生命周期原38+新5=43项；`a03g-backend-full.log` |
| 前端/构建 | 最终 `npm test -- --maxWorkers=2` 306通过；TypeScript/Vite生产构建通过，保留既有大chunk提示；`a03g-frontend-final.log` / `a03g-build-final.log` |
| 契约兼容 | 125操作；独立按入口解析树比较，只允许新增v3 schema、相应document union引用、manifest枚举3和新策略；原全部路径/旧schema/策略（包括A03f步骤）保持，V1–V25无改动；`a03g-review.log` |
| 生产Chrome | 新聚焦1通过，最终完整58通过，零重试；`a03g-browser-focused.log` / `a03g-browser.log`。真实提交后故意丢响应，原body重试；ZIP实际下载SHA、独立重导入、旧战役完整快照保持 |
| 视觉 | 已查看桌面及390px截图，保留 `.tools/a03g-portable-{desktop,mobile-layout}.png`；不是物理手机验收 |
| 实际重启 | PG55440/新库/Java18099/web18100真实停止重启；新会话三份HTTP/浏览器便携捕获、原ZIP/基线/副本/回执与旧A03f/A03e/A05/A06捕获逐字段保持，最终JAR哈希相同；`a03g-native-restart.log` |
| 完整独立恢复 | migration归档后停止本轮来源，在独立PG55441/新库/素材/Java18101/web18102离线恢复；全部数据库行/迁移/引用/文件匹配，三份便携及所有旧捕获通过；`a03g-restore.log` |
| 恢复后继续备团 | 按恢复后的原当前版本修改START意图理由、另存v3模板2.0.0；两个新请求及旧回执重试都单效，不覆盖新编辑、稳定节点/原基线保持、先攻/战斗不变化；实际下载并预览新包完整匹配；`a03g-restore-resume.log`，新增回执/详情/ZIP哈希在 `a03g-http-capture.json.resumed.json`。此后原HTTP捕获代表继续编辑前快照，不能再把它当作目标当前快照 |
| 停库离线复验 | 按CommandLine/数据目录核对后停止全部本轮验收服务，六独立端口无监听；清除数据库环境后 `mvn -o -B -ntp test` 再次171unit通过；`a03g-unit-pg-stopped.log` / `a03g-stopped.log` |

5项新HTTP测试实际覆盖：原件/基线独立、v1来源显式升级、v3另存与下载重导入、
原编辑/另存回执、当前DM先于旧回执、非空定义开团无任何部分战役/文件、两条
实况门预览及直接写入均阻断、运行时/悬空字段拒绝且逐表/文件无变化、即时与
延迟提交失败完全回滚、并发相同请求单效/双DM同版本单胜、三方冲突按稳定路径
明确采用，以及恢复ZIP保留更新前v3完整步骤。库导入/文件/权限旧回归全部保留。

CI仅增补便携HTTP/浏览器捕获、原容器重启/独立卷恢复比较和恢复后继续备团；
A03f及所有更早断言保留。没有新提交，所以没有本轮远端CI或Docker执行结论，
也未重新声称容量测量。数据、归档、新来源fence保留，旧fence/恢复目标未操作。
dnd-roll-local及其数据完全未启停、重置或用于测试；云端/真实手机继续后置。

A03g本地有界切片通过，M3整体不关闭。下一包A03h先固定每团独立操作ID、稳定
引用解析/当前版本显式绑定、PREPARE留DRAFT/START精确确认、选择更新/删除与
实况影响/恢复契约，再实际TDD；不能直接去掉本轮阻断或导出运行时版本充作绑定。
