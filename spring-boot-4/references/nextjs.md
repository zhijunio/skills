# Next.js

Use Next.js when the frontend needs its routing, server rendering, or Node.js
runtime. Decide whether the production frontend requires a Node.js server
before designing the deployment.

The current stable Next.js line is 16.4 and requires Node.js 20.9 or later.
Check the project's selected release and support policy before setting runtime
versions.

| Mode | Deployment | Choose it when |
| --- | --- | --- |
| Static export | Static host or selected Spring Boot static hosting | Every route can be generated at build time and browser code calls Spring Boot |
| Node.js server | Separate frontend service behind a proxy or ingress | The app needs request-time rendering, server actions, or server-side features |

Static export cannot use features that require a Next.js server. Spring Boot
remains the business API in either mode.

## Development proxy

Create or retain a Next.js App Router project using the current supported
release. In Node.js mode, route browser `/api/**` requests to Spring Boot
through a Next.js rewrite during development and production:

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

Use relative URLs in browser code. Keep server-side API URLs in server-only
environment variables. Never put secrets in `NEXT_PUBLIC_*` variables.
Rewrites require the Next.js server. They are incompatible with static export;
for a static-export project, use a separate local reverse proxy during
development and route `/api/**` at the production host or ingress.

## Static export

Static export fits routes that can be generated at build time. Enable it in
`next.config.ts`:

```ts
const nextConfig = {
  output: 'export',
}

export default nextConfig
```

`next build` writes files to `out`. Spring Boot's static resource handling
does not automatically map every exported route to its generated HTML file.
Prefer a static host or reverse proxy that maps clean URLs to those files. If
Spring Boot must serve the export, copy `out` into the build's static resources
and implement route mapping for the chosen `trailingSlash` setting. Test
direct navigation and refresh for nested routes against the packaged app.

For Maven, run `npm ci` and `npm run build` before copying `frontend/out` into
`${project.build.outputDirectory}/static`. Bind these tasks before resource
processing and run `mvn clean` in CI so deleted export files cannot remain in
the packaged output. For Gradle, make frontend build and copy tasks explicit
dependencies of `processResources`.

## Node.js server deployment

Run Next.js as a separate service when server rendering or other runtime
features are required. Route page requests to Next.js and `/api/**` to Spring
Boot through a trusted reverse proxy or ingress. Browser code can use the
relative `/api/**` path. Server-side rendering must call Spring Boot through
an explicit internal service URL because server-side `fetch` has no browser
origin. Configure forwarded headers only for trusted proxies.

## Verify

Run frontend lint, type checks, tests, and a production build. For static
export, inspect `out`, package the application, and request the root, nested
routes, and assets. For Node.js mode, verify both services through the actual
routing setup, including API errors, server-rendered requests, and cookies.

References: [Next.js documentation](https://nextjs.org/docs), [static exports](https://nextjs.org/docs/app/guides/static-exports),
[rewrites](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites).
