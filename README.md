# DND Roll

面向固定伙伴的 D&D 5R 网页虚拟桌面。电脑优先，长期保存战役进度。当前实现 **M0 框架、M1 纵向切片、P01 账号邀请、P02a 多场景、P02b 私有素材备团、P02c 棋子作者及 P02d 安全地图与手动遮盖**：固定账号、创建战役、邀请与成员管理、DM 创建及切换全团当前场景、持久私有原图及地图校准、棋子配置与共享控制、网格移动、权限过滤、事务保存与实时同步。玩家只接收服务端按揭露区域重新编码的安全地图；原生 Pixi 支持平移、缩放、吸附拖动、几何测距及 DM 矩形遮盖，单位隐藏独立。P02e 整体验收及整个 P02 已完成：17 条真实浏览器、四组实际服务重启和最终提交两组完整 CI 通过，各包证据见验收记录。

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

访问 `http://localhost:8080`；使用 .env 的 `BOOTSTRAP_USERNAME` / `BOOTSTRAP_PASSWORD` 登录，创建战役并邀请伙伴。初始化只插入不存在的账号，修改环境变量不会重置已有密码或成员权限。两个 bootstrap 变量必须同时配置或同时留空；新账号名使用 3–32 位小写字母、数字、下划线或连字符，以字母开头；密码至少 12 个字符且不超过 72 个 UTF-8 字节。详见 [部署说明](deploy/README.md)。

本地源码开发需要 JDK 25、Node.js 24.22.0（本次也验证了 24.21.0）和 PostgreSQL 18.6。Maven Wrapper 固定下载 Maven 3.9.16。

```powershell
# 后端：设置实际数据库和初始账号，不把凭据写入源码
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/dnd_roll'
$env:DATABASE_USERNAME = 'dnd_roll'
$env:DATABASE_PASSWORD = '<your database password>'
$env:BOOTSTRAP_USERNAME = 'owner'
$env:BOOTSTRAP_PASSWORD = '<your initial account password>'
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

DM 可创建多个空场景，再明确切换全团当前场景；创建不会自动切图。玩家只读取当前场景，不能列出备团场景。切回旧场景保留棋子位置、版本与控制权，历史切换请求重试不会再次切图。契约见 [P02a](contracts/scenes.md)。

DM 可在地图备团区上传、预览、下载私有 PNG/JPEG，并给场景保存地图引用、每格像素、偏移和每格距离；重试不会重复上传或重新应用历史配置。原图和配置只向本团当前 DM 开放，不进入玩家快照或下载权限。素材保存在独立持久目录，数据库保存稳定 ID 和同团引用；详见 [P02b 契约](contracts/assets.md) 与 [部署说明](deploy/README.md)。

DM 可创建棋子，编辑名称、网格位置、隐藏状态与多名控制者；控制者只能选本团当前成员。移动和编辑共享棋子版本，历史回执只确认原请求，不能恢复旧控制权。移除成员会撤回其控制并推进相关版本，重新加入仍需 DM 新授权。契约见 [P02c](contracts/tokens.md)。

P02d 在逻辑网格上交付服务器生成的安全地图，默认全遮盖，DM 保存揭露格后玩家才收到公开像素；原图、素材标识与校准配置仍只向 DM 开放。改校准或换图会重新全遮盖；再次遮盖无法撤回已保存的公开内容。Pixi 提供缩放、平移、吸附移动、直线测距与 DM 矩形遮盖工具，坐标表单继续可用。契约见 [P02d](contracts/map-visibility.md)，实际验收结果见下列记录。P02e 已按 [整体验收契约](contracts/p02-acceptance.md) 通过双端切图、离线与真实 socket 重连、撤权、精确重试和两场景实际服务重启；整个 P02 完成，证据见 [验收记录](docs/verification.md)。完整角色卡、掷骰、战斗、RP、备份恢复仍属后续 M2 工作，模组与自动结算为 M3。具体分工及验收条件见 [实施路线](docs/implementation-plan.md)，实际检查见 [验收记录](docs/verification.md)，产品范围见 [设计方案](docs/product-design.md)。云端发布和完整产品验收按各自阶段执行。

仓库仅包含应用代码和自主设计资料。工作区的本地规则原文、全文索引、模型、私人战役与下载的运行时不随仓库分发。来源与资料边界见 [说明](docs/local-sources.md)。
