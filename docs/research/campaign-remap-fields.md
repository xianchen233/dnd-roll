# A24b 战役 ID 重映射字段盘点（2026-10-31）

本文是当前代码的静态证据与拒绝矩阵，不是已执行的迁移计划，也不证明新团可续办。
本轮没有新增规则效果；沿用 A08–A23 已查证的 5R 行为与各自来源门。
同 ID P09 恢复和 A03h 定义恢复的区别见[恢复矩阵](../m3-recovery-matrix.md)。
字段路径以 Java record/JDBC 的真实 JSON 字段为准，`[]` 表示明确集合成员，不能
扩展为递归搜索所有 UUID 字符串。列名与同义 JSON 字段必须一起核验。

## 身份与版本 namespace

| 对象 | 身份 / 引用路径 | 绑定与注意事项 |
| --- | --- | --- |
| 战役 | 各表 `campaign_id`；嵌入 Card 的 `campaignId` | `character_history` 以 character_id 关联，并非每表都有 campaign_id；不得只按 campaign_id 列筛选后宣布闭包完整 |
| 角色 | character_card.id；Card.id；preview.evidence.review.characterId/targetId；evidence.source/target.id；Combat Action.characterId/targetId | 同一 UUID 在快照、历史、关系列中保持同一映射；角色当前/历史版本不可混为一个对象 |
| 角色版本 | Card.version；Dice Roll.sourceVersion；Create.sourceVersion/targetVersion；Change.expectedVersion | namespace 是角色身份与版本的二元组；新团即便保留版本数字也要显式证明历史链和 before/after 一致 |
| 攻击预览 | attack_preview.id；Preview.id；Condition.id 中经识别的 preview 后缀 | Preview.version 与角色 version 分开；Preview.hash 不是对象 ID |
| 战斗行动/事件 | Evidence.actionId；Action.id；Event.id；Event.correctionOf；Preview.Event.combatEventId | correctionOf/combatEventId 是 String，但有明确事件语义；不能因为不是 UUID 类型而遗漏 |
| 骰 | Roll.id；Roll.characterId（String）；Evidence.attackRoll/damageRoll；Combat Event.roll；A15 Event.id | 不能重掷；骰结果/calculation/sourceVersion/visibility/actor/时间是证据，需与引用逐项一致 |
| A16 Attempt | State.latest.id == State.latest.roll.id；Plan.rollId 是同一骰 ID | Attempt.id 来自 `roll.id()`，不能给 attempt 和 roll 各造一个不相等的 ID |
| A19 Attempt | 同上，但独立 A19 专用骰 | A19 与 A16 骰 namespace 和来源版本不同；不能因都是 skill:medicine 就相互认领 |
| 稳定账本 | State.id == target card id；State.version | `State.id` 不是额外生成的状态身份。A16 与 A19 以同一 target_id 使用两本独立版本账本 |
| 账号/操作者 | actor、completion.actor、Card.controllers[]、各 controller.username | 是 app_user.username 权限引用，不按 UUID 映射；源 actor 历史与新目标授权另行审核 |

证据：[CharacterService records](../../backend/src/main/java/io/dndroll/application/CharacterService.java:16)、
[AttackPreview records](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:10)、
[Combat records](../../backend/src/main/java/io/dndroll/application/CombatService.java:14)、
[Dice records/专用骰](../../backend/src/main/java/io/dndroll/application/DiceService.java:14)、
[A16 roll](../../backend/src/main/java/io/dndroll/application/StabilizationService.java:40)、
[A19 roll](../../backend/src/main/java/io/dndroll/application/RestabilizationService.java:44)。

## A08–A15：同一个攻击来源图，不能只换顶层预览 ID

时间线还存在 SQL 硬 alias：`timeline_entry.id` 与其唯一 `message_id`、`roll_id` 或
`action_id` 相同。RP/OOC 由 JdbcTimelineStore.save 显式复用消息 ID，骰/行动由 V14
触发器复用来源 ID；TimelineService.projection 又直接按 `row.id` 查询对应来源。
因此时间线目标 ID 必须复用 canonical owner 的映射，不能因 namespace 不同而
生成独立 UUID。将来 writer 还需处理自动生成条目的触发器，不能重复插入。
证据：[V14 source trigger](../../backend/src/main/resources/db/migration/V14__timeline_images_and_sessions.sql:43)、
[RP timeline save](../../backend/src/main/java/io/dndroll/persistence/JdbcTimelineStore.java:20)、
[projection](../../backend/src/main/java/io/dndroll/application/TimelineService.java:72)。

