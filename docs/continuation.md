# 继续工作的入口

## A03c 目标地图与阻断边界审计（2026-10-24）

开工独立核验8ddbb0d：push40074564212 / PR40074567963均completed/success，
六份新取完整日志通过126unit+158IT、248前端/构建、111契约、52浏览器及全部旧
重启/隔离恢复/容量断言（外.tools/a03c-boundary-entry-ci.log）。
实际两项RED后修复：摘除实况地图后遗漏目标地图缺图；资料节点仅重排被误判变更。
目标地图现校验稳定素材链接、真实字节/哈希/类型/尺寸，预览与应用都在写入前阻断；
资料按身份比较，实际内容/策略/manifest变更仍拒绝。扩展链接/图片/无部分写入边界，
完整126unit+163真实IT（35生命周期）、248前端/构建、111契约通过。
独立18081、新库dnd_a03c_boundary及独立素材上的真实Java/PG重启、新会话逐字段
verify通过；不冒称本地新生产浏览器或新独立卷恢复。TDD及中间测试错误见
[a03c-boundary-tdd.md](a03c-boundary-tdd.md)。验收服务按身份停止，数据保留。
最终HEAD须严格运行外.tools/a03c-boundary-check-ci.mjs；六日志/结果索引为
a03c-boundary-head-ci-final.log、a03c-boundary-ci-{HEAD}.json，pending不算通过。

**体验实例实况修正：**只读检查发现dnd-roll-local三个容器本轮开工前已于16:29退出，
8080无监听。本轮未启停/重建该项目或操作其数据；下方“正在运行”段落为此前状态。
原分支/草稿PR #1保留，不合并、不发布；原source fence/恢复目标保持。
下一可执行：独立核验最新HEAD后审计组合恢复图片ZIP容量与500条确认上限；A03d手动组合、A03e中文控件/
最终联验及A04–A06/M3仍待办，不能宣称A03c/A03全部完成。云端/真实手机继续后置。

## 本机体验实例正在运行（2026-10-24）

用户已要求部署以查看当前状态，现保留本机 Docker Compose 实例运行，地址
http://localhost:8080，项目 dnd-roll-local，代码/镜像版本8ddbb0d。web绑定
127.0.0.1:8080；backend/db仅Compose内部端口。原生验收Java/PG仍停止。
后续测试请使用其他端口/独立项目，不要擅自停止或重置这个用户体验实例。

本地配置/已验收JAR/生产前端/启动停止脚本/账号说明都在仓库外
D:/workspace/dnd/local-preview；密码仅在本地.env/accounts.txt，不进入Git。
Start.cmd启动，Stop.cmd保留数据停止。持久卷dnd-roll-local_postgres_data、
dnd-roll-local_asset_data、dnd-roll-local_backup_data均独立于旧验收库/source fence。
首次Docker Hub认证域名连接失败，日志deploy.log保留；使用Public ECR官方镜像
副本及已验收产物打包，覆盖文件compose.local.yaml，不改标准Compose和源码。

实际本地Chrome双会话登录、Pixi画布、实时连接、DM两棋子/玩家一棋子、无脚本
错误通过（local-preview/browser-check.json、screenshots/{dm,player}.png）。
已有合成示例地图/遮盖、角色卡/受损生命、隐藏守卫、TEAM/DM笔记与RP；不是私人团数据。
本次仅部署和运行记录，不实现新业务或冒称A03c/A03d/e全部完成；云端/真实手机后置。
下一开发步骤仍按下文A03c交接；本地Docker体验实例应继续保留运行。


## A03c 全部场景退役后的连续迁移与未映射对象恢复（2026-10-24）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后本地/origin/PR 精确 HEAD 8578e76，独立重取 push39988746436 /
PR39988753462 六份完整日志，均 completed/success：126unit+155真实IT、248前端/
构建、111契约、52浏览器及全部旧重启/隔离恢复/容量断言通过。
外 .tools/a03c-empty-entry-ci.log；不得复用为新 HEAD 的结果。

实际两组 RED：删光映射场景后的兜底场景阻断后续迁移；额外实况场景/棋子被阻断。
第三组 RED：缩图遗漏未映射棋子边界。现在逐项确认后恢复所有相关实况场景、地图、
关联角色/无角色棋子及独立图片，保留名称/位置/隐藏/当前场景，控制仅归迁移 DM。
原团未映射对象不纳入模板映射；活动棋子仍在退役场景中时也保留可玩恢复场景，
已退役模板棋子不复活。缺图/损坏/超限/失效链接在写入前阻断；即时/延迟回滚删除
新恢复文件，原数据/图片保持。契约 module-updates.md，真实 TDD a03c-unmapped-tdd.md。

最终代码完整126unit+158真实IT零失败/错误/跳过（a03c-unmapped-full-final.log）；
248前端、生产构建、111契约通过。实际Java/PG停止、同库/素材根重启后，新会话逐字段
核验三次迁移、兜底/额外场景、关联角色/无角色棋子、全部恢复副本/图片/受损资源/
前像和原回执通过（a03c-unmapped-restart-{seed,verify}-final.log）。不冒称本地跑过
新生产浏览器或新独立卷恢复；这些由精确新HEAD双CI完整日志核验，pending不等于通过。
检查器a03c-unmapped-check-ci.mjs要求158IT及全部旧断言、新4条UNMAPPED VERIFIED；
结果索引为外.tools/a03c-unmapped-head-ci-final.log、a03c-unmapped-ci-{HEAD}.json及
六份a03c-unmapped-job-*.log；不得复用开工SHA的结果。

原source fence、恢复目标/素材保持。验收Java/PG已按实际身份停止，8080/8081/5173/
55432无监听；清除数据库环境后离线126unit通过（a03c-unmapped-unit-pg-stopped.log）。
A03c仍保留缺失/变化链接、危险缩图、恢复容量/图片完整性、资料/策略/manifest阻断边界。
A03d手动组合/A03e中文控件与最终联验、A04–A06/M3继续待办；云端/TLS/物理异机/
真实手机后置。本轮不冒称整个A03c/A03完成。下一可执行：恢复时先独立核验最新HEAD；
按契约继续A03c尚未覆盖的边界审计，再推进A03d，保留A03e最终联验。


## A03c 棋子迁移与缩图边界（2026-10-24）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工fetch确认本地/origin/PR为d14871f，重新下载push39985141565/PR39985151305
六份完整日志，均completed/success：126unit+153真实IT、248前端/构建、111契约、
52生产浏览器及全部重启/隔离恢复/容量断言通过（外.tools/a03c-next-entry-ci.log）。

本轮不重做A03a/b及文字切片。实际两组RED：只改棋子隐藏重置实况(7,8)为(2,3)；
缩图并明确移回界内仍按旧位置阻断。预览/应用现在共用逐轴合并：未改轴保留实况，
明确修改轴采用新定义，跨场景使用目标坐标；已隐藏棋子不因公开定义自动揭露。
缩图按迁移后位置与退役判断，仍阻断实际滞留界外的棋子；恢复副本保留原位置。
旧回执在第二次迁移后精确重试不重放、不回退位置；玩家不可应用。契约module-updates.md，
真实TDD/脚本准备错误见a03c-placement-tdd.md。

聚焦126unit+27生命周期IT，最终完整126unit+155真实IT零失败/错误/跳过；111契约通过。
实际Java/PG重启后新会话逐字段verify通过：迁移(9,8)/仍隐藏，恢复(7,8)、受损资源、
前像/原回执及全部旧生命周期保持。脚本新增隐藏断言先误读快照缺省字段，已用DM运行时
证据和玩家真实不可见检查修正，重新seed/verify通过；非产品RED。未声称本地重跑
前端/生产浏览器或独立卷恢复；新HEAD双CI须实际核验这些门槛。
证据外.tools/a03c-placement-{red,green,full,restart-seed-final,restart-verify-final}.log。
精确HEAD六日志索引：a03c-placement-ci-{HEAD}.json、a03c-placement-job-*.log及
 a03c-placement-head-ci-final.log；检查器要求155IT和全部旧断言，pending不等于通过。

A03c仍有明确边界：未映射场景/棋子/地图、缺失链接、资料/策略/manifest变化仍阻断；
删除全部映射场景后的后续迁移及未映射实况对象恢复仍需继续检验，不能宣称A03c全部关闭。
A03d手动组合/A03e中文控件和最终联验、A04–A06/M3保持；云端/TLS/物理异机/真实手机后置。
原source fence、恢复目标/素材保持；本机验收服务已按身份停止。
本地8080/8081/5173/55432无监听；停PG并清除数据库环境后离线126unit通过
（a03c-placement-unit-pg-stopped.log）。


## A03c 连续迁移恢复补齐（2026-10-24）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后本地/origin/PR 精确 HEAD e6d9651；独立重取 push39836569868 /
PR39836575609 六份完整日志，均 completed/success：126unit+151真实IT、
248前端/构建、111契约、52浏览器及全部重启/隔离恢复/容量断言通过。
证据外 .tools/a03c-resume-entry-ci.log；不复用为新HEAD结果。

A03a/b及文字切片保持。实际RED证明第二次迁移恢复副本遗漏旧参战角色，另一次
RED证明额外实况角色被阻断；恢复现在复制全部实况卡（含退役参战者/额外角色），
逐项RECOVER_EXTRA_UNIT确认、独立身份及DM-only控制，原团映射/控制/回合/历史保持。
恢复身份避让节点/棋子冲突；预览校验完整恢复定义，容量超限明确阻断且无部分写入。
契约 contracts/module-updates.md；真实命令、准备错误、RED/GREEN见 a03c-recovery-tdd.md。

最终全量126unit+153真实IT零失败/错误/跳过，248前端/构建、111契约通过。
首轮全量因同时启动验收服务耗尽PG连接失败，保留日志；停验收服务独占重跑通过。
实际Java/PG重启后新会话verify通过：额外受损卡、独立恢复、原映射/前像/原回执及旧
生命周期逐字段保持。未声称本地跑新生产浏览器或新独立卷恢复；这些由新HEAD CI验证。
外 .tools/a03c-recovery-{full-final,frontend,build,restart-verify-final}.log。
最终HEAD必须重新核验push/PR六日志：a03c-recovery-check-ci.mjs严格检查153IT及全部旧
断言，索引a03c-recovery-ci-{HEAD}.json、a03c-recovery-job-*.log、a03c-recovery-head-ci-final.log。
新CI未完成不得报通过。

A03c仍有明确边界：未映射场景/棋子/地图、越界缩图、缺失链接、已发布资料/策略/
manifest变化仍阻断；A03d手动组合/A03e中文控件及最终联验、A04–A06/M3保持待办。
云端/TLS/物理异机/真实手机后置；原source fence、恢复目标和素材保持。
本地验收Java/PG已按身份停止，8080/8081/5173/55432无监听；清除数据库环境后
离线126unit通过（a03c-recovery-unit-pg-stopped.log），原数据/素材保留。



