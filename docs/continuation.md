# 继续工作的入口

更新：2026-10-18。当前分支 `feat/accounts-and-invitations`，3c12f8d主功能、dbe61cf交接和dabef40竞态修复均已推送；本地最终36项前端测试、构建及2条真实E2E通过。最新远端状态直接核对[PR #1 Checks](https://github.com/xianchen233/dnd-roll/pull/1/checks)，交付前须确认当前提交全通过。最新文档提交只同步这轮实际结果；Git status/log是当前HEAD的依据。

**CI并发回归修复：** dbe61cf push run39126867019三个job通过，但PR run39126870907 browser失败：原M1移动已保存，直接refresh被并发STOMP refresh替代，界面误报刷新失败。20:25/26真实deferred RED后，记录POST成功时的refresh序号，由之后最新有效快照完成确认，不再让被替代读取覆盖结果；保留旧403清空与POST前快照隔离。20:26:50前端36测试/构建通过，20:29实际Chrome两流程再次通过（11.8秒）。复验另发现E2E退出后立即导航的顺序错误，已等待真实登录入口再重登录，未放宽产品断言或加retry。后端无变，原30unit/18IT及真实进程重启证据保留。CHANGELOG/verification/TDD均记录此轮。
**基线c57c35a已推送，第三轮CI全通过；已按用户要求通过CI后才开始P01。**
本文件用于上下文用尽或新会话接手，必须随实际进度更新。

## 已明确的用户授权与约束

- 已授权持续编码及具体分工，按“整体框架 → 最小单元测试 → 实现 → 阶段验收”推进，遵循TDD。
- 仓库 `https://github.com/xianchen233/dnd-roll.git`；本地 `D:/workspace/dnd/dnd-roll`。
- PostgreSQL容器为标准部署；本机原生PG只用于本地测试。目录沿用frontend/backend/contracts/deploy/docs/CI。
- 规则基线5R、既有产品决策见product-design/CONTEXT；不要重问已经决定的目标。当前P01没有D&D机制实现，无需规则查证。
- 用户要求CI通过后进入下一阶段；已满足M0/M1门槛。新阶段仍需自身测试和CI通过，不能只引用旧基线。
- 用户要求及时维护交接记录，并按Keep a Changelog维护根CHANGELOG.md；实际功能完成再加Unreleased条目，不伪造发布版本。

## 接手顺序

1. `git status --short`、`git log -5 --oneline`、检查当前分支/远端；保留未提交实现，禁止重建、reset或force覆盖。
2. 读verification.md、work-packages.md及当前契约 `../contracts/accounts.md`，按下方下一步继续。
3. 核实实际进程、端口和代理状态；不要并发运行会修改同一数据库的集成测试。
4. 代理是否仍可用不是续接前提；源码、测试、TDD证据和本文件才是依据。

## 已完成且有实际证据的基线

- M0/M1：Java25/Spring4.1.1/PostgreSQL18.6、React/TS/Pixi、session/CSRF、权限过滤、网格移动、乐观版本、持久幂等回执、私有STOMP失效通知与重连。
- 本地16后端unit、9真实PG/HTTP/WS、14前端测试、真实双Chrome上下文、构建和打包进程重启回放通过。
- 曾发现并修复原生fetch receiver、autoDensity画布裁切、严格整数JSON、WS的CSRF/注销、scene版本、demo重启恢复撤权。实际RED/GREEN保留在backend/TDD.md和frontend/TDD.md。
- 初始实现3e3aa70；交接79cf832。首轮CI的Docker无unzip导致ZIP SHA失败，d903d40补unzip；第二轮容器就绪curl错误56，c57c35a补有界retry-all-errors。
- [基线CI成功](https://github.com/xianchen233/dnd-roll/actions/runs/39124712714)：c57c35aed9c7a52c11ab598937a5b537654eda64，frontend/backend/browser三个job均success，包含标准Compose镜像构建、启动和真实浏览器流程。
- 尚未进行公网部署、完整M2业务、团测、容量测试、完整备份及异机恢复，不因框架通过CI而关闭这些工作包。

## 当前P01工作与责任

契约详见 `../contracts/accounts.md`。代码已提交推送3c12f8d；[草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)已创建并附到聊天。P01代码CI三个job全部success，尚未合并/发布。代理均已交付，其可用性不影响接手。

| 范围 | 当前责任 | 工作 |
| --- | --- | --- |
| backend/** | p01_backend | Bootstrap固定账号、战役创建、邀请/注册/接受、DM成员管理、版本/回执/撤权；先RED再实现及真实IT；V2迁移不能修改V1 |
| frontend/** | p01_frontend | 创建战役、DM管理、fragment邀请页、受邀固定账号、既有账号接受；401/403清除地图；unit与真实E2E |
| contracts/openapi.json、scripts/check-contracts.mjs | p01_contract_qa | 扩展机器契约与检查，随后只读安全审查 |
| 其余contracts、docs、deploy、CHANGELOG、Git/CI/整合 | 协调者 | 契约协调、环境、最终浏览器与CI验收、统一提交 |

重要约定：邀请明文仅首次返回，数据库及回执只存hash/metadata；重复创建返回token:null而不再发明文；分享放URL fragment不进入服务端日志。受邀注册事务内创建账号、成员与claim；重放已消费邀请不能恢复被移除成员。管理命令使用campaignVersion和持久requestId，独立于scene/token版本。P01新战役为空网格，作者入口属于P02。

## 下一步

1. 本地最终证据已在verification.md：30后端unit+18真实PG/HTTP/WS、36前端unit/组件、构建、16HTTP操作契约、2条真实Chrome流程和真实Java进程重启全部通过。V1既有库已成功升级V2，M1回归保留。
2. Root已核查实现、权限/回执、前端异步撤权，人工查看实际截图；README/deploy已切bootstrap owner，CHANGELOG按Keep a Changelog记录P01。
3. 3c12f8d的[CI](https://github.com/xianchen233/dnd-roll/actions/runs/39126498540)全通过（frontend/backend/browser），这是P01实际代码证据。PR创建也会触发独立pull_request CI；最新文档提交亦应核对当前CI，不以旧代码基线绿灯冒充代码改动的结果。
4. 最新修复提交的CI以PR Checks为准；若任何job失败先修复，不通过重跑掩盖真实断言失败。保留草稿待审阅，未合并main，不是已发布版本。下一实现为work-packages.md的P02a，先契约和最小RED，不直接铺开P02全部功能。
5. 已清理仅属于本任务的Java/Vite/PG进程，测试数据及产物保留。P01之后再按依赖进入P02场景/素材和P03角色卡，M2其余包仍待实现。

中途审查已发现并通过真实RED/GREEN修复：邀请消费后preview失败仍可登录并恢复accept；Unicode密码字符数两端一致；任一当前epoch的401/403都会废弃在途旧成功响应，不能重新显示被撤权地图；邀请到期时间统一PG微秒精度。测试专用Hikari池3避免小PG连接上限，生产配置未变。实际Java重启后权限/邀请/管理回执均已验收，不仅调用bootstrap模拟。

## GitHub与CI访问

- 用户先通过Desktop发布，随后完成Git Credential Manager设备授权。CLI `git push`已成功，自动推送可用；不要再让用户反复登录Desktop。
- GitHub连接器读取Actions可用；先前写Contents返回403。正常代码推送用已认证Git，不记录或打印凭据。
- PR写入连接器同样403，已用本机Git认证为同一GitHub账号创建PR #1；辅助脚本只在内存使用Git提供的凭据，不落盘/打印。后续更新PR可用相同本机认证，无需再次向用户要求登录。
- 通用GitHub fetch可读 `/repos/xianchen233/dnd-roll/actions/runs`，然后用jobs/logs工具读取；专用fetch_commit_workflow_runs只筛PR，push可能空。匿名API共享出口可能限流，避免重复匿名轮询。
- CI包含frontend、真实PG backend、Compose部署browser。每次以目标提交SHA核实结论。

## 本机辅助环境（均在应用仓库外，不提交）

- PowerShell7、Node24.21.0/npm11.19.0。Git ownership提示时仅对该命令加 `-c safe.directory=D:/workspace/dnd/dnd-roll`。
- JDK `.tools/java/jdk-25.0.4.1+1`；Maven `.tools/apache-maven-3.9.16/bin/mvn.cmd`；依赖缓存 `.tools/m2`（均相对于D:/workspace/dnd）。
- Java命令设置JAVA_HOME；TEMP/TMP设 `D:/workspace/dnd/.tools/tmp`，JAVA_TOOL_OPTIONS设 `-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp`，避免此机器C盘临时目录权限故障。Maven的-D参数用完整引号，避免PowerShell拆点号版本。
- PG已正常停止，data `.tools/pg-data`仍保留，重启用pg_ctl指定127.0.0.1:55432和huge_pages=off。测试连接 `.tools/test-database.json`；browser库连接 `.tools/browser-database.json`。**读取为环境变量，禁止打印/复制密码。**
- Java/Vite测试服务已停止；PID文件仍保留，后续不可假设该PID仍属于旧进程。browser测试账号 `.tools/browser-credentials.json`，默认E2E需四个账号变量；本机Chrome路径 `C:/Program Files/Google/Chrome/Application/chrome.exe`。辅助脚本 `.tools/start-p01-browser.ps1` 和 `.tools/smoke-p01-restart.ps1` 均在应用仓库外，不提交、不打印凭据。
- 当前完全权限；不要沿用旧沙箱的提权参数，也不要假设中断后的进程和代理仍在运行。
