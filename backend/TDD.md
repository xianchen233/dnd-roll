# TDD evidence

2026-10-18, Java 25.0.4.1 and Maven 3.9.16 on Windows.

RED: `mvn -Dmaven.repo.local=D:/workspace/dnd/.tools/m2 -f backend/pom.xml test` executed before the movement/application/STOMP policy implementations. Result: **12 tests, 5 failures, 7 errors, 0 skipped; BUILD FAILURE** at 18:34 +08:00. Expected causes: deliberate `UnsupportedOperationException("TDD red")` movement/application stubs and an allow-all STOMP policy. Dependencies and tests compiled successfully; this was a behavioral failure.

GREEN: same command with `-B -ntp`, at 18:36 +08:00: **12 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. All 13 production sources compiled. Unit tests use no Spring context, database, Docker or network.

Real PostgreSQL integration results will be recorded after execution.

Strict JSON follow-up RED at 18:39 +08:00: **13 tests, 1 failure, 0 errors**. The new fractional-coordinate test reproduced Jackson silently accepting `x:1.5` as an integer. Disabled `ACCEPT_FLOAT_AS_INT` for both coordinates and versions; HTTP negative cases also cover fractions and absent fields.

Strict JSON GREEN at 18:39 +08:00: **13 tests passed**. Session revocation RED at 18:40 +08:00: **15 tests, 1 failure**, proving that invalidating a session did not yet close its sockets. Added an HTTP session lifecycle listener and transport registry; the real STOMP suite also tests logout closing an established socket.

Session revocation GREEN at 18:41 +08:00: **15 tests passed**. The coordinator then ran the scalar coercion follow-up RED at 18:59:04 +08:00: **16 tests, 1 failure, 0 errors, 0 skipped** (`stringCoordinatesAndVersionsAreRejected`, string numbers were accepted). Disabled `MapperFeature.ALLOW_COERCION_OF_SCALARS`; final GREEN is pending coordinator execution.

The coordinator ran real PostgreSQL integration RED at 19:03:09 +08:00: **9 tests, 2 failures, 0 errors, 0 skipped**. The movement test reproduced a committed move leaving scene version at 0; the restart test reproduced demo initialization restoring revoked control. All four actual WebSocket tests passed. An earlier run failed on access to the host's default Java temporary directory; that environment failure is not counted as behavioral RED. The coordinator set `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:/workspace/dnd/.tools/tmp` for this Windows environment and reran successfully to obtain the behavioral failures.

Minimal fixes: increment the scene snapshot version in the same transaction as the move and receipt; only seed scene data/memberships/controllers when the campaign insertion creates a new campaign.

Final coordinator GREEN, 19:06 +08:00: the official Maven 3.9.16 only-script Wrapper (distribution SHA-256 checked) executed `scripts/check.ps1 -Integration`: **16 unit tests and 9 real PostgreSQL 18.6 integration tests passed, 0 failures/errors/skipped; BUILD SUCCESS**, producing the executable Spring Boot jar. HTTP checks include strict JSON, membership/control, hidden filtering, conflicting commands, identical concurrent retries and transaction rollback; four real WebSocket tests cover connection/CSRF, private invalidations, forbidden sends/subscriptions, logout and anonymous rejection. This resolves the scalar coercion, scene version and restart authorization regressions above.

At 19:12 +08:00 the coordinator also ran an actual packaged-server process restart: commit a move, stop/restart Java against the same PostgreSQL database, authenticate again, retry the original stale-version request. The original receipt, coordinates and token/scene versions survived; replay did not apply the move twice. This was a local smoke check, separate from the committed Maven test suite. See `../docs/verification.md` for reproducible steps and remaining Docker verification.
