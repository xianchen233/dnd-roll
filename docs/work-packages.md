# 全量工作包与依赖

## A06 路径、预算提示与 DM 覆盖（2026-10-25）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后 clean，本地/origin/PR 精确8a2a6be；独立重取 push40159268831 /
PR40159271974六份完整日志，153unit+185IT、291前端/构建、121契约、55浏览器
及全部旧重启/隔离恢复/容量断言通过（外.tools/a06-entry-ci.log）。
先读交接、验收、设计与契约，再用规则技能回读SRD并核对官方5.2.1。
5尺方格按实际逐格路径计费，斜向一格、地形和基础移动方式加算；角色共享
已用预算，START初始化参战者、进入自身新回合才重置，GRANT不重置。
预算为提示，特殊速度/墙角/占位/效果仍由DM裁定；不读取隐藏障碍或单位推断路径。
DM可带理由覆盖路径最终消耗或校正当前已用/预算，位置/账本/私密历史/回执同事务。
旧坐标移动协议保留但账本标为不完整；控制、可见性、回合授权先于历史回执。
未关联棋子的GET正常返回available:false，不撤销普通地图操作或制造预期404。
中文逐格声明、地图路径预览、单独提交与DM历史保留；不确定写入锁住竞争操作，
精确重试原body/版本/UUID，确认后的读取失败只恢复读取，切域/撤权隔离晚到结果。

真实RED/GREEN及准备错误见 a06-tdd.md。最终159离线unit+199真实IT（13新HTTP+1迁移）、
300前端/生产构建、125契约、56真实生产Chrome通过，零失败/错误/跳过/浏览器重试。
V25逐表保留V24旧列/行及24checksum；独立比较全部旧OpenAPI路径/schema/策略不变。
独立PG55436/新库/HTTP18091/18092实际停Java/web/PG再启，新会话精确核对HTTP与
浏览器路径/预算/DM历史/原回执及旧A03e/A05控件数据，最终JAR哈希匹配。
完整迁移备份并停止此验收来源后，在独立PG55437/新库/素材/HTTP18093/18094恢复，
全部数据库行/迁移/引用/文件匹配，四份移动及旧控件/攻击浏览器记录均保持；
真实恢复账本继续提交原版本草稿并单效重试。验收服务按身份停止，数据与新fence保留；
两验收PG停止、清除数据库环境后159离线unit复验通过。dnd-roll-local及旧fence/恢复目标未操作。
已查看桌面/390px截图，仍不是真实手机。并发云部署文档保留在工作区，不纳入A06提交。

交付HEAD须用外.tools/a06-check-ci.mjs独立重取push/PR六完整日志，严格核验159/199、
300/构建、125契约、56浏览器、全部旧断言及每browser六条MOVEMENT和一条RESUMED。
结果索引a06-ci-{HEAD}.json、六a06-job日志及a06-head-ci-final.log；不能复用开工SHA。
下一可执行：先确认最新HEAD完整CI证据，再处理M3剩余便携组合/附图公开等范围；
特殊移动与扩展效果未自动化，M3整体、云端/TLS/物理异机/真实手机继续保留。

## A05 持久预览、DM确认、原子应用与更正（2026-10-25）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后 clean，本地/origin/PR 精确 68a8a06；独立新取 push40154621542 /
PR40154625315 六完整日志，148unit+175真实IT、283前端/构建、116契约、54浏览器
及全部旧重启/隔离恢复/容量断言通过（外 .tools/a05-entry-ci.log）。
先读交接/验收/设计/契约/工作包并用规则技能回读SRD，再定 contracts/attack-previews.md。
仅可信内置SRD 5.2.1普通单目标武器与DM明确审阅情境；来源/条目被改、精通、未知
例外或未审阅转人工，无部分候选/骰。P04原来源/许可与覆盖标记、A04公式和P06手动
语义保持；不承诺自动识别角色自由文本或执行所有核心/扩展效果。
服务端从当前角色/目录组装并保存暗骰、实际重击额外骰、来源/计算快照和hash。
CONFIRM只存明确最终值，APPLY以双方及关联P06行动原版本一次事务执行P06结果/资源/历史/回执；
CORRECT关联原事件、使用当前版本最终值，保留后续变化；PUBLISH单独公开所选摘要。
当前DM先于旧回执；公开前按当前源/目标可见性过滤，不泄露骰点/AC/精确HP/私密理由。
中文控件锁住不确定请求，精确保留原body/hash/版本；确认后读取失败只重新读取。
切团/API/角色域与并发晚到撤权清理私密状态；完整来源、逐骰、计算和历史可展开。

