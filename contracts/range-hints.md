# A07: visible-token range hints

Support matrix and sources: [5R / SRD 5.2.1](../docs/research/range-hints-5r.md).
`POST /api/campaigns/{campaignId}/range-hints/preview` is a read-only authenticated,
CSRF-protected measurement. No dice, history, receipt, version increment, combat,
resource, movement, policy or broadcast is written. Repetition recomputes current
authorization and versions; no saved result can authorize an attack.

Exact body: `{sceneId,sceneVersion,sourceId,sourceVersion,targetId,targetVersion,
profile,reviewed,mode,normalFeet,longFeet}`. IDs are UUIDs, versions nonnegative
safe integers. Profile is `GRID_5R_5FT_RANGE_V1`, mode MELEE or RANGED.
Both thresholds are integers 0..100000; normalFeet <= longFeet; MELEE requires
equal thresholds (the declared final reach). RANGED equal thresholds represent
a single range. Unknown/extra/missing/null/wrong-type fields reject with 400.
There are no client coordinates, hidden-target guesses or free-text rules.

Current campaign membership is checked first. Both endpoints must be currently
visible in the current shared scene. Missing, hidden or foreign endpoints all
produce the same 404 without measurements or secret metadata, even for stale
versions. No control or turn grant is required for this read-only operation.
DM may measure their visible private endpoints; the response remains private.
Scene identity/version and both token versions are checked under the campaign
lock; stale context returns 409. Authenticated responses and every error,
including anonymous and malformed requests, carry `Cache-Control: no-store, private`
and `X-Content-Type-Options: nosniff`.

Exact result: `{status,distanceFeet,band,reason}`. CANDIDATE has integer distance,
band WITHIN_REACH / NORMAL_RANGE / LONG_RANGE / OUT_OF_RANGE and empty reason.
Distance is five times the maximum coordinate difference, including diagonals.
Unreviewed context or non-5-foot calibrated map returns MANUAL_REQUIRED with
null distance/band and reason REVIEW_REQUIRED / GRID_REQUIRES_MANUAL. No-map
scenes use the existing default 5-foot grid. reviewed=true explicitly acknowledges
same plane, one-cell representations and separately reviewed rules/exceptions.
It never certifies visibility in game, cover, corners, occupancy, creature size,
height, opportunity attacks, final advantage or valid action/ability selection.

Chinese UI selects only endpoints from the current filtered scene and explicitly
enters reach/ranges. It shows provenance/bounds and offers a separate measurement
button. Edit, scene/token/version/role/API/campaign changes immediately hide old
results and isolate late responses. 401/403 clears evidence and rechecks access;
404/409 clears the hint and asks for a fresh scene. Errors never leave a previous
hint displayed as current. It has no execute/apply button or implicit mutation.
