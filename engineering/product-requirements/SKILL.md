---
name: product-requirements
description: Discover, define, review, approve and revise product requirements for a complete product, MVP, release or capability increment. Use for product scope, users, journeys, capability boundaries, priorities, quality needs and PRDs; not detailed use-case behavior, technical architecture or implementation.
---

# Product Requirements

Establish a reviewable product contract from grounded needs and explicit decisions. Use the user's language, existing evidence and established project conventions.

## Select the task

- Discovery: explore an uncertain product idea, problem or opportunity.
- Definition: establish the scope of a complete product, MVP, release or capability increment.
- Review: inspect an existing PRD for ambiguity, gaps and conflicting decisions.
- Approval or change: record an explicit approval or revise an existing product contract.

Do not force MVP scope when the user wants a complete product. Do not restart discovery when a supplied PRD or confirmed decisions already answer the relevant questions. Use a lightweight response for a bounded request; create or revise files only within authorized scope.

## Responsibility and handoff

Own product intent, users, capabilities, scope, product-level quality needs, priorities and the PRD. Use grilling for a requested stress-test, domain-modeling for canonical terminology, prototype for bounded UI or behavior exploration, and behavior-specification for detailed feature behavior. A prototype is decision evidence, not scope approval. Route technical tradeoffs and feasibility to the appropriate design owner. Do not automatically invoke other skills, choose architecture or implement features.

## Establish product scope

Read the existing PRD, project charter, decision records and relevant user-provided context before asking questions. Separate confirmed decisions, evidence, proposals, assumptions and unknowns. Load [requirements analysis](references/requirements-analysis.md) when defining a broad product, resolving scope conflicts or reviewing complex requirements.

1. Ground the problem, users, stakeholder needs, existing workflows and desired outcomes. Treat unsupported personas or demand as hypotheses.
2. Research direct and adjacent products or open-source alternatives when this can change a decision. Prefer primary documentation and repositories; check license, activity, relevant releases and fit before suggesting reuse. Distinguish observed capabilities from inference. If tools are unavailable or research is unnecessary, record the limitation rather than inventing findings.
3. Compare meaningful approaches, including reuse or extending an existing product where viable. Explain tradeoffs in user value, effort and maintenance without treating an estimate as a fact.
4. Define the target product boundary and the current delivery boundary separately. Identify capabilities, primary journeys, role responsibilities, exclusions, dependencies and deferred scope. Ask about priorities and meaningful constraints; do not assign business priorities without authority.
5. Establish product-level quality needs and observable outcomes. Identify applicable security, accessibility, reliability, compatibility, privacy and operational constraints without designing their implementation or fabricating numerical targets.
6. Resolve material ambiguities. Ask related questions together when useful, reuse answers, label recommendations and preserve unresolved decisions. Stop asking once there is sufficient clarity for the requested deliverable.

## Draft or revise the PRD

Summarize the decisions and request confirmation before drafting a new PRD, unless the user has already explicitly authorized drafting from established scope. Confirmation of the summary is not approval of the resulting PRD. A request for a preliminary draft may leave material questions open; label it as such.

Use the user's requested format or existing schema. For a new default PRD, use [the PRD outline](references/prd-outline.md). Scale the sections to the scope; do not force a full-product document for one capability. Honor explicit paths, otherwise use the established PRD path or docs/PRD.md. If both root PRD.md and docs/PRD.md exist and authority is unclear, ask before creating or overwriting a file.

Before finishing, check that current scope, target scope, deferred items and exclusions are distinguishable; every material capability has a stated purpose and relevant actor or consumer; priorities have a stated basis; quality needs and success measures have known sources or labeled gaps; and no unapproved proposal has become a commitment. Do not generate use-case IDs, detailed specifications, architecture or implementation plans here.

Report the actual artifact path (if written), revision, approval state, material open decisions and recommended next review.

## Review, approval and change

When asked to review, approve or revise an existing PRD, read the authoritative file first. Identify the exact revision and scope under review, including material unresolved decisions and exclusions. Ask only about unresolved decisions affecting approval. Explicit approval of that exact revision is sufficient; confirmation of a discussion summary is not.

For documents using the default metadata convention, record status: approved and approval: approved only after explicit product owner approval of the actual document. A material scope revision becomes status: draft, approval: pending until explicitly approved again. Preserve alternative lifecycle conventions and legacy approved documents; do not force metadata migration. Preserve or identify the prior approved revision and scope in existing version history or an agreed decision record. Nonmaterial corrections do not automatically invalidate approval.

Assess affected use cases, specifications, plans and evidence for material changes. Do not silently overwrite an approved baseline or downstream index, reset implementation statuses or redefine requirements to match a defect. If unresolved decisions affect the proposed scope, keep it pending or explicitly exclude those decisions from the approved scope. Report approval evidence and revision. Hand off to prd-to-use-cases when requested and authoritative scope is established. Approval is not authorization to implement or publish.