真实RED/GREEN、测试准备错误及限制见 a05-tdd.md：4离线runtime RED、4真实HTTP404
RED、5前端RED及契约RED；另真实RED修复可变列表证据与长依据/P05标签上限。
最终全量153unit+185真实IT（9新HTTP+1迁移）、291前端/生产构建、121契约、55真实
生产Chrome全部通过，零失败/错误/跳过/浏览器重试。V24逐表保留V23旧列/行及23checksum。
独立PG55434/新库dnd_a05_final/HTTP18089/18090实际停Java/web/PG并重启，新会话逐字段核验HTTP和浏览器
创建的预览/待确认状态/证据/hash/骰/回执及原A03e组合恢复数据（a05-final-restart-verify.log）。
此新验收实例完整迁移备份后停止源，恢复到独立PG55435、新库/素材/HTTP18087/18088；
a05-final-restore.log验证全部数据库行/迁移/引用/文件，a05-final-restore-verify.log验证两类预览及
原组合控件，且实际继续确认/应用一个恢复后的待确认预览，原骰与hash保持、重试单效。
已查看桌面/390px截图；这不是真实手机验证。验收进程按身份停止、数据和新验收source
fence保留；旧source fence/恢复目标、dnd-roll-local及其数据完全未操作。
停止验收PG并清除数据库环境后153离线unit复验见 a05-unit-pg-stopped-final.log。

交付HEAD的push/PR必须独立重取六完整日志，不能复用68a8a06：外 .tools/a05-check-ci.mjs
严格要求153/185、291/构建、121契约、55浏览器、全部旧断言、每browser两条CONTROLS、
六条ATTACK PREVIEWS及一条ATTACK RESUMED。最终索引a05-ci-{HEAD}.json、六a05-job日志
及a05-head-ci-final.log；缺失/pending/failure不算通过。CI保留原容器重启/独立项目卷恢复/
容量流程，新增HTTP与真实浏览器预览精确验证及恢复后继续应用。以精确HEAD证据为准。
下一可执行：先独立核验交付HEAD，再推进A06移动路径/预算契约与5R依据、实际RED；
A03便携组合/附图公开、M3剩余范围、云端/TLS/物理异机/真实手机继续保留。


## A04 有界自动结算矩阵与纯计算器（2026-10-25）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后 clean，本地/origin/PR 精确 c52d3ef；重新下载 push40120580408 /
PR40120584894 六完整日志，130unit+175IT、283前端/构建、116契约、54浏览器及
所有原重启/隔离恢复/容量断言通过（仓库外 .tools/a04-entry-ci.log）。
先用本地规则技能定位/回读SRD 5.2，再核对官方5.2.1；矩阵与明确扩展边界见
research/automatic-attacks-5r.md，内部契约 contracts/automatic-attacks.md。
本轮只实现明确审阅的单目标普通武器候选：天然1/20、优劣势、重击骰计划及
单类型伤害免疫/抗性/易伤。未知来源/版本、未审阅情境、所有额外效果均转人工，
不返回部分候选；扩展自动化条目数0，不推定2014兼容或核心特性全支持。
先执行10项真实runtime RED（1failure+9error，尚无计算实现），实现后修正一项
优势骰fixture；最终18项聚焦、148离线unit、283前端/构建、116契约通过。
复核再实际RED证明P04来源书名不匹配；统一为既有SRD 5.2.1身份，真实目录填卡
后的entry/许可快照保持，不重命名历史来源。不接HTTP/UI、不改HP/资源/状态或迁移；
没有新本机PG/浏览器/重启/独立恢复声明。命令与限制见 a04-tdd.md。
代码668f9d6已独立核验push40153847972 / PR40153850400六份新取完整日志：
两run均completed/success，148unit（18计算器）+175真实IT、283前端/构建、
116契约、54浏览器及全部原重启/独立卷恢复/容量断言和每browser两条CONTROLS VERIFIED。
证据外.tools/a04-code-ci.log、a04-ci-668f9d6a224772219fab06583ec60847ae3ebc7f.json
及六a04-job日志。旧52e4131双run已被新提交取消，不算完整通过。
这个明确矩阵内的A04已验收；本次文档新HEAD仍须用外.tools/a04-check-ci.mjs
独立重取六日志，最终索引a04-head-ci-final.log / a04-ci-{HEAD}.json，不复用代码SHA。
下一步：复核最新HEAD后进入A05持久预览、DM确认、事务应用和更正，先契约与RED；
A03便携组合/附图公开、A06/M3、云端/TLS/物理异机/真实手机继续保留。
未启停/重置dnd-roll-local或操作其数据，旧source fence/恢复目标保持。


