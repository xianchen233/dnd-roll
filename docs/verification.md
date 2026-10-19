# 阶段验收记录

## P02d 开工核对（2026-10-19）

当前分支 `feat/accounts-and-invitations`，开工 HEAD
`0bacdab378e18886f78fdcd4199937f17c48ac87`，工作区干净，fetch 后与 origin
一致。草稿 PR #1 仍 open/draft、base main；按这个 SHA 独立核对
[push39143471892](https://github.com/xianchen233/dnd-roll/actions/runs/39143471892) 与
[PR39143474706](https://github.com/xianchen233/dnd-roll/actions/runs/39143474706)，两者
completed/success，frontend/backend/browser 全部 success。

先写 `contracts/map-visibility.md`：默认全遮盖、服务端重新编码玩家安全 PNG、
当前 scene/version URL、逐次授权与 no-store、DM 独立 fog 回执和校准重置遮盖；
再次遮盖不能撤回之前已下载的公开像素。单位隐藏/控制保持独立。按工作包先协调
契约后分工 backend/frontend/contracts，IT 库串行独占。后续实际 RED/GREEN 另记；
P02d 实施中，P02e 与整个 P02 未完成。

### P02d 实际 RED 与安全故障注入

- 契约检查 22:19:37 实际 exit 1：缺少 map-view/map-image/GET fog/POST fog
  四个操作；补齐后 `node scripts/check-contracts.mjs` 31 操作通过，原 27 操作
  和断言保留。这是聚焦契约检查，不冒充完整 OpenAPI validator。
- 后端 22:20:21：原 56 unit 通过，真实 `MapVisibilityIT` 两项断言失败；
  map-view 预期 200 实际 404，匿名地图响应缺约定的 private 标头。缺路由使后续
  像素断言未执行，此 RED 不声称既有实现已经泄漏图像。22:22:47 首轮 56 unit
  加两项真实 HTTP/PNG 验收通过，随后继续扩展边界覆盖。
- 22:27:01 临时故障注入绕过 `revealed` 过滤，真实 HTTP PNG 的遮盖像素断言
  失败：预期 `0xff000000`，得到源色 `0xff12ab34`。Maven 明确 1 failure、
  0 errors/skips；`finally` 随即恢复安全实现。用于证明测试能抓住泄漏，故障
  实现不提交；恢复后必须重新全量构建，不能复用故障前产物。
- 22:28:34 内存缓存探针实际 RED：启用 ImageIO 默认磁盘缓存并令缓存目录
  失效，生产 render 经 FileCacheImageInputStream 失败；测试解码器显式走内存，
  排除测试自身污染。改用显式 MemoryCache 输入/输出流，不修改全局缓存开关。
  Java 默认缓存行为可查 [JDK 25 ImageIO API](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/imageio/ImageIO.html#setUseCache(boolean))。
- 前端先实际运行新地图 API 测试：三项中两项断言失败，证明 map-view/image
  尚未设置 `cache: no-store`。后续缓存生命周期、fog 和 Pixi 结果见最终验收；
  不能把类型错误、环境失败或只列出 E2E 当作行为 RED/GREEN。

### P02d 本地完整验收（2026-10-19）

- 22:31:55 全量 Maven `-Pintegration verify -B -ntp`（同交接外置 JDK25、
  Maven 和 m2，专用真实 PG）：**61 unit + 60 IT 通过**，0 失败/错误/跳过，
  原 56 unit + 50 IT 全保留。新增 PNG 全像素/校准/alpha/metadata/内存流 unit，
  9 项 HTTP/PG/WS 及 V6 迁移 IT；读源图期间的 latch 验证 cover 等待同团锁。
  fog 的鉴权、严格 JSON、原回执、全局 actor 请求竞争、共享版本、原子重置及
  即时/延迟故障回滚均覆盖。22:33:24 清除全部 TEST_DATABASE_* 后单独 `mvn test`
  **61 unit 通过**。实现阶段读流重复关闭失败已修成显式 reader/dispose，未放宽断言。
- 前端最终 **14 文件 148 测试通过**，原 112 项保留，TypeScript/Vite 构建通过；
  约 583.57kB 主 chunk 提示保留。两个授权和测距边界有实际 RED/GREEN，见两端
  TDD。单位隐藏与 fog 独立；玩家图片只来自安全接口，纹理不进入全局素材缓存。
- 22:34–35 协调者 `npm run test:e2e` 真实 Chrome **10 passed（31.1s）**，
  原 7 条文件原样保留，新增 3 条；60 秒/条、零 retry。无 HTTP/STOMP 业务 mock。
  DM/玩家双会话实际操作选择、drag 预览/吸附/确认、点击移动、光标缩放、平移、
  测距、矩形和坐标揭露/遮盖；解码 PNG 比较公开色与秘密黑色，源 metadata 不携带。
  用实际 STOMP 失效加 snapshot 网络中断验证先清图、revoke blob、恢复后黑图。
- 启动新安全 JAR，自然升级既有 V5 浏览器库至 V6；外置 `P02dSnapshot.java`
  与 `compare-p02d-snapshots.mjs` 比较 **14 张旧表全部列/行及 V1–V5 checksum
  完全一致**，非仅比较数量。素材根沿用 `.tools/browser-assets`，未重建旧库。
- 三个 seed 后核对实际 Java CommandLine、PG data/PID 与 pg_ctl status，真正停止
  Java 和 PostgreSQL，再以同库/同素材根启动新进程。新会话执行地图、素材、棋子
  三个 verify 全部 PASS：fog/scene 版本/历史回执/安全 PNG SHA-256 保持，旧揭露
  回执不能恢复像素、旧 URL 409、撤权 403、退出 401；原素材与棋子验收继续通过。
  批量命令曾被自动审批拒绝，未执行；分步重新核实身份后停止/重启成功，无遗留阻塞。
- 已查看 DM/player 截图：完整 24×16 画布、主持矩形和表单可用；玩家全遮盖图为黑色，
  公开单位仍可见，隐藏单位与 DM 作者/素材区不出现。截图在忽略的 `test-results/maps-*`。

日志在仓库外 `.tools/p02d-backend-full-verify.log`、`p02d-browser-full.log`，证据
快照/三份重启回执也在外层 `.tools`，不提交运行时、凭据、战役数据或二进制图片。
本地无 Docker，当前提交的标准 Compose/10 浏览器/三类容器重启验证须等新 SHA
远端 CI，不能用基线绿灯代替。**P02e 与整个 P02 仍未完成**；TLS、触屏、容量、
完整素材一致备份与异机恢复仍按原工作包保留。再次遮盖无法撤回之前已保存的公开像素。

### P02d 首次远端：Pixi 在途手势丢失

代码 `d593092e83a1eb2613cde916fcc4090577d646d2` 的
[push39271185130](https://github.com/xianchen233/dnd-roll/actions/runs/39271185130)
frontend/backend success，日志确认 148 前端及 61 unit + 60 IT 通过。
标准 Compose 构建/启动成功，但 browser **8 passed、2 failed（5.7m）**：旧 7 条
及新增坐标遮盖/失败刷新清图通过，新增 maps.spec.ts:71 拖动后位置与 :84 矩形后
揭露数量断言失败；不是 60 秒总时限失败。容器重启步骤尚未执行，不能标通过。

22:51:42 新增可控同场景 snapshot/mapUrl 更新穿插 pointerdown/up 的组件用例，
3 项实际断言失败：两项拖动 onMove 为 0、一项矩形 onRectangle 为 0，原 7 Pixi
测试通过。源码将手势闭包绑定到随图片/快照变化重建的 effect，失效清图会丢弃手势。
这是直接组件复现，CI 具体时间线仍待 artifact trace 核准；修复后须重新本地完整
回归及新 SHA 全部 CI，不增加 timeout/retry 或删除业务断言。

修复将同场景交互、棋子绘制与图片资源分为独立生命周期，安全图清除/销毁不再
移除 stage 手势处理器；切场景、选择/控制/棋子版本变化仍取消在途手势。另有
Fog 背景读吞掉已有草稿编辑的实际 RED，修成编辑保留原版本而提交等待新读。
首轮修复前端 **156 项/构建通过**，生产 preview 新三条 **3 passed（8.8s）**。
前两条还在 pointerdown/up 间触发真实 HTTP 快照刷新并等待 Image.onload，强制
覆盖该边界；原业务断言/60 秒时限/零 retry 保留。下载原 CI artifact 的单连接
和分块请求均遭网络 reset/timeout，未拿到可用 trace；因此不声称已确定第一次
CI 失败的唯一直接原因。新 SHA 的完整 CI 仍须独立确认。

稍后取回同代码 [PR39271191222](https://github.com/xianchen233/dnd-roll/actions/runs/39271191222)
日志：也是 frontend/backend success、browser **8 pass / 2 fail（8.0m）**，但
停点不同。第一条已完成移动，到测距预期 25 却仍为第二点起点，最终 60 秒到期；
第二条揭露已通过，执行再次遮盖的保存时总时限到期。不能把 push 的非总时限失败
概括为 PR 的失败。稳定交互生命周期保留测距起点；新长流程将按移动、测距/视图、
揭露、再次遮盖分为独立验收，保留所有断言、真实刷新注入、60 秒/条和零 retry。
协调者在拆分前已运行修复后的完整生产 preview：**10 passed（27.4s）**；最新
权限清理小修及最终拆分仍须另行完整复验。

23:24:04 最新前端全量 **157 项/构建通过**（主 chunk 585.14kB）；23:24–25 新增
五条生产构建 E2E 通过。协调者随后以最终 build 统一真实 Chrome 复验：**12 passed
（30.1s）**，包括原 7 条及新增 5 条，60 秒/条、0 retry。最新 deny 清理已包含。
本次后端/契约/部署/重启脚本无变化，不重复此前已通过的后端和实际进程重启；
新 SHA 标准 Compose/12 浏览器/三类容器重启仍须 CI 确认，旧失败未冒称关闭。

## 2026-10-19：P02d续接前的最终基线核验

用户要求将下一阶段交接给下一会话，本次不实施P02d。交接前Git HEAD为
`a3e7f7ba9c2b66349e38df8f2c84ae087c9b60c0`，工作区干净，与origin一致。
[push39142300219](https://github.com/xianchen233/dnd-roll/actions/runs/39142300219)及
[PR39142302856](https://github.com/xianchen233/dnd-roll/actions/runs/39142302856)均
completed/success，frontend/backend/browser全绿；最终文档HEAD也已独立验收。
这两条browser日志均确认7 passed（PR4.7m/push4.8m），随后素材/棋子两个seed及
两个实际db/backend容器重启后的verify均PASS。原P02c代码验收和失败闭环见下文。

本次仅更新交接文档，不新增生产行为，不重复运行本地测试。下一会话先核对本次
交接提交的新HEAD Checks，再按continuation.md开始P02d契约及秘密像素泄漏实际RED。
P02d/e未实现，P02整体未完成；继续沿用既有分支和草稿PR #1。

## P02c 开工核对（2026-10-18）

沿用 `feat/accounts-and-invitations` 和 open/draft PR #1，开工 HEAD
`2967c37071ccec2f967928a84c1f5bda880b76ed`，工作区干净；fetch 后与 origin
一致。该 SHA 的 [push run39135956651](https://github.com/xianchen233/dnd-roll/actions/runs/39135956651)
与 [PR run39135959413](https://github.com/xianchen233/dnd-roll/actions/runs/39135959413)
均 completed/success，frontend/backend/browser 三个 job 全部 success。
先写 `contracts/tokens.md` 再开展新测试；P02c 新功能检查不会复用这组旧 SHA 结果。

本轮首先固定 DM 棋子创建/完整编辑、隐藏与多控制者、独立持久回执、scene/token
版本和撤权边界。契约检查实际 RED：`node scripts/check-contracts.mjs` exit 1，
documented/actual 比较缺少 GET tokens、POST tokens、POST tokens/{tokenId} 三个操作。
后端 22:44:36 `TokenServiceTest` 实际 4 项断言失败；环境/路径错误不计行为 RED。
完整结果随后追加，当前不声明 P02c 已完成。

聚焦检查已实际通过：契约 27 HTTP 操作；前端初次全量 108 项与构建；
22:52:16 后端聚焦 verify 为 56 unit + 9 新真实 PG/HTTP/WS/迁移测试，0
失败/错误/跳过。后端在实际 PostgreSQL 上验证创建/完整配置、双玩家控制、
隐藏/撤控后旧移动拒绝、成员移除/再加入及旧编辑版本冲突、当前DM先于回执、
跨团/场景归属、严格JSON、幂等指纹、并发单效/版本单胜、事务回滚和秘密不入
原生STOMP事件。完整结果如下。

### P02c 本地完整验收

- 22:54:39 全量 `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp`：
  **56 unit + 50 真实 PG/HTTP/WS/迁移测试全部通过**，零失败/错误/跳过，重新打包。
  既有52 unit及41 IT保留；新9项IT包含V4→V5逐列数据/checksum/FK、双玩家控制、
  移除仅撤目标成员且保留其他控制者、Unicode用户名边界及Java统一控制者排序。
- 22:58:40 协调者最终 `npm test` **110项**通过，随后TypeScript/Vite生产构建通过。
  旧90项保留，新测试包含在途撤控/隐藏、再授权不恢复选择、作者草稿保留原版本、
  401/403并发拒绝、作用域隔离、精确重试、最新post-ack权威读确认及失败恢复。
  约572kB主chunk提示保留。契约检查**27 HTTP操作**通过，全部旧24操作断言保留。
- 22:55–56 真实Chrome `npm run test:e2e`：**5通过（21.0秒），0 retry**。
  原M1/P01/P02a/P02b四条保留；新增独立战役DM界面创建、双控制者、玩家移动、
  隐藏/公开、撤控/再授权、移除/再邀请、旧move/create回执不得恢复控制，真实STOMP
  不含棋子秘密。新流程等待可用按钮与新邀请链接，不增加timeout或retry。
- 自然升级：旧V4浏览器库启动新打包Jar后升V5；外置`P02cSnapshot.java`与
  `compare-p02c-snapshots.mjs`比较**13张V1–V4表全部原列/原行及四个迁移checksum一致**。
  新增campaign_id只作回填，不纳入原列比较，另由迁移IT验证其FK/归属。
- 22:57–58 实际核对PID/CommandLine后停止Java和PostgreSQL，以**同库/同素材根**
  启动，非重建Spring context。`scripts/check-token-restart.mjs`的seed→真实重启→
  新会话verify通过：名称/位置/隐藏/控制者/token及scene版本/原回执保持，撤控与移除
  再加入后的旧move仍403，旧author/remove回执无新效果、stale编辑409；
  `scripts/check-asset-restart.mjs`继续通过SHA-256、地图/版本/回执与下载权限回归。
  棋子脚本邀请创建明确期待既有201，不改变P01接口。
- 已人工查看新DM/player截图：画布完整，DM配置与多选控制者可用，重新加入的玩家
  棋子仅查看且无DM作者/素材区。截图在忽略目录`frontend/test-results/tokens-*`，不提交。

两端实际RED/GREEN见`backend/TDD.md`、`frontend/TDD.md`。本机无Docker，新SHA标准
Compose/五浏览器/实际容器重启待远端CI确认，不以本地通过宣称远端通过。P02d玩家
派生地图/Pixi/遮盖、P02e整体验收及P03–P10/A继续保留，整个P02尚未完成。

### P02c 首次远端：新长流程超时

代码`a84ca8e904babe6cf488e0c7216d49a3a12925bf`的
[push39138572141](https://github.com/xianchen233/dnd-roll/actions/runs/39138572141)与
[PR39138575388](https://github.com/xianchen233/dnd-roll/actions/runs/39138575388)
frontend/backend均success，日志确认110前端及56unit+50IT全部通过。
标准Compose构建/启动成功，原四条真实流程均通过；新增tokens流程60秒总时限超时，
finally的context.close在测试结束时再次报错，掩盖具体业务步骤。push原四条分别
40.4/50.4/36.9/40.6秒；本机相同流程2–8秒。新容器重启步骤尚未执行，不能标通过。
已下载保留trace，下一步按实际动作时间轴定位；不以本地绿灯、全局timeout/retry或
删除断言关闭失败。P02c远端门槛仍未完成。

trace实际时间轴：UI建团/邀请/创建截至31.67秒，玩家移动确认40.65秒，隐藏及秘密
快照/旧move404截至47.74秒，公开及无旧选择截至57.83秒；随后玩家重新选择耗时到
60.25秒，下一编辑仅刚就绪，尚未提交撤权。单次click/check约2.2–3.1秒，HTTP仅
3–34毫秒，没有某一业务断言挂死。按独立验收场景拆分：UI创建与共享移动；独立
真实HTTP前置后的UI显隐/撤控/再授权；独立前置后的UI移除/再邀请及旧回执。每条
继续60秒、零retry，原全部业务断言保留；context清理放fixture独立teardown，不让
finally错误掩盖实际步骤。拆分后须重新本地及当前SHA远端验证。

23:22 本地拆分后完整 `npm run test:e2e` **7通过（23.2秒）**：原四条及三个独立
P02c案例全部通过，仍60秒/条和零retry。第一次拆分运行6过1失败：第七条原本观察
玩家帧，但移除前排除/关闭玩家、接受邀请先于新socket、后续回执不发事件，因此
正确无帧；改为观察持续在线DM收到的真实成员失效事件，保留`>0`及所有秘密/权限/
回执断言，前两条继续验证玩家事件。此为测试前置与观察对象修正，非生产行为修复。
前端构建/测试发现检查通过；生产代码未改，110unit及56+50后端全量结果仍适用。
新SHA远端7流程及两个实际容器重启脚本仍须完整运行确认。

### P02c 第二次远端：控制者输入与后台读竞争

`8339354f7cdf9858c930a93f106ceea2185104fa`的
[push39140226138](https://github.com/xianchen233/dnd-roll/actions/runs/39140226138)
已全success（7浏览器及素材/棋子实际容器重启）；
[PR39140229143](https://github.com/xianchen233/dnd-roll/actions/runs/39140229143)
frontend/backend仍success，原四条与新增创建/移动、移除/再邀请均通过，但第六条36.3秒
在取消`控制者 browser-player`时失败：`Clicking the checkbox did not change its state`。
不是总时限失败，也不能用同SHA绿色push关闭。23:32:06补deferred后两个真实断言RED：
已有成员的checkbox在背景重验证时被disabled，以及重新选中草稿后旧ack横幅被背景读
重新插回。23:32:39原生checkbox.click在mousedown→背景读→click顺序下实际仍checked，
复现动作被吃掉。针对最小表单行为修复，保持提交就绪/版本检查和所有浏览器断言。
完整修复与新SHA结果后续记录；当前P02c远端门槛仍未完成。

PR trace随后直接证实：失败uncheck在240477.847ms检查enabled/stable，240836ms开始
背景scene/tokens/members读，241011.149ms action快照中FIELDSET已disabled，目标仍
checked，241183.539ms才执行click，因此点击被禁用fieldset吃掉。after快照fieldset
恢复但checked未变，且before/action没有的旧确认横幅重新出现。不是HTTP超时，相关
HTTP仅7–13ms。原生click/deferred RED与真实trace一致。

最小修复不改变E2E：后台权威读期间仍禁止提交，已加载成员checkbox的本地草稿保持
可编辑；选择或开始新编辑时结束旧ack确认周期，旧横幅不再回插。23:33:35作者16项
GREEN，23:33:38全量**112前端测试与构建通过**，旧110项保留；23:36协调者真实Chrome
完整**7通过（24.3秒）**，仍60秒/条、零retry。CHANGELOG同步，后端与契约未改变。
提交修复后仍按新SHA核实完整push/PR CI，8339354的绿色push不替代失败PR。

### P02c 最终完整 CI 通过

最终代码 **5160c0fdbb98a9bb189193f1ec8735d004ffb978** 的
[push39141337705](https://github.com/xianchen233/dnd-roll/actions/runs/39141337705)和
[PR39141340870](https://github.com/xianchen233/dnd-roll/actions/runs/39141340870)
均 **completed/success，frontend/backend/browser全部通过**。新SHA前端112项/构建/
27契约、后端56unit+50真实PG/HTTP/WS通过；标准Compose构建/启动、原四条加三个独立
P02c流程全通过，PR日志7 passed（4.7m）。随后两个seed和两个新会话verify均PASS：
实际db/backend容器重启后，文件SHA-256/地图/版本/素材回执保持，棋子名称/位置/
隐藏/控制/版本/原回执保持，撤控/重新加入后的旧移动仍拒绝，旧作者/移除回执无新
效果，历史地图回执不重挂图。按当前SHA检查，没有以8339354绿色push代替失败PR。

至此P02c门槛完成。前述真实失败历史已由对应RED/GREEN与最终新SHA关闭；没有删除
旧测试、放宽业务断言、增加retry或延长每条业务60秒时限。PR #1仍open draft/base
main，未合并、未发布。Java/Vite/PG已按进程身份核对后停止，数据/素材保留。后续
交接文档提交仍核对自己的Checks。下一包P02d；P02e与整个P02、M2尚未完成。

## 既有里程碑验收历史

日期：2026-10-18（Asia/Shanghai）。以下保留M0/M1、P01、P02a、P02b原始验收记录；P02c当前结果见上文。

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

M0/M1交付仓库框架与最小纵向切片；完整首版业务、模组与自动化见实施路线。demo 是显式启用的开发入口，账号密码从环境提供。正式的邀请与账号管理在P01推进，场景作者入口为P02。

## 尚未验证与后续工作

- 本机无Docker：没有在此机器执行镜像构建、Compose启动或容器重启。远端CI的browser job会通过标准Compose部署运行同一真实浏览器测试；必须查看实际CI结果才能标为通过。
- 外层TLS证书及反向代理链只完成配置与说明，仍需部署环境验证HTTPS和WSS。未部署到公网。
- 尚未进行完整测试团、手机触控、性能/容量测量、完整备份与异机恢复。这些属于后续业务/发布阶段，未因本轮验收而标为完成。
- 首版完整业务与模组/自动化仍待实现，按 `work-packages.md` 的P/A工作包继续。
- 原先CLI认证阻塞已在用户完成GCM授权后解除；基线已推送，远端CI实际结果见下文。新的P01提交仍须单独验收。

## 远端CI首次执行

2026-10-18，用户通过Desktop发布79cf832后，运行 [Build and test #1](https://github.com/xianchen233/dnd-roll/actions/runs/39124170015)：frontend与backend通过；browser在后端Docker构建时失败，还未执行浏览器步骤。Temurin构建镜像没有unzip，only-script Wrapper将ZIP替换为tar.gz，导致与ZIP的固定SHA-256不符。已为构建阶段补unzip，保留校验，需推送并等待新CI结果。此项为真实部署RED，不是业务单元测试失败。

[Build and test #2](https://github.com/xianchen233/dnd-roll/actions/runs/39124457926)（d903d40）：frontend/backend仍通过，前后端镜像均成功构建、PostgreSQL健康；紧接容器启动的curl遇到连接重置(56)，原有retry未涵盖此错误，browser尚未执行。就绪检查改为重试所有暂时错误、单次5秒、总重试120秒；没有跳过就绪检查或放宽业务测试断言。

[Build and test #3](https://github.com/xianchen233/dnd-roll/actions/runs/39124712714)（c57c35a）：**frontend、backend、browser全部success**。标准Docker Compose的前后端镜像构建、PostgreSQL启动、代理HTTP/WS及真实双浏览器流程均有远端运行证据。至此M0/M1的容器检查门槛完成；公网TLS和完整产品验收仍另行进行。用户确认GCM终端授权后CLI推送已成功。

## 当前P01

在上述CI通过后开工，契约位于contracts/accounts.md，分支feat/accounts-and-invitations。以下为P01本地实际结果，远端CI另行记录。

| 检查 | 实际结果 |
| --- | --- |
| 机器契约 | `node scripts/check-contracts.mjs`：16个HTTP操作通过；保留M1并增加账号、版本、秘密字段及fragment约束；聚焦检查器不冒充完整OpenAPI validator |
| Java unit | 20:10:41，全量Maven `-Pintegration verify`中30通过，0失败/错误/跳过，无数据库的单元层包含bootstrap、policy、application及socket registry |
| 真实PG/HTTP/WS | 同次verify共18通过，0失败/错误/跳过：AccountsIT9、FirstSliceIT5、RealtimeIT4；V1既有数据库成功升级V2 |
| 前端 | 20:06:21，`npm test`共31通过；`npm run build` TypeScript/Vite通过，约550kB主chunk提示仍保留 |
| 真实Chrome | 20:11，`npm run test:e2e`两条通过（13.6秒）：原M1双会话回归和新P01三会话账号/邀请/移除流程，无HTTP/STOMP mock |
| 打包进程重启 | 20:12，停止Java、同库重启、新会话认证后，创建战役回执不重复建团；邀请回执保持id/到期/version且token:null；被移除玩家仍403，旧邀请仍409；未使用邀请仍能接受，旧移除回执不删除新授予成员 |
| 静态审查 | 邀请明文不进入表、回执、列表或后端URL；管理授权先于回执；跨战役控制权删除带campaign scope；成员删除和回执故障均原子回滚；`git diff --check`通过 |

TDD实际发现并修复：邀请首次响应纳秒与PG微秒精度不一致；邀请消费后preview失败阻断恢复；Unicode密码字符计数不同；较早403被忽略、在途旧快照恢复撤权地图。证据见两端TDD.md。全量IT曾遇本机PG连接上限，测试专用Hikari池缩至3后18项全通过；环境失败未算业务RED。

真实P01浏览器步骤：DM创建空战役→分享`#invite=`链接→第三个独立context注册固定账号并自动正常登录→选择新战役→DM刷新成员并移除→真实socket关闭、客户端地图清空、后续场景403→账号仍保持有效且重新登录也不恢复成员。原M1截图已重新生成并人工查看，画布完整、管理区及权限单位列表正常。

进程重启为本机外置smoke脚本，非Maven自动测试；复现时用独立数据库执行上表命令顺序，不把在同一Spring context内调用bootstrap视为进程重启。P02场景与素材、P03角色卡以及M2其余工作包未完成。

P01代码提交 **3c12f8d4c0fe0687e3c3af09af40a87ab60065f4** 的[远端CI](https://github.com/xianchen233/dnd-roll/actions/runs/39126498540)：**frontend、backend、browser全部success**，含标准Compose构建/启动、真实PG与两条真实浏览器流程。[草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)已创建并附到聊天，未合并或发布。后续文档交接提交不改变上述代码测试证据；应继续核对PR当前提交状态。

### PR CI暴露的并发回归

dbe61cf的push run39126867019三个job通过，但[PR run39126870907](https://github.com/xianchen233/dnd-roll/actions/runs/39126870907)在M1浏览器移动确认处失败。真实STOMP刷新替代移动后的直接refresh时，位置已保存/同步，界面却停留在“场景刷新失败”。这是真实状态竞争，不能以另一个绿色run或自动retry关闭。

20:25:19先补deferred RED（两种响应顺序2失败），20:26:32补恢复边界再RED；最小修复记录POST成功时refresh序号，只让其后最新有效快照完成同步确认，旧/撤权响应继续隔离。20:26:50 `npm test`最终**36通过**，TypeScript/Vite构建通过，原M1/P01测试保留。

本机复验中，P01所有邀请/移除/403断言先通过，但退出后脚本立即导航导致重新登录表单超时；trace定位于8.34秒开始的账号fill。补上等待退出完成后的“进入冒险”按钮可见再导航。20:29再次 `npm run test:e2e`：**2通过（11.8秒）**，E2E业务断言未放宽、未配置retry。后端源码未改，不重复冒称执行新的Java检查。最新提交的远端结果以[PR Checks](https://github.com/xianchen233/dnd-roll/pull/1/checks)为准，交付必须检查当前SHA。

## P02a 开工核对

2026-10-18：HEAD `d9a03935920a1e75013e3de24a31370e33a87849`，Git clean，与 origin 一致。通过已认证 GitHub API 核对 PR #1 open/draft/base main，当前 SHA 的 [push run39127654306](https://github.com/xianchen233/dnd-roll/actions/runs/39127654306) 和 [pull_request run39127657531](https://github.com/xianchen233/dnd-roll/actions/runs/39127657531) 均 completed/success。P01 门槛满足后才开始 P02a 契约与 RED。P02a 检查尚未执行，不复用 P01 绿灯标记新功能通过。

### P02a 契约 TDD

先完成 `contracts/scenes.md` 后扩展检查器，实际 `node scripts/check-contracts.mjs` exit 1：documented/actual 操作比较缺少 GET scenes、POST scenes、POST activate（check-contracts.mjs:79）。随后补 OpenAPI，实际同命令 exit 0：19 个 HTTP 操作通过，保留全部 16 个旧操作。覆盖 DM-only、共享 campaignVersion、独立持久 scene-command 回执命名空间、授权先于回放、历史切换回执、严格字段及 Unicode 名称边界、无秘密元数据事件/玩家快照。此检查仍是聚焦契约检查器，不冒充通用 OpenAPI validator。

### P02a 本地最终验收

2026-10-18（Asia/Shanghai），所有旧 M1/P01 测试保留；未开始 P02b。

| 检查 | 实际结果 |
| --- | --- |
| 后端 | 20:55:38 `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify`：37 无数据库 unit + 28 真实 PG/HTTP/WS，0 失败/错误/跳过；AccountsIT9、FirstSliceIT6、RealtimeIT4、ScenesIT8、SceneMigrationIT1 |
| 前端 | 协调者20:56:49 `npm test`：6 文件53通过；`npm run build` TypeScript/Vite通过，主chunk约556kB提示仍保留 |
| 契约 | `node scripts/check-contracts.mjs`：19 HTTP 操作，保留全部旧16操作；命令/快照字段兼容、DM-only、共享版本/幂等与秘密边界检查通过 |
| 真实浏览器 | 20:56–57 `npm run test:e2e`：本机 Chrome、真实后端/PG/Vite，3通过（14.8秒），原 M1/P01 两流程原样保留，无 HTTP/STOMP mock、0 retry |
| V2→V3 实际升级 | 20:56 打包服务对旧 browser 库自然升级，Flyway验证1/2/3；升级前后5个campaign/scene、2个token、所有controls及旧receipt数量逐项相同；自动迁移IT另逐行验证原表及同团FK |
| 真实进程重启 | 20:58 外置 smoke-p02a-restart.ps1 实际停止Java、同库重启、新会话：当前scene、创建/激活回执、旧scene/token位置与版本、move回执保持；旧activate回放不再切图、inactive move回放404、demo不复位 |
| 视觉与静态检查 | 已人工查看真实DM/player PNG，画布完整、DM场景/成员管理正常、玩家无管理区和隐藏单位；`git diff --check`通过 |

第三条真实浏览器流程：玩家移动原demo棋子 → DM创建空备团场景而双端仍在旧场景 → 玩家不请求备团列表、直接列表/activate均403 → DM切图并观察真实私有STOMP frame（无场景名或秘密单位）→ 玩家新空场景/清选择 → DM切回 → 完整玩家快照与原保存位置、token/scene版本一致，DM仍可见隐藏敌人。finally只在需要时恢复旧current，避免破坏原M1复跑。截图在忽略目录 `frontend/test-results/scenes-DM-creates-a-prepar-f5e8f-o-the-preserved-moved-scene/`。

前端 TDD 包含初始11项预期失败，追加409刷新跨scene和POST成功但快照503的实际RED；采用独立scene generation，保留campaign epoch与任一当前epoch401/403全局清除，原P01实时刷新竞争修复保留。切换命令回执仅提示已确认，地图是否同步取决于真实新快照，不以历史回执宣称同步完成。

本地没有执行Docker/Compose或公网TLS；本轮新提交的远端CI尚待核实。迁移与重启检查只针对测试数据库，不等于P09完整备份/异机恢复。

### P02a 最终审查修复与复验

最终只读 QA 发现 `CampaignManagement.load()` 的 `Promise.all` 先网络/503、后另一请求401/403时，后到撤权未被观察。21:01:40 focused RED 实际4失败/14通过；每个请求独立观察授权失败再聚合，不等待另一挂起请求。新增6项deferred回归，晚到地图/备团/管理成功均不得恢复数据。21:02:04 全量前端最终 **59通过**，构建通过。21:03协调者再运行真实 Chrome `npm run test:e2e`，**3通过**，含原两流程与P02a，没有mock/retry或放宽断言。后端生产代码未改，沿用本轮37unit/28IT及真实重启证据，不声称重跑了新的Java检查。

### P02a 远端 CI 完成

代码提交 **8836abbcca9a28a2e130770cb9839e0136f51fcf** 已推送至原分支。经认证 GitHub API 按该 SHA 核对：[push run39129943816](https://github.com/xianchen233/dnd-roll/actions/runs/39129943816) 与 [pull_request run39129946834](https://github.com/xianchen233/dnd-roll/actions/runs/39129946834) 均 **completed/success，frontend/backend/browser 全部通过**。标准 Compose 的新鲜 V1/V2/V3 迁移、镜像构建/启动、NGINX HTTP/WS 与全部3条真实浏览器流程均有远端证据；非仅静态配置检查。

PR #1 标题/说明已按最终 P01+P02a 范围更新，仍为 open draft、base main，未合并/发布。P02a 门槛完成；下一子包 P02b 尚未开工。交接文档后续提交不改变该代码 SHA 的测试证据，交付前另核对 PR 当前 HEAD 的 Checks。任务启动的本机 Java/Vite/PG 已核实后停止，测试数据与忽略产物保留。

## P02b 持久素材与地图配置

2026-10-18（Asia/Shanghai）。开工 HEAD `be96f2c0841fc81a9917d1fd7d3510ef4feac928`，Git clean、fetch 后与 origin 一致；连接器核对 PR #1 open/draft/base main，按该 SHA 核对 [push run39130339805](https://github.com/xianchen233/dnd-roll/actions/runs/39130339805) 与 [PR run39130343257](https://github.com/xianchen233/dnd-roll/actions/runs/39130343257) 均三个 job success。未重复开发 P01/P02a。

先写 `contracts/assets.md`，再补检查器并实际运行 exit1：缺少 assets list/upload/content 与 map get/post 共五操作。补 OpenAPI 后 exit0：**24 HTTP 操作**，全部原19操作检查保留。检查器覆盖multipart精确parts、DM-only、上传限额/解码、no-store/503、版本/回执、严格可空地图配置及秘密字段；仍不冒称通用OpenAPI validator。后端图片验证3断言RED和真实HTTP预期200/实际404 RED、前端12断言RED及共享管理版本RED的具体命令/时间见两端 TDD.md。

| 检查 | 实际执行结果 |
| --- | --- |
| 后端全量 | 21:31:33 `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp`：**49 unit + 40真实PG/HTTP/WS通过**，0失败/错误/跳过，jar打包；原28IT全部保留 |
| 后端最后增量 | 21:34:43 新增额外/重复multipart parts拒绝后，聚焦AssetsIT：**49 unit + 12 AssetsIT通过**，jar重新打包；合计41个独立IT均执行通过，不声称此命令重跑其余IT。上一全量后生产代码无变动 |
| 前端最终 | 21:38 `npm test`：**7文件84通过**，保留旧59；`npm run build` TypeScript/Vite通过；约564kB主chunk提示未放宽 |
| 真实Chrome | 21:39–40 `npm run test:e2e`：**4通过（16.8s）**，本机Chrome+真实Java/PG/Vite、独立DM/player会话、无HTTP/STOMP mock、0 retry；原M1/P01/P02a三流程保留 |
| V3→V4旧库自然升级 | 21:32打包服务自然升级原browser库，`P02Snapshot.java`升级前后逐项比较campaign/scene/token/control和旧receipt数量一致，Flyway为1/2/3/4；独立AssetMigrationIT另逐行比较全部原表和V1–V3 checksums，验证asset和scene两条同团FK |
| 文件与数据库实际重启 | 21:36–37 `node scripts/check-asset-restart.mjs seed <外置JSON>` → 核实PID/CommandLine后停止Java、pg_ctl停止/同库重启PG、同素材根重新启动jar → 新会话`verify`：**PASS**。文件SHA-256、稳定ID、校准、版本和两类原回执一致，重复上传只有一个ID；detach后历史map重放不重挂图，旧文件不删除；玩家/其他战役/退出后content403/404/401 |
| 人工/静态 | 已查看DM/player实际PNG，完整画布、DM配置可见、玩家无素材/管理区；`git diff --check`、`node --check scripts/check-asset-restart.mjs`通过。Compose/NGINX/Dockerfile已改，本机无Docker，不以此声称容器运行通过 |

新增真实流程实际完成：DM创建备团场景并切图→上传运行时生成PNG→鉴权Blob预览→真实浏览器下载及HTTP下载SHA-256相同→保存校准→玩家不请求素材/地图、已知ID/猜ID/直链均403且快照无原图字段→切走再回来配置保持。截图位于忽略目录 `frontend/test-results/assets-DM-uploads-download-a95ce--fetch-original-or-metadata/`。

第一次真实运行中，P01通过，素材上传/预览/下载/SHA都已通过，随后`getByLabel(...exact:true).selectOption`因包裹select的label文本包含option而等不到目标，耗尽60秒。finally恢复请求被测试结束取消，造成后面两条旧流程落在空场景的继发失败。trace定位后给select明确accessible name，精确combobox定位；恢复放入独立20秒fixture teardown，每个恢复HTTP限5秒、动作限10秒，原业务期望与60秒总测试限额不变，不加retry。经版本化授权HTTP恢复demo原图后重跑全部4条通过，未将首次失败记作通过。下载生命周期无故障，没有修改。前端新增名称回归后最终84通过。

实现安全与故障验收：原图只由同团当前DM访问；玩家快照/事件不增加asset/map字段；上传真实PNG/JPEG解码、完整容器/CRC、字节/尺寸/像素限额；忽略客户端文件名，独立UUID存储键不出接口。asset-commands按actor→campaign锁，上传共享campaignVersion，map与move共享sceneVersion；回执前重验DM。同request并发单效、双DM单版本单胜、跨团全局request竞争、撤权/退出、缺文件503均有实际测试。即时receipt插入及延迟commit触发器故障验证DB引用/版本/回执原子回滚，只清新文件、旧素材保持。前端独立观察并发401/403，切场景/团/撤权清理objectURL与pending并隔离晚到成功；不确定重试保留原File/body。

Compose新增asset_data私有卷及运行用户目录权限，NGINX允许10MiB文件加multipart开销；CI新增真实浏览器后seed→重启db/backend→verify同套HTTP脚本。**本轮新SHA远端CI尚待推送核实**，不复用旧P02a容器绿灯。崩溃可遗留无元数据的不可下载孤立文件，无自动删除；完整一致备份/保留/异机恢复仍P09。玩家派生图和Pixi交互/遮盖仍P02d，P02c/d/e、P03–P10/A未完成。

### P02b 首次远端 CI 暴露同步确认竞态

代码 `cfdfccfc3fb5dd777c997a4cd5488057b882988a` 的 [push run39132905104](https://github.com/xianchen233/dnd-roll/actions/runs/39132905104) 与 [PR run39132907662](https://github.com/xianchen233/dnd-roll/actions/runs/39132907662) frontend/backend均success，标准Compose构建/启动和原M1/P01/P02a三条浏览器均通过；新增assets流程在保存后等不到“地图请求已确认；当前配置已从服务器刷新。”而失败。故browser未通过、容器重启step尚未执行。正在用deferred测试重现POST确认后的主动刷新被revision/实时刷新抢占；不能用本地绿灯、重试或改断言关闭。新修复另记录RED/GREEN并推送检查。

21:51:02 先补两种deferred响应顺序，实际2断言失败，21:51:39同测试GREEN。修复POST确认时记录读序号，让其后启动且最新有效的权威读取（含revision/实时刷新）统一完成同步提示；确认前的读、旧成功/旧失败均不能确认或覆盖，新读失败明确未同步，之后成功可恢复而不重复POST。新增6项边界后21:52:45前端全量90项/构建通过。后端源码未改；首次远端backend实际日志确认49unit+41IT全部通过，0失败/错误/跳过。

协调者随后复验中，原三条再次通过，新增assets因登录后立即inputValue读取尚为空的campaign selector而提前失败，未进入地图操作；补与既有scenes测试相同的自动等待 `expect(combobox).not.toHaveValue('')`，未改变业务断言/timeout/retry。21:54–55再实际运行全部E2E：**4通过（16.6秒）**，保存后的同步提示及全部素材/权限/切图断言通过。修复的新SHA须再次检查完整push/PR CI；不复用cfdfccf失败结果，也不将新提交重启step尚未执行记为通过。

### P02b 第二次远端：生产静态路径误判

修复提交 `c237dc7` 的 [PR run39133708410](https://github.com/xianchen233/dnd-roll/actions/runs/39133708410) frontend/backend仍success，原三条浏览器通过，新流程已通过保存后同步确认，随后“玩家未请求私有素材”断言失败。原因是路径检查 `includes('/assets')` 也匹配生产Vite公开的 `/assets/index-*.js`/CSS，本机开发服务没有这一bundle路径。检查改为精确匹配 `/api/campaigns/{id}/assets...` 和 `/api/campaigns/{id}/scenes/{id}/map`，并以具体路径数组报错；全部私有API仍被检测，静态JS/CSS排除，没有开放玩家原图权限或删除断言。对公开JS/CSS、素材列表/content、地图配置、普通场景路径逐项验证通过。该部署测试修正需新SHA远端运行确认；前端生产实现未变。

### P02b 旧场景测试初始化同步

`1b42c6c` 的push frontend90通过，但PR run39134127052 frontend89通过/1失败，旧scenes测试在等待“重试同一场景操作”处失败。create helper原先仅等名称输入出现便点击，初始列表尚未完成时创建按钮仍disabled，点击被忽略。helper现在填完后等待按钮enabled，setup补正常assets/sceneMap只读mock，移除无关的api.assets未定义错误。旧18项和全部原断言保留，未改生产逻辑、timeout或retry。22:04:52聚焦scenes18通过，22:04:57全量90和构建通过。修复需新SHA完整CI确认；此失败不冒称新产品行为RED。

`1b42c6c` 的PR run39134127052 browser job **success**：标准Compose私有素材卷、四条真实浏览器及seed→实际db/backend容器重启→verify已运行通过。日志明确输出重启后SHA-256、引用/版本/原回执与历史回放检查PASS。因此本轮容器持久化已有实际证据，但该SHA的整个PR run因上述单项旧测试初始化失败而仍failure，交付须等修复的新SHA所有job通过。

### P02b 素材测试动作就绪与提交确认丢失边界

`a33bd99` 的push/PR frontend均89过1失败，assets原detach用例在初始map读取未完成、按钮disabled时点击被忽略。系统检查assets/scenes测试首次动作，统一语义clickReady等待按钮enabled；upload/create也等待输入与提交可用。detach原用例改为deferred初始读取，先断言disabled/POST0，resolve后按原版本与map:null断言；全部90项与原业务断言保留。22:10:38聚焦46通过，22:11:16全量90、构建及diff-check通过；未改生产逻辑/全局timeout/retry/E2E。

最终审查另发现文件清理的真实持久性风险：原upload catch会在任何异常后删新文件，即使数据库已成功提交而commit确认丢失；可能使原回执指向丢失的文件。契约进一步明确未知提交不能视为已回滚，需新campaign锁等待原服务端事务结束后核对asset/storageKey；确认未提交才清，已提交或无法核对则保留，仍返回原失败、通过同requestId回执重试恢复。无metadata的保留文件不可下载，后续保留/恢复仍P09。后端先补实际unit RED并实现，证据随后记录。

22:13:05先运行AssetServiceTest，9项中新增3断言实际失败；22:14:09同命令9项通过。以纯transaction Store模拟已提交后确认丢失、恢复库不可用、已回滚但无法核对，验证文件保护、原异常、同requestId原回执/原bytes和无metadata不可下载；没有声称进行真实断网commit实验。22:15:51最终全量 `mvn '-Dmaven.repo.local=D:/workspace/dnd/.tools/m2' -Pintegration verify -B -ntp`：**52 unit + 41真实PG/HTTP/WS全部通过**，0失败/错误/跳过，jar重新打包；即时receipt与延迟commit rollback清理等旧IT全保留。22:16核对PG PID/CommandLine后已停止。本轮最新代码的全量CI仍按新SHA核实，先前容器成功证据不能替代新提交。

## P02b 最终完整 CI 通过

最终代码 **1bfac83d55eb3ccab18778a30e5f5ba54f4e4289** 的 [push run39135326549](https://github.com/xianchen233/dnd-roll/actions/runs/39135326549) 与 [pull_request run39135329930](https://github.com/xianchen233/dnd-roll/actions/runs/39135329930) 均 **completed/success，frontend/backend/browser全部通过**。按该SHA核验而非复用前次运行：前端90测试/构建/24契约，后端日志52unit+41真实PG/HTTP/WS、0失败/错误/跳过；两条browser job均完成标准Compose构建、私有素材卷、原M1/P01/P02a及新素材共4流程和实际db/backend容器重启。PR浏览器日志4 passed（2.1m），随后明确输出seed PASS和新会话verify PASS：SHA-256、地图校准、版本、上传/map原回执、历史回放不重挂图、玩家/跨团/退出后下载拒绝。

至此P02b本地与远端门槛完成，前述pending/failure记录为真实历史，已由对应RED/GREEN与最终新SHA验证关闭。PR #1仍open draft/base main，标题与说明按最终账号/多场景/私有素材范围更新，未合并或发布。当前本机Java/Vite/PG均在核对进程身份后停止，测试数据及外置素材保留。后续交接文档提交仍须单独查看当前PR Checks；不把代码1bfac83的结果冒称为文档新SHA结果。

下一可执行包为P02c棋子作者与共享控制，先契约和授权/隐藏/撤权/旧回执失败测试，再实现与验收。P02d玩家派生图/Pixi/遮盖、P02e整体验收及P03–P10/A继续保留。整个P02和M2未完成；TLS、触屏、容量和完整一致备份/异机恢复不在本轮通过范围。
