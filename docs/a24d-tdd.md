# A24d 类型化 JSON 重绑定与 hash 再生 TDD（2026-10-31，未提交）

本片接续 A24c，仅支持已识别 A08–A23 records。主线先备份329份dirty入口到仓库外.tools/a24d-entry。本人拥有新增CampaignEvidenceRebind/Command及两个对应测试文件；主线拥有共享契约、原A24b Connection辅助、独立HashIT、PG环境、原生16组与最终全量验收。未修改旧业务来源门、UI、迁移、演示目录或源回执。不提交/推送。

## 实际执行

- `.tools/a24d-rebind-red.log`：305离线unit中2实际断言失败、0错误/跳过。stub错误接受未知adapter及缺身份映射，属行为RED。
- `.tools/a24d-rebind-green.log`：305离线unit通过。
- `.tools/a24d-rebind-hash-green.log`：308离线unit通过，增加Preview完整evidence原算法hash/新hash差异、源hash篡改与重复顺序稳定性测试。
- `.tools/a24d-hash-gate-red.log`：针对已有源hash核验门进行明确故障注入（临时移除旧hash校验），308离线unit中1实际断言失败、0错误/跳过，篡改Preview hash会被错误接受。已恢复门。这是门敏感性反证，不冒称初始stub已经包含该hash RED。
- `.tools/a24d-rebind-focused-green.log`：310离线unit及3真实PG专项IT全部通过。PG专项使用本轮新18516/test库，覆盖审计快照只读/稳定、变更行/schema先拒绝、同源目标/不存在源拒绝。它们不替代非空原生证据/hash及最终全量。

随后按主线和字段复审收紧：报告元数据绑定源/目标/seed/digest；UUID_FIELDS显式record class+字段允许矩阵；canonical使用原Effect完整生成；A15 correction引用复用专用骰alias；事件ID碰撞核查源/目标身份和既有生成事件；未知table报告只固定UNKNOWN。上述最终修改由主线执行最终验收，本段不提前写通过。

一次PowerShell使用Unix大括号文件列表导致解析错误，命令未执行，属于准备问题，不算行为RED。入口offline因缺临时目录ImageIO错误由主线另记，不算本片行为RED。

## 实现边界

完整私密records仅在内存，报告仅固定元数据/类型计数/UUID/hash证明与固定阻断码。明确字段重绑，不扫描自由文本、资源本地键或所有UUID。固定Source/ContentEntry、actor/controllers/时间戳与源Create.requestId保留仅作历史证据，不产生目标授权。

A24b映射算法逐项重验（timeline允许明确owner alias），源A24c结构/角色历史/骰回执fingerprint/actor/canonical来源门全部先通过。六journal调用原服务private plan，使用拒绝写入的历史Store代理与精确before/账本版本；代理只供内部历史重建，不是外部API或权限授予。A18/A21先验证旧hash并重生新journal，A22/A23随后绑定完整新journal。Preview按原fp的一元素数组算法重生。没有持久化的临时A13–A16/A19 planHash不从摘要猜测。

所有输出始终BLOCKED/executable=false。源回执留源；无writer、无新团恢复、无花费资源/新目标确认。权限/文件/模组图、隔离writer、独立续办与P09/M3关闭仍待。最终完整回归、native16、CREATE_NEW、后审计与停服由主线按实际日志追加。

## 最终边界补强与实际复验

- `.tools/a24d-final-boundary-red.log`：313离线unit中3实际断言失败、0错误/跳过。非法sourceDigest原字符串进入失败报告；生成的事件ID与显式occupied SQL UUID相同仍放行；attack_preview没有SQL version列时证明错误输出-1而非JSON版本。
- 修复后 `.tools/a24d-final-boundary-green.log`：313离线unit和4真实PG专项IT零失败/错误/跳过。4IT含主线新增非空JDBC Preview/hash污染/重归档测试；不是一次完整382IT回归。
- 最终失败报告仅保留合法64hex digest；事件collision集合包含源JSON全部Preview/Combat Event.id（含A15专用roll）、proposal源/目标UUID和当前只读snapshot全库SQL UUID。Command查询列来自已审计76表明确UUID列，最大1000000去重身份，超限拒绝；不输出全库身份集合或私密JSON。
- Preview Proof.version从已核验JSON Preview.version取得，journal仍使用SQL结果账本版本。CLI仍BLOCKED/退出2，不复制源回执、不授予目标确认。

