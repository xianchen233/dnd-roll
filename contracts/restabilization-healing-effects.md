# A20 healing of the original A19 restabilized outcome

See [5R sources/matrix](../docs/research/restabilization-healing-5r.md).
DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization/restabilization`:
POST `/healing-preview` is read-only, POST `/healings` explicitly applies, GET
`/healing-history` returns immutable private entries. All errors/responses are
private no-store; writes/preview require session CSRF.

Strict Review `{expectedVersion,originVersion,targetVersion,attemptId,amount,reviewed}`:
independent A19 state version, shared A16 state version, current target version,
latest A19 attempt UUID, integer amount1–100000, explicit review. Apply adds
`requestId`, lowercase64hex `planHash`, nonblank max1000 `reason`. Every field is
required; unknown/null/duplicate/coerced fields and final values are rejected.
Plan `{id,version,originVersion,targetVersion,attemptId,hash,effect}` binds full
two states/current card, exact review and deterministic effect.

Fresh commands require A19 APPLIED/completion and shared RESTABILIZED, exact
parent history at A19.originVersion+1, and bound A18 journal. Reconstruct A16
successful applied card and A18 damage result using original authoritative rolls
and stored snapshots; reconstruct A19 successful check/applied card using its
snapshots and authoritative new roll. Require current full card equality and no
import review. Helper edits after application do not invalidate historical origin.
Domain requires exact new a19-stable/original a16-unconscious pair, HP0/maxHP>0,
death counts0/0, no modifiers/overrides and reviewed. Unsupported source/cards
return MANUAL_REQUIRED with null executable values; stale versions/attempt conflict.

Apply rederives/hash-checks and atomically saves target version+1, safe full-card
character history kind RESTABILIZATION_HEALING, A19 version+1 with latest HEALED,
shared state version+1 with latest HEALED, both immutable histories, private journal
and original ack (A19 version). Preserve old attempts/checks/completions. HP caps at
max, tempHP/resources unchanged, exactly two source IDs removed. Private requested
amount/review/reason never enter player history; use a fixed safe explanation.
Entry retains id/version/originVersion/attemptId/actor/at/reason/review/hash/before/after.

Actor then campaign locks and shared stabilization-command receipts. Current DM
and target existence precede replay; exact receipt precedes stale/support checks.
Cross-route/campaign/target/body request reuse409; exact retries emit no event/write
and preserve later play. Same/different keys at one version apply once. Immediate
and deferred failure must roll back both ledgers/card/histories/journal/receipt.

V31 adds one restabilization_healing_history table linked to exact A19 history
and shared stabilization history result versions. Old migrations/rows unchanged.
Whole DB restoration includes it; campaign-ID remapping remains final M3 work.

UI: separate default-closed DM launcher, saved A19 evidence, amount/review/reason,
read-only preview, separate confirmation; unknown result retries captured exact
body across revisions, known409 reads only. Input/scope/revision clears previews;
any late401/403/404 clears private state. HEALED shows history without fresh A19
controls; a later ordinary0HP episode may begin a new independently reviewed A16
Help attempt. No automatic dice/resource/action/state engine. Local actual RED,
realPG/auth/concurrency/rollback, UI/browser/restart checks precede acceptance.
Cloud and physical-phone checks are deferred.
