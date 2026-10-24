# A03c placement migration and bounds

2026-10-24 (Asia/Shanghai). Same branch and draft PR #1, no merge/release.

Entry: fetched origin, clean local/upstream/PR HEAD
`d14871f891812e948da0ae60164b0086ec777968`. Independently downloaded six complete
logs for push39985141565 and PR39985151305, both completed/success: each 126 unit,
153 real IT, 248 frontend/build, 111 contracts, 52 production browsers and all
restart/isolated restore/capacity assertions. External index:
`.tools/a03c-next-entry-ci.log`; no reuse as evidence for the new HEAD.

Actual RED: two focused HTTP integration tests failed with no setup errors
(`a03c-placement-red.log`). Changing only placement hiding reset a moved live
token from (7,8) to definition (2,3). A simultaneous scene shrink and explicit
move into bounds was incorrectly blocked by the pre-migration position.

Implemented one placement merge used by preview bounds and application. Unchanged
axes retain current live values, changed axes adopt the reviewed definition, and
moving across scenes uses both destination coordinates. Existing hidden tokens
stay hidden until the existing explicit live authoring API reveals them. Removed
placements do not block shrinking their old scene. Unchanged out-of-bounds live
placements remain blockers. Recovery retains actual positions before migration.

First GREEN: 126 unit + 27 lifecycle IT, zero failures/errors/skips
(`a03c-placement-green.log`). Covers selective-axis updates, player invisibility,
unsafe shrink rejection, safe shrink-and-move, subsequent shrink-and-retire and
pre-migration recovery coordinates. Expanded the same test with historical exact
retry after a second migration (original acknowledgement, no position rewind) and
player rejection before final full regression; those additions are direct boundary
checks, not claimed as separately observed REDs.

The lifecycle restart/isolated restore script now moves a live token to (7,8),
changes only definition x to 9 and public hiding, checks result (9,8)/still hidden,
and recovery (7,8), alongside all existing damaged-card/history/receipt assertions.
No schema/route/client shape changes. Contract checker: 111 operations passed.

Final local full regression: **126 unit + 155 real IT**, zero failures/errors/skips
(`a03c-placement-full.log`). Restart seed initially asserted a nonexistent `hidden`
property on the public scene-token shape; corrected the script to inspect DM-only
runtime evidence and actual player invisibility instead, with no API change. The
fresh seed passed. This script-shape failure is not a product RED. The partial
fixture files were retained and a new fixture path used for the fresh seed.

Final full execution, actual restart and stopped-process results are indexed in
continuation.md and verification.md. Final exact-HEAD checker:
`.tools/a03c-placement-check-ci.mjs`, requiring 126 unit + 155 IT and all previous
frontend/browser/restart/restore/capacity assertions. Six freshly downloaded logs
and `a03c-placement-ci-{HEAD}.json` are outside the repository. Pending/failed runs
must not be called success.

A03a/b, text updates and previous structural/recovery behavior remain. A03c still
blocks unmanaged scenes/tokens/maps, missing links, unsafe bounds, materials,
policy and manifest changes. A03d/e and A04–A06/M3 remain pending. Cloud/TLS,
physical cross-host recovery and real phones remain deferred.
