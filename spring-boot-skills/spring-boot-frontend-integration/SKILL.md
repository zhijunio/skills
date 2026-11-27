---
name: spring-boot-frontend-integration
description: >
  Use when integrating a React SPA or Next.js frontend with a Spring Boot API, configuring
  same-origin routing, frontend builds, static packaging, SPA routes, or frontend deployment modes.
  Covers the shared API boundary plus React and Next.js choices.
---

# Spring Boot Frontend Integration

Choose the reference for the frontend and deployment mode:

- [Shared frontend integration](references/frontend-integration.md) for API calls, same-origin
  routing, build integration, static packaging, and client-side routes.
- [React](references/react.md) for Vite, SPA structure, client-side routing, and packaged React
  assets.
- [Next.js](references/nextjs.md) for static export versus a Node.js server, rewrites, and
  server-side API calls.

Keep Spring Boot as the application API. Preserve its HTTP and error contracts, keep secrets out
of browser-visible configuration, and validate input on the server. Use
[Spring Boot Web MVC](../spring-boot-webmvc/SKILL.md) for backend HTTP behavior and
[Spring Boot Maven](../spring-boot-maven/SKILL.md) or
[Spring Boot Gradle](../spring-boot-gradle/SKILL.md) for build-file changes.
