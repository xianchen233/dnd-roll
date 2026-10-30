# A19 原A18失稳后经新Help再次稳定：5R来源与矩阵

2026-10-29。dnd-rule-lookup：A库《玩家手册2024》“稳定”只定位；B库全文回读
“生命值降至0点”“属性检定”，另核对动作条目。交叉核验[官方SRD5.2.1](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
pp17–18的Stabilizing a Character、Death Saving Throws，以及pp6–9属性检定/熟练。
未读取/复制A库商业正文；2014/扩展/自制不纳入本配置。

5R摘要：仍存活的0HP角色可经一次Help动作的DC10感知（医药）检定稳定；该SRD
条款未设普通稳定尝试的次数上限，新动作合法性和特殊例外由DM确认。稳定会清零
成功/失败计数，仍0HP且昏迷。新Help
需要独立检定，医药比较总值，不采用死亡豁免天然1/20规则。技能熟练不是前提。
以下更窄的来源/版本门属于软件边界，不作为规则本身的限制。

| 情形 | 本次支持矩阵 |
| --- | --- |
| 最新UNSTABLE A16来源 + 对应A18私有伤害日志，完整当前卡恰等于A18 after | 可尝试；原规范0HP昏迷单记录、HP0/maxHP>0、成功0/失败1或2，无审核/调整/覆盖 |
| DM确认仍活着、未稳定、此次新Help及接触合法、无例外 | 新服务器WIS医药检定；仅普通无优劣势，熟练/专精/已保存平直加值可用 |
| 原目标与本次施救者版本/完整卡、原A18版本及最新专用尝试绑定 | 读取和精确重试不重掷；新动作可替代上次失败尝试，旧方案不可复用 |
| 新检定总值>=10，另行显式确认 | HP0/tempHP保持、死豁清零，新增a19-stable来源记录并逐字保留原a16-unconscious记录 |
| 总值<10 | 检定与失败证据保存，无可执行应用值；无自动再次尝试 |
| 原A16/A15骰、普通骰、其他目标/施救者/版本/旧A19尝试、预占请求 | 拒绝；HTTP不能提供total/DC/boundOrigin或内部信任标志 |
| 原目标后续编辑、伪造/复制来源、额外状态、三失败/已稳定/已治疗/非零HP | 人工或冲突，不从名称/自由文字判来源；DM审阅不绕过服务器门 |
| 原A18施救者后续编辑 | 不影响其已完成的历史来源；本次新施救者必须在当前检定后保持原卡/版本 |
| 再次稳定后的伤害/治疗、自动死豁/1d4小时恢复、死亡/复活、完整动作/状态/资源 | 仍人工；旧通用0HP门与A16–A18输入边界保持 |

配置 `5R-SRD-5.2.1-RESTABILIZATION-V1`。本文件为中文摘要及项目支持矩阵。
This work includes material from the System Reference Document 5.2.1 by Wizards
of the Coast LLC, available at https://www.dndbeyond.com/srd, licensed under
[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).
修改说明：中文摘要、软件支持边界与来源追踪设计。
