# A24c 嵌套证据图 TDD 记录（2026-10-31，未提交）

父线已读取AGENTS和最新交接、备份314份入口改动到仓库外.tools/a24c-entry。本人负责新增CampaignEvidenceGraph、独立离线Command、对应测试、契约与来源研究。未改旧业务服务、规则效果、JDBC、迁移、原A24b命令；不提交或推送。

先核对真实records/JDBC/V27–V34，写contracts/campaign-evidence-graph.md，再添加可编译stub与两个未知adapter/嵌套shape断言。

## 已实际执行

- .tools/a24c-unit-red.log：280unit，2实际断言失败（unknown JSON与unknown adapter仍错误返回consistent），0错误/跳过。属产品行为RED。
- .tools/a24c-unit-green.log：280unit通过；green3：290unit通过，12个图单测涵盖结构闭包、alias、版本、缺历史、源指纹门、跨团、canonical owner与隐私/不可变/稳定顺序。
- .tools/a24c-focused-green.log：290unit及3真实PG IT通过；空图只读、跨团scope、未知JSON与归档/schema改变拒绝。
- .tools/a24c-focused-green2.log：294unit及4真实PG IT通过；补A19 origin/A22/A23 damage与SQL JSON版本反例，新增非空真实Card/History reader与损坏历史JSON版本校验。
- .tools/a24c-focused-green3.log：294unit及4真实PG IT通过；补事件namespace/链接闭包、journal来源绑定。随后补源receipt actor及未知discriminator门、内部before-history适配器和3项反例，最终结果由主线补充。

执行准备中曾调用Windows python别名，提示未安装；未写产品文件，之后改用Node/PowerShell。一次PowerShell大括号文件路径解析错误发生在命令执行前。这些不计行为RED。

## 行为边界

同一只读repeatable-read事务先完整A24a归档/76表/schema/资产核验，再读取明确适配器。严格record反序列化，unknown/missing/primitive-null及未知kind/status拒绝。图输出只固定路径/UUID/版本/计数/错误代码。

Card当前关系列/控制者/sheet重组，与before/after快照索引交叉核对。嵌套Roll必须等于SQL保存的不可变Roll；A15/16/19源专用dice receipt fingerprint只读重算比对，A16/19包含当时target/独立ledger及helper/calculation tuple，源actor核对；回执不输出、不复制、不授权。

A16/A19状态target别名、attempt/roll别名、不同ledger版本、A19/A20/A21 origin、A22/A23 damage指向独立namespace。canonical注记经原Effect和真实骰total生成完整Condition后精确比较。文本、固定Source/内容/资源本地键不扫描替换。事件身份按preview/combat owner隔离，A15事件复用dice owner；correctionOf/combatEventId与Change卡版本闭包检查。

所有结果仍BLOCKED/executable=false，consistent只表示本切片提取与校验。未创建新团；完整JSON重绑定/hash再生/权限文件模组图/writer/独立续办仍待。主线负责最终完整回归、16原生历史、资产后审计、停服、入口保护与共享交接。

## 子任务最终交付证据

.tools/a24c-focused-final.log：299离线unit（含21图专项）与4真实PG专项IT，全部零失败/错误/跳过；新JAR已复制仓库外.tools/a24c-graph-acceptance.jar。当前卡必须匹配authoritative after_history同版本且为最新；所有v>0 after_history都要有精确前像before(v-1)==previous-after。before-history为内部适配器，不能伪造authoritative版本存在证明。新增反例包括源receipt actor、A20共享版本、A21 review来源、缺authoritative历史与缺前像。

主线接手最终完整回归、native16团及资产后审计；本文不提前声称这些已通过。主线如补充重复事件/分ledger状态等最终审查修复，以主线日志和补充为准。

## 主线最终审查与实际验收补充

- `.tools/a24c-final-boundary-red.log`：23图单测中2实际失败、0错误/跳过，复现同owner重复事件与A19认领父账本专属状态；修复后进入全量回归。
- `.tools/a24c-backend-full-final.log`：301unit、377完整真实PG/HTTP/WS IT全部零失败/错误/跳过。该全量在最后来源审查修复之前执行，不能称最后修复后再次跑了完整377。
- `.tools/a24c-a19-canonical-red.log`：24图单测中1实际失败，原A19注记分支错误提高失败骰total；改为真实骰total。
- `.tools/a24c-history-reader-red.log`：302unit通过，5真实PG专项中1实际断言失败、0错误/跳过；交换SQL历史行的before快照可以借用另一行自报版本。reader改以所属SQL历史行version-1核before，不信JSON自报版本。
- `.tools/a24c-canonical-target-red.log`：25图单测中1实际失败、0错误/跳过；全文canonical且owner存在仍不能借给另一target。现在按承载Card核owner.target；A19生成canonical也使用原reviewed/真实total。
- 最终固定代码 `.tools/a24c-acceptance-final.log`：303离线unit（25图专项）及5真实PG专项全部零失败/错误/跳过。不同IT共378，但不称一次378项全量。未改旧后端业务、旧A24b、迁移或HTTP协议。
- 最终 `.tools/a24c-accepted-native-graphs-summary.json`：原生P09档案副本在新PG18514/dnd_a24c_identity和独立资产根，16个A08–A23真实历史全部consistent；A08跨进程重复逐字节一致，已有输出CLI退出1且不覆盖。所有报告BLOCKED/executable=false、CLI退出2。
- `.tools/a24c-accepted-after-graphs-audit.json`：全76表/54资产VERIFIED，无源数据或资产改写。之前两轮native属于中间构建，最终结论只引用accepted目录。
- 固定JAR SHA256 `354930D8C690B0443D0685750BAFDC21C554C53C5CC2C655C16D45DB390355E8`；659个既有main class/资源与本轮UI验收jar逐字节一致。最终停库离线构建后再核固定jar与当前所有main class/资源逐字节一致，无验收漂移。
- UI本轮437前端测试/构建、固定最终77Chrome零retry、14文件摘要、1440/390px与五配色均通过；79旧dirty前端文件保持。详见ui-adaptation.md，不沿用A24a435/77冒称重跑。新文件EOF空白整理后重新构建，14输出摘要仍与浏览器固定构建一致。
- `.tools/a24c-stopped-ui.json`/`a24c-stopped.json`按CommandLine/目录身份停止Java18177、网页18178和PG18514；数据/素材/档案/报告全部保留，停库清DB环境后303offline再次通过。
- 主线外置脚本准备曾误用Windows Python别名、默认GBK读取UTF-8及相对路径定位；纠正后执行，不计行为RED。git diff --check仅入口两历史文档已有EOF空行，保持原字节；新文件无额外空白错误。

源回执只读核fingerprint/actor后留源；不输出私密游戏JSON/文本/账号/凭据。最终图校验不替代完整JSON重绑定、hash再生、权限/文件/模组图或writer，没有新团恢复或独立续办。CAMPAIGN_REMAP仍BLOCKED，P09/M3、容量、精确交付HEAD CI与云端/真机保持待办。
