# A15 动作急救解除原击昏：5R来源与支持矩阵

2026-10-28。先查证，再契约与实际RED。A07–A14不重做。
按 dnd-rule-lookup：A库仅定位《玩家手册2024》生命值降至0点，未读取商业正文；
B库检索并全文回读“生命值降至0点”“D20检定”“属性检定”。交叉核对
[官方SRD 5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
Knocking Out a Creature p17、Stabilizing a Character p18、D20 Tests p6。
昏迷结束仍倒地的边界沿用已核对的Unconscious p191。

5R近战击昏留下1HP；其他生物花一个动作急救并通过DC10感知（医药）可提前解除。
0HP稳定另用Help动作和医药检定，稳定仍昏迷，不是本切片。普通属性检定比较总值
与DC；不套用攻击的天然1/20自动失手/重击。软件不从“急救”名称推断检定来源。

| 情形 | A15支持与边界 |
| --- | --- |
| A11/A12已应用且目标卡为原应用后版本、唯一规范记录、HP1/tempHP0/无死豁 | 支持独立急救动作检定、方案和显式解除；满生命也可解除 |
| 施救者 | 同战役、与目标不同、未待导入审核、HP>0/无状态/无死豁；普通WIS医药技能与熟练/专精、已保存平直加值；不支持修正器/覆盖值或自动成功/替骰规则 |
| 动作/接触/行动合法性/未处理例外 | DM明确审阅；不自动扣动作、推断距离/回合许可或使用Healer特性 |
| 检定证据 | 专用DM入口调用服务器skill:medicine、单d20、无优势/劣势；原子保存骰、回执及绑定当前击昏/施救者版本的私有事件 |
| 总值>=10 | 只读方案后显式移除原记录；生命/资源/施救者保持 |
| 总值<10 | 保留失败检定，方案不可执行；读方案不掷骰，同键重试不重掷；新尝试需新动作审阅/版本/请求 |
| 客户端提供总值、任意普通骰、其他击昏或旧尝试的骰 | 不采信；必须是当前击昏最新专用尝试事件绑定的rollId |
| 目标/施救者改卡、重建注记、已治疗/休息/急救解除 | 人工或版本冲突；旧回执不会再次移除后来恢复的注记 |
| 优劣势、其他属性、修正器/覆盖值、状态、0HP稳定/死豁、医疗资源或扩展 | 人工，不放开旧门 |
| 倒地/失能/速度/掉落/专注等完整昏迷关联效果 | DM继续处理，醒来仍倒地 |

来源和动作审阅是有界技术支持，不声称游戏规则禁止未支持的施救者急救。
配置 `5R-SRD-5.2.1-KNOCKOUT-FIRST-AID-V1`；原骰与原攻击证据/hash不重写。

This work includes material taken from the System Reference Document 5.2.1
(“SRD 5.2.1”) by Wizards of the Coast LLC, available at the official link above.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0
International License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
