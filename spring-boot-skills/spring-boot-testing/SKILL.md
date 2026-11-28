---
name: spring-boot-testing
description: >
  Choose tests for Spring Boot application behavior and assess build readiness before
  a pull request or release. Covers unit, MVC and JPA slice, and integration tests,
  plus configured verification gates and evidence-based reporting.
---

# Spring Boot Testing

Implement or review Spring-specific tests and collect readiness evidence. Use an existing
risk-driven strategy; test-strategy owns cross-stack
verification obligations where installed. If that skill is unavailable, use the approved
acceptance criteria and existing test policy. Do not require a separate strategy for a small
change. Preserve the project's Boot/JDK line, dependencies, test conventions and required gates.

## Select the smallest sufficient test scope

| Need to establish | Read |
| --- | --- |
| Domain invariants, calculations, service decisions without framework behavior | [Unit testing](references/unit-testing.md) |
| HTTP binding, validation, JSON, response or MVC error mapping | [MVC testing](references/mvc-testing.md) |
| Mapping, repository queries, constraints, flush behavior | [Persistence testing](references/persistence-testing.md) |
| Application wiring, real transactions, concurrency, external protocol or real HTTP | [Integration testing](references/integration-testing.md) |
| Authentication, permissions, tenant isolation or denied side effects | [Security testing](references/security-testing.md), plus the relevant MVC or integration guide |
| Build, PR or release readiness | [Build verification](references/build-verification.md) |

Read only the relevant guides. Inspect existing tests and the effective dependencies before
selecting an annotation, test starter or client. Examples target Boot 4; verify the actual
minor, managed versions and imports. Do not migrate an existing Boot 3 project merely to use
these examples. Preserve the existing JUnit, assertion library, mocking and test naming
conventions unless changing them is requested.

## Protect correctness and evidence

- Start from approved behavior and material risks; exercise negative paths and assert observable
  outcomes. Do not invent fixed layer ratios, coverage thresholds or arbitrary test counts.
- Mocks are useful for pure decisions and delegation, not proof of real transactions, database
  isolation, serialization, security wiring or concurrency. Use compatible real dependencies
  when their semantics determine correctness. Do not infer PostgreSQL semantics from H2.
- Preserve actual security policy, including CSRF where applicable; test denied operations and
  unchanged persisted state when relevant. A test principal does not prove JWT decoding.
- Use isolated synthetic data. Real HTTP writes occur outside the test thread's transaction;
  clean only owned fixtures using the project's approved mechanism. Do not use production
  services as a fallback.
- Verify test discovery and actual execution, including required negative cases. Do not disable
  tests, weaken assertions, alter thresholds or introduce permissive security to obtain green CI.
- Record PASS, FAIL or NOT RUN for required checks, with the blocking reason separately.
  Missing required evidence means readiness is unestablished.

Read [Web MVC](../spring-boot-webmvc/SKILL.md) for HTTP contracts,
[JPA](../spring-data-jpa/SKILL.md) for persistence mapping,
[security](../spring-boot-security/SKILL.md) for policy, and
[browser regression](../agent-qa-browser-regression/SKILL.md) only for requested browser journeys.
Follow existing build commands; use the selected Maven or Gradle skill when build changes are
authorized. Test design or verification alone does not authorize publication, deployments,
production access, active scans or paid external evaluations.
