# A22 healing of the unchanged original A21 instability outcome

See [5R evidence/matrix](../docs/research/instability-healing-5r.md).
DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization/restabilization/instability`:
POST `/healing-preview` is read-only; POST `/healings` explicitly applies; GET
`/healing-history` returns immutable private entries in ascending version order.
All responses/errors private no-store; every POST requires session CSRF.

Strict Review `{expectedVersion,originVersion,targetVersion,attemptId,amount,reviewed}`:
current A19 ledger/shared ledger/card versions, latest A19 attempt UUID, integer
adjudicated healing1–100000, explicit review. Apply adds requestId, lowercase64hex
planHash, nonblank max1000 reason. All required; unknown/null/coerced/duplicate
fields and final values rejected. No client damageVersion or origin/bound predicates.
Plan `{id,version,originVersion,damageVersion,targetVersion,attemptId,hash,effect}`
binds full two states/card, exact A21 journal, review and deterministic effect.

Fresh commands require A19 UNSTABLE/completion and shared RESTABILIZATION_UNSTABLE,
matching exact A21 journal at current A19 version and shared result version. Check
both preceding APPLIED/RESTABILIZED states and that current attempts preserve their
snapshots/checks/completions with only the declared A21 status transition. Reconstruct
A16 successful card, original A18 journal/outcome, A19 success/applied card, then A21
damage outcome using authoritative original server rolls and historical helper
snapshots. Verify the original A21 plan hash. Require current full-card equality and
no import review; later helper edits do not invalidate historical outcome.

Domain requires only the exact original a16-unconscious record, HP0/maxHP>0, death
successes0/failures1 or2, no modifiers/overrides and reviewed. Candidate restores
HP=min(amount,maxHP), keeps tempHP, resets deaths0/0 and removes exactly that record.
Unsupported origins/cards return MANUAL_REQUIRED with null executable values;
stale versions/attempts conflict. No new dice/resource/action/state/death engine.

Apply rederives/hash-checks and atomically writes card version+1, safe full-card
history kind INSTABILITY_HEALING, both ledgers version+1/latest HEALED, both histories,
private journal and original ack (A19 version). Preserve old snapshots/checks/
completions and all A21 evidence. Player history uses a fixed safe explanation,
never private reason/requested amount/review. Entry `{id,version,originVersion,
damageVersion,attemptId,actor,at,reason,review,planHash,before,after}` retains exact source.

Actor then campaign locks and shared stabilization-command receipts. Current DM and
target existence precede replay; exact old receipt precedes stale/support gates and
does not write/event or overwrite later play. Changed route/body/campaign/target
request reuse409. Concurrent same/different keys at one version apply once; immediate
and deferred failure rolls back card/both ledgers/all histories/journal/receipt.
Old A17/A20 gates remain; fresh ordinary later0HP episodes need independent review.

V33 adds instability_healing_history with exact FKs to A21 source damage version,
A19 result-history version and shared result-history version. Old rows/migrations
preserved. Whole-DB restoration includes it; campaign-ID remapping remains M3 final work.

UI: independent default-closed launcher, saved evidence, amount/review/reason →
preview → separate confirmation. Unknown result retries captured exact body across
revisions; known409 reads only. Input/scope/revision clears plans; any late401/403/404
clears private data. HEALED shows saved completion without fresh treatment controls.
Actual RED/GREEN, realPG/auth/retry/concurrency/rollback, production browser and real
restart/restored saved-plan continuation required; cloud/physical-phone checks deferred.
