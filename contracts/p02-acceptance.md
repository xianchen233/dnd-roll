# P02e: integrated scene, map and permission acceptance

This is an acceptance composition of [scenes](scenes.md), [private assets](assets.md),
[tokens](tokens.md) and [safe maps](map-visibility.md), with no new HTTP fields,
database schema or D&D rules. Earlier regression tests remain required.

## Required scenarios

1. Independent DM/player browser sessions switch between two prepared scenes.
   Each scene retains its own fog, calibration, token positions, controls and
   versions. Creating/preparing an inactive scene does not activate it. A player
   cannot enumerate inactive preparation or use an inactive/stale image URL.
2. Reveal and cover map cells, then recalibrate the map. Recalibration clears fog
   atomically, including previously revealed cells. Public tokens remain public
   on covered cells; hidden tokens remain absent on revealed cells. Historical
   map/fog/token receipts confirm past commands without restoring past state.
3. Take the player's actual browser context offline. Change current scene/fog or
   permissions from the DM session while disconnected. On reconnect, authoritative
   reads recover current state and clear obsolete selection, images and retries.
   Control revocation denies old movement receipts; membership revocation denies
   snapshots and both original and derived images, including direct known URLs.
4. Inject an uncertain command result by losing its response after the server has
   committed. The client must preserve the exact requestId/body for explicit retry;
   retry obtains the original receipt, advances no version again, and only a fresh
   successful read can confirm current synchronization. Fault injection must be
   identified separately from actual browser offline/reconnect behavior.
5. Seed two scenes with maps, distinct fog, moved/shared/hidden tokens and durable
   receipts. Actually stop/restart both PostgreSQL and the packaged application
   (or standard Compose services), preserving database and private asset root.
   Fresh sessions verify current scene, both scenes' complete state, original
   bytes and safe PNG hashes/pixels, versions and historical receipt semantics.
   All earlier asset/token/map restart scripts also remain required.

## Completion gate

Record commands, actual failures and passing results in docs/verification.md;
keep resume instructions and CHANGELOG current. New behavior discovered necessary
during acceptance follows executed assertion RED → minimal fix → GREEN. An added
acceptance test that passes existing behavior is coverage, not a fabricated RED.

Run database-free units, all frontend tests and production build, contract checks,
all real PostgreSQL/HTTP/WebSocket/migration tests, all real browser scenarios and
the real restart checks. Run standard Compose in current-commit CI if unavailable
locally. Check both push and PR runs for the exact delivered SHA. Only after all
required gates pass may P02e and P02 be marked complete. Keep PR #1 a draft and do
not merge or release. Capacity, touch input, TLS, consistent backup and restore
on another machine remain the separately tracked P09/P10/release work.
