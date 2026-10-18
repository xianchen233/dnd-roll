# 比较集中式服务、DM 本机托管与 P2P 联机

Parent: ../design-map.md
Labels: wayfinder:research
Type: research
Status: resolved
Assignee: /root/network_research
Blocked by: none

## Question

基于官方资料比较网页 VTT 的集中式 WebSocket 服务、DM 本机运行同一后端与浏览器 WebRTC P2P，解释公网可达性、信令、STUN/TURN、权限与暗骰、掉线及迁移差异。提出单团小规模部署的架构建议，区分资料事实与容量估算。

## Comments

- 工作区不是 Git 仓库，不初始化仓库或虚构 research 分支；研究资产写入本努力的 research/ 目录，以本票链接作上下文指针。

## Answer

研究见 [联机拓扑调研](../research/network-topology.md)。官方资料确认 WebRTC 需要独立的信令交换，无法直连时依赖 TURN 中继；P2P 不自动免除服务与流量成本。

本项目建议权威 Java 后端 + HTTPS/WSS，云端与 DM 本机是同一套服务的两种部署方式，迁移不要求改变通信拓扑。本机仍须可达入口，DM 电脑关闭期间不能访问。权限过滤、裁定归属与持久化独立于是否使用 P2P。此票解决事实调查与建议，最终方案在架构票与用户确认。
