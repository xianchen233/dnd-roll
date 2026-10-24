# 实施路线与分工

## A03c 边界审计补齐（2026-10-24）

目标地图缺图漏检、资料节点重排误阻断经真实RED修复；缺失/变化实况链接、图片完整性、
资料/策略/manifest阻断与无部分写入已追加回归。完整126unit+163真实IT、248前端/构建、
111契约及独立18081新库上的真实Java/PG重启通过。见[a03c-boundary-tdd.md](a03c-boundary-tdd.md)，
最新HEAD双CI须按外.tools/a03c-boundary-check-ci.mjs独立核验，不能复用开工8ddbb0d绿灯。
A03c/A03不提前关闭；下一步继续边界审计，再A03d手动组合、A03e中文控件/最终联验。
A04–A06/M3、云端/真实手机仍待办；沿用原分支/草稿PR #1，不合并、不发布。
本轮测试不使用8080/体验数据；只读发现原dnd-roll-local开工前已退出，未启停或重建。

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

## P09 功能及本地/容器验收通过；云端联验后置（2026-10-22）

P01–P08保持完成。契约contracts/backups.md及操作手册deploy/backups.md：独立部署
授权、每日/手动/下载/失败、停写归档、严格完整性与隔离离线恢复、迁移持续停写和
中文管理已实现。225前端/87契约/44生产浏览器、独立目标302文件与十组业务/待确认
结果恢复通过；最终102+118串行和独立无库102通过。代码f6c9130的push39633129367/
PR39633137675六份完整日志重新核对通过：两组102+118、225/构建、87契约、44浏览器、
十组实际容器重启与独立新项目/新卷离线恢复、待确认行动及十组新会话核验全PASS。
完成文档HEAD另独立核验；下一可执行包P10完整测试团与功能验收。
用户要求整体功能完成后，再统一准备云服务器环境、部署和物理异机恢复联验。
该最终门槛后置且未通过；后续可进入P10功能验收，不冒称P10/A/M2或发布完成。
下方分工/阶段表保留历史语境，当前下一步以continuation为准。

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


状态：2026-10-18，用户已授权进入代码实现，并指定 `https://github.com/xianchen233/dnd-roll.git`。按 TDD 执行用户所说的“ttd”。产品目标见 [设计方案](product-design.md)，术语见 [领域词汇](../CONTEXT.md)。

续接入口：[当前交接状态](continuation.md) → [实际验收结果](verification.md) → [完整工作包与依赖](work-packages.md)。用户特别要求：任务超出聊天上下文后，必须能凭仓库文件继续推进。

## 本轮当前状态（2026-10-21）

**P01–P06均已完成；下一包P07未开工，M2及P07–P10/A仍未完成。**
P06代码d23c592的push39477351204/PR39477361622六job/完整日志独立核验全绿：
81unit+100真实IT、201前端/构建、65契约、标准Compose35浏览器及八组真实容器重启；
本地无库81、完整同等验收、28旧表全部原列/行/10checksum及八组Java/PG重启均通过。
契约contracts/combat.md、依据docs/research/combat-5r.md，实际失败/修复见verification。
完成文档新HEAD另核对，不合并、不发布；下面保留P06开发时的历史状态。
P06契约`contracts/combat.md`，来源`docs/research/combat-5r.md`。最终81unit+100IT、
独立无库81、201前端/构建、65契约、完整35真实浏览器、28旧表/10checksum及八组
实际Java/PG重启已通过；精确代码SHA push及PR CI仍待完成，不提前关闭。实际RED、
准备失败和后续步骤见verification。
契约 `contracts/dice-initiative.md`，5R依据 `docs/research/dice-initiative-5r.md`。
P05代码f416e62的push39468342643/PR39468347675六job全成功：77unit+91真实IT、
195前端/构建、58契约、标准Compose31浏览器及七组实际重启通过；V8→V10保留23
旧表原列/行/八checksum。实际RED/GREEN/失败修复、边界和下一步见continuation/
verification。只提供投骰与先攻准备；回合/伤害仍P06，不合并、不发布。