## A03e 中文选择、实况影响与手动组合（2026-10-25）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工 fetch 后工作区干净，本地/origin/PR 精确 9c17675。连接器传输失败后，
独立重取 push40095551240 / PR40095555043 六完整日志，均 completed/success：
130unit+175真实IT、249前端/构建、116契约、52浏览器及全部旧重启/隔离恢复/
容量断言通过；外 .tools/a03e-entry-ci.log，不复用为新 HEAD 的证据。

独立选择性更新面板逐项 KEEP/INCOMING，无默认采用；服务器重算完整影响后才应用，
并可下载更新前恢复包。实况文字和结构迁移分别预览，读取当前来源副本版本，
结构变化逐项明确确认、显示中文解释/精确标识/实况前像及独立恢复战役入口。
本团手动组合支持五类 typed 步骤、目标选择/排序/显式采用当前版本、保存后预览
再触发；先攻生成仍为 DRAFT，START 只绑定已 CONFIRMED 的精确版本。
不确定结果锁住该操作编辑，保留原路由/UUID/完整body/版本/hash；刷新不重基，
确认后的读取失败不提供重复写重试。切团/副本/API域或撤权清私密状态/回执/
确认/Blob URL，独立观察并发晚到401/403，旧域成功或拒绝不污染新团。
实际TDD及准备错误见 a03e-tdd.md；补充真实RED修复候选缺依赖时KEEP被过度阻断。

本地完整130unit+175真实IT（独立55433/dnd_a03e_test，零失败/错误/跳过），
283前端/生产构建、116契约通过；全部旧52+新2条真实生产Chrome流程通过。
未改backend/OpenAPI请求或V1–V23迁移，旧数据/列/checksum门槛保持。
独立18083/18084、dnd_a03e及独立素材/备份上实际停Java/web/PG再重启，新会话
逐字段核验浏览器创建的选择更新/实况迁移/恢复ZIP/地图/角色/五类操作/原请求，
旧回执不覆盖后续扣血、隐藏、位置、停止战斗、进度或RP；证据外.tools/
a03e-{backend-full,frontend-final,build-final,browser,restart-verify,controls-restart-verify-final}.log。
停独立PG并清除数据库环境后，130离线unit再次通过（a03e-unit-pg-stopped.log）。
已人工查看桌面/390px截图；这不是真实手机验证。本机未新做独立卷恢复。
CI新增两份浏览器真实产物capture，在原容器重启及独立项目/卷恢复后逐字段verify；
原所有旧断言/容量检查保留。最终新HEAD必须独立重取push/PR六日志，外.tools/
a03e-check-ci.mjs要求130/175、283/构建、116、54浏览器、全部旧断言及每browser
两条CONTROLS VERIFIED；结果索引a03e-ci-{SHA}.json、六a03e-job-*.log及
a03e-head-ci-final.log。pending不算通过，文档也不把开工SHA绿灯冒称最终HEAD通过。
首次提交1db4702的push40119506867在零job阶段失败：job级env误用runner.temp。已改为浏览器run步骤使用$RUNNER_TEMP并保留所有恢复断言；修复后的新HEAD必须另取完整双CI证据。

代码e680c36已独立重取push40119684682 / PR40119689443六完整日志并严格通过：
两run均completed/success，各130unit+175真实IT、283前端/构建、116契约、54浏览器、
全部旧重启/独立恢复/容量断言及每browser两条CONTROLS VERIFIED。实际浏览器创建的
更新、恢复ZIP/地图/角色和五类组合在真实容器重启及独立项目/卷恢复后完整保持。
证据a03e-ci-e680c36f98c447d74fbac0185845e4cc52a5f79d.json、六a03e-job-*.log和
 a03e-code-ci-final.log；本交接新增HEAD仍须用a03e-check-ci.mjs独立验证，不复用代码SHA。

体验项目dnd-roll-local及数据、旧source fence/恢复目标未操作。验收进程按身份停止，
所有测试数据保留。下一可执行：独立核验最终HEAD完整日志；通过后进入A04支持矩阵
与明确5R依据/契约/失败测试。便携模板组合定义、附图公开、自动条件/效果仍后续，
A03有界切片不能冒称全部产品完成；A04–A06/M3、云端/TLS/物理异机/真实手机后置。


## A03d 实况手动组合服务切片（2026-10-24）

沿用 feat/accounts-and-invitations / 草稿 PR #1，不合并、不发布。
开工fetch确认clean，本地/origin/PR为ee9039a；独立重取push40085533515 /
PR40085536284六完整日志，126unit+166IT、248前端/构建、111契约、52浏览器及全部
旧重启/隔离恢复/容量断言通过（外.tools/a03d-entry-ci.log），不重复A03a/b/c实现。

