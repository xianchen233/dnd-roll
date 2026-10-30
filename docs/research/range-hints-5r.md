# A07 范围提示的 5R 依据与支持矩阵

2026-10-26（Asia/Shanghai）。下一包从 `m3-remaining.md` 的范围缺口拆出；
不重做 A06 路径预算，也不关闭自动效果或公开自动应用。

使用 dnd-rule-lookup：A 仅 `远程攻击 --book 玩家手册2024 --locate`；
B 用 lex 定位后完整回读 `玩家手册2024/进行游戏/移动和位置.htm`、
`远程攻击.htm`、`近战攻击.htm`。商业正文没有读取或复制。
另核对[官方 SRD 下载页](https://www.dndbeyond.com/srd)与
[SRD 5.2.1 PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
印刷 pp14–15 的 Playing on a Grid、Range、Reach。中文 5.2 快照与本包
使用的 5.2.1 范围规则一致。以下是概括与软件解释。

| 情境 | 支持程度与边界 |
| --- | --- |
| 5尺方格、同一平面、两者均以一个格表示，用户明确审阅 | 只读距离提示；最短逐格距离为 `5 * max(abs(dx), abs(dy))`，斜邻一格；同格距离0 |
| MELEE 与明确填写的最终触及 | 距离不大于触及时显示 WITHIN_REACH，否则 OUT_OF_RANGE；不根据角色/武器名称猜测触及 |
| RANGED 与明确填写的普通/最大射程 | 普通边界以内 NORMAL_RANGE，普通之外且最大以内 LONG_RANGE，最大之外 OUT_OF_RANGE；单一射程用两个相等值表示 |
| LONG_RANGE | 仅提示该射程档通常带来劣势；不计算最终优劣势、不投骰、不修改A04/A05情境 |
| 未审阅、非5尺地图、体型/高度等不能按单格平面表示 | MANUAL_REQUIRED，无距离或部分分档；DM另行裁定 |
| 隐藏/其他场景/其他战役的棋子 | 不可作为玩家查询端点，统一404；不返回坐标、距离、隐蔽原因或候选数量 |
| 遮蔽、墙、视线、单位阵营/感知/状态、贴身远程、借机攻击 | 不计算或推断；地图公开可见不代表规则上的可见或可攻击 |
| 法术范围形状、范围效果、多格空间、3D、特殊规则、2014、官方扩展/第三方/自制自动化 | 人工；本包扩展自动化条目为0；后续逐项核验来源、版本与例外 |

范围阈值为用户声明的最终值，软件不证明它属于某个已获准能力；最大100000尺是
输入限额，不是规则上限。结果是当前场景版本的测量，不是行动合法性或可执行凭据。
仅当前成员可测量当前可见的两个棋子，无需移动控制权或当前回合授权，因为不改变状态。

This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
Changes: paraphrased into a bounded software support matrix; no rules text copied.