以下为P04完成时的历史证据：
P04契约 `contracts/content.md`；a7ef388的push39460581794/PR39460588799六job
全部成功：70unit+82真实IT、186前端/构建、50契约、标准Compose27浏览器及六组实际
重启通过。本地无库单测、V8旧数据迁移与Java/PG重启也通过；实际RED/GREEN、限制与
证据见verification/两端TDD。原分支/草稿PR #1不合并、不发布。交接文档新HEAD另核对。

## 工作阶段

| 阶段 | 工作 | 完成条件 |
| --- | --- | --- |
| M0 整体框架 | 独立 Git 仓库；frontend/backend/contracts/deploy/docs/CI；固定依赖与环境示例；模块和接口边界 | 前端可构建，Java 单元测试无需数据库运行；配置中无密码；CI 定义完整 |
| M1 最小可用纵向切片 | 最小失败单元测试 → 实现移动规则；会话登录、CSRF、战役成员、可控棋子、隐藏过滤、版本冲突、请求幂等、PostgreSQL、实时失效通知与重连快照 | 单元/组件/真实 PostgreSQL 集成测试通过；双会话可读取移动后的共同状态，玩家收不到隐藏单位；可用 Compose 启动 |
| M2 首版业务 | 邀请和成员管理、场景与素材编辑、角色卡、规则检索及填卡、服务器掷骰、5R 先攻与手动战斗、RP/附件/日志、更正、备份恢复 | 设计方案中的第一阶段验收场景逐项可重现，一次完整测试团与迁移恢复演练通过 |
| M3 模组与自动化 | 模组包、独立定制与战役实例、选择性更新、手动触发操作、限定支持矩阵内的自动结算、移动预算 | 模板与进行中状态隔离；自动化可解释、可确认、可更正，重试不重复消耗 |
| M4 发布验收 | 权限回归、真实浏览器/双客户端测试、断网与重启、备份恢复、容量测量、部署运维说明 | 自动检查与人工验收有证据，已知限制明确，达到相应发布阶段的范围 |

以下为P03完成时的历史基线（当前P04结果见上）：

**M0/M1、P01、整个P02及P03a–e/整个P03均已完成本地与远端完整验收。**
P03代码f66c4e7的push39452554961/PR39452563178六个job全部success：65unit+75真实
PG/HTTP/WS/迁移、177前端/构建、45契约、标准Compose23真实浏览器和五组实际容器
重启均通过；本地独立无数据库单测、真实Java+PG重启及16旧表原列/原行/六checksum
自然升级比较也通过。原全部测试保留，详细失败闭环与证据见verification和两端TDD。
继续原分支/草稿PR #1，不合并、不发布。下一包P04规则检索与填卡尚未开工；整个M2、
P04–P10/A仍未完成，检索、投骰、战斗、容量/触屏/TLS/完整备份与异机恢复不冒称完成。

## 当前分工

2026-10-21：P01–P07的本地及精确代码SHA双CI门槛全部通过，当前下一包P08团队笔记及DM资料。契约与目录责任延续下表；不重复开发已完成包。完整证据见verification/continuation，M2及P08–P10/A仍未完成。完成文档HEAD亦独立核验。

| 负责方 | 目录与责任 | 依赖 |
| --- | --- | --- |
| 协调者 | contracts、docs、deploy、scripts、CI、Git；工具准备、最终代码审查、集成验收 | 契约先行，汇总各模块测试证据 |
| 后端代理 | backend；领域、应用、JDBC/Flyway、HTTP、会话安全、STOMP | 遵循 contracts；测试数据库由协调者准备 |
| 前端代理 | frontend；React/PixiJS、API 客户端、状态同步、中文界面、组件测试 | 遵循同一契约；不发送自报权限或最终骰值 |

