# A14 explicit completion of the original knockout Short Rest

[5R source/support matrix](../docs/research/knockout-rest-5r.md).
DM-only routes under `/api/campaigns/{campaignId}/attack-previews/{id}`:

- `POST /knockout-rest-effect`: read-only authoritative plan, with CSRF.
- `POST /knockout-rest-completions`: explicit atomic removal, with CSRF.

Review `{expectedVersion,previewHash,targetVersion,minutes,interrupted,reviewed}`.
Versions are nonnegative integers; previewHash 64 lowercase hex; minutes an integer
0–100000 representing DM-adjudicated continuous game time from this original knockout;
interrupted/reviewed booleans. Apply adds `{requestId,planHash,reason}` with UUID,
64 lowercase hex and nonblank max1000 reason. Strict JSON: no extra/missing/null,
duplicate or wrongly typed fields. No supplied target ID/condition/final HP/actor.

Plan `{profile,status,reasons,hp,tempHp,conditionId,targetVersion,hash}`. Profile
`5R-SRD-5.2.1-KNOCKOUT-REST-V1`, status CANDIDATE or MANUAL_REQUIRED. Candidate
has unchanged authoritative HP/tempHP and original conditionId; manual has null
hp/tempHp/conditionId. Hash covers origin ID/version/hash, current card, review and
derived plan. Preview is not persisted and never mutates any card/history/receipt.

Only APPLIED origins with one A11/A12 confirmation, an APPLY and no correction or
prior supported removal qualify. Target must still be at confirmed expectedVersion+1,
with the exact sole canonical annotation, HP1/tempHP0 and no death saves. Require
minutes>=60, interrupted=false, reviewed=true. Full HP does not prevent this pathway.
Other evidence/current versions return MANUAL_REQUIRED; submitted stale versions,
hash or stage conflict409. DM independently reviews permitted rest activity and all
interruptions; there is no wall-clock/game-clock inference, timer, restarted-rest or
Long Rest conversion. Later source-card activity does not prevent target completion.

Application locks actor then campaign, rechecks current DM and both objects before
receipt replay. Exact retries return original `{id:previewId,version}` before current
version/support checks. New writes rederive and compare planHash. Remove exactly the
canonical annotation; preserve every other sheet field including all HP/resources;
increment card once, write full before/after KNOCKOUT_REST history, append
END_KNOCKOUT_REST with unchanged numeric target values and reviewed reason, increment
preview once (status stays APPLIED), receipt and visibility-safe invalidation all in
one transaction. Preserve original dice/action/evidence/hash. No migration.
The private event summary is server-generated and retains the declared continuous
minutes and uninterrupted, reviewed-rest conclusion for later audit; it is never a
PUBLISH event or public result.

Same key/body has one effect; changed body or route conflicts409. Different keys/DMs
and A13/A14 races share target/preview versions so at most one applies. Immediate or
deferred failure rolls back card/history/event/preview/receipt together. Replaying an
old receipt never removes a later restored annotation. Generic `/events` cannot inject
END_KNOCKOUT_REST; editor/ticks/normal healing retain their existing semantics.
Players cannot read private plans, reasons, hashes or origin details. Private no-store
headers apply on success/errors. UI requires review+preview then explicit completion;
unknown outcomes retry exact captured body, known conflicts require reread. Review,
selection, campaign, API, role and authoritative revision changes invalidate plans;
late authorization loss clears private evidence. Remaining Prone is explicitly manual.
