# A10 bounded monster death effect

[5R source/support matrix](../docs/research/monster-death-5r.md). This separate opt-in
profile does not relax A08/A09 zero-HP gates or change existing evidence hashes.

`POST /api/campaigns/{campaignId}/attack-previews/{id}/monster-death-effect`
`POST /api/campaigns/{campaignId}/attack-previews/{id}/monster-death-confirmations`

Read body: `{expectedVersion,previewHash,treatment,reviewed}`. Treatment is exactly
`MONSTER_DIES_AT_ZERO` or `MANUAL`; reviewed is an explicit boolean. Confirmation
adds `{requestId,reason}`. No extra, duplicate, missing, null or client-final fields.
Reason is nonempty, at most 1000 codepoints; hash is 64 lowercase hex characters.
Current campaign DM, source/target existence precede any historical receipt replay.
Reads check PREVIEW/CONFIRMED, exact preview version/hash and original source,
target and linked action versions under campaign lock. Responses/errors are private,
no-store. Reads never write or roll. Confirmation requires PREVIEW, same checks,
and a CANDIDATE; fallback returns 409 with no writes. Current permission precedes
replay; same actor/key/body/route replays exactly, different body/route conflicts.

Plan `{profile,status,reasons,hp,tempHp,absorbed,hpLoss}` uses profile
`5R-SRD-5.2.1-MONSTER-DEATH-V1`. CANDIDATE derives only from immutable saved final
damage: absorbed=min(tempHp,damage), hpLoss=original hp, hp=tempHp=0. MANUAL_REQUIRED
returns reasons with all four numbers null. Original hp must be positive, loss
after tempHP must reach hp, target conditions empty and death saves both zero;
source/target must differ. Explicit monster treatment and review are mandatory.
Review attests no knockout/character-treatment choice, special survival/death
trigger, unresolved concentration/reaction/resource or other linked exception.

Confirmation saves one `CONFIRM_MONSTER_DEATH` event with the server target change;
it does not change cards. Its kind permanently encodes this profile and the review
attestation; the generic events route rejects this kind. APPLY remains separate.
For this confirmation only, APPLY uses one P06 transaction to write HP/tempHP and
a deterministic target condition-container annotation `a10-death:<preview UUID>`,
name `死亡（记录）`, SRD source, no expiry. This is a death annotation, not a new
5R condition. Card version advances once; P06 before/after history includes the
annotation. Source, resources, death-save counts and all other fields stay intact.
Preview, action, card, histories and receipts commit or roll back together, even on
deferred failure. Same-key races have one effect, competing previews one winner.

Do not delete tokens, change initiative, automatically disable actions, resurrect,
or infer PC/monster from names/visibility/controllers. CORRECT retains explicit
current-value semantics and keeps the death annotation; removal requires a separate
DM condition operation. Historical APPLY never restores a removed annotation.
PUBLISH remains independent; private evidence is never added to player payloads.
Existing visible-card condition summaries can show the annotation like other marks.

Chinese UI requires treatment selection, exception review and reason, reads the
authoritative plan, then separately confirms/applies. Scope/revision/input changes
clear plans and stale responses; current-scope late 401/403 clears private state.
Uncertain confirmation retains exact route/body/key and blocks competing mutations;
ack plus read failure offers read-only recovery. Desktop/390px browser checks do
not constitute real-device validation. Full recovery/cloud/device gates remain later.
