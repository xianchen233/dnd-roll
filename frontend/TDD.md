# 前端 TDD 与验证记录

日期：2026-10-18（Asia/Shanghai）。测试运行环境：Windows、Node 24.21.0、npm 11.19.0。

## P01 固定账号、战役与邀请

### PR CI：移动确认与实时刷新的竞态

`dbe61cf` 的 push CI 通过后，pull_request run `39126870907` 的 browser job 在原 M1 `tabletop.spec.ts:84` 发现真实时序失败：位置已同步，移动状态却停在“服务器已接受移动；场景刷新失败”。直接刷新被随后 STOMP 刷新替代时，旧 refresh 返回 false，而后者没有完成移动确认，导致错误提示长期保留。

1. **20:25:19 RED**：先加入两个 deferred 用例，分别让直接读取/实时读取先完成；运行 `npm test -- src/scene-store.test.ts`，**2 失败、11 通过**，场景均已是新 version/位置，但 status 为 failed。没有修改 E2E 断言或添加重试。
2. 增加替代读取失败后再次刷新恢复、POST 确认前开始的快照不能确认移动、旧 403 晚于实时同步时仍清空地图三项边界。**20:26:32 RED**：同一命令 **3 失败、13 通过**；恢复确认用例也复现了旧提示不会收敛。
3. 保存 POST 已确认时的 refresh 序号；之后发起的最新有效快照均可完成同步确认。直接刷新被替代后不再覆盖后续读取的结果；实际读取失败保留已保存的事实与同步标记，后续刷新成功再确认。reset/撤权清理此标记，原 epoch、请求顺序、campaign 和 version 校验保持有效。
4. **20:26:50 GREEN**：`npm test` **5 文件、36 通过**；`npm run build` TypeScript/Vite 通过。约 551 kB 主 chunk 提示仍保留。协调者负责这次修复后的真实浏览器与远端 CI 验收，本代理未声称重新执行已通过。
5. 协调者复验时M1通过；P01在退出后立即导航重登录处超时，trace表明账号fill开始时退出尚未确认，所有移除/403断言此前已通过。先等待“进入冒险”入口可见再导航；**20:29真实Chrome GREEN：2条E2E通过（11.8秒）**。未放宽业务断言或加入retry；后端仍为原已验证jar。

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

## P02a 多场景（2026-10-18，Asia/Shanghai）

- **20:49:03 RED**：先添加 `scenes.test.tsx` 与 SceneStore 的切图回归，再执行 `npm test -- --run src/scenes.test.tsx src/scene-store.test.ts`：28 项中 **11 失败、17 通过**。缺少 DM 场景入口的 7 项断言失败、同 token ID 的新场景未清选择失败，以及失败移动重试/旧在途成功/失败跨场景的 3 项断言失败。玩家不请求备团信息的边界当次即通过。
- 实现仅 DM 的创建空场景与显式切换、场景 API/types、共享 campaignVersion 的新命令前重读、409 刷新、transport/5xx 同 requestId/body 显式重试。回执只作命令确认，列表与地图以重新读取为准；创建不本地激活。双方管理面板彼此失效刷新。地图或管理 401/403 清空地图和全部管理数据，卸载后的迟到成功不得恢复。
- SceneStore 引入独立的 scene generation；切图清除旧移动重试/待确认，隔离旧响应，即使随后又回原场景也不能确认旧移动。campaign epoch 未因切图改变，保留 P01 对同团迟到 401/403 的全局清除和 realtime 替代刷新竞态修复。
- **20:52:17 RED**：补旧移动 409 的冲突读取恰好切图场景，`npm test -- --run src/scene-store.test.ts`：**1 失败、19 通过**，实际新场景误标 `conflict`。在冲突读取后增加 generation 检查，保留新场景 `confirmed`。
- **20:52:41 GREEN**：全量 **50 项通过**，TypeScript/Vite 构建通过。P01 的全部 36 项保留；账号测试只补新的场景列表 API fixture，既有行为断言未放宽。
- **20:53:34 RED**：补“切换 POST 成功而地图快照 503”的组件测试，12 项中 **1 失败、11 通过**；旧文案错误宣称已重新读取当前场景。改为“切换请求已确认；全团当前场景以最新服务器快照为准”，地图另行明确刷新失败；已确认命令不再提供原 POST 重试。补管理命令 401/403 的清空回归，当次即通过。
- **20:54:22 GREEN**：`npm test` 全量 **6 文件、53 项通过**；`npm run build` 通过，入口约 556 kB 的既有优化提示仍保留。工具环境中 Python 命令不可用及中间测试参数/TypeScript 错误均已修正，不计业务 RED。
- 新增第三条真实服务 E2E `e2e/scenes.spec.ts`，保留原 M1/P01 两条原样；无 HTTP/STOMP mock。验收包括移动、创建不激活、玩家列表/激活 403、真实 frame 双端切图、返回后完整快照与版本保持，finally 恢复原 demo current。**本代理未运行真实浏览器**，由协调者统一启动后端/PG 并记录实际结果，不能把源码或 discovery 当作通过。

