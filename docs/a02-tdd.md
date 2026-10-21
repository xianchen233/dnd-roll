# A02 实际开发与验收（2026-10-22）

沿用 `feat/accounts-and-invitations` / 草稿 PR #1，不合并、不发布。
契约 [module-lifecycle.md](../contracts/module-lifecycle.md)。云端联验与真实手机继续后置。

## 开工基线

已先阅读 AGENTS、continuation、verification、产品设计、实施路线、CONTEXT、
contracts/README、A01契约及生命周期决策，fetch 后本地/origin/PR均为
8322dce08c82baa15c62dde3fd85ea0658406d92，工作区干净，PR open/draft/main。
通过 GitHub 连接器重新获取 push39653418130 / PR39653430398 六份完整日志，
独立断言两组109unit+124真实IT零失败/错误/跳过、234前端/构建、91契约、49浏览器、
43原PASS/4MODULE VERIFIED、十一组容器重启、隔离新项目/新卷恢复、两次完整团比较、
恢复后三流程和9会话/100棋子/72消息errors=0。新证据保存在仓库外.tools：
a02-entry-ci-verified.json、a02-entry-job-{jobId}.log，不沿用旧代理的通过结论。

## 实际 RED → GREEN

1. `a02-isolation-red.log`：两条真实HTTP测试分别在缺少
   `/module-customizations` 的404处失败（2fail/0error/0skip）。随后实现独立副本、
   typed定义、创建新团、进度和另存；`a02-lifecycle-green-first.log`两条通过。
   已确认两团角色/场景不同ID，HP与资源/剧情进度互不影响，原件/基线/进行中状态保持。
   后续补入真实OOC旧记录与其逐字段保持断言。
2. `a02-v2-red.log`：新v2有明确play的归档被拒，1error（旧7项通过）。
   增加严格v2定义与引用/字段/边界校验后，同原7项全部通过。
3. `a02-frontend-red.log`：六项新中文生命周期UI测试因缺入口失败；
   `a02-frontend-green.log`六项通过。覆盖编辑、另存、刷新保留草稿、版本冲突、
   原命令重试、切团/迟到响应、撤权清理、进度独立版本与不进入模板。
4. `a02-entry-ui-red.log`：原模组库缺生命周期入口，1fail+7pass；
   接入后`a02-frontend-focused.log`两文件14pass。
5. `a02-contract-red.log`：缺A02 GET路由；增加8个操作及严格typed schemas，
   `a02-contract-green.log`99操作通过。v1仍受支持，显式允许v2，v3拒绝。
6. `a02-boundaries-red-behavior.log`：重复JSON键被静默接受（200而非400），
   新私密路由缓存头未符合约定（5项中2fail）。启用HTTP重复键拒绝、覆盖新私密路由。
7. `a02-boundaries-green.log`：实际延迟提交失败后新图片未清除。
   Spring在部分提交错误时回报UNKNOWN；改为独立事务持有原source锁，等待原事务结束，
   重读地图/笔记图片元数据，确认未提交才清理；无法确定则保留不可公开的opaque文件。
   `a02-boundaries-green-final.log`12unit+7真实HTTP测试全部通过，零失败/错误/跳过。
   含并发同请求/版本竞争、当前源与目标DM撤权、缺依赖/旧版、图片/公开笔记/
   自定义依赖重映射/同角色多棋子、导出typed回读、即时及延迟提交回滚和原请求重试。
8. `a02-detail-race-red.log`：显式重载详情期间仍能保存旧详情，1fail+6pass；
   重载期间禁用编辑/保存后`a02-detail-race-green.log`7pass。
9. 检查发现v1内容已满1MiB时，计算新增空play会缩小旧版允许范围。
   首个新夹具剩余文本超出单节点限额，属于准备错误；修正夹具后在未修复的
   计数行为上重新执行`a02-v1-limit-red-behavior.log`，实际1条INVALID_MODULE错误；
   恢复修复后完整115unit+132真实IT零失败/错误/跳过（a02-backend-final.log）。
   同时追加v1描述文档五图兼容回归；v2的可玩笔记仍使用现有四图上限。
   `a02-v1-images-red.log`实际INVALID_MODULE→最终模块14unit+12真实IT通过
   （a02-module-final.log，含原A01五条与新A02七条）。
10. 显式选团暴露Tabletop在切域的一帧内用新campaignId加旧sceneId读取fog/map，
    服务器安全404，但浏览器错误日志不得忽略。新增`a02-scope-race-red.log`聚焦
    测试实际1fail+2pass；Tabletop只投影campaignId匹配的scene，再允许地图/控制读取。
    原全量失败日志保留，不放宽错误收集或重试。

## 准备错误与测试修正（不冒充行为RED）

- 新Java测试曾用不允许的复合`var`声明；首次修复命令工作目录重复backend，
  无文件改动；修正声明和路径后才取得第6项的真实行为失败。
- 新前端测试ApiError构造参数及Testing Library查询选项不符实际类型；运行时测试
  已通过但生产构建报告类型错误，修正后`a02-build-fixed.log`通过。
- 带地图JSON的断言用原始整数5直接比较typed输出double5.0；改为比较typed文档。
- 单元边界测试误把原本正确的sceneId当成坏引用；修正为错误种类ID，未放宽验证。
- 一次PowerShell替换字符串转义、一次apply_patch同文件删除/新增形式被拒，
  均无目标文件变更；修正命令后继续。
- 新生产浏览器最初两条用嵌套textarea的精确label查找失败，详情实际已读取；
  添加稳定aria-label后通过。重试流程随后在尚未完成切团时读取旧选择值，
  改为等待服务端返回的唯一新团ID进入选择器，未放宽超时或增加retry。
- 首个重启seed给创建战役发送多余expectedVersion（已在执行前修正）；随后
  在邀请玩家后仍用0作为OOC版本收到409，改为读取当前版本。新前缀重seed通过。
- 完整浏览器首轮49pass/2fail：原scene/tabletop测试取排序第一的战役，A02新团
  排在演示团前。改为显式选择原稳定演示团ID，保留全部原断言/60秒/零retry。
  第二轮50pass/1fail：tabletop刷新页面后又回到默认团；刷新后同样显式选择原团，
  继续核对服务器持久坐标，不改变业务实现或等待上限。
- 切域修复后的全量前端中，原角色卡断网测试一次性mock可能被保存后的后台读取
  消费，手动刷新便落回默认成功。改为模拟断网期间所有详情读取失败，保留原请求
  重试及刷新失败断言；未放宽等待上限，也不修改角色卡业务行为。

## 当前验收

完整前端最终243项与生产构建通过（a02-frontend-final-scope.log / a02-build-final-scope.log）。
四条新旧聚焦Chrome最终10.1s通过（a02-browser-fixture-green.log），错误日志为空。
完整后端115+132、最终模块14+12、99契约及旧操作/schema保持均通过。
全量51浏览器最终复验、服务重启、备份、无数据库116unit和精确提交双CI正在按序验收。
未通过前不关闭A02或整体M3。后续A03–A06仍按work-packages保留。
