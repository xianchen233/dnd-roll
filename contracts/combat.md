# P06: manual combat and linked corrections

Status: P06a–e completed, 2026-10-21; code d23c592 passed local and exact-SHA
push/PR CI. P01–P05 remain complete; P07–P10/A and M2 remain incomplete.
Rules and manual boundaries:
[5R evidence](../docs/research/combat-5r.md). No automatic attack resolution.

Ordered slices: a action/target/server-roll association; b DM verdict, explicit
application and linked correction; c character turns and movement permission;
d Chinese workspace/recovery; e full regression, migration, browser, actual
service restart and exact delivered SHA push/PR CI. Do not close P06 before e.

All routes below are under `/api/campaigns/{campaignId}`. Session/CSRF and strict
required JSON apply. Actor→campaign locks; separate durable combat receipt
namespace `(actor,requestId)`, fingerprint includes operation/campaign/target and
all fields. Current membership, visibility, source control and DM checks precede
receipt lookup; identical receipt precedes versions. Replays never apply twice.
Changed request 409; stale fresh versions 409. State/history/character histories/
versions/receipt commit atomically; failures roll back. Same request races have
one effect; same version races one winner. Every mutation emits after-commit
existing secret-free invalidation only, private operations to DMs only.

## Actions

- GET `/actions?before=UUID`: newest 50 currently visible actions with `next`
  visible cursor or empty. Filter before pagination; hidden/foreign cursor 404.
- GET `/actions/{id}`: current permission-filtered action and immutable events.
- POST `/actions` `{requestId,expectedVersion,characterId,targetId,attackId,
  visibility}`. expectedVersion is source character version; source/target must
  be independent same-campaign characters. Source controller or DM; public actions
  require both characters player-visible and reviewed. Saved attackId required.
  Snapshot selected attack/source/name/version and target name. No roll or resource
  change. Actions may be declared outside combat or outside the source's turn.
- POST `/actions/{id}/events` `{requestId,expectedVersion,kind,rollId,verdict,
  changes,correctionOf,reason}`. expectedVersion is action version. Unused strings
  are empty and changes is empty. kind LINK_ROLL attaches a currently readable
  immutable P05 roll of the same source and saved attack/damage key; the controller
  must be its actual roller (DM can link any). Deduplicate roll IDs per action.
  Source changes do not rewrite snapshots. Public action may include DM-only dice;
  linking those and their events never exposes their existence to players.
- kind VERDICT: DM only; verdict HIT|MISS|OTHER; records manual judgement and
  private reason, never infers AC, critical hits or resistance. No resource change.
- kind APPLY: DM only, requires a verdict; explicitly sets current HP/temp HP and
  selected resource values for source/target. At most one APPLY per action.
  `changes:[{characterId,expectedVersion,hp,tempHp,resources:[{id,current}]}]`.
  Every affected character version must match; distinct IDs; validate against
  current bounds. Zero HP/death marks/conditions, resistance, critical hits,
  healing and concentration remain explicit DM decisions. The UI explains that
  damage normally consumes temp HP first but final numbers require DM confirmation.
  Dice and verdict never consume resources. No implicit application of roll total.
- kind CORRECT: DM only, nonempty correctionOf points to an immutable event of
  this action. Can append a replacement verdict and/or explicit current-version
  changes. Preserve original and all later events; never restore an old sheet or
  subtract an old delta automatically. Reason required. Further corrections can
  reference a previous correction. No-op/empty correction rejected.

DM detail includes every raw roll/source, actual actor/time, DM reason, linked
event and full before/after changes. Players see public visible source/target
identity snapshots, selected attack, PUBLIC currently visible rolls and manual
verdict/application/correction markers. Never private reasons, full sheets,
precise HP/resources, private event IDs, global sequence or full action version.
  Only source controllers get mutationVersion for linking public rolls; noncontrollers
get no command version. Hiding either source or target removes prior action list,
detail and cursor; current authorization also applies to historical receipts.
Already delivered public information cannot be retracted.

## Turns and movement

- GET `/combat`: DM full `{version,status,round,order,currentId,grants,reason}`;
  players visible order/name only, current visible character or empty, and
  opaque random `endVersion` compare token only when controlling the currently
  visible character; it is not a global counter and DM grants leave it unchanged. No hidden
  ranks, counts, grants, reasons, round or global version in player payloads.
  An entirely hidden roster projects IDLE/empty, emits no player hint and leaves
  player exploration movement unchanged; it must not disclose itself by locking maps.
- POST `/combat` `{requestId,expectedVersion,kind,characterId,order,reason}`.
  START (DM) copies the latest CONFIRMED P05 order into independent combat state;
  round 1/first character. END_TURN current source controller or DM advances once,
  last wraps to next round. SKIP (DM) same advance with explicit reason. SWITCH
  (DM) selects roster character. ORDER (DM) exact roster permutation, current stays.
  GRANT (DM) allows chosen roster character to move out of turn for the remainder
  of the current turn. STOP (DM) restores exploration movement. Other commands
  clear grants. A publicly observable transition rotates the compare token; player END_TURN
  uses that token as expectedVersion, while DMs use the full state version.
  Every change appends immutable DM history (GET `/combat/history`).
  Source permission and roster membership are current, including on receipt replay;
  END_TURN names the source character so stale replays cannot end somebody else's
  turn. Replay itself remains single-effect after current turn changes.
- Existing move request/response shapes remain intact. Exploration is unchanged.
  During ACTIVE combat, player moves require existing visible token control AND
  a linked, visible, currently controlled character that is current or DM-granted.
  Unlinked/nonparticipant tokens require DM direct adjustment. DM moves any token.
  Current combat permission precedes historical move receipts. Snapshot canMove
  reflects it. No path/budget/effect automation. Out-of-turn dice/actions/resources
  remain available; turn advance does not silently tick/remove conditions. P03
  explicit DM tick/removal remains the duration confirmation entry.

## Recovery and acceptance

Chinese UI: select independent source/attack/target, declare action, attach existing
server attack/damage dice, DM verdict, explicit HP/temp/resource application,
expand immutable source/journal and append correction. Show DM current round/order,
end/skip/switch/reorder/grant/stop and explicit condition-duration reminder.
Ambiguous POST retains exact body/id and locks competing edits; retry exact command,
then require a new authoritative read. 409 refreshes without resubmitting; preserve
original draft version until user reselects. Authorization denial clears private
state and invalidates concurrent successes. Scope switches/unmount and revocation
cannot revive late private data. Reconnect/periodic invalidations reread all state.
Keep all P01–P05 tests; verify real PG/HTTP/WS rollback/races, old migration
rows/checksums, real dual-browser offline/retry and real database/backend restart.