### 最终 QA：并发管理读取的晚到撤权

- **21:01:40 RED**：针对 `CampaignManagement.load()` 的 `Promise.all` 先拒绝边界新增 6 项 deferred 回归，运行 `npm test -- --run src/scenes.test.tsx`：**4 失败、14 通过**。四种真实失败顺序包括 members/invitations 先网络错误或 503、另一请求随后 401/403；实际断言确认地图仍在，两个管理区未及时清空。另两项验证一个请求 403、另一请求一直 pending 时必须立即清空，这两项初次即通过，不冒称 RED。
- 每个管理请求分别附加授权错误观察后再 `Promise.all` 聚合。后到 401/403 不再被较早 transport/503 的 aggregate rejection 吞掉；不等待其他挂起请求。active 守卫保留，晚到地图/备团列表成功和另一管理请求成功均不能复活撤权后的页面。
- **21:02:04 GREEN**：`npm test` 全量 **6 文件、59 项通过**，`npm run build` TypeScript/Vite 通过，`git diff --check -- .` 通过。既有 53 项与 3 条 E2E 均保留；真实 3 E2E 在本次修复后仍由协调者统一重跑。

协调者补充：20:56–57 已实际运行 `npm run test:e2e`，全部3条通过（14.8秒），原M1/P01加P02a双端切图；人工查看DM/player截图。20:58真实Java进程重启也通过，详细证据见 `docs/verification.md`。本机未运行Compose，新提交远端CI另记。

21:03协调者在最终晚到撤权修复后重新实际执行全部3条Chrome E2E，全部通过；无mock、无retry，原M1/P01断言保留。最终59前端测试/构建与本轮37unit/28IT、真实Java重启证据见verification.md。

## P02b 持久素材与地图备团（2026-10-18，Asia/Shanghai）

- 先读取 `contracts/assets.md`；不改变 Pixi 逻辑网格、P01 账号/邀请及 P02a 场景行为。
- **21:22:42 RED**：`npm test -- --run src/assets.test.tsx src/api.test.ts` 实际 **12 失败、7 通过**。API 3 项因缺少 assets/content 等操作失败；组件 9 项因缺少地图文件入口、校准、私密预览等断言失败，玩家不请求作者元数据的边界当次即通过。初次命令在工具返回后继续运行，第二次完整收集到相同失败结果；不把代码/测试 discovery 当作执行通过。
- 实现全部五项 API，multipart 的 command 为 application/json Blob，file 保留原 File；浏览器设置 multipart boundary，POST 使用会话 CSRF。内容通过 credentials/include 与 cache/no-store fetch 获取 Blob，保留非 JSON 代理错误状态；不把原图地址作为公共 img src。
- 实现当前场景 DM 原图列表/上传/Blob 预览/下载及校准/移除。新命令前读取最新 campaign/scene 版本；网络或 5xx 不确定结果保留相同 body/requestId/File；4xx 清除、409 刷新；回执仅确认请求，读回权威状态后才宣称同步。已确认后读失败明确提示未同步，不再重试 POST。
- 并发 assets/map 读取分别观察 401/403，较早网络/503 不吞掉晚到撤权、不等待挂起的另一个读。内容和 POST 同样观察授权；卸载、切场景/团、退出、撤权时清理 pending 和私密预览并 revoke Object URL，晚到成功不得恢复。新的测试同时覆盖先失败后拒绝、另一个读一直 pending、迟到内容/POST、相同对象重试、替换/关闭/下载 URL 清理、原图不进入玩家请求。
- **21:24:31 GREEN**：聚焦 19 项通过。**21:24:57** 全量 72 项及 TypeScript/Vite 构建通过；继续补充上述授权与回归边界。
- **21:27:33 RED**：聚焦共享 campaignVersion 回归 `npm test -- --run src/assets.test.tsx -t 'refreshes shared campaign'`，实际断言 `members.mock.calls.length` 为 1、预期大于 1，**1 失败**。上传会推进 P01 管理版本，旧接线仅刷新场景列表；修复 assetsChanged 同时失效成员/邀请、场景管理列表和权威逻辑快照。**21:27:50 GREEN** 同一聚焦测试通过；其余项按 `-t` 未执行，不能冒称全量通过。
- **21:28:49 GREEN**：全量 `npm test` **7 文件、83 项通过**；`npm run build` TypeScript/Vite 通过，主 chunk 约 564 kB 的既有体积提示仍保留。全部既有 59 项未删除或放宽。
- 第四条真实服务验收 `e2e/assets.spec.ts` 已编写；运行时生成有效 PNG、真实 DM 上传/Blob 预览/下载 SHA-256/保存校准、玩家 metadata/已知和猜 ID/直链 403、切图返回保持，并 finally 恢复原 current。`npx playwright test --list` 实际识别全部 **4 条**；**本代理未执行真实 E2E**，协调者统一启动服务并记录结果。旧三条完整保留。

