# Docker Compose 部署

标准运行方式为应用与 PostgreSQL 分开容器，通过内部网络连接。需要 Docker Engine/Desktop 和 Compose v2。当前主机尚无 Docker，本轮本地数据库测试运行的是同版本 PostgreSQL 原生二进制；容器构建/启动结果以验收记录为准。

在仓库根目录：

```powershell
Copy-Item .env.example .env
# 编辑 .env，为数据库及两个 demo 账号设置不同密码
docker compose --env-file .env -f deploy/compose.yaml up --build -d
docker compose --env-file .env -f deploy/compose.yaml logs -f
```

访问 `http://localhost:8080`（可在 .env 中调整 APP_PORT）。初始账号名默认为 dm 和 player，密码使用你设置的值。demo 配置显式创建初始战役和棋子，用于本轮验证；关闭 demo profile 不会删除已经创建的记录。

Compose 默认只绑定本机回环端口，数据库不发布宿主机端口。上线时在宿主机配置域名、TLS 反向代理及会话 Cookie 的 Secure 属性，再按需调整发布接口。开发环境通过 Vite 的 `/api`、`/ws` 代理访问后端，不另行开放跨域权限。

HTTPS 入口应由可信的本机反向代理终止TLS，再转发到 `127.0.0.1:APP_PORT`；外层代理必须保留原始 Host，并**覆盖** `X-Forwarded-Proto: https`，同时转发 WebSocket Upgrade/Connection。内层NGINX清除客户端 `Forwarded`/转发端口头，重建 Host/协议并交给仅在内部网络可达、已启用 `forward-headers-strategy=framework` 的后端；这样HTTP会话与WebSocket同源判断使用同一浏览器域名和协议。`.env` 中另设 `SESSION_COOKIE_SECURE=true`。如发布自定义外部HTTPS端口，Host须包含该端口。不要把当前依赖可信本机代理的监听器直接改为公网任意客户端可访问，否则必须先重新定义可信代理边界。TLS证书与外层代理仍需发布环境实测。

数据库使用命名卷 `postgres_data`，挂载在 PostgreSQL 18 官方镜像要求的 `/var/lib/postgresql`，重建容器保留数据。`docker compose down` 保留卷；不要使用 `down -v` 清理需要保留的战役。卷本身不能替代异机备份。产品级每日自动备份/恢复是 M2 工作，本轮只建立持久化基础。

当前固定 PostgreSQL 补丁、前端锁文件与 Maven 版本；基础镜像中 Java/NGINX 使用维护标签，发布阶段再固定经验证的镜像摘要并建立安全更新流程。该配置不代表已经完成容量测试、TLS 运维或备份恢复验收。
