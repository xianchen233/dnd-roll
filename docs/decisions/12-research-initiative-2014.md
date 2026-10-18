# 核对 2014 自动先攻的规则边界

当前用途：历史对照。用户已将基线改为 5R（2024 修订版）+ 扩展，当前设计依据见 [5R 核验](13-research-initiative-2024.md)，不得继续将本票当作新版规则依据。

Parent: ../design-map.md
Labels: wayfinder:research
Type: research
Status: resolved
Assignee: /root/initiative_research
Blocked by: none

## Question

在不外传本地商业规则正文的前提下，按 dnd-rule-lookup 流程核对 2014 战斗开始时的先攻、相同怪物分组、平手处理与顺序保持规则。区分已确认的“开始战斗自动投先攻”和产品默认行为建议，为首版 DM 确认与调整保留空间。

## Comments

- 本工作区不是 Git 仓库，不创建 research 分支；研究结果写到 research/initiative-2014.md。
- A 层仅返回 --locate 路径，不回传商业正文。若本地没有可用开放 2014 原文，则引用官方公开 2014 Basic Rules 或 SRD 5.1，不能拿 SRD 5.2 证明 2014 规则。

## Answer

见 [2014 先攻规则核对](../research/initiative-2014.md)。已通过官方公开 Basic Rules 2014 核对：战斗开始时确定先攻、顺序跨轮保持、同类怪物组及平手规则。A 层仅作路径定位，没有读取或外传商业正文。

设计建议为 DM 选择参战单位与分组后自动投首次先攻，平手待 DM 定序确认，保留额外修正来源和手动调整；隐藏单位不通过先攻列表或日志泄露。先攻例外与混合版本内容需要独立配置，不能当作已全部自动支持。
