# 前端 TDD 与验证记录

日期：2026-10-18（Asia/Shanghai）。测试运行环境：Windows、Node 24.21.0、npm 11.19.0。

## P01 固定账号、战役与邀请

基线为 c57c35a 的 14 项前端测试；只扩展 frontend，契约见 `../contracts/accounts.md`。

1. 先写 API 请求体/匿名 CSRF、空账号创建战役、PLAYER 隐藏管理、一次性 fragment 邀请、注册后显式登录、已有账号接受、409 刷新以及 401/403 清场景/待重试命令测试。**19:59:40 RED**：`npm test -- src/api.test.ts src/scene-store.test.ts src/accounts.test.tsx`，3 文件 **9 失败、11 通过**。API 方法尚不存在、找不到创建/邀请入口，撤权仍保留旧场景，均为实际行为断言失败。
2. 按契约实现 API 和中文组件，拆分 LoginForm、InvitationPage、CampaignCreate、CampaignManagement、Tabletop；保留 M1 地图/移动行为和原生 fetch receiver。**20:04:00 GREEN**：`npm test`，**23 通过**。原测试的 403 后保留场景断言随 P01 撤权契约改为清空；原匿名会话替身改用真实客户端相同的 ApiError(401)。此时首次类型构建发现新测试的泛型和查询选项类型错误，修复后才标记构建成功，编译错误不算业务 RED。
3. 补充撤销邀请版本、网络失败同 requestId 重试、注册成功但自动登录失败恢复、已登录账号接受失败显示测试；这批首次通过，属于回归。**20:05:10**：`npm test` **27 通过**；`npm run build` TypeScript/Vite 通过。`npm run test:e2e -- --list` 发现 **2 条**真实服务流程，未实际运行浏览器。
4. 协调者/只读 QA 指出恢复与并发边界，先补测试。**20:05:47 RED**：`npm test -- src/accounts.test.tsx src/scene-store.test.ts`，**4 失败、19 通过**：注册响应丢失后邀请已消费，preview 失败导致登录/接受入口消失；已移除用户无恢复接受入口；11 个 emoji 被 UTF-16 长度误认满足 12 字符；较早 refresh 的 403 被忽略后，另一在途旧授权快照恢复到地图。
5. 让 preview 失败仍能登录/接受，只有有效 preview 显示新注册；接受结果使用后端返回 campaignId。密码按 Unicode code points 计数并检查 UTF-8 字节。当前战役 epoch 内任一 401/403 立即清空场景/命令并推进 epoch，阻止全部在途旧响应。**20:06:21 GREEN**：`npm test` **5 文件、31 通过**；`npm run build` TypeScript/Vite 通过。

P01 浏览器脚本 `e2e/accounts.spec.ts` 使用现有 DM/PLAYER 环境账号作 setup，创建空战役、生成 fragment 链接、第三个独立 context 注册固定账号、DM 刷新/移除、真实 socket 关闭与重连/周期 HTTP 后地图清空、403、账号仍可登录。没有 mock API/STOMP。**代理仅运行了 `--list`；协调者随后于20:11启动真实打包后端、PG和Vite，运行 `npm run test:e2e`，2条均通过（13.6秒），含本流程与原M1双会话回归。** Vitest 仍仅匹配 src，Playwright 仅匹配 e2e。构建仍提示约 550 kB 主 chunk，尚未做首屏拆包优化。

## 框架 → 最小行为 RED → GREEN

1. 建立 React/TypeScript/Vite 框架、Vitest/jsdom 测试运行器及空 API/SceneStore 实现。先写会话 CSRF、失败保留位置、幂等重试、409 刷新与旧响应测试。
2. **18:34:01** 执行 `npm test`：2 个文件，**6 失败、1 通过**，退出码 1。失败包括登录 `Not implemented`、代理错误未抛出、已确认位置为 `undefined`、冲突状态未更新、旧战役响应未隔离。这是实际行为失败，依赖安装或类型错误未算作 RED。
3. 实现 HTTP 客户端与服务端快照状态管理。**18:34:39** 执行 `npm test`：**7 通过**，退出码 0。

## 组件 RED → GREEN

