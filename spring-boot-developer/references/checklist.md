# Best Practices Checklist

Use this for a review pass over existing code. Each rule links to the reference that explains when it does not apply.

This checklist covers Spring application concerns. Use the focused architecture and Java references in this directory for records, sealed types, `Optional`, `var`, and pattern matching.

## Versions and build

| # | Rule | Reference |
| - | ---- | --------- |
| 1 | Target the current Boot 4.x line. Do not start a new project on 3.5.x. | [spring-boot-migration.md](spring-boot-migration.md) |
| 2 | Never declare a version for anything the Boot BOM manages. | [maven.md](tooling/maven.md) · [gradle.md](tooling/gradle.md) |
| 3 | On Boot 4, add `spring-boot-starter-flyway` explicitly - it is no longer transitive, and without it migrations silently never run. | [flyway.md](persistence/flyway.md) |
| 4 | Migrate 3.4 → 3.5 → 4.0 in separate commits, then review the diff. | [spring-boot-migration.md](spring-boot-migration.md) |
| 5 | Commit the build wrapper. Pin the JDK with a Gradle toolchain, not `sourceCompatibility`. | [maven.md](tooling/maven.md) · [gradle.md](tooling/gradle.md) |

## Structure

| # | Rule | Reference |
| - | ---- | --------- |
| 6 | Package by feature, not by layer. Entities and repositories package-private. | [code-organization.md](architecture/code-organization.md) |
| 7 | Escalate one rung at a time: package-private → Modulith → nested modules → build modules. | [code-organization.md](architecture/code-organization.md) |
| 8 | Global `config/` and `shared/` marked `@ApplicationModule(type = OPEN)` under Modulith. | [spring-modulith.md](architecture/spring-modulith.md) |
| 9 | Authentication is global; **authorization belongs to the feature**. | [security-fundamentals.md](security/security-fundamentals.md) |
| 10 | Enforce layout and dependency rules with ArchUnit or Modulith - not review. | [archunit.md](architecture/archunit.md) |

## Beans and proxies

| # | Rule | Reference |
| - | ---- | --------- |
| 11 | Constructor injection only. No field injection, no `@Autowired` on a sole constructor. | [spring-proxies-and-di.md](architecture/spring-proxies-and-di.md) |
| 12 | **Never call an annotated method on `this`** - the proxy is bypassed and `@Transactional`, `@Async`, `@Cacheable`, `@Retryable` and `@PreAuthorize` all silently do nothing. | [spring-proxies-and-di.md](architecture/spring-proxies-and-di.md) |
| 13 | No Lombok. Write the constructor, the accessors and the logger. | [java-in-spring.md](architecture/java-in-spring.md) |
| 14 | Records for DTOs, commands, events and value objects; classes for entities and beans. | [java-in-spring.md](architecture/java-in-spring.md) |

## Web

| # | Rule | Reference |
| - | ---- | --------- |
| 15 | Keep business orchestration in the service. No entities in or out, no `@Transactional`, no `try`/`catch` in controllers. | [rest-controllers.md](web/rest-controllers.md) |
| 16 | Errors are RFC 9457 `ProblemDetail`. No success/error envelope. Set `type`. | [error-handling.md](web/error-handling.md) |
| 17 | Extend `ResponseEntityExceptionHandler`; global advice for framework errors, feature advice for domain errors. | [error-handling.md](web/error-handling.md) |
| 18 | Never put `ex.getMessage()` in a 500. Log it; return something generic. | [error-handling.md](web/error-handling.md) |
| 19 | Cap `Pageable` with `max-page-size`. Never return `Page<Entity>`. | [rest-controllers.md](web/rest-controllers.md) |
| 20 | Validate the request record with `@Valid`; report **all** violations. | [validation.md](web/validation.md) |
| 21 | On Boot 4, declare `JsonMapper` beans, not `ObjectMapper` - and never replace Boot's mapper bean. | [json-and-jackson.md](web/json-and-jackson.md) |
| 22 | Version the mapping, not the controller. One resolution strategy. | [api-versioning.md](web/api-versioning.md) |
| 23 | Define safe JSON representations for Java `Long`, `BigDecimal`, date-only values, and timestamps consumed by JavaScript clients. | [json-and-jackson.md](web/json-and-jackson.md) |
| 24 | If generating a frontend client, pin its generator, regenerate from the contract, type-check, and keep generated wire models at the API boundary. | [openapi.md](web/openapi.md) |
| 25 | Frontend error handling covers Problem Details plus empty, non-JSON, malformed, and no-response failures. | [error-handling.md](web/error-handling.md) |

