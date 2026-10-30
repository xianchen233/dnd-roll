# A23 原A18失稳后的直接治疗：实际测试记录

2026-10-29，未提交。按AGENTS读取最新交接/验收/设计/计划/CONTEXT/契约、检查
原分支feat/accounts-and-invitations、HEAD c034aa4和空暂存区。280份入口未提交
改动与云补充逐字节备份于仓库外`.tools/a23-entry`和manifest。无提交/推送/合并/
发布或PR变更，不操作dnd-roll-local及数据/旧fence。

先[dnd-rule-lookup来源/矩阵](research/initial-instability-healing-5r.md)→
[独立契约](../contracts/initial-instability-healing-effects.md)/OpenAPI→实际RED。
A只定位商业书，B完整回读治疗/0HP/临时生命页，复用本会话直接获取并核验的官方
SRD5.2.1 pp17–18；无商业正文、无2014混用，也不声称重新检查在线最新勘误。

## 实际 RED / GREEN

- `.tools/a23-domain-red.log`：261离线unit中新增8项7断言失败，0错误/跳过。
  空实现不能产生治疗/清零/准确移除/人工原因或非法输入拒绝，是实际行为RED。
- `.tools/a23-http-red.log`：261unit全部通过，10真实PG/HTTP项因新路由404失败，
  0错误/跳过，未把编译失败或未执行测试算RED。
- `.tools/a23-ui-red.log`：7界面项因缺少实际目标和操作控件失败。
- `.tools/a23-http-green.log`：261unit+10真实HTTP通过，0失败/错误/跳过。
- `.tools/a23-ui-green.log`7界面+1API通过；`.tools/a23-frontend-full.log`57文件
  435测试通过，构建成功，仅既有大于500KB分块提示。
- `.tools/a23-contract.log`177operations通过：必填字段、非法值、拒绝客户端
  originVersion/damageVersion/最终值、合法治疗响应；旧174操作和所有旧schema保持。

## 实现与支持边界

独立InitialInstabilityHealing领域/应用/控制器/JDBC，不放宽A17/A20/A22。
仅共享账本最新UNSTABLE与原A18完整结果未改：前一APPLIED和只变状态的原尝试
核对，重建A16原服务器检定/稳定应用，再核原A18方案hash/伤害与前后完整卡。
新方案绑定整个原A18日志、当前完整卡/状态/两版本/尝试与审阅。原施救者后续编辑
不影响保存的历史来源，当前目标任何后续编辑则人工。

HP0/maxHP>0、死豁0/1或0/2、唯一原a16-unconscious，无导入审核/修正器/覆盖；
DM裁定真实治疗后预览再确认：HP上限封顶、死豁0/0，只移除原昏迷，临时生命/
资源/原骰/施救者保持。已移除的原稳定不再次删除，无治疗骰/资源/动作/状态引擎。

应用原子写卡/安全角色历史/共享HEALED状态与历史/私有治疗日志/回执。当前DM/
目标存在先于精确旧回执，不覆盖后续游玩；玩家历史不含私有理由/审阅/请求量。
若原A18结果仍未改但已有未应用的新Help，独立A19账本/历史/骰逐字保持；推进父
版本使旧检定不能继续应用，精确旧检定回执仍可返回，且不重掷或改卡。

10真实HTTP项覆盖治疗上限/原记录解除/死豁归零/临时生命与原骰保持、施救者后续
编辑、角色控制玩家/匿名/CSRF/撤权/no-store与安全历史、严格字段/null/重复/类型、
版本/尝试/hash/完整卡/导入审核、精确重试与跨路由冲突、同键异键并发单效、立即/
延迟触发器对卡/角色历史/账本/历史/新旧日志/回执整体回滚；普通1与重击2失败的
治疗均通过，未应用A19证据保留且旧预览409；原A18hash/版本/critical篡改拒绝，
预览后原日志理由变化使保存方案失效；复制卡/跨战役/已再次稳定拒绝，删目标后
旧回执404。

V34新增私有治疗日志，FK绑定原A18源版本与共享结果历史版本。独立PG18504、
Java18169/网页18170，恢复库18506/18171/18172；所有新运行时/凭据/数据/证据在
仓库外。两套六服务均按身份停止、数据保留，最终结果如下。

M3未关闭。完整P09清单、含新绑定的独立战役ID重映射、容量、精确交付HEAD日志
仍待；云端/物理真机后置，无远端新HEAD CI或发布结论。

## 最终本地验收

- `.tools/a23-backend-full.log`261离线unit与365完整真实PG/HTTP/WS IT全部通过，
  0失败/错误/跳过；包含新增迁移1项和HTTP10项。
- V34迁移测试先V33种旧A16–A22历史，旧所有表列/行与33个checksum保持；新日志
  错误A18源版本或结果历史版本被FK拒绝，正确绑定可写。
- `.tools/a23-frontend-full.log`57文件435测试、构建通过；契约177operations，
  旧174个操作及所有旧schema保持，仅新增3操作/5schemas，无旧状态enum变更。
- `.tools/a23-browser.log`新增生产Chrome定向首次通过：A16→A18结果、新的未
  应用A19 Help证据保存后，实际治疗提交丢响应，精确重试仅应用一次；HP3/tempHP4、
  死豁0/0，原昏迷移除，旧资源/施救者/骰保持，保留另一目标和上限治疗方案。
  桌面/390px截图已查看，无横向溢出，软件视口不是物理真机。
- `.tools/a23-chrome-final.log`77完整Chrome零retry，使用固定验收jar。
- `.tools/a23-before-restart.log`、`a23-after-restart.log`：完整卡/角色历史/共享
  状态与历史/A18伤害/新治疗/原骰/未应用A19账本与历史/所有旧回执深比较，保存
  方案hash保持。Java/网页/PG核PID命令身份、端口无监听后，以同一jar/数据真正重启。
- `.tools/a23-restored-verify.log`、`a23-restored-resume.log`：全库dump与独立资产
  副本恢复到新PG进程/数据库与新Java/网页，核完整快照后保存方案独立续办一次：
  HP封顶12/tempHP4、死豁0/0、原昏迷解除，旧骰/资源/施救者和A19证据保持。
- `.tools/a23-source-independent.log`：恢复库续办不改原库；`.tools/a23-resume.log`
  原库在真正停启后也独立续办一次，旧回执不覆盖后续游玩。全部新服务最终按身份
  停止，数据目录保留。停库清DB环境后`.tools/a23-final-offline.log`再次261unit通过。

固定验收jar SHA256：`E3478066289F11F960016AF13F517F1C5A2FADD731104065BE70FC03347E320B`。

最终审计`.tools/a23-audit.log`：280入口文件中268逐字节不变、12项预期编辑，新增
17文件。旧后端/迁移/云CI保持原字节，历史文档原文保留，暂存区为空，分支与HEAD
不变。外部审计生成脚本曾有一处多余加号的语法错误，未执行产品或数据写入，修正
后审计通过，不计行为RED。`git diff --check`只有入口已有continuation/verification
末尾空行，保留原内容；新增文件无空白错误。

下一步转M3恢复总清单和独立战役重映射，先来源绑定/支持缺口矩阵，再契约和实际
RED。全部未完效果保留；M3总联验、云端/物理真机与发布验收仍未完成。
