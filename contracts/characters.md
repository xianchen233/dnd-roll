# P03: complete semi-automatic character sheets (5R)

Status: P03a–e completed, 2026-10-20; code f66c4e7 passed local and exact-SHA push/PR CI.
The whole M2 remains **incomplete**. Evidence: docs/verification.md.
Rules and deliberate manual boundaries: [5R evidence](../docs/research/character-sheet-5r.md).
P02 tokens are map representations, never character sheets or resource owners.

## Ordered delivery and completion gate

1. P03a: independent character identity, six field groups, multiple characters per
   account, persistence, membership/control/visibility, versioned edits and history.
2. P03b: server calculations with components/source, DM overrides with reasons;
   players cannot change overrides. Growth remains an explicit edit, no auto-builder.
3. P03c: independent HP/temp HP, death marks, hit dice/spell slots/feature/custom
   counters; explicit consume/recover, conditions and DM-confirmed expiry reminders.
4. P03d: copy, versioned structured export/import, target campaign review and
   optional multiple token placements referencing the same character.
5. P03e: all previous regressions, new real HTTP/PG/WS/migration/browser/offline/
   restart coverage; exact delivered SHA push and PR CI. Only then close P03.

Each slice requires executed assertion RED → minimum implementation → GREEN.
Compile/setup failures are separate. Keep the existing 61 units, 61 integration,
166 frontend, 31 HTTP operations, 17 browser scenarios and four restart groups.

## Data and permissions

Character has server UUID, campaign, version, hidden/shareFull flags, a set of
current campaign controllers, an importReview flag and one typed sheet. Sheet:

- name, avatar (short text/emoji marker), species, background (including selected
  ability increases, origin feat, proficiencies/equipment links), class/subclass
  levels; each content entry carries book, revision, rules and detail.
- final six ability scores, all 18 standard skills, six saves, per-check ability,
  proficiency (NONE/PROFICIENT/EXPERT; saves disallow EXPERT) and sourced bonus.
- combat: manual sourced HP maximum/current/temp, speed, death successes/failures,
  selected AC (UNARMORED or MANUAL), manual AC and source.
- attacks with chosen ability, proficiency, hit/damage bonuses, damage dice/type,
  actual mastery choice and source; multiple independent spellcasting entries and
  spell list with level/prepared/casting association and source.
- features (including origin feats, species traits, class features, chosen options),
  resources (HIT_DICE/SPELL_SLOT/PACT_SLOT/FEATURE/CUSTOM, current/max/recovery/source).
- items (quantity/equipped/description/source), five coin denominations, notes;
  conditions (stable local ID/name/icon/description/source/optional remainingRounds
  and ISO UTC expiresAt), sourced calculation modifiers and DM overrides.

Manual entries do not claim rules legality or automatic execution. Final abilities
already include background increases; those links explain choices and are never
added twice. This slice supplies a text/emoji avatar marker explicitly; bitmap
avatar upload is not implemented. Content catalog/search/approval policy and dice
execution remain P04/P05 respectively.

Current members can create multiple independent sheets, initially controlled by
the actor, public combat summary, unshared full sheet. DM has implicit full access.
Hidden characters are absent from all player responses, even former controllers.
Controllers can edit full sheets; shared-full viewers can read/copy/export current
data but cannot edit or see private historical snapshots. Other members see only
name/avatar/version, AC/speed, coarse health (UNHURT/HURT/DOWN), condition labels.
No precise HP/resources/notes/sources/controllers/history enters summaries.
Pending imported sheets are visible only to controllers and DM until DM review.
Token control, sheet control and content approval are independent.

DM changes controllers/hidden/shareFull and confirms import review. Member removal
removes character controls campaign-wide and advances affected character versions,
with an actual-actor history entry. Rejoining and historical receipts never restore
grants. Old forms conflict. All lookup/history/export/copy/placement/replay routes
reauthorize current membership and visibility first. Unknown/foreign/hidden: 404;
visible but unpermitted: 403. No secrets in errors or realtime invalidations.

## HTTP, commands and persistence

Canonical routes (the relative descriptions below specify their bodies):

`GET /api/campaigns/{campaignId}/characters`
`POST /api/campaigns/{campaignId}/characters`
`GET /api/campaigns/{campaignId}/characters/{id}`
`POST /api/campaigns/{campaignId}/characters/{id}`
`GET /api/campaigns/{campaignId}/characters/{id}/history`
`GET /api/campaigns/{campaignId}/characters/{id}/access`
`POST /api/campaigns/{campaignId}/characters/{id}/access`
`POST /api/campaigns/{campaignId}/characters/{id}/resources`
`POST /api/campaigns/{campaignId}/characters/{id}/conditions`
`POST /api/campaigns/{campaignId}/characters/{id}/copy`
`GET /api/campaigns/{campaignId}/characters/{id}/export`
`POST /api/campaigns/{campaignId}/characters/import`
`GET /api/campaigns/{campaignId}/characters/{id}/placements`
`POST /api/campaigns/{campaignId}/characters/{id}/placements`

