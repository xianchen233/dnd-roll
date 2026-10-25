# A06: declared paths and advisory movement budgets

5R profile `GRID_5R_5FT`, [evidence](../docs/research/movement-5r.md).
This is a declared-path aid, not a collision, terrain or effects engine. Each
request explicitly acknowledges `reviewed=true`: the map represents 5-foot
squares and the actor has reviewed terrain/obstacles/applicable effects with DM.
Calibrated maps whose distancePerCell is not 5 require manual movement instead.
No hidden token, obstacle or terrain data is consulted or disclosed.

Routes under `/api/campaigns/{campaignId}/tokens/{tokenId}/movement`:

- GET: current visible controlled token, linked currently controlled card, scene
  version, character version, opaque budget version, random turnKey, tracked,
  allowance (current sheet speed unless DM overridden), used, complete, latest
  50 DM-only journal records. Players receive no journal/reasons/other cards.
  A visible uncontrolled token returns 403, hidden/missing token/card 404.
  A controlled visible unlinked token returns only `{available:false}` (200),
  a normal unavailable capability without character, turn or budget fields.
  Current P06 turn/grant permission is required for preview/commit, including
  before receipt replay. Reading is allowed outside the character's turn.
- POST `/preview`: read-only authoritative calculation with the exact path body.
- POST `/paths`: atomically saves final position, immutable declared steps,
  calculation, card speed/source snapshot, actual actor/time, budget and receipt.
  Body: `{requestId,expectedVersion,sceneVersion,characterVersion,budgetVersion,
  turnKey,reviewed,path:[{x,y,mode,difficult}],override,cost,reason}`.
  1..200 entered cells, starting after current token position. Ordinary steps
  must be adjacent (including diagonals), distinct from the previous cell and
  in bounds; revisiting earlier cells counts again. WALK costs 5 feet; CRAWL,
  CLIMB and SWIM cost 10 without special speeds. Difficult terrain adds 5,
  once per cell. Multiple modes share spent distance. No shortest-path substitution.
  Supported modes never infer special speed, Dash, condition or feature effects.
  DM-only `override=true` with nonempty reason chooses whole-path final cost
  (0..100000), allows nonadjacent repositioning and mode MANUAL. Otherwise cost=0
  and reason="". Unknown modes/invalid fields are 400, not partial calculations.
- POST `/adjustments`: DM-only `{requestId,expectedVersion,sceneVersion,
  characterVersion,budgetVersion,turnKey,used,allowance,reason}`. Explicit current
  final used/allowance values (0..100000), required reason; marks complete.
  Never moves a token, rewinds history or changes the character sheet.

Budget ownership is character identity, shared across placements/scenes and
controllers. Tracked only in active combat for a roster character. START initializes
all participants; each next actual turn entry resets only that character's used/override;
GRANT/ORDER do not reset; SWITCH to the already-current character does not reset.
Out-of-turn granted movement spends the same character budget until next entry.
STOP/exploration offers per-path cost with no turn-budget claim. Upgrade during
active combat starts incomplete until a new turn entry or explicit DM adjustment.
Unmeasured legacy `/moves` keeps its protocol and behavior but marks any existing
tracked character budget incomplete (including DM direct moves). Old coordinate
receipts replay without changing the ledger. Token authoring/relinking is checked
by an anchor fingerprint of all placements; changes make completeness unknown.

Over-budget remains a warning, including negative remaining distance. `complete`
false always warns that remaining is provisional. No automatic effect execution,
attack/opportunity/reach legality, wall corners, occupied-space legality, 3D,
flight/burrow/special speeds, jumping or expansion automation is claimed.

Identity/membership/token + card control/current visibility precede receipts.
Actor→campaign locks serialize new global `(actor,requestId)` path/adjustment
receipts; same exact retry returns original response before versions, changed
route/token/campaign/body is 409. Fresh requests check token, scene, card, budget
and turnKey versions. Position/budget/history/receipt roll back together on failure.
History is DM-only; no private reasons or global combat counters reach players.
Only existing secret-free after-commit invalidations are emitted.

UI exposes explicit entered cells/modes/terrain, server preview then separate
commit. Uncertain writes lock edits and retain exact body/id; successful POST with
failed read permits read recovery only. Refresh never rebases the draft. New
selection explicitly discards stale drafts. Campaign/API/role/token switches and
authorization failures invalidate late results and clear private state.