## Persistence

| # | Rule | Reference |
| - | ---- | --------- |
| 26 | Value-object `@EmbeddedId` with a time-ordered UUIDv7. Never `GenerationType.UUID` (v4) on a high-write table. | [spring-data-jpa.md](persistence/spring-data-jpa.md) |
| 27 | `@Version` as a nullable `Long` - optimistic locking *and* new-state detection. | [spring-data-jpa.md](persistence/spring-data-jpa.md) |
| 28 | Do not write `equals`/`hashCode` on an entity unless there is a stable natural key; then use `instanceof`, never `getClass()`. | [spring-data-jpa.md](persistence/spring-data-jpa.md) |
| 29 | `fetch = LAZY` on every to-one. `@Enumerated(STRING)`. No association in `equals`/`hashCode`/`toString`. | [spring-data-jpa.md](persistence/spring-data-jpa.md) |
| 30 | `spring.jpa.open-in-view=false` and `ddl-auto=validate` - in every environment, including local. | [spring-data-jpa.md](persistence/spring-data-jpa.md) |
| 31 | One repository per aggregate root. Projections for list views. Never `findAll()` in an endpoint. | [spring-data-jpa.md](persistence/spring-data-jpa.md) |
| 32 | Never edit an applied migration. Expand and contract for anything destructive. | [flyway.md](persistence/flyway.md) |

## Transactions and side effects

| # | Rule | Reference |
| - | ---- | --------- |
| 33 | `@Transactional` on the service. Class-level `readOnly = true` with method-level overrides for writes. | [transactions.md](persistence/transactions.md) |
| 34 | **Never make a network call inside a transaction.** | [transactions.md](persistence/transactions.md) |
| 35 | Bind side effects to commit - `@TransactionalEventListener(AFTER_COMMIT)`, or Modulith's `@ApplicationModuleListener`. | [transactions.md](persistence/transactions.md) |
| 36 | Domain exceptions unchecked, so rollback is automatic. | [error-handling.md](web/error-handling.md) |
| 37 | Never stack `@Retryable` and `@Transactional` on one method - put the retry on a calling bean. | [resilience.md](integration/resilience.md) |

## Integration

| # | Rule | Reference |
| - | ---- | --------- |
| 38 | **Every HTTP client needs a connect and a read timeout.** The default is effectively infinite. | [http-clients.md](integration/http-clients.md) |
| 39 | Translate remote failures to domain exceptions at the client boundary. Never leak the upstream message. | [http-clients.md](integration/http-clients.md) |
| 40 | Retry only transient failures, with backoff **and jitter**. Set `includes` - the default retries bugs. | [resilience.md](integration/resilience.md) |
| 41 | Every message consumer is idempotent - brokers deliver at least once. | [messaging.md](integration/messaging.md) |
| 42 | Never publish to a broker inside a transaction - use an outbox, or Modulith's event registry. | [messaging.md](integration/messaging.md) |
| 43 | Never set `spring.json.trusted.packages=*`, and always wrap with `ErrorHandlingDeserializer`. | [messaging.md](integration/messaging.md) |

## Security