Under `/api/campaigns/{campaignId}/characters`:

- GET → `{campaignVersion, characters:[Summary]}`.
- POST `{requestId,expectedVersion,sheet}` → `{id,version}`; campaignVersion is
  required for create/import, advanced once; actor becomes sole explicit controller.
- GET `/{id}` → `{id,version,canEdit,canManage,sheet,calculations,expiredConditionIds}`.
- POST `/{id}` `{requestId,expectedVersion,sheet,reason}` → acknowledgement. Full
  replacement retains stable IDs; expectedVersion is the character version.
- GET `/{id}/history` → ordered immutable before/after sheet/access snapshots with
  version, actor, kind, reason, timestamp; controllers/DM only. Never rewrite history.
- GET `/{id}/access` and POST `/{id}/access`
  `{requestId,expectedVersion,controllers,hidden,shareFull,approveImport,reason}`:
  DM only, all controllers distinct current members; returns acknowledgement.
- POST `/{id}/resources` `{requestId,expectedVersion,resourceId,delta,reason}`:
  controllers/DM, integer delta, reject out of bounds (no silent clamp).
  Reserved IDs `hp` and `tempHp` adjust those separately. tempHp gain replaces
  through explicit sheet edit; positive delta is forbidden (never additive stacking).
  Resource edits never roll dice or implicitly apply damage/healing rules.
- POST `/{id}/conditions` `{requestId,expectedVersion,action,conditionId,reason}`:
  DM only; TICK decrements positive remainingRounds once, never removes; REMOVE
  explicitly confirms removal. conditionId is null for TICK. Wall-clock expiry
  uses server Clock; reads never mutate state. Players cannot remove an expired
  condition through full replacement; expired indicators/reminders are DM-only.
- POST `/{id}/copy` `{requestId,expectedVersion,name}`: full-read permission and
  source character version; new UUID, version 0, independent values, sole actor
  controller, public/unshared, no history/grants/placements copied; overrides cleared.
  A source awaiting import review stays pending in its copy; copying cannot bypass review.
- GET `/{id}/export` → `{format:"dnd-roll.character",formatVersion:1,rules:"5R",sheet}`.
- POST `/import` `{requestId,expectedVersion,document}`: strictly validate envelope
  and every nested field/limit atomically. Creates new identity in target campaign;
  no campaign/user/permissions/history/token IDs accepted. Overrides cleared with
  visible explanation in UI; a player's import requires target DM review, DM's
  import is reviewed. P04 broader content admission remains explicitly unfinished.
- GET `/{id}/placements` (DM only) and POST `/{id}/placements`
  `{requestId,expectedVersion,tokenId,tokenVersion,reason}`: DM links a same-campaign
  token (including inactive scenes), checks both versions, advances character,
  token and scene once. A token has at most one character; relinking to another
  character requires a fresh command for that target. No resource copy. Existing
  shared token/movement payloads and all P02 behavior retain their exact shapes.

All POSTs use session CSRF; strict JSON, required fields, integer versions,
bounded arrays/text, no unknown/null fields except documented optional duration/
conditionId. Source rules are explicit `5R|2014|CUSTOM`; revisions separate from
book/publication year. Only 5R basic formulae execute; unsupported entries are text.

Acquire actor → campaign locks. Character-command receipts use independent durable
`(actor,requestId)` namespace across all character mutations. Fingerprint includes
kind, campaign/target, expectedVersion and every command field; canonical sorted
controllers. Current authorization/target ownership precedes receipt lookup, then
identical receipts precede stale-version checks. Receipts contain only `{id,version}`,
not historical private sheets. Changed payload 409 IDEMPOTENCY_CONFLICT; fresh stale
version 409 VERSION_CONFLICT. Every mutation/version/history/receipt is atomic;
For create/copy/import, also authorize the **result** identity's current visibility
before returning its receipt; a later-hidden result gives 404, never its ID.
identical concurrent retries one effect, same-version commands one winner.
No-op edits still advance once, replays never mutate or emit new events.

V7 preserves V1–V6 rows/checksums, uses same-campaign foreign keys for controls and
placements, separate append-only history rows and bounded typed sheet JSONB, no
whole-campaign document. Server recalculates output; client totals are not accepted.
Emit only existing after-commit SCENE_CHANGED `{type,campaignId}`. Reads/responses
use no-store; logout/revocation and scope switches dispose client private state.

## UI and acceptance

Chinese character workspace supports six groups, individual field/list editing,
calculation breakdown/source, histories, DM access/overrides, counters, conditions,
copy/download/paste-or-file structured import with preview/review explanation.
On ambiguous POST retain exact body/requestId for explicit retry and lock competing
edits. Receipt alone never claims synchronization: require successful latest
post-ack authoritative read. Preserve a draft's original version; conflict refresh
does not auto-submit. Independent concurrent authorization denials invalidate late
successes; switching campaign/character clears old operations and private data.
Reconnect and periodic reads refresh permission-filtered state. Full-card access
revocation clears full data even when the public summary remains visible.
