# Next.js with Spring Boot

Use Next.js as a separate frontend application that consumes Spring Boot HTTP APIs. Decide at the start whether the frontend needs a Node.js server at runtime:

| Next.js mode | Spring Boot integration | Choose it when |
|---|---|---|
| Static export | Serve generated files from a static host or Spring Boot | All routes can be rendered at build time and browser code calls the Spring API |
| Node.js server | Deploy Next.js as a separate service; route `/api/**` to Spring Boot | You need request-time rendering, server actions, cookies or other server features |

Static export does not support features that require a Next.js server. Do not configure `output: 'export'` for an application that needs those features. Keep Spring Boot the owner of the business API in either mode.

## Scaffold and local development

From the Spring Boot project root:

```bash
npx create-next-app@latest frontend
cd frontend
npm run dev
```

Choose TypeScript and the App Router if they fit the project; retain existing choices when adding Next.js to an established frontend. Set the dev port in the `dev` script if needed, for example `next dev --port 3000`.

For the Node.js server mode, proxy browser API calls through Next.js in development. This keeps browser requests same-origin and avoids broad CORS configuration. In `frontend/next.config.ts`:

```ts
import type { NextConfig } from 'next'

const springBootUrl = process.env.SPRING_BOOT_URL ?? 'http://localhost:8080'

const nextConfig: NextConfig = {
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${springBootUrl}/api/:path*` }]
  },
}

export default nextConfig
```

Use relative browser URLs such as `fetch('/api/products')`. Keep the proxy target in an environment variable for local development; do not expose secrets through `NEXT_PUBLIC_*` variables. Next.js rewrites require the Next.js server and are unavailable in a static export. For static-export development, use a local reverse proxy in front of Next.js and Spring Boot, or call Spring Boot directly and configure narrowly scoped development CORS.

## Static export into Spring Boot

Static export fits a frontend whose routes can be generated at build time (including dynamic routes with build-time parameters) and whose data can be loaded in the browser. Add `output: 'export'` to `next.config.ts`:

```ts
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  output: 'export',
  // Needed only if the app uses next/image without an external image service.
  images: { unoptimized: true },
}

export default nextConfig
```

`next build` writes the export to `frontend/out`. The export contains route files such as `about.html` or `about/index.html`, depending on `trailingSlash`. A static web server can map clean URLs to these files. Spring Boot serves classpath resources by path and its automatic welcome-page handling covers the root `index.html`; it does not provide Next.js-style clean-URL mapping for every exported route. For the simplest production routing, serve `out` through a static host or reverse proxy configured to map routes to exported files, and route `/api/**` to Spring Boot.

If the application must serve the export from the Spring Boot process, copy its contents into `${project.build.outputDirectory}/static` and implement and verify the clean-URL mapping for the chosen `trailingSlash` setting. For Maven, a `maven-resources-plugin` execution can copy `frontend/out` during `process-resources`; bind the frontend install/build executions in `generate-resources` so the export exists first. Use `npm ci` when a lockfile is committed, then `npm run build`. Run `mvn clean` before packaging in CI so deleted files from previous exports cannot remain in `target/classes` and enter the artifact.

For example, add a copy execution to `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-next-static-export</id>
            <phase>process-resources</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                <resources>
                    <resource>
                        <directory>${project.basedir}/frontend/out</directory>
                        <filtering>false</filtering>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Test direct navigation and refresh on nested routes against the packaged artifact. Static export emits route files rather than a single SPA entry point, so do not add a catch-all forward to `/` without checking it against those files. Keep API and asset paths out of any application-page fallback.

The existing `frontend-maven-plugin` pattern in [React integration](react.md) can install Node/npm and run scripts, but use Next.js' `build` script and copy `out`; do not use the Vite `outDir` setup. For Gradle, wire `npm ci`, `npm run build`, and copying `out` into `processResources` with task dependencies so resource processing cannot run first.

## Node.js server deployment

When runtime rendering or other server features are required, deploy Next.js with `next start` (or the project's chosen Node server/container) as a separate process. Put a reverse proxy, ingress, or gateway in front of both services so `/api/**` reaches Spring Boot and page requests reach Next.js. Browser code can use the relative `/api/**` URL through the Next.js rewrite. Server Components and other server-side code should call Spring Boot using an absolute server-side service URL; relative browser URLs have no origin in server-side `fetch`. Keep internal service URLs server-side and configure forwarded headers and trusted proxy behavior deliberately. Spring Boot's static resource server cannot host a full Next.js runtime.

## Checks

- Run the frontend's lint, type-check, test, and production build scripts from `frontend/package.json`.
- For static export, inspect `frontend/out`, package the Spring Boot app, and request `/` plus representative nested routes and assets from the packaged app.
- For Node.js mode, verify both services through the production routing setup; confirm API requests, SSR data fetching, errors, and authentication cookies use the intended origin and headers.
- Keep frontend checks in CI even when Maven or Gradle owns the backend build. Do not let a backend package succeed with a stale or missing frontend export.

## References

- [Next.js static exports](https://nextjs.org/docs/app/guides/static-exports)
- [Next.js rewrites](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites)
- [Spring Boot static content and welcome pages](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Spring Boot static resources](../web/static-resources.md)
- [Spring Boot React/Vite integration](react.md) for the frontend Maven plugin pattern
