# A17 原A16稳定/0HP昏迷记录的治疗解除：5R依据与支持矩阵

2026-10-28。dnd-rule-lookup：A库《玩家手册2024》“稳定”只返回定位；未读取或
外传商业正文。B库回读完整“生命值降至0点”“治疗”“临时生命值”页，再核对
[官方SRD5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18的Healing、Falling Unconscious、Death Saving Throws、Stabilizing a Character、
Temporary Hit Points。2014/扩展/自制未纳入。

5R规则摘要：普通0HP且未死者的昏迷持续至恢复任意真实生命值；治疗不能超过
生命值上限，恢复生命也使两项死豁计数归零。稳定是仍0HP时不再投死豁的情形；
恢复真实HP后不再需要该0HP稳定记录。临时生命不算治疗，也不使0HP者苏醒。
软件移除两条A16记录属于来源管理，不声称执行完整昏迷的其他机械效果。

| 情形 | 本次支持边界 |
| --- | --- |
| 原A16最新APPLIED尝试、当前目标完整卡恰为其应用结果 | 可预览；要求未改版本、原始规范两条注记、HP0/maxHP>0/死豁0/0，导入审核关闭 |
| DM已裁定合法普通治疗，整数amount为1–100000且明确reviewed | 加入真实HP，上限封顶；同一事务移除原A16稳定和0HP昏迷两条记录 |
| 原tempHP、资源、施救者、原A16医药骰 | 全部保持；不重掷、不回滚旧稳定证据；施救者之后的编辑不影响目标的治疗资格 |
| 非A16注记、文字伪造、复制卡、旧/ROLLED/HEALED尝试 | 人工或版本冲突，不从名称/文字识别可解除来源 |
| 原目标任意后续编辑/权限配置变化、额外或修改的状态、非零HP、死豁变化 | 人工；新治疗不追随手工编辑后的卡，历史精确回执仍按当前DM授权返回 |
| 零治疗、负数、小数、超限、客户端最终HP/删除列表/可信布尔值 | 拒绝非法输入，不作为临时生命增益入口 |
| 未审阅、特殊治疗限制、怪物/死亡/复活或另一个昏迷来源 | 人工，不让审阅覆盖服务器的来源/版本/数值门 |
| 治疗骰、施法/药水/资源消耗、行动/接触判断、自动恢复/死豁/伤害/倒地 | 未执行，DM另行处理；旧0HP伤害门保持 |

配置 `5R-SRD-5.2.1-STABILIZATION-HEALING-V1`。规则与上述更窄的软件边界分开。
本文件为摘要和软件设计，未复制规则全文。This work includes material from the
System Reference Document 5.2.1 by Wizards of the Coast LLC, available at
https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文规则摘要及项目支持矩阵。
