# Design Readiness Assessment / Contract Proposal: <capability>

<!-- Assessment scope, the coverage matrix, and API contract section are required. Other sections are included when relevant. -->

**PRD status:** Approved<br>
**Gate result:** Pending (in progress) / Ready / Blocked<br>
**Updated:** <date><br>
**Related PRD:** <path and section><br>
**Assessment scope:** <approved delivery batch or subset of approved PRD requirement IDs/sections; exclude unrelated future candidates><br>
**Assessment record:** <standalone record path only when requested or required by repository convention; otherwise “Recorded in resulting spec(s)”><br>
**Contract acceptance:** <Not required / Pending / accepted by contract owner on date>

## PRD contract coverage

| PRD requirement | Domain contract disposition | API contract disposition | Canonical contract(s) | Decision / gap |
| --- | --- | --- | --- |
| <FR or product behavior in assessment scope> | <Covered by existing / Updated or created and accepted / No domain impact / Open or blocked> | <Covered by existing / Updated or created and accepted / No API impact / Open or blocked> | <Document and section for each applicable contract> | <Rationale, change, or unresolved decision> |

<Use only after the parent PRD is Approved and the required requirements document exists. Every requirement in scope needs separate final domain and API dispositions. Ready means required new/updated contracts are accepted and no blocking decision remains. Blocked means a named issue prevents spec approval for this scope. No-change dispositions need a rationale. Pending is only for an incomplete assessment.>

## Domain design

<Omit this section when the assessment finds no shared domain impact.>

### Domain models

List every domain model in scope and enumerate its domain fields. These are domain concepts, not database tables or columns. For each field, record its name, domain-level type/shape, whether it is required, optional, or conditional, its business meaning, and applicable constraints, defaults, or derived/lifecycle behavior. Mark unresolved fields as open. In this project, identifier types are `Long` by default or `String` when explicitly selected; never use UUID. Keep physical column names when they differ, SQL storage types, indexes, physical nullability, and table layout in implementation/data design or migration artifacts.

| Model | Meaning / responsibility | Identity / lifecycle | PRD requirement |
| --- | --- | --- | --- |
| <Entity / value object> | <What it represents and owns> | <Identity rule or state lifecycle, if relevant> | <FR ID> |

#### Domain fields

| Model | Field | Domain type / shape | Requiredness | Meaning and business constraints / defaults | PRD requirement |
| --- | --- | --- | --- | --- | --- |
| <Model> | <fieldName> | <Long / String for IDs; Email, Instant, enum, collection, value object, etc.> | <Required / Optional / Conditional> | <Meaning, allowed values, default, or lifecycle/derivation rule; mark open if unresolved> | <FR ID> |

### Model relationships

Use either a Mermaid conceptual relationship diagram or the table below to show ownership, direction, and cardinality; do not fill both with the same information. Prefer the diagram when several relationships are easier to review visually; use the table for a small or linear model. Show domain models only; this is not a database ERD. Cite related PRD requirement IDs.

```mermaid
flowchart LR
  MODEL_A["<Model A>"] -->|"<owns / references; 1 to many>"| MODEL_B["<Model B>"]
```

| Source model | Relationship | Target model | Cardinality / ownership | Invariant / rule | PRD requirement |
| --- | --- | --- | --- | --- | --- |
| <Model A> | <owns / references / records> | <Model B> | <one-to-one, one-to-many, optionality, lifecycle ownership> | <Tenant boundary or other rule> | <FR ID> |

### Shared domain rules

- **Invariants:** <rules that must always hold>
- **Lifecycle and state transitions:** <states, allowed transitions, outcomes>
- **Authorization / trust boundaries:** <relevant actors and server-side checks>

## API contract — required

Group operations by business capability or resource boundary, such as device registration, credential lifecycle, telemetry, or alert handling. Do not group by HTTP method, screen, or user role. State shared API conventions (versioning, error envelope, pagination, and authentication) once or link to the canonical API document. Keep MQTT and other device protocols in Integration and protocol contracts. For each affected operation, include method/resource, request inputs, response/status, authorization, and relevant errors or transition/idempotency behavior. Reference the canonical API contract instead of duplicating its full definition when one exists. If no API operation is affected, state **No API impact** and explain why; do not omit this section.

<If there is no API impact, give the reason here and omit the operation groups and table.>

### <Capability or resource group>

| Method and resource | PRD requirement | Purpose | Request | Response / status | Authorization | Errors / semantics |
| --- | --- | --- | --- | --- | --- | --- |
| <GET/POST/PUT/DELETE path> | <FR ID> | <behavior> | <path, query, and body inputs> | <response shape and HTTP status> | <permission / tenant rule> | <errors, state transition, idempotency, pagination as relevant> |

<Repeat a group heading and table for each capability that has API operations.>

## Integration and protocol contracts

| Integration / message | Producer and consumer | Contract / semantics | Failure and security behavior | PRD requirement |
| --- | --- | --- | --- | --- |
| <system, event, or protocol> | <who sends and receives> | <schema, identity, ordering, delivery, idempotency, or versioning as relevant> | <retry, timeout, rejection, authorization, or recovery behavior> | <FR or product behavior> |

<Omit when the assessed scope has no shared integration or protocol contract to define or change.>

## Compatibility and data implications

- **Existing clients / contracts:** <impact or none known>
- **Persistence / migration:** <impact or none known>
- **Operational constraints:** <relevant rate, reliability, or rollout concerns>

## Alternatives and decision

<Alternatives considered and accepted rationale, or link to an ADR when the decision is architectural and shared.>

## Open questions and conflicts

| Item | Impact | Decision needed from |
| --- | --- | --- |
| <item> | <impact> | <owner or Unknown> |

## Appendix

### Related documents

- PRD: <path>
- Design: <path>
- API contract: <path>
- ADRs / glossary: <paths>

### Supporting material (optional)

- <Evidence, diagrams, or references needed to review this assessment; omit when none>

### Revision history

| Date | Gate result | Change / decision |
| --- | --- | --- |
| <date> | <Pending / Ready / Blocked> | <Initial assessment or substantive update> |

<!-- This readiness result is part of sdd-spec and is required before approving a feature spec. -->
