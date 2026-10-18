# Backend

Java 25, Spring Boot 4.1.1, Maven 3.9+, PostgreSQL 18.6. `domain` contains pure movement/visibility rules; `application` coordinates permission checks and idempotent commands through ports; `persistence` implements PostgreSQL transactions; `web` adapts HTTP/session/CSRF/STOMP. Wire fields and errors follow `../contracts/README.md`.

Run unit tests without a database: `mvn test`. Package: `mvn package`. Start: `mvn spring-boot:run` or `java -jar target/dnd-roll-backend-0.1.0-SNAPSHOT.jar`.

Configure `DATABASE_URL` (JDBC URL), `DATABASE_USERNAME`, `DATABASE_PASSWORD`. Flyway migrates an empty database at startup. HTTP listens on port 8080. Configure `SESSION_COOKIE_SECURE=true` behind production HTTPS. Use a same-origin reverse proxy; the development frontend proxies `/api` and `/ws`.

Demo setup is explicitly opt-in: set `SPRING_PROFILES_ACTIVE=demo`, `DEMO_DM_USERNAME`, `DEMO_DM_PASSWORD`, `DEMO_PLAYER_USERNAME`, `DEMO_PLAYER_PASSWORD`. Usernames default to `dm`/`player`; both passwords are required. Scene data and permission grants are seeded only when the demo campaign is first created. Restarts preserve positions, revoked membership and revoked control. Changing an environment password does not reset an existing account password. Do not enable demo initialization for an unrelated live campaign database.

Run real PostgreSQL integration tests against an **isolated test database** using `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD` and `mvn -Pintegration verify`. Missing configuration fails the suite; tests are never silently skipped. Tests create demo data and move tokens. Unit test `test` does not execute `*IT` classes.

Every move locks its campaign row, then checks membership, token visibility/control and the original request receipt before token version. Token movement, token/scene version changes and the receipt commit in one transaction. Retrying an identical actor/campaign/request key returns its original result without advancing either version; a different token or payload conflicts. Current visibility/control is checked again before replay. Scene versions order authoritative snapshots; token versions detect conflicting edits to that token.

STOMP accepts authenticated session connections with their current CSRF token and subscriptions only to `/user/queue/scene-changes`; client SEND is refused. Events contain only campaign invalidations, addressed individually to members and published after commit. Clients recover missed notifications by requesting snapshots. This milestone implements exploration movement; combat, invitations, map assets, authoring and backup remain later work.
