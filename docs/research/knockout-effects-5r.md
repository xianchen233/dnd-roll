# A11：普通短棒近战击昏的生命与状态记录

2026-10-27（Asia/Shanghai）。A07/A08/A09/A10保持，不重做。
先用A索引 `search.py 生命值降至 --book 玩家手册2024 --locate` 定位，未回读商业正文。
B索引检索后完整回读 `玩家手册2024/进行游戏/生命值降至0点.htm` 与
`玩家手册2024/进行游戏/临时生命值.htm`，另以“昏迷 Unconscious”定位并完整回读
`玩家手册2024/术语汇编/状态.htm`；根目录为
`D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/`。
在线核验[官方SRD 5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
印刷p17 Knocking Out a Creature、p18 Temporary Hit Points、p91 Weapons、p191 Unconscious。

| 项目 | 5R规则与本包支持范围 |
| --- | --- |
| 击昏选择 | 近战攻击原本足以降至0时，可改留1HP并昏迷。不是2014的0HP稳定。独立配置 `5R-SRD-5.2.1-KNOCKOUT-V1` |
| 武器与方式 | 本包仅受信内置短棒 `916bf8b1-a24e-5dea-be38-2caee47dad54`、STR、无精通，DM明确选择本次为近战击昏。其他武器、投掷/远程、徒手、法术/扩展仍人工；不按名称或钝击类型猜测 |
| 数值 | 原HP>0，先耗临时HP；最终伤害足以耗尽原HP时，保存HP=1/tempHP=0、实际HP损失为原HP减1。原HP=1时损失为0仍可击昏。失手/免疫/不足均人工 |
| 原子记录 | 独立确认不改卡；APPLY一次写生命与来源化“昏迷（击昏记录）”，保留死豁/资源。该状态记录不等于完整状态执行引擎 |
| 后续解除 | 规则要求立即开始短休，短休结束、恢复任意HP或动作急救通过DC10感知（医药）可解除。软件无休息/急救事务；由DM跟踪并独立移除记录，无固定轮数/墙钟自动删除；数值更正不能推定治疗 |
| 昏迷关联 | 失能、倒地、掉落持有物、速度/攻击/豁免及专注等关联规则由DM处理，醒来仍倒地；本包不自动禁用操作或改装备、先攻、移动账本 |
| 例外审阅 | 须明确审阅目标免疫/存活/受伤触发、攻击合法性、资源和其他关联效果。任一已有目标状态或死亡豁免、原0HP、同来源目标均人工；不解析自由文字 |
| 旧门保持 | A08/A09的0HP/状态门、A10怪物死亡、旧确认/hash/回执均不变；不组合箭支、不自动公开，不自动执行其他死亡/稳定/复活效果 |

以上为规则概括与软件限制，有意缩小自动化范围；不是完整原文转载。
This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
