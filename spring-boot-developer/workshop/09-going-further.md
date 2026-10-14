# 09 — Going further

The inventory application is a starting point. Extend it one behavior at a time, using the same prompt, review, test, and checkpoint loop.

## 1. Real authentication

Replace the workshop's unauthenticated access only after defining identity, session/token handling, and who may view or change inventory. Read [security fundamentals](../references/security/security-fundamentals.md) and choose [JWT](../references/security/jwt-authentication.md) or [OAuth2 resource server](../references/security/oauth2-resource-server.md) to match the identity provider.

```text
Plan how to add [identity provider] authentication and [authorization rule] to
the inventory app. Inspect the current API and security references. Identify
the login flow, protected routes, data ownership rules, migration needs, and
tests. Do not edit files until I review the plan.
```

## 2. Observability with Prometheus and Grafana

Use the [observability guide](../references/observability/observability.md) to choose useful signals. Keep metrics free of product identifiers or other high-cardinality values, and protect management endpoints.

```text
Plan a local Prometheus and Grafana setup for this inventory app. Check which
Actuator endpoints and registry dependencies already exist. Identify the
Compose changes, scrape configuration, dashboard signals, and management
endpoint exposure. Do not edit files until I review the plan.
```

## 3. Keyset pagination

If the inventory grows beyond a practical page size, compare offset and keyset pagination for the chosen ordering. Use the [REST controller](../references/web/rest-controllers.md) and [database schema and SQL](../references/persistence/database-schema-and-sql.md) guides.

```text
Design keyset pagination for GET /api/products using the current stable sort
order. Set a bounded default and maximum page size, define the continuation
cursor, and explain how filters interact with it. Do not implement until I
review the API and database plan.
```

## 4. Try a different front-end

Create a separate copy of the workspace and try [React](../references/frontend/react.md), [Next.js](../references/frontend/nextjs.md), [Angular](../references/frontend/angular.md), or [Vanilla JavaScript](../references/frontend/vanilla-js.md). Keep the same product API so you can compare frontends without changing backend behavior.

```text
Use spring-boot-developer to create a separate frontend for this inventory API
using [React, Next.js, Angular, or Vanilla JavaScript]. Read the matching frontend guide,
preserve the existing API contract, and do not modify the backend. Scaffold,
normalize if required, and verify the development proxy and production build.
Report the files and commands used.
```

## 5. Replay on another domain

Use the same workflow to generate a small application in another domain, such as a bookmark manager or equipment register. Describe the behavior and constraints in the prompt; do not reuse the inventory's product fields where they do not fit.

```text
Use spring-boot-developer to create a [domain] application in a new empty workspace.
The main records are [fields] and users need to [observable behavior]. Inspect
the environment and generator options first. Use [frontend], [persistence],
and [database]. Do not overwrite files. After generation, implement one
vertical slice, add focused tests, and report the exact checks and results.
```

## 6. Turn the skill into yours

When a project needs a new convention, update the focused reference or generator asset. Read the [skill instructions](../SKILL.md), check version compatibility, update navigation, and verify generated output. Do not introduce a second copy of a rule when an existing reference can own it.

```text
Update spring-boot-developer to document [team convention]. Inspect existing
references and scripts first. Propose the smallest edit, update any relevant
navigation and examples, and verify local links and generated output. Keep the
skill independent of external skills.
```

## 7. Teach the workshop

Run the chapters in order with a clean workspace. Ask learners to type or adapt the prompts, inspect diffs, and explain what each checkpoint proves. Leave time to compare different implementations and discuss why a prompt's constraints matter.

## Further reading

- [Spring Boot migration and compatibility](../references/spring-boot-migration.md)
- [Spring Data JPA](../references/persistence/spring-data-jpa.md)
- [Testing strategy](../references/testing/testing-strategy.md)
- [Docker deployment](../references/deployment/docker.md)
- [GraalVM Native Image](../references/deployment/graalvm.md)

**Checkpoint**

Any extension has a reviewed plan, implementation diff, focused tests, and recorded verification. If you made no extension, the completed inventory app and its checkpoints are the result.