战役内可编辑组合：1..20严格typed步骤、完整save最多64KiB、每实例100定义；
分别揭露棋子/角色，DM无图文字复制为TEAM笔记，原P05生成DRAFT先攻，或原P06
启动明确CONFIRMED的精确先攻版本。每组合最多一个遭遇步骤，无隐式平手确认/效果。
先重算全部目标/版本/本地精确依赖和hash，再原服务同事务执行并保存实际actor/证据/
回执；当前目标DM先于历史回执，独立团不依赖原模板库DM身份。
实际RED修复组合内揭露后角色版本、START预览未绑定当前参战者、私密缓存头。
即时/延迟commit故障逐字段验证五类操作状态/历史/所有child回执/外层证据原子回滚。
并发单效/双DM单胜、后续编辑/隐藏/扣血/停止后旧回执不重放；附图资料明确409，
不静默丢图或公开。契约 contracts/module-operations.md；TDD/准备错误见 a03d-tdd.md。

最终a03d-full-final.log：130unit+175真实IT（8新操作HTTP+1迁移，旧38生命周期保持），
249前端/生产构建、116契约通过，零失败/错误/跳过。旧path/操作/schema保持，仅错误码
兼容增补；V23保留所有旧行/列/22checksum。独立18082/dnd_a03d_ops/独立素材和备份
上的真实Java/PG重启、新会话逐字段verify通过（a03d-restart-{seed,verify}.log）；
本地未新跑生产浏览器或独立卷恢复。停库/清除环境后130离线unit通过。验收服务按
身份停止、数据保留；dnd-roll-local体验项目/数据、旧source fence/恢复目标未操作。

check-operations-restart.mjs已接入CI原重启/独立项目恢复。最终新HEAD须独立重取双CI
六日志：外.tools/a03d-check-ci.mjs严格要求130/175、249/构建、116、52浏览器、全部
旧断言和4条OPERATIONS VERIFIED；索引a03d-head-ci-final.log、a03d-ci-{HEAD}.json
和六份a03d-job-*.log，pending不是通过，开工SHA不能替代新HEAD。
下一可执行：先独立核验最新HEAD，再推进A03e中文选择/影响/组合编辑/触发、原请求
精确重试/切团撤权隔离及最终浏览器/重启恢复联验。便携模板操作定义/附图公开、
自动条件/效果不计本轮已实现范围；A03/A04–A06/M3未关闭。云端和真实手机继续后置。


## A03c 恢复限额审计完成；下一包 A03d（2026-10-24）

499/500/501确认、128/129组合图片、共享素材身份、32MiB含清单正文总量、10MiB ZIP，
及降回限额后实际恢复与文件SHA已追加验证。运行时首跑通过，无后端生产代码改动；
契约实际RED→GREEN修复阻断预览误限500项响应，应用请求限额保持。完整126unit+
166真实IT、111契约通过。见[a03c-limits-tdd.md](a03c-limits-tdd.md)，新HEAD双CI须重取。
下一可执行A03d契约/失败测试：有界typed可编辑手动操作组合，共用原服务原子执行，
撤权先于回执、并发/回滚。之后A03e中文选择/影响/触发与最终联验；A03/A04–A06/M3
不提前关闭，云端/真实手机后置。原分支/草稿PR #1不合并发布；体验项目和数据未动。

## A03c 目标地图及阻断边界（2026-10-24）

真实两项RED修复目标地图缺图漏检与资料重排误阻断；补齐失效实况链接、图片完整性、
资料/策略/manifest拒绝及无部分写入回归。最终126unit+163真实IT、248前端/构建、111契约，
独立18081新库上的实际Java/PG重启/新会话逐字段verify通过。命令及中间错误见
[a03c-boundary-tdd.md](a03c-boundary-tdd.md)；最新HEAD双CI按外.tools/a03c-boundary-check-ci.mjs
重新核验，pending不算通过。A03c/A03不提前关闭，继续审计后依次A03d手动组合、A03e
中文控件/最终联验；A04–A06/M3不丢项。云端/真实手机后置，原分支/草稿PR #1不合并发布。
本轮未启停/重建体验项目或操作其数据，测试使用独立库/素材及其他端口。

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
最新本地结果及精确最终 HEAD CI 以该文档和外 .tools/a03c-* 证据为准。
原 source fence/恢复库/素材保持；本轮验收使用独立 a03c-backups，不能移除源端停写。


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

## P09 功能及本地/容器验收通过，云端门槛后置（2026-10-22）

