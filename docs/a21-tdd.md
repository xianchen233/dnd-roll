# A21 原A19再次稳定后首次受伤：实际测试记录

2026-10-29，未提交。按AGENTS读取最新交接/验收/设计/计划/CONTEXT/契约后，
检查原分支feat/accounts-and-invitations、HEAD c034aa4及空暂存区。246份入口
未提交改动与云补充逐字节备份于仓库外`.tools/a21-entry`及manifest。
不暂存/提交/推送/合并/发布，不操作dnd-roll-local及数据或旧fence。

先[dnd-rule-lookup来源/矩阵](research/restabilization-damage-5r.md)→
[独立契约](../contracts/restabilization-damage-effects.md)/OpenAPI→实际RED。
A库只定位，B完整回读0HP与临时生命页，复用本会话直接取得的官方SRD5.2.1，
重新读取p18；未读取商业正文、未混用2014/扩展。正伤害即使被tempHP全吸收
也失稳并记失败。再次稳定前旧失败已归零，普通1/重击2；最终伤害不再倍增。

## 实际 RED / GREEN

- `.tools/a21-domain-red.log`：245离线unit中新增8项7断言失败，0错误/跳过；
  空实现不能生成效果、人工原因和输入拒绝。这是真实行为RED。
- `.tools/a21-http-red.log`：245unit全部通过，10真实PG/HTTP测试因新路由404
  失败，0错误/跳过；没有把未执行或编译失败当RED。
- `.tools/a21-ui-red.log`：7界面项因缺少实际控件失败。
- `.tools/a21-old-controls-red.log`：8项通过、1项失败；新共享状态下旧A16仍显示
  Help按钮。添加准确状态guard及历史提示后，`.tools/a21-ui-green.log`9项通过。
- `.tools/a21-http-green.log`：245unit与10真实HTTP项通过，0失败/错误/跳过。
- `.tools/a21-frontend-full.log`：53文件419测试通过；`.tools/a21-build.log`构建
  成功，仅既有大于500KB分块提示。
- 检查响应发现新效果schema误继承治疗的HP>=1/失败0/吸收量>=1约束。
  先增加合法HP0/吸收0/失败1、重击失败2及manual/非法值验证，
  `.tools/a21-contract-red.log`实际断言失败，再修正为伤害数值约束。
  `.tools/a21-contract.log`171operations通过；成功输出移到全部检查之后。
- `.tools/a21-browser.log`：新增生产构建Chrome定向1项首次通过，实际提交后
  丢响应、精确重试只应用一次；HP0/tempHP1、死豁0/2，只有新稳定记录移除，
  原昏迷对象逐字保持，资源/施救者/原骰不变。另存普通伤害方案用于停启续办。
  桌面/390px截图已查看，无横向溢出；不是物理真机。

## 实现与事务覆盖

独立领域/应用/控制器/JDBC，完整重建A16→原A18伤害→A19新Help与应用，要求
原A19目标结果完整未改、完成证据与服务器骰绑定。使用保存的两次施救者快照；
施救者后续编辑不影响已应用来源的本次受伤资格，也不修改施救者或原骰。
三版本/尝试/hash绑定，严格最终金额和critical/reviewed，预览不写不掷骰。

仅0<amount<maxHP有候选，HP仍0，先扣tempHP，成功0/失败普通1或重击2，只移除
a19-stable，保留确切a16-unconscious。零伤害/达到maxHP均人工；没有死亡、
连续伤害、重击/防御自动判定、死豁掷骰、治疗/新Help/资源/完整状态引擎。

应用原子写完整卡与安全角色历史、A19 UNSTABLE、共享RESTABILIZATION_UNSTABLE、
两本历史、私有伤害日志与回执。旧A16/A18/A19/A20入口边界保持，新状态不误入
旧Help。私有理由/金额/critical/审阅不进玩家历史。当前DM/目标存在先于旧回执，
精确旧命令不改后续游玩，不写历史或发事件。

10真实HTTP项覆盖合法效果、后续施救者编辑、上限/零/普通/重击、两次历史骰保持、
严格字段/null/重复/类型、玩家/匿名/CSRF/当前撤权/no-store、复制/跨战役/原日志
篡改、三版本/尝试/hash/原目标/导入审核、精确回执和跨路由冲突、同键异键并发
单次应用、立即/延迟触发器失败整笔回滚。回滚核完整卡/角色历史/双状态/双历史/
新日志/回执。后续旧Help与旧治疗入口拒绝。

V32只新增私有伤害日志，FK绑定确切A19及共享结果历史版本。独立测试PG18496、
Java18161/网页18162，恢复库18498/18163/18164；全部新数据均在仓库外。
两套六服务最终均按身份停止、数据保留；完成结果如下。

M3仍未关闭：完整P09、含新绑定的战役ID重映射恢复、容量和精确交付HEAD日志
待办；云端/物理真机后置，没有远端新HEAD CI或发布结论。

## 最终本地验收

- `.tools/a21-backend-full.log`：245离线unit与342完整真实PG/HTTP/WS IT通过，
  0失败/错误/跳过，7分57秒。含新增迁移1项和HTTP10项。
- 新迁移测试先V31种A16/A17/A18/A19/A20历史，旧所有表的列/行与31个checksum
  升级后保持，新日志错误A19/共享版本被FK拒绝、正确绑定允许。
- `.tools/a21-frontend-full.log`：53文件419测试；构建成功。契约171operations，
  原168个操作与旧schemas保持，只有A19/共享状态enum各增加一个新终态。
- `.tools/a21-chrome-final.log`：75完整Chrome零retry，3.6分钟；生产定向与
  完整浏览器均使用最终固定jar，源码没有在验收后改动行为。
- `.tools/a21-before-restart.log`、`a21-after-restart.log`：完整卡/角色历史/
  双状态/双历史/两份伤害日志/骰/全部原回执深比较，保存方案hash保持。
  Java/网页/PG先核PID命令身份、确认端口无监听，再用同一jar/数据真正重启。
- `.tools/a21-restored-verify.log`、`a21-restored-resume.log`：全库dump和独立
  资产副本恢复到新PG进程/数据库与新Java/网页，核对全部快照后独立应用保存的
  普通伤害方案一次：HP0/tempHP3、死豁0/1、只删本次稳定且原昏迷精确保持。
- `.tools/a21-source-independent.log`：恢复库续办没有改变原库；
  `.tools/a21-resume.log`：原库在真正停启后也独立续办一次，精确旧回执无重复效果。
- 所有新服务最终按身份停止、数据目录保留；清DB环境后
  `.tools/a21-final-offline.log`再次245unit通过，无数据库/网络依赖。

固定验收jar SHA256：`F314858C6AADA78AA499FDD8FBAD11D0B25607255332D3E57802351492D75A65`。

最终保留审计在仓库外`.tools/a21-audit.log`：246入口文件中230逐字节不变，
16项预期编辑，新增17文件；全部旧后端/迁移/云CI字节保持、历史文档原文保留。
暂存区为空，原分支/HEAD不变，无提交/推送/合并/发布或PR变更。
`git diff --check`仅有入口已有continuation/verification末尾空行，保持原内容；
新增文件无空白错误。M3总联验、云端与物理真机仍未完成。
