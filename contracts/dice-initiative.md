# P05: server dice and 5R initiative

Status: P05a–e completed, 2026-10-21; code f416e62 passed local and exact-SHA
push/PR CI. P01–P04 remain complete; P06 turn execution/damage is separate.
The whole M2 remains incomplete. Evidence: docs/verification.md.
Rules: [5R evidence](../docs/research/dice-initiative-5r.md).

Ordered slices: P05a bounded dice/parser and snapshots; P05b durable authorized
rolls and shared button/command service; P05c initiative candidates/confirmation;
P05d Chinese UI/recovery; P05e all regressions, migration, real browser/restart
and exact delivered SHA push/PR CI. Each behavior requires executed RED/GREEN.

## Rolls

Under `/api/campaigns/{campaignId}`:

- GET `/rolls?before=UUID` returns at most 50 currently visible records, newest
  first; `next` is the last visible record ID or empty. Filter before pagination;
  no global sequence/count/version or hidden placeholders. Foreign/hidden cursor 404.
- GET `/rolls/{id}` returns one currently visible immutable roll, otherwise 404.
- POST `/rolls` body `{requestId,expectedVersion,characterId,key,expression,
  visibility,advantage,disadvantage}`. characterId is UUID text or empty for an
  account roll. key is `EXPRESSION` or a saved calculation key: `ability:CODE`,
  `skill:id`, `save:CODE`, `initiative`, `attack:id`, `damage:id`, `spellAttack:id`.
  Expression rolls use the explicit expression; shortcuts require expression empty
  and an editable character. expectedVersion is that character's version, or
  campaignVersion for an account roll. Read snapshots never advance sheet/resources.
- POST `/roll-commands` body `{requestId,expectedVersion,characterId,text}`.
  `/roll EXPR [adv] [dis] [dm]` or `/check KEY [adv] [dis] [dm]`; ASCII tokens,
  no whitespace inside EXPR. Both map to the SAME normalized roll command and
  durable receipt namespace. Button flags use sources `手动优势` / `手动劣势`.

Expression grammar: signed sums of NdS or integers; dS means 1dS; spaces and D
are normalized. 1–20 terms, at most 100 dice total, 2–1000 sides, constants
0–10000 per term, 100 input characters. No eval, multiplication, keep grammar,
exploding dice or arbitrary code. Advantage/disadvantage are explicit arrays of
1–200 character source descriptions (max 10 each); they apply only to expressions
with exactly one positive d20 term. Both present cancel to one d20; repeated
sources never stack. Store every raw die, sign, kept flag, constants, total, mode,
sources, expression and chosen saved calculation (components/DM override included).
Damage uses saved attack dice plus the authoritative damage modifier; no critical
hit, hit/miss, minimum damage or effect/resource application is inferred.

PUBLIC rolls are visible to current members while their source character is
currently player-visible. DM rolls can only be initiated/read by current DMs.
Hidden or pending characters require DM visibility. Account rolls use actor name;
character rolls snapshot character name/version and actual actor. Players need
current character control to initiate/replay, independently of token control.
Public result details disclose the selected calculation snapshot intentionally,
never the rest of the sheet. Hiding a character filters prior records; historical
snapshots are never rewritten. Previously delivered public information cannot be
retracted. Revocation/foreign IDs/guessed roll IDs cannot bypass current checks.

All mutations use session/CSRF, strict required JSON fields, bounded input and
actor→campaign locks. Current membership/DM/target/control precedes receipt;
identical retries precede source-version checks and never generate new dice or
events. Fingerprints cover campaign, source version and all normalized fields.
Changed body 409; fresh stale source version 409. Result+receipt atomic in PG;
concurrent same request one effect. Server SecureRandom only in production;
deterministic injectable entropy only for unit tests. No final values from clients.
Rolls do not share a public activity version that would reveal DM-only activity.

## Initiative

All combatants reference independent P03 characters, including NPCs. Multiple map
placements do not give extra initiative slots. No token/character auto-creation.

- GET `/initiative`: DM gets `{version,status,entries,reason}`; each full entry
  has characterId/name/sourceVersion/kind/group/surprised, selected initiative
  calculation and complete dice result/sources. Players get `{status,entries}`
  with confirmed visible `{characterId,name,total}` rows only. Draft/hidden entries,
  groups, secret dice, versions, positions, counts and sources never enter payloads.
  No candidate-only change emits a player hint. Status is EMPTY until confirmation.
- POST `/initiative` `{requestId,expectedVersion,participants,reason}`: DM only,
  expectedVersion is initiative version (initially 0). participants (1–100) have
  `{characterId,expectedVersion,kind,group,surprised,advantage,disadvantage}`.
  Distinct character UUIDs; kind PC|MONSTER. PC group must be empty. A nonempty
  monster group is DM's explicit identical-creature declaration, never name based;
  all members must have equal calculation and surprise/mode/source configuration,
  or reject before drawing any dice. Roll once/group, preserve individual slots.
  Surprise adds a sourced disadvantage. Produce DRAFT sorted by total descending;
  input order is only a stable candidate tie order, never a forced rule tiebreaker.
  A new generation/re-roll replaces current candidates and appends immutable DM
  history, with reason; explicitly select complete roster (including any additions).
- POST `/initiative/confirm` `{requestId,expectedVersion,order,reason}`: DM only;
  order is an exact permutation of participant character IDs. Explicitly record
  player-decided PC ties/DM-decided monster or mixed ties and exceptional manual
  adjustments in reason. No arbitrary automatic DEX/name/ID tiebreak. Persists
  CONFIRMED order; no turn, round, movement restriction, damage or expiry tick yet.
- GET `/initiative/history`: DM only, immutable full snapshots, actual actor and
  timestamp; generation/confirmation use one separate durable receipt namespace.

Initiative ack is `{id:campaignId,version}` only; replays never restore old order.
Current authorization precedes historical receipts; roster source versions checked
on generation; confirm uses saved candidates without recomputing changed sheets.
Dynamic hiding/import review filters even confirmed history from player reads;
only DM can read history. A hidden participant produces no blank rank or group
metadata. after-commit SCENE_CHANGED hints remain exactly `{type,campaignId}`;
private dice/generation hints go to DMs only. V9/V10 preserve all V1–V8 rows/checksums.

## UI/recovery and completion

Chinese dice/initiative workspace: account or controlled character, shortcuts and
expression, modes/DM visibility, same command grammar, expandable immutable dice
and modifier sources, older visible results; DM roster/group/surprise selection,
candidate review and explicit order confirmation. Explain PC tie decisions and
manual exception source. Uncertain POST retains exact body/id for explicit retry,
locks competing edits; 409 refreshes without resubmitting. Receipt alone is not
synchronization; require a latest post-ack authoritative read. Independent 401/403
invalidates concurrent successes; source control loss clears pending/card data.
Switch campaign/identity, logout/revocation and late responses must not revive
private state; reconnect/periodic reads recover missed hints. Preserve all previous
regressions; test real DB/HTTP/WS, rollback/races, migration and actual service restart.
