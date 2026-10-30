# A24d 已识别 JSON 重绑定与 hash 来源矩阵（2026-10-31）

本片顺延 A24c，只处理其已声明 A08–A23 records，不是新团 writer。不新增规则判断，
不改 A08–A23 原来源门；5R 效果仍由原 Effect 支持矩阵约束。以下是静态来源核查，
不是执行通过声明，实际 RED/GREEN 和原生验收由 a24d-tdd.md 记录。

## 持久边界

实际适配类型与 SQL 入口沿用 [A24c TYPES](../../backend/src/main/java/io/dndroll/backup/CampaignEvidenceGraph.java:15)
和 [reader](../../backend/src/main/java/io/dndroll/backup/CampaignEvidenceGraphCommand.java:13)：
当前 Card、character_history.before_state/after_state、dice_roll.data、combat_action.data、
attack_preview.data、A16/A19 state/history.snapshot 与六类 healing/damage Entry.snapshot。
Card 当前值由关系列、控制者列表及 sheet 重组；历史版本不能以当前卡替代。
character_history_before 是 reader 内部适配器，所属 SQL 行 version-1 才是其版本依据。

所有输入须先过 A24a 原生档案/schema/76表/资产核验和 A24c 完整来源门。未知 JSON
字段、record shape、kind/status、缺历史或错 owner 继续阻断。只读转换不复制数据库
行、源回执、文件或账号，不变更源写栅栏。

## 身份、路径与保留字段

| Record / 明确路径 | 策略 |
| --- | --- |
| Card.id/campaignId；History 内 before/after | character_card 与显式新 campaign 映射；version、hidden/shareFull/importReview、controllers 保留为历史证据，controllers 不授予新目标权限 |
| Roll.id；Roll.characterId | dice_roll 映射；非空 characterId 明确解析角色引用；sourceVersion、actor/name/at/visibility/key/calculation/结果及骰列表保留，不重掷 |
| Action.id/characterId/targetId | combat_action/character_card 映射；sourceVersion/publicVersion/version 与 attack 本地配置及显示名称保留 |
| Combat Event.id/correctionOf | 按所属 action 的 combat_event namespace；roll 与 Applied.before/after 走明确类型映射 |
| Preview.id；Evidence.actionId/source/target/attackRoll/damageRoll | attack_preview/combat_action/角色/骰分别映射 |
| Evidence.review.characterId/targetId | 角色引用；sourceVersion/targetVersion 保留，attackId 是本地键；requestId 是源审核 provenance，保留不转新幂等命令 |
| Preview Event.id/correctionOf/combatEventId | event 属所属 preview；correctionOf 同 owner；combatEventId 属 Evidence.actionId 内事件；ROLL_KNOCKOUT_FIRST_AID.id 硬 alias 到骰 |
| Change.characterId/expectedVersion/resources[].id | 角色引用映射，角色版本数字保留；资源 id 是角色本地键，即使像 UUID 也保留 |
| A16/A19 State.id | target Card 别名，不能生成独立 state UUID |
| A16/A19 Attempt.id/roll.id；Review.attemptId；Entry.attemptId | 同一专用 dice_roll alias；A16/A19 不互借骰或账本 |
| 六类 Entry.id/before/after/review | id 是 target；Card 与 Review 引用按明确字段映射，其余金额/critical/reviewed/reason/actor/时间保留 |

Source、ContentEntry 及内容身份、CharacterSheet attacks/resources/casting/spells/skills 本地键、
自由文本 name/reason/summary/description/notes、Calculation 内容和未知 UUID 样式字符串
不扫描替换。历史 actor/completion.actor/controllers 只保留证据，不认定为新目标授权。
资产 URL/头像、权限和模组/opaque 内容图仍需后续适配，不能把本片通过当完整复制支持。

## 版本路径

