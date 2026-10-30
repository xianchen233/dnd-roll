# A20 原A19再次稳定记录的治疗解除：5R依据与支持矩阵

2026-10-29。使用dnd-rule-lookup：A库《玩家手册2024》“治疗”仅返回定位，
未读取商业正文；B库完整回读“生命值降至0点”“治疗”“临时生命值”。
规则版本为5R/SRD5.2，2014/扩展/自制不纳入。官方基线为
[SRD5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18。在线检索工具429后，直接下载官方PDF，逐页读取pp17–18，确认与本地译本一致。

规则摘要：未死的0HP者恢复任意真实HP后结束该0HP来源的昏迷；治疗不得超过
生命上限，恢复HP使死豁计数归零。临时生命不是治疗，不使0HP者苏醒。
再次稳定没有另一个治疗门槛。本软件只解除确切来源记录，仍不执行完整状态引擎。

| 情形 | 本次支持边界 |
| --- | --- |
| A19最新APPLIED，A16共享状态RESTABILIZED且原A18日志仍绑定 | 重建完整A16→A18→A19来源，要求当前完整目标恰为原A19应用结果 |
| 原a19-stable及逐字保留的a16-unconscious两条记录 | 同事务移除两条；不靠名字或文字识别，不接受原A16稳定对冒充 |
| DM已裁定合法普通真实治疗、整数amount1–100000且显式reviewed | HP=min(amount,maxHP)，死豁0/0；tempHP、资源、施救者和全部原骰保持 |
| A16原施救者或A19新施救者之后编辑 | 不影响已应用的历史治疗资格；重建使用存储快照，无新检定 |
| 任一源账本版本不符、旧尝试、ROLLED/HEALED或未完成A19 | 版本冲突，不能从旧A17入口治疗再次稳定结果 |
| 目标后续任何编辑/权限变化、导入审核、其他状态、非0HP/非零死豁、修正器/覆盖 | 人工；新操作不跟随修改后的卡，精确旧回执仍先核当前权限 |
| 零/负/小数/超限治疗，客户端HP/移除列表/来源可信布尔值 | 严格拒绝；临时生命增益另由DM处理 |
| 特殊治疗限制、死亡/复活、其他昏迷、未审阅 | 人工，不让审阅覆盖服务器来源检查 |
| 治疗骰、资源、行动、法术药水、倒地、再次受伤、1d4小时恢复 | 未执行；旧通用0HP门及A17/A18支持边界保持 |

配置`5R-SRD-5.2.1-RESTABILIZATION-HEALING-V1`。软件矩阵比规则适用范围更窄。
This work includes material from the System Reference Document 5.2.1 by Wizards
of the Coast LLC, available at https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文规则摘要与来源/软件支持矩阵，未复制规则全文。