`attack_preview.data` 是完整 Preview：`id/version/status/actor/at/evidence/hash/events[]`。
其中 `evidence.review` 保存 Create（包括 requestId），`source` 和 `target` 保存完整
CharacterService.Card，不是只有卡 ID。`entry` 是 ContentEntry；`attackRoll`、
`damageRoll` 是完整 Roll；`actionId` 指向 combat_action。
关系列 `character_id/target_id` 与 JSON evidence.source/target.id 一起核验。
[JDBC](../../backend/src/main/java/io/dndroll/persistence/JdbcAttackPreviewStore.java:15)
读写整份 JSON，[V24](../../backend/src/main/resources/db/migration/V24__attack_previews.sql:1)
有两个 campaign+character FK；不能只修 FK 后忽略 JSON。

`events[].changes[].characterId` 指角色，`changes[].resources[].id` 是角色本地资源键；
`events[].id` 通常是预览事件，但 kind=ROLL_KNOCKOUT_FIRST_AID 时直接等于专用骰 ID。
`events[].correctionOf` 指原预览事件，`combatEventId` 指关联行动内事件，必须区分。
A15 检定事件的 `changes[0]` 是 helper，`changes[1]` 是 target，并保存双方版本；
其 `id` 与所读骰的 id/actor、Roll.characterId/sourceVersion/calculation 一起核对。
`events[].summary` 中可以显示骰 ID，但它是显示文本，不是未来 mapper 的通用引用字段。
[A15 binding](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:286)
明确检查最后一次对应事件；不能把任意历史骰改名后当作当前检查。

AttackPreviewService.child 派生请求键使用 `UUID.nameUUIDFromBytes("A05:" + request + ":" + purpose)`，
purpose 包含 attack/damage/action/link-attack/link-damage/first-aid/verdict/apply/correct。
这些是幂等请求的派生 namespace，不是所有对象的身份推导规则，不能拿新目标根请求
伪造源回执。[代码](../../backend/src/main/java/io/dndroll/application/AttackPreviewService.java:54)

## A16–A23：JSON 与关系版本的交叉绑定

| 表 / JSON | 关系键或 JSON 路径 | 必须核验的来源 |
| --- | --- | --- |
| stabilization_state / stabilization_history.snapshot | campaign_id,target_id,version；State.id/version/latest | latest.target/helper 是完整历史 Card；latest.roll 完整历史骰；latest.completion 是独立完成元数据 |
| stabilization_healing_history.snapshot（A17） | Entry.id/version/attemptId/review/planHash/before/after | Entry.id 是 target；version 同时 FK 到 stabilization_history 的**结果版本**，不是普通卡版本 |
| stabilization_damage_history.snapshot（A18） | 同上，Review 含 amount/critical/reviewed | result version FK 到 stabilization_history；后续来源重建用之前 APPLIED 状态和原伤害日志/planHash |
| restabilization_state / restabilization_history.snapshot（A19） | campaign_id,target_id,version,origin_version；State.latest.originVersion | origin_version FK 到 stabilization_damage_history；A19 当前 version 独立于共享 A16 version |
| restabilization_healing_history.snapshot（A20） | Entry.id/version/originVersion/attemptId/review/planHash/before/after | version FK 到 A19 history；origin_version FK 到共享 stabilization_history **结果版本** |
| restabilization_damage_history.snapshot（A21） | 与 A20 相同，Review 含 critical | 两本结果历史版本均需一致；不能把 originVersion 误当角色版本 |
| instability_healing_history.snapshot（A22） | Entry.id/version/originVersion/damageVersion/attemptId/review/planHash/before/after | damage_version FK 到 A21 日志；version FK A19 history；origin_version FK 共享 history |
| initial_instability_healing_history.snapshot（A23） | Entry.id/version/damageVersion/attemptId/review/planHash/before/after | damage_version FK 到 A18 日志；version FK 共享 history；未应用 A19 历史/骰仍可同时存在 |

