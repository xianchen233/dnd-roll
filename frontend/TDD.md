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

最终远端验收：代码1bfac83的push39135326549与PR39135329930均frontend/backend/browser success；90前端测试/构建及原三条加素材共四条真实Compose浏览器通过，随后数据库/后端容器实际重启的SHA/配置/版本/回执验证通过。详见docs/verification.md。后续文档提交按当前HEAD另核Checks。

## P02c 棋子作者与共享控制（2026-10-18）

最终远端闭环：代码`5160c0fdbb98a9bb189193f1ec8735d004ffb978`的push39141337705与PR39141340870均frontend/backend/browser success。112前端测试/构建/27契约、全部7条真实Compose浏览器通过；PR七条4.7m、push4.8m，仍60秒/条和0 retry。两个实际db/backend容器重启脚本均seed/verify PASS。下文保留先前RED与失败历史，最终新SHA已验证关闭，后续文档提交检查自己的Checks。

- 先读 `contracts/tokens.md` 固定契约。**22:43:11 RED**：`npm test -- src/scene-store.test.ts` 23 项中 3 项实际断言失败：撤权/隐藏后 `canRetry()` 仍为 true，撤权再授权后的旧重试多发一次 POST。实现当前权威快照清除命令并推进移动 generation，使旧异步成功不再启动读或恢复待移动状态。
- **22:45:05 RED**：新作者组件以可编译的空实现运行 `npm test -- src/tokens.test.tsx src/scene-store.test.ts`，作者 8 项因缺少实际名称/编辑/控制者界面断言失败，移动 23 项通过。随后实现 DM 当前场景列表、完整创建/编辑表单与 API；token/member 双读就绪、独立观察任一 401/403、作用域卸载隔离、409 放弃命令刷新、transport/5xx 同一 body/requestId 显式重试、回执只确认历史并重读权威。**22:46:10 GREEN**：同命令 31 项通过。
- **22:47:47 RED**：追加未保存草稿/旧版本测试，实际读回字段 `另一位 DM`，而应保留 `未保存名称`。实现后台刷新保留 dirty 草稿和最初 token version，剔除已退出成员的 controllers，显式提示版本变化；成功/409 才重置为服务器当前配置。**22:48:21 GREEN**：作者 9 项通过。
- 补充后最终覆盖：重叠 post-ack 读两种完成顺序、旧读失败不覆盖新确认、新读失败后无额外 POST 恢复、peer transport 失败后迟到 401/403 清私有状态并隔离 unresolved POST、token/member 任一旧拒绝压过新成功、跨场景旧 POST 隔离、完整 CSRF/路径/body 契约。Tabletop 在同场景隐藏/撤权清空选中与坐标，成员变动也刷新共享场景与作者列表；实时/15秒补偿读更新管理区。
- **22:50:44 GREEN**：全量 `npm test` **8 文件 108 项通过**，保留既有 90 项。唯一旧测试调整为适配作者新增独立成员读：素材上传前记录成员读调用基线，上传后仍断言有新刷新；旧功能断言未删除或放宽。新增 API 测试初次复用同一 Response 触发 body 已读，改为每次生成 Response；初次 build 的 Testing Library ByRoleOptions 不支持 exact 已修复，均属于测试/类型接入问题，未记业务 RED。
- **22:50:58 GREEN**：`npm run build` TypeScript/Vite 通过，约 572 kB 主 chunk 提示保留。`npx playwright test --list` 正确发现 **5 条**真实流程（既有 4 条保留，新增 tokens.spec.ts）；本代理尚未启动真实服务或运行 E2E，由协调者统一执行并记录结果。
- **22:53:09 / 22:53:21**：补充实际 App 层同场景隐藏/撤权→再授权→旧移动成功的两项回归，验证 selection/坐标清空、确认按钮仍 disabled、没有多发权威读，玩家不取 DM tokens/members；聚焦 18 项通过，全量最终 **110 项通过**，构建通过。新 E2E 首次动作显式等待 enabled，第二次生成邀请等待新 POST 成功且链接变更，避免读取旧已消费链接；业务断言和全局 timeout/retry 未放宽。

### P02c CI 长流程的实际时间轴与拆分

