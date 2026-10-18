# Frontend

React + TypeScript + native PixiJS tabletop. Same-origin `/api` HTTP requests carry session cookies and CSRF; STOMP uses `/ws` and the personal scene-change queue. Realtime events only trigger authoritative HTTP refreshes.

```powershell
npm ci
npm test
npm run build
npm run dev
```

Open Vite's local URL. Development proxies `/api` and `/ws` to the Spring Boot service at `localhost:8080`. Without the backend the real login page is still available; account login requires a running backend. Obtain provisioned account credentials from the host. No account password is embedded in the client.

Select a campaign and a controllable unit, then click a destination grid cell or submit integer X/Y coordinates in the right panel. Coordinates begin at `(0, 0)`. Uncontrolled units are disabled. The map keeps confirmed coordinates while a command is pending. A network failure offers retry with the original request ID; conflict refreshes the snapshot and asks for a new choice.

Connection state refers to realtime transport. HTTP refresh also runs every 15 seconds and after reconnect; this is not an offline editing client. See [TDD evidence](TDD.md) for executed tests and remaining real-browser validation.

## Real-service browser acceptance

Start the backend with opt-in `demo` data and start Vite before running E2E. The test expects the seeded `Adventurer` and `Hidden enemy` units, and changes the adventurer's X coordinate by one cell. Use an isolated test database.

Provide `E2E_DM_USERNAME`, `E2E_DM_PASSWORD`, `E2E_PLAYER_USERNAME`, and `E2E_PLAYER_PASSWORD` in the execution environment; every variable is required and passwords have no fallback. `E2E_BASE_URL` defaults to `http://127.0.0.1:5173`.

```powershell
# Use a local Chrome installation, or omit this variable for Playwright Chromium.
$env:E2E_CHROME_PATH = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
npm run test:e2e
```

For CI's bundled browser run `npx playwright install --with-deps chromium` first. `npm run test:e2e:headed` is available for manual observation. The test uses two isolated cookie contexts and real HTTP/STOMP connections, checks player filtering, observes a real DM STOMP invalidation, moves through the UI, reloads, verifies canvas rendering has initialized, and logs out both accounts. It rejects browser JavaScript/console errors except the expected unauthenticated `/api/auth/me` probe. Screenshots and failure traces are written under ignored `test-results/`; the HTML report is under ignored `playwright-report/`.
