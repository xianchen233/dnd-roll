# 继续工作的入口

更新：2026-10-18，用户通过GitHub Desktop发布main，首轮CI前后端通过、Docker校验失败修复中。用户要求CI通过后才进入P01。**本文件用于上下文用尽或新会话接手，必须随着实际进度更新。**

## 用户已明确的授权与约束

- 已授权开始业务编码，并要求“整体框架 → 最小单元测试 → 继续代码 → 最终阶段”，按 TDD 执行，允许具体分工。
- 指定仓库 `https://github.com/xianchen233/dnd-roll.git`，应用位于独立 `dnd-roll/`。本地规则材料和索引不进仓库。
- 使用 frontend/backend/contracts/deploy/docs/.github/workflows 目录布局。
- PostgreSQL放在Docker中作为标准开发和部署入口；本机便携式PG仅用于当前真实数据库验证。
- 已有产品决策见 `product-design.md`、`design-map.md`、`decisions/`；无需重问设计目标。完整未完成工作见 `work-packages.md`。
- 用户要求交接不依赖聊天上下文；不要把当前骨架冒称为全部产品完成。

## 接手顺序

1. 在应用仓库运行 `git status --short`、`git log -5 --oneline`，确认远端与未提交文件；不要重建/覆盖已有仓库。
2. 读 `verification.md` 和 `implementation-plan.md`，再按当前工作包读取源文件与契约。
3. 核实实际运行进程和端口；聊天中报告过的服务可能已经停止。不要重复启动同端口数据库或并发运行会改同库的集成测试。
4. 从下面“当前下一步”继续；执行后更新本文件与验收记录。

## 已落地但需要区分验证层级

- 前端React/TS/Pixi、API/状态/组件/STOMP替身测试；最终14测试通过、TypeScript与Vite构建通过；真实Chrome双context流程通过。
- 后端Java25/Spring4.1.1、纯领域与应用/JDBC/会话/CSRF/STOMP/Flyway；最终16unit与9真实PG/HTTP/WS测试通过，0跳过。
- 已修复Jackson浮点到整数静默转换；握手时物化CSRF令牌；已补退出关闭既有WebSocket。
- 契约 OpenAPI3.1 与scene event schema已落地，`node scripts/check-contracts.mjs`通过。
- 已修复默认fetch原生receiver、画布autoDensity裁切、scene版本不递增与demo重启恢复撤权；均有实际RED/GREEN。
- Compose/NGINX/Dockerfile、PowerShell检查入口和CI已写，官方Maven Wrapper3.9.16已生成、校验并用于统一检查。
- 本地main已提交实现，初始实现提交为 `3e3aa70`；后续交接提交请以 `git log` 为准。工作区应保持干净。
- GitHub远端最初为空，**未推送成功**。CLI的Git Credential Manager无可用凭据，非交互push失败；已连接GitHub工具能读且metadata显示push权限，但实际create_file返回 `403 Resource not accessible by integration`，没有创建远端README或提交。不要因读取权限存在而继续假设可写，不要尝试绕过权限。
- 需要用户在Git Credential Manager完成GitHub登录，或为连接的GitHub App授予此仓库实际Contents写权限；然后重新核实远端历史并推送。用户已授权推送，无需重问是否推送，仅缺实际认证/权限。

## 当前下一步（按顺序）

1. 本地检查与重启回放已完成，详见verification；不用因为旧会话的中断或代理不可用重做已通过检查。
2. main已通过Desktop发布到79cf832，远端与本地初始两提交一致。首轮CI链接 https://github.com/xianchen233/dnd-roll/actions/runs/39124170015 ：frontend/backend成功，browser在Docker build失败；镜像无unzip使Wrapper自动改下载tar.gz，与固定ZIP的SHA-256不匹配。当前修复是在构建阶段安装unzip，保持相同ZIP与校验。推送修复再观察CI。CLI凭据仍未共享Desktop登录，必要时通过Desktop Push origin，不能标成尚未发布仓库。
3. 查看远端CI三项job：frontend、backend、browser。browser使用Compose构建并启动标准部署，验证生产代理链的HTTP/WS；本机无Docker，不能提前标为通过。
4. 有CI失败先修并添加必要回归；如CI未运行或权限受限，明确记录，不把配置存在当作执行成功。
5. 当前框架批次交接完成后进入P01正式账号与邀请，再按work-packages推进P02场景/素材、P03角色卡。后续能力未实现，不能因当前40项检查通过而关闭整个产品计划。

读取Actions时可用已连接GitHub的通用fetch读取 `/repos/xianchen233/dnd-roll/actions/runs`，再读取jobs/logs；专用fetch_commit_workflow_runs只筛PR事件，push会返回空。匿名API共享出口可能限流；不要重复匿名轮询。当前尚未开始P01编码。

## 当前机器的辅助环境（仅供接手定位，不是项目依赖）

这些下载都在应用仓库外，不应提交。其他机器按README配置正常JDK/PostgreSQL即可。

- 工作区：`D:/workspace/dnd`；应用：`D:/workspace/dnd/dnd-roll`。
- PowerShell7可用；Node24.21.0、npm11.19.0。Git如提示ownership，可对命令使用 `-c safe.directory=D:/workspace/dnd/dnd-roll`，不要放开全盘。
- JDK：`D:/workspace/dnd/.tools/java/jdk-25.0.4.1+1`，Maven：`D:/workspace/dnd/.tools/apache-maven-3.9.16/bin/mvn.cmd`，缓存：`.tools/m2`。
- 此机器C盘临时目录曾报权限错误，运行Java/浏览器测试时将 `TEMP`、`TMP` 指向工作区外层 `.tools/tmp`，并为Java设置 `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp`。这属于本机配置，不把路径写死到应用中，也不算行为RED。
- 临时PostgreSQL18.6：`.tools/postgres/bin`，数据`.tools/pg-data`，回环`127.0.0.1:55432`；Windows下测试实例使用 `huge_pages=off` 排除巨页权限错误。连接配置在仓库外 `.tools/test-database.json`，**读取成环境变量，不打印或复制密码到文档/聊天**。
- 本机Chrome：`C:/Program Files/Google/Chrome/Application/chrome.exe`；E2E可用 `E2E_CHROME_PATH`，CI使用Playwright Chromium。
- 本轮临时Java/Vite/PostgreSQL测试服务在交接时关闭；数据与本地截图保留，接手不要假设8080/5173/55432仍有服务。浏览器测试账号配置在仓库外 `.tools/browser-credentials.json`，数据库配置 `.tools/browser-database.json`，不要打印或提交密码。
- 之前沙箱网络受限；用户已切换完全权限。按当前工具策略执行，不要沿用旧会话的提权参数或旧进程编号。

## 续接后的默认方向

当前M0/M1收敛后优先P01正式账号/邀请与P02/P03场景和角色卡。每包按TDD推进，规则相关实现先用本工作区dnd-rule-lookup核对5R；不要用2014研究结果替代新版依据。M2整个首版完成后再按依赖进入模组与自动化。
