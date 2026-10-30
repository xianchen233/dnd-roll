# A24b 类型化战役身份建议：TDD与验收记录（2026-10-31，未提交）

本记录包含静态盘点、契约、实际RED、定向复验及主线最终本地验收。
最终全量回归、原生CLI、停库与入口保护审计的实际结果见末尾补充。

## 入口与范围

按AGENTS读取A24a交接/验收、设计/计划、CONTEXT与契约后推进。入口307份未提交
文件已备份到仓库外`.tools/a24b-entry`及manifest；沿用原分支
`feat/accounts-and-invitations`、HEAD `c034aa4`与草稿PR #1。无提交/推送/合并/
发布授权，不操作dnd-roll-local及数据或任何旧fence。A07–A23不重做。

本轮是恢复规划，不新增5R游戏效果。先完成
[字段来源与namespace盘点](research/campaign-remap-fields.md)，核对真实records、
JDBC、V27–V34复合FK与76表清单，再写
[独立契约](../contracts/campaign-remap-plan.md)，之后实际失败测试与最小实现。
两个少量子agent承担字段研究及只读风险复核，主线负责共享契约、代码、测试与集成；
本TDD文档由字段研究agent根据实际日志记录，最终结果由主线补充。

新增CampaignRemapPlan与离线CampaignRemapPlanCommand。规划前在同一个只读
repeatable-read事务中通过A24a完整档案/schema/76表行摘要/已提交资产核验。
各SQL列均声明策略；源团scope以campaign_id或明确父表关联定义，角色/笔记/RP
历史不能因没有campaign_id列而跳过。全局账号、schema元数据与全局账号回执不复制；
战役回执统计但源端保留。只读取SQL身份、声明版本及覆盖计数，不遍历私有JSON。

15个明确运行时table namespace分配身份建议，根campaign映到显式目标；其余建议
绑定seed、档案database.dump摘要、源/目标campaign、namespace和原ID，固定domain
`dnd-roll-remap-proposal-v1`。同UUID在不同namespace通常分开；重复/缺失身份、
跨namespace引用、全库已占用UUID冲突、漏表/列/scope与身份行覆盖异常均阻断。
SQL版本原样列出，不重置成0；seq/ordinal需后续重新分配，不假称已执行。

时间线有明确硬alias：timeline_entry.id等于message_id/roll_id/action_id对应owner
身份，并复用该owner的目标ID。其来源是TimelineService按row.id投影和原触发器；
缺失/重复/错owner/不相等alias必须阻断。它不是可随意忽略的UUID碰巧相等。

所有报告始终`status=BLOCKED`、`executable=false`，保留writer、hash再绑定和新目标
权限审核阻断项。已占用JSON/module/assets范围另列gap。没有新HTTP/迁移/前端行为，
没有新战役、文件副本、源回执或fence写入。报告CREATE_NEW，不替换旧报告；CLI仅
字面REVIEW_ONLY，写出建议退出2，失败退出1，没有成功退出0。身份/引用/已占用UUID
分别有10000/100000/1000000安全上限，不能当作M3容量通过。

## 实际行为RED

- `.tools/a24b-unit-red.log`：276unit，6实际断言失败，0错误/跳过。初始stub错将
  规划标READY，未隔离namespace/绑定seed与档案、未拒绝身份/引用/碰撞或覆盖缺口。
  失败发生在产品断言，不是缺工具、编译错误或数据库未启动。
- `.tools/a24b-it-red3.log`：276unit先全部通过；随后4个真实PG规划IT均实际断言
  失败，0错误/跳过。入口stub抛`Planning not implemented`，被assertDoesNotThrow
  确认无法返回合法的只读BLOCKED建议。不是SQL夹具准备失败。
- `.tools/a24b-alias-red.log`：277unit中1实际断言失败，0错误/跳过；timeline独立
  随机建议与其骰owner建议不一致，复现“两个namespace本应同一目标”的投影错误。
  修复仅对明确timeline alias共用目标，不把所有相同UUID强制合并。

当前新增unit覆盖namespace/当前版本、seed与档案/顺序确定性、全库UUID碰撞、
缺失/错namespace引用、重复/非法版本/同目标、漏表/列与结果不可变、timeline alias，
及假scope/身份行覆盖/错列namespace声明。新增8项相对入口270项，当前预期278；
这只是测试清单，不替代实际最终执行结果。

4个新增真实PG IT针对：两个团相同UUID的namespace隔离、非campaign_id历史的父关联
scope、源回执排除、全部SQL行保持及报告隐私；目标已存在/真实跨团FK拒绝；档案行
变化/未知列在建议前拒绝；CREATE_NEW保护和不存在源团不得假成功。测试用新独立PG
18512、新目录/数据库与合成数据，不启动A24a或dnd-roll-local服务，不读旧团数据。

## 准备错误、真实数据库证据与定向复验

