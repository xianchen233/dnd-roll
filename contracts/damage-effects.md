# A08 bounded damage effects

[A09](ammunition-effects.md) adds a separate opt-in shortbow arrow-counter profile.
The DAMAGE-HP-V1 confirmation and resource-preservation semantics here remain fixed.

Rules and exclusions: [5R support matrix](../docs/research/damage-effects-5r.md).
Extends A05 using the existing private preview/event/receipt transaction; no migration,
policy switch, new content admission or automatic public action execution.

`GET /api/campaigns/{campaignId}/attack-previews/{id}/damage-effect`

Current campaign DM only, current source/target existence and original source, target
and linked P06 action versions required. Only PREVIEW/CONFIRMED are available; stale
or applied/manual previews return 409. Checks run under the campaign lock. All success
and error responses are `no-store, private`. Read-only: no dice, receipts or writes.

Returns `{profile,status,reasons,hp,tempHp,absorbed,hpLoss}`. Profile is exactly
`5R-SRD-5.2.1-DAMAGE-HP-V1`. CANDIDATE has empty reasons and nonnegative final values;
MANUAL_REQUIRED has reasons and all four numeric fields null. HP already zero,
post-damage HP<=0, any target condition instance or nonzero death-save record require
manual resolution. The source is the saved final A04 damage after defenses, never a
client amount. A05 already pinned trusted catalog/source and DM review. Zero damage
and misses yield unchanged values. No resource, concentration, condition, death,
healing, granted temporary HP, ammo or action-economy automation is implied.

Existing command shape adds `kind: CONFIRM_DAMAGE`. `changes`, `summary` and
`correctionOf` must be empty; hash, version and nonempty reason remain mandatory.
This kind permanently identifies DAMAGE-HP-V1; future effect profiles require a
separate versioned contract rather than changing historical confirmation semantics.
This explicit command attests that the DM reviewed concentration, reactions and all
other damage-linked exceptions and has no unresolved linked effect. The server
recomputes eligibility and exact final values from immutable evidence and current
versions. MANUAL_REQUIRED returns 409 without any write. Never accept client final
values on this command. It saves one CONFIRM_DAMAGE event containing one server-built
target change with original version and empty resources (preserve all resources),
then becomes CONFIRMED; cards remain untouched until a separate APPLY.

APPLY accepts exactly the saved CONFIRM or CONFIRM_DAMAGE changes, uses the existing
P06 atomic application/history/receipt and retains source/target/action version
checks. Same actor/key/body replays the original ack after later edits without a
second effect; current DM authorization precedes replay. Body/route/campaign changes
conflict. Concurrent confirmations have one winner; immediate/deferred failures roll
back all changes. CORRECT remains linked explicit current final values. PUBLISH and
player summaries remain separate and unchanged. Existing persisted previews, hashes,
events, manual CONFIRM and all prior routes retain their meaning.

Chinese controls request the server plan, show final HP/temp HP and absorbed/HP loss,
then require a separate exception review and reason for CONFIRM_DAMAGE. Manual final
values stay available. A new selection/revision clears the plan/review. API/campaign/
role switches and late 401/403 clear private state. Uncertain writes retain the exact
body/key/hash/version; an ack followed by failed read offers read-only recovery.
