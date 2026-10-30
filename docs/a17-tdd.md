# A17 治疗解除原A16稳定/0HP昏迷：实际TDD与本地验收

2026-10-29，未提交。按AGENTS读最新交接/设计/计划/CONTEXT/契约并核对Git。
沿用feat/accounts-and-invitations、HEAD c034aa48fe1176f8e4e0711cf22031d04b5f6ebd、
草稿PR #1；178份入口文件逐字节备份于D:/workspace/dnd/.tools/a17-entry，清单
a17-entry-manifest.json。所有以下日志和运行数据位于仓库外.tools，不含对外交付凭据。

## 来源 → 契约 → 实际失败

- [来源/5R版本和矩阵](research/stabilization-healing-5r.md)：A库只定位，B库回读
  生命值降至0点/治疗/临时生命三整页，官方SRD5.2.1 pp17–18交叉核验，商业正文未读取或复制。
- [契约](../contracts/stabilization-healing-effects.md)和OpenAPI先于HTTP实现。
- a17-domain-red.log：8领域测试实际运行，7失败/1已有确定性与只读性通过，0错误/
  跳过；占位计算无候选/错误拒绝/回退原因，是真行为RED。实现后8通过。
- a17-domain-green.log完整unit曾因新a17-tmp目录未建立，9个原图片夹具报缓存错误；
  这是运行环境准备失败，未改产品代码规避。随后HTTP运行的完整213unit已全通过。
- a17-http-red.log及red-behavior.log：各213unit通过、7真实HTTP因404实际失败，
  0错误/跳过；之后才实现服务/存储/控制器/V28，a17-http-green.log的7IT全通过。
- a17-ui-red.log：6界面测试实际缺控件失败，随后实施中文流程、精确未知结果重试、
  revision/切域/撤权处理；UI+API7项通过。复审新增later-episode-red.log：6通过/
  1失败，HEALED旧记录挡住后续独立普通0HP Help入口；修复并重跑完整前端。

## 实现边界与安全

DM明确普通真实治疗量/来源/例外；最新APPLIED A16和其完整原应用结果未变，原
服务器骰重新核对，完整卡/状态/审阅/版本/hash绑定。只读方案再确认，服务器重算
后原子写目标/完整安全角色历史、HEALED状态/不可变稳定历史、私有治疗日志/回执。
真实HP上限封顶，原tempHP/资源/施救者/骰/稳定证据保持，两条原来源记录解除，
死豁0/0。治疗骰/资源消耗/完整状态/倒地不执行，其他来源及后续编辑人工处理。
当前DM/对象先于旧回执；异体/跨路由/跨目标冲突，并发单效；立即/延迟DB约束
错误使整笔事务回滚。玩家受控目标的历史也不泄露私有治疗理由/审阅/请求量。

V28仅新增stabilization_healing_history，复合FK绑定准确历史版本。迁移IT从V27
填充角色与A16状态/历史/回执后，对全部旧表列/行、27个checksum逐项对比；新表
初始空，错误历史版本FK失败、存在版本合法插入。旧迁移与旧A07–A15实现保持。

## 最终实际验证

| 检查 | 结果与日志 |
| --- | --- |
| 后端 | 213unit+303真实PG/HTTP/WS IT，0失败/错误/跳过；a17-backend-full.log |
| 前端 | 45文件385测试通过；a17-frontend-final.log；a17-build-final.log构建通过，现有chunk体积提示保持 |
| 契约 | 157 operations；a17-contract.log，旧154 operations除A16状态增加HEALED外保持，新增3路由/5schema |
| 初版Chrome | a17-browser.log：70旧场景通过/1新场景失败，测试在abort前unroute导致已处理请求；改用一次性标记、实际提交后abort，后续continue |
| 聚焦Chrome | 首次focused因390px切到默认地图页签而找不到角色区；补真实“角色卡”页签点击；focused-green的完整新流程通过，非产品绕过 |
| 最终Chrome | a17-chrome-final.log：71场景全通过、零retry；实际提交丢响应后精确重试只应用一次；390px无横向溢出，桌面/手机截图已查看（非真机） |
| 真停启 | 三进程按身份停后端口无监听，保留first-stopped-processes；重新启动同一固定jar/PG/网页，新会话完整cards/histories/states/stateHistory/healingHistory/dice/旧回执/保存方案核对通过，a17-after-restart.log |
| 独立恢复 | pg_dump整库→新PG18482及独立资产副本，restored-verify/resume.log核对后续办一次；source-independent.log证明原库未变，原库resume.log再独立续办一次 |
| 停库离线 | 两套Java/网页/PG最后全按身份停止、六端口无监听、数据保留；清DB环境且Maven-o，a17-final-offline.log再次213unit通过 |

原独立PG18480/Java18145/网页18146；恢复PG18482/18147/18148。固定jar hash见
a17-jar-sha256.txt，真实停止进程证据在a17-first-stopped-processes.json、
a17-stopped-processes.json、a17r-stopped-processes.json。验收数据和凭据均不入仓库。

下一候选：稳定后受伤的失稳/死豁记录，先查5R来源/矩阵再契约与实际RED。
M3未关闭，完整P09清单/独立战役重映射（含A08–A17）、容量及精确交付HEAD日志
待办；本次整库恢复不代替总验收。云端/真机后置。未暂存/提交/推送/合并/发布，
不操作dnd-roll-local及数据/旧fence。全部分工和进度位于work-packages.md顶部。

## 最终保留核对

a17-audit.log核验178份入口文件与备份hash：164份逐字节未改，14份仅本次接口/
生命周期接点及契约、文档和CHANGELOG的预期补充，17个新增文件均属于A17。
六份交接/计划文档与契约README完整原字节保留；云补充CI文件逐字节未改。
既有154个OpenAPI operations逐项相等，全部旧schemas仅A16状态枚举增加HEALED，
新增3个operations/5个schemas。旧迁移文件、A07–A15实现及A16后端保持；原分支/
HEAD、空暂存区和六个本次端口无监听核验通过。git diff --check只报告入口已有的
continuation/verification尾部空行，未清理用户原内容；A17新增文件无空白错误。
