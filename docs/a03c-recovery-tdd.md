# A03c consecutive migration recovery

2026-10-24 (Asia/Shanghai). Same branch and draft PR #1; no merge/release.

Entry: fetch confirmed local/upstream `e6d9651fa153a84487bc1aa51ebf79e1836fef33`.
Independently re-downloaded six full logs: push39836569868 and PR39836575609
both completed/success. Each has 126 unit, 151 real IT, 248 frontend/build,
111 contracts, 52 browser flows and the complete restart/isolated restore/capacity
assertions. External evidence: `.tools/a03c-resume-entry-ci.log` and freshly
downloaded `a03c-structure-job-*.log`. GitHub connector failed transport; the
existing authenticated REST checker supplied the evidence. A03a/b and text gate
are retained.

Actual TDD:

- Extended the combat replacement test to migrate twice. Actual RED: second
  recovery had one card instead of two, omitting the retired combat identity
  (`a03c-recovery-red.log`). Original combat/initiative remained in the source.
- Extra-card test initially failed in fixture setup on an incorrect campaign-list
  shape; corrected it. The independent rerun then actually failed on the existing
  `/unmapped-unit` blocker (`a03c-recovery-extra-red.log`). Setup errors are not RED.
- Recovery now copies all live cards, without adopting extra cards into the source
  mapping. Recovery controls belong only to the migrating DM; collision-safe node
  IDs are deterministic and every extra card has an explicit confirmation.
- First GREEN: 126 unit + 24 lifecycle IT, no failures/errors/skips
  (`a03c-recovery-green.log`). Added direct boundary coverage for 101-card rejection
  before writes and a live card UUID colliding with a template scene UUID.
- Full recovery definitions are validated during preview. Capacity/invalid exchange
  definitions produce `/recovery-definition`, without partially creating a copy.
  Ad-hoc scenes/tokens/maps, unsafe bounds, published materials, policy and manifest
  changes still block. Original turns, HP/resources, control grants and history stay.
- Extended the existing restart/isolated restore script to include an extra damaged
  live card, compare both cards and verify the independent recovery after restart.
  The first local seed was attempted before the backend listened (ECONNREFUSED);
  after readiness returned 200, seed passed. An incorrect JAR filename was corrected
  before startup. Neither preparation failure is counted as behavior RED.
- First full run failed after the concurrently started acceptance backend consumed
  remaining PostgreSQL connections (`FATAL: sorry, too many clients already`).
  Stopped that verified backend and reran the full suite with no acceptance service;
  retained the failed `a03c-recovery-full.log`. No assertion/timeout was relaxed.

Final execution results and exact-HEAD CI index are recorded in continuation.md
and verification.md. External logs use `.tools/a03c-recovery-*`; credentials,
runtimes, assets and databases stay outside Git. A03c remains bounded; A03d/e,
A04–A06/M3, cloud/TLS/physical cross-host recovery and real phones remain pending.
