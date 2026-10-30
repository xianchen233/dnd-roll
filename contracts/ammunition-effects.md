# A09 shortbow ammunition counter and damage effects

[5R source/support matrix](../docs/research/ammunition-effects-5r.md) precedes this
contract. New bounded profile, not a change to A08 DAMAGE-HP-V1 or its confirmation.

`POST /api/campaigns/{campaignId}/attack-previews/{id}/ammunition-effect`

Read-only CSRF POST: `{expectedVersion,previewHash,resourceId,reviewed}`. Current DM,
current source/target existence, preview version/hash, original source/target and
linked P06 action versions are checked under the campaign lock. PREVIEW only.
Never creates dice, receipts, histories or writes. Strict fields/64KiB, nonnegative
integer version, exact SHA-256 and a nonempty resourceId up to 100 code points.

Response `{profile,status,reasons,resourceId,before,after,hp,tempHp}`. Profile is
`5R-SRD-5.2.1-SHORTBOW-AMMO-V1`. CANDIDATE has empty reasons and server-derived final
values; MANUAL_REQUIRED has reasons and all four numeric fields null. Candidate
requires exact trusted builtin shortbow identity, DEX, distinct source/target,
reviewed=true, source alive with no condition/death records, selected source CUSTOM
resource with current>=1 and A08-supported target damage. DM review attests that the
counter represents ordinary available arrows and that hands/range/legality and all
linked effects have no unresolved exception. Labels or source metadata alone never
establish counter semantics. After=before-1 even on a miss or zero final damage.

`POST /api/campaigns/{campaignId}/attack-previews/{id}/ammunition-confirmations`

Write: same fields plus `{requestId,reason}`; nonempty reason up to 1000 code points.
No client amounts, HP, snapshots or changes accepted. Current DM authorization and
source/target existence precede durable replay. Fingerprint binds campaign, route,
preview ID and entire input. Original versions/hash/eligibility are rechecked on a
fresh command; MANUAL_REQUIRED rejects 409 with no partial write. Saves one immutable
CONFIRM_AMMUNITION event with two derived changes: original source version/unchanged
HP/tempHp and only selected resource after; original target version/new HP/tempHp
and empty resources. This event kind permanently identifies this V1 profile.
Status becomes CONFIRMED; no card changes yet. Existing generic `/events` does NOT
accept CONFIRM_AMMUNITION, preventing a bypass of explicit mapping/review.

Separate existing APPLY accepts the saved CONFIRM_AMMUNITION alongside old CONFIRM
and CONFIRM_DAMAGE. Original source, target and action versions remain mandatory.
Both card changes, histories, P06 action, preview event and receipts commit together;
immediate/deferred failures roll everything back. Equal concurrent keys replay one
ack; competing confirmations have one winner. Same key/different route/body conflicts.
Exact historical receipts remain read-only after later play; current DM revocation
still rejects. Linked CORRECT uses explicit current final values, never refunds a
historical delta automatically. PUBLISH/player results remain unchanged and private
evidence never enters player payloads. All errors/responses: no-store, private.

No migration or persistent record shape change: only a new event kind with existing
derived change values. Source evidence already retains the complete original resource
snapshot. Old evidence/hash/confirmations retain meaning. No inventory decrement,
recovery, spell slots, conditions, mastery or automatic public action policy.

Chinese UI requires an explicit saved-source counter selection and review, reads the
authoritative plan, then separately confirms and applies. Editing selection/review,
revision or preview change clears the plan; late success cannot restore it. API/
campaign/role changes and current-domain 401/403 clear private state. Unknown outcome
locks competing controls and retries exact route/body/key; an ack then failed read
permits read-only recovery. Full existing manual and A08 controls remain available.
