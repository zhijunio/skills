---
name: sdd-design
description: Required after an Approved PRD and before implementation specs to assess domain/API contract readiness, reuse or update canonical contracts, and surface unresolved decisions. Draft proposals may explore a Draft PRD but do not satisfy the gate. Not for product scope decisions, one-slice specs, or implementation plans.
---

# sdd-design

## Role

Act as the mandatory design-readiness gate between an Approved PRD and `sdd-spec`. Assess the scope being prepared for specification: the approved delivery batch or a selected subset of requirements already approved in the PRD, plus shared contracts those requirements depend on. Do not pull deferred candidates or unrelated roadmap phases into the gate. If the requested work changes or adds product scope, return to `sdd-prd` for revision and approval first. Assess shared domain concepts and relationships, business invariants, lifecycle/state transitions, authorization and tenant boundaries, API resources and behavior, and external integration/protocol contracts. Record domain and API coverage separately for every requirement; API contract assessment is required even when the conclusion is no API change. Reuse existing canonical contracts where they already cover the scope; create or revise contracts only where the assessment finds a real gap.

This is product-facing domain and interface design, not a second PRD or an implementation blueprint. Do not prescribe UI acceptance details, service/class decomposition, database products, deployment topology, or other technology choices unless an approved decision requires documenting them in an ADR or architecture contract. Slice-specific user flows and acceptance criteria belong in `sdd-spec`.

A Draft PRD may support exploratory design, but all conclusions are provisional and do not satisfy the required gate. The assessment scope must name the approved delivery batch or requirement IDs/sections. If the intended scope is unclear, or the PRD has not approved a current scope, return to `sdd-prd` to clarify it. The gate is complete only when every requirement in the assessed scope has a recorded disposition: covered by an existing contract, covered by an accepted new/updated contract, or no shared domain/API contract impact. A product decision or contract gap blocks only the affected scope; unrelated features may proceed if their own assessment is Ready.

Present the assessment and proposed changes in the user's language. For an Approved PRD, persist a concise Ready/Blocked assessment, its requirement coverage, and update date in the repository's established design documentation location so `sdd-spec`, `sdd-plan`, and `sdd-build` can verify the gate. Record substantive changes to the assessment there; use repository versioning conventions for canonical domain/API contracts, adding a concise dated change record when no convention exists. Do not add a separate semantic version to a gate assessment unless the repository treats it as a versioned contract. If no location is established, follow repository documentation conventions; do not create empty domain/API contract files merely to record a no-change result.

The assessment is mandatory after PRD approval even when no contract changes are needed. Feature-local behavior belongs in `sdd-spec`.

## When to use

- Required readiness assessment for an Approved PRD scope before that scope enters `sdd-spec`, including when existing contracts may already be sufficient
- The assessed scope introduces or changes domain concepts, business rules, lifecycle, relationships, API behavior, or integrations

## When not to use

- Product goals, users, or scope are unresolved → clarify and revise [`sdd-prd`](../sdd-prd/SKILL.md)
- The requested change is a detailed behavior slice with user cases, AC, and edge cases → [`sdd-spec`](../sdd-spec/SKILL.md)
- A technical implementation plan is needed after an approved spec → [`sdd-plan`](../sdd-plan/SKILL.md)

## Task

