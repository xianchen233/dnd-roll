# A17 adjudicated healing of original A16 records

See [5R evidence/matrix](../docs/research/stabilization-healing-5r.md).
DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization`:
POST `/healing-preview` is read-only; POST `/healings` is an explicit application;
GET `/healing-history` returns immutable private healing entries in version order.
Every response/error is private no-store, all POSTs require session CSRF.

Review `{expectedVersion,targetVersion,attemptId,amount,reviewed}` is strict and
complete: current stabilization/card versions, latest A16 attempt UUID, adjudicated
integer healing amount 1–100000, explicit review. Apply adds `requestId`, lowercase
64hex `planHash`, nonblank `reason` max1000. No HP, removed list, helper, total,
originVersion, actor or bound predicates are accepted. Unknown/null/coerced/duplicate
fields fail. Plan `{id,version,targetVersion,attemptId,hash,effect}` binds full
current state/card, exact review and deterministic domain effect.

Fresh commands require latest APPLIED attempt with durable completion, authoritative
stored original server check and canonical A16 successful plan. Reconstruct the full
card that A16 applied, including version+1, access flags/controllers and exact sheet;
require equality to the current target and no import review. No subsequent helper
edit invalidates this original target outcome. Domain additionally checks exact
canonical two annotations, HP0/maxHP>0/death counts0, no modifiers/overrides, explicit
healing review. Unsupported plans return MANUAL_REQUIRED with no executable values;
stale versions/attempt conflict. Amount is a DM-adjudicated input, not a dice result.

Candidate effect contains profile/status/reasons, hp=min(amount,maxHP), unchanged
tempHp, actual healed amount, deathSuccesses=deathFailures=0 and the exact two
removed record IDs. No auto dice/resource/action/condition engine or prone removal.
Application rederives/hash-checks then atomically saves target version+1 and full
safe character history, advances A16 state once with latest status HEALED, appends
its immutable state history, private healing entry and original acknowledgement.
Preserve original target/helper/check/review/completion in the A16 attempt. Never
put private healing reason/review/requested amount in player-readable history;
use a fixed safe result description. Private entry retains actor/time/reason,
review/amount, hash, original attempt ID, full before/after cards and result version.

Use existing actor-then-campaign transaction locks and stabilization-command receipt
namespace. Current DM and current target existence precede receipt replay. Exact
old command returns original ack before stale/support gates with no writes/event;
different route/campaign/target/body with same request409. Same/different request
concurrency at one state version applies once; immediate/deferred failures roll
back card/history/state/private entry/receipt together. A16 and A17 receipts cannot
substitute for one another. Read/preview never mutate or roll.

V28 adds one healing-journal table linked to the exact durable stabilization-history
version; old migrations unchanged. Whole-database backup contains it. Independent
campaign remapping of these bindings remains an M3 final item.

UI: DM launcher → saved original evidence → adjudicated amount/review/reason →
preview → separate confirm. Capture exact unknown-outcome retry even across realtime
revisions. Clear plans on input/scope/version change, clear private state on any
401/403/404 including late replies, never auto-create a new command on409. After
HEALED show completion while retaining historical A16 evidence. For a later separate
ordinary 0HP episode, the A16 launcher can offer a new independently reviewed Help
attempt when the current card is again at0HP; old healing evidence is historical,
never a preview or automatic application. Local acceptance requires actual
RED/GREEN, realPG/auth/concurrency/rollback, UI, production browser,
real restart/saved-plan continuation. Cloud/real-phone verification is postponed.
