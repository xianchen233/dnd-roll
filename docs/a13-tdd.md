# A13 治疗恢复生命并解除击昏记录：实际执行记录

2026-10-27，未提交。原分支 `feat/accounts-and-invitations`，HEAD
`c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。132份入口改动逐字节备份至仓库外
`D:/workspace/dnd/.tools/a13-entry`；不操作 dnd-roll-local、数据或旧fence。
GitHub只读重试已确认PR #1仍open/draft、未合并且head相同；首次读取传输失败不算状态证据。

先按AGENTS读交接/Git、设计/计划/CONTEXT/契约入口，再查
[5R来源与支持矩阵](research/knockout-healing-5r.md)，落
[独立契约](../contracts/knockout-healing-effects.md)，随后实际执行RED。

## 实际 RED

- `a13-red.log`：188既有离线unit通过；新增6真实PG/HTTP测试全部实际失败于
  `/knockout-healing-effect` 404。新PG18470和新库，不复用旧验收数据。
- `a13-ui-red.log`：4新增交互测试实际失败于缺少治疗控件。
- `a13-contract-red.log`：缺少A13入口断言实际失败。
- 补入响应示例后 `a13-contract-fixture-red.log` 揭示现有轻量checker不接受
  `type:[integer,null]`；改用既有`anyOf`词汇后候选/人工响应及非法请求示例通过。
- 准备阶段曾有PowerShell文件列表花括号语法错误；未执行写操作，随后改为显式路径。
  该准备失败不冒充行为RED。

## 实现与首轮 GREEN

只接受A11/A12规范注记、已应用来源且目标卡仍为应用后版本；原确认中保留目标
版本，APPLY预览事件本身changes为空，不能拿它推断来源。即使手动重建相同卡也
回退人工。生命1/tempHP0/无死豁/唯一规范记录、DM审阅与实际正恢复量均为服务器门槛。
生命封顶；临时生命不算治疗；短休、急救、资源与倒地等仍人工。

读方案无写入；执行在actor/campaign锁下重算并比对方案hash。目标卡一次增版、
完整before/after历史、HEAL_KNOCKOUT事件、预览增版和receipt同事务保存；无迁移。
原骰、行动、证据与hash保持。权限与对象存在性在回执前检查，确切重试在当前版本
门槛前返回原ack，不覆盖后续游戏。一般加血、编辑和CORRECT不推断解除。

- `a13-focused-green.log`：191离线unit及6真实PG/HTTP测试通过，无失败/跳过。
  覆盖两个来源、封顶、无写预览、字段严格性/CSRF/玩家拒绝、未审阅、满HP、
  卡修改/重建/临时生命/0HP回退、旧回执/撤权、同键与双DM并发、即时及提交时故障回滚。
- 4交互测试通过：现卡读取、显式预览再执行、不发送最终值、未知结果精确重试、
  编辑/版本使方案失效、人工回退、迟到结果/403清私密状态、已知409只允许重读。
- `a13-frontend-full.log`：39文件358前端测试通过；生产构建通过。
- `a13-contract-green.log`：144 HTTP operations，含新请求/响应及非法客户端最终值检查。

完整真实IT、完整生产Chrome、真实停启/继续与停库离线结果在下面追加；在实际结束前
不视为通过。本记录不代表M3最终恢复/容量、云端或真机验收完成。

## 最终实际结果

- `a13-full.log`：191unit、269完整真实PG/HTTP/WS IT，全部0失败/错误/跳过。
- `a13-browser.log`：67完整生产Chrome通过，retries=0，未改既有超时/断言/配置。
  新浏览器流程覆盖短棒和匕首、只读预览、真实提交后丢响应、同请求重试单效、
  后续恢复注记不被旧回执移除、原行动/骰子/证据不变、玩家无法读私有方案。
- `a13-before-restart.log` / `a13-after-restart.log`：新会话读回及旧回执重放通过。
  中间按进程命令行与数据目录核对身份后真正停止Java、网页、PG，再启动同一产物和
  独立数据。三端口停止状态有检查，不是仅浏览器刷新。
- `a13-resume.log`：停启前读取的治疗方案hash仍匹配；恢复后执行一次，从1HP到8HP
  并移除原注记。版本只增1，历史回执/骰子/源卡/原行动保持。完整证据
  `a13-browser-final-evidence.json` 与 `.resumed.json` 在仓库外。
- 最终验收JAR SHA256：
  `654a2e01b7034ef64d905b085069f0d1ef3be7ee156a6f99cfddc0e13b22011e`。
- 已查看桌面及390px截图，治疗控件/边界说明可读；这不是真实手机验收。
- `a13-stopped-processes.json`：最终PG18470/Java18135/网页18136按身份停止，数据保留。
  `a13-final-offline.log`：清数据库环境且停库后191离线unit再次通过。
- 132入口文件仍全部存在且备份hash一致；OpenAPI仅新增2路由、3schema、1响应事件，
  其余旧结构逐项相同。CI/cloud补充未改。六份交接/设计文档原字节完整保留为后缀。
  `git diff --check`仅报入口已存在的continuation/verification文件尾空行，不清理旧内容。

本轮未暂存/提交/推送/合并/发布；原分支/草稿PR/HEAD保持，无新HEAD远端CI。
M3未关闭；下一具体效果及完整恢复/容量/云端/真机后置见[m3-remaining.md](m3-remaining.md)。
