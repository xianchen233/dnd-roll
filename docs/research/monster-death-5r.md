# A10：普通怪物降至 0 HP 的死亡记录

2026-10-26（Asia/Shanghai）。A07/A08/A09 保持。本次先核验来源，再契约及实际 RED。

A 索引仅 `search.py 生命值降至 --book 玩家手册2024 --locate`，没有回读商业正文。
B 索引 `hsearch.py 生命值降至 --mode lex --limit 3 --json` 后完整回读：
`玩家手册2024/进行游戏/生命值降至0点.htm`、`玩家手册2024/进行游戏/临时生命值.htm`。
本地根为 `D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/`。
同时核验[官方 SRD 5.2.1 PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
印刷 p17 Monster Death / Knocking Out a Creature 和 p18 Temporary Hit Points。
中文快照的“可能会立即死亡”比官方英文默认规则语气弱；本配置要求 DM 明确选择普通怪物死亡，保留主持人按角色处理的选择。

| 项目 | 5R 依据与支持范围 |
| --- | --- |
| 普通怪物降至 0 | p17：怪物降至 0 即死，DM 可为个别怪物改用角色规则。独立 `5R-SRD-5.2.1-MONSTER-DEATH-V1`，只接受 DM 明确选择 `MONSTER_DIES_AT_ZERO` 且审阅无例外 |
| 数值 | p18：先耗临时 HP；只在原 HP>0、最终伤害足以耗尽 HP 时生成 HP=0/tempHp=0 方案；不足、失手、免疫转人工，不替代 A08 |
| 死亡记录 | 原子保存 HP 与来源化“死亡（记录）”标记；死亡不是 5R 的 condition，本软件复用状态实例作为注记容器，不声称自动执行死者的全部规则、禁用行动、移出先攻或删除棋子 |
| 分类 | 软件没有可靠 PC/monster 类型字段；不得按名称、隐藏或控制者猜测。DM 明确声明对象采用怪物规则，选按角色处理则转人工 |
| 击昏 | p17：近战可改为 1 HP 并昏迷，与死亡不同。本配置不做此选择；审阅必须确认未选择击昏 |
| 例外 | 不死坚韧、坚韧不屈、变形、死亡触发、专注/反应等须 DM 审阅；软件不解析自由文字。任一已有状态、死亡豁免记录、当前 0 HP、同来源/目标均转人工 |
| 资源 | 不消费弹药、法术位或其他资源；DM 须确认已另行处理且无未结算资源。A09 原短弓箭支方案不能直接组合此配置 |
| 后续 | 角色 0HP、过量伤害、死亡豁免、稳定、复活、治疗、自动移除死亡标记及扩展均人工；普通更正仍仅明确修改数值，标记移除需独立 DM 状态操作 |

本文件为规则概括和软件范围说明，不是完整规则转载。
This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