契约变动先协调，禁止分别实现两套不兼容字段；各代理只改自己目录，协调者统一提交。

## M1 测试顺序与验收

1. 建立框架与测试运行器。首先针对移动权限、坐标/版本或客户端状态写最小行为测试，执行并观察预期失败。
2. 实现纯领域行为。用相同测试确认通过，再重构；记录具体命令、结果和测试数量，环境失败不算行为 RED。
3. 接入会话安全、HTTP 与 PostgreSQL。集成测试运行真实迁移与真实事务，覆盖登录、CSRF、隐藏过滤、未授权移动、版本冲突与幂等重试。
4. 接入前端和 STOMP。确认结果才更新已保存状态；失败保留已确认快照；重连重新读取。实时消息仅作失效提示。
5. 两个不同会话进入同一战役：DM 能看到隐藏敌人，玩家不能；玩家移动获授权棋子，另一会话读取同一位置；重复请求不再次移动，过期版本被拒绝；重新建立会话和查询仍读到持久化结果。
6. 运行前端测试/类型检查/构建、后端单元与集成测试、仓库与契约检查。Docker、浏览器或网络受限的检查明确列为未执行，不能当作已通过。

## 执行状态

- M0框架与M1最小切片已实现；目录、契约、前后端、真实PostgreSQL迁移、会话及实时同步已落地。
- 最终本地检查：16后端unit、9真实数据库/WS集成、14前端测试、1真实双浏览器流程、构建及打包进程重启回放均通过。证据见 [阶段验收记录](verification.md)。
- c57c35a已推送，[远端CI](https://github.com/xianchen233/dnd-roll/actions/runs/39124712714)全通过，含Compose构建/启动/真实浏览器；Git终端认证已解决。
- 当前分支feat/accounts-and-invitations，P01正式账号/邀请/成员管理已实现。30后端unit、18真实PG/HTTP/WS、最终36前端测试、2真实Chrome流程与实际Java重启检查通过；初始代码3c12f8d的[远端CI](https://github.com/xianchen233/dnd-roll/actions/runs/39126498540)三个job全部success。后续PR暴露的刷新竞态已通过RED/GREEN修复并推送dabef40，当前交付门槛为[PR #1 Checks](https://github.com/xianchen233/dnd-roll/pull/1/checks)中最新SHA全通过；草稿供审阅，尚未合并或发布。
- P02a 已按 contracts/scenes.md 契约与实际 RED/GREEN 实现 DM 空场景创建、共享当前场景切换、持久回执/版本、旧场景保存、V3 数据保留及前端跨场景响应隔离。37 后端 unit、28 真实 PG/HTTP/WS、前端测试/构建、3 真实浏览器和实际 Java 重启通过；最终审查已修复晚到撤权边界，59 前端测试/构建通过；代码 8836abb 的 push 与 PR CI 三个 job 全部 success，证据见 verification.md。当时下一子包为P02b，后续P02b/c结果见下文；P02整体仍未完成。
- 后续任务不能以“预留接口”为由标记完成。规则相关实现开始前使用工作区 `dnd-rule-lookup` 核对 5R 依据；当前移动仅为探索网格操作，不承诺战斗移动规则。
- P02b私有素材/地图配置已按contracts/assets.md完成实际RED/GREEN、52unit+41真实PG/HTTP/WS全量、90前端测试/构建、24HTTP契约、4真实Chrome、V3→V4数据比较及Java+PG实际重启。最终修复素材确认竞态和提交确认丢失文件保护，代码1bfac83的push/PR全部CI success，含标准Compose私有卷与数据库/后端重启。P02c接续已完成本地56unit+50IT、112前端测试/构建、27契约、7真实Chrome、V4→V5全旧列/行比较和Java+PG实际重启，5160c0f的push39141337705/PR39141340870全部CI success，含七流程及实际容器重启；然后P02d/e，P02整体尚未完成。
