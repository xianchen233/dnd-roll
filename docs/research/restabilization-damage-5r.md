# A21 原A19再次稳定后首次受伤：5R来源与支持矩阵

2026-10-29。使用dnd-rule-lookup：A库《玩家手册2024》“稳定”仅定位；B库完整
回读“生命值降至0点”“临时生命值”，未读取商业正文。复用本会话A20直接从
官方取得的[SRD5.2.1 PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)，
重新读取p18的Damage at 0 Hit Points、Stabilizing a Character、Temporary Hit Points。
仅适用于5R；不推定2014/扩展/自制的规则与例外。

摘要：稳定的0HP生物受伤后不再稳定，仍昏迷；0HP受伤记一次死豁失败，若伤害
来自重击则两次。临时生命先吸收伤害，并不提供伤害免疫，因此全部被临时生命
吸收的正伤害仍使其失稳。再次稳定已把计数归零，本次从0/0记为0/1或0/2。
重击只用于失败计数；输入已是裁定后的最终伤害，不再次倍增。

| 情形 | 支持矩阵（软件限制比规则窄） |
| --- | --- |
| A19最新APPLIED且共享来源RESTABILIZED，当前完整卡恰为原应用结果 | 重建A16→A18→A19及两次服务器骰，要求完成记录、确切原A18伤害日志、三版本/尝试/hash绑定 |
| 原a19-stable与逐字保留的a16-unconscious，HP0/maxHP>0、死豁0/0 | 同事务只移除a19-stable，保留原昏迷对象；无导入审核/修正器/覆盖 |
| DM已裁定amount整数0–220000、critical与reviewed | 不自动推断重击、抗性/免疫/减伤；不重新掷伤害或消耗资源 |
| 0<amount<maxHP | HP仍0；吸收min(tempHP,amount)，tempHP减去吸收量；死豁成功0、失败普通1或重击2 |
| tempHP吸收全部正伤害 | 仍失稳并记失败；不以HP损失大于0作为触发门 |
| amount=0 | 人工/无伤害，没有可执行值，不移除稳定或消耗tempHP |
| amount>=maxHP | 保守转人工，即使tempHP可能吸收；不在本切片执行死亡 |
| 目标后续任意编辑/权限变化、额外/伪造/改变的记录、非零HP或计数 | 人工；不从名称、自由文字或复制卡推断来源 |
| A19 ROLLED/HEALED/UNSTABLE、原A16稳定对或旧A18结果 | 版本冲突或人工；A17/A18/A20与旧再次稳定入口不放宽 |
| 原或新施救者之后编辑 | 不影响已应用来源；使用保存快照，施救者/原骰/资源保持 |
| 后续连续伤害/再Help/治疗、自动死豁/死亡/复活、1d4小时、完整动作与状态 | 仍人工，旧通用0HP伤害门保持 |

配置`5R-SRD-5.2.1-RESTABILIZATION-DAMAGE-V1`。This work includes material from
the System Reference Document 5.2.1 by Wizards of the Coast LLC, available at
https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文规则摘要与保守软件支持矩阵，未复制规则全文。
