# Design Readiness Assessment / Contract Proposal: <capability>

<!-- Assessment scope, the coverage matrix, and API contract section are required. Other sections are included when relevant. -->

**PRD status:** Draft / Approved<br>
**Gate result:** Exploratory (Draft PRD) / Pending (in progress) / Ready / Blocked<br>
**Updated:** <date><br>
**Related PRD:** <path and section>
**Assessment scope:** <approved delivery batch or subset of approved PRD requirement IDs/sections; exclude unrelated future candidates>
**Assessment record:** <path; required for an Approved PRD>
**Contract acceptance:** <Not required / Pending / accepted by contract owner on date>

## PRD contract coverage

| PRD requirement | Domain contract disposition | API contract disposition | Canonical contract(s) | Decision / gap |
| --- | --- | --- | --- | --- |
| <FR or product behavior in assessment scope> | <Covered by existing / Updated or created and accepted / No domain impact / Open or blocked; use Exploratory for a Draft PRD> | <Covered by existing / Updated or created and accepted / No API impact / Open or blocked; use Exploratory for a Draft PRD> | <Document and section for each applicable contract> | <Rationale, change, or unresolved decision> |

<For an Approved PRD, every requirement in scope needs separate final domain and API dispositions. For a Draft PRD, use Exploratory in the coverage cells; these provisional notes do not satisfy the gate. Ready means required new/updated contracts are accepted and no blocking decision remains. Blocked means a named issue prevents spec approval for this scope. No-change dispositions need a rationale. Pending is only for an incomplete assessment.>

## Domain design

<Omit this section when the assessment finds no shared domain impact.>

### Domain models

List models and conceptual attributes only when they clarify a shared product/domain contract. These are domain concepts, not database tables or columns. Keep database column names, storage types, indexes, nullability, and physical schema in implementation/data design or migration artifacts.

| Model | Meaning / responsibility | Key domain attributes | Identity / lifecycle | PRD requirement |
| --- | --- | --- | --- | --- |
| <Entity / value object> | <What it represents and owns> | <Business-meaningful attributes; avoid persistence details> | <Identity rule or state lifecycle, if relevant> | <FR ID> |

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
| <date> | <Exploratory / Pending / Ready / Blocked> | <Initial assessment or substantive update> |

<!-- Draft PRDs produce exploratory assessments only and cannot pass the design gate. -->
