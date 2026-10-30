# A20 原A19再次稳定后的治疗：实际测试记录

2026-10-29，未提交。当前分支feat/accounts-and-invitations、HEAD c034aa4、
草稿PR #1沿用。按AGENTS读取交接/验收/设计/计划/CONTEXT/契约后推进。
229份入口改动及云补充逐字节备份于仓库外`.tools/a20-entry`及manifest。
不暂存/提交/推送/合并/发布，不操作dnd-roll-local或任何旧数据/fence。

先[dnd-rule-lookup查证/矩阵](research/restabilization-healing-5r.md)，再
[独立契约](../contracts/restabilization-healing-effects.md)、OpenAPI及实际RED。
A商业书只定位，B完整回读“生命值降至0点”“治疗”“临时生命值”。在线工具
429后直接下载官方SRD5.2.1并读取PDF pp17–18，与本地译本核对。未使用2014规则。

## RED / GREEN

- `.tools/a20-domain-red.log`：237离线unit中新增8项有7断言失败，0错误/跳过。
  空实现不能产生可执行治疗/原记录移除/非法输入/人工原因，是真实行为RED。
- 首次领域GREEN暴露测试夹具仍用死豁成功1：A19要求失稳成功0。修正夹具后
  `.tools/a20-http-red-behavior.log`先237离线unit全部通过，再7真实HTTP测试
  因新路由404失败，0错误/跳过。更早`a20-http-red.log`被夹具失败挡住，不能算HTTP RED。
- `.tools/a20-ui-red.log`：新增6项因缺失目标/审阅/预览控件真实失败。
- 接路由时生成器引起的重复Re类名/引用和遗漏构造依赖属于编译装配错误，已修正，
  不算行为RED。`.tools/a20-http-green.log`：237unit+9真实PG/HTTP通过，0失败/错误/跳过。
- `.tools/a20-ui-green.log`：6界面+1API通过；`.tools/a20-frontend-full.log`：
  51文件410项通过；`.tools/a20-build.log`构建成功，只有既有大于500KB分块提示。
- 契约168个operations通过；旧165个操作保持，只给A19状态enum增加HEALED。
- `.tools/a20-browser.log`：新增生产构建Chrome定向1项首次通过，实际提交丢响应
  后精确重试只应用一次；来源双记录准确移除，HP3/tempHP4、资源/施救者/骰保持。
  第二目标的治疗方案保存用于停启。桌面/390px截图已查看，无横向溢出；不是物理真机。

## 有界实现与测试覆盖

独立`RestabilizationHealingEffect/Service/Controller/JdbcStore`，未放宽A17/A18。
重建A16原检定/应用→A18精确伤害日志→A19新检定/应用，绑定双方账本和当前完整卡。
原/新施救者后续编辑不影响已应用来源。当前卡任何后续改变、伪造卡、未审阅、
导入审核、旧来源和非规范状态均不能应用；三版本/尝试/hash守卫独立检查。

只应用DM裁定真实治疗：HP上限封顶、tempHP/资源/原骰保持，原a19-stable与原
a16-unconscious同事务移除，死豁0/0。A19与共享A16状态各推进一次HEALED，
原证据不覆盖。固定安全角色历史不泄漏私有理由/审阅/请求量。

9真实HTTP项覆盖合法/上限、精确旧回执保持后续游玩/跨路由冲突、玩家/匿名/
当前撤权/CSRF/私有no-store、严格字段/null/重复/类型、来源/卡/导入/版本/hash、
同键与异键并发单次应用、立即和延迟触发器失败整笔回滚、旧入口/复制/跨战役门、
篡改原伤害日志拒绝且不写。回滚同时核卡/角色历史/共享状态/双历史/回执/新日志。

V31只新增私有治疗日志，FK同时绑定确切A19历史结果版本及共享A16结果版本；
旧迁移保持。独立PG18492、Java18157/网页18158；恢复库18494/18159/18160。
两套服务均按身份停止，数据保留；完整结果如下。

M3未关闭。完整P09清单、战役ID重映射（含新绑定）、容量、精确交付HEAD日志仍待。
云端/物理真机后置，无新HEAD远端CI或发布验收结论。

## 最终本地验收

- `.tools/a20-backend-full.log`：237离线unit及331完整真实PG/HTTP/WS IT全部通过，
  0失败/错误/跳过，耗时7分48秒。包含新增迁移1项和HTTP9项。
- 新迁移测试先V30种旧A16/A17/A18/A19数据，核对旧所有表的列/行及30个checksum，
  迁移后不变；新日志错误A19版本/错误共享版本FK拒绝，正确版本可写。
- `.tools/a20-contract.log`：168operations；`.tools/a20-chrome-final.log`：74项
  完整Chrome通过，零retry，3.6分钟。定向截图及最终截图都在仓库外。
- `.tools/a20-before-restart.log`、`a20-after-restart.log`：新会话完整快照与
  所有旧命令ack深比较、旧回执无覆盖、保存方案hash不变。停止Java/网页/PG前
  先核PID命令身份，端口全部无监听后重启同一固定jar和原PG数据。
- `.tools/a20-restored-verify.log`、`a20-restored-resume.log`：全库dump与独立
  资产副本恢复到新PG进程/数据库、Java/网页，核对快照后将保存治疗方案应用一次，
  回执重试不重复，原证据/资源/tempHP/骰保持。
- `.tools/a20-source-independent.log`证明恢复库续办未改原库；
  `.tools/a20-resume.log`证明原库在真正停启后也可独立续办同一保存方案一次。
- 两套六个服务按身份停止，所有新数据目录保留；清数据库环境后
  `.tools/a20-final-offline.log`再次237unit通过，无数据库/网络依赖。
- 固定验收jar SHA256：`D65DF47AF767C35389ADD18BAB9E25E28D9C74EB51328ADEA27C2EB9721ED0BA`。

最初停启核对脚本在完整Chrome生成最终证据文件前被调用，返回ENOENT，未作写入；
证据生成后重跑通过。这不是行为RED或停启失败。外层脚本工作目录变更造成一次
相对路径读取失败，改用绝对路径后完成恢复，不影响产品或测试数据。

来源与支持矩阵、契约、实际RED/GREEN、最终边界均已更新。全部既有改动与云补充
保留，旧后端字节不变，旧165个操作保持；A19状态enum仅增加HEALED。最终审计
见仓库外`.tools/a20-audit.log`：229入口文件中215逐字节不变、14项预期编辑，
新增17文件；所有旧后端/迁移/云CI字节保持，历史文档原文保留，暂存区为空，
分支与HEAD不变。`git diff --check`仅报入口已存在的continuation/verification
末尾空行，保留以免改写既有内容；新增文件无空白错误。
M3总联验、云端和物理真机仍未完成。
