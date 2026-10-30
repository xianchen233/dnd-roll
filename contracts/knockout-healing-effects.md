# A13 bounded healing of an applied knockout

Source and support limits: [5R matrix](../docs/research/knockout-healing-5r.md).
Independent DM-only routes under `/api/campaigns/{campaignId}/attack-previews/{id}`:

- `POST /knockout-healing-effect`: read-only plan, requires CSRF.
- `POST /knockout-healings`: explicit atomic HP recovery and annotation removal.

Review body: `{expectedVersion,previewHash,targetVersion,amount,reviewed}`.
Versions are nonnegative integers, hash is 64 lowercase hex, amount is an integer
1–100000, reviewed is a boolean. Apply adds `{requestId,planHash,reason}`;
reason is nonempty (max1000), planHash is 64 lowercase hex. No client HP, condition,
target ID, actor or resource changes. Extra/missing/null/duplicate/wrongly typed
fields are 400. Auth/CSRF and current campaign DM are required, including retries.

Plan: `{profile,status,reasons,hp,tempHp,healed,conditionId,targetVersion,hash}`.
Profile is `5R-SRD-5.2.1-KNOCKOUT-HEALING-V1`; status is CANDIDATE or
MANUAL_REQUIRED. The latter has null hp/tempHp/healed/conditionId and reason codes.
Hash covers the review, origin preview ID/version/hash, current card and derived plan.
TargetVersion always identifies the reviewed current card. No receipt or mutation on read.

Only APPLIED previews are eligible. Require exactly one original A11/A12 confirmation
and an APPLY event, no earlier HEAL_KNOCKOUT or CORRECT, and target version equals the
confirmation target change's expectedVersion + 1. Require the exact canonical
annotation as the sole condition, HP1/tempHP0 and no death saves. (The APPLY preview
event itself has empty changes; the confirmation holds the applied target change.)
Other card versions or unsupported evidence return MANUAL_REQUIRED; stale submitted
preview/card versions or hash return409. Source existence is checked, but its current
version is not used: later source activity need not prevent healing this target.

DM reviews that the already adjudicated healing has no unresolved blockers and handles
its resource cost separately. Actual recovery=min(amount,maxHp-hp) must be positive.
Apply rederives and compares planHash under actor/campaign locks, updates the card once,
writes full before/after history kind KNOCKOUT_HEALING, appends HEAL_KNOCKOUT with the
actual numeric target change, increments preview version (status stays APPLIED), saves
receipt and emits visibility-safe invalidation in the same transaction. Original
evidence/hash/dice/action stay intact. Generic `/events` cannot inject HEAL_KNOCKOUT.
No schema migration. Wake-up does not remove Prone or implement other linked effects.

Ack is `{id:previewId,version}`. Permission and source/target existence precede receipt
replay; identical retries return the original ack before current version/support checks.
They never heal again or remove reintroduced records. Different body/route under the same
key conflicts. Concurrent same-key requests have one effect; different keys with stale
versions conflict. Immediate or deferred transaction failure rolls back card/history,
preview/event/receipt. Player responses never contain private origin, plan, reason or hash.
All responses use private no-store cache headers. Existing generic healing/editor and
correction semantics are unchanged. Unknown browser outcomes retain the exact request;
known conflicts require reread; scope/review edits discard old plans.
