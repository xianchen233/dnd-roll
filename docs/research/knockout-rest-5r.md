# A14 短休结束解除击昏：5R来源与支持矩阵

2026-10-27。先来源/矩阵，再契约、实际失败测试与实现。A07–A13不重做。

按 dnd-rule-lookup：A库只用`--locate`定位“短休”（首命中装备魔法物品，不能据此
证明短休定义）；未读取商业正文。B库检索后全文回读《玩家手册2024》SRD中文
“术语汇编/其他术语”“术语汇编/状态”“进行游戏/生命值降至0点”。
交叉核对[官方SRD 5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
Knocking Out a Creature p17、Short Rest p187、Unconscious p191。

短休是一小时休整，活动不能比阅读、交谈、进食或站岗更费力，开始时至少1HP。
投先攻、施展非戏法法术、受到任何伤害会中断；中断的短休没有收益。5R近战击昏
留下1HP并立即开始短休，完成时结束该昏迷；另有治疗/急救提前解除路径。昏迷
结束不会同时结束倒地。这里不采用2014的击昏到0HP规则。

| 情形 | A14支持行为 |
| --- | --- |
| A11/A12已应用、目标仍为应用后原版本、唯一完整规范注记、HP1/tempHP0/无死豁 | DM声明从该次击昏开始的连续合规短休已满60分钟、没有中断且审阅例外后，预览再显式移除记录 |
| 最大HP也为1 | 可以解除；此路径不要求实际治疗 |
| 不足60分钟、曾被中断、尚未审阅 | MANUAL_REQUIRED，不写入 |
| 卡修改/资源变化/更正、注记重建、A13已治疗或本路径已解除 | 人工/版本冲突；即使数值相同也不恢复来源资格 |
| 投先攻、非戏法施法、伤害或更费力活动 | DM必须声明中断或不合规；本切片不推断事件与游戏内时间轴 |
| 中断后重新开始休息、长休折算、自定义休息时间、其他来源的昏迷 | 人工，不把其算作原击昏立即开始的这次短休 |
| HP/临时生命/生命骰/法术位/特性或装备资源恢复 | 本切片不改变；如需先进行其他卡变化，后续解除按人工处理 |
| 现实时间、服务器启动/停机、浏览器等待、回合/状态tick | 不作为短休完成证据；无计时器 |
| 倒地、失能、速度、掉落、专注等 | DM继续处理；解除记录不声称完成完整状态引擎 |

这是技术支持边界；不声称游戏规则禁止编辑后的目标完成短休。DM输入的是游戏内
已裁定的连续分钟数与中断情况，不是客户端授权；身份、版本、来源与最终移除由
服务器检查。配置 `5R-SRD-5.2.1-KNOCKOUT-REST-V1`。本文件是规则摘要与实现选择。

This work includes material taken from the System Reference Document 5.2.1
(“SRD 5.2.1”) by Wizards of the Coast LLC, available at the official link above.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0
International License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
