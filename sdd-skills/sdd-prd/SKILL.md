---
name: sdd-prd
description: Use when an early product idea, stakeholder input, or existing product document needs to become or revise a product requirements document (PRD). Clarify evidence, assumptions, users, goals, prioritized requirements, dependencies, delivery batches, and open decisions; keep the PRD in Draft until the user explicitly approves its scope. Not for shared domain/API contracts, a feature-slice spec, or implementation planning.
---

# sdd-prd

## Role

Create or revise the durable product contract: the PRD. Keep one master PRD for a product as its needs and delivery batches evolve; a feature iteration or release batch is normally a revision or scoped section of that PRD, not a new PRD. Create a separate PRD only when the work has independently managed product goals, target users, success measures, and an approval lifecycle. It may start from an early idea and mature through clarification; do not require a separate requirements-specification document. Keep evidence, assumptions, proposals, and decisions distinguishable. Use the user's language.

Default: present a concise draft in chat. Persist or revise the repository PRD when requested, when the user approves it, or when repository conventions require it. An approved PRD must be durable before it is used by downstream design or specs. Keep product discovery focused; when the user wants extensive brainstorming, grilling, or interviews, use a suitable available skill and bring its findings back into the PRD.

When problem framing or evidence synthesis is a distinct task, [`sdd-discover`](../sdd-discover/SKILL.md) is an optional step before this skill. Its findings feed directly into the PRD; it does not create a mandatory intermediate document or approval gate. If the user already asks for a PRD or the product direction is clear enough, proceed here directly.

## When to use

- An early idea or stakeholder request needs to be clarified and expressed as product scope
- Inputs from brainstorming, interviews, existing product documents, support data, or analytics need to be consolidated
- An existing PRD needs revision after product goals, scope, or decisions change

## When not to use

- Shared domain rules or API behavior need a canonical contract → [`sdd-design`](../sdd-design/SKILL.md)
- One feature slice needs detailed user cases, acceptance criteria, edge cases, and verification → [`sdd-spec`](../sdd-spec/SKILL.md)
- An approved spec needs implementation decomposition → [`sdd-plan`](../sdd-plan/SKILL.md)

## Task

1. Read repository guidance and locate the existing PRD, related product documents, design/API contracts, glossary, ADRs, and specs. Treat documented capabilities as planned unless implementation evidence confirms they exist. Do not use implementation details to infer product intent. Reuse the repository's established PRD path and filename; otherwise follow the shared [SDD artifact path policy](../references/output-paths.md). Use a separate initiative slug only when the independent-PRD rule in Role applies.
2. Extract the available evidence. Distinguish user or business evidence from stakeholder opinions, repository facts, assumptions, and proposals. If a missing fact or decision would materially change the product direction, ask a focused question; otherwise record it as an open decision and continue with a Draft.
3. State the problem, target users and context, desired outcomes, success measures, and product approach. Do not invent users, evidence, metrics, or confirmed decisions.
4. Define product direction, current delivery scope when known, exclusions, deferred candidates, and product-level requirements. Maintain one requirements register as the source of truth: give each requirement a stable ID, observable statement, priority, decision status, relevant goal/story, dependency (or `None` / `TBD`), and source where useful. Record functional and non-functional requirements there or in clearly linked registers with the same fields. Use phases and delivery batches as planning views that reference requirement IDs; do not group the canonical requirement definitions under phases or duplicate their text. Distinguish true prerequisites from planned order, and do not infer a dependency from phase placement. Phase order is not a release commitment.
5. Add user stories, key flows, product-significant exceptions, non-functional requirements, integrations/data, dependencies, risks, assumptions, and open decisions only when they clarify a product decision. Keep domain rules and API details in their canonical documents; keep detailed feature acceptance criteria and exhaustive edge cases in `sdd-spec`.
6. Follow the `deliver-prd` section spine: Overview; Goals & Success Metrics; User Stories; Scope; Solution Design; Technical Considerations; Dependencies & Risks; Timeline & Milestones; Open Questions; Appendix. Include AI Behavior & Evaluation only when a model produces user-visible or otherwise consequential output; specify required behavior, refusal and abstention, evidence, fixed pass rules, evaluation-set coverage, run policy, held-out cases, and separate slice reporting. Omit it for deterministic features. Include the Agent Execution Contract only for a cold handoff that downstream specs/plans do not cover. Use [template.md](references/template.md) as a flexible outline, omit irrelevant sections, and avoid repeating scope summaries, requirements, and milestones. Consult [example.md](references/example.md) when useful.
7. Self-review that problem, users, goals, scope, requirement priorities and dependencies, delivery batches, evidence, and open decisions agree; planned behavior is not represented as implemented; each phase/batch references valid requirement IDs; links point to canonical design/API documents where they exist.
8. Present the PRD and request explicit approval of its product scope. Revise when the user requests changes. Keep status **Draft** until the user approves the PRD itself; record approver and date in the status field after approval.