| 路径 | 原语义；数字保留时仍需证明闭包 |
| --- | --- |
| Card.version；Roll.sourceVersion；Create.sourceVersion/targetVersion；Change.expectedVersion | 角色历史版本，与账本不同 |
| A16 State.version；A17/A18/A23 Entry.version | 共享 stabilization_history；Entry.version 是结果账本版本 |
| A19 State.version；A20/A21/A22 Entry.version | 独立 restabilization_history；不能套共享版本 |
| A19 Attempt.originVersion | stabilization_damage_history 的原 A18 版本 |
| A20/A21/A22 Entry.originVersion | stabilization_history 的共享结果版本；review.originVersion 是前一版本 |
| A22 damageVersion | restabilization_damage_history 的 A21 版本 |
| A23 damageVersion | stabilization_damage_history 的 A18 版本 |

真实 JDBC 与复合 FK 来源：
[A16 store](../../backend/src/main/java/io/dndroll/persistence/JdbcStabilizationStore.java:11)、
[A19 store](../../backend/src/main/java/io/dndroll/persistence/JdbcRestabilizationStore.java:10)、
[A22 store](../../backend/src/main/java/io/dndroll/persistence/JdbcInstabilityHealingStore.java:10)、
[A23 store](../../backend/src/main/java/io/dndroll/persistence/JdbcInitialInstabilityHealingStore.java:10)、
[V27](../../backend/src/main/resources/db/migration/V27__stabilization_attempts.sql)、
[V30](../../backend/src/main/resources/db/migration/V30__restabilization_attempts.sql)、
[V31](../../backend/src/main/resources/db/migration/V31__restabilization_healing_history.sql)、
[V32](../../backend/src/main/resources/db/migration/V32__restabilization_damage_history.sql)、
[V33](../../backend/src/main/resources/db/migration/V33__instability_healing_history.sql)、
[V34](../../backend/src/main/resources/db/migration/V34__initial_instability_healing_history.sql)。

## 原 hash 算法与完整输入

`H(values)` 为 `DiceService.fingerprint(json, values)`：以当前 record JSON 序列化字符串
UTF-8 字节计算 SHA-256、小写 hex；不对 JSON object 键另做 canonical 排序。
应用 `fp(Object...)` 把参数包成 `Arrays.asList`，因此单个 Evidence 也有外层单元素数组。
[原算法](../../backend/src/main/java/io/dndroll/application/DiceService.java:122)。

| 持久字段 / 可重建值 | 精确原 tuple，顺序不可改变 |
| --- | --- |
| Preview.hash | H([完整 Evidence])；不是 H(Evidence)，不绑定 Preview.id/events/status |
| A17 Entry.planHash | H(["A17_PLAN", campaign, 共享 before State, before Card, 原 Review, 原 Effect.Plan]) |
| A18 Entry.planHash | H(["A18_PLAN", campaign, 共享 before State, before Card, 原 Review, 原 Effect.Plan]) |
| A20 Entry.planHash | H(["A20_PLAN", campaign, 独立 before State, 共享 before State, before Card, 原 Review, 原 Effect.Plan]) |
| A21 Entry.planHash | H(["A21_PLAN", campaign, 独立 before State, 共享 before State, before Card, 原 Review, 原 Effect.Plan]) |
| A22 Entry.planHash | H(["A22_PLAN", campaign, 独立 before State, 共享 before State, before Card, 完整原 A21 Entry, 原 Review, 原 Effect.Plan]) |
| A23 Entry.planHash | H(["A23_PLAN", campaign, 共享 before State, before Card, 完整原 A18 Entry, 原 Review, 原 Effect.Plan]) |
| A16 临时 Plan.hash | H(["A16_PLAN", campaign, State, target Card, helper Card, Review, Effect.Plan]) |
| A19 临时 Plan.hash | H(["A19_PLAN", campaign, 独立 State, 共享 State, target Card, helper Card, Review, Effect.Plan]) |

来源：
[Preview](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:94)、
[A16](../../backend/src/main/java/io/dndroll/application/StabilizationService.java:51)、
[A17](../../backend/src/main/java/io/dndroll/application/StabilizationHealingService.java:36)、
[A18](../../backend/src/main/java/io/dndroll/application/StabilizationDamageService.java:36)、
[A19](../../backend/src/main/java/io/dndroll/application/RestabilizationService.java:52)、
[A20](../../backend/src/main/java/io/dndroll/application/RestabilizationHealingService.java:41)、
[A21](../../backend/src/main/java/io/dndroll/application/RestabilizationDamageService.java:41)、
[A22](../../backend/src/main/java/io/dndroll/application/InstabilityHealingService.java:59)、
[A23](../../backend/src/main/java/io/dndroll/application/InitialInstabilityHealingService.java:42)。