1. Read repository guidance, the parent PRD and its status, its input sources and open decisions, existing design/API contracts, glossary, relevant ADRs, and affected specs/code where they reveal compatibility constraints. If no PRD exists, or product direction is unresolved, return to `sdd-prd`; do not update canonical contracts from unresolved product intent.
2. State the assessment scope using the approved delivery batch or a subset of already approved PRD requirement IDs/sections. Exclude Deferred/Future candidates and unrelated phases. If the requested scope is not approved in the PRD, route to `sdd-prd` rather than designing the whole roadmap by default.
3. Build a coverage assessment for each requirement in scope. Use final dispositions: **covered by existing contract**, **covered by accepted new/updated contract**, **no shared contract impact**, or **open / blocked**. Cite the canonical document and section for covered outcomes; give a short rationale for no-impact outcomes.
4. Trace every proposed domain rule and API behavior to an approved PRD behavior or decision. If the PRD is Draft, label all findings exploratory; if traceability is missing, raise it rather than inventing a product requirement.
5. For requirements needing contract changes, define affected concepts and relationships, invariants, ownership, lifecycle/state transitions, permissions/trust boundaries, API resources and operations, external integrations/protocols and message semantics, errors, compatibility, and migration implications as relevant. Compare with existing contracts and ADRs; report conflicts and their impact rather than silently rewriting a shared contract or treating implementation behavior as product intent.
6. Keep domain design and API contract sections in their repository-defined canonical documents. Avoid duplicating rules; cross-reference the source of truth. Do not create empty documents. If no changes are needed, provide the scoped coverage assessment and conclude **Ready — no contract changes required for this scope**.
7. Set the gate status using these meanings: **Ready** means all scoped requirements have a final disposition, required contracts are accepted, and no blocking decision remains; **Blocked** means a specific product decision or contract gap prevents spec approval for part of this scope; **Pending** means assessment is still in progress; **Exploratory** means the parent PRD is Draft. Mark **Blocked** only for issues affecting this scope. Name the affected requirements/specs and route product decisions to `sdd-prd`; unrelated Ready scopes may proceed.
8. Use [template.md](references/template.md): the assessment scope, requirement coverage, and **API contract** sections are required. Group operations by business capability or resource boundary, not by HTTP method, screen, or user role. Declare shared API conventions once and reference the canonical API document. The API section must list affected operations or explicitly explain why there is no API impact. Keep device protocols such as MQTT in Integration and protocol contracts. Other design sections are conditional. Consult [example.md](references/example.md) when useful. Follow repository conventions.
9. Self-review that every requirement in the named scope has a final disposition, relevant contracts are accepted or cited, and terminology and traceability are consistent.

## Output

Produce a durable, scope-specific design-readiness assessment and, where needed, focused changes to canonical design/API contracts. State the assessed delivery batch or requirement IDs, coverage dispositions, affected documents, compatibility notes, and unresolved decisions. For an Approved PRD, record the gate result as **Ready** or **Blocked** in the repository. Ready requires final dispositions for all assessed requirements and acceptance of required contract changes by the contract owner. If the parent PRD is Draft, keep the result Exploratory; it cannot satisfy the gate. `Pending` is only for work not yet assessed. This is not an implementation plan.

## Quality checklist

- [ ] Assessment scope is explicit and limited to the approved delivery batch or selected requirements; deferred roadmap items are excluded
- [ ] Every requirement in the named scope has a final coverage disposition
- [ ] Domain and API coverage are assessed separately for every requirement
- [ ] The API contract section lists affected operations or explicitly explains why no API change is needed
- [ ] Every new domain rule and API behavior traces to an approved PRD decision; provisional decisions are clearly labeled
- [ ] Required contract changes are accepted by the contract owner before the gate is Ready
- [ ] A no-change result cites existing coverage or explains why no shared contract applies; no empty contract files were created
- [ ] Existing terminology and ADRs have been checked
- [ ] Ownership, invariants, and state transitions are explicit where applicable
- [ ] API resources, operations, inputs, outputs, and error behavior are unambiguous where applicable
- [ ] Integration, protocol, and message-delivery semantics are explicit where applicable
- [ ] Compatibility and data migration implications are surfaced where applicable
- [ ] Canonical document locations are respected and duplicate rules are cross-referenced
- [ ] Persisted assessment includes its update date and substantive history; canonical contracts follow repository versioning/history conventions
- [ ] Conflicts and unresolved decisions are visible, not guessed through

## References

- [Design/API coverage template](references/template.md)
- [Worked example](references/example.md)
