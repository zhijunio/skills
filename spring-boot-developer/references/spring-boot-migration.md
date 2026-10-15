# Spring Boot Versions and Migration

## Establish the version before writing code

Read the build configuration - it is authoritative.

| Build tool | Where to look |
|---|---|
| Maven | `spring-boot-starter-parent` version, or `spring-boot-dependencies` imported in `dependencyManagement` |
| Gradle | the `org.springframework.boot` plugin version, or the `spring-boot-dependencies` platform |
| Either | a `spring-boot.version` property, CI workflow files, `Dockerfile` base image |

Also check the Java toolchain configured in the build and CI. Both supported Boot lines require Java 17 or later; newer Java features have their own JDK requirements.

## System requirements

| Boot line | Java | Maven | Gradle | Native image |
|---|---:|---:|---:|---:|
| 4.1.x | 17+ | 3.6.3+ | 8.14+ or 9.x | GraalVM 25+ |
| 3.5.x | 17+ | 3.6.3+ | 7.6.4+ or 8.4+ | GraalVM 22.3+ |

These are framework minimums, not project recommendations. The generator's selected version and the repository's Maven/Gradle Wrapper may require newer tools. Check the official [Boot 4 system requirements](https://docs.spring.io/spring-boot/system-requirements.html) or [Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) for the exact release.

## Release lines

| Line | Status | Framework | Notes |
|---|---|---|---|
| **4.1.x** | Current | Spring Framework 7 | The default target for new projects |
| 4.0.x | Maintenance | Spring Framework 7 | |
| 3.5.x | Final 3.x line; OSS support ended 2026-06-30, commercial support until 2032-06-30 | Spring Framework 6 | Existing projects and upgrade source only; Spring Initializr no longer generates it |
| ≤ 3.4 | Out of scope here | | Upgrade to 3.5.x first, then to 4.x |

For a new project, target the current 4.x line. Do not start a new project on 3.5.x. The topic guides describe Boot 4 only; for work inside a 3.5.x project, see [Working in an existing Boot 3.5.x project](#working-in-an-existing-boot-35x-project).

**Pin nothing that Boot manages.** The parent or the imported BOM sets versions for Hibernate, Jackson, Testcontainers, JUnit, Micrometer and several hundred more. Declaring your own version for any of them is how you get a runtime `NoSuchMethodError`. Add versions only for libraries Boot does not manage.

## The Boot 3.5 → 4.x compatibility index

This is the reference for both migration and for judging whether a given API exists in the target version.

### Build and starters

| Change | Detail |
|---|---|
| Modular starters | Boot split into `spring-boot-<technology>` modules, each rooted at package `org.springframework.boot.<technology>` |
| `spring-boot-starter-web` | → `spring-boot-starter-webmvc` |
| `spring-boot-starter-aop` | → `spring-boot-starter-aspectj` |
| `spring-boot-starter-oauth2-*` | → `spring-boot-starter-security-oauth2-*` (client, resource-server, authorization-server) |
| `spring-boot-starter-web-services` | → `spring-boot-starter-webservices` |
| Test starters | Per technology: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`. Self-contained - do not add plain `spring-boot-starter-test` alongside one |
| Flyway / Liquibase | **No longer transitive** from the JDBC/JPA starters. Declare `spring-boot-starter-flyway` or `-liquibase` or migrations silently never run |
| Optional dependencies | Excluded from the repackaged jar by default; set `<includeOptional>true</includeOptional>` if you rely on them |
| Auto-configuration internals | Members of auto-configuration classes are no longer public API - do not reach into them |
| **Auto-configuration follows the module** | A `spring.*` property whose owning module is absent **binds to nothing and reports nothing**. Verified on 4.1.0: `spring.data.web.pageable.serialization-mode=VIA_DTO` is silently ignored with only `spring-boot-starter-web` and `spring-data-commons` present, and takes effect as soon as `spring-boot-data-commons` is added. Same failure shape as the Flyway row above, one layer further in |
| Transitional | `spring-boot-starter-classic` / `-test-classic` bundle the old monolithic set. Migration aid only |

The renamed starters are **deprecated, not removed** - an old name still resolves on 4.x. Use the new name in new code; the build will not fail on the old one.

### Language and nullability

| Change | Detail |
|---|---|
| JSpecify | The framework is `@NullMarked`. `org.springframework.lang.Nullable` → `org.jspecify.annotations.Nullable`. A null checker or Kotlin in your build may now fail on previously silent code |

### JSON

| Change | Detail |
|---|---|
| Jackson 3 | `com.fasterxml.jackson.*` → `tools.jackson.*` |
| Mapper bean | Declare `JsonMapper`, not a generic `ObjectMapper` |
| Customizer | `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer` |
| Annotation | `@JsonComponent` → `@JacksonComponent` |
| `java.time` | Handled natively - do not register `JavaTimeModule` |
| Properties | `spring.jackson.*` now configures Jackson 3; the Jackson 2 equivalents moved to `spring.jackson2.*` |

Read [json-and-jackson.md](web/json-and-jackson.md).

### Security - Spring Security 7

| Removed | Replacement |
|---|---|
| `and()` chaining, `authorizeRequests()` | Lambda DSL only: `authorizeHttpRequests(auth -> …)` |
| `antMatchers()`, `mvcMatchers()` | `requestMatchers("/path/**")` |
| `AntPathRequestMatcher`, `MvcRequestMatcher` | `PathPatternRequestMatcher` |
| `WebSecurityConfigurerAdapter` | A `SecurityFilterChain` bean |
| `provider.setUserDetailsService(…)` | `new DaoAuthenticationProvider(userDetailsService)` |

Read [security-fundamentals.md](security/security-fundamentals.md).

### Persistence

| Change | Detail |
|---|---|
| Jakarta Persistence 3.2, Hibernate ORM 7 | A record may be an `@Embeddable` without the annotation |
| Spring Data | Version aligned with Boot 4 |

Read [spring-data-jpa.md](persistence/spring-data-jpa.md).

### Batch - Spring Batch 6

| Change | Detail |
|---|---|
| Default `JobRepository` is **resourceless** | In-memory; no restart, no audit. Add `spring-boot-starter-batch-jdbc` for the `BATCH_*` tables |
| `JobLauncher` + `JobExplorer` | Consolidated into `JobOperator` |
| `chunk(n, txManager)` | `chunk(n)` plus `.transactionManager(txManager)` |
| Sequence rename | `BATCH_JOB_SEQ` → `BATCH_JOB_INSTANCE_SEQ` - needs a migration on an existing database |

Read [spring-batch.md](integration/spring-batch.md).

### Testing

| Change | Detail |
|---|---|
| `@MockBean` / `@SpyBean` | **Removed.** Use `@MockitoBean` / `@MockitoSpyBean` |
| `@SpringBootTest` and MockMvc | No longer auto-provided; add `@AutoConfigureMockMvc` |
| `RestTestClient` | New in Framework 7, in the `spring-boot-resttestclient` module; enable with `@AutoConfigureRestTestClient` |
| `TestRestTemplate` | Now requires `@AutoConfigureTestRestTemplate` |
| Testcontainers 2.x | `org.testcontainers:junit-jupiter` → `testcontainers-junit-jupiter`; `org.testcontainers:postgresql` → `testcontainers-postgresql`; `org.testcontainers.containers.PostgreSQLContainer` → `org.testcontainers.postgresql.PostgreSQLContainer` |
| JUnit 6 | Replaces JUnit 5. No change to the code you write |
| Context pausing | Framework 7 freezes `@Scheduled` tasks and listeners in cached contexts between tests |
| Test auto-configure packages | Moved as part of modularization; let the IDE re-resolve imports |

Read [testing-strategy.md](testing/testing-strategy.md) and [testcontainers.md](testing/testcontainers.md).

### New in Boot 4 / Framework 7

| Feature | Replaces |
|---|---|
| Native API versioning - `version` attribute on mappings, `spring.mvc.apiversion.*` | Hand-rolled `/v1` controller duplication. Read [api-versioning.md](web/api-versioning.md) |
| Core resilience - `@Retryable`, `@ConcurrencyLimit`, `@EnableResilientMethods` in `org.springframework.resilience.annotation` | The separate Spring Retry project. **Not a repackaging** - attribute names and recovery behaviour differ. Read [resilience.md](integration/resilience.md) |
| `@ImportHttpServices` group registration | A manual `HttpServiceProxyFactory` bean per client. Read [http-clients.md](integration/http-clients.md) |

### Ecosystem version pairing

| Project | Boot 3.5.x | Boot 4.x |
|---|---|---|
| Spring Framework | 6 | 7 |
| Spring Security | 6 | 7 |
| Spring Batch | 5 | 6 |
| Spring Modulith | 1.4.x | 2.x |
| Spring AI | 1.x | 2.0 |
| Jackson | 2 | 3 |
| Testcontainers | 1.x | 2.x |
| JUnit | 5 | 6 |

Getting a pairing wrong is the most common cause of an unresolvable dependency graph. Spring AI 1.x on Boot 4, or 2.0 on Boot 3.5, will not work.

## Working in an existing Boot 3.5.x project

The topic guides describe Boot 4. In a 3.5.x project, use a topic guide for the design and patterns, then translate its Boot 4 code with the compatibility index above read in reverse (old starter names, Jackson 2, Security 6, Testcontainers 1, JUnit 5) and the topic differences below. Keep the project on 3.5.x unless the task is the upgrade.

Topics not listed here need nothing beyond the index: architecture, domain modelling, proxies, transactions, validation, error handling, HATEOAS, internationalization, idempotency, database concurrency, schema and SQL, auditing, multi-tenancy, backup, logging, observability, async and scheduling, WebSocket, Thymeleaf, CI/CD, local development, and ArchUnit.

| Topic | Boot 3.5.x difference |
|---|---|
| JPA | Hibernate 6.x and Jakarta Persistence 3.1. A record used as an `@Embeddable` **must** carry the annotation. `@UuidGenerator(style = VERSION_7)` needs Hibernate 6.6+ |
| Flyway | Arrives with the JDBC/JPA starters, so no `spring-boot-starter-flyway`. `flyway-database-postgresql` is still required on Flyway 10+ |
| MyBatis-Plus | `mybatis-plus-spring-boot3-starter` and its matching test starter; never substitute the Boot 4 artifact |
| Jackson | Boot registers `JavaTimeModule` itself, so still do not add it |
| Redis cache and session | `GenericJackson2JsonRedisSerializer` with an `ObjectMapper`. Spring Session properties are `spring.session.redis.*` (Boot 4: `spring.session.data.redis.*`) |
| Messaging | The Rabbit JSON message converter takes a Jackson 2 `ObjectMapper` |
| Spring Batch 5 | The job repository is JDBC-backed by default, with no `-batch-jdbc` starter. Launch with `JobLauncher`, query with `JobExplorer`. Use `chunk(n, transactionManager)`. The sequence is `BATCH_JOB_SEQ` |
| HTTP clients | No `@ImportHttpServices` or `spring.http.serviceclient.*`; build each proxy with `HttpServiceProxyFactory` (below) |
| Resilience | No core `@Retryable`, `@ConcurrencyLimit`, or `@EnableResilientMethods`; use Spring Retry (below) |
| API versioning | No `version` mapping attribute or `ApiVersionConfigurer`. Prefer not versioning yet; otherwise use path-prefixed controllers (`/api/v1/...`) that map onto one version-free service |
| OpenAPI | Use the springdoc 2.x line built for Spring Framework 6. springdoc is not BOM-managed, so the build will not warn about a mismatch |
| Security 6 | APIs removed in Security 7 still exist as deprecated; write the lambda DSL anyway. `DaoAuthenticationProvider.setUserDetailsService` exists. OAuth2 starters lack the `security-` prefix; SAML uses `spring-security-saml2-service-provider` |
| MFA on Security 6.5 | `FactorGrantedAuthority`, `@EnableMultiFactorAuthentication`, and `AuthorizationManagerFactories.multiFactor()` do not exist. Orchestrate factors in the existing `AuthenticationProvider` flow, grant authorities such as `FACTOR_PASSWORD` or `FACTOR_TOTP` only after that factor is verified, authorize on the required combination, and test those authorities directly. WebAuthn (`webAuthn` DSL with `webauthn4j-core`) and `oneTimeTokenLogin()` are available |
| Testing | `spring-boot-starter-test` instead of per-technology test starters. No `RestTestClient`; use `WebTestClient` or `TestRestTemplate` (no `@AutoConfigureTestRestTemplate` needed). `@MockBean` is deprecated; use `@MockitoBean`. No context pausing. Test auto-configure packages differ: `@WebMvcTest` is in `org.springframework.boot.test.autoconfigure.web.servlet`, `@AutoConfigureDataJpa` in `org.springframework.boot.test.autoconfigure.orm.jpa`, `@EntityScan` in `org.springframework.boot.autoconfigure.domain` |
| Testcontainers 1.x | `org.testcontainers:postgresql` and `org.testcontainers.containers.PostgreSQLContainer<>` |
| Maven packaging | Optional dependencies are included in the repackaged jar |
| Spring AI 1.x and MCP | 2.0 flattened `chat.options.*`, removed `ChatMemory.DEFAULT_CONVERSATION_ID` and `PromptChatMemoryAdvisor`, renamed `spring-ai-advisors-vector-store`, and made advisors builder-only; check the 1.x reference instead of porting 2.0 shapes. MCP tools use `@Tool`/`@ToolParam` with a `MethodToolCallbackProvider`, and remote transport is SSE. `CallAroundAdvisor` became `CallAdvisor` at 1.0 GA |
| Spring Modulith 1.4.x | Integration of module tests with Boot slice tests is 2.1+ only |
| Native image and Docker | GraalVM 22.3+. The bundled Dockerfiles work (Java 25, `jarmode=tools` since Boot 3.3) |
| BootUI | Not supported; BootUI 2.x targets Boot 4 only |

HTTP service proxy without `@ImportHttpServices`, one factory per external service:

```java
@Bean
InventoryClient inventoryClient(RestClient.Builder builder, InventoryProperties properties) {
    var restClient = builder.baseUrl(properties.baseUrl()).requestFactory(timeoutFactory()).build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(InventoryClient.class);
}
```

Spring Retry instead of core resilience; it needs `org.springframework.retry:spring-retry` and `spring-boot-starter-aop`:

```java
@EnableRetry

@Retryable(retryFor = ConnectException.class,
           maxAttempts = 4,                                   // total attempts, not retries
           backoff = @Backoff(delay = 200, multiplier = 2.0, maxDelay = 2000, random = true))
public StockLevel stockFor(ProductId productId) { … }

@Recover
public StockLevel fallback(ConnectException e, ProductId productId) {
    return StockLevel.unknown();
}
```

When upgrading this code to core resilience, translate `maxAttempts` to `maxRetries` minus one, flatten `@Backoff`, and replace `@Recover` with a `try`/`catch` at the caller.

## Running the migration

Upgrade **3.4 → 3.5 → 4.0** in separate steps. If the target is a later 4.x release, first complete and verify the 4.0 migration, then update to the selected 4.x release. Skipping 3.5 conflates unrelated breakage.

[OpenRewrite](https://docs.openrewrite.org/recipes/java/spring/boot4) has recipes for both the upgrade and the starter split:

| Recipe | Effect |
|---|---|
| `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` | Umbrella migration - build files, deprecated APIs, and the Framework 7 / Security 7 / Batch 6 sub-recipes |
| `org.openrewrite.java.spring.boot4.MigrateToModularStarters` | Rewrites starters to the modular set |
| `org.openrewrite.java.spring.boot4.SpringBootProperties_4_0` | Migrates renamed configuration properties |

Check the recipe catalogue before running - some Boot 4 recipes ship in a Community Edition requiring extra repository access, and the exact set moves between `rewrite-spring` releases.

**Always review the diff.** OpenRewrite renames the starter and rewrites the import; it will not tell you that your `ObjectMapper` customization silently stopped applying because the bean type changed, or that your Batch job lost restartability because nothing added the JDBC starter.

### Migration order that works

1. Upgrade to 3.5.x on Java 17+. Get green.
2. Run the Boot 4 umbrella recipe. Get it compiling.
3. Fix Security 7 by hand - the DSL removals need judgement the recipe cannot supply.
4. Add `spring-boot-starter-flyway`. Verify migrations actually run.
5. Add `spring-boot-starter-batch-jdbc` if you have Batch jobs, plus the sequence-rename migration.
6. Replace `@MockBean` with `@MockitoBean`; add `@AutoConfigureMockMvc` where `@SpringBootTest` relied on the old behaviour.
7. Move Testcontainers to 2.x coordinates and classes.
8. Only then adopt the new features - API versioning, core resilience, `@ImportHttpServices`.

## Gotchas

- Agent pins a version Boot already manages (Hibernate, Jackson, Testcontainers, JUnit) - remove it; the BOM decides
- Agent assumes a renamed starter was removed - the old names are deprecated but still resolve on 4.x
- Agent adds `spring-boot-starter-data-jpa` and expects Flyway to run - Boot 4 requires `spring-boot-starter-flyway` explicitly
- Agent mixes ecosystem versions (Spring AI 1.x on Boot 4, Modulith 1.x on Boot 4) - see the pairing table
- Agent migrates 3.4 straight to 4.0 - go via 3.5 so the breakage is separable
- Agent adds `spring-boot-starter-test` next to `spring-boot-starter-webmvc-test` - the `-test` starters are self-contained on Boot 4
- Agent writes Boot 4 code for a 3.5.x project without checking the build file - the `version` mapping attribute, `@ImportHttpServices` and core `@Retryable` do not exist there; translate topic-guide code with [Working in an existing Boot 3.5.x project](#working-in-an-existing-boot-35x-project)
- Agent adds `spring-boot-starter-classic` to make an upgrade compile - it is a transitional aid, not a destination

## Related

- [maven.md](tooling/maven.md) · [gradle.md](tooling/gradle.md) · [json-and-jackson.md](web/json-and-jackson.md) · [security-fundamentals.md](security/security-fundamentals.md) · [testing-strategy.md](testing/testing-strategy.md)

## References

- [Spring Boot 4.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Spring Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4.0 release notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [OpenRewrite Spring Boot 4 recipes](https://docs.openrewrite.org/recipes/java/spring/boot4)