每个持久旧 hash 先以完整源 tuple 核验，再在全部字段转换后按原算法重算新 hash。
A22/A23 必须先重生所依赖 A21/A18 Entry 的 planHash，然后把整份新 Entry 纳入新 tuple。
不能沿用旧嵌套 hash；不能以字符串替换 hash；不能把新证据 hash 当新目标确认。
Effect.Plan 必须按原服务相同的 canonical/unchanged/bound/真实 total/reviewed 来源重建，
不能把当前 before.conditions 直接冒认 canonical 或无条件传 true 绕过原门。

A13–A15 临时计划的原 tuple 分别是 `[preview.id, preview.version, preview.hash, target,
review, effect]`，A15 再含 helper 与 roll。它们的 planHash 不在 Preview.Event 中保存，
确认请求只在源回执范围；本片不从 summary/reason 猜测缺失 review，也不复制或认领
这些幂等回执。[A13](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:235)、
[A14](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:267)、
[A15](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:322)。
A16/A19 临时 Plan 本身也未持久化，不把它们称为已复制的新目标方案。

CharacterService 的回执 fingerprint 另有递归排序 object key 的 canonical JSON 步骤；
不能用 DiceService 算法代替。由于源 character receipts 留源，本片不重生它们。
[canonical 算法](../../backend/src/main/java/io/dndroll/application/CharacterService.java:110)。

## Canonical 注记及回执

A10/A11/A12 的三类 Condition 由正确 preview owner 与其 target 重建；A16 两类由原
attempt/roll 和真实 total、原 reviewed 重建；A19 只新建 a19-stable，原 a16-unconscious
仍属于原 A16 owner。须比较整份原 Condition（含 Source/description 等）再重建完整新值，
不能只识别前缀/后缀。未知普通条件保持；伪装已知前缀、错 owner/target、非 canonical
全文继续阻断。[A24c owner 门](../../backend/src/main/java/io/dndroll/backup/CampaignEvidenceGraph.java:81)、
[A16 Effect](../../backend/src/main/java/io/dndroll/domain/StabilizationEffect.java:19)、
[A19 Effect](../../backend/src/main/java/io/dndroll/domain/RestabilizationEffect.java:8)。

A15/A16/A19 dice receipt fingerprint 与 actor 继续只读核源。新 target 数据若要进行图检查，
不得伪造新 receipt 来满足来源门；映射证明与源回执证明必须区分。源 requestId/actor
和 old planHash 不构成新目标授权。CAMPAIGN_REMAP 始终 BLOCKED/executable=false；
权限/文件/模组图、隔离事务 writer、独立新团重新预览确认与续办仍待。

## 本片实现复用与复核边界

CampaignEvidenceRebind 以明确 class/field 的 UUID_FIELDS 表执行身份转换；String 引用只
处理 Roll.characterId 和已知事件 correctionOf/combatEventId，A15 correction 另查被引用
事件是否是骰 alias。canonical Condition 先通过 A24c 的源全文/owner/target 检查，再用
原 Effect 构造新 owner 对应的完整 Condition；固定 Source 不改。

历史 hash 复用原服务的只读 plan 计算路径，隔离 Store 仅提供历史、骰与读取所需的
角色能力；任何写方法拒绝。通过反射调用 private plan 是为复用现行来源判断，不是
公开执行入口。读取的 state 来自该条 Review.expectedVersion；共享 source 来自
Review.originVersion，不使用当前 latest 冒充旧时点。A18/A21 新 journal hash 先完成，
之后 A23/A22 从同一新证据集合读取完整新 journal。无目标回执被伪造或持久化。

只读审查不替代主线的完整单测、真实 PG 专项、16 原生历史与后审计结果，也不构成
新团授权。具体日志与未执行项以本片 TDD/verification 为准。
