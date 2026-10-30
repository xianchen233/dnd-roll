# A16b 普通角色0HP稳定：适配器/UI/本地验证实际TDD

2026-10-28，未提交。读取最新交接/Git，沿用A16a来源/5R版本和支持矩阵；A07–A15
不重做。原分支feat/accounts-and-invitations、HEAD c034aa48fe1176f8e4e0711cf22031d04b5f6ebd、
草稿PR #1保持。165份入口文件备份在仓库外D:/workspace/dnd/.tools/a16b-entry，
hash清单a16b-entry-manifest.json。以下日志与运行数据均在该.tools，未进入版本控制。

## 契约 → 实际失败 → 实现

- 先补stabilization-effects传输/存储契约和5个OpenAPI operations/7 schemas，再写真实
  HTTP测试。a16b-http-red.log：205unit通过，7集成测试实际404失败，0错误/跳过。
- a16b-http-green-initial.log首次实现后因事件bean缺失而启动错误，改用现有SceneEvents。
  a16b-http-green-2.log为6通过/1测试夹具FK错误；撤权改为降级PLAYER再恢复DM，
  保留受控卡关联。a16b-http-green-3.log：205unit及7真实IT通过。
- a16b-ui-red.log：5界面测试在占位组件实际因缺少操作控件失败；实现中文三阶段及
  未知结果原请求重试。a16b-ui-green.log暴露重复初始读取，修复guard后green-2全通过。
  再补未知写跨revision及较早私有读取403晚到的清除验证，最终7UI+1API测试通过。
- a16b-contract-initial/green.log分别发现README漏路由、权限元数据缺失；补齐并只为
  新专用命令声明独立幂等命名空间。既有operation/schema及既有断言保持。
- 隐私复审补充真实失败测试：a16b-private-history-red.log的9IT中1失败，玩家控制目标
  读取角色历史可见私有应用理由。改为固定安全结果说明，将原理由/操作者/时间写
  DM专用Completion，green.log的205unit+9IT全部通过。不是以鉴权失败掩盖泄漏。

## 行为、安全与迁移

独立角色级稳定状态版本；保存Help审阅、完整双卡快照及专用服务器医药骰，失败
仍持久。只允许最新ROLLED检定和未改双卡预览；方案hash绑定完整状态/输入/计算。
确认时服务器重算，原子写角色及完整历史、两条来源记录、稳定状态/不可变历史/
回执；HP仍0、tempHP/资源/施救者保持。专用骰指纹拒绝普通骰UUID预占。
当前DM与对象存在先于回执，原请求返回原ack，异体/跨路由/跨战役冲突；角色及
尝试版本门、同key与异key并发、立即检定写入失败与延迟应用约束失败的全事务
回滚均实测。无客户端total/boundCheck，CSRF/严格字段/私有no-store保持。

V27仅新增stabilization_state/history/receipt三表。StabilizationMigrationIT从V26
填充旧表，逐表列/行及26个迁移checksum对比后升级；三表初始空且孤立history FK
拒绝。旧迁移文件无修改。整库备份支持这些表，独立战役ID重映射尚未支持。

## 最终实际结果（最终源码，0失败/错误/跳过）

| 检查 | 结果/日志 |
| --- | --- |
| 后端全量 | 205unit + 295真实PG/HTTP/WS IT；a16b-backend-final.log |
| 前端全量 | 43文件377测试；a16b-frontend-final.log；a16b-build-final.log构建通过，现有chunk体积提示保持 |
| 契约 | 154 HTTP operations；a16b-contract-private-final.log |
| 真实Chrome | 70场景全部通过、零retry；a16b-browser.log；两次真实提交后丢响应分别精确重试单效 |
| 视觉检查 | stabilization-desktop.png、stabilization-mobile.png（390px）已查看；非真机 |
| 真正停启 | a16b-first-stopped-processes.json证明Java/网页/PG身份与停端口；新进程/新会话后a16b-after-restart.log核对完整cards/histories/states/rolls/旧回执/保存方案 |
| 独立PG恢复 | pg_dump自原验收库，pg_restore至新进程18478及独立资产目录；a16b-restored-verify/resume.log核对并续办保存方案一次；a16b-source-independent.log证明原库未变 |
| 原库续办 | a16b-resume.log再次独立续办一次，旧回执不覆盖后续卡、骰不增加，tempHP/资源/施救者保持 |
| 停库离线 | 两套Java/网页/PG全按身份停止、端口无监听、数据保留；清DB环境且Maven-o，a16b-final-offline.log再次205unit通过 |

新前端fixture仅本地验收账号/新战役；生产域名/云端/真机未操作。原库PG18476、
Java18141、网页18142；恢复库18478/18143/18144，均已停止。固定验收jar SHA256：
2A0BD84BD777688DBC4C327FD4F8121F41926617370C5B1C21AA607A1954AD5C。

## 剩余与下一步

下一候选是治疗恢复生命后解除原A16稳定/0HP昏迷来源记录；先独立5R来源/版本及
支持矩阵，再契约与实际RED。受伤、1d4小时恢复、自动死豁、完整昏迷仍人工；
旧0HP伤害门不放开。M3未关闭：完整P09清单/独立战役重映射（含A08–A16绑定）、
容量、精确交付HEAD日志仍待办；本轮独立整库PG恢复不替代这些总验收。云端/真机
后置，无暂存/提交/推送/合并/发布，不操作dnd-roll-local及数据/旧fence。
以下A16a原文逐字节保留为历史；其“尚无适配器”不覆盖本段。

