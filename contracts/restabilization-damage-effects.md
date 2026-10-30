# A21 first damage to the unchanged original A19 restabilized outcome

See [5R evidence/support matrix](../docs/research/restabilization-damage-5r.md).
DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization/restabilization`:
POST `/damage-preview` is read-only; POST `/damages` explicitly applies;
GET `/damage-history` returns immutable private entries in ascending version order.
All responses/errors private no-store; session CSRF on all POSTs.

Strict Review `{expectedVersion,originVersion,targetVersion,attemptId,amount,critical,reviewed}`
binds current A19 state, shared A16 state and target versions, latest A19 attempt UUID,
integer final adjudicated damage0–220000 and explicit booleans. Apply adds requestId,
lowercase64hex planHash and nonblank max1000 reason. All fields required; reject
unknown/null/coerced/duplicate fields and client final values. Body max4096 characters.
Plan `{id,version,originVersion,targetVersion,attemptId,hash,effect}` binds entire
two states/current card, review and deterministic domain effect. Preview never writes/rolls.

Fresh plans require A19 APPLIED/completion and shared RESTABILIZED at A19.originVersion+1.
Reconstruct original A16 success/applied card, exact A18 journal/result and A19
success/applied card with authoritative stored rolls and historical helper snapshots.
Require complete current-card equality, exact a19-stable/a16-unconscious pair, HP0,
maxHP>0/death counts0/0, no import review/modifiers/overrides and reviewed damage.
Post-application helper edits do not invalidate origin. Unsupported origins return
MANUAL_REQUIRED with null executable values; stale versions/attempts conflict.

Candidate only0<amount<maxHP: HP0, absorbed=min(tempHP,amount), tempHP reduced,
death successes0/failures critical?2:1; remove only a19-stable and retain the exact
original a16-unconscious object. Fully absorbed positive damage still triggers.
Zero damage and amount>=maxHP are manual, even with large tempHP. No automatic
critical/defense/death determination, dice, resource consumption or repeated damage.

Apply rederives/hash-checks then atomically writes card version+1, safe full-card
history kind RESTABILIZATION_DAMAGE, A19 version+1/latest UNSTABLE, shared version+1/
latest RESTABILIZATION_UNSTABLE, both immutable histories, private journal and ack
(A19 version). Keep original snapshots/checks/completions. Player history uses a
fixed safe outcome, never private reason/requested amount/critical/review payload.
Entry `{id,version,originVersion,attemptId,actor,at,reason,review,planHash,before,after}`.

Actor then campaign locks and shared stabilization-command receipts. Current DM and
target existence precede replay; exact old receipt precedes stale/support gates and
emits no event/write. Changed route/body/target/campaign request reuse409. Concurrent
same/different keys at one version apply once; immediate/deferred failures roll back
card/both states/all histories/private journal/receipt. Old A16/A18/A19/A20 commands
do not treat this new instability as their original source; subsequent handling manual.

V32 adds restabilization_damage_history linked to exact A19 and shared result-history
versions. Old migrations/rows preserved; whole-DB restoration includes it. Independent
campaign-ID remapping remains M3 final work.

UI: independent default-closed launcher, saved source, amount/critical/review/reason,
preview then separate confirmation. Capture exact unknown-result retry across revisions;
known409 reads only; input/scope/revision clears plans; any late401/403/404 clears private
state. UNSTABLE shows history without fresh controls, including old Help controls.
Actual RED/GREEN, realPG/auth/retry/concurrency/rollback, production browser and real
restart/restored saved-plan continuation required. Cloud and physical-phone checks deferred.
