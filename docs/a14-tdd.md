# A14 原击昏短休完成解除：实际TDD记录

2026-10-27，未提交。原分支 `feat/accounts-and-invitations`，HEAD
`c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。按AGENTS读最新交接/Git及设计、计划、
CONTEXT和契约入口；A07–A13不重复。141份入口文件逐字节备份到仓库外
`D:/workspace/dnd/.tools/a14-entry`。全部既有改动与云补充保留。
只读检查确认远端分支同HEAD；PR #1仍open/draft、未合并且head相同，未改PR元数据。

先[5R来源与矩阵](research/knockout-rest-5r.md)，再[独立契约](../contracts/knockout-rest-effects.md)，
随后写测试并实际执行RED。A库仅定位，B库全文回读，官方SRD核对；不读取商业正文。

## 实际 RED

- `a14-red.log`：191既有离线unit通过，8新增真实PG/HTTP测试全部实际失败于新方案
  路由404；不是未执行就标RED。使用新PG18472及新数据目录/测试库。
- `a14-ui-red.log`：4交互测试实际失败于缺少短休输入与解除控件。
- `a14-contract-red.log`：缺少A14路由断言实际失败。

## 实现与首轮 GREEN

DM声明的是从这次击昏开始的连续游戏内分钟数、是否中断及例外审阅。
服务器检查>=60分钟、未中断、已审阅及A11/A12规范来源/目标原版本；不推断现实
时间或事件时间轴，不支持中断后重新开始或长休折算。满生命也可完成此路径。
只移除原击昏记录，生命/临时生命/资源等整张卡其余字段保持，醒来仍倒地。

抽取原击昏来源校验供A13/A14共用，保留A13请求、响应与算法；两种解除共享
预览/卡版本与锁，不能都写成功。独立执行在actor/campaign事务内保存一次卡增版、
完整before/after KNOCKOUT_REST历史、END_KNOCKOUT_REST事件、预览增版、回执；
原骰/行动/证据/hash保持，无迁移。权限与对象存在性先于回执，精确重试先于旧版本
门槛，不重新移除后来重建的记录。通用编辑/加血/状态tick不推断短休完成。

- `a14-focused-green.log`：194unit、8真实PG/HTTP测试通过，0失败/错误/跳过。
  覆盖短棒/匕首及满HP、只读预览、时长/中断/审阅回退、来源编辑/重建/一般加血、
  CSRF/玩家/严格JSON、错方案hash/时长/预览版本/跨战役ID、后续源卡活动、
  历史回执/撤权、同键并发、即时及提交时故障回滚、双DM的A13/A14竞争单效。
- `a14-ui-green.log`：A13/A14合计8交互测试通过；新4项覆盖显式输入/预览/执行、
  未知结果精确重试无客户端最终值、分钟/中断变更使方案失效、人工回退、
  revision与迟到403清私密状态、已知409只读恢复、持久完成后隐藏解除控件。
- `a14-frontend-full.log`：40文件363前端测试通过；生产构建通过。
- `a14-contract-green.log`：146 HTTP operations；候选/人工响应及严格请求示例通过。

完整真实IT、生产Chrome、真正停启/继续和最终停库离线结果在下方追加；在实际结束
前不视为通过。M3最终独立恢复、容量、精确交付HEAD日志、云端及真机继续后置。

## 最终实际验证与后续补齐

- 初版 `a14-full.log`（194unit+277真实IT）和 `a14-browser.log`（68完整Chrome零retry）
  通过；初版JAR另存a14-initial-acceptance.jar，SHA256
  `198e7b74219e419c39f709b88bdb10017d8d7b602204bd0361a6c5fbe4999db4`。
- 随后复核发现解除历史未保留具体休息分钟数，新增断言并实际执行
  `a14-history-red.log`：8项IT中1项失败“Rest history must preserve the adjudicated duration”。
  补入服务器生成的私有事件summary，保留分钟数、未中断及活动合规结论；玩家摘要
  仍仅来自PUBLISH，新事件不公开。补强浏览器与重启继续断言。
- 最终 `a14-full-final.log`：194unit+277完整真实PG/HTTP/WS IT，全部0失败/错误/跳过。
- `a14-browser-final.log`：67通过/1失败，trace显示旧地图测试静态模块CanvasPool加载
  net::ERR_NO_BUFFER_SPACE，应用未初始化；A14新流程通过。日志、trace和截图保留，
  未改产品代码、既有断言、超时或retries。待完整IT结束后重新跑完整浏览器套件。
- `a14-browser-accepted.log`：同一最终产物68完整生产Chrome、0自动retry；新独立浏览器库
  dnd_a14_browser_final，初版数据保留。既有断言/超时/重试配置未改。
- 前端363测试/生产构建、146契约通过。最终后端修正不改变前端产物或契约结构。
- `a14-before-restart.log`、`a14-after-restart.log`：新会话读回全部快照、历史及回执，
  精确重放不改后来重新建立的记录；中间真正按身份停止并重启Java/网页/PG。
- `a14-resume.log`：停启前的只读短休方案hash仍匹配，继续执行仅一次移除，版本增1；
  HP、临时生命、资源、源卡、原行动与骰子不变，私有历史保留时长。证据在仓库外
  a14-browser-complete-evidence.json及.resumed.json。
- 最终JAR SHA256：`f9e5ea46807b2f1f8c369c3d64ba9aea9e7b78b14ca3eb5f092635f5cdb2beec`。
- 最终桌面和390px截图已查看；不是真机。a14-stopped-processes.json记录身份停止；
  PG18472/Java18137/网页18138无监听、数据保留。a14-final-offline.log：清数据库环境
  且停库后194离线unit再次通过。
- 141份入口文件均仍存在且备份SHA一致；六份交接/设计文档保留旧字节为完整后缀。
  OpenAPI仅新增2路由/3schema/1响应事件，其余解析结构相同；CI/cloud补充未改。
  diff-check仅有入口已存在的continuation/verification文件尾空行，未清理旧内容。

无暂存/提交/推送/合并/发布，原分支/草稿PR/HEAD保持，无新HEAD远端CI。
M3未关闭；下一动作急救候选与其余最终门槛见[m3-remaining.md](m3-remaining.md)。