- `.tools/a24b-it-red.log`首次夹具向campaign_note.ordinal显式插入，实际列为
  generated always；`.tools/a24b-it-red2.log`用空JSON构造RP消息，违反真实
  rp_message_kind_identity约束。修正夹具后才得到it-red3的实际行为失败。
  这两次属于准备错误，不能记作产品RED。
- `.tools/a24b-focused-green2.log`：278unit中0失败、1错误；既有
  PrivateAssetFilesTest.atomicPublicationAndNewAdapterPreserveOriginalBytesWithoutTemporaryFiles
  在JUnit临时目录清理阶段报`Failed to close extension context`。主线保留日志并重跑；
  不能将该次执行宣称通过，也不把它算成规划行为RED。
- `.tools/a24b-focused-green3.log`暴露先前错误假设：对campaign.current_scene_id
  写入另一团scene并非“SQL合法”，真实PG以campaign_current_scene_fk拒绝。
  V3已定义`(campaign.id,current_scene_id) -> scene(campaign_id,id)`复合FK，且
  DEFERRABLE INITIALLY DEFERRED。测试改为断言SQL拒绝，保留规划自身引用闭包核验；
  没有为了制造跨团夹具而放松或删除旧FK。该次4IT中1错误不是成功结果。
- 最新定向复验日志为`.tools/a24b-focused-green4.log`，主线需核对最终摘要并在
  下方追加准确结果。全量后端、原生CLI的退出码/只读/新报告路径、最终停库离线、
  固定jar与入口审计尚不在本文当前最终结论中。

## 尚未实现

SQL身份建议与76表列策略不是完整运行时重映射。嵌套JSON来源图、A15/A16/A19专用
骰/attempt别名、canonical注记、角色与双账本历史版本、原方案hash重建、固定/定制
内容身份、module图、权限和独立资产文件复制、隔离事务writer仍待。源回执不能复制
到新目标认领，旧确认不能当新目标授权。A24a CAMPAIGN_REMAP保持BLOCKED。

独立新团可续办、完整P09/M3、容量及精确交付HEAD日志仍未完成；云端和物理真机后置。
本轮不重跑或改写A07–A23效果，不把只读建议写成“已恢复新战役”。

## 主线最终实际验收补充

最终实际结果（主线补充）：

- `.tools/a24b-focused-green4.log`278unit与4规划专项真实PG IT通过；
  `.tools/a24b-backend-full.log`278unit、373完整真实PG/HTTP/WS IT零失败/错误/跳过，
  包含A24a恢复审计4项与新增规划4项。177HTTP契约检查通过，无新HTTP操作。
- `.tools/a24b-native-plan-summary.json`：保留A24a原生档案字节，副本RestoreCommand
  恢复到本轮新PG18512/dnd_a24b_identity及新资产根；16个A08–A23真实历史战役
  分别生成76表SQL提案。各团SQL引用/namespace/身份覆盖/时间线alias均无结构性
  阻断，报告始终BLOCKED/executable=false，真实CLI退出2；不是新团恢复成功。
- A08同seed/目标/档案重复提案逐字节相同；既有报告路径真实CLI退出1、内容保持。
  每组alias复用对应canonical owner目标ID；`.tools/a24b-after-plans-audit.json`
  再次全76表行摘要/54资产VERIFIED，证明提案不改已恢复的完整源数据和文件。
- `.tools/a24b-stopped-pg.json`核新PG PID命令及目录身份后停机；无Java/网页监听
  服务启动，旧服务/数据/fence保持。停库清DB环境`.tools/a24b-final-offline.log`
  278unit再次通过；临时数据库和资产目录均保留。
- 既有frontend/OpenAPI/34次迁移/云补充字节保持，前端435测试/构建及77Chrome沿用
  A24a证据；本轮无UI改动，未重新执行前端/浏览器测试。唯一旧后端编辑是将
  RecoveryAuditCommand的校验提取为共享Connection方法，旧公开行为及原4IT保持。

固定验收jar SHA256：`FC092A77825B8D9747FCB9504473A4C496A6A63D7E294EBE978C7890E8859B99`。
`.tools/a24b-jar-scope.json`另外核实：固定原生验收jar的所有main class与资源，
均与完整回归构建产物逐字节相同，没有验收过程中代码漂移。

307入口文件中297逐字节保持、10项预期增补/重构，新添7文件。审计
`.tools/a24b-preservation-audit.log`核入口备份hash、原历史字节、云补充、旧业务代码/
前端/OpenAPI/迁移、原分支HEAD与空暂存区保持。git diff --check仅入口已有两文档
末尾空行；全部新文件无空白错误。外部PowerShell脚本一次变量后冒号解析错误在
执行前被拒绝，修正后执行；不计产品RED。

下一步A24c应先完成嵌套运行时证据图的类型化提取和一致性校验，再逐步hash/权限/
文件/模组与writer。A24b不是完整新团mapper；M3/P09总联验、容量、精确交付HEAD
日志及云端/真机仍待。本切片已形成适合新会话的完整仓库交接。
