# A24c 嵌套证据图来源核对（2026-10-31）

本切片不新增规则裁定。沿用A08–A23来源门，不修改游戏服务、效果、JDBC或迁移。
实际适配依据是CharacterService.Card/History、DiceService.Roll、CombatService.Action/Event、AttackPreviewService.Preview/Evidence/Event及A16–A23 State/Attempt/Entry/Review records。

JdbcCharacterStore中当前卡由关系列、controllers、sheet重组；历史的before_state/after_state保存Card。reader读取两种历史快照，不只当前卡。JdbcDiceStore、JdbcCombatStore与JdbcAttackPreviewStore写入整个data；A16/A19及六种日志写入snapshot。V27–V34复合FK定义角色版本之外的两本账本。

| 来源字段 | 精确含义 |
| --- | --- |
| A16 State.id/latest.target.id | target别名；不是独立状态UUID |
| A16/A19 latest.id/latest.roll.id | 专用骰别名；两种尝试不能共用namespace |
| A19 latest.originVersion / SQL origin_version | A18 stabilization_damage_history结果版本 |
| A20/A21 Entry.originVersion | shared stabilization_history结果版本；Review.originVersion为其前一版本 |
| A22 Entry.damageVersion | restabilization_damage_history（A21）版本 |
| A23 Entry.damageVersion | stabilization_damage_history（A18）版本 |
| timeline_entry.id | 唯一message/roll/action owner ID硬alias |

专用骰Roll自身不保存目标/独立账本字段。真实绑定存在DiceService.plannedFirstAid/plannedStabilization/plannedRestabilization的源dice_command_receipt fingerprint。reader只读匹配A15_FIRST_AID、A16_STABILIZATION、A19_RESTABILIZATION域及当时版本、目标、helper/calculation tuple；回执不输出、不复制、不当目标授权。

canonical记录由既有MonsterDeathEffect、KnockoutEffect、DaggerKnockoutEffect、StabilizationEffect、RestabilizationEffect生成原文Condition后精确比较，包括固定Source、名称、说明和时限。A19仍引用原A16 unconscious的旧尝试，不重写后缀。使用真实骰total，不用成功值替代失败值。

只遍历明确Java record字段及List；只对已知Card/Roll/Condition/Attempt/Entry路径提取身份，Map/local key/free text/Source/content不扫描UUID。严格反序列化拒绝未知字段、缺record字段和primitive null。报告仅固定路径/UUID/版本/计数/固定错误代码。

此图不是JSON重绑定或writer。其他运行时、文件、权限、模组、hash重建与新团续办仍待，所有报告BLOCKED/executable=false。
