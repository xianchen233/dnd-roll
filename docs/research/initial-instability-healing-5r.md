# A23 原A18失稳后的直接治疗：5R来源与支持矩阵

2026-10-29。dnd-rule-lookup：A库《玩家手册2024》“治疗”仅返回定位，B库完整
回读“治疗”“生命值降至0点”“临时生命值”。复用本会话已直接取得并核验的
[官方SRD5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18，未声称重新查询最新勘误。未读取商业正文或混用2014/扩展。

摘要：未死的0HP角色恢复真实HP后结束该0HP来源的昏迷，死豁成功与失败均清零。
治疗上限为maxHP，临时生命不算治疗且不能使0HP者苏醒。软件仅解除确切来源记录，
不执行完整昏迷、倒地、治疗骰或动作资源引擎。

| 情形 | 软件支持边界 |
| --- | --- |
| 原A16账本最新UNSTABLE，确切原A18日志和前一APPLIED仍绑定 | 重建A16→A18及原服务器骰，核对原A18方案hash、状态转换、前后卡与当前完整卡 |
| HP0/maxHP>0、成功0/失败1或2、唯一原a16-unconscious | 治疗后HP上限封顶、死豁0/0，仅解除这一条原昏迷；原稳定已由A18移除 |
| DM裁定普通合法真实治疗量整数1–100000且reviewed | 先只读预览、再显式应用；tempHP/资源/施救者/原骰保持 |
| 原施救者后续编辑 | 不影响已应用来源，使用原保存快照 |
| 尚未应用的A19新Help检定 | 保留其骰与独立账本；治疗推进父版本后旧检定不能应用，不重掷或删除尝试 |
| A19已再次稳定/A21再失稳、HEALED、ROLLED或其他来源 | 冲突或人工；A17/A20/A22各自来源门保持 |
| 后续目标编辑/权限改变、导入审核、额外或改变的状态、HP/死豁改变、修正器/覆盖 | 人工，不从名称/文字/复制卡推断来源 |
| 原A18日志/hash改变或预览后证据变化 | 人工或旧方案冲突，精确旧回执仍先核当前权限/目标存在 |
| 零/负/小数/超限治疗，客户端damageVersion/最终HP/移除列表/可信布尔值 | 严格拒绝，源版本由服务器推导 |
| 死亡/复活、特殊治疗限制、另一昏迷来源、未审阅、连续受伤/再Help/1d4恢复 | 人工，无自动执行；旧通用0HP门保持 |

配置`5R-SRD-5.2.1-INITIAL-INSTABILITY-HEALING-V1`。This work includes material
from the System Reference Document 5.2.1 by Wizards of the Coast LLC, available
at https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文规则摘要与软件支持矩阵，未复制规则全文。
