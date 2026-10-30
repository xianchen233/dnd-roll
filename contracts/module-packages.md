# A01: module package identity and import validation

[A03g format v3](module-portable-operations.md) adds an inert operations envelope
with strict stable references. V1/v2 retain their exact original archive shapes;
all ZIP/image/hash/security bounds and current-DM authorization below still apply.

Status: completed (2026-10-22), code 2a9bb2a; local and exact-SHA push/PR CI passed.
A02 customization/campaign creation, A03 updates/triggers
and automation are separate work packages. Packages are inert DM preparation;
import never creates a playable campaign, executes rules or runs package code.

## Exchange format v1

ZIP (maximum 10 MiB compressed, 32 MiB expanded, 130 entries), exactly
`manifest.json`, `content.json` and `assets/<canonical lowercase UUID>` for each
declared image. No directories, extra entries, duplicate names, paths, scripts,
HTML, SVG or executable fields. Never extract archive paths to the filesystem.
JSON uses exact required fields/types, rejects duplicates/unknown/null fields.
Manifest/content each <= 1 MiB; assets use the existing real PNG/JPEG validator
(10 MiB/image, 8192px/dimension, 32 million pixels). Lengths and SHA-256 are checked
against actual decompressed bytes. Hashes establish integrity, not authorship.

Manifest fields: `format: "dnd-roll.module"`, `formatVersion: 1`, `moduleId: UUID`,
`moduleVersion: "major.minor.patch"` (nonnegative integers, no leading zeros,
each <= 999999), `name` (1–100), `rules: "5R"|"2014"|"CUSTOM"`, `rights` (1–1000,
publisher's declaration of permission to share), `dependencies`, `content`, `assets`.
`content: {size,sha256}` describes content.json. Each asset has exactly
`{id,mediaType,size,sha256}`. Each dependency has exactly `{id,version,rules}`:
id is a P04 content identity, version is its exact source revision (1–80 chars),
rules its exact rules family. Up to 128 assets/100 dependencies, no duplicate IDs.
Same-name or other-version content never substitutes for a missing dependency.

Content is `{nodes: [...]}` (up to 500; empty is valid). Each node has exactly
`{id,kind,name,text,visibility,nodeIds,assetIds,dependencyIds}`. Kinds are
`CHAPTER|SCENE|UNIT|ENCOUNTER|DOCUMENT|REWARD|TRIGGER`; visibility `DM|PUBLIC`.
All IDs are stable canonical UUIDs; node identity is unique across kinds. Name
1–100, text <= 10000 literal characters. Reference arrays are distinct, max 500;
all node/image/dependency references must resolve inside the package declarations.
References/cycles are data only. Rendering uses escaped text, never HTML/Markdown
execution; TRIGGER is an inert descriptive placeholder until A03.
V1 deliberately stores descriptions and stable links. [A02 v2](module-lifecycle.md)
adds explicit typed scene/unit/placement definitions; v1 remains valid and promotes
to an editable v2 document with empty play. Trigger operations remain A03 work.

## Authority, preview and import

Routes under `/api/campaigns/{campaignId}/module-packages`, current DM only,
including list, preview, download and historical receipt replay. Never expose
package titles, images or dependency diagnostics to players/nonmembers. CSRF,
no-store/private and nosniff apply. Filename supplied by browser is ignored.

- GET base: `{campaignVersion,packages:[{id,manifest,sha256,missingDependencies}]}`.
- POST `/preview`: multipart exactly one `file` ZIP; read-only, returns
  `{manifest,sha256,nodes,missingDependencies,existingId,requiresLegacyConfirmation}`.
  No version/receipt/state side effects. Dependencies are checked against the
  current campaign's P04 catalog by ID AND exact revision AND rules, independent
  of content admission (DM can inspect all entries). Missing dependencies remain
  visible and do not forbid storing an inert template; they forbid any later use
  until separately resolved. Internal dangling refs/corrupt files reject the package.
- POST base: multipart exactly `command` JSON + `file` ZIP. Command exactly
  `{requestId:UUID,expectedVersion:nonnegative integer,createCopy:boolean,
  confirmLegacy:boolean}`. Non-5R requires explicit confirmLegacy; never converts
  rules. Validate everything again server-side; preview is not authority.
  Return `{id,version}`. Actor then campaign lock, current DM before receipt,
  identical retry before stale version. Request fingerprint includes campaign,
  expectedVersion, both flags and archive SHA. Changed same request -> 409.
  Fresh stale version -> 409. Max 100 packages/campaign.
- GET `/{id}/content`: original validated ZIP attachment, current DM, stable bytes;
  unknown/foreign ID -> 404 only after DM authorization.

Module identity is (moduleId,moduleVersion); library record ID is separate.
An exact same archive with same identity defaults to the existing record without
advancing campaignVersion; a new request gets its own durable receipt. Explicit
createCopy creates another independent library record and advances once. Different
bytes claiming an existing identity/version return 409 MODULE_VERSION_CONFLICT,
even with createCopy; publish a new moduleVersion. ZIP byte identity is intentional:
repacking requires a new version. This does not claim semantic ZIP equivalence.

Imported compressed ZIP is bounded BYTEA in PostgreSQL, including all its images,
so library record/bytes/version/receipt commit in one transaction with no orphan
file publication. Failure leaves no library rows, playable state, version or receipt.
Full P09 logical archives automatically retain package bytes and table digests;
module download is not a deployment backup. Events keep the existing secret-free
SCENE_CHANGED hint. A02 must retain these source identities and validate readiness
again before creating independent customized/campaign state.

Ordered TDD slices: archive validation; authorized transactional library/HTTP;
Chinese preview/import/download UI; migration, regressions, restart/backup and
exact-HEAD push/PR verification. Record actual RED/GREEN and setup errors in
docs/a01-tdd.md. Cloud/TLS/physical cross-host recovery and real-phone checks stay
deferred as requested. No merge or release.
