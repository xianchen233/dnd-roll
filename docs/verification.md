# M0/M1 验收记录

日期：2026-10-18（Asia/Shanghai）。状态：M0/M1本地功能验收完成；远端提交与容器CI结果见续接记录。

## 测试与构建

| 检查 | 实际命令 / 环境 | 结果 |
| --- | --- | --- |
| 契约 | `node scripts/check-contracts.mjs` | 7个HTTP操作、CSRF/会话、移动/幂等、快照与无棋子数据事件检查通过 |
| 后端单元 | Maven Wrapper3.9.16，Java25，`scripts/check.ps1 -Integration` | 16通过；无需Spring服务或数据库的unit |
| 后端集成 | 同一检查入口，真实PostgreSQL18.6 | 9通过、0跳过：HTTP5、原生WebSocket4；事务回滚、并发单胜和同请求并发幂等、撤权后重启保持均覆盖 |
| 前端 | `npm test` | 最终14通过，4个文件；API/状态/组件/实时替身检查 |
| 类型与产物 | `npm run build` | TypeScript及Vite通过；539kB主包提示保留为后续优化 |
| 真实浏览器 | `npm run test:e2e`，本机Chrome，两独立context，真实后端与PG | 1个多步骤场景通过；没有mock HTTP或STOMP |
| YAML静态检查 | 解析CI/Compose，核对3个job/3个服务、数据库无外部端口、PG18卷路径 | 通过；此项不能证明容器已运行 |
| 打包进程重启 | 移动并确认 → 停止Java → 同库重启 → 新会话重放原requestId | 原回执、位置、token/scene版本保持，不重复应用 |

统一检查使用官方only-script Maven Wrapper，Maven zip SHA-256已锁定。Windows本机临时目录权限问题通过将TEMP/TMP和java.io.tmpdir指向工作区外层 `.tools/tmp` 解决；环境故障没有记为TDD行为RED。

## 真实浏览器验收步骤

1. 使用环境注入的两个测试账号登录，DM与玩家各自独立Cookie会话。
2. DM看到Adventurer和Hidden enemy，玩家列表与HTTP快照仅含Adventurer。
3. 检查真实canvas完整置于面板内、宽高比例匹配24×16逻辑网格；客户端实时连接成功。
4. 玩家用坐标表单移动，确认服务器已保存；DM实际接到SCENE_CHANGED帧并自动更新位置。
5. 玩家点击缩放画布的(22,14)格，双方显示正确逻辑坐标；刷新仍读取相同位置，玩家仍无隐藏单位。
6. 保存DM/玩家截图，退出双方账号，认证接口变为401；没有非预期浏览器控制台/脚本异常。

截图由Playwright生成在 `frontend/test-results/tabletop-two-real-sessions-c8daf-ceive-realtime-invalidation/`，未纳入Git。复现命令及四个必填账号环境变量见前端README。

## TDD

领域与安全测试记录见 `../backend/TDD.md`；前端测试记录见 `../frontend/TDD.md`。只把实际预期断言失败计为 RED；安装、网络或编译器故障单独记录。

## 当前范围

本轮交付仓库框架与最小纵向切片；完整首版业务、模组与自动化见实施路线。demo 是显式启用的开发入口，账号密码从环境提供。正式的邀请、账号管理与场景作者入口尚属后续工作。

## 尚未验证与后续工作

- 本机无Docker：没有在此机器执行镜像构建、Compose启动或容器重启。远端CI的browser job会通过标准Compose部署运行同一真实浏览器测试；必须查看实际CI结果才能标为通过。
- 外层TLS证书及反向代理链只完成配置与说明，仍需部署环境验证HTTPS和WSS。未部署到公网。
- 尚未进行完整测试团、手机触控、性能/容量测量、完整备份与异机恢复。这些属于后续业务/发布阶段，未因本轮验收而标为完成。
- 首版完整业务与模组/自动化仍待实现，按 `work-packages.md` 的P/A工作包继续。
- 本地实现已提交。CLI push缺GitHub凭据，GitHub连接器写入返回403，因此尚无远端CI运行结果；解决认证/Contents写权限后推送，再验证CI。没有把CI配置的存在记为执行通过。

## 远端CI首次执行

2026-10-18，用户通过Desktop发布79cf832后，运行 [Build and test #1](https://github.com/xianchen233/dnd-roll/actions/runs/39124170015)：frontend与backend通过；browser在后端Docker构建时失败，还未执行浏览器步骤。Temurin构建镜像没有unzip，only-script Wrapper将ZIP替换为tar.gz，导致与ZIP的固定SHA-256不符。已为构建阶段补unzip，保留校验，需推送并等待新CI结果。此项为真实部署RED，不是业务单元测试失败。

[Build and test #2](https://github.com/xianchen233/dnd-roll/actions/runs/39124457926)（d903d40）：frontend/backend仍通过，前后端镜像均成功构建、PostgreSQL健康；紧接容器启动的curl遇到连接重置(56)，原有retry未涵盖此错误，browser尚未执行。就绪检查改为重试所有暂时错误、单次5秒、总重试120秒；没有跳过就绪检查或放宽业务测试断言。
