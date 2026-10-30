# A19 原A18失稳经新Help再次稳定：实际TDD与本地验收

2026-10-29，未提交。按AGENTS读取最新交接/验收/设计/计划/CONTEXT/契约及Git；
沿用feat/accounts-and-invitations、HEAD c034aa48fe1176f8e4e0711cf22031d04b5f6ebd、
草稿PR #1，212份入口文件备份在D:/workspace/dnd/.tools/a19-entry及hash清单
a19-entry-manifest.json。以下日志/验收数据均在仓库外.tools。

## 来源 → 契约 → 实际失败

- [5R来源/版本与矩阵](research/restabilization-5r.md)：A只定位，B全文核对生命值
  降至0点/属性检定/动作，官方SRD5.2.1交叉核验。商业正文未读取/复制；区分新
  Help稳定检定与一般Help授予优势，不要求技能熟练，不应用死豁天然1/20规则。
- [契约](../contracts/restabilization-effects.md)与OpenAPI先于HTTP实现。
- domain-red首次8测试为7断言失败/1空列表取首元素异常；先给身份测试补候选
  长度断言，domain-red-behavior.log实际8失败、0错误/跳过，再实现领域计算。
  domain-green.log的新增8及全量229离线unit通过，无数据库/网络依赖。
- http-red.log：229unit通过，8真实HTTP因404实际失败、0错误/跳过；随后才实现
  独立服务/存储/控制器与专用骰。http-green.log的8真实IT全通过。
- ui-red.log第一次因测试生成的错误导入名而未运行，不算行为RED；修正导入后
  ui-red-behavior.log的7交互测试实际缺控件失败。实现后7UI+1API通过。新增旧
  A16 RESTABILIZED隔离测试的old-controls-red.log为7通过/1失败；修复不适用
  Help/应用按钮及历史提示，最终全量前端通过。未删除或放宽旧断言。

## 实现与权限边界

共享UNSTABLE状态及同版本A18日志绑定；重新核对原私有A16骰，完整重建A16
应用卡与A18伤害before/after，要求当前目标完整卡等于原A18结果。新施救者普通
WIS医药/不同身份、无审核/状态/调整/覆盖，原施救者之后的编辑不影响旧来源。
专用A19请求UUID和骰指纹拒绝普通预占；独立账本版本、原来源版本、双方完整
快照/版本/计算与最新新骰绑定，失败留痕。读不重掷，另一次明确动作替代旧尝试。

只读方案再确认，服务器重算/hash比对后原子写目标/安全完整角色历史、新账本
APPLIED/私有completion及历史、共享RESTABILIZED/历史、回执/安全失效事件。
死豁清零，HP0/tempHP保持；新a19-stable并原样保留a16-unconscious，不新造第二个
昏迷来源。当前DM/双方存在先于旧回执；同/异key竞争单效，立即roll失败和延迟
apply失败整笔回滚，含骰及双账本。玩家受控目标的历史也不泄露私有检定/理由。

V30只新增两表，双表均复合FK绑定确切A18伤害历史版本；从V29填充A16状态/
历史/回执、A17/A18日志后比较全部旧表列/行及29个checksum。新表空、错误来源
版本FK失败，合法版本可写。旧迁移与A16/A17/A18后端保持；DiceService仅增加
新的内部专用方法，所有原方法字节保留。

## 最终实际结果（最终源码）

| 检查 | 结果/日志 |
| --- | --- |
| 后端 | 229unit+321真实PG/HTTP/WS IT，0失败/错误/跳过；a19-backend-full.log |
| 前端 | 49文件403测试，a19-frontend-full.log；a19-build.log构建通过，现有chunk体积提示保持 |
| 契约 | 165operations；a19-contract.log；新增5路由/7schema，旧160操作保持，原A16状态仅增加RESTABILIZED |
| 聚焦Chrome | a19-browser.log首次新场景通过：真实失败检定、独立新动作、保存检定和应用两次真实提交丢响应后的精确重试单效 |
| 全量Chrome | a19-chrome-final.log：73场景通过、零retry，390px无溢出；桌面/手机截图已查看，非真机 |
| 真正停启 | 按身份停Java/网页/PG、端口无监听，first-stopped-processes.json留证；新进程同jar/新会话后after-restart.log核对完整卡/历史/双账本/A18日志/骰/回执及保存方案 |
| 独立恢复 | pg_dump→新PG18490及独立资产副本，restored-verify/resume.log核对并续办保存新检定一次，HP/tempHP/原昏迷/资源/施救者/骰保持、死豁归零；source-independent.log证明原库未变 |
| 原库续办 | resume.log再独立续办一次，读/精确重试不多掷骰或重置后续编辑；原A16/A18及失败新检定旧回执保持后续游玩 |
| 停库离线 | 两套Java/网页/PG最终按身份停止、六端口无监听、数据保留；清DB环境且Maven-o，final-offline.log再次229unit通过 |

日志上述简写均带a19-前缀。原独立PG18488/Java18153/网页18154，恢复18490/
18155/18156均已停止；固定jar hash在a19-jar-sha256.txt，凭据和数据不入仓库。

下一候选为治疗解除原A19再次稳定记录，先规则来源/矩阵再契约与实际RED。
再次稳定后伤害、自动死豁/死亡/复活、1d4小时恢复与完整动作/状态/资源仍人工，
旧0HP门不放开。M3未关闭，完整P09清单、含A08–A19的独立战役重映射、容量及
精确交付HEAD日志待办；整库恢复不代替总验收。云端/真机后置，无暂存/提交/
推送/合并/发布，不操作dnd-roll-local及数据/旧fence。分工进度见work-packages.md。

## 最终保留核对

a19-audit.log核验212份入口文件和备份hash：197份逐字节未改，15份仅本次专用骰/
生命周期接点、契约、交接及CHANGELOG的预期补充；17个新文件均为A19。六份交接/
计划文档和契约README完整原字节保留；云补充CI文件逐字节未变。DiceService原有
全部字节/方法保留，仅新增A19内部方法并归位说明注释；全量回归后只整理了该
注释，行为未变，停库229离线重新编译通过，运行验收固定jar的hash已留证。
旧160个OpenAPI operations逐项相等，旧schema仅A16状态枚举增加RESTABILIZED；
新增5路由/7schema。旧迁移与A07–A18其余后端保持。原分支/HEAD、空暂存区及本次
六端口无监听核验通过；git diff --check仅报告入口已有的continuation/verification
尾部空行，保留用户原内容；新文件无空白错误。
