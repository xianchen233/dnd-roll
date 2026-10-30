# A13 治疗恢复生命并解除击昏记录：来源与支持矩阵

2026-10-27；先查证，再契约、实际 RED、实现。A07–A12不重复。

已按本地 dnd-rule-lookup 检索：A库仅定位《玩家手册2024》“生命值降至0点”，
未读取或复制商业正文；B库 SRD5.2 中文全文读取“治疗”“生命值降至0点”“临时生命值”。
交叉核对 [官方 SRD 5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
的 Knocking Out a Creature / Healing（p17）、Temporary Hit Points（p18）。
Unconscious（p191）的醒来仍倒地边界沿用 A11/A12 已核对来源。

5R近战击昏留下1HP并昏迷；恢复任意实际HP可提前结束这种昏迷。
治疗不能超过生命上限，溢出丢弃；临时生命不是实际生命，也不是治疗。
因此满生命时治疗没有实际恢复，不能凭治疗输入移除记录。
短休结束和动作急救DC10感知（医药）是另外的解除路径，不纳入此切片。

| 情形 | A13行为及边界 |
| --- | --- |
| A11短棒/A12匕首已应用，目标卡仍为当时版本与完整规范注记 | 支持；DM输入已裁定的正整数治疗量并审阅例外，服务器计算恢复量并原子移除该注记 |
| 超出生命上限 | 只恢复到上限；实际恢复必须大于0 |
| 满生命、0HP、死豁、临时生命或其他状态 | 人工；无写入 |
| 编辑、资源变化、更正、移除后重建注记 | 人工；即使数值和注记恢复相同也不能重新取得来源资格 |
| 非A11/A12或未应用的来源 | 人工/阶段冲突；不信任客户端名称和伪造注记 |
| 治疗法术骰、法术位、药剂/库存、休息、急救 | DM先独立裁定；本切片不自动掷骰或消耗资源 |
| 昏迷关联效果、专注、掉落、速度、倒地 | 不自动执行；醒来仍倒地，DM继续处理 |
| 通用加血/编辑/CORRECT | 原有语义保持，不推断解除 |

本切片是技术支持边界，不声称规则禁止编辑后的目标接受治疗。
配置 `5R-SRD-5.2.1-KNOCKOUT-HEALING-V1`；读方案不写状态，显式执行才同时保存HP与解除。

This work includes material taken from the System Reference Document 5.2.1
(“SRD 5.2.1”) by Wizards of the Coast LLC, available at the official link above.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0
International License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
