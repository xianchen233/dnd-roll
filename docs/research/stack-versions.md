# VTT 技术栈版本调查

调查日期：2026-10-18。以下是本次从官方页面实际核实的正式版本，作为设计基线；不承诺在未来实施时仍是最新补丁。未编写或安装应用代码。

## 推荐基线

| 部分 | 建议版本 | 依据与选择说明 |
| --- | --- | --- |
| Java | Eclipse Temurin JDK 25 LTS，已核实构建 `25.0.4.1+1` | Adoptium 将 JDK 25 标为 LTS，官方发布库提供该构建。部署时固定镜像摘要，并定期升级同系列安全补丁。[LTS 下载页](https://adoptium.net/temurin/releases/?version=25)、[正式发布](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4.1%2B1) |
| 后端 | Spring Boot `4.1.1` | 官方稳定文档要求 Java 17 至 26，因此 JDK 25 在支持范围内。Spring Framework、Security、数据库驱动等优先采用 Boot 依赖管理，不逐项指定互不协调的版本。[系统要求](https://docs.spring.io/spring-boot/system-requirements.html)、[依赖版本管理](https://docs.spring.io/spring-boot/reference/using/build-systems.html) |
| 数据库 | PostgreSQL `18.6` | 官方版本表列为受支持稳定版本，18 系列支持至 2030-11-14；页面所示 19 仍为 RC，不用于此基线。[版本支持表](https://www.postgresql.org/support/versioning/) |
| 前端 UI | React / React DOM `19.3.0` | 官方版本页列出 2026-09-09 的正式发布；两者保持相同版本。[版本页](https://react.dev/versions) |
| 前端类型检查 | TypeScript `5.9.3` | 已核实正式版本。虽然官方已有 7.0.2，本项目为减少初学者类型配置，先用 5.9.3；这是工程取舍，不是“最新版”声明。[正式发布](https://github.com/microsoft/TypeScript/releases/tag/v5.9.3) |
| 前端构建 | Vite `8.3.4` | 官方发布页列为正式发布；当前指南要求 Node 20.19+ 或 22.12+，并提供 `react-ts` 模板。[正式发布](https://github.com/vitejs/vite/releases/tag/v8.3.4)、[指南](https://vite.dev/guide/) |
| 构建环境 | Node.js `24.22.0` LTS + 自带 npm | 官方发布页列为 Latest LTS；满足 Vite 的 Node 要求。Node 只用于开发、检查、打包；生产网页为静态文件，生产业务后端仍为 Java。[发布状态](https://nodejs.org/en/about/previous-releases)、[Vite 静态构建](https://vite.dev/guide/build) |
| 地图画布 | PixiJS `8.22.0` | 官方发布页有固定版本安装方式。8.21 已明确兼容 TS 5、6、7；TS 6/7 的 WebGPU 类型需额外处理，TS 5 不需新增该迁移配置。[正式发布](https://github.com/pixijs/pixijs/releases/tag/v8.22.0)、[类型兼容说明](https://github.com/pixijs/pixijs/releases/tag/v8.21.0) |

如果开发者现有代码、常用库明确依赖 Boot 3，可替代采用 Boot `3.5.16` + JDK 25；官方文档确认此组合兼容。新项目没有该约束时，本报告推荐 Boot 4.1.1，避免初建后很快做一次大版本迁移。[Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)

## 降低前端复杂度的设计建议

以下为本项目的架构判断，并非官方唯一推荐：

- 使用 React 单页应用，先采用 Vite 官方 `react-ts` 模板；无公开内容 SEO 或服务端渲染需求，暂不增加 Next.js、Node 业务服务器。
- 普通界面由 React 负责；地图区域封装为一个组件，在生命周期中创建、更新、销毁原生 PixiJS Application。React 官方将 Effect 用于同步外部系统，PixiJS Application 提供独立初始化和渲染生命周期；因此可以采用这种集成方式。[React Effect](https://react.dev/reference/react/useEffect)、[PixiJS Application](https://pixijs.com/8.x/guides/components/application)
- 首版不强制引入 `@pixi/react`。它是可用的官方生态方案，但额外增加 React renderer 版本匹配；直接封装 PixiJS 可减少学习一层抽象。高频拖动、动画留在 PixiJS 中，完成操作后与应用状态同步，不让每帧动画驱动整套 React 表单重绘。[Pixi React 项目](https://github.com/pixijs/pixi-react)
- npm 锁文件、Maven Wrapper 和部署镜像摘要在实施时一并固定；版本基线允许后续安全补丁升级，不等同于永久冻结。
- 本次只核对官方支持条件，未实际安装、构建或联调，不能把文档兼容性写成已通过集成测试。