1. 先写登录失败不进入本地假战役、服务器裁剪后的单位呈现/禁用测试，用空 App 运行。
2. **18:35:02** 执行 `npm test -- src/App.test.tsx`：**2 失败**，退出码 1。页面找不到单位按钮和账号表单。
3. 实现中文暗色界面、账号表单、战役选择、地图与坐标移动、连接/待确认/冲突提示；实现 Pixi 初始化/销毁与 STOMP 个人队列失效通知。
4. 第一次全量运行发现 Testing Library 在当前 Vitest 配置没有自动清理，前一用例 DOM 残留。添加显式 `afterEach(cleanup)` 后，**18:37:44** 执行 `npm test`：**9 通过**；`npm run build`：TypeScript 和 Vite 构建通过。

## 补充边界回归

补充等待服务器快照才能确认、403 后禁止重试、切换战役忽略在途移动响应，以及 STOMP CSRF/个人队列/事件过滤/重连/周期刷新/清理测试。此组补充测试第一次运行即通过，属于回归证据，不冒称新的 RED 循环。

**18:38:31** 执行 `npm test`：**4 文件、13 测试通过**；执行 `npm run build`：TypeScript 与 Vite 构建通过，退出码均为 0。构建提示主包超过 500 kB，当前包含 Pixi 代码，可在后续首屏优化中拆包。

## 验证边界

- npm 安装 123 个包，安装输出审计结果为 0 vulnerabilities；`package-lock.json` 固定依赖。
- API 与状态测试使用请求替身；组件测试将 Pixi 地图组件替换为标记元素，实时测试替换 STOMP Client。
- 本记录没有声称真实 WebGL/WebGPU、双浏览器/触摸地图、端到端登录或服务器 STOMP 已通过；这些需要协调者的真实服务/浏览器验收。
- 权限隐藏由服务器裁剪；客户端只呈现收到的快照。组件测试不能独立证明后端权限安全。
- 当前仅实现探索坐标移动，未实现地图素材、平移缩放、角色卡、战斗、掷骰、RP 或备份。

## 协调者真实浏览器验收与回归

上述“未实测浏览器”是初次代理交付时的边界；以下为协调者后续实际运行的结果。

- **19:07** 首次真实Chrome双会话测试在登录处失败，页面显示原生 `fetch` 的 `Illegal invocation`。默认fetch被保存为Api对象方法后失去浏览器要求的receiver；请求替身没有暴露此问题。
- **19:08:34 RED**：新增原生receiver回归，运行 `npm test -- src/api.test.ts`，**1失败、2通过**。将默认fetch绑定globalThis后，**19:08:43 GREEN：14测试通过**，生产构建通过。
- 后续真实浏览器操作链路通过，但console检查发现缺失favicon的404；添加本地SVG favicon，没有通过过滤错误来隐藏问题。
- 截图视觉检查发现autoDensity生成的固定inline宽度把1152px画布裁进882px面板。新增浏览器尺寸断言，**19:10 RED** 实际得到 `expected <=882, received1152`；在每次renderer.resize后设置显示宽100%和height:auto，保持逻辑网格不变。
- **19:10:46 GREEN**：14项单元/组件测试通过，TypeScript/Vite构建通过；真实Chrome双context E2E通过。补充最右下区域网格点击，**19:11** 再次真实E2E通过。

真实E2E覆盖：CSRF/会话登录、DM看两个单位而玩家只收一个、原生Pixi canvas完整显示、表单移动、真实STOMP frame与另一页面更新、缩放画布右侧格子点击到(22,14)、刷新恢复、退出后的401、无非预期console/pageerror。截图保存在忽略目录 `test-results/`，不包含密码。当前仍有约539kB入口chunk提示，已记录到后续优化；没有为消除提示而放宽阈值。

## 真实浏览器验收准备

新增 `@playwright/test` 精确版本依赖、`playwright.config.ts` 和 `e2e/tabletop.spec.ts`，使用真实服务与两套独立 cookie 会话，无 API/STOMP mock。四项账号环境变量为必填；本机 Chrome 由 `E2E_CHROME_PATH` 指定，CI 默认采用 Playwright Chromium。产物存入已忽略的 `test-results/`、`playwright-report/`。

**18:49:32** 再次运行 `npm test`：13 测试通过；`npm run build`：通过。`npm run test:e2e -- --list` 成功发现 1 条完整双会话验收测试；发现测试不等于运行通过，真实执行结果由协调者在服务启动后记录。