## A03c 结构迁移服务切片（2026-10-23，本地验收通过，精确 HEAD CI 待核验）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布；A03a/A03b 和
3c5fb1d 文字切片保持。契约 contracts/module-updates.md，实际 TDD 见
[a03c-structure-tdd.md](a03c-structure-tdd.md)。独立结构预览绑定源/目标版本及实况
快照；逐项明确确认后，事务创建 DM-only 可玩恢复战役、迁移场景/地图/角色定义/
棋子映射并写前像/回执。删除场景/棋子采用退役保留历史引用；替换角色不自动
加入/改写实况先攻或回合，旧参战身份/HP/资源/状态/剧情及全部历史保持。
恢复副本保留迁移前映射对象的受损卡/几何/位置和独立素材，地图全盖；不重放历史。
替换棋子不继承旧玩家控制；隐藏变更不重置实况位置，公开定义不自动揭露旧棋子。
未映射实况对象、缺失链接、越界缩图、已发布资料/策略/manifest变化仍明确阻断。

开工独立重取3c5fb1d的六份完整日志：各126unit+146IT、248/构建、108契约、
52浏览器及全部重启/隔离恢复断言通过；push39827303900汇总仍in_progress，
严格校验拒绝标双CI完成，PR39827311813独立报告completed/success。证据外.tools/
a03c-structure-entry-*.log。不能将成功job冒充两个run已完成。
本地首轮及最终完整126unit+151真实IT均零失败/错误/跳过；控制继承/重复迁移
均有真实RED→最终GREEN。248前端/生产构建、111契约及旧path/schema结构比较通过。
实际Java/PG重启后，新会话逐字段核验结构恢复副本/地图/受损资源/前像/原回执及
全部旧生命周期通过；未冒称本地执行生产浏览器或新的独立卷恢复。精确最终HEAD
push/PR六日志索引：外.tools/a03c-structure-ci-{HEAD}.json、a03c-structure-job-*.log
与a03c-structure-head-ci-final.log，必须实际严格通过后才算双CI完成。
下一可执行：独立核验上述最终HEAD；继续A03c显式blockers范围/A03d手动操作及A03e控件。
A03c不在此冒称全部关闭；A03d手动操作/A03e中文控件与最终联验、A04–A06/M3保持。
停PG并清除数据库环境后离线126unit通过；8080/8081/5173/55432无监听。
云端/TLS/物理异机/真实手机继续后置；source fence、恢复目标及素材保持。


## A03c 首个安全切片（2026-10-23，进行中）

继续沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后本地/origin/PR 精确 HEAD c105a5da8239d49aefe8507131a54c6570af6322。
独立重新下载 push39821053960 / PR39821061499 六份完整日志，严格核对
126 unit + 143 IT、248 前端/构建、105 契约、52 生产浏览器及原全部重启/隔离恢复。
全部 completed/success；证据在仓库外 .tools/a03c-entry-ci.log。

A03a/A03b 不重复开发。A03c 本轮新增独立实例文字定义影响预览/应用：源和目标
当前 DM、双版本、服务器重算 hash、事务内前像/实例/持久回执、精确重试不重放。
稳定映射、剧情进度和原始包来源保持；不写任何生命/资源/状态/回合/场景/笔记/记录。
所有超出节点 name/text 的变化，以及素材/依赖/权利变化，都列为 blockers 并拒绝应用。
这只是备团文字定义切片，**A03c 尚未完成**：实际场景/地图/角色定义同步、删除场景/
替换参战者的明确影响确认及可恢复结构迁移仍为下一可执行工作；不能把文字更新
冒称实况结构更新。A03d/A03e、A04–A06/M3 均保留；云端/真实手机继续后置。
契约 contracts/module-updates.md；实际 TDD/故障与验收 docs/a03c-tdd.md。
本地最终完整 126 unit + 146 真实 IT 零失败/错误/跳过；248 前端/构建、108 契约通过。
实际 Java/PG 重启后新会话验证前像/原回执及全部资源/剧情快照通过；停 PG 并清除
数据库环境后离线 126 unit 通过。8080/8081/5173/55432 无监听。首次准备/全量错误
及实际修正均见 a03c-tdd.md；不冒称本地跑过新的生产浏览器。最终 HEAD 双 CI
严格结果索引为外 .tools/a03c-head-ci-final.log、a03c-ci-{HEAD}.json 和六份 job 日志。
原 source fence/恢复库/素材保持；本轮验收使用独立 a03c-backups，不能移除源端停写。


## A03b 服务实现与验收索引（2026-10-23）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
开工 fetch 后本地/origin/PR 为48b086aa8a72266303d1f1f347c42d67dc5728f2；独立重新下载
push39811174761 / PR39811184432六份完整日志，两组121unit+136IT、248前端/构建、100契约、
52生产浏览器、十二组实际重启与隔离恢复/完整团/容量全部成功。证据外.tools/a03b-entry-ci.log。

A03a保持，未重复开发。A03b服务/typed客户端实现逐项KEEP/INCOMING、显式冲突决定、
manifest权利/素材字节/精确依赖三方比较，服务器重算版本/包hash/组合闭包绑定的影响预览；
事务保存定制/有效ZIP/前像/来源/实际操作者/持久回执，精确重试不重放，撤权先于旧回执。
原始来源包/基线保持，另存和新团使用组合素材；旧实例/受损HP/RP/剧情不变。
DM可下载独立身份的前像ZIP恢复为另一个副本。V20保留全部旧列/行及19个checksum。
契约 contracts/module-updates.md，真实RED/GREEN及准备错误 docs/a03b-tdd.md。

本地完整126unit+140真实IT零失败/错误/跳过；最终追加2HTTP+1迁移后聚焦18IT全过，
独立共143IT（原完整命令不是143）。248前端/生产构建、105契约，原100路由及全部旧schema
结构保持。恢复目标上新包seed→实际Java/PG重启→新会话verify通过，含有效包/前像ZIP hash/
选择来源/原回执与旧团逐字段保持；未把准备时ECONNREFUSED当行为RED。证据外.tools/a03b-*。
新增检查已接入原lifecycle容器重启/独立新卷恢复，精确提交的双CI结果须重新取得，不能沿用开工SHA。

本地最终完整生产Chrome **52 passed（2.5m）**，原断言/60秒/零retry保持。
首次完整51pass/1fail：旧P02e准备地图初始化模块报net::ERR_NO_BUFFER_SPACE（trace留存），
独立原P02e四流程17.3s全过后再跑完整52通过；不是改断言或掩盖首次失败。
已按实际命令行停止Java/preview/PG，8080/8081/5173/55432无监听；清除数据库环境后离线
126unit全过（a03b-unit-pg-stopped.log）。source fence与恢复目标/素材保留。

代码61305b3f8ca46a382b89bbbb98a58b1a853fd48f的push39818867630 completed/success；
已重取三个完整日志严格核对126unit+143真实IT、248前端/构建、105契约、52生产浏览器、
原43PASS/4MODULE/4LIFECYCLE及新增4UPDATE VERIFIED、十二组实际容器重启与独立新卷
恢复/完整团/待确认行动/恢复后3流程/9会话100棋子72消息errors=0。PR39818876262在本文
更新时尚in_progress，不冒称其已通过；严格检查已拒绝未完成状态。最终文档提交会触发
新的push/PR，均必须按精确HEAD重新下载六份完整日志；验收索引为外.tools/
**a03b-head-ci-final.log、a03b-ci-{HEAD}.json及a03b-job-{jobId}.log**。恢复工作时重新fetch
并独立核验最新HEAD，不沿用61305b3或历史SHA的结果。原草稿PR范围已更新。

A03尚未完成：下一包A03c单独预览/应用到进行中实例，保护生命/资源/状态/回合/剧情/记录；
删场景/替换参战者须明确影响确认和可恢复副本。随后A03d可编辑手动操作组合和原服务原子执行；
A03e中文选择/影响/触发控件、不确定原请求重试/切域撤权隔离与完整生产浏览器/重启恢复/双CI。
A04–A06/M3仍未完成；云搭建/部署/HTTPS/WSS/物理异机恢复和真实手机继续后置。
原source fence、恢复目标配置及素材保持。运行时/凭据均在仓库外.tools；PID以实际身份核验。
以下为A03a及更早历史。


## A03 进行中：三方差异预览已实现（2026-10-22）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
开工 d40f03f 已 fetch，独立重取 push39669763972 / PR39669771205 六份完整日志，
116unit+133IT、243前端/构建、99契约、51浏览器、十二组重启和隔离恢复全部通过。
首次两次 fresh 查询未取得预期 runs，严格失败；第三次完整重取通过，未复用旧SHA。

A03 契约 [module-updates.md](../contracts/module-updates.md)，实际 RED/GREEN、准备错误
与全部本地证据见 [a03-tdd.md](a03-tdd.md)。本轮完成 A03a 只读三方差异切片：
稳定UUID匹配原始基线/已保存定制/新模板，字段递归比较，引用列表原子比较，
删除/编辑和不同新增冲突保留三值；列表重排不产生差异。当前DM、版本、同模组/
规则/不同版本校验，缺失依赖明示。中文比较视图切域/版本/撤权清理与晚到响应隔离。
预览不写副本、回执或实例，不应用任何资源或触发动作。

