# Docker Compose 部署

标准运行方式为应用与 PostgreSQL 分开容器，通过内部网络连接。需要 Docker Engine/Desktop 和 Compose v2。当前主机尚无 Docker，本轮本地数据库测试运行的是同版本 PostgreSQL 原生二进制；容器构建/启动结果以验收记录为准。

在仓库根目录：

```powershell
Copy-Item .env.example .env
# 编辑 .env，为数据库与 bootstrap 初始账号设置不同密码
docker compose --env-file .env -f deploy/compose.yaml up --build -d
docker compose --env-file .env -f deploy/compose.yaml logs -f
```

访问 `http://localhost:8080`（可在 .env 中调整 APP_PORT）。默认初始账号为 `owner`，密码使用 `.env` 中的 `BOOTSTRAP_PASSWORD`。登录后创建战役，再生成邀请链接；受邀者可以注册固定账号或用已有账号加入。DM 可创建多个空场景并切换全团当前场景，上传私有 PNG/JPEG 素材并保存地图引用与校准，以及创建/编辑棋子名称、位置、隐藏和共享控制。地图默认全遮盖，DM 明确保存揭露区域后玩家才收到对应像素；单位隐藏独立管理。

`BOOTSTRAP_USERNAME` 与 `BOOTSTRAP_PASSWORD` 必须同时有效或同时留空；新账号名为 `[a-z][a-z0-9_-]{2,31}`，密码至少 12 个字符且不超过 72 个 UTF-8 字节。初始化仅插入不存在的账号；重启不会重设已有密码，也不会授予其他战役访问权。需要演示数据时显式设置 `SPRING_PROFILES_ACTIVE=demo` 并设置两个 `DEMO_*_PASSWORD`；这会创建原有 dm/player 样例战役，关闭 profile 不会删除已有数据。

邀请链接包含一次性秘密，位于浏览器 URL fragment，只有首次创建时可以复制。服务端仅保存其 SHA-256；创建请求重试返回元数据，不再提供秘密。链接丢失时撤销后重新生成；有效期为七天。DM 可撤销未使用邀请和移除玩家；移除立即撤销该战役控制权并关闭该玩家现有实时连接，账号和其他战役成员关系仍保留。V2 Flyway 迁移为既有数据库增加战役管理版本、邀请与命令回执，不修改 V1。

Compose 默认只绑定本机回环端口，数据库不发布宿主机端口。上线时在宿主机配置域名、TLS 反向代理及会话 Cookie 的 Secure 属性，再按需调整发布接口。开发环境通过 Vite 的 `/api`、`/ws` 代理访问后端，不另行开放跨域权限。

HTTPS 入口应由可信的本机反向代理终止TLS，再转发到 `127.0.0.1:APP_PORT`；外层代理必须保留原始 Host，并**覆盖** `X-Forwarded-Proto: https`，同时转发 WebSocket Upgrade/Connection。内层NGINX清除客户端 `Forwarded`/转发端口头，重建 Host/协议并交给仅在内部网络可达、已启用 `forward-headers-strategy=framework` 的后端；这样HTTP会话与WebSocket同源判断使用同一浏览器域名和协议。`.env` 中另设 `SESSION_COOKIE_SECURE=true`。如发布自定义外部HTTPS端口，Host须包含该端口。不要把当前依赖可信本机代理的监听器直接改为公网任意客户端可访问，否则必须先重新定义可信代理边界。TLS证书与外层代理仍需发布环境实测。

数据库使用命名卷 `postgres_data`，挂载在 PostgreSQL 18 官方镜像要求的 `/var/lib/postgresql`；私有素材使用独立命名卷 `asset_data`，挂载在后端的 `/var/lib/dnd-roll/assets`（`ASSET_STORAGE_ROOT`）。重建容器保留两者。素材目录不挂到 NGINX，也不作为静态文件服务；每次下载必须经后端会话、战役与 DM 授权。单文件上限 10 MiB，NGINX 11 MiB 请求上限为 multipart 预留开销，外层 TLS 代理也应允许相同请求大小。

`docker compose down` 保留卷；不要使用 `down -v` 清理需要保留的战役和素材。升级或迁移前停写，配套保留数据库和素材目录，不能只复制其中一个。稳定素材 ID 与数据库中的不透明存储键使绝对宿主路径可改变；新实例应指向恢复后的素材根目录。没有数据库元数据的孤立文件不可通过下载接口访问。当前不自动删除素材，避免破坏已保存引用；崩溃或数据库提交确认不明时可能遗留未引用文件。提交异常后应用以新的战役锁等待旧事务结束并核对元数据，只有确认未提交才清理；已提交或数据库暂不可用时保留文件，重试由原命令回执恢复，不将错误假报为成功。后续 P09 提供一致备份、保留和恢复流程。卷本身不能替代异机备份。

当前固定 PostgreSQL 补丁、前端锁文件与 Maven 版本；基础镜像中 Java/NGINX 使用维护标签，发布阶段再固定经验证的镜像摘要并建立安全更新流程。该配置不代表已经完成容量测试、TLS 运维或备份恢复验收。