| # | Rule | Reference |
| - | ---- | --------- |
| 44 | Security 7 is lambda-DSL only. Scope each chain with `securityMatcher` unless it is intentionally catch-all; `anyRequest()` is last. | [security-fundamentals.md](security/security-fundamentals.md) |
| 45 | Enable method security when features use `@PreAuthorize`; keep coarse URL rules in the chain and feature authorization with the feature. | [security-fundamentals.md](security/security-fundamentals.md) |
| 46 | Configure `authenticationEntryPoint` and `accessDeniedHandler` - advice cannot catch filter exceptions. | [security-fundamentals.md](security/security-fundamentals.md) |
| 47 | Authorize STOMP inbound commands and subscriptions, require CSRF for cookie-authenticated `CONNECT`, and bound connections and frames. | [websocket.md](web/websocket.md) |
| 48 | Prefer an external IdP. If self-issuing, use Spring's JWT encoder/decoder and bearer filter; use `AuthenticationFilter` for credential endpoints when the flow requires it. | [jwt-authentication.md](security/jwt-authentication.md) |
| 49 | Validate issuer and audience for JWTs; use `issuer-uri` and `audiences` for an external resource server. Scopes are `hasAuthority('SCOPE_…')`, not `hasRole`. | [jwt-authentication.md](security/jwt-authentication.md) · [oauth2-resource-server.md](security/oauth2-resource-server.md) |
| 50 | Refresh tokens are opaque, high-entropy and rotated on use; detect reuse and revoke the session. Hash at rest by default; raw storage requires an explicit requirement and documented database-leak impact. | [jwt-authentication.md](security/jwt-authentication.md) |

## Configuration and operations

| # | Rule | Reference |
| - | ---- | --------- |
| 51 | `@ConfigurationProperties` records with `@Validated`, not scattered `@Value`. Fail at startup. | [configuration.md](configuration/configuration.md) |
| 52 | Secrets never in the repository. Required ones have no default, so absence stops startup. | [configuration.md](configuration/configuration.md) |
| 53 | The no-profile default must be safe. Never `@Profile` a business implementation. | [configuration.md](configuration/configuration.md) |
| 54 | Never `management.endpoints.web.exposure.include: "*"` - that publishes `/env` and `/heapdump`. | [observability.md](observability/observability.md) |
| 55 | Database in **readiness**, never liveness. | [observability.md](observability/observability.md) |
| 56 | Never tag a metric with a user id, entity id or exception message. | [observability.md](observability/observability.md) |
| 57 | Log the exception object, not `ex.getMessage()`. Never log secrets or personal data. | [observability.md](observability/observability.md) |
| 58 | Enable virtual threads on Java 21+ - they are off by default - then size the connection pool, which is now the bottleneck. | [async-and-scheduling.md](integration/async-and-scheduling.md) |
| 59 | Virtual threads do nothing for a sequential fan-out inside one request. `StructuredTaskScope` is that fix - preview on Java 25, in a service and never in a controller, never around a repository call inside `@Transactional`. | [async-and-scheduling.md](integration/async-and-scheduling.md) |
| 60 | A `@Scheduled` job runs once per replica. Make it idempotent or claim the work. | [async-and-scheduling.md](integration/async-and-scheduling.md) |
| 61 | `MaxRAMPercentage`, not `-Xmx`. Run as non-root. Graceful shutdown shorter than the orchestrator's grace period. | [Docker](deployment/docker.md) · [GraalVM Native Image](deployment/graalvm.md) |

## Testing

| # | Rule | Reference |
| - | ---- | --------- |
| 62 | Layer the levels - unit, sliced, smoke, one end-to-end. Green unit tests do not mean a working application. | [testing-strategy.md](testing/testing-strategy.md) |
| 63 | Guard the context cache: shared `application-test.yml`, mocks in a base class, avoid `@DirtiesContext`. | [testing-strategy.md](testing/testing-strategy.md) |
| 64 | **Testcontainers against the real database, never H2** - H2 also means your migrations never run. | [testing-slices-persistence.md](testing/testing-slices-persistence.md) |
| 65 | One container for the suite. `static` or a shared `@TestConfiguration`. Pin the image tag. | [testcontainers.md](testing/testcontainers.md) |
| 66 | `@ServiceConnection` where a factory exists; `DynamicPropertyRegistrar` only for values that do not exist at authoring time. | [testcontainers.md](testing/testcontainers.md) |
| 67 | `@WebMvcTest` needs `@Import(SecurityConfig.class)` - otherwise it tests Boot's defaults. | [testing-slices-web.md](testing/testing-slices-web.md) |
| 68 | Assert the negative security cases: 401 and 403. | [testing-slices-web.md](testing/testing-slices-web.md) |
| 69 | Awaitility for anything asynchronous. Never `Thread.sleep`. | [testing-integration.md](testing/testing-integration.md) |
| 70 | Inject a `Clock`; never call `Instant.now()` in code you need to test. | [testing-unit.md](testing/testing-unit.md) |
| 71 | `@MockitoBean`, not `@MockBean` - removed in Boot 4. | [spring-boot-migration.md](spring-boot-migration.md) |

