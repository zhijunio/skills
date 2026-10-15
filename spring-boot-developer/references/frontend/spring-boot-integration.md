# Frontend Integration with Spring Boot

Framework-independent integration for a single-page application in `frontend/` that is built into the Spring Boot jar. Read this together with the framework guide ([Vue](vue.md), [React](react.md), [Angular](angular.md), or [Vanilla JavaScript](vanilla-js.md)); each guide lists the dev-server command, port, and the script names used below. Next.js has its own packaging model; see [Next.js](nextjs.md).

## Contents

- [How the pieces fit](#how-the-pieces-fit)
- [API client rules](#api-client-rules)
- [Build the frontend with Maven](#build-the-frontend-with-maven)
- [Run in development and production](#run-in-development-and-production)
- [Forward client-side routes to the SPA](#forward-client-side-routes-to-the-spa)
- [CORS only when not proxying](#cors-only-when-not-proxying)
- [Verify the integration](#verify-the-integration)
- [Gotchas](#gotchas)
- [References](#references)

## How the pieces fit

| Mode | Frontend | API calls |
|---|---|---|
| Development | The framework dev server with hot reload (Vite on `VITE_PORT`, default 5173; Angular on 4200) | The dev server proxies `/api` to Spring Boot on `SPRING_BOOT_PORT` (default 8080), so the browser stays same-origin |
| Production | The build writes minified, content-hashed files to `src/main/resources/static` | Spring Boot serves the files and the API from one origin |

Use relative URLs such as `/api/items` in browser code. The same code then works behind the dev proxy and in the packaged jar, and neither mode needs CORS.

## API client rules

These apply to every framework guide:

- Keep HTTP calls in a service layer, separate from views and state.
- Preserve the HTTP status and the RFC 9457 fields (`type`, `title`, `detail`, and extensions) from error responses. Also handle empty, non-JSON, and malformed error bodies, and requests that never receive a response. See [Error handling](../web/error-handling.md).
- Keep large identifiers and exact decimal values as strings; represent `LocalDate` as `YYYY-MM-DD` and timestamps with an explicit offset. If OpenAPI client types are generated, keep those wire types at the API boundary and map them to UI state there. See [JSON and Jackson](../web/json-and-jackson.md) and [OpenAPI](../web/openapi.md).
- Treat a mutation and its follow-up list refresh as two outcomes. Once a create, update, or delete succeeds, a later refresh failure must not be reported as though the mutation failed; the server may already have committed it. Keep the successful result, show the refresh error with a retry, and keep mutation errors attached to the form or action that caused them. Record initial-load and retry errors in state rather than leaving rejected promises unhandled.
- Make loading state belong to the user action that owns it. Do not let a nested action clear a shared loading flag while an outer mutation is still running; disable conflicting actions or track state per operation.
- Validate input on the client for usability and on the server for correctness. If Spring Security CSRF protection is enabled, send its token with state-changing requests ([Security fundamentals](../security/security-fundamentals.md)).

## Build the frontend with Maven

Add the [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin) to `pom.xml`. The plugin, Node.js, and npm versions below are synced from `versions.json` (`vue.mavenFrontendPlugin`, `node.default`, `node.npm`) by `node scripts/sync-versions-in-docs.mjs`. Replace `LINT_SCRIPT` and `TEST_COMMAND` with the values from the framework guide:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>2.0.2</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <phase>generate-resources</phase>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>v24.21.0</nodeVersion>
                <npmVersion>11.20.0</npmVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <phase>generate-resources</phase>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>install</arguments></configuration>
        </execution>
        <execution>
            <id>npm run lint</id>
            <phase>generate-resources</phase>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run LINT_SCRIPT</arguments></configuration>
        </execution>
        <execution>
            <id>npm run test</id>
            <phase>generate-resources</phase>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>TEST_COMMAND</arguments></configuration>
        </execution>
        <execution>
            <id>npm run build</id>
            <phase>generate-resources</phase>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run build</arguments></configuration>
        </execution>
    </executions>
</plugin>
```

Bind every execution to `generate-resources`. Then `./mvnw verify` fails on frontend lint or test failures, and `./mvnw spring-boot:run` builds the frontend before Spring Boot serves `src/main/resources/static`. Drop the lint and test executions only if the project checks the frontend elsewhere.

Add `src/main/resources/static/` to `.gitignore` when it contains only build output.

## Run in development and production

Development uses two terminals:

```bash
./mvnw spring-boot:run          # terminal 1: backend
cd frontend && npm run dev      # terminal 2: dev server (Angular: npm start)
```

Open the dev server URL from the framework guide. Production is a single jar:

```bash
./mvnw clean package
java -jar target/my-app.jar     # http://localhost:${SPRING_BOOT_PORT:-8080}
```

Content-hashed files under `/assets/**` can be cached for a year; keep `index.html` uncached so clients pick up new bundles. See [static resource caching](../web/static-resources.md).

## Forward client-side routes to the SPA

With HTML5 history routing, a browser refresh on `/items/123` reaches Spring Boot, which has no mapping for it. Forward unmapped non-API 404s to `index.html` from an `ErrorController`. Static files and `@RestController` mappings still win, and `/api/**` and `/actuator/**` keep real 404 responses:

```java
package com.example.demo.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, HttpServletResponse response) {
        Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (status != null
                && status == HttpStatus.NOT_FOUND.value()
                && path != null
                && !path.startsWith("/api/")
                && !path.startsWith("/actuator/")) {
            // The ERROR dispatch already set 404; the SPA shell must be served as 200.
            response.setStatus(HttpStatus.OK.value());
            return "forward:/index.html";
        }
        return ResponseEntity.status(status != null ? status : 500).build();
    }
}
```

`MockMvc` does not perform the servlet `ERROR` dispatch, so a `@WebMvcTest` against an unmapped path returns a raw 404 and never reaches `SpaController`. Test the forward against a real embedded server:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class SpaControllerIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void shouldForwardDeepLinksToTheSinglePageApp() {
        restTestClient.get().uri("/items/123")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("<html"));
    }
}
```

## CORS only when not proxying

The dev proxy keeps browser requests same-origin, so the setup above needs no CORS configuration. Add CORS only when the browser calls the backend origin directly, for example a frontend served from another host. Restrict it to the `dev` profile and the exact dev-server origin:

```java
@Configuration
@Profile("dev")
public class DevCorsConfig {

    @Bean
    public CorsFilter corsFilter(@Value("${FRONTEND_DEV_ORIGIN:http://localhost:5173}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

Start the backend with `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. With Spring Security, configure CORS through `http.cors(...)` instead so it runs before authentication; see [Security fundamentals](../security/security-fundamentals.md).

## Verify the integration

- Development: the dev-server URL loads, edits hot-reload, and `/api` calls succeed through the proxy.
- Production: after `./mvnw clean package`, the jar serves `/`, a direct load of a nested route such as `/items/123` returns the SPA, and an unknown `/api/...` path returns a JSON 404.
- `./mvnw verify` runs the frontend lint, unit tests, and build.
- The browser network tab shows relative `/api` requests and no CORS preflights in either mode.

## Gotchas

- Agent hardcodes `http://localhost:8080` in browser code - use relative `/api` URLs so the proxy and the packaged jar both work
- Agent adds CORS alongside the dev proxy - the proxy already makes requests same-origin
- Agent tests the SPA forward with `@WebMvcTest` - MockMvc skips the `ERROR` dispatch; use a `RANDOM_PORT` test
- Agent forwards every 404 to `index.html` - exclude `/api/**` and `/actuator/**` so API clients still get real 404s
- Agent reports a failed list refresh as a failed save - the mutation may already be committed

## References

- [Frontend Maven Plugin](https://github.com/eirslett/frontend-maven-plugin)
- [Spring Boot static content](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.static-content)
- [Spring Framework CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)
