# A04 自动攻击规则依据与支持矩阵

2026-10-25（Asia/Shanghai）。先用 dnd-rule-lookup 查证，再写计算器。
基线为 **5R / SRD 5.2.1**，不是 2014；来源出版年份不代替规则版本。

本机 A 索引只用 `search.py 攻击检定 --book 玩家手册2024 --locate` 定位，
未读取或提交商业正文。B 索引用 `hsearch.py <术语> --mode lex --json` 定位，
随后逐页 `--read`：`玩家手册2024/进行游戏/` 下的 `攻击检定.htm`、
`优势_劣势.htm`、`重击.htm`、`伤害掷骰.htm`、`抗性和易伤.htm`、`免疫.htm`。
本机根为 `D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/`。
B 是 5.2 中文快照；另行打开[官方 SRD 下载页](https://www.dndbeyond.com/srd)和
[官方英文 5.2.1 PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)，
核对下面章节，未发现本切片公式差异。页码为 PDF 印刷页码。

| 项目 | 已核对依据 | 本轮实现 |
| --- | --- | --- |
| 命中 | Attack Rolls / Rolling 20 or 1，p7 | 选定 d20 加最终命中修正 >= 当前 AC；天然1失手，天然20命中并重击 |
| 优劣势 | Advantage/Disadvantage，pp7–8 | 多个同向因素只用两骰；双向同时存在则只用一骰；取高/低的是裸骰，不把总值当作天然20 |
| 伤害 | Damage Rolls，p16 | 一组同面数伤害骰加最终定值修正，最低0；修正由上游已保存计算提供，不再次加属性/熟练 |
| 重击 | Critical Hits，p16 | 伤害骰数量翻倍，定值修正只加一次；必须提供实际额外骰，禁止复制原伤害总值 |
| 防御 | Resistance and Vulnerability / Immunity，p17 | 同类型抗性只计一次，先修正再抗性向下取整，再易伤翻倍；伤害免疫最终为0 |

支持配置精确身份：`5R-SRD-5.2.1-WEAPON-V1`。这是**纯候选计算**支持程度，
不是“可自动应用”。仅接受来源 `SRD / 5.2.1 / 5R`、单目标普通武器攻击，
一种明确伤害类型、一组同面数骰和一个已合计的定值修正。DM/未来服务适配器必须
明确审阅情境并确认没有遗漏的例外；计算器不从自由文字或角色名称猜测能力。

| 情境/内容 | 支持程度 | 处理 |
| --- | --- | --- |
| 普通近战/远程武器攻击；审阅后的最终命中、伤害和AC | 候选计算 | 地图合法性、射程、触及、遮蔽和目标有效性已由DM审阅；不自动推断 |
| 已明确的优劣势因素、同伤害类型免疫/抗性/易伤 | 候选计算 | 输入保留各因素及来源；适用条件必须已确认 |
| 未审阅情境、未知配置或来源版本、旧版2014、CUSTOM | 人工裁定 | 不返回部分命中/伤害候选，不静默降级成普通攻击 |
| 法术、豁免、徒手、固定无骰伤害、混合伤害/多组骰、多目标 | 人工裁定 | 不套用本武器计算器；后续逐项扩充矩阵与测试 |
| 精通、偷袭、惩击、扩展重击范围、自动重击、重骰/替换骰、反应、条件性减伤 | 人工裁定 | 包括SRD核心中存在但本切片未实现的特性；来源开放不等于已自动支持 |
| 状态效果、专注、0HP/死亡、击昏、临时HP、资源/弹药/动作经济 | 人工裁定/后续A05 | 本轮仅计算伤害数值，不修改任何游戏状态 |
| DM选用的官方扩展、第三方、自制、旧版兼容 | 人工裁定 | 当前没有具体选定并核验的扩展自动化条目，支持数量为0；不假定全部兼容 |

扩展接入须记录条目、书名、精确版本、规则体系、DM启用/兼容决定、可共享的依据、
测试和执行边界后再新增独立配置。商业索引定位不能冒称正文已核验；也不能借用
SRD署名推定扩展文本可分发。本轮没有复制扩展或商业规则正文。

本文件为中文概括与软件范围说明，非完整规则转载。
This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
