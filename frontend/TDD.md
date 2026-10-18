# 前端 TDD 与验证记录

日期：2026-10-18（Asia/Shanghai）。测试运行环境：Windows、Node 24.21.0、npm 11.19.0。

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
