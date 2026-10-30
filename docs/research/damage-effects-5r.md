# A08：普通武器伤害的生命扣减支持矩阵

2026-10-26（Asia/Shanghai）。先查证来源，再契约、实际失败测试与实现。
基线 **5R / SRD 5.2.1**；A07已完成，不接入测距或移动执行。

A索引仅 `search.py 临时生命值 --book 玩家手册2024 --locate`，未回读商业正文。
B索引以 `hsearch.py 临时生命值/生命值/生命值降至/专注 --mode lex --json`
定位，随后完整回读以下页（不是只凭摘要）：

- `玩家手册2024/进行游戏/临时生命值.htm`
- `玩家手册2024/进行游戏/生命值.htm`
- `玩家手册2024/进行游戏/生命值降至0点.htm`
- `玩家手册2024/术语汇编/其他术语.htm`（专注）

B根目录为 `D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/`。
另打开[官方SRD下载入口](https://www.dndbeyond.com/srd)及
[官方SRD 5.2.1 PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)，
核对 Hit Points（印刷p16）、Dropping to 0 Hit Points（pp17–18）、Temporary Hit
Points（p18）、Concentration（p179）；本切片未发现与本地5.2中文快照的公式差异。

| 项目 | 来源及规则概括 | 软件支持 |
| --- | --- | --- |
| 普通伤害扣减 | Hit Points p16；Temporary Hit Points p18：先消耗临时生命，剩余伤害扣HP | 新配置 `5R-SRD-5.2.1-DAMAGE-HP-V1`；只接受A05受信武器预览保存的最终伤害，防御/重击已在A04计算，不重复结算 |
| 临时生命耗尽但仍有正HP | 同上；吸收量=min(tempHp,damage)，HP损失=damage-吸收量 | 服务器推导目标最终HP/tempHp，资源列表为空（保持资源），原卡其余字段保持 |
| 失手、免疫或伤害为0 | A04矩阵及上列扣减规则 | 显示零变化方案，可明确确认/应用一次；不制造额外骰子或治疗 |
| 当前HP=0或扣减后HP<=0 | Dropping to 0 HP pp17–18：涉及怪物死亡、击昏选择、死亡豁免和过量伤害 | 整体转人工，无部分最终值；不默认夹到0或1，不自动加昏迷/死亡 |
| 专注、受伤触发、反应、浴血阈值能力、变形、特殊临时HP | Concentration p179及具体能力各自定义；临时生命吸收不意味着没受伤 | DM须另行明确审阅无未处理的关联效应；不会从自由文字猜测。已有任意状态实例或非零死亡记录保守转人工，包括已到期但未移除状态 |
| 资源、弹药、动作经济、治疗、赋予临时HP、状态执行/计时、精通/法术/扩展 | 未在此配置逐项核验 | 全部人工；本包新增扩展自动化条目数仍为0。未选定的官方扩展/2014/第三方/自制不推定兼容 |

额外DM审阅是本包明确的输入前提，不代表软件能自动识别所有角色特性。
仅在A05当前DM、原双方及关联行动版本、精确hash均有效时确认；生成方案和确认
都不改变角色，另行APPLY才原子写入。暗骰保持私密，公开摘要仍独立。
状态字段和持续时间存在不等于自动效果执行；本包只扩展上列HP/tempHp数值效果。

本文件为中文概括及软件支持范围说明，非完整规则转载。
This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.
