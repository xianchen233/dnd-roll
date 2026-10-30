# A19 dedicated new Help check after original A18 instability

[5R evidence/matrix](../docs/research/restabilization-5r.md).
DM-only root `/api/campaigns/{campaignId}/characters/{id}/stabilization/restabilization`:
GET root and `/history` return independent A19 state (initial version0/latest null)
and immutable snapshots; POST `/rolls`, `/preview`, `/applications` save a new check,
read a plan and explicitly apply it. All responses/errors private no-store; session
CSRF on all POSTs. Strict complete fields, no null/duplicates/coercion/extra keys,
max4096-character body.

Roll `{requestId,expectedVersion,originVersion,targetVersion,helperId,helperVersion,
reviewed,reason}`. Review removes requestId/reason and adds rollId. Apply adds
requestId/planHash/reason to Review. Version fields are nonnegative integers,
identities UUIDs, reviewed boolean, reason nonblank max1000, hash lowercase64hex.
originVersion is the shared A16/A18 state version; expectedVersion belongs to the
separate A19 ledger. No client total/DC/final values/canonical records/bound flags.

Origin requires current shared state UNSTABLE and corresponding immutable A18
damage journal at originVersion, original attempt identity and authoritative old
A16 private check. Reconstruct the A16 applied card and A18 damage before/after;
require exact complete current target equals the journal after, with no import
review. Current helper must exist in campaign, be distinct and support ordinary
WIS Medicine. Recheck both current versions. No dependency on later edits to the
old A18 helper; only this new helper must remain unchanged after its new roll.

Roll is a new explicit Help action with its own internal dice fingerprint namespace
and derived request UUID. Bind origin/target/helper IDs, versions, full snapshots,
helper calculation, shared origin version and prior A19 version; save failed checks
too. Generic/A15/A16 checks and pre-seeded ordinary dice receipts cannot supply it.
Latest ROLLED attempt only; preview rechecks current full cards/source and stored
private new roll without rolling/writing. Plan `{id,version,originVersion,
targetVersion,helperVersion,rollId,hash,effect}` hashes exact current states/cards,
review and effect. Unsupported/failed/unreviewed returns MANUAL_REQUIRED with no
application values; stale versions/wrong attempt or origin409.

Domain effect profile/status/reasons/dc10/total/hp/tempHp/deathSuccesses/deathFailures/
annotations. Candidate requires exact original Unconscious-only target, HP0/maxHP>0,
successes0/failures1–2, no modifiers/overrides, reviewed/dedicated bound check total>=10.
HP0/tempHP preserved, death counts0/0, annotations contain new a19-stable:<rollId>
record and the EXACT original a16-unconscious record. No new unconscious identity,
automatic wake/healing/timer/conditions/turn/Help budget or resource execution.

Apply rederives and hash-checks; atomically save target version+1/full safe character
history, A19 latest APPLIED with private completion, its version+1/history, shared
state originVersion+1/latest RESTABILIZED and shared immutable history, receipt and
safe invalidations. Keep original A16/A18 evidence, old/new dice, helper/resources.
Only current DM and both current object existences precede receipt replay; exact
historical command returns original ack before stale/support gates, no mutation or
event. Same key/different route/body/campaign/target409. Reuse stabilization-command
receipts and actor-then-campaign transaction locks; concurrency single effect,
immediate/deferred roll/apply failures roll back every row and die. Private total,
review/reason/completion stay in DM A19 history; player-readable character history
uses a fixed safe outcome description, with actual before/after target cards.

V30 adds two A19 state/history tables linked to the exact A18 damage journal version.
Old migrations unchanged. Whole-PG backup includes them; independent campaign ID
remapping remains M3 final work. Shared RESTABILIZED is distinct from original
APPLIED, so old A17/A18 fresh operations do not consume the new outcome.

UI: new helper/review/reason → save check → preview → separate apply; failed checks
remain, no reroll on reads, exact unknown response retry across revisions. Clear
plans on input/scope/revision, clear private data on any401/403/404 including late
denials, known409 rereads without automatic new command. Local acceptance requires
actual RED/GREEN, realPG/auth/transactions/concurrency, UI, production Chrome,
true restart and saved-check continuation. Cloud/physical-phone checks postponed.
