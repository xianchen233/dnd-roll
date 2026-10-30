## A24b补充（2026-10-31）

新增[类型化字段盘点](research/campaign-remap-fields.md)与[SQL身份建议契约](../contracts/campaign-remap-plan.md)。
76表范围/列策略、namespace与时间线canonical alias、所选团SQL引用闭包、本轮
原生16团只读提案通过；所有报告BLOCKED/executable=false，深JSON/版本链/注记/
hash/权限/文件/模组/writer仍未实现，M3未关闭。详见[a24b-tdd.md](a24b-tdd.md)。
以下A24a原始矩阵与证据保留。

# M3 恢复清单与 ID 重映射矩阵（A24a，2026-10-30）

仓库实现核对：P09 PostgresBackup 动态枚举全部 public 表、逐表行数/摘要与所有
已提交图像，RestoreCommand 在独立空库/新资产根恢复并核对，ID原样保留。
ModuleCampaignProvisioner.createRecovery 则创建新战役/角色/棋子身份，只复制定义
与当前卡；A03h契约明确排除骰、战斗、执行历史和旧回执。二者不是同一恢复保证。
本次原生P09档案/RestoreCommand与76表审计已实际通过，16组A08–A23新会话核对、
独立续办和真停启后快照比较通过；重映射未实现，不把以往裸dump当验收。

| 范围 | 必须保留的同ID证据 | 新战役ID重映射要求 |
| --- | --- | --- |
| A08 普通伤害 | attack_preview完整快照/事件/方案hash，character_history，combat_action，骰及回执 | 攻击/目标/角色/战役/行动/骰ID、版本、确认hash与应用历史须按类型重绑定 |
| A09 短弓弹药 | 上述证据与资源映射/箭支前后值 | 不能只复制库存或重放扣减；资源键与攻击来源一起核验 |
| A10 怪物死亡 | 死亡注记来源、攻击结果/目标原卡 | 注记中的来源ID与确认计划须重建 |
| A11–A12 击昏 | 武器选择、原攻击/1HP昏迷记录 | 原注记和目标/攻击身份一致，不能按名称认领 |
| A13 击昏治疗 | 治疗私有事件/原记录移除/目标历史 | 原应用结果与后续治疗来源链、hash须保持可核验 |
| A14 短休解除 | 已声明分钟/中断审阅、原来源与回执 | 不得用新时钟自动完成旧休息 |
| A15 急救 | 专用医药骰、独立尝试/失败/确认/原攻击来源 | 专用骰namespace与helper/target版本绑定，不能换成普通骰 |
| A16 普通稳定 | stabilization_state/history/receipt，原医药骰与双方完整快照 | a16-stable/unconscious IDs、目标/施救者/骰与版本须同步重绑定 |
| A17 稳定治疗 | stabilization_healing_history及准确父历史版本 | 源应用、治疗日志前后卡及hash须重建 |
| A18 首次失稳 | stabilization_damage_history，原稳定/唯一昏迷/死豁失败 | 精确父版本、源hash、critical和tempHP结果不可丢失 |
| A19 再次稳定 | restabilization_state/history与原A18版本、新专用骰 | 双账本、旧昏迷/新稳定记录、新骰namespace与原链重绑定 |
| A20 再稳定治疗 | restabilization_healing_history、两本结果历史版本 | 原A16→A18→A19来源、两次原骰与hash重新核验 |
| A21 再稳定后伤害 | restabilization_damage_history、双状态/双历史 | a19-stable删除与原a16-unconscious保留须一致 |
| A22 A21后治疗 | instability_healing_history及A21源/双结果版本 | 原A21hash、整份日志、新方案hash与唯一原昏迷来源重绑定 |
| A23 A18直接治疗 | initial_instability_healing_history及A18源/结果版本，未应用A19证据 | 保留新Help历史并使旧父版本不可继续，不能重掷/误应用 |
| A06/先攻/回合/更正 | movement_*、campaign_initiative/combat、history、actions及回执 | 坐标/回合/行动/版本同一图谱，不因复制而执行 |
| 模组/地图/资料/RP | 包/定制/实例/绑定/退休/迁移历史、图片旧版本、私密可见性 | 稳定内容身份与运行时ID分开，资产独立复制，未知blob不字符串替换 |
| 账号/控制/邀请/回执 | 完整部署权限、actor/request指纹、原ack | 新目标权限需明确复核，源回执不能跨战役认领；不复制环境凭据或会话 |

本轮新增只读 RecoveryInventory/RecoveryAuditCommand，覆盖34次迁移的完整表/schema
清单。未知表/列/约束、档案表遗漏、摘要或资产不符均阻断。CAMPAIGN_REMAP明确
BLOCKED，返回类型化重绑定要求和尚无writer，不会输出“已恢复新战役”。
原生dump/restore会把12表13条CHECK的数组整体text转换重解析为逐元素text转换；
清单仅显式接受已核对的13条等价文本，改变允许值/删CHECK/改列或FK仍阻断。

完整类型化mapper/应用事务与独立战役恢复续办、容量、精确交付HEAD CI、云端/真机
仍未完成；本矩阵和同ID原生恢复验收均不关闭M3。后续先做可审阅的类型化映射计划，
再实际失败测试与writer，不改变A07–A23规则边界。