## Output

Produce a product-level PRD. A PRD may be created and iterated as a Draft even when some scope details remain open; clearly label assumptions, proposed scope, and decisions. Only an explicitly **Approved** PRD authorizes accepted design/API contracts. After PRD approval, the `sdd-design` readiness gate is required before `sdd-spec` for the approved delivery batch or a selected subset of requirements already approved in the PRD; it does not pull deferred roadmap candidates into scope. A scope change returns to `sdd-prd` for revision and approval. Reuse existing contracts where sufficient and update canonical design/API contracts only where needed. A no-change result is valid and must not create empty documents. An approved spec requires a **Ready** design assessment covering its requirements. A Draft PRD may support discussion and exploratory artifacts, which must remain marked Draft and do not satisfy the gate.

When revising an Approved PRD, identify changed requirement IDs and decisions and trace them to existing readiness assessments, specs, plans, and any active build. Keep the last approved baseline in force until the revised scope is approved. Once approved, mark affected downstream artifacts as stale/reconciliation required; preserve their approval history but do not treat it as current authorization. Reassess readiness for changed requirements. Reconcile affected specs and reapprove them if their behavior, constraints, or coverage changes. Reapprove a plan only if its AC mapping, implementation approach, slice order, or verification changes. Pause affected builds until their governing artifacts are reconciled. Unaffected requirement scopes and artifacts remain valid.

For persisted PRDs, start at version `0.1`; increment the minor version for substantive Draft revisions, and use `1.0` for the first approved baseline. Increment the minor version for each later approved substantive revision (for example, `1.1`, `1.2`); keep typo-only edits out of the history. Record scope, decision, and approval changes in the revision history. Chat-only drafts do not need version metadata. Follow the shared [SDD artifact path policy](../references/output-paths.md), revise the master PRD as delivery batches change rather than creating a dated or per-iteration file, and apply the independent-PRD rule in Role.

## Quality checklist

- [ ] Product problem, target user, context, and desired outcome are explicit or marked unknown
- [ ] Evidence, assumptions, proposals, and decisions are distinguishable
- [ ] Goals have measures or explicitly state that baselines/targets are unknown
- [ ] Product direction, current release scope, exclusions, and deferred candidates are distinct
- [ ] Requirements have stable IDs, observable statements, priority, decision status, and explicit dependency/source information or a clear `None` / `TBD`
- [ ] Delivery batches and phases reference requirement IDs without duplicating requirement definitions
- [ ] Dependencies represent real prerequisites; phase order is not mistaken for dependency or delivery commitment
- [ ] User stories and key flows are included when they clarify outcomes
- [ ] Relevant exceptions, quality needs, dependencies, risks, assumptions, and open decisions are captured without duplicating detail
- [ ] Domain/API details point to canonical design documents
- [ ] Planned capability is not described as shipped implementation
- [ ] PRD status is Draft until explicit product-scope approval; approval records approver and date
- [ ] Revisions to an Approved PRD identify affected downstream readiness evidence, specs, plans, and active builds
- [ ] Persisted PRD has a version and concise change/approval history

## References

- [PRD template](references/template.md)
- [Worked example](references/example.md)
- [PM-Skills deliver-prd template](https://github.com/product-on-purpose/pm-skills/blob/main/skills/deliver-prd/references/TEMPLATE.md)
