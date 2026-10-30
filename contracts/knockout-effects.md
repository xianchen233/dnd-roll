# A11 bounded melee knockout record

[5R source/support matrix](../docs/research/knockout-effects-5r.md). Independent opt-in
profile; A08/A09/A10 and original evidence/hashes/receipts retain their meanings.

`POST /api/campaigns/{campaignId}/attack-previews/{id}/knockout-effect`
`POST /api/campaigns/{campaignId}/attack-previews/{id}/knockout-confirmations`

Read body `{expectedVersion,previewHash,treatment,reviewed}`; treatment is exactly
`KNOCK_OUT_MELEE` or `MANUAL`. Confirmation adds `{requestId,reason}`. All fields
required, no null/extra/duplicate/client final fields. Hash: 64 lowercase hex;
reason: nonempty, at most 1000 codepoints. Current campaign DM and source/target
existence precede replay. No-store/private success and errors. Read checks original
card/action versions and exact preview version/hash under campaign lock, status
PREVIEW or CONFIRMED. No writes/dice. Confirm additionally requires PREVIEW and
CANDIDATE, persists CONFIRM_KNOCKOUT, changes no card. Same actor/key/body/route
replays exact ack; changed body/route conflicts; current permission always rechecked.

Plan `{profile,status,reasons,hp,tempHp,absorbed,hpLoss}` uses
`5R-SRD-5.2.1-KNOCKOUT-V1`. Only canonical builtin club/STR/no mastery, explicit
KNOCK_OUT_MELEE choice and reviewed=true qualify. Review attests actual melee use,
no immunity/survival/resource/linked exception, and manual management of unconscious
consequences, immediate short-rest start, healing/first-aid/rest-end removal.
Distinct source/target, original target HP>0, no target conditions/death saves,
saved final damage >= HP+tempHP. CANDIDATE: hp=1,tempHp=0,absorbed=original tempHP,
hpLoss=original HP-1 (possibly zero). MANUAL_REQUIRED: all four numbers null.
Unknown/mismatched catalog or ranged/thrown use is never inferred as supported.

CONFIRM_KNOCKOUT records the server target change; only the dedicated confirmation
route can create this kind. Separate APPLY adds deterministic condition-container
record `a11-knockout:<preview UUID>`, name `昏迷（击昏记录）`, SRD source and explicit
manual follow-up text, no numeric/wall-clock expiry. HP and record share one card
version, P06 before/after history, action/preview/receipts and one transaction.
Immediate and deferred errors roll everything back. Same-key races single effect;
two confirmed previews compete on original card version. Old replay never restores
a removed record. CORRECT changes explicit current numeric values and retains the
record; DM separately removes it after resolving wake-up/remaining Prone state.

This slice records knockout; it does not automate unconscious consequences,
short-rest lifecycle, healing, first aid, concentration, equipment drops, movement,
initiative or death saves. No inferred PC/monster classification. PUBLISH remains
separate; private evidence never enters player payloads. Normal visible-card status
summaries may show the record. Existing automatic damage profiles reject it.

Chinese UI requires explicit treatment, review and reason; server plan, separate
confirmation then APPLY. Changes/revision/scope invalidate plans, late current-scope
401/403 clears private state. Unknown confirmation preserves exact route/body/key,
blocks other writes and offers exact retry; confirmed write plus failed read only
offers read recovery. Desktop/390px checks are not physical device verification.
