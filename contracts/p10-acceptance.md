# P10: complete first-edition acceptance

Status: in progress, 2026-10-22. P01–P08 and P09 functionality/local/container
recovery remain accepted. Continue the original branch and draft PR #1; no merge
or release. No new D&D rules, HTTP schema or database migration is introduced.

## Ordered work

1. Independently verify the exact entry HEAD against origin and both push/PR runs,
   including all six full job logs. Map product-design section 16 to executable
   evidence in docs/p10-acceptance.md. Earlier passing tests are regression
   coverage, never a fabricated RED.
2. Run a continuous DM/two-player campaign across invitations, two independent
   characters, delegated/revoked control, map/fog/hidden enemies, content admission,
   RP/images/replies/edits, button/command dice, initiative, turns, explicit manual
   application/correction, private data and team notes. Include concurrent/retried
   commands and a real offline/reconnected browser. Preserve all earlier tests.
3. Exercise a 390px and 360px touch-enabled browser: single-column navigation to
   character/resources, dice and RP/logs without document horizontal scrolling.
   A tab switch retains mounted drafts and exact uncertain commands; campaign
   switches/access denial dispose them. Show the chosen tab, keyboard focus and
   touch targets. Desktop workspaces retain their existing independent toggles.
   Full phone map editing/DM hosting and physical-device certification are outside
   this check. Record emulation separately from physical touch evidence.
4. Measure a bounded local 1 DM + 8 player workload, map bytes/load latency,
   history growth, Java/PostgreSQL memory, and real backup time/size/peak memory.
   Record machine, sample counts and p50/p95/max/errors; no cloud sizing guarantee
   follows from localhost results. Scripts must fail on unexpected HTTP results,
   use fresh disposable campaigns and keep credentials/private evidence external.
5. Run database-free units, real PG/HTTP/WS/migrations, frontend/build/contracts,
   production browser suite on the P09 restored target, actual restart/pending
   state checks, and exact delivered code SHA push/PR CI with standard Compose.

## Evidence and remaining gates

Tests discovering required changes must actually fail before the fix; distinguish
fixture/environment errors, direct-GREEN coverage and regressions. Record commands,
counts, limitations and next steps in verification/continuation and CHANGELOG.

Product scenarios 1–9 are local functional acceptance; scenario 10 additionally
requires physical cross-host recovery. Scenarios 11–12 belong to A/M3. Per the
user's sequencing, cloud environment preparation, deployment, public HTTPS/WSS,
and physical cross-host restoration are deferred to one joint drill after overall
functionality is finished. These remain unpassed. P10 local functional evidence
must be reported separately from full P10/M2/release completion.