本地完整121unit+135真实IT零失败/错误/跳过；再追加一项HTTP边界后聚焦3项全部通过
（最终独立IT共136，不能说前次完整命令已跑136）。248前端/生产构建、100契约、
完整52生产Chrome（保留原51、60秒/零retry、2.6m）通过。旧99路由/全部旧schema结构保持。
真实浏览器先扣HP并发消息，再展开比较，原卡/消息/副本/实例逐字段保持；1440/390px
截图与无溢出通过，窄屏浏览器不冒称真实手机。无迁移/新持久化写路径。
代码 **4f11ceeafa88cac270bc9827ca9276655116cc8f** 的
[push39808922365](https://github.com/xianchen233/dnd-roll/actions/runs/39808922365) /
[PR39808930410](https://github.com/xianchen233/dnd-roll/actions/runs/39808930410)六job均completed/success。
2026-10-23独立重取六份完整日志，严格核对每组121unit+136真实IT零失败/错误/跳过、
248前端/构建、100契约、52生产浏览器、原43PASS/4MODULE/4LIFECYCLE、十二组实际
容器重启、真实迁移归档及独立Compose新卷离线恢复、完整团两次逐字段比较、
待确认结果、恢复后3流程和9会话/100棋子/72消息容量errors=0，全部通过。
证据外.tools/a03-code-ci-final.log、a03-ci-4f11ceeafa88cac270bc9827ca9276655116cc8f.json
及a03-job-{jobId}.log。按HEAD的列表间歇空结果时，直接用已知run ID并断言SHA；
首次严格检查拒绝in_progress，最终completed后才通过。原草稿PR已更新范围。
文档最终HEAD仍须独立核对同等日志；最终索引为外.tools/a03-head-ci-final.log和
a03-ci-{HEAD}.json。A03a仅定义字段差异，不比较manifest素材字节/依赖/权利声明，
无差异不等于相同包或可以应用；这些列入A03b。停PG并清除数据库环境后121unit通过，
本地验收Java/preview/PG已按身份停止，8080/8081/5173/55432无监听。

**A03仍未完成**：下一可执行 A03b 为明确逐项KEEP/INCOMING与冲突决定、服务器重算
版本/hash绑定的影响预览、素材/依赖闭包及原子应用/持久回执/可恢复前像。
其后A03c进行中实例更新保护生命/资源/回合/剧情/记录，删除场景/替换参战单位须
影响确认及可恢复副本；A03d DM可编辑手动操作组合与原服务执行/重试不重放；
A03e最终生产浏览器/实际重启与隔离恢复/精确HEAD双CI。不得重做A01/A02或跳过后续。
A04–A06/M3、云端搭建/部署/HTTPS/WSS、物理跨主机恢复和真实手机继续后置。
原source fence及恢复库/素材保持。证据/运行时均在仓库外D:/workspace/dnd/.tools（a03-*）。
以下A02段为历史，当前下一步以上述A03状态为准。

<!-- A02 current -->
## A02 已完成；下一包 A03 尚未开工（2026-10-22）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
开工8322dce已fetch并独立重取push39653418130/PR39653430398六份完整日志核验通过。
契约 [module-lifecycle.md](../contracts/module-lifecycle.md)，实际RED/GREEN、准备错误和
全部修正见 [a02-tdd.md](a02-tdd.md)。P01–A01及P10本地/容器功能验收保持。

模板原件、不可变编辑基线、独立定制副本与新战役身份分离。v2显式定义场景/地图、
完整角色卡/初始资源、预摆棋子、资料及准入；v1满额内容/多图惰性文档兼容保持。
DM可完整编辑、另存新身份/版本并导出；新团拥有独立素材、角色、笔记、控制与剧情。
进行中的HP/资源/RP/历史不进入模板，不因另存或新模板导入覆盖。当前源/目标DM、
版本/回执/并发/精确依赖、即时及延迟提交回滚均通过。未知提交结果先用新事务source锁
及已提交元数据确认再清理新图。切团仅使用匹配当前战役的场景，避免跨域地图/控制读取。

本地最终完整116unit+133真实IT（含模块14unit+13HTTP），零失败/错误/跳过；停PG并清除
数据库环境后Maven离线116unit亦通过。243前端/生产构建、99契约、完整51生产Chrome
（原49保留、60秒/零retry）、V18全部旧列/行/18checksum通过。旧91操作/schema保持，
仅增加显式manifest v2及可选完整定义预览。十二组实际Java/PG重启、新会话逐字段
比对、完整测试团/待确认结果、真实手动部署归档含V19及新表/图片摘要均通过。

代码 **7d16737df7c72b748dc81bdf9ce46c2cae57d464** 的
[push39666272709](https://github.com/xianchen233/dnd-roll/actions/runs/39666272709) /
[PR39666278838](https://github.com/xianchen233/dnd-roll/actions/runs/39666278838)六job均completed/success。
独立重取六份完整日志并严格断言每组116+132、243/构建、99契约、51生产浏览器、
原43PASS、4MODULE VERIFIED和4LIFECYCLE VERIFIED：十二组实际容器重启、真实迁移
归档和独立项目/新卷离线恢复后原件/基线/副本/两团资源/剧情/旧记录/权限及回执保持。
完整团两次逐字段比较、待确认结果、恢复后3流程及9会话/100棋子/72消息容量errors=0。
a02-code-ci-verified.log、a02-ci-7d16737df7c72b748dc81bdf9ce46c2cae57d464.json和
a02-job-{jobId}.log在仓库外.tools。A02关闭。

最终复核另补内置依赖准入往返：保持builtin自动准入，只映射复制的自定义批准条目，
新团可继续保存P04策略。实际HTTP400 RED→完整116+133 GREEN见a02-policy-full-final.log。
含此修正的最终HEAD须按116+133/243/构建/99/51重取同等六日志，不沿用7d16737结果；
最终索引为外.tools的a02-head-ci-verified.log、a02-ci-{HEAD}.json及原PR #1。

本地验收Java/preview/PG均已按实际身份停止，8080/8081/5173/55432无监听。
原source fence、p09-target-database.json和p09-restored-assets-final保持；证据/运行时在
D:/workspace/dnd/.tools（a02-*）。当前PID必须重新核对。Windows PG启动请分步执行，
不要依赖旧helper管道返回。GitHub连接器写元数据403后已用现有Git凭据更新原草稿PR。

下一可执行包 **A03 选择性更新与手动触发**：恢复后先fetch核对最新HEAD的push/PR
完整证据，再固定原始基线/当前定制/新模板的稳定身份三方差异、冲突确认、影响预览
及DM可编辑的手动操作组合契约，实际RED证明更新不覆盖进行中资源/记录与重试不重放。
不重复A01/A02惰性导入或独立实例。A03–A06及整体M3未完成；云环境准备/部署、
HTTPS/WSS、物理跨主机恢复和真实手机验证继续按用户要求后置。以下为历史交接记录。
<!-- /A02 current -->



## A01 已完成；下一包 A02 尚未开工（2026-10-22）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
开工682af77已fetch并独立重取push39643825879/PR39643836476六份完整日志通过。
M2业务及P10本地/容器功能验收保持；云端最终门槛和真实手机仍待执行。

A01契约 [module-packages.md](../contracts/module-packages.md)，实际TDD及准备失败记录
见 [a01-tdd.md](a01-tdd.md)。有界严格ZIP/JSON、稳定模组/版本/库副本身份、真实
PNG/JPEG/哈希/内部引用、精确依赖缺失可见、旧版明确确认、DM-only惰性预览/导入/
原ZIP下载、默认复用/显式副本、版本和持久回执事务原子性均已完成。
v1保存描述/稳定链接/图片；创建定制/战役与typed载荷属于A02，不执行包内代码。

实际RED→GREEN：7归档、2真实HTTP404、6前端、4路由契约及1已解析依赖展示。
追加并发/撤权/插入和延迟提交回滚、V17→18旧列/行/17checksum均通过。
最终109unit+124真实IT零失败/错误/跳过，234前端/生产构建、91契约、49生产Chrome
（原47保留、60秒/零retry）及最终新增2流程5.0s通过。实际Java+PG重启后模块
ZIP/图片/依赖/副本/原回执、原十组业务及完整团均保持；真实手动备份包含V18模组。
停PG/清除数据库环境后Maven离线109unit通过；验收Java/preview/PG按身份停止，
8080/8081/5173/55432无监听。原source fence、恢复目标库/素材保留，证据在外.tools。

代码 **2a9bb2a9c2455cda06944d7068b88854378972d6** 的
[push39651535455](https://github.com/xianchen233/dnd-roll/actions/runs/39651535455) /
[PR39651544111](https://github.com/xianchen233/dnd-roll/actions/runs/39651544111)六job均completed/success。
独立重取六份完整日志，严格断言每组109+124、234/构建、91契约、49生产浏览器、
43原PASS与4条MODULE VERIFIED：十一组实际容器重启、真实归档和独立项目/新卷
离线恢复后原ZIP/依赖/副本/回执/权限保持；完整团两次逐字段比较、恢复后3流程、
9会话/100棋子/72消息容量errors=0均通过。a01-code-ci-verified.log、
a01-ci-2a9bb2a9c2455cda06944d7068b88854378972d6.json及a01-job-{jobId}.log在外.tools。

A01关闭；完成文档的新HEAD仍须独立核验同等完整证据，恢复工作时重新fetch核对。
下一可执行包 **A02 模板、定制副本与战役实例**：先固定模板原件/编辑基线/定制副本/
目标战役独立身份、权限和typed交换载荷版本；实际RED证明同模板两个团资源/进度
互不影响、另存模板不覆盖当前团或旧记录，再实现最小切片。不要重做A01惰性导入。
A02–A06和整体M3未完成。云环境准备、部署、HTTPS/WSS与物理跨主机恢复继续在
整体功能完成后统一联验；真实手机仍未实测。以下为此前历史交接。


<!-- P10 current -->
## P10 本地/容器功能验收通过；云端最终门槛后置（2026-10-22）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
开工 befd8c1 已 fetch 并独立核验 push39634782600 / PR39634789666 六份完整日志，
102+118、225/构建、87契约、44浏览器、十组实际容器重启与隔离恢复43PASS全部通过。
P01–P08和P09功能/本地/容器验收保持；P10契约 contracts/p10-acceptance.md，
逐项产品矩阵、实际RED/GREEN/准备错误和容量数据见 [p10-acceptance.md](p10-acceptance.md)。

新增手机工作区导航，跨标签保留挂载草稿/原请求，切团/失权清理；1个真实缺导航RED→GREEN，
再加直接GREEN边界覆盖。227前端/生产构建、87契约、47生产Chrome（原44保留、60秒、零retry）
和最终新3流程16.8s通过。连续DM+两玩家同团覆盖双角色/代管/异角色RP、秘密地图像素、
准入/骰子/分组先攻/第二轮/手动应用及更正/到期提醒/团队笔记和实际断网撤权。
102unit+118真实IT串行零失败/错误/跳过；停PG/清除数据库环境后Maven离线102unit通过。
实际Java+PG重启后完整团所有公开/私密快照、原骰子/待确认行动、十组原业务新会话核验通过。

9独立HTTP会话/100棋子/72消息的本机容量通过：2.66MB地图p95 764ms，组合读取p95 152ms，
完整备份3.92MB/1.55s；Java working set峰值568.7MB，PG进程求和366.6MB（含共享页重复）。
不是WAN/TLS/2vCPU云主机性能保证；物理手机尚未实测。CI新增完整团重启/隔离恢复逐字段
比较，以及恢复后新团/手机/容量和容器内存证据，精确代码SHA两组已全部通过。

本地Java/production preview/PG已按实际身份停止，8080/8081/5173/55432无监听；
原 source fence、p09-target-database.json和p09-restored-assets-final保持。凭据/证据均在外.tools。
最终p10-backend-final.log、p10-unit-no-db.log、p10-frontend-final.log、p10-browser-full.log、
p10-final-browser-focus.log、p10-restart-{seed,verify}.log、p10-campaign-restart-verify.log、
p10-capacity-local.json/p10-capacity-processes.json及p10-final-stopped.log留存。

代码 **09f81d1f4cfd9c616aba13a959d6378e7b13ddc3** 的
[push39641891945](https://github.com/xianchen233/dnd-roll/actions/runs/39641891945) /
[PR39641904875](https://github.com/xianchen233/dnd-roll/actions/runs/39641904875)六job均completed/success。
独立重取六份完整日志，严格断言两组102+118零失败/错误/跳过、227前端/构建、87契约、
47生产浏览器、原43PASS、十组容器重启、真实完整归档和独立项目/新卷离线恢复。
每组完整团逐字段验证两次（重启和恢复），恢复后再运行新团/两种手机3流程和9会话容量，
均errors=0。外置p10-code-ci-verified.log/p10-ci-09f81d1f4cfd9c616aba13a959d6378e7b13ddc3.json及p10-job-{jobId}.log。
P10本地/容器功能验收关闭；完成状态文档的新HEAD也必须独立核验同样完整证据。
下一可执行功能包为 **A01模组包身份、格式与导入校验**：先读交接及必读契约，fetch核对
最新HEAD的push/PR Checks，再固定版本/依赖/引用/原子失败契约并实际RED→GREEN。
云环境准备、部署、公共HTTPS/WSS和物理跨主机恢复按用户要求留到整体功能完成后统一联验。
这些最终门槛及物理手机/A/M2仍未完成，不以本地结果冒称完整P10或发布完成。
<!-- /P10 current -->

## P09 功能与本地/容器验收通过；云端联验按用户要求后置（2026-10-22）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，
不合并、不发布。开工63ad3af已独立核验push39618022510/PR39618030841六job完整日志；
P01–P08保持完成，不重复开发。当前HEAD和工作区始终以Git实际状态为准。

P09契约 [backups.md](../contracts/backups.md)，实际RED/GREEN及准备错误见
[p09-tdd.md](p09-tdd.md)。部署管理员独立授权、手动/每日默认7份、失败/轮换警告、
停写等待在途操作、真实PG逻辑归档+不可变素材、严格版本/哈希/引用/逐表摘要校验、
隔离空库/新素材根离线恢复与持续源端停写、中文管理和下载已实现。
225前端/构建、87契约、原83操作/全部旧schema结构保持、44生产Chrome通过。
十组实际备份→独立目标库/新根302文件恢复→新会话业务verify和待确认行动保持均PASS。
源实例真实重启后仍停写，目标启动前源实例已停止；两端应用及preview已停止。
最终102unit+118真实IT串行复验已通过，零失败/错误/跳过；停PG并清除数据库环境后
独立102unit也通过。之前并行连接耗尽的失败完整保留，不冒充行为RED。

代码 **f6c9130c0bbdd7d6dd1aa9ddf235074bee827cb0** 的
[push39633129367](https://github.com/xianchen233/dnd-roll/actions/runs/39633129367) 与
[PR39633137675](https://github.com/xianchen233/dnd-roll/actions/runs/39633137675)
均completed/success。重新读取六份完整日志，两组均102unit+118真实IT零失败/错误/跳过、
225前端/构建、87契约、标准Compose44流程、十组实际db/backend重启通过；随后各自
十组新seed→实际迁移归档→源实例停止→独立dnd-roll-restored项目/全新卷离线恢复→
待确认行动保持及十组fresh-session verify全部通过，每组43条PASS与RESTORE VERIFIED。
首次严格检查在PR尚in_progress时拒绝通过，完成后重新取得六份才通过。证据在外置
p09-code-ci-verified.log、p09-ci-f6c9130c0bbdd7d6dd1aa9ddf235074bee827cb0.json、
p09-job-{jobId}.log；凭据仅内存。完成文档的新HEAD也须独立核验，不沿用代码SHA结果。

下一可执行包为 **P10完整测试团与功能验收**：先按产品首版验收逐项建立证据清单，
在恢复后的目标上运行跨功能完整测试团、重试/断网/权限与容量/触屏检查，缺口如实保留。
用户已有尚未准备环境的云服务器，明确要求
整体功能完成后，将云环境搭建、部署和物理异机恢复统一联验；本轮不连接/部署云端。
该最终门槛保持未通过，不阻止后续P10功能验收准备；P10/容量/触屏/TLS/A/M2不冒称完成。

运行时/私有证据在仓库外.tools。p09-acceptance.jar、start-p09-backend.ps1、
run-p09-evidence.ps1和p09-final-*证据留存；新seed必须新前缀（wx拒覆盖）。
source browser库和browser-assets保留，p09-backups/write-fence保持；恢复目标配置
p09-target-database.json及p09-restored-assets-final保留，不打印凭据。后续验收应使用
恢复后的目标配置和素材根；不要解除源fence后与目标并行写入。PG及两端应用/preview
已核对实际身份停止，8080/8081/5173/55432均无监听。以下是历史交接记录。

## P08 已完成；下一包 P09 尚未开工（2026-10-21）

沿用 feat/accounts-and-invitations / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。工作区与最新HEAD以Git实际状态为准。
P08代码 **1450e5db875c49e29d6446183d9f3851a32ff757** 的 [push39615748242](https://github.com/xianchen233/dnd-roll/actions/runs/39615748242) 与
[PR39615751296](https://github.com/xianchen233/dnd-roll/actions/runs/39615751296) 均completed/success，frontend/backend/browser六job全绿，PR open/draft/main。
逐份重新获取六份完整日志，严格确认两组均87unit+114真实PG集成（零失败/错误/跳过）、221前端/生产构建、83契约、
标准Compose完整43浏览器及十组seed→实际db/backend容器重启→十组新会话verify全PASS。
证据在仓库外 p08-ci-1450e5db875c49e29d6446183d9f3851a32ff757.json、p08-job-{jobId}.log及p08-code-ci-verified.log；认证仅在内存。
首次推送后PR API短暂仍返回旧SHA，严格核验拒绝通过；待PR更新才确认新SHA与两组run，不拿旧HEAD结果替代。

契约 [notes.md](../contracts/notes.md)：TEAM由当前成员共同编辑、DM资料仅当前DM可见，创建后可见性固定。
持久图片、实际编辑者及不可变历史、版本/独立回执、字面搜索/分页、完整Markdown/JSON导出及附件清单均遵守当前权限。
中文工作区保留原文件/原请求精确重试；断网、重连、切团、失权清理旧数据与blob，不静默覆盖冲突草稿。
实际RED/GREEN见两端TDD：notes两条404→2GREEN、图片404→5GREEN、前端7RED→7GREEN、入口RED→GREEN、契约缺路径RED→83操作GREEN。
真实浏览器发现详情回读覆盖输入、WS刷新覆盖未确认状态；补两条聚焦测试实际9项2fail→9GREEN并修复，四条新增生产流程全通过。
准备/环境错误与初次迁移夹具比较失败均如实保留，未冒充行为RED。修复夹具清理后原列/行/15checksum全部保留并通过。

本地独立无数据库87unit、完整87+114后端、221前端/构建、83契约、43生产Chrome（原39保留，60秒/零重试）、V15全部旧表原列/行/15checksum，
及十组真实Java/PG停止/确认旧端口关闭/同库同素材根启动/新会话verify亦通过。原75HTTP操作及全部原schema与开工HEAD结构相等。
笔记/历史/图片SHA/导出/原回执完整保留，撤权/重入仍检查当前权限。外置p08-backend-final.log、p08-unit-no-db.log、p08-frontend-final-copy.log、
p08-build-final-copy.log、p08-contract-final.log、p08-browser-full.log、p08-restart-{seed,verify}.log及进程证据JSON。
本地验收Java/preview/PG已停止，8080/5173/55432无监听；数据库、素材及证据留存仓库外p08-final-stopped.log/p08-stopped-processes.json。

**P01–P08完成；整个M2、P09/P10/A仍未完成。** 本次完成状态文档提交的新HEAD也必须独立核验push/PR完整日志；恢复工作时不可只读本段即假定最新HEAD已绿。
下一步：先按AGENTS读交接/验收/必读契约，fetch独立核对最新HEAD（含本文档）的push/PR Checks；再进入 **P09 备份、恢复与迁移**。
先写契约并按序切片：每日自动备份默认7份、手动生成/下载与失败可见；暂停新写入并等待在途操作后数据库逻辑归档+不可变素材+版本清单；
校验版本/引用/文件哈希和待确认结果，隔离目标数据库及素材根实际恢复，迁移时旧主机停止写入；轮换不得删除RP/笔记/战斗历史或仍被历史引用的图片。
不能把P07/P08记录导出当部署备份，也不能用同库进程重启代替异机恢复。每切片实际RED→GREEN→完整回归；P01–P08不重复开发。
保留P10完整测试团、容量/触屏/TLS与异机恢复门槛。不创建外部定时任务、不合并、不发布。以下为此前历史记录。

## P07 已完成；下一包 P08 尚未开工（2026-10-21）

继续 feat/accounts-and-invitations / [草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，
不合并、不发布。最新HEAD与工作区以Git实际状态为准；完成状态文档HEAD亦独立核验。

P07代码 **5d96a72ee441741f702fb7cfd1641016a6cd8d8f** 的
[push39485707929](https://github.com/xianchen233/dnd-roll/actions/runs/39485707929) 与
[PR39485715062](https://github.com/xianchen233/dnd-roll/actions/runs/39485715062)
均completed/success，frontend/backend/browser六job全绿，PR open/draft/main。
逐份重新获取完整日志并严格核对：两组均84unit+108真实IT、211前端/生产构建、75契约，
标准Compose完整39浏览器以及九组seed→实际db/backend容器重启→九组新会话verify全PASS。
外置p07-job-118845439558/118845439977/118845440055及118845464786/118845465315/
118845465645，汇总p07-ci-5d96a72ee441741f702fb7cfd1641016a6cd8d8f.json和
p07-code-ci-verified.log。首次严格检查在PR尚in_progress时退出，保留p07-code-ci-pending.log；
待完成后重新获取全部六份并通过，不将该未完成检查或旧SHA结果当作新检查成功。

本地独立无库84unit、最终84+108完整后端、211前端/构建、75契约、39生产Chrome、
V12全部旧表原列/行/12checksum，以及九组实际Java+PG停止/同库同素材根启动/新会话
verify亦全部通过。来源/身份/编辑/图片SHA/场次/引用/搜索/完整导出/原回执和当前隐藏
过滤均已验证。实际RED、准备错误和修复保留在下文与两端TDD，不伪造先失败证据。

**P01–P07均完成；整个M2、P08–P10/A仍未完成。** 契约contracts/timeline.md。
机器人沿用P03/P05/P06逻辑；时间线不引入新骰子/数值/资源执行；不接QQ或导入QQ历史。
记录/图片/编辑长期保留；Markdown/JSON记录导出不是P09数据库+素材一致部署备份。

下一可执行步骤：按AGENTS读交接、verification、work-packages、产品/路线/CONTEXT与
必读契约，独立核对最新HEAD（含完成文档）的push/PR Checks；再进入 **P08 团队笔记
及DM资料**，先固定共享/私密、当前权限下查询/搜索/附件/导出契约，实际RED→最小实现→
GREEN→完整验收。P01–P07不重复开发，保留P09/P10/A、容量/触屏/TLS及异机恢复门槛。

日志/运行时/私有证据均在仓库外D:/workspace/dnd/.tools，凭据仅内存。最终JAR
p07-acceptance.jar；start-p07-backend.ps1/run-p07-restart.ps1可复用，新seed须新前缀
（wx拒覆盖）。p07-final九组JSON、p07-restart-{seed,verify}.log与进程前后身份保留。
最后按CommandLine/data核对停止Java/production preview/PG，55432/8080/5173无监听，
p07-stopped-processes.json留证。不要复用旧PID；PG启动huge_pages=off且分步操作。
以下为本包开发/验收与此前交接历史，不作为当前下一步。


## 历史：P07 本地完整验收通过；代码双 CI 当时待核验（2026-10-21）

继续 feat/accounts-and-invitations / [草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，
不合并、不发布。开工HEAD 23dfa21的push39478445935/PR39478453446已独立核验六job/
完整日志通过；P01–P06完成且所有原回归保留。

契约 contracts/timeline.md。P07a文字/身份快照/不可变更正历史、b持久PNG/JPEG与
权限化引用、c共用RP/OOC/骰子/行动时间线、检索/分页/场次/完整Markdown及JSON导出、
d中文工作区/既有P03/P05/P06共用bot入口已实现。无独立骰子/数值/资源逻辑；不接QQ。

最终本地结果：p07-backend-final.log **84unit+108真实PG/HTTP/WS/迁移零失败/错误/
跳过**；p07-unit-no-db.log独立清除数据库环境后84unit；前端p07-frontend-full.log
211项，生产构建通过；p07-contract-final.log75操作，原65操作/全部旧schema逐项
结构比较相同。V12所有旧表原列/行/12checksum保持，当前到V15且同团引用FK通过。
p07-browser-full.log完整39生产Chrome流程通过1.8m，原35及60秒/条/零retry保留。
已复看深色timeline-dm.png。完整RED/GREEN/原始准备错误/失败修复见下文及两端TDD。

九组p07-final-{asset,token,map,p02,character,content,dice,combat,timeline}-restart-evidence.json
在实际Java+PG停止/同库同素材根启动后，新会话verify全PASS；p07-restart-{seed,verify}.log，
p07-process-before.json/p07-process-after.json显示不同Java/PG进程ID及真实CommandLine。
最终JAR .tools/p07-acceptance.jar；外置start-p07-backend.ps1/run-p07-restart.ps1可复用，
新seed须新前缀（wx禁止覆盖）。所有运行时/私有证据在D:/workspace/dnd/.tools，凭据仅内存。
最后按实际身份停止Java/production preview/PG，三端口无监听（p07-stopped-processes.json）。

**P07e/P07/M2尚未完成**：下一步提交/推送原分支、更新原草稿PR，核对精确代码SHA
的push/PR六job、标准Compose39流程及九组实际容器重启日志。全部通过后才关闭P07；
完成状态文档新HEAD也独立核验。P08–P10/A与容量/触屏/TLS/一致备份异机恢复继续保留。
以下为本包开发与此前交接历史，不作为最新运行状态。


## 历史：P07 开发/验收过程（2026-10-21）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。P01–P06保持完成。
开工独立 fetch：本地/origin/PR HEAD 23dfa2169baf65282d5687720993ff5340811b7a，clean；
认证 API 核对 push39478445935 / PR39478453446 六 job success。重新获取六份完整日志：
81unit+100IT、201前端/构建、65契约、两组35浏览器、八组实际容器重启均PASS；
PR open/draft/main。连接器临时传输失败后使用现有 Git credential 内存认证，不打印秘密。

P07契约 contracts/timeline.md，已固定a消息身份/历史、b图片/引用、c统一检索/场次/
导出、d中文入口与共用指令、e完整验收的有序切片。当前已实现后端和初版前端，
**P07e/P07/M2尚未完成，不以聚焦GREEN关闭。**

实际TDD：p07-messages-red.log 两条真实HTTP缺/messages 404失败→green两条通过。
p07-images-query-red.log 六条中新图片/私密引用/场次三条失败→green-2六条通过。
第一轮green仍有引用暗骰触发UnexpectedRollbackException：嵌套服务的404标记事务
rollback-only；改为当前可见性预检后才投影，第二轮完整六条通过（保留前次失败）。
p07-contract-red.log 缺10路由RED→75操作GREEN；仍须追加新schema/边界检查。
p07-frontend-red.log 七项缺入口失败，另有未消费deferred拒绝准备错误；实现后6过1败，
最后一条mock让新旧scope共用同一旧响应，修正mock单次响应后green-2七条通过。
构建最初误在仓库根运行（无package.json），第二轮发现测试helper使用不支持exact类型；
已修正，待最终构建。上述准备/类型问题不计行为RED。

所有日志/运行时/私有证据在仓库外 D:/workspace/dnd/.tools，凭据只注入内存。
PG已启动（huge_pages=off）；最新本地进程/端口应现场核对，不能沿用旧PID。
当前全量后端 p07-backend-full-initial.log 正在运行。下一步：完成前端入口/更广边界与
契约检查、旧库迁移保留测试、全量前后端/生产浏览器、九组实际Java/PG重启，再提交/
推送同分支并独立核验精确代码SHA的push/PR CI；完成后才更新关闭状态。
P08–P10/A及容量/触屏/TLS/一致备份异机恢复门槛全部保留。以下为此前历史状态。


## 历史：P06 完成时的 P07 交接

更新2026-10-21（Asia/Shanghai）。沿用feat/accounts-and-invitations/
[草稿PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
最新HEAD/工作区以Git实际状态为准；完成状态文档新HEAD也须独立核验push/PR。

P06代码 **d23c592c677fbaeeda2a3108c516410e411ef95a** 的
[push39477351204](https://github.com/xianchen233/dnd-roll/actions/runs/39477351204) 与
[PR39477361622](https://github.com/xianchen233/dnd-roll/actions/runs/39477361622)
六job均success；已逐份获取日志确认81unit+100真实IT、201前端/构建、65契约、
两组标准Compose35浏览器及八组实际db/backend容器重启全部PASS。PR open/draft/main。
本地独立81无库、最终81+100、201/构建、65、串行35流程、28旧表/10checksum及八组
实际Java/PG重启亦通过。完整RED/环境/失败修复见verification与两端TDD。

**P01–P06完成；整个M2、P07–P10/A仍未完成。** 契约contracts/combat.md，依据
docs/research/combat-5r.md。行动/目标/投骰关联、DM命中及显式生命/临时生命/资源
应用、当前版本关联更正、独立角色回合/跳过/切换/重排/移动放行与来源历史已交付。
探索自由，公开战斗移动需棋子+角色控制及当前回合/放行；全隐藏遭遇不泄露状态/
hint或锁探索。回合外投骰/行动不封禁；状态到期仍P03显式DM确认。没有自动命中、
抗性/重击/死亡/专注/效果与资源联动，不冒称完整规则自动执行。

下一可执行步骤：按AGENTS先读本页、verification/work-packages及必读产品/路线/
CONTEXT/契约，独立核对Git、远端及最新HEAD（含完成文档）的push/PR Checks。
然后进入 **P07 RP时间线与机器人**：先固定文字/图片/引用、实际操作者与发言身份
快照、编辑历史、搜索分页/导出和共用角色/投骰/行动入口的契约与有序切片，实际
RED→最小实现→GREEN→完整验收；引用/搜索/导出/附件必须沿用当前权限，不泄露
私密骰子、行动或DM资料。P01–P06不重做；长期记录不因备份轮换清理。保留P08–P10/A
和容量/触屏/TLS/一致备份异机恢复门槛。继续原分支/草稿PR #1，不合并、不发布。

运行时/凭据/私有证据均在外.tools，凭据仅内存。最终日志p06-backend-final-2.log、
p06-unit-delivered.log、p06-frontend-final.log、p06-build-final.log、p06-browser-delivered.log；
八组p06-delivered-*-restart-evidence.json及p06-delivered-restart-{seed,verify}.log；
前后进程身份p06-delivered-process-evidence.json，最终停止核对p06-stopped-processes.json。
JAR p06-acceptance.jar，脚本start-p06-backend.ps1/run-p06-delivered-restart.ps1可复用，
但新seed须换新证据前缀（wx拒覆盖）；不要与浏览器并跑共享账号的撤权seed。
Java/preview/PG已按实际CommandLine/data停止，三端口无监听；数据素材保留，不复用旧PID。
PG启动huge_pages=off并分步操作。以下为P06及更早开发历史，不作为当前下一步。

## 历史：P06 本地完整验收通过，代码 SHA 双 CI 待核验

更新2026-10-21（Asia/Shanghai）。原分支feat/accounts-and-invitations/草稿PR #1，
不合并、不发布。开工HEAD d4e1068的push39469353602/PR39469359789六job/完整日志
独立核验成功，详见verification。P01–P05保持完成，P06e/P06/M2尚未完成。

P06契约contracts/combat.md，来源docs/research/combat-5r.md。行动/目标/既有服务器
投骰关联、DM命中/显式生命临时生命资源应用、当前版本关联更正、独立角色回合与
DM跳过/切换/重排/移动放行/结束、中文工作区已实现。探索行为保留，回合外操作
不封禁，状态到期仍P03显式确认。私密日志裁剪及比较版本泄漏已实际RED修复。

本地最终81unit+100IT、独立无库81、201前端/构建、65契约、完整35生产浏览器及
八组实际Java/PG重启均通过。28旧表/10checksum保留；代码SHA的push/PR尚待提交核验，
不得提前关闭P06。最终日志p06-backend-final-2/ p06-unit-delivered/ p06-frontend-final/
p06-build-final/ p06-browser-delivered.log，重启证据p06-delivered-*-restart-evidence.json，
实际进程身份在p06-delivered-process-evidence.json。私有文件均在外.tools。
日志/准备失败/实际RED见verification及两端TDD。运行时/凭据只在外.tools，
不要打印credentials；停止进程前核验最新CommandLine/data，不复用旧PID。

下一执行步骤：审查提交推送原分支，更新原草稿
PR并核验该代码SHA push/PR六job/标准Compose35流程/八组重启全通过后再关闭P06。
完成状态文档的新HEAD也独立核验。后续P07–P10/A、容量/触屏/TLS及完整备份异机
恢复仍未完成，不静默丢弃。以下为P05及更早历史入口。

## 历史：P05 已完成；下一包 P06 尚未开工

更新：2026-10-21（Asia/Shanghai）。继续 `feat/accounts-and-invitations` /
[草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，未合并、未发布。
最新HEAD/工作区始终以Git实际状态为准；完成状态文档的新HEAD亦独立核验push/PR。

P05代码 **f416e627d99c7dbf332bbf174309cde7b1d7291e** 的
[push39468342643](https://github.com/xianchen233/dnd-roll/actions/runs/39468342643) 与
[PR39468347675](https://github.com/xianchen233/dnd-roll/actions/runs/39468347675)
均completed/success，frontend/backend/browser六job全绿，PR open/draft、base main。
已逐份读取六份日志：77unit+91真实IT、195前端/构建、58契约、两组标准Compose
31浏览器（push3.1m/PR2.4m）、七组实际db/backend容器重启seed/verify均PASS。
本地独立无数据库77unit、完整77+91、195前端、31浏览器及七组真实Java/PG重启也
通过；V8→V10保留23旧表原列/行/八checksum。实际RED/GREEN及失败/修复记录完整保留。

**P01–P05均完成；整个M2、P06–P10/A仍未完成。** P05契约
`contracts/dice-initiative.md`，规则依据 `docs/research/dice-initiative-5r.md`。
受限表达式、服务端逐骰/取舍/来源快照、暗骰和当前权限过滤、按钮/命令共用回执，
5R独立角色参战/显式怪物组/突袭劣势/候选/理由化次序确认与DM历史已交付。
多个地图棋子不复制先攻；只辅助投骰与确认顺序，不执行回合/伤害/资源。
NPC须有独立角色卡；固定先攻/英雄激励重骰/警戒交换等未自动实现，保留手动来源处理。

下一可执行步骤：按AGENTS先读本页、verification/work-packages及产品/路线/CONTEXT/
必读契约，独立核对Git、远端及最新HEAD（含本次完成文档）的push/PR Checks。
然后进入 **P06 手动战斗与更正**：先核对5R来源，固定行动/目标/投骰关联、DM命中
裁定/手动应用伤害、角色级回合/回合外操作/来源日志/关联更正的契约与有序切片；
实际RED→最小实现→GREEN→完整验收。保留P01–P05回归，不把探索移动当战斗限制，
不因P05完成标M2完成。沿用原分支/草稿PR #1，不合并、不发布。

运行时/凭据/私有证据在仓库外 `.tools`，凭据只注入内存。最终日志
`p05-backend-delivered.log`、`p05-unit-no-db.log`、`p05-frontend-delivered.log`、
`p05-build-delivered.log`、`p05-browser-full.log`及`p05-browser-delivered.log`。
重启七组 `p05-final-*-restart-evidence.json`、`p05-restart-{seed,verify}.log`，
前后进程身份在p05-process-evidence.json。JAR为.tools/p05-acceptance.jar，外置
start-p05-backend.ps1/run-p05-restart.ps1可复用；新seed用全新前缀，不覆盖已有证据。
Java/production preview/PG最后按实际CommandLine/data核对后停止，55432/8080/5173
无监听，数据库/私有素材保留；不要复用旧PID。PG启动huge_pages=off且分步操作。

以下为P05开发/验收时的历史入口，不作为当前下一步。

## 历史过程：P05 已实现；最终整体验收/精确 SHA 双 CI 进行中

更新：2026-10-21（Asia/Shanghai）。沿用 `feat/accounts-and-invitations` /
[草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，不合并、不发布。
开工独立fetch及认证API核对HEAD **963ddd74750644b33fdfddd661e24d7977e806c5**：
本地/origin/PR一致、clean；push39461677478/PR39461684085六job均completed/success，
PR open/draft、base main。P01–P04保持完成，不重复开发。

P05契约 `contracts/dice-initiative.md`，规则来源 `docs/research/dice-initiative-5r.md`。
已实现服务端SecureRandom、受限表达式/逐骰取舍与来源快照、暗骰和当前权限过滤、
按钮/命令共用持久回执；5R独立角色参战、显式怪物分组、突袭劣势、候选/手动确认
次序与DM历史，V9/V10保留旧资料。不会执行回合/伤害/资源（P06）。

实际TDD见两端TDD及verification：骰子4断言RED→GREEN，真实HTTP缺路由RED→
GREEN，先攻2缺路由RED→GREEN；前端7入口RED→GREEN，真实offline重连暴露旧
公开记录残留，新增组件断言RED→立即清空/重新读GREEN；历史详情追加RED→GREEN。
独立无库77unit、195前端/构建、58契约（旧50操作/全部schema deepEqual）、完整31
浏览器（原27保留）和历史UI追加后4条再次复验均通过。首次完整77unit+88IT通过；
追加3IT聚焦8通过。一次全量因同时运行验收Java而耗尽本机PG连接，30项上下文无法
启动；停止验收Java后串行最终 **77unit+91IT全部通过**，未弱化断言或略过检查。

七组seed→实际Java/PG停止/同库同素材根启动→新会话verify均PASS；证据
`.tools/p05-final-*-restart-evidence.json`、`p05-restart-{seed,verify}.log`与
`p05-process-evidence.json`（真实CommandLine、前后不同ID）。最终JAR在.tools/
p05-acceptance.jar，启动脚本start-p05-backend.ps1。任何进程操作前重新核对身份。
credentials只在内存注入，不打印；所有日志/运行时/私有证据在仓库外.tools。

下一可执行步骤：检查新代码和实际状态记录后提交推送
原分支；独立核验代码SHA的push/PR六job/标准Compose31浏览器/七组容器重启全PASS，
然后才关闭P05并更新本页/verification/work-packages/implementation-plan及原草稿PR。
**P05e、P05与M2尚未完成**；后续P06–P10/A不丢弃。完成文档的新HEAD也独立核验。

以下为P04完成交接时的历史入口，不作为当前下一步。

## 历史入口：P04 已完成；下一包 P05 尚未开工

更新：2026-10-21（Asia/Shanghai）。继续 `feat/accounts-and-invitations` /
[草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，未合并、未发布。
最新HEAD与工作区以Git实际状态为准；本次完成文档的push/PR Checks也独立核验。

P04代码 **a7ef38830b34a5e55f51294cf289a3e1b216fbda** 的
[push39460581794](https://github.com/xianchen233/dnd-roll/actions/runs/39460581794) 与
[PR39460588799](https://github.com/xianchen233/dnd-roll/actions/runs/39460588799)
均completed/success，六job全绿；已逐份读取日志确认70unit+82真实IT、186前端/构建、
50契约、两组标准Compose27浏览器和六组实际db/backend重启全PASS。
本地独立无数据库70unit、最终新JAR27流程、六组实际Java/PG重启、V7→V8保留全部
20旧表原列/行和七checksum也通过。P01/P02/P03全部原回归保留。

**P01–P04均完成；整个M2及P05–P10/A仍未完成。**
P04契约 `contracts/content.md`，5R/许可依据 `docs/research/content-5r.md`。
十条SRD5.2.1精选、团内不可变自定义条目、来源/版本/准入筛选、DM审核/自由选用、
服务器预览和来源化填卡已交付；已有卡和历史不被目录更新或切回审核改写。
填卡不赋予熟练/精通、不应用属性/资源/效果或执行投骰；完整资料库和自动化不冒称完成。

下一可执行步骤：按AGENTS先读本页、verification、work-packages、产品/路线/CONTEXT
与必读契约，独立核对Git、远端和最新HEAD（含本次文档提交）的push/PR Checks。
然后进入 **P05 掷骰与5R先攻**：使用dnd-rule-lookup核对5R服务端随机结果/逐骰与修正
快照、暗骰权限、按钮/命令共用逻辑、参战/分组/突袭劣势/平手/隐藏先攻来源；
先固定契约与有序切片，实际RED→最小实现→GREEN→完整验收，不重做P01–P04。
战斗手动结算属P06；RP/资料/备份/触屏/容量/TLS仍按后续包与发布门槛推进。

最终日志/凭据/运行时在仓库外 `D:/workspace/dnd/.tools`，凭据只注入内存，不打印。
本轮日志 `p04-backend-final.log`、`p04-unit-no-db-final.log`、`p04-frontend-final-3.log`、
`p04-build-final-3.log`、`p04-browser-full-final.log`；最终六组证据
`p04-delivered-{asset,token,map,p02,character,content}-restart-evidence.json` 与
`p04-delivered-restart-{seed,verify}.log`，真实进程前后ID在p04-process-evidence.json。
验收JAR复制为.tools/p04-acceptance.jar，运行脚本start-p04-backend.ps1、
run-p02d-browser.ps1、run-p04-restart.ps1；seed文件采用wx保护，需新前缀，不能覆盖旧证据。
Java/production preview/PG最后均按实际CommandLine/data核对后停止；三端口无监听，
数据库/私有素材保留，不复用旧PID。Windows的pg_ctl带huge_pages=off且分步启动。

以下为P03交接时的历史入口，不作为当前下一步。

## 历史入口：P03 已完成；下一包 P04 尚未开工

交接更新：2026-10-21（Asia/Shanghai）；P03代码验收于2026-10-20完成。
继续 `feat/accounts-and-invitations` / [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，
未合并、未发布。最新HEAD/工作区始终以Git实际状态为准；完成状态文档提交也独立核对Checks。

P03完成代码 **f66c4e731f174c81be0ee7ab9724db30c0827446** 的
[push39452554961](https://github.com/xianchen233/dnd-roll/actions/runs/39452554961) 和
[PR39452563178](https://github.com/xianchen233/dnd-roll/actions/runs/39452563178) 六个
frontend/backend/browser job均completed/success，PR open/draft、base main。已逐份读取
最新六个job日志：**65unit+75真实PG/HTTP/WS/迁移、177前端/构建、45契约、两组标准
Compose23真实浏览器（push2.2m/PR2.5m）及五组实际db/backend容器重启全PASS**。

本地独立无数据库65unit、最终新JAR23流程、重新seed后的五组实际Java+PG重启也通过；
16张旧表全部原列/原行/六个checksum自然升级一致。原61unit/61IT/166前端/31契约/
17浏览器及四组重启全部保留。完整实际RED/GREEN、准备失败与修复见verification及两端TDD。

**P01、P02a–e/整个P02、P03a–e/整个P03完成；整个M2、P04–P10/A仍未完成。**
P03契约 `contracts/characters.md`，5R依据 `docs/research/character-sheet-5r.md`，开放
传输示例 `contracts/character.example.json`。角色身份/六组字段、来源计算/DM覆盖、
独立资源、编辑历史/实际操作者、状态到期由DM确认、复制/目标审核导入导出及多棋子
引用已交付；新建/复制/导入旧回执还会复核结果角色当前可见性，隐藏后不返回其ID。
头像支持短文本/emoji；生命上限、升级选择、特殊机制和资源上限仍为有来源的手动字段。
内容库/准入切换/便捷填卡、服务端投骰、战斗分别属于P04/P05/P06，未冒称完成。

下一可执行步骤：按AGENTS先读本页、verification、work-packages、产品/路线/CONTEXT
及必读契约，独立核对Git、远端和最新HEAD（含本次完成文档）的push/PR Checks。
然后进入 **P04 规则检索与填卡**：先沿用dnd-rule-lookup核对5R及开放来源授权，
固定名称/来源/规则版本/准入切换/结构化填卡契约和有序切片，再实际RED→最小实现→
GREEN→完整验收。不要重做P01/P02/P03，不把检索覆盖当自动执行覆盖，不把商业索引
正文或私有素材打包。触屏、容量、TLS、完整一致备份/异机恢复继续按P09/P10/发布门槛。

运行时、凭据与证据全在仓库外 `D:/workspace/dnd/.tools`，凭据只注入内存、不打印。
本轮最后日志 `p03-backend-final.log`、`p03-unit-no-db-final.log`、`p03-frontend-final.log`、
`p03-build-final-production.log`、`p03-browser-final-after-receipts.log`；五组最终证据为
`p03-final-{asset,token,map,p02,character}-restart-evidence.json`，进程前后ID在
`p03-final-process-evidence.json`。实际服务脚本仍可复用start-p02e-backend.ps1（启动当前
JAR）、run-p02d-browser.ps1；run-p03-restart-final.ps1注入测试凭据调用五组seed/verify。

Java/production preview/PostgreSQL已再次核对实际CommandLine/data后全部停止，
55432/8080/5173无监听，数据库、私有素材和截图保留。不要复用旧PID。Windows下pg_ctl
启动采用独立命令，避免包在全流重定向的旧restart wrapper中等待继承输出管道；首次
wrapper卡住及分步重启证据已记verification。本次末尾只更新完成/交接文档，交付前仍
独立核验该文档新SHA的双CI，不能拿f66c4e7绿灯代替新HEAD。

以下为历史交接记录，保留当时状态，不作为当前下一步。

更新：2026-10-20（Asia/Shanghai）。最新分支、HEAD 和工作区以 `git status --short --branch` / `git log -5 --oneline` 为准。当前沿用 `feat/accounts-and-invitations` 和 [草稿 PR #1](https://github.com/xianchen233/dnd-roll/pull/1)，未合并、未发布。

## 历史入口：P02e / P02 完成后的 P03 交接

2026-10-20 用户明确要求：**下一包交给下一会话，本会话不启动 P03**。
交接前独立核验 `863a68abb4addf0dcfc7664a0c04cc204c97c31f` 工作区干净、与 origin
一致；其 [push39437833963](https://github.com/xianchen233/dnd-roll/actions/runs/39437833963)
与 [PR39437844561](https://github.com/xianchen233/dnd-roll/actions/runs/39437844561) 均
completed/success，六个 job 全绿，PR 仍 open/draft。本次仅补交接文档，不改行为、
不重复本地测试；后续交接文档新 HEAD 的 Checks 留待下一会话开工独立核验，
不能把863a68a绿灯当作新SHA结果。55432/8080/5173再次确认无监听。

最终验收提交 `853944d36d59d31b833c509f30db84b356449a5a` 的
[push39436631381](https://github.com/xianchen233/dnd-roll/actions/runs/39436631381) 与
[PR39436640191](https://github.com/xianchen233/dnd-roll/actions/runs/39436640191) 均
completed/success，frontend/backend/browser 六 job 全绿，PR #1 open/draft、base main。
最新日志确认 **61unit + 61真实PG/HTTP/WS/迁移、166前端测试/构建、31契约、17真实
浏览器**，两组标准Compose均通过17条及四组实际db/backend重启seed/verify。
本地完整生产preview17条46.4秒、Java+PG实际重启四组也通过。原六个E2E文件/13条
及全部旧测试保留，生产源码/迁移/OpenAPI未改；新验收不伪造RED。具体证据、初次
测试准备失败和三项证据加强见verification及两端TDD。

P02e 契约 `contracts/p02-acceptance.md`；新增 `scripts/check-p02-restart.mjs` 和
`frontend/e2e/acceptance.spec.ts`。实际双场景fog/校准/单位独立、真实offline加原生
socket断开/新CONNECTED、撤权先提交再上线、fog提交后丢响应/原body重试/失败读恢复
均通过。已更新CHANGELOG Unreleased、部署及验收。**P01/P02a–e与整个P02完成**；
角色卡、整个M2、P04–P10/A、容量/触屏/TLS及完整备份/异机恢复仍未完成。

下一可执行步骤：先读AGENTS/本页/verification/work-packages及必读契约，独立核对
Git和最新HEAD Checks（含本次完成状态文档提交），再开始 **P03 完整半自动角色卡**。
先核对5R规则来源（使用工作区dnd-rule-lookup），固定一人多角、独立资源、基础计算
及来源、编辑历史、DM覆盖、状态到期提醒、复制与结构化导入导出的契约和最小切片；
按契约→实际行为RED→最小实现→完整验收，不把既有棋子配置当角色卡，不重复P01/P02。
继续现有分支/草稿PR #1，不合并/发布，不因P02完成标整个M2完成。

工具/日志/证据在外层 `.tools` 的 `p02e-*`；`run-p02e-restart.ps1` 注入已有测试
凭据调用原三组加新组合脚本，`start-p02e-backend.ps1`只启动当前打包Java，
`update-p02e-pr.mjs`沿用GCM维护原PR。凭据只在内存，不打印/不提交。验收
Java/production preview/PG 已逐一核实CommandLine/data后停止，55432/8080/5173
无监听，数据库/私有素材/截图及证据保留；不能复用旧PID。

## P02e 开工及首次验收历史

2026-10-20 从 `3c834b4` 干净工作区开工，fetch 后与 origin 一致；该 SHA 的
push39289292710 / PR39289302976 六个 job 全部 success，PR #1 仍 open/draft。
先写 `contracts/p02-acceptance.md`，只组合既有功能，保留全部旧测试。
本轮 61 无数据库 unit、166 前端测试/生产构建、31 契约以及最终后端
61 unit + 61 真实 PG/HTTP/WS/迁移已通过；新增真实 HTTP 跨场景组合首次即通过，
不伪造 RED。协调者完整真实 Chrome 生产 preview **17 passed（40.7s）**，
原六个 E2E 文件及13流程未改；真实双端离线/撤权/提交后响应丢失精确重试通过。
四组（原素材/棋子/地图 + 新两场景组合）实际 Java+PG 停止/同库同素材根重启，
seed/verify 全 PASS；详情、首两轮测试准备失败及边界见 verification。
首次验收提交 `5ac7491` 的 push39435183782 / PR39435191314 六 job 全 success，
标准Compose17流程及四组实际容器重启全 PASS。其后只读审查加强了三项证据：
移除实际204/名单无成员后才上线；捕获校准POST并读回offsetX=1；真实浏览器offline
加原生socket关闭，要求实际close及新socket的服务端CONNECTED。新四条18.4秒通过，
无生产行为变化；完整17条再次 **46.4秒通过**，加强提交的远端CI仍独立核实。
**P02e/P02仍未完成**。
下一步提交推送加强版本并核实该 SHA 的 push/PR 全量CI，全部通过后才更新完成状态。
外置 `.tools/run-p02e-restart.ps1` 注入已有测试凭据调用四组脚本，证据/日志以
`p02e-`前缀在同目录。新仓库脚本 `scripts/check-p02-restart.mjs`，验收契约
`contracts/p02-acceptance.md`；无新 API/迁移/生产行为。验收 Java/production preview/PG
已逐一核实实际 CommandLine/data 目录后停止，55432/8080/5173 无监听，数据与素材保留。

## P02d 已完成的历史交接

2026-10-19 本次从 `0bacdab` 开工，干净且与 origin 一致；其 push39143471892 /
PR39143474706 均 frontend/backend/browser success 后才实施 P02d。先写
`contracts/map-visibility.md`，实际缺路由/缓存头 RED，额外注入不安全全图输出使
真实 HTTP 秘密像素断言失败；恢复后实现服务器安全 PNG、手动 fog、V6 和原生 Pixi。
地图默认全遮盖；换图、校准或拆离原子清 fog，单位隐藏独立。旧公开像素无法撤回。
派生 PNG 重新编码、显式内存流、无磁盘/HTTP 派生缓存，URL 绑定当前场景和版本。

本地最终 **61 unit + 60 真实 PG/HTTP/WS/迁移、166 前端测试/构建、31 HTTP 契约、
13 真实 Chrome 生产构建（30.3s）**通过。原全部测试保留，旧 7 E2E 未改；14 张 V5 旧表
全部列/行与五个 checksum 自然升级一致。Java+PG 实际停止/同库同素材根启动，
新会话地图/素材/棋子三个 verify 全通过。详见 verification.md 与两端 TDD.md。

代码 `7f539cf7640f54f07087bffbd1e9bc8b012a79ed` 的
[push39287462278](https://github.com/xianchen233/dnd-roll/actions/runs/39287462278) 与
[PR39287472000](https://github.com/xianchen233/dnd-roll/actions/runs/39287472000) 均
completed/success，frontend/backend/browser 全绿。两组标准 Compose 分别13 passed
（push1.5m、PR1.1m）；素材/棋子/地图三个seed、实际db/backend重启和三个verify均PASS。
地图verify重新登录校验fog/version/回执及掩盖PNG哈希，旧URL、历史揭露、撤权和退出
不能取得秘密像素。本段是该代码SHA证据；接续时仍独立核对当前HEAD，含文档提交。

d593092 的刷新吞手势/草稿编辑问题已有实际RED/GREEN；bbdd54b组合流程超时已拆独立场景。88afda9两组
前后端通过、browser均12过1失败：push旧素材切图没有激活POST，PR平移/缩放断言
全部通过但最后截图耗尽总时限；两份trace已取得。场景preflight被背景读取静默吞掉
有4项实际RED，修复保留原版本、授权拒绝及409。静态Pixi改按需绘制，安全清图同步
重绘，有2项实际RED/GREEN；测距/视图用例仅打开所需玩家UI，其他仍双端。166项/
构建及完整13条本地复验通过；旧5个E2E文件不改，60秒/条、零retry。详见verification和
frontend/TDD。下一可执行步骤：核对Git与当前HEAD Checks后，按work-packages的P02e
制定独立整体验收组合，实际验证跨场景fog保存、校准重置、双端撤权/重连/失败恢复及
进程重启；保留全部既有测试、独立回执与单位隐藏边界，不重复开发P02a/b/c/d。
**P02e、整个 P02 及后续工作包仍未完成**；不合并、不发布。

本次外置工具：`P02dSnapshot.java`、`compare-p02d-snapshots.mjs`、
`p02d-before-v6.json` / `p02d-after-v6.json`、`run-p02d-browser.ps1`；
三个新重启证据为 `p02d-map-restart-evidence.json`、`p02d-asset-restart-evidence.json`、
`p02d-token-restart-evidence.json`。复用此前账号、DB、JDK、Maven、私有素材根；
仓库新增 `scripts/check-map-restart.mjs`。本次验收Java/Vite/PostgreSQL已按实际命令行
和data目录核对后停止，55432/8080/5173无监听，数据库/素材保留；不能复用旧PID。

## 原 P02c 交接基线（历史记录）

2026-10-19交接前再次核验：`a3e7f7ba9c2b66349e38df8f2c84ae087c9b60c0`工作区干净、与origin一致；[push39142300219](https://github.com/xianchen233/dnd-roll/actions/runs/39142300219)与[PR39142302856](https://github.com/xianchen233/dnd-roll/actions/runs/39142302856)均completed/success，frontend/backend/browser全绿。这是P02c最终交接HEAD的独立证据，不只引用代码5160c0f。以下本次仅补交接文档；其新HEAD Checks由下一会话开工时独立核对，不能将a3e7f7b绿灯当作新文档SHA结果。

下一会话按AGENTS先读本文、verification、work-packages，再读product-design、implementation-plan、CONTEXT和contracts/README及scenes/assets/tokens契约。不重复P01/P02a/b/c；保留56后端unit、50真实PG/HTTP/WS、112前端测试、27契约操作和7真实浏览器全部既有覆盖。

P02d先固定玩家安全图像/手动揭露的契约，再实际运行秘密像素泄漏的失败测试：玩家不能取得DM原图或遮盖区域原始像素，已公开内容再次遮盖不能声称从玩家缓存中撤回。覆盖直接URL、猜ID、跨团、撤权、历史回执、重连/切图及派生图缓存失效；客户端遮罩不能作为秘密边界，单位隐藏仍独立。随后实现服务端安全图像交付、DM手动揭露/遮盖及原生Pixi平移/缩放/网格校准/吸附/测距，按工作包先协调契约再分工，IT数据库串行使用。

继续维护CHANGELOG Unreleased和实际RED/GREEN/验收/交接；沿用当前分支与草稿PR #1，不合并、不发布。P02d完成后才进入P02e整体验收，不能提前标整个P02完成。本机Java/Vite/PG已停止，凭据与运行时均在外层.tools；重启前核对端口和进程身份，不依赖旧代理或本聊天历史。

## P02a/b/c 历史状态与当时下一步

- **P01 已完成，禁止重复开发。** P02a 开工起点 `d9a0393` 工作区干净、与 origin 一致；其 [push CI](https://github.com/xianchen233/dnd-roll/actions/runs/39127654306) 与 [PR CI](https://github.com/xianchen233/dnd-roll/actions/runs/39127657531) 均 completed/success，核对后才开工 P02a。
- **P02a 已完成并通过本地及远端完整 CI。** 代码提交 `8836abbcca9a28a2e130770cb9839e0136f51fcf` 的 [push CI](https://github.com/xianchen233/dnd-roll/actions/runs/39129943816) 与 [PR CI](https://github.com/xianchen233/dnd-roll/actions/runs/39129946834) 均 frontend/backend/browser success，含标准 Compose 部署和全部三条真实浏览器流程。 契约先写 `contracts/scenes.md`，实际 RED/GREEN 见两端 TDD.md 与 verification.md。37 后端 unit、28 真实 PG/HTTP/WS、59 前端测试、构建、19 HTTP 操作契约、3 条真实 Chrome 流程、V2→V3 旧数据比较及真实 Java 进程重启全部通过。
- DM 创建多个空场景（创建不切图）、明确切换全团当前场景；玩家不能列出未激活场景。旧场景位置/版本/控制权保存；历史激活回执只返回原结果，不再次切图。服务器仅接受当前场景的移动/移动重放，前端隔离旧场景在途响应和重试。
- V3 保留 V1/V2，回填当前场景指针并通过复合 FK 限制归属；P01/demo 在同一事务设置指针，demo 重启不重置选择。scene-command 回执独立命名空间，全局 actor→campaign 锁；与 P01 共用 campaignVersion。
- **P02b 已完成本地与远端完整 CI 验收。** 代码 `1bfac83d55eb3ccab18778a30e5f5ba54f4e4289` 的 [push run39135326549](https://github.com/xianchen233/dnd-roll/actions/runs/39135326549) 和 [PR run39135329930](https://github.com/xianchen233/dnd-roll/actions/runs/39135329930) 均 frontend/backend/browser success，含四条真实浏览器和标准Compose的数据库/后端重启。开工起点 `be96f2c` clean、与origin一致，旧SHA CI先全绿才开工。先写 `contracts/assets.md` 和实际RED，再实现DM私有PNG/JPEG素材与地图引用/校准、V4同团FK、独立素材回执、Compose私有卷。共享逻辑网格和玩家快照不含原图或地图素材字段，玩家图片揭露仍为P02d。
- 本地证据：最终52 unit + 41真实PG/HTTP/WS全量、最终90前端测试与构建、24 HTTP契约、4条真实Chrome全部通过；V3旧库自然升级比较、Java与PostgreSQL实际停止/同库同素材根重启的SHA-256/引用/版本/回执检查通过。初次新增E2E精确label定位失败已修复，原三条流程未放宽；详情见 verification.md 和两端 TDD.md。
- 最终审查修复：同步确认由POST确认后启动且最新有效的权威读完成，旧成功/失败不能覆盖；测试动作等实际可用，不放宽业务断言/timeout、不加retry。提交确认丢失时先取得新campaign锁等待旧事务结束，再核对asset/storageKey；确认未提交才清文件，已提交或无法核对则保留，通过原requestId回执恢复。模拟故障的范围及实际RED/GREEN见两端TDD和verification。
- **P02c 已完成本地与远端完整验收。** 代码`5160c0fdbb98a9bb189193f1ec8735d004ffb978`的[push39141337705](https://github.com/xianchen233/dnd-roll/actions/runs/39141337705)和[PR39141340870](https://github.com/xianchen233/dnd-roll/actions/runs/39141340870)均frontend/backend/browser success，含标准Compose七流程和素材/棋子实际数据库/后端重启。开工2967c37干净且与origin一致，其push/PR全部success后才开工。先写`contracts/tokens.md`和实际RED，再实现DM创建/完整编辑名称、位置、隐藏与多控制者，移动协议不变。56unit+50真实PG/HTTP/WS、112前端测试/构建、27HTTP契约、7真实Chrome通过；13表全部旧列/行及V1–V4 checksum自然升级一致，Java+PG本机真实重启亦通过。首次长流程CI超时和第二次checkbox交互竞争均已实际重现/修复，最终新SHA关闭，见verification及两端TDD。
- P02c独立token回执使用actor→campaign锁，DM/目标归属先于回放；历史配置只返回原确认、不恢复控制。V5回填同团键并限制成员FK；移除成员推进受影响token/scene版本，重加入不恢复控制。前端撤控/隐藏清除旧选择与在途移动，作者草稿保留原版本，任一并发拒绝清私有状态，最新post-ack读完成确认。
- 下一可执行步骤：先核对Git与PR #1当前HEAD及Checks（交接文档提交也独立核对），再进入 **P02d 地图交互及手动遮盖**，先契约及秘密像素不可经URL/缓存取得的实际失败测试。P02c不用重复开发；P02d/e、整个P02及后续包仍未完成。

## 用户授权与开发约定

- 仓库 `https://github.com/xianchen233/dnd-roll.git`，本地 `D:/workspace/dnd/dnd-roll`。已授权持续编码、按工作包分工、TDD、阶段验收和统一 Git 提交推送；不合并或发布草稿 PR。
- 开始读 AGENTS.md、本文、verification.md、work-packages.md，改行为前读 product-design.md、implementation-plan.md、CONTEXT.md、contracts/README.md 和对应行为契约。未完成工作不得静默删除。
- “契约 → 聚焦失败测试并实际运行 → 最小实现 → 集成验收”。只把实际断言失败算 RED；环境/编译失败单列。保留 M1/P01 全部功能与测试。
- 本轮按 work-packages 显式分工完成 backend/**、frontend/**、openapi/checker；协调者拥有其他契约、docs/deploy、实际验收和 Git。代理可用性不是接续前提，禁止并发改同一 IT 数据库。
- 服务器校验身份、成员、控制权、对象版本和幂等；秘密不进入玩家快照、错误或事件。所有新变更在根 CHANGELOG 的 Unreleased 同步记录，不伪造发布。
- 规则基线 5R；P02a 只有场景与探索坐标，无 D&D 机制实现。本轮无需规则检索；开始规则相关行为时使用 dnd-rule-lookup。

## 真实验收与边界

详情见 `docs/verification.md`：M1/P01 两条真实浏览器原样保留，新增 P02a 双端移动、创建不激活、玩家 403、真实 STOMP 切图、返回完整快照/版本保持。已人工查看 DM/player 截图，画布完整、DM 管理可用、玩家无隐藏单位或管理区。

V2 浏览器库升级前后逐项比较 5 个战役/场景、2 个棋子、controls 与旧回执数量一致。独立自动迁移 IT 另外逐行比较全部原表内容并验证跨团指针 FK。真实 Java 进程停止/同库重启后，当前指针、创建/切换/移动回执、旧场景位置与版本保持；demo 不重置选择，历史切换重放不重新切图。

本机无Docker；P02c的5160c0f两次远端CI已实际通过容器构建、私有素材卷、代理、七条浏览器及数据库/后端重启，后续交接提交仍独立核对Checks。公网TLS、触屏、容量、完整素材备份和异机恢复未验收；遮盖P02d、整体验收P02e及P03–P10/A继续见work-packages.md。约572kB主chunk提示保留为后续优化，不放宽检查。

## GitHub 与 CI

- Git Credential Manager 已授权，CLI fetch/push 可用，不要再次要求用户登录。PR #1 仍 open draft、base main；继续更新这条 PR，不创建重复 P01 PR。
- P02b续接时GitHub连接器可读PR，但更新PR返回integration 403，部分日志读取出现transport error；已沿用现有GCM凭据更新同一PR，无需用户再登录。只读脚本 `.tools/check-p02-ci.mjs --jobs` 按当前HEAD核验Actions，`.tools/read-p02-job.mjs <jobId>`只输出筛选证据，`.tools/update-p02b-pr.mjs`读取外置body更新PR；凭据仅在内存，禁止打印或落盘。
- CI 包含 frontend、真实 PG backend、标准 Compose browser；每次按目标 SHA 核验。旧 P01 全绿不代替新 P02a 检查。

## 本机辅助环境（均在应用仓库外，不提交）

- 外层工具目录 `D:/workspace/dnd/.tools`：JDK `java/jdk-25.0.4.1+1`、Maven `apache-maven-3.9.16/bin/mvn.cmd`、缓存 `m2`。
- Java 命令设置 JAVA_HOME；TEMP/TMP 及 `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp` 避免系统临时目录权限问题。Maven `-D` 参数用完整引号防止 PowerShell 拆分。
- PG data `pg-data`、原生 bin `postgres/bin`，回环 127.0.0.1:55432、huge_pages=off。`test-database.json` 与 `browser-database.json` 分别为 IT/browser 凭据，只注入环境，禁止打印。测试专用 Hikari pool 3，生产配置未变。
- `browser-credentials.json` 为测试账号；Chrome `C:/Program Files/Google/Chrome/Application/chrome.exe`。E2E 需四个 E2E_* 账号环境变量，无默认密码。
- `start-p01-browser.ps1` 可启动当前打包 jar/Vite；`smoke-p02a-restart.ps1` 为本轮实际进程重启验收，`P02Snapshot.java`/`p02a-before-v3.json`/`p02a-after-v3.json` 为迁移比较。均在 `.tools`，不提交数据/运行时。
- P02b 素材根 `.tools/browser-assets`，实际双进程重启脚本 `.tools/restart-p02b-services.ps1`；配合仓库 `scripts/check-asset-restart.mjs seed|verify <证据文件>`，本轮证据 `.tools/p02b-restart-evidence.json`。V3→V4快照为 `p02b-before-v4.json` / `p02b-after-v4.json`；Java单文件快照程序的JDBC driver取 `.tools/m2/org/postgresql/postgresql/**/postgresql-*.jar`，不要误用外层 `postgres.jar` 归档。PG通过 `pg_ctl start -D ... -l ... -o '-h 127.0.0.1 -p 55432 -c huge_pages=off' -w` 启动；直接管理员启动postgres会被拒绝。
- P02c沿用同一浏览器库/素材根和双进程重启脚本，新增仓库`check-token-restart.mjs`；两份新证据为`.tools/p02c-token-restart-evidence.json`与`p02c-asset-restart-evidence.json`。`P02cSnapshot.java`、`compare-p02c-snapshots.mjs`和`p02c-before-v5.json`/`p02c-after-v5.json`为13表全部原列/行及迁移checksum比较。外置`update-p02c-pr.mjs`更新同一草稿，body在`p02c-pr-body.md`；凭据仍只在内存。全量日志为`p02c-full-verify.log`、`p02c-frontend-final.log`、`p02c-browser.log`，无运行时或数据纳入Git。
- PID 文件只供定位，停止前必须核对实际进程 CommandLine，不能假设旧 PID 仍属于本任务。本轮 Java/Vite/PG 已按 CommandLine/数据目录核对后停止，测试数据保留；续接仍须核实实际进程和端口。
