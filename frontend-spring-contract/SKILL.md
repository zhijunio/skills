---
name: frontend-spring-contract
description: Owns the HTTP contract between a React or Vue frontend and a Spring Boot backend. Use when OpenAPI or a generated TypeScript client is involved; identifiers or money lose precision; dates shift; RFC 9457 errors, pagination, JWT refresh, optimistic concurrency, uploads, or trace ids cross the wire; or React and Vue clients disagree with Spring Boot responses.
license: MIT
metadata:
  author: Avinay Basnet
  version: '1.0'
---

# React/Vue and Spring Boot Contract

Own the seam between a React or Vue client and a Spring Boot server. Keep component design,
framework state management, controller implementation, persistence, and database design in their
respective skills.

## Establish the contract first

Before giving guidance or changing code:

1. Identify React or Vue and its version, the Spring Boot version, Java release, rendering mode
   (SPA, SSR, or hybrid), request library, and state/query library.
2. Locate `openapi.json` or `openapi.yaml`, springdoc configuration, and the client generator plus
   its exact options. If no document exists, state that the current client/server agreement is only
   an inferred contract.
3. Identify relevant database and Java types. `bigint`, `numeric`, `date`, `timestamptz`, UUIDs,
   and binary data require explicit wire decisions.
4. Separate wire DTOs from frontend domain models. Map once at the HTTP boundary.

Use companion skills for single-layer work:

| Work | Owner |
| --- | --- |
| React/Vue components, routing, SSR, composables/hooks, framework testing | the installed framework-specific skill |
| Controllers, Spring Security, Jackson, server-side `ProblemDetail`, JPA, Actuator | `spring-boot-developer` |
| Java records, `BigDecimal`, time types | `java-developer` |
| TypeScript strictness, branded types, discriminated unions | `typescript-developer` |
| PostgreSQL types, constraints, indexes | `postgresql-developer` |

When a companion is unavailable, name the missing verification and continue on the seam under an
explicit assumption.

## Non-negotiable wire rules

1. Treat the OpenAPI document as the source of truth. Change the producing schema or server,
   regenerate, and never hand-edit generated client code.
2. Send identifiers backed by Java `long`/PostgreSQL `bigint` as JSON strings.
3. Send money and exact decimals as strings; parse them into a decimal type or minor units from
   the original string.
4. Keep date-only values as `YYYY-MM-DD` strings. They are not JavaScript `Date` instances.
5. Represent instants as ISO 8601 values with an offset, normally UTC `Z`; render them with an
   explicit business timezone when the business meaning requires one.
6. Treat client route checks as navigation UX. Spring Security remains the authorization authority.
7. Regenerate and type-check the client after every contract change, then compile/test the server.

## Endpoint workflow

1. Decide wire types and failure types before writing the DTO.
2. Implement the server DTO and endpoint in the backend layer.
3. Publish the success and RFC 9457 failure shapes in OpenAPI.
4. Regenerate the document and the TypeScript client.
5. Type-check the generated client without casts that conceal contract drift.
6. Map generated DTOs to domain types at one boundary.
7. Test a success, declared business failure, validation failure, unauthenticated request, and
   unexpected server failure.

Completion requires clean generation/diff checks and compilation on both sides. A passing frontend
or backend alone does not prove the seam.

## Topic routing

- Generator choice, schema corrections, exact numeric types, and dates:
  [openapi-and-types.md](references/openapi-and-types.md)
- Error envelopes and stable frontend failure handling:
  [errors.md](references/errors.md)
- Token storage, refresh single-flight, SSR, 401 replay, and navigation checks:
  [auth-flow.md](references/auth-flow.md)
- React/Vue implementation translations:
  [framework-adapters.md](references/framework-adapters.md)
- Pagination, concurrency, uploads, and tracing:
  [http-behaviour.md](references/http-behaviour.md)
- Review pass: [checklist.md](references/checklist.md)

## Symptom routing

| Symptom | Read |
| --- | --- |
| IDs collide, end in unexpected zeroes, or money differs by a cent | [openapi-and-types.md](references/openapi-and-types.md) |
| A calendar date changes for users west of UTC | [openapi-and-types.md](references/openapi-and-types.md) |
| Generated fields are optional or have the wrong type | [openapi-and-types.md](references/openapi-and-types.md) |
| An error renders as `undefined` | [errors.md](references/errors.md) |
| Parallel 401s log the user out or refresh repeatedly | [auth-flow.md](references/auth-flow.md) |
| SSR first renders logged out, then flips after hydration | [auth-flow.md](references/auth-flow.md) |
| A stale write overwrites a newer one | [http-behaviour.md](references/http-behaviour.md) |
| Upload failure returns HTML or an empty response | [http-behaviour.md](references/http-behaviour.md) |
| A user-visible error cannot be connected to server logs | [http-behaviour.md](references/http-behaviour.md) |
