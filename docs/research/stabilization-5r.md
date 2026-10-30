## A16b实施状态补充（2026-10-28）

下文来源、5R版本结论及有界支持矩阵继续适用。A16b已实现专用服务器Help医药
检定、失败持久化、双卡快照/最新尝试绑定和显式应用；普通骰/A15骰/预占请求不能
充当证据。客户端不提供total/DC/boundCheck，仍不实现伤害/治疗/计时/死豁或完整
昏迷。详细[契约](../../contracts/stabilization-effects.md)与[实际验证](../a16-tdd.md)。
下文“A16a仅计算/后续适配器”保留原始查证时间语境，当前入口以本段为准。

# A16 普通角色0HP稳定：5R来源与支持矩阵

2026-10-28。A07–A15保持；A16a仅内部纯计算，尚无HTTP、持久化或UI入口。
先按dnd-rule-lookup查A库《玩家手册2024》“稳定”，仅取得定位路径；B库检索并
全文回读“生命值降至0点”“属性检定”，另回读“动作”。本机检索证据存仓库外
`.tools/a16-zero-source.txt`、`a16-check-source.txt`。交叉核对
[官方SRD 5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18的Dropping to 0 Hit Points、Death Saving Throws、Stabilizing a Character。

规则摘要（5R）：Help动作的DC10感知（医药）检定可使0HP生物稳定。稳定时两项
死豁计数归零，仍0HP且昏迷，停止死豁。受到伤害会失去稳定；未治疗者1d4小时后
恢复1HP。临时生命不能代替治疗。医药属性检定比较总值，不采用死豁天然1/20规则。
这些是规则结论；下表更窄的限制属于软件支持边界。

| 版本/来源 | 用途 |
| --- | --- |
| SRD 5.2.1（5R），官方英文；本地SRD5.2中文译本 | 前者交叉核验，后者本地全文查证；有差异时以明确版本的官方原文为准 |
| 《玩家手册2024》商业库 | 仅定位，不读取/复制正文，不随代码交付 |
| 2014版、DM扩展、自制、特殊怪物规则 | 未纳入本配置，不默认为5R兼容 |

| 情形 | 支持矩阵 |
| --- | --- |
| 普通角色，HP0、maxHP>0，死豁成功/失败各0–2，空状态列表 | DM须另行确认仍活着、普通0HP昏迷、未稳定、动作/接触合法及无例外；候选计算 |
| tempHP>=0 | 保持原值，不视为治疗，不解除昏迷 |
| 专用服务器WIS医药总值>=10 | 方案保持HP0/tempHP，将死豁归零，增加来源化“稳定记录”和“0HP昏迷记录” |
| 总值<10或缺少绑定证据 | 人工方案，无可执行数值/记录；失败检定后续须持久保存 |
| 施救者 | 不同角色，HP>0、无状态/死豁/修正器/覆盖；普通WIS医药，支持无熟练/熟练/专精及已保存平直加值 |
| 目标存在任意状态、修正器或覆盖；待导入审核 | 回退；不从自由文字认定昏迷、死亡或稳定，不覆盖已有注记 |
| 三成功、三失败、maxHP0、HP>0、怪物/即死/特殊0HP规则 | 人工；三成功已稳定，不能伪装新急救；DM审阅也不能绕过数值门 |
| 任意普通骰、客户端总值、A15骰、其他目标/版本或旧尝试 | 后续适配器必须拒绝，不把同名医药骰视为专用绑定证据 |
| 优劣势、特性/扩展、Healer、资源消耗、完整动作/距离 | 人工；不自动授予Help优势，不以技能熟练作为急救前提 |
| 新伤害、治疗、1d4小时恢复、死豁投掷及完整昏迷关联执行 | 未实现，DM继续处理；旧0HP伤害门不放开；不自动计时、回血或唤醒 |

配置`5R-SRD-5.2.1-STABILIZATION-V1`。0/0死豁不证明未稳定或仍存活，需明确审阅；
纯计算收到的boundCheck是内部信任边界，未来HTTP不得接受它或total。记录存在不等于
已有完整状态引擎。后续必须先完成专用尝试身份/版本/事务和权限，再向UI开放。

This work includes material taken from the System Reference Document 5.2.1
(“SRD 5.2.1”) by Wizards of the Coast LLC, available at the official link above.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0
International License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
Changes: summarized rules and defined a narrower software support matrix.
