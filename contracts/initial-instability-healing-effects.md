# A23 direct healing of the original A18 instability outcome

See [5R evidence/matrix](../docs/research/initial-instability-healing-5r.md).
DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization/instability`:
POST `/healing-preview` read-only; POST `/healings` explicit application; GET
`/healing-history` immutable private entries in ascending version order. All
responses/errors private no-store; session CSRF on every POST, body max4096 chars.

Strict Review `{expectedVersion,targetVersion,attemptId,amount,reviewed}`: shared
A16 state/card versions, latest original A16 attempt UUID, adjudicated integer real
healing1–100000 and explicit review. Apply adds requestId, lowercase64hex planHash,
nonblank max1000 reason. All required; reject unknown/null/coerced/duplicate fields,
originVersion, damageVersion and client final values. Plan `{id,version,damageVersion,
targetVersion,attemptId,hash,effect}` binds full state/card, entire source A18 journal,
review and deterministic effect. Server derives damageVersion from current source.

Fresh commands require latest UNSTABLE/completion and exact A18 journal at current
state version. Verify preceding APPLIED state and original attempt preserved except
for the A18 status transition. Reconstruct original A16 successful card using its
authoritative saved roll/helper snapshot, then original A18 damage plan and result;
verify original A18 planHash. Require complete current-card equality and no import
review. Helper edits after application do not invalidate historical origin.

Domain requires the sole exact original a16-unconscious, HP0/maxHP>0, death
successes0/failures1 or2, no modifiers/overrides and reviewed. Candidate restores
HP=min(amount,maxHP), keeps tempHP/resources, resets death counts0/0 and removes
only that record. Unsupported source/cards return MANUAL_REQUIRED with null
executable values; stale versions/attempt conflict. Preview never writes/rolls.

Apply rederives/hash-checks, atomically writes target version+1, safe full-card
history kind INITIAL_INSTABILITY_HEALING, shared state version+1/latest HEALED,
immutable state history, private journal and original ack (shared state version).
Preserve original evidence. Entry `{id,version,damageVersion,attemptId,actor,at,reason,
review,planHash,before,after}`. Player history uses fixed safe outcome text, never
private reason/requested amount/review. No treatment dice/resource/action/state engine.

Pending unapplied A19 Help checks may exist against unchanged A18 results: retain
their independent ledger, snapshots and dice verbatim. Advancing the shared version
invalidates any old A19 continuation; do not delete or reapply it. Once A19 has
already applied, this direct A18 treatment is unavailable. A17/A20/A22 unchanged.

Actor then campaign locks and shared stabilization-command receipts. Current DM and
target existence precede replay; exact old receipt precedes stale/support gates,
without mutation/event or overwriting later play. Changed route/body/campaign/target
request reuse409. Same/different-key concurrency applies once; immediate/deferred
failures roll back card/history/state/private journal/receipt together.

V34 adds initial_instability_healing_history with exact FKs to the source A18
damage-history version and resulting shared history version. Old data/migrations
preserved. Whole-DB restoration includes it; campaign-ID remapping remains M3 final work.

UI: independent default-closed launcher, saved source, amount/review/reason →
preview → separate confirmation, exact unknown-result retry across revisions,
known409 reads only. Input/scope/revision clears plans; any late401/403/404 clears
private state. Check this route's journal before showing saved HEALED completion.
Actual RED/GREEN, realPG/auth/retry/concurrency/rollback, production browser and
real restart/restored saved-plan continuation required; cloud/physical-phone deferred.
