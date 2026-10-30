# A18 原稳定后首次受伤失稳：实际TDD与本地验收

2026-10-29，未提交。按AGENTS读取最新交接/验收、设计/计划/CONTEXT/契约与Git；
原分支feat/accounts-and-invitations、HEAD c034aa48fe1176f8e4e0711cf22031d04b5f6ebd、
草稿PR #1保持。195份入口文件逐字节备份于仓库外.tools/a18-entry，清单
a18-entry-manifest.json。以下日志与本次运行数据均在D:/workspace/dnd/.tools。

## 来源 → 契约 → 实际RED

- [5R来源/支持矩阵](research/stabilization-damage-5r.md)：A库只定位，B库两整页
  回读，官方SRD5.2.1 pp17–18核验。tempHP是HP损失缓冲而非伤害免疫；本切片
  以已裁定的正伤害事件触发失稳，不以HP实际损失>0为前提。amount>=maxHP即使
  有tempHP也保守转人工，不推断死亡边界。未读取/复制商业正文。
- [契约](../contracts/stabilization-damage-effects.md)/OpenAPI先于实现。
- a18-domain-red.log：8测试实际运行7失败/1确定性只读检查已通过，0错误/跳过。
  占位计算没有候选值/正确回退/输入拒绝；实现后8领域测试及完整221离线unit通过。
- a18-http-red.log：221unit通过，8真实HTTP测试因新接口404实际失败，0错误/
  跳过；随后实施独立服务/JDBC/控制器/V29，a18-http-green.log的8IT全通过。
- a18-ui-red.log：7交互测试实际缺控件失败；实施后补UNSTABLE旧控件隔离测试，
  green-initial中8通过/1失败（旧A16仍显示不适用Help按钮）。修复并保留新历史提示，
  最终8UI+1API及全量前端通过。非删除断言绕过失败。

## 最终行为与保护边界

最新APPLIED原A16结果、完整目标卡与版本、规范两条记录及原服务器骰绑定；DM
已裁定最终伤害/critical/reviewed，预览hash包含完整当前状态与确切输入。确认再
重算，原子写角色及完整安全历史、UNSTABLE状态与不可变历史、私有伤害日志和
回执。HP仍0、tempHP先扣，移除原Stable并保留原0HP昏迷，死豁失败普通1/重击2。
0金额无应用、致死边界人工，不翻倍已裁定金额、不自动判重击/死亡或掷死豁。
施救者/资源/原骰/原A16证据保持；当前施救者编辑不影响原目标来源校验。
当前DM/对象先于历史回执；精确旧回执不改后续游玩；同key/异key并发单效，
立即/延迟DB约束错误整笔回滚。玩家受控角色历史也不泄露私有理由或输入包。

V29仅新增stabilization_damage_history；复合FK关联准确稳定历史版本。迁移IT从
V28填充角色/A16状态历史回执/A17治疗日志，比较全部旧表列/行及28个checksum，
新表初始空、错误版本FK失败、合法历史版本成功。旧迁移与A16/A17后端保持。

## 最终实际结果（最终源码）

| 检查 | 结果/日志 |
| --- | --- |
| 后端全量 | 221unit+312真实PG/HTTP/WS IT，0失败/错误/跳过；a18-backend-full.log |
| 前端全量 | 47文件394测试，a18-frontend-full.log；a18-build.log构建通过，现有chunk体积提示保持 |
| 契约 | 160operations，a18-contract.log；新增3路由/5schema，旧157操作保持，原A16状态仅加UNSTABLE |
| 聚焦浏览器 | a18-browser.log：首次新真实Chrome场景通过，包括0/致死伤害人工回退、真实提交后丢响应及精确重试单效 |
| 全量浏览器 | a18-chrome-final.log：72场景全通过、零retry；390px无溢出，桌面/手机截图已查看，非真机 |
| 真实停启 | a18-first-stopped-processes.json保存按身份停止的Java/网页/PG；新进程同jar与新会话后a18-after-restart.log核对完整cards/histories/states/stateHistory/damageHistory/dice/回执和保存方案 |
| 独立整库恢复 | pg_dump→独立PG18486及独立资产副本，restored-verify/resume.log核对并续办一次，先吸收7tempHP、仍0HP、记1失败、只移除Stable；source-independent.log证明原库未变 |
| 原库续办 | a18-resume.log再独立续办一次，精确重试不重复吸收tempHP或加失败，旧回执不覆盖后续记录，资源/施救者/骰保持 |
| 停库离线 | 两套Java/网页/PG最后按身份停止、六端口无监听，数据保留；清DB环境且Maven-o，a18-final-offline.log再次221unit通过 |

原库PG18484/Java18149/网页18150；恢复18486/18151/18152，均已停止。固定验收jar
hash在a18-jar-sha256.txt；过程数据、备份及凭据不进入仓库。

下一候选：原A18失稳记录经新Help再次稳定，先5R来源/支持矩阵再契约及实际RED。
连续受伤、失稳后治疗、自动死豁/死亡/复活、1d4小时与完整状态/资源仍人工；
通用DamageEffect旧0HP门保持。M3未关闭：完整P09清单/含A08–A18的独立战役
重映射、容量、精确交付HEAD日志待办；整库独立PG恢复不代替总验收。云端/真机
后置。全部入口改动及云补充保留，无暂存/提交/推送/合并/发布，不操作dnd-roll-local
及数据/旧fence；全部分工/进度见work-packages.md顶部。

## 最终保留核对

a18-audit.log核验195份入口文件与备份hash：181份逐字节未改，14份仅本次接口/
生命周期接点和契约、交接、CHANGELOG的预期补充，17个新增文件均属于A18。
六份交接/计划文档及契约README的全部原字节保留；云补充CI文件逐字节未改。
旧157个OpenAPI operations逐项相等，旧schema仅A16状态枚举增加UNSTABLE，
新增3路由/5schema。旧迁移、通用DamageEffect及A07–A17后端实现均保持；原分支/
HEAD、空暂存区和本次六端口无监听核验通过。git diff --check只报告入口已有的
continuation/verification尾部空行，未清理用户原内容；新文件无空白错误。
