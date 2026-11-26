# React

Use React for a browser-rendered SPA that consumes Spring Boot HTTP APIs. For
shared API, Maven, and routing rules, read [Frontend integration](frontend-integration.md).

## Start a frontend

The current stable React line is 19.3. Use the current React template
supported by Vite. Choose TypeScript for a new
frontend unless the project already uses JavaScript or has a different type
strategy. Commit the lockfile and use `npm ci` in CI.

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm run dev
```

Check Vite's current Node.js requirement before selecting the project's Node
version. Vite 8 requires Node.js 20.19+ or 22.12+.

## Connect Spring Boot

Configure the Vite dev server to proxy `/api` to the Spring Boot development
server. Use relative browser URLs such as `fetch('/api/orders')`. Configure
Vite's `build.outDir` to write the production files into
`../src/main/resources/static` only when the React app ships inside the Boot
jar. Keep the output outside source-controlled inputs and set `emptyOutDir` so
removed assets do not survive rebuilds.

If the frontend has client-side routes, add the scoped SPA fallback described
in [Frontend integration](frontend-integration.md). Do not route API or
Actuator 404s to the React entry point.

## Application structure

Keep API access in typed service modules. Keep page state close to the page
that owns it. Add shared state libraries only for state that crosses real
feature boundaries. Use React Router only when the application needs
client-side routing. Keep loading, empty, error, and success states explicit.

Keep API wire types separate from view-specific state when their semantics
differ. Handle RFC 9457 errors and distinguish mutation failures from failures
to refresh data after a successful mutation.

## Verify

Run lint, type-check, tests, and the Vite production build. Test the packaged
Boot application at `/`, one nested route, an asset URL, and representative
API success and error responses.

References: [React documentation](https://react.dev/), [Vite guide](https://vite.dev/guide/),
[React Router](https://reactrouter.com/).
