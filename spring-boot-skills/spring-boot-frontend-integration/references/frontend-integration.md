# Frontend integration

Use this with [React](react.md) or [Next.js](nextjs.md) when a JavaScript
frontend consumes a Spring Boot API or ships with the application.

## Keep the API same-origin

In development, proxy browser requests from the frontend dev server to Spring
Boot. In production, route `/api/**` to Spring Boot and page requests to the
frontend. Browser code should use relative URLs such as `/api/orders`. This
keeps development and production requests same-origin and avoids development
CORS rules.

Keep HTTP calls in a service layer. Preserve status codes and RFC 9457 error
fields. Handle empty, non-JSON, malformed, and unavailable responses. Treat a
successful mutation and a failed follow-up refresh as separate outcomes. See
[Spring Boot Web MVC](../../spring-boot-webmvc/SKILL.md) for the backend contract.

Keep secrets out of browser-visible environment variables. Validate user
input on the server even when the frontend also validates it. If Spring
Security enables CSRF protection, send the CSRF token with state-changing
requests.

## Build a React SPA with Maven

For Maven, use `frontend-maven-plugin` to install the project's selected Node
and npm versions, then run the frontend checks and production build in
`generate-resources`. Use the exact script names defined in
`package.json`. Use `npm ci` when the project commits a lockfile.
Pin the plugin version in `<pluginManagement>` and track that version in the project’s existing version-management convention; do not copy an unpinned third-party plugin declaration.

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install-node</id>
            <phase>generate-resources</phase>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>NODE_VERSION_FROM_PROJECT</nodeVersion>
                <npmVersion>NPM_VERSION_FROM_PROJECT</npmVersion>
            </configuration>
        </execution>
        <execution>
            <id>frontend-checks</id>
            <phase>generate-resources</phase>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>ci</arguments></configuration>
        </execution>
        <execution>
            <id>frontend-build</id>
            <phase>generate-resources</phase>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run build</arguments></configuration>
        </execution>
    </executions>
</plugin>
```

Add lint and test executions using project scripts. Bind the production build
before Spring Boot packages `src/main/resources/static`. Ignore that directory
when it contains generated output only. For Gradle, use [Spring Boot Gradle](../../spring-boot-gradle/SKILL.md) and make the frontend build a dependency of `processResources` and the verification task.

## Development and production

Run Spring Boot and the frontend dev server as separate processes during
development. Configure the dev-server proxy for `/api/**`. In production,
either package a static SPA build in the Spring Boot jar or serve the frontend
from a static host or reverse proxy. Configure static resource caching so
content-hashed assets can be cached while the HTML entry point is revalidated.

## Client-side routing

For an SPA using history routing, a nested route must still load after browser
refresh. Add a narrowly scoped fallback to `index.html` for frontend routes.
Keep `/api/**`, `/actuator/**`, and real static-resource 404s as 404 responses.
Verify fallback behavior against a running packaged application because
`MockMvc` does not run the servlet error dispatch.

## Verify

Run frontend lint, type checks, tests, and a production build. Package the
application and request `/`, a nested frontend route, a hashed asset, and an
API route. Confirm the frontend build cannot silently reuse stale output.