V3 Flyway 迁移移除单场景限制，为既有战役回填原场景指针，保留原场景、棋子、控制权与回执；同团复合外键限制当前场景归属。应用创建战役时在同一事务中设置初始指针，重启 demo 不重置已有选择。V4 增加素材、地图引用和独立素材命令回执，保留 V1–V3 数据；同团外键限制地图不能引用其他战役素材。升级前按上述流程保留数据库和素材。完整备份恢复仍属于 P09。

本机开发可通过 `ASSET_STORAGE_ROOT` 指向仓库外专用目录；默认 `.runtime/assets` 已被 Git 忽略。不要把素材根目录设为源码、前端 public 或 NGINX 静态目录。权限应只允许应用运行用户读写。内容下载禁用缓存；已被 DM 下载到本机的文件不受后续会话撤销影响。

P02d 的玩家地图由后端按当前场景、版本、成员和揭露格重新编码为 PNG，未揭露格是不透明黑色，原图仍只向 DM 开放。派生图不写磁盘、不使用共享缓存；`map-view`、`map-image` 和 `fog` 的成功及失败响应均为 `no-store, private`。反向代理/CDN 应保留这些标头，不给 `/api/` 配置缓存或静态替代路径。修改校准、更换或移除地图会在同一事务清空揭露格；旧版本图像 URL 失效。再次遮盖只影响后续请求，无法撤回玩家之前保存的公开图像。

V6 新增揭露格与独立 fog 命令回执，保留 V1–V5 的数据与迁移文件。原生 Pixi 提供缩放、平移、吸附移动及直线几何测距；遮盖可用矩形工具或坐标表单编辑后保存。`node scripts/check-map-restart.mjs seed <仓库外新证据文件.json>` → 实际重启 PostgreSQL/后端 → `verify` 验证遮盖、版本、历史回执和安全 PNG 保持；沿用上述 E2E 环境变量和独立验收数据库。CI 将它与既有素材/棋子检查共用一次真实服务重启。P02e 的独立整体验收已通过，最终证据见 docs/verification.md；P09 的完整备份/异机恢复仍未完成。

V5 保留原数据与回执，回填棋子/控制关系的战役键并增加同团成员外键及棋子命令回执。成员移除同时撤回本团全部控制，并推进相关棋子和场景版本；重新邀请不会恢复旧控制。升级不会创建新的角色资源。

P02e 组合重启验收见 `contracts/p02-acceptance.md`：在上述同套独立验收环境中，
`node scripts/check-p02-restart.mjs seed <仓库外新证据文件.json>` 创建同一团的两张
配图场景，保存不同 fog/校准、共享与隐藏棋子和已确认移动；实际停止/重启数据库和
打包应用后执行同文件 `verify`。脚本使用新会话逐项比较两个场景、原图 SHA-256、
安全 PNG 哈希和像素、版本及历史回执；再验证跨场景撤权/重加入不恢复控制。
CI 将这组组合检查与既有素材、棋子、地图三组检查共用一次真实容器重启，四组均须
通过。证据文件使用排他创建防止误用旧 seed，不放进源码库；这仍不等于 P09 备份恢复。

棋子重启验收使用同套环境变量：`node scripts/check-token-restart.mjs seed <仓库外新证据文件.json>` → 实际重启 PostgreSQL/后端 → `verify`。它创建独立验收战役，验证共享移动、隐藏/撤控、移除再加入、旧编辑版本冲突，以及新会话中名称/位置/隐藏/控制/版本/原回执保持。历史作者和移除回执不能恢复或再次删除新权限。CI 与素材脚本共用一次真实服务重启；该检查不等于 P09 异机恢复。

专用验收环境可复现文件与数据库重启检查：环境注入 `E2E_BASE_URL`、`E2E_DM_USERNAME`、`E2E_DM_PASSWORD`、`E2E_PLAYER_USERNAME`、`E2E_PLAYER_PASSWORD`（均不提供生产凭据），先运行 `node scripts/check-asset-restart.mjs seed <仓库外新证据文件.json>`，实际重启 PostgreSQL 和后端并等就绪，再以同一文件运行 `verify`。脚本会创建独立验收战役与素材，校验原始文件 SHA-256、地图引用、版本、重试回执、历史回放不重挂图以及其他战役/玩家/退出后下载拒绝；不会清理已提交测试素材。CI 在标准 Compose 的真实浏览器流程后执行这一检查。该检查不等于 P09 异机恢复。
## P03 character persistence and restart acceptance

V7 adds character sheets, independent controls, immutable edit history, command
receipts and same-campaign optional token references. Keep the existing database
and private asset volume; do not recreate them for upgrade. V1–V6 checksums remain
unchanged. Text/emoji avatars are sheet fields; bitmap avatar upload is not supplied.

Alongside the four existing restart checks, run
`node scripts/check-character-restart.mjs seed <new-outside-repo-evidence.json>`,
actually stop/restart PostgreSQL and the packaged backend, then run `verify` with
the same file. Use the existing E2E environment variables and dedicated test data.
This fifth group compares complete fields, sources/calculations, DM overrides,
resources, two token placements, access, actual-actor histories, target import
review, versions and historical receipts under fresh sessions. It verifies that
member removal/rejoining and old receipts never restore control or consume again.
New fixture names sort after Demo so the preserved original browser flows retain
their fixture. CI runs all five groups around a real standard Compose restart.

The check writes synthetic private evidence outside the source tree; it is not a
deployment backup or proof of the separately pending P09 restore on another host.