V27–V34 定义了这些复合 FK：[V27](../../backend/src/main/resources/db/migration/V27__stabilization_attempts.sql:1)、
[V30](../../backend/src/main/resources/db/migration/V30__restabilization_attempts.sql:1)、
[V31](../../backend/src/main/resources/db/migration/V31__restabilization_healing_history.sql:1)、
[V32](../../backend/src/main/resources/db/migration/V32__restabilization_damage_history.sql:1)、
[V33](../../backend/src/main/resources/db/migration/V33__instability_healing_history.sql:1)、
[V34](../../backend/src/main/resources/db/migration/V34__initial_instability_healing_history.sql:1)。
版本数字本身不是可任意重分配的全局 ID。保留数字的计划仍须同时验证 `(ledger,target,version)`
闭包、共享历史和独立历史及 Entry JSON 一致；更改数字则要求明确的逐账本映射。

## Canonical annotations、固定键与 hash

| 字段 | 当前格式与策略 |
| --- | --- |
| A10 Condition.id | `a10-death:<preview UUID>`；经确认是对应原预览的规范记录才重建 |
| A11 / A12 Condition.id | `a11-knockout:<preview UUID>` / `a12-dagger-knockout:<preview UUID>`；A13–A15只移除各自原记录 |
| A16 Condition.id | `a16-stable:<attempt/roll UUID>` 与 `a16-unconscious:<同一 UUID>` |
| A19 Condition.id | 新 `a19-stable:<A19 attempt/roll UUID>`，保留原 A16 unconscious 的身份；不能把两者的后缀同时替换成新 A19 骰 |
| Condition.source | A16/A19 是固定书目/修订/5R/profile/页码；Source.detail 并不含 attempt UUID，应原样保留 |
| CharacterSheet 本地键 | attacks[].id、resources[].id、casting[].id、spells[].castingId、skills[].id 等属于角色内键；即使字符串像 UUID 也不自动变成运行时身份 |
| ContentEntry 与 source | 内容条目身份、book/revision/rules/profile、许可与归属是固定来源身份；战役定制内容是否复制成新 entry 必须单独策略，不能一律重写 UUID |

来源：[A10](../../backend/src/main/java/io/dndroll/domain/MonsterDeathEffect.java:26)、
[A11](../../backend/src/main/java/io/dndroll/domain/KnockoutEffect.java)、
[A12](../../backend/src/main/java/io/dndroll/domain/DaggerKnockoutEffect.java:18)、
[A16](../../backend/src/main/java/io/dndroll/domain/StabilizationEffect.java:38)、
[A19](../../backend/src/main/java/io/dndroll/domain/RestabilizationEffect.java:16)、
[卡字段](../../backend/src/main/java/io/dndroll/domain/CharacterSheet.java:14)。

hash/fingerprint 必须经原算法与完整新证据重算，并且旧值只保留为来源证明；不能通过
替换 hash 中字符“修复”。DiceService.fingerprint 是 JSON SHA-256；CharacterService
还有 canonical JSON 过程。A16_PLAN/A17_PLAN/A18_PLAN/A19_PLAN/A20_PLAN/A21_PLAN/A22_PLAN/
A23_PLAN 的输入形状各异。A23 明确包括**整份原 A18 日志**；A22 包括**整份原 A21 日志**，
并重建更早的源计划以核原伤害。当前审核计划的 hash 不能当作新目标可执行确认。
未来 writer 即使能重建历史 hash，也必须由新目标重新预览/确认待办，禁止自动花费资源。
[A22 source/hash](../../backend/src/main/java/io/dndroll/application/InstabilityHealingService.java:54)、
[A23 source/hash](../../backend/src/main/java/io/dndroll/application/InitialInstabilityHealingService.java:38)。

## 回执、权限、自由文本和 opaque 内容

`stabilization_receipt` 的 PK 是 `(actor,request_id)`，跨 A16–A23 共用；result_id 指目标卡，
result_version 是确认返回的账本版本。attack_preview_receipt.result 是 Combat Ack JSON。
角色/战斗/骰/行动/移动等回执也不只是按 campaign 作为幂等命名空间；复制 requestId
并改 campaignId 会产生错误认领或冲突。源回执只留源，历史 ack 可作来源证明；新团
新命令使用新的幂等键，规划文档不能把“已映射历史”当作执行源命令的授权。
源 actor/completion.actor 与当前 DM 权限、controllers[] 是不同问题；重映射不授予账号权限。

