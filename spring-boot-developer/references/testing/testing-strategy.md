# Testing Strategy

Read this to decide **which levels** of test to write, and why a large Spring Boot suite stays fast. The individual how-tos live in the leaf references.

## The levels are complementary, not alternatives

Test layers complement one another. Choose enough of each layer to cover framework behavior, integration boundaries, and business rules; do not expect one layer to replace the others.

- A **pure unit test** can have complete coverage and still pass while the application is broken. It exercises no persistence mapping, no request mapping, no serialization, no security configuration, no annotation. A missing `@RestController` means the endpoint returns nothing in production; the unit test never notices.
- A **full-context smoke test** catches wiring failures - a bad bean definition, a mapping that will not resolve - but as a black box it tells you little about *where* a behavioural bug is, and it is slow.
- A **sliced Spring test** boots the real framework around the components under test, so it catches annotation, mapping and SQL bugs a unit test misses, while staying fast.

The mistake is assuming green unit tests mean the framework-touching tests can be skipped.

## The distribution

A useful default distribution is:

- **The bulk - focused Spring tests.** Use built-in slices such as `@WebMvcTest` and `@DataJpaTest` for common layers. Use `@SpringBootTest(classes = { … })` with selected `@AutoConfigure…` annotations for a custom cross-layer context. Mock only slow or costly external boundaries; keep the rest real. See [testing-integration.md](testing-integration.md), [testing-slices-web.md](testing-slices-web.md), [testing-slices-persistence.md](testing-slices-persistence.md).
- **For web applications, keep a small number of smoke and request-level tests.** A context-load test catches wiring errors; a representative request verifies the exposed path. See [testing-integration.md](testing-integration.md) and [testing-rest-api.md](testing-rest-api.md).
- **Some unit tests** for complex framework-free logic - fast and valuable, rarely sufficient alone. See [testing-unit.md](testing-unit.md).

Avoid targeting a fixed percentage pyramid. Prefer the least expensive test that proves the behavior, while retaining framework-level coverage for mappings, serialization, wiring, and persistence.

Use the decision table in [SKILL.md](../../SKILL.md) for "what am I testing → which reference". This file is the reasoning behind it.

## Why the sliced test is the workhorse: context cost

Full-context `@SpringBootTest` startup is the expensive part - several seconds for a medium application, considerably longer for a large one once data initialisation and cache warming are involved. That is why the *sliced* Spring test is the default and full-context tests are kept to the smoke and end-to-end handful. Not because slices and end-to-end are alternatives, but because the full context is what is slow.

## Context caching

Spring reuses a loaded `ApplicationContext` across test classes with identical configuration; the first test pays startup, later ones reuse it. Getting this right is the difference between a suite that takes minutes and one that takes tens of minutes.

The cache key is the **`MergedContextConfiguration`**. Spring reuses a context only when all of these match:

- configuration classes / `classes`
- active profiles (`@ActiveProfiles`)
- properties (`@TestPropertySource` and inline `properties`)
- **mock beans** (`@MockitoBean` / `@MockitoSpyBean`)
- the test's initializers and customizers

If any differ, Spring builds a new context - another full startup. **Adding one `@MockitoBean` forks the cache.** So:

- Consolidate common mocks into an `abstract BaseIntegrationTest`; do not sprinkle `@MockitoBean` per class.
- Put shared configuration in `application-test.yml` with `@ActiveProfiles("test")` rather than scattering property overrides.
- **Avoid `@DirtiesContext`.** It discards the cached context; ten uses can cost minutes. Fix the isolation problem instead - usually a test that mutates shared state and does not clean up.
- Watch it with `logging.level.org.springframework.test.context.cache=DEBUG`.

A suite that has quietly grown twenty distinct context configurations pays twenty startups. It is worth counting.

## One container per suite

Real-infrastructure tests are only cheap if the container is reused. Reuse the suite's container configuration where contexts are shared. JUnit `@Container` static fields are class-scoped; use Spring-managed shared configuration or Testcontainers' singleton pattern when reuse across test classes is intended. See [testcontainers.md](testcontainers.md).

This composes with context caching: one container plus one cached context makes "always test against the real database" practical at scale. Two different container configurations in the same suite means two databases and two context-cache families, and is almost always accidental.

## Mocking is a boundary tool

Mock at architectural boundaries - external services, the clock, randomness. Keep everything else real. The full rationale and the over-mocking smell are in [testing-unit.md](testing-unit.md).

## Isolation and ordering

Tests must not depend on execution order or on each other's leftovers.

- Persistence slices such as `@DataJpaTest` are normally transactional and roll back. Full-context `@SpringBootTest(webEnvironment = RANDOM_PORT)` tests are **not** - the request runs on another thread, so the test's transaction does not cover it. Clean up explicitly, or use `@Sql` to reset.
- Inject a fixed `Clock` rather than calling `Instant.now()` in production code, so time-dependent assertions are stable. A test that fails at midnight or in another timezone is a real bug in the test.
- Parallel execution multiplies all of this. Get isolation right first.

## Native image test coverage

Spring recommends running most tests on the JVM and using native tests for code paths likely to differ under native compilation. Native test support and mock compatibility depend on the selected Spring, Mockito, and GraalVM versions; verify the project toolchain before excluding mock-based tests. See [GraalVM Native Image](../deployment/graalvm.md).

## If on Boot 3.5.x

- Context caching and its cache key work identically.
- **4.x only: context pausing** (Spring Framework 7) pauses eligible lifecycle components in inactive cached contexts and resumes them when the context is reused. See the [Spring Framework context pausing reference](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/context-pausing.html).
- Mock beans are `@MockitoBean` / `@MockitoSpyBean` on both lines. `@MockBean` is deprecated on 3.5.x and **removed** on 4.x.

## Gotchas

- Agent writes only unit tests - they pass while the application is broken; nothing exercises mapping, serialization or configuration
- Agent uses `@SpringBootTest` for everything - the full context is the slow part; use slices
- Agent targets a 70/20/10 pyramid - that weighting optimises for a cost cached slice tests no longer have
- Agent scatters `@MockitoBean` across test classes - each distinct set forks the context cache and costs another startup
- Agent scatters `@TestPropertySource` instead of one `application-test.yml` plus `@ActiveProfiles` - same cache-forking effect
- Agent reaches for `@DirtiesContext` to fix flakiness - that discards the cached context; fix the isolation problem
- Agent assumes a static `@Container` is shared across all test classes - its JUnit lifecycle is scoped to a test class
- Agent defines two Testcontainers configurations in one suite - two databases and two cache families
- Agent assumes a `RANDOM_PORT` test rolls back - it does not; the request runs on another thread
- Agent calls `Instant.now()` in production code and asserts on time - inject a `Clock`
- Agent assumes all tests run in a native image - keep the fast development suite on the JVM and target native tests to compatibility risks

## Related

- [testing-unit.md](testing-unit.md) · [testing-slices-web.md](testing-slices-web.md) · [testing-slices-persistence.md](testing-slices-persistence.md) · [testing-integration.md](testing-integration.md) · [testing-rest-api.md](testing-rest-api.md) · [testcontainers.md](testcontainers.md)

---

*The layered model and the sliced-test-first distribution credit **Paul Bakker** (Netflix), [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation). The context-caching cost model credits **Philip Riecks**, Testing Spring Boot Applications Demystified.*

## References

- [Spring Framework context pausing reference](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/context-pausing.html)
- [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation)
