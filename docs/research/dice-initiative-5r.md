# P05 规则核验（2026-10-21）

规则基线5R。使用dnd-rule-lookup：A只运行`先攻 --book 玩家手册2024 --locate`；
B以lex检索并完整回读以下两页，未发送A侧商业正文。

- [战斗流程](<D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/玩家手册2024/进行游戏/战斗流程.htm>)：各参战者敏捷检定；完全相同怪物组一次投掷，突袭造成先攻劣势，总值降序；PC平手由玩家决定，怪物/混合平手DM裁定。
- [优势/劣势](<D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/玩家手册2024/进行游戏/优势_劣势.htm>)：两颗d20取高/低，多源不叠加，同时有优劣则抵消。

在线再次核对官方[SRD入口](https://www.dndbeyond.com/srd)当前提供5.2.1，
以及[英文PDF](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf)
第7页Advantage/Disadvantage、第13页Initiative/Surprise/Ties；上述实现边界一致。
这不是核验所有扩展。既有来源化先攻修正/DM覆盖沿用P03，警戒交换等由DM明确
调整并记录理由；不读取专长名称来静默执行规则。固定先攻、英雄激励重骰、重击、
攻击/伤害应用不在本包自动化支持范围内。表达式上限、显式分组校验、候选/确认、
权限裁剪和多地图同角色去重是本项目工程设计，不冒称规则原文。

This work includes material from the System Reference Document 5.2.1 (“SRD 5.2.1”)
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd. The SRD
5.2.1 is licensed under the Creative Commons Attribution 4.0 International License,
available at https://creativecommons.org/licenses/by/4.0/legalcode.

本页为自行中文概述及软件设计推论，非逐字转录。中文译本贡献者见本地[鸣谢列表](<D:/workspace/dnd/Documents/SRD5.2_中文版/SRD5.2Chm-main/鸣谢列表.htm>)；商业资料不随软件打包。
