# 2026-10-26 UI 概念适配（2026-10-31）

将概念目录的最新 v5 登录整体门扉构图及 v4 桌面材质适配到现有 React/PixiJS。
概念目录只读，资源复制到 frontend/public；真实服务器数据、权限过滤、移动确认、
工作区/手机导航及请求身份保持。概念中的示例角色、地图、HP 和虚构在线状态不移入产品。

界面配色是浏览器本地偏好，只接受黑曜石/深林/月夜/余烬/羊皮纸五个已知值。
损坏或不可用的 localStorage 回落黑曜石，存储失败不阻断切换，不写任何战役数据。
配色不会卸载工作区或改动待确认请求。使用原有中文标签与键盘焦点样式；390px
保留原手机导航，所有表单继续可用，不以背景装饰覆盖真实地图。

## 实际 TDD 与保护检查

先写本文件的配色偏好约定，新增可编译 Appearance 静态控件与两条行为断言。
`.tools/a24c-ui-red.log` 实际两项断言失败：配色不更新根节点、不保存偏好，
不是准备或编译错误。实现有效值恢复、根节点配色及容错存储后，同两项通过
（`.tools/a24c-ui-green.log`）。完整离线前端 **58 文件 / 437 测试通过**
（`.tools/a24c-ui-full.log`），最终 TypeScript/production build 通过
（`.tools/a24c-ui-build-final.log`）。既有约855 kB主包提示保持，不算构建失败。

原 clean 入口 App.tsx、LoginForm.tsx、main.tsx、styles.css 修改前已复制到
`.tools/a24c-ui-entry` 并记录 manifest；实际只改 App/main，LoginForm/styles保持。
79 份入口已有未提交 frontend 文件逐字节保持，见 `.tools/a24c-ui-preservation.json`。
两张门画按原字节复制，不修改演示目录，其 SHA256 为：

- dragon-gate-obsidian-v4.png：`F1B8BCD2DAA3C877024AF857918083E0B80BE8FAEA2D1FD7826AD0460E824382`
- dragon-gate-parchment-v4.png：`CDB81D906F631106862054891AEA2B54BD7A2D58985B312079FBFA82CC4B90A0`

以上两个 hash 已与演示目录源图比较相同。未对整个演示目录建立开工前 hash，
因此不把“操作全程只读演示目录”写成全目录前后摘要验证。无暂存/提交/推送/PR变更。

## 真实界面验收

使用主线新 PG18514/dnd_a24c_browser、Java18177、production preview18178，
没有复用旧服务或 dnd-roll-local。首次完整真实 Chrome **77 passed（3.8m）**，
见 `.tools/a24c-chrome-full.log`。此轮期间有纯 CSS 的对比修复；不把它作为最终
固定构建的独立证据。再次回归因最终视觉复核发现羊皮纸状态文字对比不足而主动
中断，记为 **interrupted**，既不算失败也不算通过。

最终修复后固定14文件production build摘要于 `.tools/a24c-ui-final-build.json`，
最终完整77真实Chrome回归的日志为 `.tools/a24c-ui-fixed-chrome.log`：**77 passed（4.0m），
零retry**。回归后逐文件复核14文件摘要全部保持，见
`.tools/a24c-ui-final-build-verified.json`。此轮才是最终固定构建的实际浏览器证据，
没有把旧A24a的77条移记为本轮执行。

`.tools/a24c-ui-visual/results.json` 保存1440与390px、黑曜石/羊皮纸、登录与真实
桌面的8张截图及宽度检查，全部无文档横向溢出；已人工查看登录桌面/手机和桌面
两配色。额外3张视口截图也已查看。`.tools/a24c-ui-visual/theme-sweep.json` 实际
五配色切换均保留所选真实单位和移动控件。附加截图脚本复跑曾默认选到测试新增的
战役而找不到Adventurer，显式选择First campaign后通过；这属于夹具定位准备问题，
不是产品RED。外置 npm/样式命令几次工作目录错误已纠正，也不计行为RED。

本代理未启动/停止独立服务，由主线在整体验收后统一核身份停机并保留数据。
云端、物理手机及精确交付HEAD CI未执行，截图390px不替代真机。
