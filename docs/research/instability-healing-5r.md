# A22 原A21失稳后的治疗：5R来源与支持矩阵

2026-10-29。dnd-rule-lookup：A库《玩家手册2024》“治疗”仅定位，B库完整回读
“生命值降至0点”“治疗”“临时生命值”。本会话已直接取得并读取
[官方SRD5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18，复用其Healing/Falling Unconscious/Death Saving Throws/Temporary Hit Points
证据，不声称再次在线获取最新勘误。未读取商业正文或混用2014/扩展。

摘要：未死的0HP角色恢复任意真实HP，便结束该0HP来源的昏迷；恢复HP同时使两项
死豁计数归零。真实HP不超过上限，临时生命不算治疗，也不使0HP者苏醒。
软件只移除确切来源的原记录，不声称执行完整昏迷/倒地或动作资源引擎。

| 情形 | 本次支持边界 |
| --- | --- |
| 最新A19账本UNSTABLE与共享RESTABILIZATION_UNSTABLE，原A21日志绑定两本确切版本 | 重建A16→A18→A19→A21，要求当前完整卡恰为原A21结果，完成记录/两次服务器骰/原方案hash均核对 |
| 唯一原a16-unconscious，HP0/maxHP>0、成功0/失败1或2 | 治疗后只移除该条原昏迷，死豁0/0；原a19-stable已由A21移除，不再次删除 |
| DM声明已裁定普通合法真实治疗，amount整数1–100000且reviewed | HP=min(amount,maxHP)，tempHP/资源/施救者/原骰保持，不重掷或消耗资源 |
| 原/新施救者之后编辑 | 已应用来源仍可核验，使用保存快照，不依赖当前施救者版本 |
| 任一后续目标编辑/权限变化、导入审核、额外或修改的状态、非0HP/死豁改变 | 人工；不从文字/复制卡/名称识别可解除来源 |
| 原A18失稳、A19仍稳定/ROLLED/HEALED、旧尝试/缺日志或不符版本 | 冲突或人工；A17/A20双记录门保持，不直接接管其他失稳来源 |
| 零/负/小数/超限治疗，客户端HP/移除列表/damageVersion/可信来源布尔值 | 严格拒绝；来源日志版本由服务器推导 |
| 特殊治疗限制、死亡/复活、另一昏迷来源、未审阅 | 人工，审阅不能绕过服务器来源/数值检查 |
| 治疗骰、法术药水/资源、动作/接触、倒地、连续受伤、再Help、1d4小时 | 未执行；旧通用0HP门保持 |

配置`5R-SRD-5.2.1-INSTABILITY-HEALING-V1`。软件矩阵比规则范围更窄。
This work includes material from the System Reference Document 5.2.1 by Wizards
of the Coast LLC, available at https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文规则摘要与软件来源矩阵，未复制规则全文。
