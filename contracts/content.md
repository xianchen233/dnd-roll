# P04: searchable content and explicit character fill

Status: P04a–e completed, 2026-10-21. Code a7ef388 passed local and exact-SHA push/PR CI.
P01/P02/P03 are complete and retained. M2 is incomplete. Evidence: docs/verification.md.

Ordered slices: (a) provenance/catalog contract and licensed seed, (b) immutable
campaign custom entries and admission, (c) authoritative preview/fill, (d) Chinese
search and preview UI, (e) regression, migration, real browser/restart and exact-SHA CI.
Every behavior change requires an executed failing assertion before implementation.

Content identity is an opaque UUID plus immutable source book/revision/rules
(`5R|2014|CUSTOM`). Same names remain separate; publication year is not rules version.
Builtin seed is an explicitly counted **selection**, not the entire SRD or local index.
Each builtin includes source URL, CC-BY-4.0, attribution and modification notice.
Custom entries are campaign-public, immutable and carry the publisher's rights
declaration; copying/revising creates another ID and never rewrites character snapshots.
The application never reads the commercial local index at runtime.

All members may search/publish a custom entry; only DM changes admission settings.
REVIEW (default) admits the builtin 5R selection and explicitly approved custom IDs.
FREE admits all visible entries, including explicitly labelled legacy entries. Returning
to REVIEW affects future catalog fills only; existing sheets/history remain unchanged.
DM may fill any entry. Admission never grants character read/edit/token permissions.
Manual edits/imports retain P03 semantics: this is catalog admission, not a validator
of every free-text rule. Imported player sheets still require target DM review.

HTTP routes under `/api/campaigns/{campaignId}`:

- GET `/content?q=&type=&source=&rules=&admitted=`: literal case-insensitive name,
  alias, description/source keyword search; optional AND filters; deterministic name/ID
  ordering. Bounded q/source 100 chars, type/rules enum, admitted `true|false|''`.
  Returns `{campaignVersion,mode,approvedIds,entries,total,structured,automated}`.
  All entries include admission and support `DESCRIPTION`; structured is catalog
  field-fill count; automated is always zero. No dice/execution support is claimed.
- POST `/content/custom` `{requestId,expectedVersion,entry}`: campaignVersion;
  entry fields `name,aliases,type,source,description,spellLevel,dice,damageType,rights`.
  Types `SPECIES|BACKGROUND|FEATURE|SPELL|ITEM|ATTACK|RULE`. Source is P03 Source.
  Max 500 custom entries/campaign; required nonempty rights declares permission to
  share with the campaign. Custom content is not relabelled as official/open licensed.
- POST `/content/settings` `{requestId,expectedVersion,mode,approvedIds}`: DM,
  campaignVersion; replace approval list with distinct current campaign custom IDs.
  Both mutations increment campaignVersion once; return `{id,version}`.
- POST `/characters/{id}/content-preview`
  `{expectedVersion,entryId,ability,castingId}`: read-only; controller/DM only,
  current character version and current admission; returns
  `{entryId,expectedVersion,policyVersion,sheet,warning}`. No history/version/resource
  effect. ability is one of six codes (used for ATTACK only); castingId must name an
  existing casting entry for SPELL, otherwise empty. No invented casting source.
- POST `/characters/{id}/content`
  `{requestId,expectedVersion,policyVersion,entryId,ability,castingId,reason}`:
  server reconstructs the preview from immutable content and current card; checks
  character and campaign versions atomically. Uses P03 character receipts/history.
  SPECIES/BACKGROUND replaces only descriptive entry; other types append stable
  `content:<entryId>` fields. RULE appends an explanatory feature. Duplicate append
  is rejected. Never applies ability increases, HP, proficiencies, mastery, resources,
  dice or effects. Attack proficiency remains false; mastery remains unselected.
  Preview explicitly identifies replacement/manual choices; source includes entry ID.

All POSTs enforce session/CSRF and strict JSON (at most 32 KiB), required primitive fields, bounded
text/lists and no unknown fields. actor -> campaign locks, current membership and
target/control/admission before receipt; identical receipts before stale versions.
Content receipts have an independent durable actor/request namespace; changed body
409, fresh stale version 409; fill shares character namespace with P03. Replay after
revocation/hiding fails and never restores grants or applies twice. State/history/
versions/receipts are one PostgreSQL transaction; unchanged SCENE_CHANGED hints
contain no content/character secrets. no-store responses. V8 preserves all old rows
and migration checksums. Custom entries and settings survive real process restart.

UI: search from the saved character's groups; filters, details/provenance/licensing,
support and coverage shown; preview before explicit fill. Save/discard sheet drafts
first. Scope switch/revocation disposes results/previews/pending work; stale reads
cannot restore them. Uncertain mutations retain exact body/requestId for retry;
ACK requires a fresh authoritative read before claiming synchronized data.