P01–P08不重做；沿用原分支/草稿PR #1。P09a停写与格式、b真实归档、c持久任务/
每日7份/下载/失败/中文入口、d独立空库/新素材根离线恢复与迁移源端fence已实现，
聚焦实际RED/GREEN及十组新会话恢复通过。e最终102+118串行和独立102无库通过，
代码f6c9130的push39633129367/PR39633137675六份完整日志已重新核对通过：两组
102+118、225/构建、87契约、44浏览器、十组实际容器重启及独立新项目/新卷离线恢复、
待确认行动与十组新会话核验全部通过。完成文档HEAD另核验。
用户已有云服务器但未准备环境，明确将云环境、部署与物理
异机恢复后置到整体功能完成后统一联验；该最终门槛保留。之后可进入P10功能验收，
不提前宣称P10/A/M2或发布完成。详情见verification/continuation及docs/p09-tdd.md。

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


本清单承接产品设计第一阶段、第二阶段与后续方向，不把整个产品误当作本次框架提交。阶段状态以 `implementation-plan.md`、`continuation.md` 与 `verification.md` 为准。每个工作包开工时再确定该包的接口、数据迁移和行为测试，避免提前编写大量未经验证的实现细节。

## M0 / M1：当前批次

| 工作包 | 前置 | 交付与验收 | 责任 / 状态 |
| --- | --- | --- | --- |
| F01 仓库与框架 | 无 | 用户指定目录、隔离本地资料、远端绑定、运行配置、版本锁定 | 协调者 / 已建并本地提交 |
| F02 契约 | 产品设计 | HTTP OpenAPI、会话/CSRF、移动幂等与版本、STOMP安全消息、无依赖契约检查 | 契约代理 / 已实现并检查通过 |
| F03 最小领域行为 | F02 | 移动边界、控制权、隐藏、冲突与幂等的真实 RED/GREEN | 后端代理 / 已实现，见 TDD.md |
| F04 数据库与接口 | F03 | PostgreSQL迁移、事务回执、登录/退出、权限裁剪；真实DB/HTTP检查 | 后端代理 + 协调者 / 真实DB检查通过 |
| F05 前端桌面 | F02 | 登录、网格棋子、权威快照、失败/冲突/重试、重连；组件测试与构建 | 前端代理 + 协调者 / 14测试、构建及真实双浏览器通过 |
| F06 实时与会话安全 | F04/F05 | 用户队列、无秘密广播、正确CSRF、拒绝SEND/任意订阅、退出关闭socket | 后端代理 + 协调者 / 4项原生WS检查通过 |
| F07 部署与CI | F01/F04/F05 | Compose三服务、持久卷、NGINX、Maven Wrapper、前端锁、CI | 协调者 / c57c35a远端容器与浏览器CI通过 |
| F08 阶段验收与提交 | F01–F07 | unit/真实DB/真浏览器证据，更新交接与状态；Git提交并推送指定远端 | 协调者 / M0/M1已提交推送，CI全通过 |

## M2：完整首版业务

当前：**P01–P06全部完成本地与精确代码SHA的远端完整验收；下一包P07未开工。**
P06代码d23c592的push39477351204/PR39477361622六job/完整日志全success：81unit+
100真实IT、201前端/构建、65契约、标准Compose35浏览器及八组实际容器重启PASS。
本地无库81、完整同等检查、28旧表/10checksum及八组Java/PG重启也通过；详见
verification/continuation。完成文档HEAD另核验；P07–P10/A及整个M2仍未完成。
以下为此前开发状态，不作为当前下一步。

当前：**P01、整个P02、P03a–e、P04a–e及P05a–e均已完成本地与远端完整验收。**
P05代码f416e62的push39468342643/PR39468347675六job全success：77unit+91真实IT、
195前端/构建、58契约、标准Compose31浏览器与七组实际容器重启通过。23旧表全列/
行/八checksum保持；下一包P06，整个M2/P06–P10/A仍未完成，完成文档HEAD另核验。
P06已开工：契约`contracts/combat.md`、依据`docs/research/combat-5r.md`。P06a行动与
关联、b裁定/显式应用/更正、c角色回合与移动授权、d中文界面本地实现；e完整验收
仍在执行。最终81unit+100IT、独立无库81、201前端/构建、65契约、完整35浏览器及
八组实际Java/PG重启通过；精确代码SHA双CI待核验，不提前关闭P06。
P06有序切片与最终门槛见契约；后续P07–P10/A及M2均保持未完成。
以下为P04完成时的历史基线：
P04代码a7ef388的push39460581794/PR39460588799六job全success：70unit+82真实IT、
186前端/构建、50契约、标准Compose27浏览器与六组实际容器重启通过。旧回归全部
保留，V8迁移保留20旧表原列/行/七checksum；详见verification。沿用原分支/草稿PR #1，
未合并/发布。P05后续完成结果见上；最新文档HEAD的Checks也独立核对。

