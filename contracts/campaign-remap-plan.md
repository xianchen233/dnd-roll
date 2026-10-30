# A24b offline typed campaign identity proposal

This read-only planning stage consumes a trusted native P09 archive, matching isolated
database/assets and source/target campaign UUIDs plus a caller-retained plan seed.
It does not create a campaign, write a receipt, copy an asset, release a fence or
authorize a future writer. No HTTP route, migration or frontend change.

Verify the complete A24a SAME_ID schema, all 76 tables/row digests and committed
assets in the same read-only repeatable-read transaction as planning. Any audit
blocker stops planning. Source must exist, target must differ and be absent. Compare
all typed SQL UUIDs for collisions, including other campaigns, accounts, receipts
and fixed content. Unknown tables/columns/types fail closed through A24a coverage.

Each of 76 tables has a scope and every SQL column has a declared policy. Scope is
source campaign_id, campaign.id, or explicit parent joins for character_history,
note_history and rp_message_history. app_user, global account receipts and schema
metadata are outside the copy set. Campaign-owned receipts are counted but excluded;
actor/request keys and old acknowledgements remain source-only. Membership,
controllers and invitations require new authority review. Module and content
identities require separate review; never treat every UUID as a fresh identity.

Only explicit runtime identity tables propose new IDs: campaign, scene, token,
character_card, campaign_asset, campaign_note, rp_message, note_attachment,
rp_attachment, timeline_entry, timeline_session, attack_preview, dice_roll,
combat_action and movement_journal. The namespace is the owning table. Equal source
UUIDs in different namespaces remain different identities, except explicit timeline
aliases: timeline_entry.id must equal its message_id/roll_id/action_id and reuse that
owner's proposed target. Missing, duplicate, wrong-owner or unequal aliases block.
This preserves TimelineService projection by row.id; a future writer also needs to
account for automatic timeline triggers. Proposal derivation binds
seed, source archive dump hash, source/target campaigns, namespace and original ID;
sorted input yields repeatable output. The campaign root maps to the explicitly
chosen target ID; the other namespaces use derivation domain dnd-roll-remap-proposal-v1.
The other namespace derivation uses UUID.nameUUIDFromBytes on the UTF-8 domain tuple
(identity allocation, not a credential). Distinct canonical identity groups must
have unique target IDs which must not match
any occupied typed UUID. Current SQL version values are retained, never reset;
historical/composite versions require a later typed evidence mapper. Local generated
seq/ordinal values require reassignment. Builtin/custom content IDs require explicit
content review; their UUID type does not establish which IDs remain pinned.

Only explicitly listed SQL references are checked against the selected identity
closure; null references stay null. Missing or cross-campaign targets block. Fixed
content keys, resource keys, free text, actors and opaque archives are never scanned
or substituted. Immutable storage keys require an independent copy plan. Nested
JSON, source-bound canonical annotation strings, receipt fingerprints, saved-plan
hashes, historical version chains and module graphs require typed follow-up work.

The result always has status BLOCKED, executable=false and REMAP_WRITER_NOT_IMPLEMENTED,
TARGET_AUTHORITY_REVIEW_REQUIRED and HASH_REBIND_NOT_IMPLEMENTED. Occupied JSON,
module and asset scopes add their own blockers. This is an identity proposal with
an explicit coverage/gap ledger, never READY or an applied migration.

Output contains UUID mappings, current versions, source dump hash, fixed metadata,
scoped row counts and fixed blocker codes only. No game JSON, actor names, private
reason text, SQL values other than declared identities/versions, credentials or host
paths. No JSON traversal is performed. References must match their declared SQL
column namespace. Scope, column and runtime-identity row coverage are checked.
Limits are 10,000 identities, 100,000 references and 1,000,000 occupied typed UUIDs;
exceeding them fails without output. These are safety limits, not M3 capacity
acceptance. Output CREATE_NEW, never replace a report. CLI takes seven arguments:
archive, assets, output, sourceCampaign, targetCampaign, seed, literal REVIEW_ONLY;
uses RECOVERY_AUDIT_DATABASE_URL/_USERNAME/_PASSWORD. Exit 2 for a written blocked
proposal, 1 for failed verification/input/output. No success exit 0 in A24b.

Acceptance: actual offline RED/GREEN for namespace isolation, determinism, collisions,
missing references, version/coverage rules, immutability and failure status; real PG
tests for selected campaign isolation, parent histories, receipt exclusion, read-only
rows, archive/schema mismatch and private output. Synthetic fixtures use a new port
and fresh data only. A24a archives, services, assets and fences are retained untouched.
Full runtime JSON extraction/writer, independent new-campaign continuation, capacity,
delivery-HEAD CI, cloud and physical-phone verification remain pending.