### 第四条真实浏览器验收的定位器修复

- 协调者首次真实 E2E 中 P01 通过，assets 在 60 秒超时；超时耗尽后原 finally 的 API 清理被取消，空场景导致后两条旧回归继发失败。只读分析忽略目录中的 trace/error-context，未打印任何登录 fill 参数或凭据。
- Trace 证实上传、Blob 预览、真实下载事件、下载 SHA-256 和鉴权 content HTTP/响应头断言都已完成；卡在后续 `getByLabel('当前场景地图素材', { exact: true }).selectOption()` 的控件定位。包装 label 的 DOM 文本混入 option 文本，不能稳定使用精确文本查找；没有修改已验证成功的下载或 URL 清理实现。
- select 增加明确 aria-label，E2E 按精确角色/accessible name 定位，补名称回归。单步 action timeout 限为 10 秒；恢复原场景与关闭 context 移到自带 20 秒 teardown 预算的 fixture，防止测试 timeout 取消清理。所有业务断言保持；不增加 retry 或放宽总 timeout。
- **21:38:11** 全量 `npm test` **7 文件、84 项通过**；随后修正测试中的 Testing Library 类型参数并重跑 `npm run build` 通过。`npx playwright test --list` 继续识别四条；本代理没有运行真实 E2E，修复后实际重跑仍由协调者执行。

### PR CI：地图保存与并发权威读取的确认竞争

- 协调者报告 `cfdfccf` 的 PR run `39132907662` 中 frontend/backend 通过，标准 Compose 启动成功；browser 的原三条通过，assets 在保存后等待“地图请求已确认；当前配置已从服务器刷新。”失败。这次不修改 E2E 等待、断言、timeout 或 retry。
- **21:51:02 RED**：新增两个实际 deferred 回归，`npm test -- --run src/assets.test.tsx -t 'refreshes overlap'` **2 失败**。POST 成功后 run 自己的读与 scene.version revision 触发的读交错，无论 command-read 先返回还是 revision-read 先返回，较新的有效配置已读回，但旧 load 返回 undefined 使 run 永不显示同步确认。断言复现文案缺失，非环境/编译失败。
- POST 确认时保存当前 read 序号；同步确认由其后启动且仍为最新的有效权威读取统一完成，包含 revision/realtime/manual read。历史回执只确认命令，不覆盖地图；POST 确认前启动的读取不能确认同步。较旧读取的成功和失败都不能覆盖新状态；最新读失败显示已确认但未同步，之后成功读可恢复同步，无须重复 POST。并发授权观察及卸载/团/场景隔离保持。
- **21:51:39 GREEN**：相同两项聚焦测试通过。新增边界还覆盖最新读失败后的旧成功/旧失败、最新成功后的晚到旧失败、失败后手动权威恢复和确认前读晚到；实际值使用不同校准与 sceneVersion 验证旧状态未覆盖。
- **21:52:45 GREEN**：`npm test` 全量 **7 文件、90 项通过**；`npm run build` TypeScript/Vite 通过，约 564 kB chunk 提示保持。既有全部 84 项和四条真实 E2E 业务断言保留；本代理未执行真实 E2E，由协调者统一复验和核对新 SHA 的 push/PR CI。

