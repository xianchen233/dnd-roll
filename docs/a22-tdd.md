# A22 原A21失稳后的治疗：实际测试记录

2026-10-29，未提交。按AGENTS读取最新交接/验收/设计/计划/CONTEXT/契约并检查
Git。原分支feat/accounts-and-invitations、HEAD c034aa4及草稿PR #1沿用，
263份入口改动/云补充逐字节备份于仓库外`.tools/a22-entry`和manifest。
无暂存/提交/推送/合并/发布，不操作dnd-roll-local及数据或旧fence。

先[dnd-rule-lookup来源/矩阵](research/instability-healing-5r.md)→
[独立契约](../contracts/instability-healing-effects.md)/OpenAPI→实际RED。
A只定位商业书，B完整回读0HP/治疗/临时生命，复用本会话已直接获取并核验的
官方SRD5.2.1 pp17–18；未读取商业正文或混用2014/扩展，也未声称重查在线勘误。

## 实际 RED / GREEN

- `.tools/a22-domain-red.log`：253离线unit中新增8项有7断言失败，0错误/跳过。
  空实现不生成真实治疗/移除原昏迷/人工原因或非法输入拒绝，是实际行为RED。
- `.tools/a22-http-red.log`：253unit全部通过，11真实PG/HTTP项因新路由404
  失败，0错误/跳过；无把编译错误或未执行测试当RED。
- `.tools/a22-ui-red.log`：6界面项因缺少实际目标/输入/操作控件失败。
- `.tools/a22-http-green.log`：253unit+11真实HTTP通过，0失败/错误/跳过。
- `.tools/a22-ui-green.log`：7界面+1API通过，新增界面历史核对防止把其他治疗
  入口的HEALED误报成本入口已保存。`.tools/a22-frontend-full.log`55文件427项
  通过；`.tools/a22-build.log`构建成功，只有既有大于500KB分块提示。
- `.tools/a22-contract.log`174operations通过，验证完整必填字段/非法输入、合法
  HP/实际治疗/死豁清零与manual响应；旧171个操作和所有旧schema保持。

## 实现与边界

新增独立InstabilityHealingEffect/Service/Controller/JdbcStore，不放宽A20双记录门。
要求A19账本UNSTABLE、共享RESTABILIZATION_UNSTABLE，原A21日志准确绑定双版本。
核对前一APPLIED/RESTABILIZED完整状态及只改变状态名的A21转换，完整重建
A16→A18→A19→A21、两次原服务器骰、原A21方案hash/卡/日志与当前完整卡。
方案hash还绑定整份原A21私有日志，预览后任何该证据变化使原方案不能应用。

只处理原A21结果未改：HP0、成功0/失败1或2、唯一原a16-unconscious、无导入审核/
修正器/覆盖。DM裁定真实治疗量，HP上限封顶、死豁0/0，只移除这一条原昏迷，
临时生命/资源/原骰/施救者保持。原a19-stable已由A21移除，不再次删除。施救者
后续编辑不影响已应用来源；当前目标后续任何编辑则人工，精确旧回执仍保留游玩。

应用同事务写完整卡、安全角色历史、双HEALED状态/双历史、私有治疗日志和回执。
理由/请求量/审阅不进玩家角色历史。当前DM与目标存在先于精确旧回执；新命令
不采信客户端damageVersion/最终HP/移除列表。没有治疗骰/资源/行动/完整状态引擎。

11真实HTTP项覆盖：治疗上限/清零/唯一原记录/原骰与施救者保持；精确重试/跨路由
冲突及后续游玩不覆盖；控制玩家/匿名/CSRF/撤权/no-store与私有角色历史；严格
字段/null/重复/类型；来源/完整卡/导入/三版本/尝试/hash；同键异键并发单次应用；
立即与延迟触发器失败对完整卡/角色历史/双状态/双历史/新旧日志/回执整笔回滚；
原A18日志篡改拒绝；原A21hash/双版本/critical篡改拒绝，预览后日志理由变化使
保存方案失效；旧A18/A19/A20不能误入、目标删除后旧回执404。

V33新增私有治疗日志，FK关联确切A21源伤害版本、A19结果历史版本和共享结果
历史版本。独立PG18500、Java18165/网页18166，恢复库18502/18167/18168；运行时、
凭据、数据和证据均在仓库外。两套六服务最终按身份停止、数据保留，实际最终结果如下。

M3未关闭。完整P09、含新绑定的独立战役ID重映射恢复、容量与精确交付HEAD日志
仍待；云端/物理真机后置，无远端新HEAD CI或发布结论。

## 最终本地验收

- `.tools/a22-backend-full.log`：253离线unit与354完整真实PG/HTTP/WS IT全部通过，
  0失败/错误/跳过。含新增迁移1项与HTTP11项。
- V33迁移测试先V32种A16/A17/A18/A19/A20/A21历史，旧所有表列/行与32个checksum
  保持；新治疗日志错误源damageVersion/A19结果版本/共享结果版本均被FK拒绝，
  正确三重绑定可写。
- `.tools/a22-frontend-full.log`55文件427测试、构建通过；契约174operations，
  原171个操作及所有旧schema完整保持，只新增3操作/5schemas，无旧状态enum变更。
- `.tools/a22-browser.log`新增生产Chrome定向首次通过：真实治疗提交丢响应后
  精确重试，只应用一次，HP3/tempHP1、死豁0/0、唯一原昏迷解除，资源/施救者/
  原骰保持。保存另一原目标及上限治疗方案用于停启。桌面/390px截图已查看，
  无横向溢出，软件视口不代表物理真机。
- `.tools/a22-chrome-final.log`：76完整Chrome零retry通过，使用固定验收jar。
- `.tools/a22-before-restart.log`、`a22-after-restart.log`：新会话完整卡/角色历史/
  双账本/双历史/A18与A21伤害日志/新治疗日志/原骰/全部旧回执深比较、保存方案
  hash保持。Java/网页/PG按PID命令身份核对并确认端口无监听后，用同一jar/数据
  真正重启，不以刷新浏览器替代停启。
- `.tools/a22-restored-verify.log`、`a22-restored-resume.log`：全库dump与独立资产
  副本恢复到新PG进程/数据库及新Java/网页，核完整快照后独立续办保存治疗一次，
  HP封顶12、tempHP1/资源/原骰保持，死豁归零、原昏迷解除，旧回执无覆盖。
- `.tools/a22-source-independent.log`：恢复库续办未改原库；`.tools/a22-resume.log`
  原库在真停启后也独立续办一次。所有新服务最终按身份停止，新数据目录保留。
- 清DB环境后`.tools/a22-final-offline.log`再次253unit通过，无数据库/网络依赖。

固定验收jar SHA256：`544FBF49C3D6E18F0D9EAD680E4F3BD68A8B317AA3BE7BFC325720AB9DA6DD1C`。

最终审计见仓库外`.tools/a22-audit.log`：263入口文件中251逐字节不变、12项预期
编辑，新增17文件；全部旧后端/迁移/云CI字节保持，历史文档原文保留。暂存区
为空、分支与HEAD不变，无提交/推送/合并/发布或PR变更。
`git diff --check`仅有入口已有continuation/verification末尾空行，保持原内容；
新增文件无空白错误。M3总联验、云端及物理真机仍未完成。
