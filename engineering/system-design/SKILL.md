---
name: system-design
description: Design or review a software system from approved product scope, architecture drivers and existing constraints. Use for system boundaries, component responsibilities, data ownership, integration, runtime and failure behavior, quality attributes and architecture tradeoffs; not stack selection, project initialization or feature implementation.
license: MIT
---

# System Design

Establish a reviewable system architecture and trace significant decisions to product needs, constraints and evidence. Adapted selectively from Magnus Hedemark's software-architecture workflow; see [source](SOURCE.md).

## Scope and inputs

Use for a new system, a target architecture or a material architectural change. For a small change within established boundaries, use the existing design rather than restarting architecture work. Read the authoritative product scope, applicable specifications, existing architecture and ADRs, repository instructions and observed implementation where relevant. Distinguish the current architecture from proposed changes. If scope is not approved, label the design exploratory and do not treat it as a committed baseline.

Own system shape, responsibilities, interfaces at the boundary level, data authority, runtime collaboration and architectural tradeoffs. Coordinate domain language and conceptual boundaries with domain-modeling; leave technology selection and initialization to project-foundation, detailed observable feature behavior to behavior-specification, and implementation tasks to the planning/implementation owners. A design recommendation is not authorization to write production code or initialize a project.

## Workflow

1. Establish the decision, scope, actors and external systems, constraints, stakeholders, change horizon and relevant unknowns. Separate observed facts, approved decisions, assumptions and proposals. Do not invent workload, regulatory requirements, team capacity or numerical targets.
2. Derive and prioritize architecture drivers and observable quality scenarios. Surface competing needs and unconfirmed thresholds. Use [drivers and tradeoffs](references/drivers-and-tradeoffs.md).
3. Compare materially different feasible structures against those drivers. Prefer the simplest shape that satisfies confirmed needs; do not default to microservices, distributed messaging or a particular framework. Preserve approved architecture unless change is justified.
4. Define system and component boundaries, responsibility, dependency direction, owned data and invariants. Describe integrations and contract ownership, without prematurely fixing detailed schemas or API payloads. Use [boundaries and runtime](references/boundaries-and-runtime.md).
5. Walk representative success and failure flows. Identify applicable consistency, concurrency, retry, duplication, authorization, isolation, recovery and operational concerns. Make assumptions explicit; recommend a scoped technical feasibility experiment when evidence cannot be established by analysis.
6. Describe the target design, alternatives, consequences, open decisions, verification approach and migration/coexistence risks where relevant. Use the [design brief](references/design-brief.md) and existing ADR conventions; do not create duplicate decision records.
7. Review against the drivers and product scope, resolve or record outstanding gaps, and request approval of material choices before treating the design as authoritative. Do not silently choose consequential tradeoffs for the user. An explicit approval of the actual design revision is sufficient.

## Deliverable and handoff

Use the requested artifact format and existing project paths. Default to a concise docs/system-design.md only when document creation is authorized and no established design document exists. Include only views necessary to answer the design question; label proposed and existing elements, and keep diagrams consistent with their text. Record significant decisions through existing ADR conventions when authorized.

Report the design path or review findings, decisions and approval state, relevant evidence and gaps, affected contracts, and recommended next owner. Use project-foundation for approved technical baseline work and applicable specialized skills for implementation. Do not select an unapproved stack, generate code, run experiments, deploy, modify the use-case index or claim production readiness merely because a design is reviewed.
