# Development agreement

Read `docs/product-design.md`, `docs/implementation-plan.md`, `CONTEXT.md`, and `contracts/README.md` before changing behavior.

At the start of a resumed session, read `docs/continuation.md` first, then `docs/verification.md`, and check Git status. At the end of each milestone or before context handoff, update those files with actual results, outstanding checks and the next executable step. Chat history and previous agents' availability are not prerequisites for continuing. See `docs/work-packages.md` for the full ordered backlog; do not silently drop unfinished work.

- Java/Spring Boot modular monolith, PostgreSQL, React/TypeScript and native PixiJS. Keep modules and transport boundaries explicit.
- Follow TDD for behavior: add a focused failing test, run it and record the failure, implement the smallest useful behavior, rerun and refactor. Never label unexecuted tests as passing.
- Unit tests must run without Docker, a database or a network. Database integration tests run separately against real PostgreSQL; never silently skip a required check.
- Every state-changing request enforces authenticated identity, campaign membership, unit control, object versions and idempotency on the server. Hidden data must not enter player payloads.
- Add tests for authorization failures and retry/concurrency boundaries alongside the happy path. Keep API contracts and clients consistent.
- Do not commit credentials, local runtime downloads, binary source libraries, commercial rules text, generated build output or private game data.
- Each task owns specific directories. Coordinate shared contracts before parallel edits. Only the coordinating agent performs Git commits.
- Keep unfinished functionality explicit in the implementation plan and UI; the current milestone does not imply the entire product is complete.
- Maintain `CHANGELOG.md` using Keep a Changelog: record notable behavior, deployment, migration and security changes under Unreleased in the same change as the code. Use Added/Changed/Deprecated/Removed/Fixed/Security as appropriate; omit empty sections. Create a dated version section only when that version is actually released, and never claim pending work or unexecuted checks as completed changes.
