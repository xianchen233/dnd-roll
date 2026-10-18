# Frontend

React + TypeScript + native PixiJS tabletop. Same-origin `/api` HTTP requests carry session cookies and CSRF; STOMP uses `/ws` and the personal scene-change queue. Realtime events only trigger authoritative HTTP refreshes.

```powershell
npm ci
npm test
npm run build
npm run dev
```

Open Vite's local URL. Development proxies `/api` and `/ws` to the Spring Boot service at `localhost:8080`. Without the backend the real login page is still available; account login requires a running backend. Obtain a provisioned fixed account or an invitation from the host. No account password is embedded in the client.

Every authenticated account can create a campaign, even with an empty campaign list. A new campaign opens an empty map; map and unit authoring are P02. DM campaigns show a members/invitations panel. Creating, revoking, and removing use a UUID request ID plus the loaded campaign version; a conflict refreshes both management lists before a new attempt. Uncertain network failures offer retry with the original request. PLAYER campaigns do not show or fetch DM invitation management.

An invitation URL uses `/#invite=TOKEN`, never a query or backend path. The DM receives a copyable link only on its first response; replay with `token:null` explains how to revoke the invitation and create a replacement. Links stay in component memory only. Invite visitors can preview anonymously, register a new fixed account, or log in with an existing one and accept. Registration explicitly logs in afterward. Success clears the fragment and opens the joined campaign. If registration succeeded but its response or the subsequent login was lost, the fixed-account login/accept recovery remains available even when preview reports that the invite was already used; the backend rechecks membership. Passwords keep spaces, require 12 Unicode characters, and allow at most 72 UTF-8 bytes.

Select a campaign and a controllable unit, then click a destination grid cell or submit integer X/Y coordinates in the right panel. Coordinates begin at `(0, 0)`. Uncontrolled units are disabled. The map keeps confirmed coordinates while a command is pending. A network failure offers retry with the original request ID; conflict refreshes the snapshot and asks for a new choice. After a successful move POST, either its own refresh or a newer realtime/periodic refresh can confirm synchronization; reads started before the acknowledgment cannot confirm the move. If synchronization fails, a later successful refresh recovers the saved confirmation.

Connection state refers to realtime transport. HTTP refresh also runs every 15 seconds and after reconnect; this is not an offline editing client. A 401/403 clears the map and any pending command, including older concurrent responses; the campaign-list refresh action lets a removed member switch to remaining campaigns. See [TDD evidence](TDD.md) for executed tests and remaining real-browser validation.

## Real-service browser acceptance

Start the backend with opt-in `demo` data and start Vite before running E2E. The tabletop test expects the seeded `Adventurer` and `Hidden enemy` units, and changes the adventurer's X coordinate by one cell. The P01 test creates a new empty campaign and a unique invited account, removes that member, checks real socket closure and the cleared guest map, then logs the persistent account in again. Use an isolated test database; these tests deliberately write real records.

Provide `E2E_DM_USERNAME`, `E2E_DM_PASSWORD`, `E2E_PLAYER_USERNAME`, and `E2E_PLAYER_PASSWORD` in the execution environment; every variable is required and passwords have no fallback. `E2E_BASE_URL` defaults to `http://127.0.0.1:5173`.

```powershell
# Use a local Chrome installation, or omit this variable for Playwright Chromium.
$env:E2E_CHROME_PATH = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
npm run test:e2e
```

For CI's bundled browser run `npx playwright install --with-deps chromium` first. `npm run test:e2e:headed` is available for manual observation. The test uses two isolated cookie contexts and real HTTP/STOMP connections, checks player filtering, observes a real DM STOMP invalidation, moves through the UI, reloads, verifies canvas rendering has initialized, and logs out both accounts. It rejects browser JavaScript/console errors except the expected unauthenticated `/api/auth/me` probe. Screenshots and failure traces are written under ignored `test-results/`; the HTML report is under ignored `playwright-report/`.