reason、summary、description、notes、name、Source.detail、module markdown 等可能包含 UUID
样式片段；这些是自由文本或固定来源字段，不能逐字符串替换。已知资产 URL、头像、
内联引用和 module 包/归档是另一个有格式的解析任务；未识别格式必须阻断，不能以
“没有外键报错”推断安全。module_package 与 module_customization_archive 中的档案 bytes
不提供通用 UUID writer；其内部文件摘要/清单/版本也不能随意重写。

## 后续可执行顺序与拒绝矩阵

1. 先绑定受信原生档案摘要、34 migration checksums、76表schema与行摘要；源只读、目标
   权限单独审核，输出审阅计划明确标为不可应用。计划不能包含原始私密 Card/理由/口令。
2. 类型化身份表按 `(namespace, source ID)` 建立映射；目标不能等于任一源运行时身份，
   目标唯一性按实际 SQL PK 域审核。解决角色/历史版本、骰/attempt、行动/事件和注记闭包。
3. 每条关系必须带明确字段路径和期望对象种类；拒绝悬空、跨战役、重复或 namespace
   冲突、未覆盖 JSON 结构、未知 schema/opaque 内容、缺历史版本与改值/删约束。
4. 之后逐类实现 reader/mapper/hash 再生，再写隔离事务 writer；保留 source receipts，
   新目标重新审核权限/待办，不重掷、不自动应用。完整新团恢复及独立续办另做验收。

当前研究不构成全部 JSON blob 的完整字段适配器。76表分类只证明没有静默遗漏表；
movement/initiative/combat turn/RP/notes/module 的历史内部字段、未知自定义内容及图像
引用仍须独立类型化 reader/closure 测试。A24a 的 CAMPAIGN_REMAP BLOCKED 必须保持，
不能因一个身份映射表通过便宣布完整运行时恢复可应用。

## 76 张 public 表的当前显式分类

以下直接按 A24a catalog 的 remapPolicy 分组；分类不是 writer 支持声明。

| 策略 | 表数 | 表 |
| --- | --- | --- |
| ACCESS_REVIEW_REQUIRED | 4 | campaign_invitation, campaign_member, character_controller, token_controller |
| AUTHORITY_KEEP_ACCOUNTS | 1 | app_user |
| IMMUTABLE_ASSET_COPY_REQUIRED | 3 | campaign_asset, note_attachment, rp_attachment |
| OPAQUE_ARCHIVE_REVIEW_REQUIRED | 2 | module_customization_archive, module_package |
| RECEIPTS_STAY_WITH_SOURCE | 20 | account_command_receipt, asset_command_receipt, attack_preview_receipt, character_command_receipt, combat_command_receipt, content_command_receipt, dice_command_receipt, fog_command_receipt, initiative_command_receipt, module_command_receipt, module_lifecycle_receipt, module_operation_binding_receipt, module_operation_receipt, movement_path_receipt, movement_receipt, note_command_receipt, scene_command_receipt, stabilization_receipt, timeline_command_receipt, token_command_receipt |
| SCHEMA_METADATA_KEEP | 1 | flyway_schema_history |
| TYPED_CHARACTER_REBIND_REQUIRED | 1 | character_card |
| TYPED_EVIDENCE_REBIND_REQUIRED | 20 | attack_preview, character_history, combat_action, combat_turn_history, dice_roll, initial_instability_healing_history, initiative_history, instability_healing_history, module_operation_binding_history, module_operation_execution, note_history, restabilization_damage_history, restabilization_healing_history, restabilization_history, restabilization_state, rp_message_history, stabilization_damage_history, stabilization_healing_history, stabilization_history, stabilization_state |
| TYPED_RUNTIME_REMAP_REQUIRED | 24 | campaign, campaign_combat, campaign_content_entry, campaign_content_settings, campaign_initiative, campaign_note, module_customization, module_customization_update, module_instance, module_instance_migration, module_instance_update, module_operation, module_operation_binding, movement_budget, movement_journal, note_attachment_reference, rp_attachment_reference, rp_message, scene, scene_map, scene_revealed_cell, timeline_entry, timeline_session, token |
