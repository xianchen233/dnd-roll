# 核实技术栈版本与兼容基线

Parent: ../design-map.md
Labels: wayfinder:research
Type: research
Status: resolved
Assignee: /root/versions_research
Blocked by: none

## Question

仅查官方资料，为 Java/Spring Boot、PostgreSQL、Node、React、TypeScript、Vite 和 PixiJS 提出可复现的具体版本基线，说明兼容性与是否需要另行固定补丁版本。用户熟悉后端、不熟悉前端，优先少量工具与维护清晰。

## Comments

- 工作区不是 Git 仓库，不初始化仓库或虚构 research 分支；研究资产写入本努力的 research/ 目录，以本票链接作上下文指针。

## Answer

详见 [技术栈版本调查](../research/stack-versions.md)。经官方资料核实，推荐设计基线：Temurin JDK 25 LTS（已核实构建 25.0.4.1+1）、Spring Boot 4.1.1、PostgreSQL 18.6、React/React DOM 19.3.0、TypeScript 5.9.3、Vite 8.3.4、Node 24.22.0 LTS、PixiJS 8.22.0。

Java 25 与 Boot 4.1.1 的官方支持范围匹配，Node 版本满足 Vite 文档要求。TypeScript 5.9.3 为降低 Pixi 类型配置复杂度而选择的正式版本，不声明为最新版本。Spring 组件采用 Boot BOM 管理。Node 只用于开发与构建，生产前端为静态文件。只做官方条件核验，未实际安装或联调；补丁升级、锁文件及镜像摘要在实施时核实固定。
