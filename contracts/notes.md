# P08: team notes and DM preparation

Status: in progress. P01–P07 remain complete; P08 closes only after full old/new
checks, migration, production browsers, actual service/container restart and exact
delivered SHA push/PR CI. No merge or release. P09/P10/A remain pending.

Ordered slices: a durable notes/history/current authorization and receipts; b
private persistent images, search/paging/export; c Chinese workspace with uncertain
retry and permission/scope isolation; d complete acceptance. Execute focused RED
before implementation. This adds no game rules or character/turn mutation.

All paths use `/api/campaigns/{campaignId}` and authenticated current membership.
TEAM notes are readable and editable by every current member; DM notes only by
current DMs. No personal player notes. Visibility TEAM|DM is immutable: publishing
selected prose requires a new TEAM note, never exposes a private revision history.
Hidden/foreign/unknown note or image IDs return 404 after membership checks;
nonmembers 403, anonymous 401. DM-only creation/upload by players returns 403.

## Notes and history

- GET `/notes?q=&visibility=&before=` → `{campaignVersion,notes,next}`. Literal
  case-insensitive q ≤100 searches current title/text only. visibility empty|TEAM|DM;
  DM filter by a player returns an empty page. Newest-created first; max 50,
  permissions applied before paging/search, no hidden totals/ordinals. Cursor UUID
  must currently be readable in this campaign or 404. Invalid filters 400.
- GET `/notes/{id}` → `{id,version,visibility,title,text,attachments,actor,at,editedAt}`.
  `actor` is the actual last editor; every revision records actual editor/time.
- POST `/notes` `{requestId,expectedVersion,visibility,title,text,attachments}`.
  expectedVersion is campaign management version (checked, never advanced).
  Title 1–200 plain Unicode characters, text 0–16000, up to 4 distinct image UUIDs.
  Initial version 0. Return `{id,version}`.
- POST `/notes/{id}` `{requestId,expectedVersion,title,text,attachments,reason}`.
  expectedVersion is note version; nonblank reason ≤1000. Replace content only,
  increment once; preserve initial time/visibility and every previous revision.
- GET `/notes/{id}/history` → chronological revisions `{note,reason}`, with the
  full note fields. Reauthorize current membership/visibility on every read.

CSRF, required fields, strict JSON types and unknown-field rejection for POSTs.
Actor → campaign locks, independent `notes` receipt namespace `(actor,requestId)`.
Current membership/visibility/attachment authority before replay; identical
fingerprint replays the original ack before stale version checks. Changed payload
409 IDEMPOTENCY_CONFLICT; stale fresh request 409 VERSION_CONFLICT. Note/history/
references/receipt are atomic. No deletion or expiry; no character/scene changes.
After commit only existing secret-free SCENE_CHANGED hints; DM notes hint DMs only.

## Images and export

- POST `/note-attachments`: multipart exactly `command` JSON
  `{requestId,expectedVersion,visibility}` plus `file`; campaign version checked
  without advancement. Same PNG/JPEG decoding/10MiB/8192/32M limits as P02.
  Return `{id,mediaType,byteSize,width,height}`. Independent durable upload receipts.
  Visibility is immutable and must match the note; no map/RP image reuse. Uploader
  can attach their current readable upload; collaborators can retain/reuse images
  already in this same note's history, never claim another author's loose upload.
- GET `/note-attachments/{id}/content`: upload alone readable only by its uploader
  with current membership and visibility; once referenced, requires a currently
  readable note/history. DM visibility also requires current DM even for uploader
  and receipt replay. Retain images removed in edits. No storage keys/filenames/
  static URLs; validated original bytes with attachment disposition, nosniff and
  no-store/private. Missing file generic 503. Atomic file publication and DB save;
  clean confirmed rollback only after a fresh campaign lock, preserve on uncertain
  commit/DB unavailability. No GC.
- GET `/notes/export?q=&visibility=&format=markdown|json`: complete matching
  currently readable notes, chronological history and deduplicated image manifest,
  no 50-row cutoff. JSON `{format:'dnd-roll.notes',formatVersion:1,notes,histories,
  attachments}`. Markdown escapes raw content as code, no executable HTML/images.
  A record export with attachment manifest, not P09 database+file backup.

All note/image routes, including failures, are no-store/private and nosniff.
Lists, detail, history, search, cursors, exports, attachment bytes and retries share
the same current authorization. No embedding note data in RP references; existing
P07 accepts its own timeline identities only. Authored text is not scanned for
secrets; already delivered copies cannot be retracted.

## Client and acceptance

Chinese team/DM selection, create/edit/history, text and image download, literal
search, older pages and Markdown/JSON export. Render text only, never raw HTML.
Clear private views/blob URLs before invalidation reads; reject late results on
refresh, campaign/API/role change, unmount or denial. Independently observe every
concurrent 401/403. 404 clears stale selected detail. Preserve exact body/File/key
for explicit uncertain retry, lock competing writes, require fresh authoritative
read after ack. 409 refreshes without silently rebasing/resubmitting drafts.

Preserve all P01–P07 regressions. Test two editors, duplicate/concurrent requests,
rollback, stale versions, removal/rejoin/DM loss, anonymous/CSRF/foreign IDs, strict
JSON, literal search, private cursor and all export/history/download paths, >50
complete export, image hashes/old edits, V15 rows/columns/15 checksum preservation,
production dual clients/offline/uncertain retry and fresh-session actual Java+PG
and Compose restart. Capacity/touch/TLS/consistent backup/other-host restore remain
P09/P10 gates.
