# A18 first adjudicated damage to an original stable A16 target

[5R source/version/support matrix](../docs/research/stabilization-damage-5r.md).
DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization`:
POST `/damage-preview` is read-only; POST `/damages` explicitly applies;
GET `/damage-history` returns immutable private damage entries, ascending version.
All outcomes private no-store, session CSRF on every POST. Strict complete fields,
no unknown/null/coerced/duplicate properties; request body max4096 characters.

Review `{expectedVersion,targetVersion,attemptId,amount,critical,reviewed}`: current
state/card versions, latest original attempt UUID, integer amount0–220000 and two
booleans. Apply adds requestId, lowercase64hex planHash, nonblank reason max1000.
No final HP/tempHP/death counts/removed list/actor/roll/verified origin is accepted.
Amount is final adjudicated damage after defenses, before temporary HP absorption.
Critical is an explicit reviewed source property; do not double damage again or
automatically infer a critical from Unconscious, proximity or an arbitrary roll.

Fresh plans require latest APPLIED A16 and its unchanged full applied target card,
including version, access flags/controllers, exact canonical pair and all sheet
fields. Recheck durable original check and reconstruct the A16 result server-side;
no current helper-version requirement. Unsupported origin/review/numerical cases
return MANUAL_REQUIRED with no executable values; stale versions/wrong attempt409.
Plan `{id,version,targetVersion,attemptId,hash,effect}` binds entire state/card,
review and deterministic effect; preview never rolls or writes.

Effect profile/status/reasons/hp/tempHp/absorbed/deathSuccesses/deathFailures/removed.
Candidate only for 0<amount<maxHP, original HP0 and deaths0/0, exact two records,
no modifiers/overrides. HP remains0, absorbed=min(tempHP,amount), tempHP reduced,
successes0, failures=critical?2:1, remove ONLY the original stable record and retain
the exact original 0HP Unconscious record. Positive damage fully absorbed by tempHP
still constitutes this damage event. Amount0 is no application; amount>=maxHP is
manual even if tempHP might absorb it. No automatic death or subsequent damage.

Apply rederives and hash-checks, then atomically writes card version+1, full safe
character history, state version+1/latest UNSTABLE, immutable stabilization history,
private damage journal and original acknowledgement. Keep original A16 snapshots,
check/review/completion and helper/resources. Private journal records actor/time,
reason, exact review/hash/attempt and full before/after cards. Player-readable
history has a fixed safe outcome description, never private reason/requested amount/
critical/review payload. HP/death counters/record changes remain actual card fields.

Existing actor-then-campaign locks and stabilization-command receipts apply. Current
DM and current target existence precede replay. Exact historical writes return
original ack before stale/support checks, without mutation/event; same key with
different route/body/campaign/target409. A16/A17/A18 receipts cannot substitute.
Same/different-key concurrency at one version applies once. Immediate/deferred
failures roll back card/history/state/private journal/receipt together.

V29 adds one private damage journal linked to the exact stabilization-history
version; old migrations remain. Whole-PG backup contains it, independent campaign
remapping of this evidence remains M3 final work. A17/APPLIED-only healing and the
generic DamageEffect 0HP fallback remain; UNSTABLE subsequent treatment is manual.

UI requires saved original source, amount/critical/review/reason → preview → separate
confirm. Capture unknown-outcome exact retry across revisions; input/scope/version
changes invalidate plans; any401/403/404, including late replies, clears private
state. Known409 reads authoritative state without automatic new write. UNSTABLE
shows saved failure counts remain manual for later play; do not offer a stale A16
apply or fresh Help against the remaining source record. Actual RED/GREEN, realPG,
production browser, restart/saved-plan continuation required for local acceptance.
Cloud and physical-phone verification remain postponed.
