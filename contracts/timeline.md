# P07: RP timeline and shared bot entry points

Status: P07a–e completed, 2026-10-21; code 5d96a72 passed local and exact-SHA
push/PR CI. P01–P06 remain complete; P08–P10/A and the whole M2 are incomplete.
No new D&D calculation or automatic resolution; use the P03/P05/P06 services.

Ordered slices (execute assertion RED before implementation): a messages, speaking
identity and immutable edits; b durable images and permission-aware references;
c unified dice/action timeline, search, sessions and exports; d Chinese workspace
and shared commands; e full old/new checks, migration, browser, real restart and
exact delivered SHA push/PR CI. Only after e may P07 be closed.

All paths below are under `/api/campaigns/{campaignId}`. Session and current
membership on every read; CSRF and strict required JSON on every POST. Identity
comes from the session. Actor → campaign locks; message commands have a separate
durable `(actor,requestId)` receipt namespace. Current membership, target visibility,
authorship and character control precede receipt lookup; equal fingerprint before
stale-version checks. Replays never edit again. Changed body 409; stale fresh
version 409. Message, history, attachment references and receipt commit atomically.
No campaign/character version is advanced by RP. No deletion or expiry of history.

## Messages and references

- POST `/messages` `{requestId,expectedVersion,characterId,kind,text,replyTo,attachments}`.
  kind RP|OOC; RP requires a controlled, reviewed public character (DM can speak a
  hidden NPC privately); OOC requires empty characterId. expectedVersion is the
  source character version or current campaign management version for account OOC.
  Text is plain Unicode, 0–8000 characters; up to 4 distinct attachment UUIDs;
  at least text or an image. replyTo is a currently readable timeline UUID or empty.
  Current active session is captured server-side. Return `{id,version}` (initial 0).
  Snapshot actual actor, character ID, source version and speaking name at send time.
- POST `/messages/{id}` `{requestId,expectedVersion,text,attachments,reason}`:
  original author only (DM cannot impersonate another author), with current source
  control for RP. expectedVersion is message version; nonempty reason ≤1000.
  Replace text/images only; original identity, kind, reply, session and time remain.
  Preserve every old version, actual editor and edit time. Cannot edit dice/actions.
- GET `/messages/{id}/history`: all current members who can read this message get
  its chronological immutable versions. Every version uses current permissions.
- GET `/timeline/{id}`: typed `{id,kind,at,sessionId,data,reply}`; kind RP|OOC|DICE|ACTION.
  `data` is message view or existing P05 roll / P06 filtered action projection.
  Message view has version, actor, characterId, sourceVersion, name, text,
  attachment metadata, edited flag, canEdit. No storage keys or full character card.
  reply is null or `{id,kind}` only, reauthorized on every read; follow its detail
  explicitly. If a reference is now hidden, omit its ID/existence entirely.
  No copied quote payload, recursive quotation, hidden counts or activity versions.

Players cannot read messages from currently hidden/pending characters, private
dice, actions whose source or target is currently hidden, DM notes/reasons or
private combat event IDs. DM can read private messages/dice/action detail.
Permission changes affect list, detail, history, references, cursors, search,
downloads, export and receipt replay. Previously delivered public text cannot be
retracted. User-authored text is not inspected or rewritten for secrets.

## Images

- POST `/attachments`: multipart exactly `command` JSON `{requestId,expectedVersion}`
  (campaign version, checked without advancing) and `file`. Same strict PNG/JPEG
  validation/10MiB/8192/32M limits as P02; ignore filename. Separate receipt namespace
  `timeline-attachments`. Return metadata `{id,mediaType,byteSize,width,height}`.
  Upload alone grants no public access. Only the uploader may attach their upload;
  already referenced uploads can be reused by that author, not claimed by others.
- GET `/attachments/{id}/content`: current uploader may read an unreferenced upload;
  once referenced, access requires at least one currently readable message/history
  reference. No DM-map reuse, static URLs, storage keys or outside image links.
  Authenticated bytes with no-store/private, attachment disposition and nosniff;
  missing file 503. Recheck every request. Files persist, including removed images
  retained by older edits. Atomic publication before DB reference; on uncertain
  commit preserve file until a fresh campaign lock proves metadata absent. No GC.

## Unified reading, sessions and export

- GET `/timeline?before=UUID&q=&kind=&characterId=&sessionId=&from=&to=`: newest
  first, at most 50, `{entries,next}`. Filter current permissions before paging and
  search. Literal case-insensitive q ≤100; RP/OOC current text/name, dice public
  expression/name and action source/target/attack names only; never private reasons
  as a search oracle. kind empty|RP|OOC|DICE|ACTION, UUID filters empty or canonical
  UUID, from/to empty or ISO UTC instants (inclusive). Invalid filters 400.
  Cursor must currently be visible in this campaign, or 404. No totals/ordinals.
  New P05 rolls and P06 declarations enter transactionally; existing records are
  backfilled without changing old rows/checksums. Source details remain authoritative.
- GET `/timeline/export` accepts the same filters (no cursor), `format=markdown|json`.
  Export every matching currently readable record, oldest first, including readable
  edit histories and a deduplicated image manifest. No silent 50-row cutoff. JSON
  envelope `dnd-roll.timeline`, formatVersion 1. Markdown is readable plain content;
  imported text cannot create active HTML/image URLs. Private data only for current
  DMs. This is a campaign record export, not P09 deployment backup/restore.
- GET `/timeline/sessions`: `{campaignVersion,sessions:[{id,name,archived}]}`,
  public session labels with no counts. At most one active session per campaign.
- POST `/timeline/sessions` `{requestId,expectedVersion,kind,name}` DM only,
  campaignVersion; kind START (name 1–100, requires no active session) or ARCHIVE
  (empty name, requires active). Captures subsequent messages/dice/actions in the
  active session; archive never deletes/truncates old history. Its own durable
  `timeline-sessions` namespace and campaignVersion increments once.

After commit emit only existing `{type:SCENE_CHANGED,campaignId}` hints; operations
on hidden source messages hint DMs only. No attachment hint until a message uses it.
All timeline/image reads and failures are no-store/private, including anonymous.

## Client and acceptance

Chinese workspace: RP/OOC and current role, text/file/reply, edits/history, filter/
older pages, sessions and downloaded Markdown/JSON. Render text as text, never raw
HTML. Revoke blob URLs on refresh/unmount/scope switch/401/403. Invalidation clears
old feed/details/export/downloads before permission reads; late results cannot
restore revoked data. Independently observe concurrent authorization failures.
Uncertain send/edit/upload retains exact body/File/requestId for explicit retry and
locks competing edits; acknowledgment requires a new authoritative read. 409 reads
fresh state without resubmitting/rebasing drafts. Reconnect/periodic reads recover.

Bot shortcuts use the same selected controlled identity and existing service APIs:
`/roll` and `/check` → P05 roll-commands; `/act ATTACK_ID TARGET_UUID` → P06 actions;
`/link ACTION_UUID ROLL_UUID` → P06 LINK_ROLL using current action version;
`/end` → P06 END_TURN using current compare token. Role switching and initiative
review reuse character/dice/combat workspaces. No separate entropy, sheet numbers,
damage application or automatic resources in this workspace.

Preserve every P01–P06 regression. Verify real PG/HTTP/WS authorization, rollback,
duplicate/concurrent retries, source rename/hide/control loss, edit/image retention,
private references/search/export, >50 paging/export, V12 preservation, dual browsers,
offline/uncertain retry, and actual Java+PG / Compose restart with fresh sessions.
P08–P10/A, capacity/touch/TLS and consistent backup/other-host restore remain pending.