| 工作包 | 前置 | 首先测试的行为 / 交付边界 |
| --- | --- | --- |
| P01 正式账号、邀请与成员 | M1 | 固定账号、DM邀请、撤销成员、不同战役权限隔离；管理/初始化入口替换demo依赖；会话权限变更可撤销 |
| P02 场景、地图与素材（已完成） | P01 | 创建/切换多个场景、地图上传校准、棋子配置与共享控制、手动遮盖和单位隐藏；原图及秘密素材不可从玩家接口/缓存获取 |
| P03 完整半自动角色卡（已完成） | P01、F03 | 一人多角、独立资源、基础计算及来源、编辑历史、DM覆盖、状态到期提醒、复制及结构化导入导出；5R字段逐项核对 |
| P04 规则检索与填卡（已完成） | P03 | 可分发的开放数据、自定义条目、名称/来源/规则版本、准入切换、结构化填卡；检索覆盖不等于自动执行覆盖 |
| P05 掷骰与5R先攻（已完成） | P03/P04 | 服务端随机结果、逐骰/修正快照、暗骰、按钮/命令共用逻辑；参战/分组/突袭/平手/隐藏先攻以5R来源核验后测试 |
| P06 手动战斗与更正（已完成） | P02/P03/P05 | 行动与目标、命中裁定、伤害手动应用、角色级回合、回合外操作、来源日志和留痕更正；并发重试不重复扣资源 |
| P07 RP时间线与机器人（已完成） | P01/P03/P05/P06 | 文字/图片/引用、发言身份快照、编辑历史、检索/分页/导出、常用角色操作；长久保留，私密内容不经引用泄漏 |
| P08 团队笔记及DM资料（已完成） | P01/P02 | 团队共享与DM私密分离；查询、搜索、附件与导出遵守相同授权 |
| P09 备份、恢复与迁移（功能/本地/容器通过，云端联验后置） | P02–P08 | 每日自动备份默认7份、手动下载、数据库+素材一致性、待确认结果恢复；轮换不清除RP/日志/被引用附件；物理异机恢复按用户要求在整体功能完成后统一实测 |
| P10 完整测试团与首版验收（本地/容器通过） | P01–P09 | 产品设计首版验收逐项运行；重试/断网/重启/越权/手机入口、备份恢复与容量测试有记录；未通过项不标完成 |

P02 和 P03 可在契约协调后并行；P07 附件存储依赖 P02，记录语义依赖 P05/P06。测试、权限和失败恢复随功能一起交付，不集中拖到 P10 才补。

### P02接手顺序与分工

先在既有审阅分支上续接，保留既有M1/P01/P02a/P02b/P02c/P02d测试。开工先写对应契约，再分别派发backend、frontend、contracts/QA；协调者负责docs/deploy、持久目录、实际双客户端与重启验收和Git。以下为已完成的有序子包，P02a–e均有本地及远端完整CI证据；后续仍保留全部回归：

1. P02a 多场景与团当前场景（已完成，8836abb 本地及 push/PR CI 通过）：先RED验证DM创建/切换、玩家不可切换、过期版本、同requestId重试以及切换后旧场景保存；统一`GET .../scene`始终返回DM指定的全团当前场景。V3保留V1/V2数据与当前地图，安全失效事件沿用无秘密消息。先完成此最小切片再引入图片。
2. P02b 持久素材与地图配置（已完成，1bfac83本地与push/PR完整CI通过）：稳定素材ID与独立存储键、PNG/JPEG真实解码与限额、V4同团引用FK、DM下载授权、Compose私有持久卷。契约先行与实际RED/GREEN，其他战役/玩家/猜ID/直链拒绝、回执/commit失败回滚及新文件清理、提交确认丢失保护已提交文件、Java+PG及Compose实际重启SHA/引用/回执均有验收证据，见verification.md。共享逻辑网格不变，玩家原图权限未开放。
3. P02c 棋子作者与共享控制（已完成，5160c0f本地及push/PR完整CI通过）：DM创建/完整配置名称/位置/隐藏和多控制者，移动保留原协议与版本/幂等；独立持久回执、V5同团成员FK、移除成员推进相关版本，历史回放/重新加入不恢复控制。56unit+50真实PG/HTTP/WS、112前端测试/构建、27契约、7真实Chrome及实际Java+PG重启通过；全部旧数据列/checksum保留比较通过，隐藏字段不进共享快照/错误/事件。标准Compose七流程和素材/棋子实际容器重启也通过；交互竞争修复有RED/GREEN，详情见verification及两端TDD。
4. P02d 地图交互及手动遮盖（已完成，7f539cf本地及push/PR完整CI通过）：先固定contracts/map-visibility.md并实际RED，含绕过revealed的秘密像素故障注入；服务端重新编码安全PNG、显式内存流、当前场景/版本/授权及无缓存交付，独立fog持久回执和V6，地图校准/换图/拆离原子全盖。原生Pixi按需绘制、平移/光标缩放/校准/吸附拖动/几何测距及矩形、坐标遮盖，单位隐藏独立。61unit+60真实PG/HTTP/WS、166前端测试/构建、31契约、13真实Chrome生产构建、14旧表全列/行自然升级及Java+PG三类重启检查本地通过；标准Compose13流程及素材/棋子/地图实际容器重启均通过，保留全部旧测试。刷新吞手势/草稿、场景preflight竞争均有实际RED/GREEN，完整记录见verification，不能提前标P02或P02e完成。
5. P02e 整体验收（已完成，853944d本地及push/PR完整CI通过）：按 `contracts/p02-acceptance.md` 独立组合实际DM/玩家切图、授权/撤销、地图揭露、真实browser offline加原生socket关闭/服务端新CONNECTED、提交后丢响应/精确重试/失败读恢复、两场景进程重启。61unit+61IT、166前端/生产构建、31契约、17真实浏览器及Java+PG四组重启本地通过；两组标准Compose17流程和四组实际容器重启均通过后才关闭整个P02，原全部功能/测试保留。CHANGELOG/契约/验收/交接已更新，下一包P03未开工；容量、素材一致备份与异机恢复仍按P09/P10执行。