- a84ca8e 的 push run39138572141 / PR run39138575388：frontend/backend 通过，原四条真实浏览器流程通过；新增 tokens 单条超过既有 60 秒预算。只读核对下载的 push trace：UI 前置、邀请、双控制者创建在 31.67 秒完成；玩家移动在 40.65 秒确认；隐藏、秘密快照和旧移动 404 在 47.74 秒通过；公开和旧选择已清空在 57.83 秒通过。57.83–60.25 秒正在玩家重新选择单位，随后进入撤权前编辑操作；尚未提交撤权。单个 UI click/check 约 2.2–3.1 秒，真实 HTTP 多为 3–34 毫秒，没有某个业务断言挂死，失败是整体累计动作超时。finally 的 context.close 在 test ended 后遮蔽了准确操作位置。
- 按三个独立验收边界拆分 `tokens.spec.ts`：①完整 UI 登录、建团、邀请、双控创建、玩家移动及回执幂等；②真实 CSRF+login 和 HTTP 建立独立团/棋子前置，再由 UI 验证隐藏/公开、撤控/再授权和旧选择清理；③另一个独立团，由 UI 移除成员、生成邀请并重新加入，再验证旧作者及移动回执不能恢复控制。全部原检查保留，HTTP/STOMP 不 mock，不注入或伪造 session/cookie，不改生产代码。后两条仅把已经单独验收过的前置设置移至真实 HTTP。
- 新 sessions fixture 的关闭阶段具有独立 20 秒 teardown 预算，并行关闭两会话，避免业务用例的 finally 掩蔽失败。每条业务测试仍为 60 秒、0 retry；原四条 E2E 文件不改。拆分后的真实执行结果等待协调者记录，不能以 `--list` 代替验收。
- 拆分后协调者实际本地执行：前六条通过，第七条已通过移除/再邀请和历史回执权限断言，最终 `frames.length > 0` 在玩家事件集合为 0 失败。该案例真实 HTTP 前置发生于 socket 建立前，移除时玩家被排除且 socket 关闭，重新接受邀请发生于新 socket 建立前，之后历史回放正确不发事件，因此旧长流程的“玩家收到过事件”前置不再成立。仅修正 E2E 观察对象：fixture 独立收集 DM 帧，第七条验证持续在线 DM 实际收到的无秘密成员失效事件；第一、二条仍验证玩家帧。保留 `> 0`、全部秘密字段及权限/回执断言，不加延时或放宽 retry/timeout。修正后的完整真实运行等待协调者复验。

### PR 8339354：后台读与控制者复选框交互

- 协调者核对 push 的 7 条浏览器与容器实际重启通过，但 PR run39140229143 的第六条在隐藏/公开后撤控前失败：`locator.uncheck` 报 `Clicking the checkbox did not change its state`。不能用 push 通过关闭 PR 失败。审读发现后台 load 每次设 ready=false，使已有成员的 fieldset 短暂 disabled；另外重新选择编辑只清消息却留下旧 afterAck，后续后台读会重新插入旧确认横幅，改变表单位置。
- **23:32:06 RED**：新增两项 deferred 测试，原作者 14 项通过、新 2 项实际断言失败：已加载 checkbox 在后台重验证期间 disabled；旧确认消息在重新选择编辑后被晚到后台读插回。**23:32:39** 将首项加强为原生 `checkbox.click()`：mousedown 后启动 deferred 重验证再 click，实际断言“点击后仍 checked”失败，复现用户动作被丢弃；不是通过 fireEvent 人工绕开 disabled。
- 最小生产修复：后台读期间继续禁止提交，但已有成员的本地 checkbox 草稿仍可编辑；尚未完成初次权威双读、待命令确认/重试以及撤权后的锁定保持。choose 或开始修改字段时结束上个命令的确认周期，后台读不再插回旧横幅；当前命令 post-ack 最新有效读的同步确认和失败恢复继续保留。未改 E2E 的真实 check/uncheck、断言、timeout 或 retry。
- **23:33:35 GREEN**：聚焦作者 16 项通过。**23:33:38 GREEN**：全量 **8 文件、112 项通过**，TypeScript/Vite 构建与 `git diff --check` 通过。两个生产问题有确定 RED/GREEN；PR trace 尚在下载，因此此时不宣称已经确定该远端失败的唯一直接原因，后续仍需 trace 核对及真实七条复验。
- 随后只读核对 PR trace833，直接原因得到证实：失败 `uncheck` 在 monotonic 240477.847 ms 通过 enabled/stable 检查，240836 ms 后台 tokens/members 开始读取；241011.149 ms 的 action 快照明确目标 checkbox 所属 FIELDSET 已有 `disabled=""` 且 checkbox checked=true，241183.539 ms 才实际 dispatch click。after 快照 FIELDSET 恢复 enabled，但 checkbox 仍 checked，说明后台重验证跨越实际点击窗口并吞掉动作。同一 after 快照重新插回旧确认横幅，before/action 均没有该横幅，第二项也有直接快照证据。HTTP 本身约 7–13 ms；失败来自浏览器异步渲染/事件派发交错，不是后端响应挂死。两项最小修复与 trace 一致，不增加其他生产改动或放宽 E2E。
- **23:36 实际浏览器 GREEN**：协调者运行修复后的完整 7 条真实 E2E，**7 项全部通过（24.3 秒）**，日志位于仓库外 `.tools/p02c-browser-controls-fixed.log`。保留真实 check/uncheck 及全部权限/秘密字段/历史回执断言，每条 60 秒、零 retry；原四条回归一并通过。后续统一提交的 push/PR Checks 仍须按新 SHA 核验，旧 push 成功不替代本次修复的远端证据。
- 协调者23:22统一真实Chrome复验 `npm run test:e2e`：**7通过（23.2秒）**，原四条及三个独立P02c用例全部通过，0 retry。拆分与观察对象修正未修改生产代码；新SHA远端Compose/7浏览器/实际容器重启继续单独核验。

