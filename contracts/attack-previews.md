A12 adds independent [dagger melee knockout](dagger-knockout-effects.md) without changing the club profile.

# A05 durable weapon previews, confirmation and correction

[A09 ammunition effects](ammunition-effects.md) define an additional opt-in shortbow
arrow-counter profile and dedicated confirmation route before existing APPLY.

[A08 damage effects](damage-effects.md) additionally support server-derived ordinary
damage that leaves positive HP, through explicit CONFIRM_DAMAGE and separate APPLY. The manual
CONFIRM/final-value semantics below remain available; unsupported effects remain manual.

The A04 matrix remains the calculation boundary. This adapter supports only the
bundled SRD 5.2.1 ATTACK entries with unchanged identity, source/licensing, name,
dice and damage type. It derives final hit/damage/AC calculations from current
cards. A DM explicitly reviews range, legality, features, conditions and applicable
factors; this review does not claim automatic detection of exceptions. Mastery,
unknown/changed entries, unreviewed context and declared exceptions require manual
P06 adjudication, with no partial candidate or rolls. No extension automation.

All preview writes and full reads are current-campaign-DM-only, including receipt
replay. The DM controls source and target through their campaign role. No client
AC, sheet snapshot, actor, dice values, totals or result is accepted. Strict JSON,
64KiB request bound, UUID identities and nonnegative versions apply.

`GET /api/campaigns/{campaignId}/attack-previews`
`POST /api/campaigns/{campaignId}/attack-previews`
`GET /api/campaigns/{campaignId}/attack-previews/{id}`
`POST /api/campaigns/{campaignId}/attack-previews/{id}/events`
`GET /api/campaigns/{campaignId}/attack-results`

Create: `{requestId,characterId,sourceVersion,targetId,targetVersion,attackId,
reviewed,exceptions,advantage,disadvantage,immunities,resistances,vulnerabilities}`.
Factor lists contain `{reason,source}` using A04 source bounds, at most 10 per list.
The server verifies both versions and the trusted catalog entry, then creates
immutable P05 DM attack dice and, on a hit, independent exact planned damage dice
(including critical extra dice). It saves full card/catalog/calculation/roll
snapshots, review, resolution and SHA-256 evidence hash. A private P06 action links
these rolls. Misses have no damage roll. MANUAL_REQUIRED saves reasons but no
action/rolls and cannot be confirmed. Nothing spends resources or changes cards.
The full factor reasons remain in immutable review/calculation evidence; P05 roll
labels retain their existing 200-code-point bound and use a bounded prefix only.

Command: `{requestId,expectedVersion,kind,previewHash,changes,correctionOf,reason,
summary}`. Kinds CONFIRM, APPLY, CORRECT, PUBLISH. Irrelevant fields must be empty.
Each successful command appends an immutable event with actual actor/time and
advances this preview version once. Create returns `{id,version}`; commands return
the original `{id,version}` receipt. Reads return current state, never receipt data.

CONFIRM pins the exact evidence hash, original source/target versions and explicit
P06 final-value changes (1..2 distinct source/target characters, current versions,
HP/temp HP and selected resource values). A nonempty DM reason is required. These
are DM final values; damage is guidance, not an implicit HP/resource command.
0HP/death, conditions, concentration and all unsupported effects stay manual.
APPLY requires that confirmation and both original card versions still match;
it atomically appends P06 verdict/application, all card histories and changes,
preview event and receipt. One APPLY only. Changes to either card after creation
require a new preview; refresh cannot rebase an old confirmation or reroll it.
The generated private P06 action must also remain at its original linked-roll
version before both CONFIRM and APPLY. An independent manual verdict/link/decision
invalidates that preview, even if neither card changed. Historical matching A05
receipts still return their original acknowledgments without applying again.

CORRECT after application references an existing APPLY/CORRECT preview event and
supplies explicit *current-version* final values and a reason. It uses P06 linked
correction, preserving the original rolls, evidence, events and later independent
edits; it never restores a stale sheet or blindly reverses an old delta.

PUBLISH after application separately confirms a 1..1000 character plain-text
summary. It never publishes dice, AC, HP/resources, private reasons, hash, full
versions or source evidence. The player results route filters current membership
and source/target visibility before the latest 50 published summaries, returning
only `{id,name,targetName,summary,actor,at}`. Hidden/import-review characters remove
even historical summaries. Publishing neither rerolls nor changes roll visibility;
later corrections require a separate publish to update the public summary.

All writes lock actor then campaign in the existing transaction, reauthorize before
receipts, bind route/campaign/full body, and use durable actor/request idempotency.
Same key/body returns the original ack after later edits without replaying effects;
different body/route/campaign conflicts. Versions are checked after receipt replay.
Concurrent equal requests have one effect, competing DM versions have one winner.
Immediate/deferred commit failures roll back rolls/actions/cards/history/preview/
receipt together. Unknown transport outcome retains the exact request for retry.
All responses/errors are `Cache-Control: no-store, private`; realtime only invalidates.

Chinese DM controls must expand evidence, distinguish preview/confirmed/applied,
require separate explicit application/publication and support linked current-value
correction. Uncertain writes lock edits and retain route/id/body/hash/versions;
confirmed writes with failed reads offer refresh only. Campaign/API/role switches
and late authorization failures clear private state without accepting late results.
Acceptance includes offline units, real PG/HTTP authorization/races/rollback,
V1–V23 preservation, production browser, actual restart and isolated backup restore.
Cloud and physical-phone verification remain deferred.
