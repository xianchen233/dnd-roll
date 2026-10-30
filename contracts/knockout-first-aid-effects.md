# A15 bound first-aid check and explicit knockout removal

[Source/support matrix](../docs/research/knockout-first-aid-5r.md). DM-only CSRF POSTs
under `/api/campaigns/{campaignId}/attack-previews/{id}`:

- `/knockout-first-aid-rolls`: explicitly take a reviewed aid action and save one check.
- `/knockout-first-aid-effect`: read-only plan from the latest bound check.
- `/knockout-first-aid-completions`: explicitly apply a successful plan.

Attempt body `{requestId,expectedVersion,previewHash,targetVersion,helperId,helperVersion,reviewed,reason}`.
Review body `{expectedVersion,previewHash,targetVersion,helperId,helperVersion,rollId,reviewed}`.
Completion adds `{requestId,planHash,reason}` to review. Versions nonnegative integers;
IDs UUID; hashes lowercase64hex; reason nonblank max1000. Strict required JSON fields,
no null/extra/duplicate/wrong types. No client dice/total/DC/final HP/condition/actor.

Ack `{id:previewId,version}` for both mutations. Attempt rechecks unchanged A11/A12
APPLIED provenance, canonical target HP1/tempHP0/no death saves/sole exact annotation,
distinct supported helper at submitted version and reviewed action/legal reach/exceptions.
Unsupported attempts conflict409 before dice. Source exists; its version may advance.
Helper requires HP>0/no conditions/death saves/import review, Medicine with WIS and no
modifiers/overrides. Saved flat skill bonus and proficiency/expertise are supported.

Under actor/campaign transaction, use existing server `skill:medicine` roll with DM
visibility and empty advantage/disadvantage. Append ROLL_KNOCKOUT_FIRST_AID whose event
ID equals the generated rollId; changes capture helper then target unchanged values and
versions, summary retains total/DC10 and outcome, reason retains action review. Increment
preview once, preserve status/evidence/hash; save dice/receipts/event atomically, never
change cards on an attempt. Failed checks remain durable; exact retries never reroll.
Generic dice or `/events` endpoints cannot create this bound event.
The internal dice fingerprint has a dedicated A15 namespace binding preview/version,
helper/version and calculation; pre-seeding its derived UUID through generic dice
cannot substitute a previously selected outcome and conflicts without partial writes.

Plan `{profile,status,reasons,dc,total,hp,tempHp,conditionId,targetVersion,hash}`. Profile
`5R-SRD-5.2.1-KNOCKOUT-FIRST-AID-V1`, dc10, CANDIDATE or MANUAL_REQUIRED. Total is the
saved server result (not a submitted input); manual hp/tempHp/conditionId null. Hash
binds preview/version/hash, current helper/target, review and saved roll/derived plan.
No writes/rolls/receipts when previewing. Require latest ROLL_KNOCKOUT_FIRST_AID event
ID=rollId, matching helper and target snapshots, same campaign private skill:medicine
roll/current helper calculation and version, no advantages/disadvantages, total>=10.
Any other/old check is MANUAL_REQUIRED; wrong submitted versions/hash/stage conflict409.
DM may explicitly request another attempt with a new reviewed action and request/version;
older checks cease to be eligible. A later PUBLISH alone need not invalidate a saved check.

Completion rechecks current DM and source/target/helper existence before receipt replay.
Exact replay returns original ack before stale-version/support checks. Fresh apply rederives
and compares planHash, removes exactly the canonical annotation, preserves other sheet fields,
increments target once, saves full KNOCKOUT_FIRST_AID before/after history, appends
END_KNOCKOUT_FIRST_AID (including bound check ID/DC/result in private summary), increments
preview once, receipt and visibility-safe invalidation in one transaction. Helper, HP,
resources, original attack action/evidence/hash and existing dice stay unchanged.
No migration. Generic `/events` cannot inject either new kind. A13/A14/A15 share locks
and versions: concurrent recovery has at most one effect. Same-key concurrency one dice
or removal; changed route/body409; immediate/deferred failures roll back every row.
Old receipts cannot reroll or erase later restored annotations; revocation still wins.

Private no-store headers cover successes/errors. Players never receive these private
checks, reviews, plans or histories. UI requires explicit attempt, saved-check preview,
then explicit removal; unknown outcomes retry exact captured body; known conflicts require
reread; review/helper/revision/scope changes invalidate plans. Wake-up leaves Prone manual.