主线首次完整回归的HashIT夹具先在内存构造hash、再落JSONB，未复用真实服务从JDBC读回sheet的map顺序，导致原非canonical算法来源偏差。主线修为先JDBC读回sheet后建hash，定向已通过；准备偏差不计产品RED，不改变原算法。主线最终固定全量/native16/源后审计/停服证据待主线追加。

## 主线最终完整验收与交接

- `.tools/a24d-backend-full-final.log`：最终冻结代码313离线unit、382完整真实PG/HTTP/WS IT，全部零失败/错误/跳过。不是将首次381成功项与专项拼成完整382；最终完整回归确实重新执行。
- `.tools/a24d-accepted-native-graphs-summary.json`及`a24d-native-proofcheck.json`：最终固定JAR的16组A08–A23真实历史全部consistent；38份源hash验证/新hash再生证明，覆盖Preview和六类Entry共7个持久hash适配器，所有证明版本非负、源/目标hash不同。A16样本无持久planHash，因此0份hash证明是明确范围，并未补造临时方案。源/目标/seed/档案digest元数据逐组核验。
- A08跨进程重复输出逐字节一致；既有输出路径CLI退出1、原文件hash保持。所有正常审阅报告仍BLOCKED/executable=false、CLI退出2，无新团写入或确认授权。
- `.tools/a24d-accepted-after-graphs-audit.json`：全76表/54资产VERIFIED，报告生成后源行/文件保持。中间preflight16组不冒充最终构建验收；最终accepted目录独立执行。
- 固定JAR SHA256 `AC59551D057CC5C278BA93C7190EF44278AF98B67371EFF1549D6E5311554545`。`a24d-jar-scope.json`核最终669主类/资源与当前编译逐字节一致；665个A24c既有main资源只有CampaignRemapPlanCommand的Connection辅助提取发生预期变化，其他旧类/资源保持。停库离线后再次核固定构建一致。
- `.tools/a24d-stopped.json`按实际PID/CommandLine/data目录核本轮PG18516后停止，未启动常驻Java/网页；所有本轮数据库/资产/档案/报告保留，未操作旧服务/数据/fence。清DB环境后`a24d-final-offline.log`313offline再次通过。
- `a24d-preservation.json`：329入口文件中320逐字节保持，9项为预期A24b辅助/CHANGELOG/契约索引/6共享文档更新；新添8文件。全部原历史字节、前端/UI/OpenAPI/迁移/旧业务与云补充保持，14个UI构建文件摘要保持。本片未重跑前端/Chrome，不声称新437/77。
- 分支feat/accounts-and-invitations、HEAD c034aa48fe1176f8e4e0711cf22031d04b5f6ebd及空暂存区保持；PR #1开工只读核为open/draft且HEAD一致。没有暂存/提交/推送/合并/发布或PR元数据修改；两个子agent已结束，无后台工作遗留。

准备/夹具问题明确保留：入口offline未建a24d-tmp造成9个ImageIO错误，建目录后303全部通过；首次完整回归310unit通过、382IT中新增HashIT正例1失败。该夹具直接用内存CharacterSheet.empty建hash，与真实服务先JDBC读回卡的Map顺序不同；改为SQL插入后读回sheet再构建Preview，原hash算法没有改变。这些不是产品行为RED，也不把首次完整失败写成通过。最终313/382完整通过以final日志为准。

本切片完成的是A24c已声明records的类型化JSON/hash审阅。其他运行时证据及权限/文件/模组图须先独立界定字段/支持与阻断矩阵，再契约和实际RED；之后才隔离事务writer、新目标重新审核/预览/确认与独立新团续办。CAMPAIGN_REMAP保持BLOCKED，P09/M3、容量、精确交付HEAD CI、云端及物理真机未关闭。仓库交接已更新，适合在此阶段边界换会话。
