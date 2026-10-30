# A15 动作急救解除原击昏：实际TDD记录

2026-10-28，未提交。原分支feat/accounts-and-invitations、HEAD
`c034aa48fe1176f8e4e0711cf22031d04b5f6ebd`。按AGENTS读最新交接/Git、设计/计划、
CONTEXT和契约入口；A07–A14不重做。150份入口改动逐字节备份至仓库外
`D:/workspace/dnd/.tools/a15-entry`。远端分支同HEAD；只读PR检查确认#1仍open/draft、
未合并且head相同，未改PR元数据。全部既有内容与云补充保留。

先[5R来源/支持矩阵](research/knockout-first-aid-5r.md)，再[独立契约](../contracts/knockout-first-aid-effects.md)，
随后实际执行RED。A库只定位，B库全文回读，官方SRD交叉核对；不读取商业正文。

## 实际 RED 与准备失败

- `a15-red.log`：新增测试初次编译失败，LinkedHashMap局部变量被赋值为Map，修正
  测试声明后重跑；该编译错误不冒充行为RED。
- `a15-red-http.log`：194既有离线unit通过，8新增真实PG/HTTP测试全部实际失败于
  新急救路由404。使用新PG18474及新库/数据目录，不复用旧验收数据。
- `a15-ui-red.log`：5新增交互测试实际失败于缺少急救控件。
- `a15-contract-red.log`：缺少A15路由断言实际失败。
- 一次补丁因DiceService单行锚点不匹配被拒绝，未写文件；随后用确切锚点应用。
  最初读取不存在的contracts/dice.md后已定位并读取dice-initiative.md，未推断其内容。

## 实现与首轮 GREEN

仅原A11/A12未改目标卡；普通WIS医药施救者、动作可用/接触/例外由DM审阅。
先专用动作入口生成单d20私有医药检定，再读绑定方案，成功后显式解除。失败也
保存，读方案不掷骰。内部A15幂等指纹绑定击昏/版本和施救者/计算，普通骰即使
预占派生请求UUID也不能代替专用动作。ROLL事件ID=rollId，记录helper/target版本
及私有DC/总值/结果；仅最新绑定尝试可执行，未采信客户端结果或任意普通骰。

两个写入口同actor/campaign事务保存各自证据/回执；故障无部分骰或解除。
解除一次卡增版+完整KNOCKOUT_FIRST_AID before/after历史+事件，保留检定ID/DC/总值；
生命、资源、施救者、原攻击行动/证据/hash保持。权限与对象存在性先于回执，旧
回执不会重掷或移除后来恢复的记录。无迁移。0HP稳定、优劣势/修正器/覆盖、
完整动作预算/距离/状态引擎仍人工，醒来仍倒地。

- `a15-focused-green.log`：初轮8项中1项失败；天然1测试后未重置夹具熵，导致下一
  场原击昏攻击失手。修正每轮夹具先用20建立击昏，再设置医药骰，未改产品规则。
- `a15-focused-final.log`：197unit及9真实PG/HTTP测试通过，0失败/错误/跳过。
  覆盖两个击昏来源及满HP、失败检定/新动作/读不重掷、天然1总值10成功和天然20
  总值9失败、CSRF/玩家/严格字段/错hash/普通骰拒绝、施救者与目标变化、
  历史回执/撤权、两阶段即时及提交时回滚、同键并发、预占普通骰回执拒绝、
  不同请求同版本竞争、跨战役对象拒绝及双DM急救/短休竞争单效。
- `a15-ui-green.log`：A13–A15共13交互测试通过；A15覆盖两写入口精确未知重试、
  无客户端最终值、保存检定不自动解除、失败/选择/审阅/版本失效、迟到403清私密
  状态、409只读恢复与持久完成后的控件隐藏。独立施救者历史名称正确显示。
- `a15-frontend-full.log`：41文件369前端测试通过，生产构建通过。
- `a15-contract-green.log`：149 HTTP operations，新增请求/响应及非法客户端结果
  示例通过；旧契约解析结构保持，仅新增3路由/4schema/2响应事件。

完整真实IT、生产Chrome、真正停启/继续和停库离线结果在下方追加；在实际结束前
不视为通过。M3最终独立恢复、容量、精确交付HEAD日志、云端和真机继续后置。

## 最终实际验证

- a15-full.log：197unit+286完整真实PG/HTTP/WS IT，全0失败/错误/跳过。
- a15-browser.log：69完整生产Chrome零retry；数据库联验结束后运行，未改既有断言/
  超时/重试配置。新流程使用独立施救者，保存失败后另行动作；骰与解除两次真实提交
  丢响应、原请求重试均单效。只读不重掷、玩家拒绝、后续恢复记录不被旧回执移除、
  原攻击证据/源卡和施救者保持均实际通过。桌面与390px截图已查看（不是真机）。
- a15-before-restart.log / a15-after-restart.log：真正按身份停止Java/网页/PG并重启同一
  产物/独立数据后，新会话完整快照、历史和原回执一致。
- a15-resume.log：停启前保存的成功专用医药检定及方案hash继续有效；显式解除+精确
  重放仅一次卡增版，骰不增加，生命/临时生命/资源/施救者/原源卡/行动保持，私有解除
  历史保留绑定rollId/DC/结果。证据a15-browser-final-evidence.json及.resumed.json在仓库外。
- 最终JAR SHA256：`32e075163f769076bee80542655f24e0eeb99fbf3ea02a8df31d7fb0c921408a`。
- a15-stopped-processes.json：最终PG18474/Java18139/网页18140按身份停止、无监听，
  数据保留；a15-final-offline.log：停库清数据库环境后197离线unit再次通过。
- 150份入口文件仍全部存在且备份hash一致；六份交接/设计文档原字节完整保留为后缀。
  OpenAPI仅新增3路由/4schema/2响应事件，其余解析结构相同；原本clean的DiceService
  与HEAD比较仅新增一个内部方法，既有入口/指纹语义保持；CI和云补充未改。
  diff-check仅报入口已存在的continuation/verification文件尾空行，未清理旧内容。

无暂存/提交/推送/合并/发布，原分支/草稿PR/HEAD保持，无新HEAD远端CI。
M3未关闭；下一0HP稳定候选及最终联验/容量/云端/真机见[m3-remaining.md](m3-remaining.md)。
