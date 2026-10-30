# A12 bounded dagger melee knockout

[5R source/support matrix](../docs/research/dagger-knockout-5r.md).
The A11 club profile, endpoints, confirmation and annotations retain their meanings.

`POST /api/campaigns/{campaignId}/attack-previews/{id}/dagger-knockout-effect`
`POST /api/campaigns/{campaignId}/attack-previews/{id}/dagger-knockout-confirmations`

Read body `{expectedVersion,previewHash,treatment,reviewed}`; confirmation adds
`{requestId,reason}`. Treatment is KNOCK_OUT_MELEE, THROWN or MANUAL. All required,
no null/extra/duplicate/client final fields. THROWN and MANUAL return manual plans.
Current campaign DM and card existence precede receipt replay; source, target,
action and preview versions/hash checked under campaign lock. Private/no-store.
Read never writes or rolls. Confirm only PREVIEW+CANDIDATE, reason 1–1000 codepoints.
Same actor/key/body/route replays; changed body or route conflicts, including A11.

Plan has the A11 numeric/status shape with independent profile
`5R-SRD-5.2.1-DAGGER-KNOCKOUT-V1`. Only canonical builtin dagger/STR or DEX/no
mastery, explicit melee treatment and reviewed=true qualify. Review attests no
Light extra attack, Nick, Sneak Attack, poison, magic, resource or linked exception.
Original positive HP, distinct target, no target conditions/death saves and saved
damage >= HP+tempHP. Candidate hp=1,tempHp=0,absorbed=old tempHP,hpLoss=old HP-1;
manual plans have null numeric fields. No dagger inventory consumption.

Only dedicated confirmation creates CONFIRM_DAGGER_KNOCKOUT. Separate APPLY uses
P06's transaction to save numeric change and `a12-dagger-knockout:<preview UUID>`
with this profile's source, one card version/history and immutable receipts.
Immediate/deferred failure rolls back every write. Same-key concurrent retries
apply once; competing previews fail on original versions. Old replay never recreates
a removed annotation; numeric correction preserves it. Private evidence stays DM-only.

Chinese UI explicitly distinguishes melee from thrown, requires exception review
and reason, shows the server plan then separate confirmation/APPLY. Edit/revision/
scope clears plans; late current-scope 401/403 clears private state. Unknown write
retains exact body/route/key for retry and blocks other writes; acknowledged write
plus failed read offers read recovery. Saved confirmations retain wake-up guidance.
Rest, healing, first aid, unconscious consequences and remaining Prone stay manual.
