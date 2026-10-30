# A09：短弓箭支计数器与伤害联动矩阵

2026-10-26（Asia/Shanghai）。先用dnd-rule-lookup查证，再契约与实际RED。
基线 **5R / SRD 5.2.1**。A索引仅`弹药 --book 玩家手册2024 --locate`，未读商业正文。
B索引定位后完整回读`玩家手册2024/装备/词条.htm`、`玩家手册2024/装备/武器.htm`、
`玩家手册2024/进行游戏/攻击检定.htm`；根为
`D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/`。
另核对[官方SRD 5.2.1 PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)：
Ammunition（印刷p89），Weapons表Shortbow行（p91），Attack Roll Abilities（p7）。
短弓为使用箭矢的双手远程武器；每次攻击消耗一支弹药，因此失手也消耗。
本切片中文快照与英文5.2.1未发现相关差异。

| 项目 | 来源与版本 | 本包支持/边界 |
| --- | --- | --- |
| 普通短弓单次远程攻击 | SRD 5.2.1 Weapons p91；Ammunition p89 | 精确配置`5R-SRD-5.2.1-SHORTBOW-AMMO-V1`；只接服务器A05保存的受信内置短弓`1fd208e1-f443-5090-b83c-363a460245aa`，DEX，未用精通 |
| 一次攻击一支箭 | Ammunition p89，每次攻击均消耗 | DM显式映射一个来源角色的CUSTOM资源计数器，并确认它代表本次可用的普通箭矢；服务器固定减1，不按命中/重击/伤害数改变数量 |
| 目标生命扣减 | 沿用A08矩阵 | 使用已保存的最终伤害，临时HP优先；源资源与目标HP/tempHp在同一APPLY事务、同一P06历史中落地；失手/免疫伤害0仍耗一箭 |
| 计数器身份 | 本团记账映射，不是新增规则来源 | 资源名称与source标签均不能证明弹药性质；不自动搜索“箭”、不默认选择、不改物品数量，不同时扣库存和资源 |
| 资源不足、缺失、非CUSTOM，或映射未审阅 | 软件前提与Ammunition的拥有弹药要求 | 整体人工回退，无部分HP/资源结果；预览和确认均不消耗，不允许负值 |
| 源已有状态、死亡记录或0HP，源目标相同 | 本包保守软件边界 | 整体人工回退；不默默合并身份或绕过A08目标0HP/状态/死亡门 |
| 双手、射程、目标合法性、专注/反应/受伤触发等 | 各自5R规则及具体能力 | DM必须明确审阅无未处理例外；不从自由文字识别、不自动执行状态/精通或动作经济 |
| 战后回收 | Ammunition p89，战后花1分钟可回收半数向下取整 | 已核对但未自动化；不把确认/取消/更正当作自动回收，不自动补回箭支 |
| 其他弓弩、投掷、魔法弹药、不耗弹药特性/扩展、其他资源 | 未逐项核验 | 人工；官方扩展/第三方/2014/自制自动化新增数仍为0，不泛化为所有远程武器 |

短弓攻击的骰子预览不是“攻击已经执行”。现有A05/A08语义不变；本包通过新的明确
确认入口选择含箭支的方案。暗骰仍先交DM，公开摘要独立。更正仍由DM根据当前版本
给出明确最终值，不盲目逆转旧消耗；公开自动应用开关另包。

本文件为中文概括及软件边界，非完整规则转载。
This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
