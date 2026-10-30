# A16 stabilization — calculator and independent character-scoped adapter

## A16b transport/storage contract (2026-10-28)

DM-only under `/api/campaigns/{campaignId}/characters/{id}/stabilization`:
GET root returns `{id,version,latest}` (initial version0/latest null); GET `/history`
returns immutable state snapshots in ascending version. POST `/rolls` saves a Help
attempt. POST `/preview` reads a plan without writes. POST `/applications` applies
one successful current plan. CSRF for all POSTs; private no-store on all outcomes.

Applied attempts retain DM-only completion `{actor,reason,at}`. Character history
uses a fixed safe result description and full before/after card snapshots; it does
not expose the private check total, review or original operation reason to players.
Local HTTP/UI, actual restart and independent whole-PostgreSQL restore continuation
have passed; see [actual results](../docs/a16-tdd.md). Whole-database continuation
does not implement independent campaign remapping or close M3 final acceptance.

Attempt body `{requestId,expectedVersion,targetVersion,helperId,helperVersion,reviewed,reason}`.
Review body `{expectedVersion,targetVersion,helperId,helperVersion,rollId,reviewed}`.
Apply adds `{requestId,planHash,reason}` to review. Required exact fields, no extra,
null, duplicate or coerced types. UUID identities, nonnegative integer versions,
lowercase64hex planHash, nonblank reason max1000. No total/DC/boundCheck/actor/final values.

State version is independent of character version: each attempt and application
advances state once; attempts do not change either card. A dedicated A16 dice
namespace binds target/version, helper/version/calculation and prior state version.
Each attempt retains server roll plus both card snapshots, actor/time/review/reason;
failed checks are saved. Only latest ROLLED attempt ID=rollId is eligible, with both
unchanged current cards and authoritative stored private Medicine roll. Generic or
A15 rolls and ordinary-dice pre-seeding cannot replace it. A different new attempt
must submit the current state version and independently review another Help action.

Plan `{id,version,targetVersion,helperVersion,rollId,hash,effect}` wraps the A16a
domain plan. Hash binds campaign, entire current state, both cards, exact review
and computed effect. Stale versions409; wrong/old roll409. Failed or unreviewed
checks return MANUAL_REQUIRED with no executable changes. No plan persistence.

Apply rederives the plan, compares planHash and requires CANDIDATE. Save exactly
two source records and reset death counts, increment target once, append full
STABILIZATION character history, mark latest APPLIED, increment state, append
private immutable state history, receipt and safe invalidation atomically. Keep
HP/tempHP, resources and helper unchanged. GET/history retain latest result after
card changes; a fresh apply conflicts, exact receipt replay never overwrites them.

Actor then campaign locks encompass all dice, state/history/card writes and receipts.
Current DM membership and both object existences precede replay; exact old writes
return original ack before stale/support checks; reused request with different
route/body/campaign/target409. Cards awaiting import review are unsupported for
fresh writes, including helper. Two attempts/applications with same state version
have at most one effect. Immediate and deferred DB failures roll back every row.

V27 adds separate state/history/receipt tables; existing migration checksums remain
unchanged. Target state/history cascade with target deletion; receipts bind campaign
and survive subsequent card edits. Existing attacks, A15 and generic dice unchanged.
Full database backup retains new tables. Independent *campaign* remapping of this
new evidence is not yet supported and remains an explicit M3 final check.

The older A16a section below records the initial internal milestone, not current
transport availability. Actual implementation/verification status is in a16-tdd.md.

[5R evidence/support matrix](../docs/research/stabilization-5r.md). A16a is a pure
domain calculation only. No HTTP operation, automatic application, migration,
durable roll or UI is implemented or advertised by this contract.

`StabilizationEffect.plan(target, helper, attemptId, ordinaryZeroReviewed,
distinct, boundCheck, total)` accepts validated CharacterSheets, a non-null
dedicated attempt UUID, internal reviewed/distinct/bound-check predicates and
the saved server Medicine total. Invalid sheets/null target/null attempt throw
INVALID_INPUT. Null/unsupported helper falls back. No dice, clock, IO or mutation.

Profile `5R-SRD-5.2.1-STABILIZATION-V1`; plan contains profile, status, immutable
reasons, DC10, total, nullable hp/tempHp/deathSuccesses/deathFailures, immutable
annotations. CANDIDATE requires all matrix predicates and total>=10. Candidate
hp=0, original tempHp, death counters=0; two deterministic attempt-scoped records
describe Stable and 0HP Unconscious with 5R/SRD5.2.1 provenance and no timer.
The current target must have no conditions; returned records are additions, never
a replacement for arbitrary existing conditions. MANUAL_REQUIRED exposes no
executable numeric values or annotations. All unmet predicates remain visible.
Repeated calculation produces the same plan; neither input sheet changes.

`helperSupported` requires ordinary WIS Medicine, conscious HP>0, no conditions,
death saves, modifiers or overrides. Proficiency is optional. Target requires
maxHP>0, HP0, death counts each<=2, no conditions/modifiers/overrides; review must
attest ordinary living, not already stable, non-monster target plus legal Help
action/contact and no special exceptions. Review cannot override numeric gates.

## Required next adapter contract (design only, no routes yet)

Use a standalone target-character scope, independent of attack previews and A15.
Save an explicit reviewed Help attempt with target/helper IDs and versions, current
DM identity, dedicated request UUID/namespace and server skill:medicine check.
The adapter must validate import-review/access and derive distinctness and all
binding predicates server-side. Bind target, helper, both versions and calculation,
attempt identity and latest-attempt status. Reject arbitrary/old/A15 checks and
generic-dice UUID pre-seeding. Never accept total/DC/boundCheck/final values from
HTTP. Failed checks persist, preview never rolls; retries never reroll.

Preview then separately confirm a hash binding those exact snapshots and review.
Apply rechecks authorization and object existence before receipt replay; fresh
writes require current versions/latest attempt and rederive the plan. Atomically
write the two records, reset both counters, increment target once, save full
before/after history, attempt/event/receipt and visibility-safe invalidation.
Applied attempts additionally retain private completion `{actor,reason,at}`.
Character history uses a fixed safe result description: players controlling the
target never receive the private check total, review or original application reason
through character history. Both full card snapshots still describe the actual change.
Helper/HP/tempHP/resources/old evidence stay unchanged. Exact replay returns its
old acknowledgement without overwriting later play; changed payload409. Concurrent
same-version attempts/applications, revoked access, cross-campaign identities,
immediate/deferred failures and response-loss retries need real PG/HTTP tests.
No private check/history/plan may enter player payloads; use CSRF and no-store.

Chinese UI must distinguish “稳定，仍0HP且昏迷” from A15 wake-up, require explicit
Help review/check/plan/application and exact unknown-outcome retry. Read recovery,
permission/scope/version changes must clear stale plans. Browser/restart/independent
restore continuation remain required before the effect is called locally accepted.
