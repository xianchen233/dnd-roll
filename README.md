# DND Roll

面向固定伙伴的 D&D 5R 网页虚拟桌面。电脑优先，长期保存战役进度。M0/M1 与 P01–P06 已完成：固定账号与邀请、私有多场景/安全地图/共享棋子、独立半自动角色卡、开放规则检索与明确填卡、服务器掷骰、5R 先攻、手动战斗、角色回合及关联更正。本地与精确提交的远端验收见[验收记录](docs/verification.md)。下一包 P07 尚未开工，整个 M2 与后续阶段仍未完成，未合并、未发布。

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

P02d 在逻辑网格上交付服务器生成的安全地图，默认全遮盖，DM 保存揭露格后玩家才收到公开像素；原图、素材标识与校准配置仍只向 DM 开放。改校准或换图会重新全遮盖；再次遮盖无法撤回已保存的公开内容。Pixi 提供缩放、平移、吸附移动、直线测距与 DM 矩形遮盖工具，坐标表单继续可用。P02e 已通过双端切图、离线与真实 socket 重连、撤权、精确重试和两场景实际重启；整个 P02 完成。

角色卡支持一人多角、独立资源、来源计算与 DM 覆盖、历史及结构化传输；规则目录支持来源/版本/准入筛选和服务器预览后明确填卡。所有投骰由服务器产生并保留逐骰及来源；5R 先攻以独立角色参战，由 DM 明确确认次序。契约见 [P03](contracts/characters.md)、[P04](contracts/content.md)、[P05](contracts/dice-initiative.md)。

手动战斗将攻击、目标和投骰关联；DM 裁定命中并明确确认最终生命/临时生命/资源值，更正使用当前版本并保留原操作和后续变化。当前角色控制者可结束回合；DM 可跳过/切换/调整次序、放行回合外移动。探索移动保持自由，回合外投骰与行动仍可用；状态效果与到期移除继续由 DM 确认。见 [P06](contracts/combat.md)。RP时间线、团队/DM资料、完整一致备份和本地/容器首版验收见 P07–P10；模板库、定制副本与独立战役见 [A01](contracts/module-packages.md) / [A02](contracts/module-lifecycle.md)。选择性更新/手动触发、限定自动结算和移动预算仍待 A03–A06。云端部署/HTTPS/WSS/物理跨主机恢复及真实手机验证继续后置。具体范围和实际证据见[实施路线](docs/implementation-plan.md)、[验收记录](docs/verification.md)与[设计方案](docs/product-design.md)。

仓库仅包含应用代码和自主设计资料。工作区的本地规则原文、全文索引、模型、私人战役与下载的运行时不随仓库分发。来源与资料边界见 [说明](docs/local-sources.md)。
