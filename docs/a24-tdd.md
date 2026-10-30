# A24a M3恢复清单与原生同ID恢复验收（2026-10-30，未提交）

本轮是恢复审计切片，没有新增游戏效果。入口按AGENTS读取交接/验收、设计/计划、
CONTEXT、契约、Git；297份入口改动逐字节备份在仓库外`.tools/a24-entry`。
原分支`feat/accounts-and-invitations`、HEAD`c034aa4`及草稿PR #1保留；不暂存、
提交、推送、合并、发布或改PR，不操作dnd-roll-local及数据和旧fence。

## 来源与范围

先核对PostgresBackup/RestoreCommand、A03h模块恢复实现和契约，记录
[完整矩阵](m3-recovery-matrix.md)，再写[只读审计契约](../contracts/recovery-audit.md)。
原生P09是整部署同ID恢复；A03h是新ID定义恢复，明确不含运行时历史/骰/回执。
新增RecoveryInventory与离线RecoveryAuditCommand，固定34次迁移/76张public表
的完整schema签名、表行数/摘要、独立资产长度/hash。包含空表和私有日志，不从
游戏JSON猜测UUID，不做任意文本替换。没有新HTTP、前端行为或数据库迁移。

审计用只读repeatable-read事务，UTC；输出CREATE_NEW。只输出固定状态、表名/
策略/计数与阻断项，不输出游戏内容、凭据或主机路径。同ID全部核对才VERIFIED；
CAMPAIGN_REMAP始终BLOCKED并列出REMAP_WRITER_NOT_IMPLEMENTED和占用表要求。
退出码分别0/2，失败1；不改源写栅栏。独立战役ID映射writer未实现，M3未关闭。

## 实际RED/GREEN

- `.tools/a24-inventory-red.log`：269unit，7个实际断言失败、0错误/跳过。stub
  错把遗漏空表、未知表/列/FK、改行、非法元数据与remap当通过；不是编译失败。
- `.tools/a24-audit-red.log`：269unit通过；3个真实PG审计断言失败、0错误/跳过，
  stub审计无法验证档案/数据库/资产；不是缺数据库或环境失败。
- `.tools/a24-audit-green.log`：269unit及3真实PG IT全部通过；包含成功后行摘要
  保持、未知列/修改行阻断、改变/缺失资产拒绝、CREATE_NEW保护、输出隐私与重映射
  不得假成功。IT独立创建随机命名测试库，真实Flyway及pg_dump生成档案。
- catalog采集准备时遇到PG内部`"char"`拼接歧义，显式`::text`修正；属于设置
  错误，不记行为RED。准备阶段旧261unit+8IT通过，只用于schema采集。

## 本地集成验收

以下为本轮实际完成结果；同ID恢复不代表独立战役ID重映射已实现。
源独立PG18508/Java18173/网页18174，目标新PG18510/Java18175/网页18176。
所有工具、凭据、合成数据、截图、档案、报告和构建输出均在仓库外`.tools`。

完整类型化字段/namespace/hash/权限/旧回执策略与writer，容量、精确交付HEAD
日志、完整P09/M3总联验仍待。云端及物理真机后置，无发布/远端HEAD验证结论。

## 最终实际结果

- `.tools/a24-backend-full.log`：269unit、368完整真实PG/HTTP/WS IT零失败/错误/跳过。
  原生恢复发现12表13条CHECK由数组整体text转换重解析为逐元素text转换；
  `.tools/a24-native-schema-unit-red.log`270unit中1实际断言失败，
  `.tools/a24-native-schema-it-red.log`4真实IT中1实际断言失败，0错误/跳过。
  catalog仅显式列13条已核对的等价文本，不做通用SQL归一化；改变允许值/删CHECK、
  列/FK/index仍阻断。`.tools/a24-native-schema-green.log`270unit及4恢复专项IT通过，
  包括实际RestoreCommand再审计与恢复后改约束拒绝。不同IT共369，并未声称修复后
  全量369重跑；停库后`.tools/a24-final-offline.log`270unit再通过。