协调者实际验收补充：21:39–40 修正精确可访问名称后，统一 `npm run test:e2e` 全部4条通过（16.8秒），包含原M1/P01/P02a和新素材流程；实际查看DM/player截图。21:36–37同库同素材根真正停止/重启Java与PostgreSQL，SHA/引用/版本/回执及下载隔离验证通过。详见docs/verification.md；新SHA远端CI另核。

协调者复验：修复确认竞态后的首次运行，原三条通过，assets刚登录即inputValue读取尚为空而失败；补与旧scenes流程相同的自动等待选团非空，不改变业务断言。21:54–55再次统一 `npm run test:e2e`：4通过（16.6秒），包含保存确认及原三条；0 retry。新的push/PR CI继续按SHA核对。

协调者远端补充：c237dc7 PR39133708410的保存确认已过，但网络断言误把公开生产 `/assets/index-*.js` 当私有素材。E2E路径改为精确的campaign assets/map API，仍断言玩家私有请求数组为空且保留全部直链403等检查；公开bundle路径应排除。本机dev无该生产路径，最终GREEN以新SHA标准Compose CI为准。

### PR CI：旧场景创建测试的初始化同步

- 协调者核对 `1b42c6c` 的 PR frontend：**89 通过、1 失败**，旧 `retains the exact body ... Error offline` 在寻找重试按钮时失败；同 SHA 的 push frontend 90 通过。CI DOM 显示名称已填写、创建按钮最终可用，却没有 offline 错误。`create()` 只等输入 label 就点击，初始场景列表仍在读取时按钮 disabled，fireEvent 点击被忽略；该 RED 属于测试初始化同步，不作为新的产品行为失败。
- 仅修改 `scenes.test.tsx`：helper 在填写后等待创建按钮 enabled 再点击；setup 补齐 assets/sceneMap 的正常只读 fixture，去掉缺少方法产生的无关错误。既有所有用例、重试 body/requestId 等业务断言保持；不改生产代码、全局 timeout 或 retry。
- **22:04:52 GREEN**：`npm test -- --run src/scenes.test.tsx` 18 项通过；**22:04:57** 全量 `npm test` 7 文件、90 项通过；`npm run build` TypeScript/Vite 通过，既有 564 kB chunk 提示保持。未执行真实 E2E、未改协调者的 assets API 路径断言；新 SHA CI 仍由协调者核对。

### PR/push CI：素材移除与动作就绪条件的全面检查

- 协调者报告 `a33bd99` 的 push/PR frontend 都为 **89 通过、1 失败**：原 detach 测试直接点击 `findByRole` 找到的按钮时，configuration 初始读取未完成，按钮仍 disabled，`saveSceneMap` 调用为 0。此次 RED 也是测试初始化同步；DOM 存在不能代替操作已可用，不改产品逻辑规避。
- 审查 assets/scenes 全部首次动作和重试/刷新动作，按钮点击统一先等待 enabled；upload helper 等文件输入可用，选择文件后再等上传按钮可用；create helper 同样等待输入及提交按钮。校准字段的动作保留先等待权威 gridSize=64，切团动作明确等待选择器可用。不会因面板已挂载而提前对 disabled 控件发 fireEvent。
- 原 detach 用例改成确定性的 deferred 初始读取：显式验证配置尚未返回时移除按钮 disabled、POST 为 0，完成读取后再等待按钮 enabled 并点击；保留原 sceneVersion=9、map:null 的命令断言。没有删除/合并用例或放宽原断言，也未更改全局 timeout/retry。
- **22:10:38** 聚焦 assets/scenes 2 文件 **46 项通过**；**22:10:46** 全量 **90 项通过**、构建通过。补齐输入/选团就绪条件后，**22:11:16** 再次全量 **7 文件、90 项通过**，TypeScript/Vite 构建通过。只改两份测试及此记录；四条真实 E2E 和容器重启的实际结果由协调者维护，未由本代理重复执行。