### P03 有序子包（已完成，f66c4e7 本地与双 CI 通过）

契约 `contracts/characters.md`，规则依据 `docs/research/character-sheet-5r.md`。
2026-10-20 从 b1f2c5f 开工，独立核对其 push39439094794 / PR39439269275
六 job 全绿，本地/origin/PR HEAD 一致，PR #1 open/draft。P01/P02不重复开发。

1. P03a 独立角色/完整字段/一人多角/权限/历史：已完成独立身份/完整字段/授权/历史验收。
2. P03b 5R基础计算/组成来源/DM覆盖：4项实际RED→GREEN，5R来源已查并通过完整验收。
3. P03c 资源/状态与到期确认：已完成，资源独立/精确重试/到期只提醒均已验收；不冒称投骰或战斗。
4. P03d 复制/结构化传输/目标审核/棋子引用：已完成；待审核复制绕过、隐藏结果回执均实际RED修复并验收。
5. P03e 全量验收：已完成；新/旧真实浏览器、迁移、五组实际服务/容器重启和代码SHA双CI全通过。

当前本地完整65unit+75真实IT、177前端/构建、45契约、23真实浏览器和五组实际服务
重启通过；16张旧表全列/全行/六checksum自然升级一致。代码f66c4e7的push39452554961/PR39452563178六job全部成功。原61unit/61IT/166前端/31契约/17浏览器/四组重启全部
保留。上述为P03完成时的历史状态；P04后续已完成，具体最新结果见verification/continuation。

### P04 有序子包（2026-10-21，已完成，a7ef388双CI全绿）

契约 `contracts/content.md`，依据 `docs/research/content-5r.md`。

1. P04a 来源与检索：十条带许可/署名/修改说明的SRD5.2.1精选、名称/别名/关键词及类型/来源/规则/准入筛选；明确收录与填卡/自动执行覆盖。
2. P04b 自定义与准入：团内公开、不可变身份的可复制条目；DM审核/自由选用，切回不改旧卡；不授予角色/棋子权限。
3. P04c 服务器预览/填卡：当前角色与策略版本、明确属性/施法来源、历史/来源快照、授权先于幂等回执；不执行效果或赋予熟练/精通。
4. P04d 中文工作区：来源/许可/版本/支持详情、预览确认、草稿与切域/撤权隔离、不确定原body重试及新读确认。
5. P04e 完整验收：保留所有旧回归，新HTTP/迁移/浏览器/六组实际重启与精确代码SHA的push/PR CI；通过后才关闭P04。

P04a–e全部完成，最终代码SHA的本地与push/PR完整CI全通过，结果见verification；P05–P10/A及整个M2继续未完成。

### P05 有序子包（2026-10-21，已完成，f416e62双CI全绿）

契约 `contracts/dice-initiative.md`，依据 `docs/research/dice-initiative-5r.md`。

