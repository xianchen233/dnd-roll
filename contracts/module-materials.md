# A03f: explicit publication of illustrated DM materials

Add `{kind:"PUBLISH_MATERIAL",noteId,expectedVersion}` to the existing manual
combination contract. This is a separate explicit choice from `PUBLISH_TEXT`,
which continues to reject attached notes. Both kinds share one note-target
identity: a combination cannot publish the same source twice by mixing kinds.
No new route, database migration or automatic trigger is required.

Only the current target-campaign DM can save, review, execute or replay. A
material source must be a current DM note in this campaign at the exact version;
its author can be another DM. At most four images retain their original order.
Review validates every source image's current readability, DM visibility, actual
bytes, SHA-256, media type, byte length and decoded dimensions. The impact binds
the note snapshot and image metadata/hashes. Missing or inconsistent files block
the whole combination before any earlier reveal, entropy or public write.

Execution recomputes that same impact. It creates a new TEAM note through P08
with the literal source title/text and independent TEAM image IDs and immutable
storage keys. It never changes source visibility, shares private image identities,
copies source revisions/reasons or exposes private actor evidence to players.
Published images are readable only under ordinary P08 membership/reference rules;
the original DM note/images/history stay private. Later source edits do not
retract material members have already downloaded.

The existing campaign transaction commits public note/history/image references,
all other combination steps, child receipts and immutable execution evidence
together. Immediate and deferred rollback remove only new public files; originals
remain intact. An unknown commit outcome is reconciled under a fresh campaign
lock; committed or unresolvable files are retained. No unreferenced file is
downloadable. Exact retry returns the original receipt and creates no extra note,
image or file; current DM authorization precedes receipt replay. Fresh competitors
must still satisfy all object/version/hash checks.

Chinese controls offer “公开资料及附图”, show the source image count and explain the
new TEAM copy before confirmation. They retain the existing original-body retry,
write/read separation and campaign/API/revocation isolation. Players never fetch
the operation editor or evidence. Existing PUBLISH_TEXT remains unchanged.

P09 already includes note tables, histories, receipts and referenced immutable
files. Acceptance adds real HTTP/player image-hash checks, rollback/races,
production browser publication and exact evidence after restart/isolated restore.
Portable operation definitions are a separate next slice: they require a versioned
stable-identity exchange contract and explicit per-instance version binding.
No cloud or real-phone validation, commit, merge or release is implied.