## AI

| # | Rule | Reference |
| - | ---- | --------- |
| 72 | Pair the versions: Spring AI 2.0 with Boot 4, 1.x with Boot 3.5. | [spring-ai.md](integration/spring-ai.md) |
| 73 | Scope the conversation id per user or session - a shared one leaks history between users. | [spring-ai.md](integration/spring-ai.md) |
| 74 | Never concatenate user input into a prompt. Never execute or render model output unescaped. | [spring-ai.md](integration/spring-ai.md) |
| 75 | `log-prompt: false` in production, and never tag a metric with prompt content. | [ai-observability.md](observability/ai-observability.md) |
| 76 | Cap `max-tokens`, track token usage per operation, and alert on spend. | [ai-observability.md](observability/ai-observability.md) |

## Cross-cutting API safeguards

| # | Rule | Reference |
| - | ---- | --------- |
| 77 | Use resource-oriented paths; put filters, pagination and allowlisted sorting in query parameters. Keep credentials out of URLs. | [rest-controllers.md](web/rest-controllers.md) |
| 78 | Define a scoped idempotency contract for retryable create operations; persist keys atomically and recheck authorization before replaying a response. | [api-idempotency.md](web/api-idempotency.md) |
| 79 | Apply rate limits and hard resource bounds at the appropriate boundary in deployed systems; add application-level rate-limit code to a sample only when requested. Return a documented overload response when limits apply. | [rate-limiting.md](integration/rate-limiting.md) |
| 80 | Treat database constraints as the final concurrency guard; translate constraint failures only after handling the failed transaction boundary. | [database-concurrency.md](persistence/database-concurrency.md) |
| 81 | Derive tenant context from trusted identity and enforce it in every persistence, cache, storage and background-job path. | [multi-tenancy.md](persistence/multi-tenancy.md) |
| 82 | CI promotes the same verified artifact digest; schema changes remain compatible across rollout and rollback windows. | [ci-cd-release.md](deployment/ci-cd-release.md) |
| 83 | Public API changes have compatibility checks; deprecation has a replacement, owner, dates and measured client usage. | [api-versioning.md](web/api-versioning.md) |
| 84 | Secrets have an owner, least-privilege scope, rotation and revocation procedure; CI does not expose them to untrusted code. | [configuration.md](configuration/configuration.md) |
| 85 | Alerts have an owner, user impact, actionable runbook and SLO-aligned threshold; logs validate client-supplied correlation IDs. | [observability.md](observability/observability.md) |
| 86 | External client tests exercise the network boundary; local substitutes cannot silently fall through to live providers. | [external-service-testing.md](testing/testing-external-service.md) |
| 87 | Runtime-managed OAuth registrations are privileged configuration: allowlist endpoint hosts, protect and audit changes, encrypt stored secrets, and let database uniqueness constraints resolve create races. | [OAuth2 login and SSO](security/oauth2-login.md) · [database concurrency](persistence/database-concurrency.md) |

---

## The five that cause the most damage

1. **Self-invocation.** `this.method()` silently disables `@Transactional`, `@Async`, `@Cacheable`, `@Retryable` - and `@PreAuthorize`, which makes it a security hole. No error, no warning.
2. **H2 in persistence tests.** Your migrations never run, dialect-specific SQL diverges, and the suite is green the whole time.
3. **No HTTP client timeout.** One slow dependency exhausts your request threads and takes the application down with it.
4. **`exposure.include: "*"`.** `/actuator/heapdump` is a downloadable copy of your process memory, including every secret in it.
5. **`open-in-view` left on.** N+1 problems succeed silently in development and surface as a production incident.

## References

- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