1. P05a 受限骰式、服务器熵、逐骰/取舍/常数/优劣抵消：实际4项RED→GREEN。
2. P05b 当前身份/角色权限、暗骰、不可变来源快照、按钮与指令同持久回执：真实HTTP缺入口RED→GREEN，重试/并发/提交失败/撤权/分页均已执行。
3. P05c 独立角色参战、显式相同怪物组、突袭来源、候选次序/理由化确认、历史：实际2入口RED→GREEN；不执行P06回合/伤害。
4. P05d 中文工作区、来源/逐骰展开、重试/读确认/隔离与重连：7入口RED→GREEN；实际offline重连404旧记录残留再RED→GREEN，历史展开再RED→GREEN。
5. P05e 完整验收：已完成；本地77unit+91IT、195前端/构建、58契约、31浏览器、七组实际Java/PG重启及23旧表原列/行/八checksum通过，代码SHA双CI六job/两组标准Compose31流程/七组容器重启也全PASS，详见verification/continuation。

## M3：模组与有限自动化

| 工作包 | 前置 | 验收边界 |
| --- | --- | --- |
| A01 模组包身份、格式与导入校验（已完成） | P02/P04/P09 | 版本清单、依赖与素材引用、缺失可见、不执行任意代码、失败原子性 |
| A02 模板、定制副本与战役实例（已完成） | A01 | 同模板多团独立进度；定制可另存模板；模板更新不覆盖进行中的资源与记录 |
| A03 选择性更新与手动触发（A03a–e有界切片已实现；最终HEAD远端联验按独立证据核验） | A02 | 中文逐项选择/实况迁移确认/五类组合与精确重试，本地283前端、54浏览器、真实重启通过；最终双CI及独立恢复以a03e-tdd.md证据为准；便携组合/附图公开等仍后续 |
| A04 自动结算支持矩阵（单目标普通武器有界配置已验收） | P04/P06 | 选定明确行动和5R/扩展版本，先写规则依据与测试，再写计算器；不支持的例外进入人工裁定 |
| A05 预览、确认、应用与更正（有界武器配置本地完整通过；交付HEAD双CI按独立索引核验） | A04/P09 | 可展开解释与来源快照、DM确认、一次事务应用、暗骰独立公开；重试与状态变化不会重复消耗 |
| A06 移动路径与预算提示（有界5尺方格配置本地完整通过；交付HEAD按独立CI索引核验） | P02/P06 | 实际逐格路径、基础移动方式、角色共享预算、DM覆盖与私密历史、原回执；特殊速度/障碍/扩展效果仍人工 |

## M4：发布门槛与更晚方向

发布按目标版本运行对应验收：权限回归、生产构建、真实浏览器/触屏、Docker启动、持久化重启、异机恢复、容量测量、TLS与会话安全、运行日志和故障操作手册。固定实际验证过的镜像摘要，并约定升级流程。

自动视野、分队地图、条件剧情、文档辅助转换、DM本机穿透/P2P、QQ和旧记录导入独立评估；当前没有把它们排入首版。新增范围必须更新产品设计、工作包和依赖，不静默挤入当前阶段。

## 每包完成的证据

1. 新行为有先失败后通过的测试，或明确说明仅为配置/文档变更；不伪造RED。
2. 对应契约、迁移、权限和失败行为完成；测试不能仅镜像实现。
3. 记录实际命令、结果、未执行检查、已知限制与Git提交；更新本表状态。
4. 写清下一工作包及其前置，不依赖旧会话或已结束的代理。

### P07 有序切片（2026-10-21，本地通过；代码SHA双CI待核验）

契约 [timeline.md](../contracts/timeline.md)。a消息身份及更正、b图片/引用、c统一检索/场次/导出、d中文UI与共用bot已实现；e本地84unit+108IT/211前端/75契约/39浏览器/九组真实重启通过，远端完整验收待核验，P07不提前关闭。后续P08–P10/A及M2仍未完成。

### P07 最终交付（5d96a72，本地与双CI全通过）

a身份与不可变编辑、b持久图片和当前权限引用、c统一分页/检索/场次/完整导出、d中文UI与P03/P05/P06共用机器人入口、e全部原回归/迁移/真实浏览器/九组真实服务及容器重启均已完成。最终84unit+108IT、211前端、75契约、39浏览器、V12全部旧列/行/12checksum与精确代码SHA的push/PR完整日志全通过。下一包P08尚未开工；P09/P10/A和M2仍未完成，完成文档HEAD继续独立核验。

### P08 有序切片（已完成，2026-10-21）

契约 contracts/notes.md。a共享/私密/历史与回执、b持久图片/搜索/完整导出、c中文工作区已实现且聚焦RED→GREEN；d本地87unit+114IT/221前端/83契约/43浏览器/十组真实Java+PG重启通过；精确代码1450e5d的push39615748242/PR39615751296六job与两组标准Compose完整流程/十组容器重启全部通过，P08完成。下一包P09尚未开工。下一步及实际结果见continuation/verification；P09/P10/A及M2仍未完成。