最终a16b-audit.log核验165份入口文件和备份hash：150份逐字节未变，15份仅本次
实现接点、契约/交接/变更记录的预期补充；历史文档原字节保留。云补充CI文件
逐字节未改，既有149个OpenAPI operations及全部旧schemas逐项相等，仅新增5个
operations/7个schemas。分支/HEAD及空暂存区保持。git diff --check仅报告入口已有的
continuation/verification尾部空行，保留用户原内容；新增文件无空白错误。

# A16a 普通角色0HP稳定：纯领域切片实际TDD

2026-10-28，未提交。仅完成来源/支持矩阵、内部契约和纯计算器；A16整体尚未完成。
按AGENTS读取最新continuation/verification、设计/计划、CONTEXT、契约并检查Git。
分支feat/accounts-and-invitations、HEAD c034aa48fe1176f8e4e0711cf22031d04b5f6ebd保持。
只读GitHub确认PR #1 open/draft、未合并且head同上；未改PR元数据。
入口160份未提交文件逐字节备份于仓库外`D:/workspace/dnd/.tools/a16-entry`，
hash清单`a16-entry-manifest.json`。云补充和A07–A15代码原样保留，不重做既有切片。

## 来源 → 契约 → 实际RED

- [来源与版本/支持矩阵](research/stabilization-5r.md)：A库只定位，B库全文回读，
  官方SRD5.2.1交叉核对。未读取/复制商业正文。区分Help稳定与A15的1HP击昏解除。
- [内部契约](../contracts/stabilization-effects.md)：纯计算无HTTP和持久化；后续
  专用服务器检定、身份/版本绑定、权限/回执/事务和UI是明确未实现的适配器要求。
- `a16-domain-red.log`：首次Maven未将未加引号的-D参数正确传递，尝试默认m2目录
  失败；这是环境失败，不算行为RED。改用参数数组及现成仓库/Java运行时。
- `a16-domain-red-behavior.log`：8测试实际运行，8失败、0错误/跳过。可编译占位
  实现始终MANUAL_REQUIRED/NOT_IMPLEMENTED；实际失败于缺候选值、回退原因、
  支持的施救者与来源化记录身份等行为。随后才实现纯计算。

## 实现与实际GREEN

`StabilizationEffect`无骰、网络、数据库、时钟或状态写入。内部已审阅且绑定成功的
普通WIS医药总值>=10才给候选；HP保持0、tempHp保持，死豁两项归零，返回两个
确定性attempt-scoped的稳定/0HP昏迷记录，无计时。所有手动回退不返回可执行值。
旧自由文字/击昏/任意状态均不覆盖；三成功、三失败、maxHP0、非零HP、修正器/
覆盖和未审阅/未绑定/同身份均不能进入候选。无熟练也可施救，不自动授予优势。

- `a16-domain-green.log`：8新增测试通过，0失败/错误/跳过。覆盖9种未决死豁组合、
  tempHP保留、DC总值边界、已有记录/未知状态不覆盖、死者/已稳定/非零HP拒绝、
  每个证据门、普通医药与熟练/专精、修正器/覆盖、不可变与确定性、非法输入。
- 修正一处测试覆盖缺口：helper修正器夹具改成HP>0，避免被HP0门提前拒绝；
  未改产品代码。之后运行`a16-offline-full.log`：完整205离线unit通过，
  0失败/错误/跳过，数据库相关环境全部清除，Maven使用-o；没有启动服务或占用端口。
- `a16-contract-regression.log`：既有149 HTTP operations契约检查通过；OpenAPI和
  既有checker字节未改。此检查不声称校验了尚无HTTP实现的A16传输。

未运行本切片PG/HTTP/浏览器/重启/恢复测试，因为尚无适配器；不能沿用A15结果
称A16通过。前端代码未改。无新迁移，无现有行为入口接入计算器，旧0HP门保持。

## 下一可执行步骤（A16b，尚未实现）

按内部契约落实独立角色级专用稳定尝试及最新尝试绑定；先定传输/存储契约和
OpenAPI，再编写并实际运行真实PG/HTTP失败测试：鉴权/CSRF、跨战役/审核卡、
普通骰及A15骰拒绝、目标/施救者版本和计算绑定、普通骰预占、两阶段回滚、
并发、未知响应精确重试和撤权先于旧回执。后接显式中文检定→方案→应用，
生产浏览器和实际停启/保存检定继续；完整独立恢复仍单列。使用新独立端口和
数据目录；不得操作dnd-roll-local或旧验收数据。不能把内部布尔值/总值作为HTTP输入。

M3未关闭，云端/真机/容量/精确交付HEAD日志继续后置；无暂存、提交、推送、
合并或发布。原分支、草稿PR、全部既有改动/云补充保留。

## 最终保留核对

`a16-audit.log`：160份入口文件及备份hash均核验，152份逐字节未改；8份文档的
全部原字节保留（7份加前缀、CHANGELOG只插入新增说明）。CI、云补充、OpenAPI、
既有实现保持。原分支/HEAD和空暂存区核验通过。
`git diff --check`只报告入口已存在的continuation/verification尾部空行；保留原内容，
未为消除此旧提示清理用户改动。A16新增文件不含占位实现。
