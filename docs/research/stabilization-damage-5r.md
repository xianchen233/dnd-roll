# A18 原A16稳定后首次受伤：5R来源与支持矩阵

2026-10-29。dnd-rule-lookup：A库《玩家手册2024》“稳定”只定位到生命值降至0点，
未回读或复制商业正文；B库全文回读“生命值降至0点”“临时生命值”。交叉核验
[官方SRD5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18：Damage at 0 Hit Points、Stabilizing a Character、Temporary Hit Points。
5R结论不推定适用于2014、扩展或自制。

摘要：0HP时受到正伤害记一次死豁失败，来自重击则两次；稳定生物受伤后失去稳定，
仍昏迷并再次面临死豁。临时生命先扣除；临时生命是HP损失缓冲，不是免疫伤害。
因此本配置以DM裁定的实际受伤事件为触发，不以实际HP损失大于0作为失稳前提。
原0HP昏迷记录保留，不自动醒来或投死豁。伤害达到上限的死亡边界留给人工。

| 情形 | 支持边界（软件限制与规则摘要分别识别） |
| --- | --- |
| 最新APPLIED A16尝试，完整当前目标卡恰等于原应用结果 | 首次有界受伤候选；原规范两记录、HP0/maxHP>0、死豁0/0、无导入审核/修正器/覆盖 |
| DM已裁定最终伤害amount、是否来自重击critical、明确reviewed | amount整数0–220000；不掷伤害、不重做抗性/免疫/减伤、不因critical再倍增最终伤害 |
| 0 < amount < maxHP | HP仍0，先消耗min(tempHP,amount)；移除原稳定记录，保留原0HP昏迷；死豁失败为critical?2:1，成功仍0 |
| tempHP吸收全部正伤害 | 仍发生受伤事件、失稳并记失败；tempHP按金额减少，HP仍0，不把缓冲当作伤害免疫 |
| amount=0（包括已裁定免疫/减伤后为0） | MANUAL_REQUIRED/无伤害，不提供应用值，不消耗临时生命、不失稳 |
| amount>=maxHP | 人工，即使tempHP可能吸收，也不在本切片推断死亡或处理相关歧义 |
| 已失稳、HEALED、ROLLED、其他来源、后续目标改动/额外注记 | 人工或版本冲突；不执行重复受伤、第三次失败死亡、再次稳定/失稳后的治疗 |
| 施救者后续编辑、资源与原服务器医药骰 | 不改动且不依赖当前施救者版本；校验原保存证据，保留不可变来源 |
| 自动重击判断/距离/倒地/专注、伤害骰/资源、死豁投掷/自然20/1d4恢复/复活 | 未实现，DM另行处理；通用DamageEffect的旧0HP门保持 |

配置 `5R-SRD-5.2.1-STABILIZATION-DAMAGE-V1`。本文件为中文摘要与软件矩阵。
This work includes material from the System Reference Document 5.2.1 by Wizards
of the Coast LLC, available at https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文摘要及保守有界实现，不复制规则全文。
