# DND Roll

面向固定伙伴的 D&D 5R 网页虚拟桌面。电脑优先，长期保存战役进度。当前实现 **M0 框架 + M1 首个纵向切片**：登录、战役场景、网格棋子移动、权限过滤、事务保存与实时同步。

功能与修复记录见 [CHANGELOG](CHANGELOG.md)，按 Keep a Changelog 维护；未发布的变更统一列在 Unreleased。

## 目录

```text
frontend/            React + TypeScript + 原生 PixiJS
backend/             Java 25 + Spring Boot + PostgreSQL/Flyway
contracts/           HTTP OpenAPI、WebSocket 消息及行为契约
deploy/              Docker Compose、Dockerfile、NGINX
docs/                产品设计、决策、实施路线与验收证据
scripts/             契约检查与 PowerShell 检查入口
.github/workflows/   前端、真实数据库、浏览器检查
```

## 启动

标准方式为 Docker Compose（PostgreSQL 放在独立容器，使用持久卷）：

```powershell
Copy-Item .env.example .env
# 编辑 .env，替换全部 CHANGE_ME 密码
docker compose --env-file .env -f deploy/compose.yaml up --build -d
```

访问 `http://localhost:8080`；使用 .env 配置的 dm/player 账号。账号初始化仅在显式开启 `demo` profile 时执行，修改环境变量不会重置已经建立的账号密码。详见 [部署说明](deploy/README.md)。

本地源码开发需要 JDK 25、Node.js 24.22.0（本次也验证了 24.21.0）和 PostgreSQL 18.6。Maven Wrapper 固定下载 Maven 3.9.16。

```powershell
# 后端：设置实际数据库和 demo 密码，不把凭据写入源码
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/dnd_roll'
$env:DATABASE_USERNAME = 'dnd_roll'
$env:DATABASE_PASSWORD = '<your database password>'
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:DEMO_DM_PASSWORD = '<your DM password>'
$env:DEMO_PLAYER_PASSWORD = '<your player password>'
cd backend
.\mvnw.cmd spring-boot:run

# 另开终端，回到仓库根目录
cd frontend
npm ci
npm run dev
```

前端默认 `http://localhost:5173`，通过代理访问后端 8080。Linux/macOS 使用 `./mvnw`；需要可用的 JAVA_HOME。`.env` 供 Compose 使用，源码运行不自动加载该文件。

## 检查与 TDD

```powershell
pwsh -File scripts/check.ps1

# 真实数据库集成检查：必须是独立测试数据库，测试会修改其中数据
$env:TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/dnd_roll_test'
$env:TEST_DATABASE_USERNAME = '<test user>'
$env:TEST_DATABASE_PASSWORD = '<test password>'
pwsh -File scripts/check.ps1 -Integration
```

单独检查：`node scripts/check-contracts.mjs`；backend 中 `./mvnw test` 或 `./mvnw -Pintegration verify`；frontend 中 `npm test`、`npm run build`。浏览器验收入口见 [前端说明](frontend/README.md)。Java 单元测试无需 Docker/数据库；集成测试必须连接真实 PostgreSQL，缺配置会失败，不静默跳过。

已执行的 RED/GREEN 证据见 [后端](backend/TDD.md)、[前端](frontend/TDD.md)；最终检查及尚未执行的检查见 [验收记录](docs/verification.md)。

## 当前边界与后续阶段

当前提供探索移动：同一玩家可移动被授权棋子，DM 可操作本团所有棋子；玩家快照不包含隐藏单位；请求版本检查和幂等回执在事务内保存。STOMP 只发送场景失效提示，重连重新查询权威状态。

账号邀请、场景/地图作者入口、完整角色卡、掷骰、战斗、RP、素材与备份恢复是 M2；模组与自动结算是 M3。具体分工及验收条件见 [实施路线](docs/implementation-plan.md)，产品范围见 [设计方案](docs/product-design.md)。Docker 容器、云端发布和完整产品验收未因骨架完成而自动视为通过。

仓库仅包含应用代码和自主设计资料。工作区的本地规则原文、全文索引、模型、私人战役与下载的运行时不随仓库分发。来源与资料边界见 [说明](docs/local-sources.md)。
