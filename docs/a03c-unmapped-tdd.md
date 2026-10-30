# A03c consecutive scene retirement and ad-hoc recovery

2026-10-24 (Asia/Shanghai). Same `feat/accounts-and-invitations` branch and draft
PR #1; no merge or release. Cloud/TLS, physical cross-host recovery and real
phones remain deferred. A03a/b and previous text/structure/placement slices stay.

Entry fetch confirmed clean local/origin/PR HEAD
`8578e76d2a76d8e679b48ad2585d4029cebb404e`. Independently downloaded six complete
logs for push39988746436 and PR39988753462: both completed/success, each 126 unit,
155 real IT, 248 frontend/build, 111 contracts, 52 production browser flows and
all previous restart/isolated restore/capacity assertions. Evidence is outside
Git: `.tools/a03c-empty-entry-ci.log`. No reuse as results for a later HEAD.

Actual TDD used the existing external `a01-maven.ps1`, from `backend/`, with the
integration profile and real PostgreSQL (55432); tests use independent schemas.

- `-Log a03c-unmapped-red.log -IT
  'ModuleLifecycleIT#migrationContinuesAfterAllMappedScenesAreRemoved+migrationRecoversUnmappedScenesTokensAndMapBytesWithoutAdoptingThem'`:
  **2 tests, 2 assertion failures, 0 errors/skips**. After deleting every mapped
  scene, the next preview rejected the generated fallback as `/unmapped-scene`.
  The second test rejected an ad-hoc scene and its linked/characterless tokens.
- Implemented recovery of live scenes/cards/tokens and actual maps with separate
  identities, exact additional confirmations and no adoption into the original
  instance mapping. First full lifecycle GREEN: **126 unit + 29 lifecycle IT**,
  zero failures/errors/skips (`a03c-unmapped-green-initial.log`). Fixture image
  dimensions were corrected to match the existing 12×10 PNG before that run.
- `-Log a03c-unmapped-bounds-red.log -IT
  'ModuleLifecycleIT#migrationBlocksShrinkingAroundUnmappedTokens'`: **1 test,
  1 assertion failure, 0 errors/skips**. An independently authored token at x=19
  did not block shrinking its scene to width=10. Added actual retained ad-hoc
  positions to the bounds gate. Template updates cannot move these tokens.
- Expanded lifecycle GREEN: **126 unit + 30 lifecycle IT**, zero failures/errors/
  skips (`a03c-unmapped-green.log`). Includes missing file rejection, preview hash
  invalidation on an unversioned live edit, incomplete confirmation rejection,
  concurrent identical requests and immediate/deferred receipt failure rollback.
  Both failures preserve the original runtime and campaign count, and remove all
  newly published recovery files. Original bytes remain. These extra boundaries
  are direct checks; they are not claimed as separately observed REDs.

Recovery preserves the current scene, live scene geometry/map calibration, token
aliases/positions/hiding and shared character references. Bare tokens remain
characterless. All recovery token/card controllers belong only to the migrating
DM. Existing template tokens retired from play are not resurrected; active ad-hoc
tokens in retired scenes retain a recoverable scene. Images have independent
metadata/storage identities, validated actual hashes/dimensions and combined
archive bounds. Full recovery validation precedes writes. No schema or route
change; contract checker still reports 111 operations.

Further direct boundaries cover 51-scene recovery rejection, a runtime scene UUID
colliding with a template node, and repeated migration after scene retirement
with a surviving ad-hoc token. Final regression results are recorded in
`verification.md` and `continuation.md`; intermediate runs are not substituted for
the final code. External evidence uses `.tools/a03c-unmapped-*`.

Final code full `-Pintegration verify` (external helper `-Full`): **126 unit +
158 real IT**, zero failures/errors/skips (`a03c-unmapped-full-final.log`). Local
frontend **248 passed**, production build and **111 contracts** passed. After
actual Java/PG stop and restart, the new-session lifecycle verifier passed every
old and new assertion (`a03c-unmapped-restart-{seed,verify}-final.log`). No seed or
verification assertion failed. New process identities are retained externally.
The verified Java/PG were then stopped, 8080/8081/5173/55432 had no listeners, and
environment-cleared offline `-UnitOnly` passed **126 unit** with PG stopped.
The source fence, restored database/assets and synthetic acceptance data remain.
No new local production-browser or independent-volume restore run is claimed;
fresh exact-HEAD push/PR CI must verify those gates and the new assertions.

The existing lifecycle seed/verify now captures all active scenes/tokens/maps and
all asset hashes, adds a live map plus linked and bare tokens, then migrates three
times including complete mapped-scene retirement. Original receipts must not
replay or rewind the final state. Every recovery copy and its independent files
are compared after restart/isolated restore, with `UNMAPPED VERIFIED` assertions.
This extends the existing CI lifecycle path; old assertions remain.

A03c still blocks missing/changed template links, unsafe bounds, incomplete or
oversized recovery, published material/policy and manifest changes. A03d manual
operations, A03e Chinese controls/final acceptance and A04–A06/M3 remain pending.
Final exact-HEAD push/PR logs must be freshly downloaded; pending is not success.
