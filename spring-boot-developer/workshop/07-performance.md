# 07 — Performance

**In this chapter:**
- Measure before tuning
- Try a small set of Spring Boot, database, and frontend performance techniques
- Keep only changes supported by evidence

The goal is not to make a small workshop app win a benchmark. Read [performance](../references/observability/performance.md), [observability](../references/observability/observability.md), and [static resources](../references/web/static-resources.md) before changing settings.

---

## 1. The rule: measure first

Start with a read-only assessment:

```text
Use spring-boot-developer to assess the inventory application's performance.
Inspect a representative product-list request and identify a repeatable way to
measure latency, throughput, errors, and database work. Separate observations
from hypotheses. Do not edit files or add dependencies yet. Recommend one small
experiment only if the evidence identifies a bottleneck.
```

If no meaningful issue can be reproduced, record that and stop. The following steps are recipes to evaluate, not a checklist of required optimizations.

## 2. Enable Actuator signals

Ask whether the existing Actuator and Micrometer setup exposes useful measurements:

```text
Check which Actuator endpoints and Micrometer metrics already exist in this
project. Do not expose sensitive endpoints publicly and do not add dependencies
without explaining why. If request metrics are missing, propose the smallest
configuration change and show how to verify the endpoint links and a sample
request metric.
```

Verify only endpoints that exist on the classpath; exposing a name does not create its implementation.

## 3. Virtual threads

Consider this only when Java 21 or later is selected and the request workload is primarily blocking:

```text
Check whether virtual threads are supported by this project's selected Java and
Spring Boot versions and whether the workload could benefit. If so, propose
enabling the supported setting, explain compatibility concerns for any
thread-local or blocking dependencies, and define a before/after measurement.
Do not edit until I review the plan.
```

## 4. HTTP compression

Measure payload sizes first. If responses are large enough to matter, ask the agent:

```text
If the measured product API responses are large enough to benefit, enable HTTP
compression using properties managed by this project's Spring Boot version.
Choose appropriate MIME types and a minimum response size. Do not compress
twice behind a known proxy. Show the exact configuration diff and how to verify
the response with Accept-Encoding: gzip.
```

Verify the response with `Accept-Encoding: gzip` and compare transfer size. Avoid compressing twice behind a proxy.

## 5. Read-only transactions

If the application has a meaningful service layer, ask the agent:

```text
Inspect the product read paths and determine whether read-only transactions
would improve correctness or database behavior. If a meaningful service layer
exists, propose the smallest annotation change and a way to verify it. Do not
add a pass-through service solely to apply an annotation.
```

Compare database behavior and query results before and after.

## 6. Lazy-loaded routes

If Vue bundles are large, inspect the production bundle and route structure. Ask the agent:

```text
Inspect the production bundle and Vue routes. If secondary routes contribute
materially to the initial bundle, convert those route components to lazy imports.
Preserve navigation and error behavior, build the frontend, and report the
before/after initial asset sizes.
```

Keep the change only when it improves the actual loading path.

## 7. Static asset caching

Inspect the production asset names and current response headers. Ask the agent:

```text
Inspect the generated frontend asset names and current cache headers. Propose
long-lived caching only for content-hashed assets, while keeping the HTML entry
point revalidatable. Do not change files until I review the plan. Include curl
commands to verify both response headers.
```

Verify both response headers with `curl -I`.

## 8. Detect N+1 queries in tests

If the product listing loads related data, ask the agent:

```text
For this JPA project, add a PostgreSQL integration test that loads a
representative product list with related data and counts SQL statements. Seed
multiple products, assert the returned values, and fail if query count grows
with the number of products. Use Testcontainers and keep the test in the
configured integration-test phase.
```

Do this only when JPA is the selected persistence layer. The test should fail if query count grows with the number of products.

## 9. Stop optimizing

Compare the same request under the same conditions. Record the command, sample size, response time, errors, and database work. Keep changes only when they improve the measured concern without breaking functional tests or shifting overload to PostgreSQL.

| Technique | Evidence needed before keeping it |
|---|---|
| Actuator metrics | A metric answers a concrete operational question |
| Virtual threads | A blocking workload benefits under a repeatable load |
| HTTP compression | Payload size and transfer time improve |
| Read-only transactions | A meaningful service read path exists and the setting has a measurable effect |
| Lazy frontend routes | Initial bundle or route load improves |
| Static asset caching | Hashed assets are cached while HTML can revalidate |
| N+1 query test | Related data is loaded and query count is bounded |

**Checkpoint**

- A baseline exists for any performance change.
- Each retained change has a comparable before/after result.
- Functional tests still pass, and speculative changes are removed.

**Next →** [Chapter 08 — Deployment](08-deployment.md)