## P02d 安全地图、Pixi 交互与手动遮盖（2026-10-19）

先按 AGENTS 读交接/验收/工作包/设计/实施/CONTEXT 和契约，遵循新 `contracts/map-visibility.md`；前端仅改 frontend/**，Git 与根验收由协调者执行。后端秘密像素的实际服务端 RED/PNG/授权/事务证据由 backend/TDD.md 与根 verification.md 记录，前端不把客户端遮罩当秘密边界。

- **22:17:50 实际 RED**：`npm test -- src/map-api.test.ts` 共3项，2项断言失败：map-view 和绑定 scene/version 的 map-image fetch 都缺少 `cache: no-store`；图片403拒绝项通过。随后实现严格安全只读API（凭据、精确路由/版本、no-store）与独立图像生命周期，**22:22:02 GREEN**：map-api/map-store/map-geometry 合计13项通过。
- MapStore 在每次失效/重读前同步移除旧URL，撤销每个替换/卸载的对象URL；普通迟到响应按generation隔离。Tabletop 将手动刷新、真实失效、重连、周期读及作者确认统一为先清图再读。2项组件检查涵盖手动和共享实时回调，即使scene读取失败仍保持空图，重新读成功才恢复；公开单位继续独立显示，玩家无原图/DM fog请求。
- **22:25:53 实际 RED**：新增同campaign切scene时旧图片403测试，期望onDenied调用一次，实际0次；新的scene图片仍可显示。**22:26:17 GREEN**：将授权拒绝owner改为campaign生命周期，scene仅隔离普通成功/409/404；同campaign任一迟到401/403清掉最新图和整个桌面，不同campaign旧拒绝仍隔离。7项map生命周期检查通过，含旧图销毁、迟到下载、前campaign隔离、map-view版本不匹配与image409刷新。
- Fog作者组件13项通过：正反矩形统一canonical集合、坐标fallback、显式save前只改本地草稿、原scene version跨背景更新保留、transport/503精确同body/requestId重试并冻结冲突编辑、409权威重读、receipt历史结果不能覆盖当前状态、两种最新post-ack读完成顺序、最新读失败/旧成功不能误报同步、无额外POST恢复、pre-ack读不能确认、任一旧401/403清草稿并隔离迟到POST、跨scene/campaign卸载隔离。单位隐藏字段从未混入fog请求。
- **22:28:24 实际 RED**：2项原生Pixi组件断言失败：首次测距起点后重置/切工具，下一次点击误用旧起点，实际分别显示`25.00 距离单位`/`5.00 格`，应开始新起点。统一resetInteraction清gesture/measureStart/overlay后，**22:29:10 GREEN**：2项通过，随后扩至7项全部通过，含selected drag只预览/整数吸附、松开才提交并恢复权威位置、click移动、pending禁止移动、pointer pan、cursor zoom/reset、DM矩形只回调草稿、旧Image.onload隔离及替换/卸载texture/source销毁。
- Pixi对每张安全blob创建独立ImageSource/Texture，nearest采样，不进入全局Assets/URL缓存；layout cleanup在新状态绘制前移除旧层并destroy(true)纹理源。迟到Image.onload检查disposed。原有图片校准表单仍由DM备团区完成，游玩不获取原图。
- 保留全部旧112测试和既有7条E2E原样；旧App/accounts/scenes的API替身仅补正常map-view/fog只读接口，不放宽原断言。初次新增测试类型接入错误（可选scene弱类型、ByRoleOptions不支持exact）已修复，环境中`python`不可用改用现有Node写文件；这些不计行为RED。Tabletop测试首次复用同一对象导致React effect未触发，改为像真实JSON读取一样返回新快照对象，亦不计业务RED。
- **22:33:49–22:33:58 真实 Chrome GREEN**：协调者启动Vite+安全后端后，外置安全runner执行`npm run test:e2e -- maps.spec.ts`，**3 passed (8.7s)**，60秒/条、零retry。新增三条均独立真实团/CSRF会话：①玩家Pixi选择/吸附drag/click移动、欧氏测距×5、半途reset、pan/zoom/reset；②DM矩形揭露/遮盖显式save，浏览器实际解码安全PNG比较公开绿像素和秘密黑像素，原图/猜ID403、历史URL409、条件/Range200且no-store、隐藏token独立和STOMP无秘密；③坐标fallback实际cover后故意中断玩家snapshot网络，失效立刻清旧图、revoke对象URL且旧blob fetch失败，恢复读取后黑像素正确。未伪造HTTP结果/STOMP或注入session。
- **22:34:01 最终本地 GREEN**：`npm test` **14文件、148项全部通过**（旧112+新36），`npm run build` TypeScript/Vite通过，主chunk约583.57kB提示保留。新E2E三项本地通过；完整10条、实际重启及新SHA远端Checks由协调者统一记录，不能用此局部结果代替。
- 新截图在忽略目录：`test-results/maps-real-player-Pixi-supp-47286-t-pan-and-cursor-zoom-reset/{dm-pixi-tabletop,player-pixi-interaction}.png`，`maps-DM-Pixi-rectangle-rev-2642d-e-conditions-stay-protected/{dm-manual-fog,player-covered-safe-map}.png`，以及`maps-coordinate-fog-fallba-97ba0-d-recovers-with-covered-PNG/player-recovered-covered-map.png`。P02e、整个P02及后续工作包仍未完成；未合并/发布。

### P02d 首次远端失败后的交互生命周期修复

- 候选`d593092`的push run39271185130和PR run39271191222：frontend/backend均success，但browser均8通过、2失败。push两条新长流程在约34秒失败：selected drag后仍(2,3)，矩形后无“已揭露1格”。PR则两条均达到60秒：第一已走到测距，期望25.00却只显示起点(4,5)；第二已通过揭露，停在遮盖后的保存动作。原7条和新fallback均通过。本地10通过不能关闭远端失败。
- CI日志已实际阅读；49MB trace artifact多次因下载reset/timeout未完整取得，未声称trace已证明唯一直接根因。根据代码把安全图片/scene读取与交互effect一起拆掉的问题，先独立实际复现行为，再修复；新SHA仍需重新远端验收。
- **22:51:42 RED**：`npm test -- src/map-canvas.test.tsx`新增3项实际断言失败：同scene新快照或mapUrl失效穿插pointerdown/up后onMove为0次；DM矩形穿插mapUrl失效后onRectangle为0次。原7项仍通过。另新增4项保护边界：scene、selected、token version、控制变化时必须取消旧drag，防止为了保留手势误用更新后的目标或授权。
- **23:18:08 RED**：Fog新增后台重验证测试失败，已有草稿在ready=false时的原生手势按钮click被吞，实际仍[]而应有4格。不能只修Canvas闭包而保留这一独立吞编辑问题。
- 生产修复把稳定同scene交互层、棋子重绘、fog草稿绘制和安全图片资源分开。安全blob更替仍在layout cleanup立即清除sprite并destroy texture/source，不再拆stage handlers、gesture或测距起点；scene几何改变重建交互，selection/控制/token version/位置变化主动取消旧gesture，DM矩形也检查scene version。新增data-interaction-ready仅在stage绑定后出现，data-map-image=ready改为实际Image.onload后出现，E2E等待语义就绪而非睡眠。
- Fog已有权威版本的本地草稿在背景读期间保持可编辑，save仍等待最新读、pending/uncertain命令仍冻结全部冲突编辑。新草稿保留最初expectedVersion，后台新版本不会覆盖。**23:18:48 GREEN**：两文件28项通过。
- 追加安全清理边界：原带pending POST的401/403项本已锁定；**23:22:27 RED**新无pending命令的旧403检查发现手势按钮仍enabled。deny现在清version/dirty并由edit显式检查active，独立作者组件也立即锁定本地操作，迟到回调不能写草稿。**23:22:42 GREEN**：两文件29项通过。
- **23:19:47**全量156项和build通过；**23:20实际production preview**新3条通过(8.8秒)，native pointer按住期间由真实HTTP刷新安全图后再松开仍正确完成drag/reveal。协调者随后完整10条production preview通过(27.4秒)。这些为修复后的本地证据，不替代远端Checks。
- 为保留每条60秒/零retry并降低CI累计动作成本，将原新前两条分别拆成独立movement、measurement/pan/zoom、UI reveal、UI cover四条；加原fallback后为新5条、整套12条。所有旧业务断言保留，原5个E2E文件（7条流程）不改。cover只把已单独UI验收的揭露前置放入真实CSRF HTTP命令；没有伪造会话或响应。drag、reveal、cover都实际按住native pointer穿插权威读，测距两点间也穿插权威读；不以延长timeout或增加retry通过。
- **23:24:04最终GREEN**：全量`npm test` **14文件、157项通过**（保留旧112）；TypeScript/Vite build通过，主chunk约585.14kB提示保留。**23:24–25真实production preview GREEN**：`maps.spec.ts`新5条全部通过(11.0秒)，每条约1.6–1.9秒。协调者负责完整12条和新SHA标准Compose/重启/Checks记录。P02e及整个P02依然未完成，不以候选旧SHA或局部绿灯宣称交付验收完成。

### P02d bbdd54b：测距与视图操作继续拆分

- `bbdd54b` 两组完整job日志已实际读取：frontend/backend均success，browser均11通过、1失败。PR日志`job-118146963326.log`记录7.5分钟总耗时，唯一测距/平移/缩放长流程达到60秒，停在旧`:97 position()`准备zoom；因此测距、跨刷新保留起点、半途重置和平移断言已通过。push日志`job-118146945011.log`记录9.5分钟总耗时，同一测试达到60秒，停在旧`:96 drag()`中的position/boundingBox准备平移；push此时可确认测距/刷新/半途重置已通过，不能声称平移断言已完成。没有新增秘密像素、授权或手势业务断言失败。状态API长时间错误地保持in_progress，此处采用完整job日志结论，后续新SHA仍独立检查。
- 本次仅重新划分新增`maps.spec.ts`测试边界：把测距及半途reset，与pan/cursor zoom/viewport reset拆成两个独立真实战役用例。完整保留25.00距离、点间真实HTTP刷新、重置后新起点、pan截图变化、120%滚轮缩放、100%重置、玩家快照无隐藏token和页面错误为空的全部断言。各条仍60秒、0 retry，原5个E2E文件（7条流程）与全部production源码不改。没有为环境速度增加业务timeout、重试、硬等待或删减断言。
- **2026-10-19 23:49 GREEN**：`npx playwright test --list`确认**13条/6文件**；协调者启动同一真实后端和production preview后，安全runner执行`maps.spec.ts` **6 passed (13.8秒)**。movement 2.2秒、measurement 1.4秒、pan/zoom 1.5秒、reveal 1.8秒、cover 2.3秒、fallback 2.2秒。每条独立团/真实会话与HTTP/STOMP，图片与交互就绪检查保持。此次无生产改动，不重复构建或组件测试；既有157项/生产build结果保留，根完整13条与新SHA CI仍由协调者独立复验，不能用局部六项代替。
- README同步为新6条、完整13条；P02e与整个P02继续未完成，未合并或发布。

### P02d 88afda9：旧素材验收暴露场景命令 preflight 竞争

- 2026-10-20读取push run39281100563完整browser日志：新增6条均通过，旧`assets.spec.ts:92`切换回原场景后找不到`First scene`标题，10秒可见性断言失败；该条共42.7秒，并未达到60秒总时限。整套12通过、1失败(6.8分钟)。没有修改旧E2E来绕过失败。随后取得artifact12202015061的trace：切图click62845–64789ms，列表GET64381ms、背景列表GET64779ms均200；直到标题等待失败75085ms没有激活POST，76434ms仅fixture清理才发出POST200。直接证据与下述确定复现路径一致，不声称trace能观察JS回调顺序或证明唯一根因。
- **00:11:34实际RED**：新增`scene-management-race.test.tsx`，使用deferred让新命令preflight读与revision背景读重叠。create/activate各自按preflight先完成或背景先完成两种顺序执行，共4项业务断言失败：预期一次POST且expectedVersion=8，实际0次。旧load用readId同时裁剪UI和调用者返回值，背景读递增readId就使命令自己的成功读取变成undefined，run静默return。并发401/403胜出的2项保护测试原本通过。
- 最小修复仍仅最新readId更新显示的scene列表，但成功读向命令调用者返回其自身结果。命令保留自己preflight的campaignVersion=8，不因背景版本9而换用新版本；陈旧时由后端明确返回409。API/campaign owner和active检查隔离旧作用域、卸载及任何授权拒绝，不能用保留preflight为理由绕过拒绝。此轮不扩大修改其他组件。场景面板过时的“遮盖后续提供”提示同步改为“地图遮盖在上方地图区编辑并保存”。
- **00:13:41 GREEN**：新race6项与既有scenes18项共24项通过。随后追加第7项保护测试，模拟后端对preflight版本8明确409，断言只POST一次、不挪用背景9、不盲重试，界面提示冲突并读取当前列表。
- **00:14:14最终GREEN**：全量`npm test` **15文件、164项通过**，TypeScript/Vite build通过（主chunk约585.23kB提示保留）。原所有测试保留，旧5个E2E文件（7条流程）与新增maps6条都未修改。真实production preview单独执行原样`assets.spec.ts` **1 passed (4.5秒)**，业务流程3.6秒。根完整13条和新SHA push/PR Checks由协调者独立复验；不将本地单项通过或旧SHA绿灯当作交付证据。P02e及整个P02继续未完成，未合并/发布。

### P02d 静态画布按需绘制（2026-10-20）

- 88afda9的PR39281105409完整browser为12通过1失败(10.5m)，唯一pan/zoom达到60秒。取回artifact12201871918：pan截图变化比较、120%缩放、100%重置均已通过；最后缩放expect265609ms结束，snapshot GET265900ms200，最后screenshot265924–269052ms，总预算耗尽于截图。不能描述成平移/缩放业务断言失败。
- 测距及视图用例仍用真实DM登录/建团/上传/地图/棋子HTTP准备，仅不启动未使用的DM UI；其他四条地图用例仍双端，玩家真实会话/WS/图片/交互ready不变。完整13条本地生产preview通过32.8s。全部业务断言、60秒/条、0 retry及旧5个E2E文件保持。
- 本地已安装Pixi的TickerPlugin源码确认默认autoStart=true；当前静态地图没有动画却持续绘制。**00:38:10实际RED**：两项新组件断言失败，未禁用ticker、图片onload没有显式render。改为autoStart=false并在图片/单位/草稿/指针预览/测距/视图改变时绘制；失效销毁图片后同步render，不能在空闲画布留下旧像素。**00:38:35 GREEN**：Pixi16项通过，既有14项全部保留。
- 随后全量**15文件166项通过**，TypeScript/Vite构建通过(主chunk585.32kB提示保留)。生产preview完整**13 passed(30.3s)**，包括真实pan截图差异、矩形手势与刷新、秘密像素、旧URL和失败刷新清图；31操作契约检查通过。后端不变，既有61unit+60IT和本机三类真实重启证据保留；新SHA远端完整Checks仍独立验收。P02e和整个P02尚未完成。
- 协调者随后独立核验代码`7f539cf`的push39287462278/PR39287472000均全部success：166前端、61unit+60IT、标准Compose13流程及素材/棋子/安全地图三类真实容器重启全部通过，完整证据见docs/verification.md。P02d完成，P02e和整个P02仍未完成，未合并/发布。

### P02e 独立组合验收（2026-10-20）

- 按 `contracts/p02-acceptance.md` 新增独立 `e2e/acceptance.spec.ts` 四条，原六个E2E文件及13条业务流程原样保留，每条60秒、零retry。全部会话经真实CSRF/login取得，独立建团/邀请/PNG上传/地图/棋子前置均调用实际HTTP，无伪造session、状态响应或STOMP。
- 双端切换两个分别有校准/不同fog的场景，再返回逐项比较原场景快照与fog/version；玩家不能列举备团/读取非当前图。真实DM表单校准后fog原子全盖，解码玩家PNG确认两格均为不透明黑，公开棋子仍可控制、隐藏单位不进入玩家快照。
- 真实 `BrowserContext.setOffline(true/false)` 并断言 `navigator.onLine`：玩家离线移动产生可重试请求，DM切图并遮盖；上线重读当前场景/PNG，旧选择/重试消失、旧blob已撤销。另一独立团离线撤控，上线按钮禁用/选择清除且旧移动回执403；再次离线移除成员，上线地图清空，旧快照/已知派生图/原图路径均403。
- 响应故障注入与实际离线分开：`route.fetch`先让真实fog POST提交，再abort丢响应；明确保存待确认原body。另条实际cover提交后，历史retry与原JSON完全一致、不推进最新version或恢复已揭露像素；retry返回后临时阻断fog GET，界面保持“请求已确认；刷新失败，遮盖尚未同步”，解除故障并刷新后才显示同步确认。
- 首轮2过2失败属于测试准备/等待错误：offline移动只填X未填required Y，失败时DOM实际显示Y空、仍为“已读取服务器状态”，未出现移动待确认；fog重试按钮先渲染disabled，`toBeVisible`不足以等待`route.fetch`提交结果。补填Y并等待enabled后第二轮3过1失败：预先阻断GET使背景重读将ready置false，重试按钮按约定禁用；改为retry POST真实取得回执后才启用GET故障。没有生产代码修复，以上不冒充业务RED；初轮/第二轮日志由外置 `.tools/p02e-browser-initial.log` / `p02e-browser-second.log` 保存。
- 最终 `../.tools/run-p02d-browser.ps1 -Spec e2e/acceptance.spec.ts` 对真实打包后端及生产preview **4 passed (11.9s)**，单项2.1/1.5/4.9/1.6秒。日志 `.tools/p02e-browser-third.log`。新截图位于忽略的test-results四个用例输出，页面错误均为空；本轮是新增验收覆盖，无生产变更、不虚构RED。完整17条、真实PG/进程重启及当前SHA标准Compose/远端Checks由协调者独立运行，未以本局部通过提前关闭P02e/P02。
- 随后只读审查加强三处验收边界：原 `setOffline`/navigator与HTTP失败仅证明浏览器HTTP离线，不能据1.5秒流程声称经历3秒STOMP重连。现将真实browser offline与捕获的原生WebSocket `close()`组合注入，等待Node实际close事件/重连状态，上线后要求新socket和真实CONNECTED帧，未伪造STOMP；两个页面的初始CONNECTED亦先实际观察。成员移除等待真实204并读members确认不存在后才上线。切回first同时等DM标题和该场景校准表单，断言实际map POST目标为first、offsetX=1及服务器读回offsetX=1，避免只保存旧配置也因清fog而误过；fog故障用例先确认一格权威初态、两格草稿与实际提交两格。上述为测试可靠性/证据精度加强，无生产变更，新版本尚未执行，由协调者统一运行，不能沿用此前4/17条通过冒充加强后结果。
- 协调者随后实际执行加强版本：新四条 **4 passed（18.4s）**，单项2.6/4.7/7.6/1.7秒，日志 `.tools/p02e-browser-strengthened.log`；真实原生socket关闭及新CONNECTED断言均实际通过。首次即通过，无生产修复，完整17条和新SHA全部CI另核实。
- 最终加强版完整真实 Chrome / 生产 preview **17 passed（46.4s）**，日志 `.tools/p02e-browser-strengthened-full.log`；原六文件13条未改，每条60秒、零retry。后端/源码/构建产物/重启脚本未变，沿用同轮实际通过证据，新SHA远端CI仍独立确认。
- 最终验收SHA `853944d` 的 push39436631381 / PR39436640191 六个job全部completed/success；166前端/构建、61unit+61IT、标准Compose17条（push2.1m/PR1.5m）与四组实际容器重启全PASS，完整日志由协调者独立读取并记录在 docs/verification.md。全部门槛通过后P02e/P02完成；后续文档SHA仍独立核对Checks，PR保持草稿、不合并/发布。P03及整个M2继续未完成。
# P03 当前证据（2026-10-20，未完成）

22:52:18新5项实际RED：缺新建/打开角色入口；22:58:33同5项GREEN，覆盖新建完整
字段、原草稿版本、精确不确定重试/权威读、撤权与旧响应隔离。JSX标签、命令路径及
Testing Library参数的中间编译/准备失败单列不冒充RED。23:00:05全量171（旧166保留）
及生产构建通过，主chunk609.74kB提示保留。
23:05:02对畸形import.classes对象not.toThrow断言实际RED；检查预览字段形状后
23:05:22新增6项GREEN。日志外层.tools/p03-frontend-*、p03-import-preview-*。
实际浏览器新6条和旧17条尚未运行，不标通过；P03/M2未完成。
## P03 最终本地回归（2026-10-20）

后续实际断言RED与日志：p03-import-conflict-red（409后列表未刷新）、p03-named-links-red
（缺命名关联选择）、p03-placement-conflict-red（冲突表单残留）、p03-create-selection-red
（ACK后旧列表清选择）。分别补目标版本刷新、按来源/场景棋子名称选择并保持原ID版本、
409销毁旧权限/关联表单、ACK同步失效旧列表读序号。单次修改不自动升级重发旧命令。
最终角色11项及完整177项通过，构建通过（p03-build-final-production，610.88kB提示保留）。
最终真实Chrome完整23 passed（58.0秒），原17及全部旧文件/60秒/零retry保留；新6条
包括真实socket offline/新CONNECTED、提交后丢响应/同body retry/失败权威读恢复和
跨战役下载/复制/导入。首次离线事件准备错误与后续验收团排序干扰旧2条均如实保留，
没有删断言或放宽时限。最终构建的源与结果对应关系见verification，未拿旧构建冒充。
P03最终代码f66c4e731f174c81be0ee7ab9724db30c0827446的push39452554961和
PR39452563178六job全success；完整日志确认65unit+75IT、177前端/构建、45契约、
标准Compose23浏览器与五组实际db/backend重启全PASS。至此P03完成；整个M2及P04–P10/A
未完成。上文pending/失败均为历史真实时点，由最终证据关闭，未伪造RED或跳过检查。
完成状态文档后续HEAD仍独立核对，见docs/verification.md与continuation.md。


## P04 规则检索与填卡（2026-10-21）

00:25:36 先写 saved character workspace 的6项测试并实际运行：6 failed，
均缺少“规则检索与填卡”入口，p04-frontend-red.log；不把未走到的后续断言当RED。
接入ContentPanel、客户端和服务器预览后00:27:24六项GREEN；
全量183通过。类型检查曾因误用Testing Library role exact选项失败，修正为受支持
调用；一次编辑脚本cwd错误未改文件，单列准备故障，未当行为失败。

真实Chrome首轮4场景：2passed、2failed，精确getByLabel等待条目类型/自定义体系
select超时；trace外置保留。显式aria-label后00:35四条11.5秒全部通过，未改timeout、
业务断言或retry。涵盖SRD许可与DEX明确选择/预览/不执行效果、玩家发布2014兼容
条目/DM准入/快照、已提交丢响应的原body重试及失败读恢复、真实offline加原生
WebSocket close/新CONNECTED后已提交隐藏撤权。增加精确accessible-name覆盖首次
即通过，不虚构为单测RED；最终全量计数见verification。

旧P01/P02/P03的全部测试和23条浏览器文件保留；新P04是独立e2e/content.spec.ts。
规则管理不确定命令保存原body；填卡沿用P03命令状态机，ACK后最新权威读取才同步。
草稿/切团/切角色/撤权/刷新隔离旧检索和预览；只允许服务器确定的字段变化。

最终17文件186通过、TS/Vite生产构建通过；原23+新4共27真实Chrome生产流程全部
通过（1.2m），p04-frontend-final-3.log / p04-build-final-3.log /
p04-browser-full-final.log。新增DM准入断言初次匹配option与正文两处，限定目录
查询后通过，不计作行为RED。新公开目录采用原浅色面板并人工检查截图。