- `.tools/a24-frontend-full.log`435项通过，生产构建通过；177操作契约通过。
  旧前端/OpenAPI和所有旧后端/迁移、云CI字节保持，无新界面/接口或迁移。
- `.tools/a24-chrome-full.log`77完整Chrome零retry，采集16个独立战役A08–A23
  真实卡/账本/私有日志/骰/原回执/保存方案。沿用已有界面，不新增UI行为。
- `.tools/a24-source-verify.log`16组全部核对；实际P09迁移任务幂等重试、下载
  private/no-store/nosniff，持久化源write-fence后游戏POST503。原生ZIP76表、
  34迁移、54资产、18张必需证据表均非空，见`.tools/a24-native-coverage.json`。
- `.tools/a24-source-audit.json`实际CLI只读核全表/schema/行/hash/文件，退出0。
  源Java/网页/PG按PID命令身份停止，未删除任何源fence或数据。
- `.tools/a24-native-restore.log`实际RestoreCommand恢复原生ZIP到新独立空PG、
  新资产根，未用裸pg_dump替代。恢复后审计76表VERIFIED退出0；重映射审计
  `.tools/a24-restored-remap.json`BLOCKED退出2，完整列出未实现writer与各表要求。
- `.tools/a24-restored-verify.log`16组新会话完整快照/原回执/私密门/保存方案通过；
  `.tools/a24-restored-resume.log`16个保存待办各续办一次，核精确重试/不重掷与
  原卡资源/来源要求。`.tools/a24-restored-capture.log`保存续办后完整HTTP快照。
- 恢复Java/网页/PG真正停止、同固定jar/同数据重启；
  `.tools/a24-restored-compare.log`16组完整卡/历史/账本/骰/行动/先攻/预览深比较
  通过。两套六服务最终按身份停止，所有数据目录保留。
- 恢复端续办后，原源仅PG短启并只读核档案：
  `.tools/a24-source-independent-audit.json`76表/行摘要/资产仍VERIFIED，证明源未改；
  原HTTP未再启动、源fence保留。PG核身份再停机，停库清DB环境270离线unit通过。

恢复审计/真停启固定jar SHA256：`01287C40A49F2451953B6D35712467F72F25C6AE9DC0A323B6DFEEACA936093D`，源工具和恢复副本相同。
浏览器/原生RestoreCommand所用较早jar另存`.tools/a24-browser-acceptance.jar`。
`.tools/a24-jar-scope.json`逐项证明最终jar仅RecoveryInventory与清单不同，所有游戏
class、RestoreCommand与其他资源逐字节相同；兼容修复后已验270unit+4专项与全部
16组原生恢复续办。没有对无改动的前端重复生成一套浏览器证据。

外部覆盖统计脚本首次未显式UTF-8、被Windows默认GBK拒绝读取中文fixture；显式
UTF-8后通过，不计产品RED；初次严格schema拒绝报告原样保留于
`.tools/a24-restored-audit.json`，最终通过报告为`.tools/a24-restored-audit-final.json`。
源PG单独短启的外层PowerShell全流重定向遇到后台进程继承输出句柄、未进入审计；
按命令身份仅终止该验收shell，拆成独立CLI只读审计与PG核身份停机后通过，结果为
`.tools/a24-source-independent-audit-cli.log`及原JSON报告。源fence与所有数据保持，
没有把外部编排卡住记为产品RED或未执行的成功。

297入口文件中289逐字节保持、8项文档预期增补；新增10文件。审计
`.tools/a24-preservation-audit.log`确认所有入口备份hash、历史原文、旧代码/迁移/
OpenAPI/云补充、原分支/HEAD、空暂存区保持。`git diff --check`只保留入口已有
continuation/verification末尾空行，不为清理而删除原历史字节。

下一步A24b：类型化映射计划先行，列出ID字段/namespace/版本/固定内容身份/
源回执隔离/新权限审核/hash再生和拒绝矩阵；契约、实际RED后再实现计划及writer。
本轮CAMPAIGN_REMAP故意阻断，完整独立新团恢复续办、容量及精确交付HEAD日志仍待。
