# 06 — Testing

**In this chapter:**
- Distinguish fast web tests from integration tests against PostgreSQL
- Ask the agent to test observable behavior, then inspect its test design
- Run the full suite and practice debugging one failure

Read the [testing strategy](../references/testing/testing-strategy.md), [web slice testing](../references/testing/testing-slices-web.md), and [Testcontainers](../references/testing/testcontainers.md) guides.

---

## 1. Why test agent-generated code?

The agent can produce plausible code that misses an edge case or makes an incorrect assumption. Tests turn the requested behavior into repeatable checks. Read the generated smoke test first; it may prove only that the application context starts.

## 2. The two test styles

Use focused tests for request binding, validation, JSON, and status codes, then use an integration test for behavior that needs the real PostgreSQL engine.

| Style | Scope | Typical command | Speed |
|---|---|---|---|
| Web slice | Controller and HTTP contract with collaborators replaced | Maven `test`; Gradle `test` | Fast |
| Integration | Application wiring, migrations, persistence, PostgreSQL behavior | Maven `verify`; Gradle configured integration task or `check` | Slower |

Do not use H2 to prove PostgreSQL-specific behavior. Follow the project's actual Maven or Gradle test configuration.

## 3. Add web tests for the product API

Ask the agent for focused HTTP tests:

```text
Use spring-boot-developer to add web-slice tests for the product API. Cover list
ordering, successful creation and its status code, validation errors, duplicate
SKU conflicts, reading/updating/deleting a product, and unknown IDs. Replace
only collaborators outside the web slice, use the test annotation supported by
this Spring Boot version, and assert response JSON and status codes. Do not add
tests that merely restate framework behavior. Run the focused test and report
the test class and behavior each case proves.
```

Review the diff. Confirm the tests run through Spring MVC's request handling and assert externally visible behavior.

## 4. Add an integration test with Testcontainers

Ask the agent to cover the real persistence path:

```text
Add a focused product integration test that exercises the application through
HTTP and PostgreSQL using Testcontainers. Reuse the project's existing
Testcontainers configuration and service-connection pattern where available.
Cover create, read, and delete, and verify the database constraint for duplicate
SKUs. Keep the test in the integration-test phase configured by this project.
Do not use H2. Run the test and report its exact command and result.
```

Check that the test uses the PostgreSQL container, applies the migration, and does not depend on a developer's local database. If the project has separate Maven Surefire/Failsafe phases, ensure the test naming matches them.

## 5. Run the full suite

For Maven:

```bash
./mvnw verify
```

For Gradle, use the configured lifecycle task (commonly `./gradlew check`). Docker must be available for Testcontainers. The first run may download the PostgreSQL image.

## 6. Speed up the feedback loop

Run the relevant web test while changing controller behavior, and run the integration test when changing persistence or migrations. Avoid waiting for the full container suite after every text or layout edit. Ask the agent to identify the narrowest test command from the actual build configuration.

## 7. Run a single test

For a Maven Surefire test class, for example:

```bash
./mvnw test -Dtest=ProductControllerTest
```

Use the project's actual test class and plugin settings. Integration tests may require a separate Failsafe goal or Gradle task.

## 8. Fix a failure with the agent

Choose a small test assertion to break locally, run the focused test, and give the failure output to the agent:

```text
This focused test failed with the output below. Trace the failure to its cause,
show me the relevant assertion and implementation path, then propose the
smallest fix. Do not change unrelated tests or code. After I review the fix,
rerun the focused test and report the result.
```

Restore any intentional test break, inspect the final diff, and run the relevant build command.

## When to write tests yourself vs. ask the agent

Write a test yourself when a single assertion will teach you the framework behavior. Ask the agent to scaffold repetitive cases, then review every case and remove redundant or weak assertions.

---

**Checkpoint**

- Web tests cover the API behavior and important error cases.
- PostgreSQL integration behavior uses Testcontainers rather than H2.
- The configured full test command passes, or its failure is understood.
- You can run one focused test and explain what it proves.

**Next →** [Chapter 07 — Performance](07-performance.md)
