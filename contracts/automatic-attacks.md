# A04: bounded pure attack calculator

Rules/support matrix: [5R evidence](../docs/research/automatic-attacks-5r.md).
Internal Java domain contract only; no HTTP route, database migration, UI execution,
or change to P04 catalog `automated=0` / P06 manual action semantics.

`AttackCalculator.plan(Request)` returns `CANDIDATE` or `MANUAL_REQUIRED`.
Request pins profile, action type, attack source, reviewed context, unsupported
exceptions with sources, sourced advantage/disadvantage factors, immutable raw d20s,
final hit modifier, final AC and one typed damage specification with defense factors.
All lists are defensively copied. Exact profile is `5R-SRD-5.2.1-WEAPON-V1`;
source must be `SRD / 5.2.1 / 5R`, action `WEAPON_ATTACK` and reviewed context true.
Every exception, unsupported source/profile/action or unreviewed context yields
explicit manual reasons, no verdict, attack total or damage plan. A supported profile
does not automatically detect exceptions in free text or prove the action is legal.

Supported request: one or two raw d20 values as required by nonstacking/cancellation
of sourced factors; final modifiers -10000..10000; AC 0..1000. Damage is 1..50 dice
of 2..1000 sides, one of thirteen typed damage kinds, final modifier -10000..10000.
At most 100 factors/exceptions per list; source metadata bounded as P03. Invalid
counts, missing/extra dice, out-of-range values or nulls reject with `INVALID_INPUT`/400.
These are software limits, not D&D limits. Fixed no-dice damage has a separate
unsupported action type and must not be approximated by this profile.

Plan records chosen die/index, mode, total, HIT/MISS/CRITICAL, exact required damage
dice count/sides and sourced explanations. Ties keep the first d20. A miss requires
zero damage dice. Critical doubles base dice count only, never the modifier.

`AttackCalculator.resolve(Request, List<Integer> damageDice)` recomputes that plan
from the same immutable request, checks exact dice count/range, and calculates
`max(0,sum+modifier)`, then resistance (floor half), vulnerability (double), immunity
(zero). Same-type defense lists are nonstacking and contain source explanations.
It returns the request/plan, copied actual damage dice, intermediate/final amounts
and rule citations; repeated evaluation is identical. No entropy, network, state
repository or clock is used. Manual results have no damage or partial calculation.

Only a future authenticated service may assemble this request from current permitted
cards/content and immutable server P05 rolls. This internal type is not a trusted
client payload. A05 must bind campaign/actor/targets/object versions/roll IDs/hash,
recheck authorization before receipts, preserve private evidence and apply one
transaction only after DM confirmation, with concurrency/retry/correction and
restart/recovery tests. A04 neither supplies those guarantees nor adds a bypass.
No player-facing serialization of raw AC/source/dice is added here.
