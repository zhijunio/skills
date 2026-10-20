---
name: sdd-plan
description: Use when one approved feature spec, with an approved parent PRD, needs an ordered implementation plan that maps every AC to verifiable vertical slices, or when an explicitly requested small feature needs a joint spec-and-plan approval. Not for decomposing a PRD into specs, clarifying product behavior, or writing code.
---

# sdd-plan

## Role

You're a senior software engineer who decomposes **one approved spec** into an ordered sequence of small, verifiable **implementation slices** that deliver observable behavior. One plan covers one spec; a multi-spec initiative has a separate plan for each spec. Exception: for an explicitly requested small-feature package, you may plan a complete Draft spec so the spec and plan can be presented for one joint approval.

Default: present a draft plan in chat. Persist a plan when requested, when the user approves it, or when repository conventions require it. An approved plan must be durable before `sdd-build`. Follow the repository's established location and naming convention or, if none exists, the [SDD artifact path policy](../references/output-paths.md).

**Approval** is an explicit conversational confirm. File presence alone is never approval. After confirmation, mark a persisted plan **Approved** and record the approver/date. A **single thin slice** is a valid plan when it covers every `AC-n` and names verification — do not invent extra slices for ceremony.

When revising an already Approved plan, record the reason and affected slices, present the revised plan, and get approval again before build continues. Plans are execution records and do not need semantic version numbers; preserve dated approval and implementation evidence in the plan.

## Task

1. Require one feature spec whose parent PRD is Approved and whose covered requirements have a **Ready design-readiness result** recorded in the spec or linked assessment. Normally the spec is already user-approved. For an explicitly requested small-feature package, a complete Draft spec may be planned for joint presentation and approval with the plan. Verify the PRD, readiness evidence, and applicable approval state; then read repository conventions, inspect affected code, and identify unresolved requirements, dependencies, and implementation constraints. If a gate is missing, route back to the relevant SDD stage. If the input contains multiple specs, plan them separately and preserve only real cross-spec dependencies.
2. Order slices by prerequisite and user-visible progress, then decompose per [template.md](references/template.md); consult [example.md](references/example.md) when useful:
    - Map every spec **`AC-n`** to at least one vertical slice (Requirements do not bind slices)
    - Keep slices small enough to complete and verify independently; split a slice when it bundles unrelated outcomes, not to meet a time estimate
    - Per slice: dependencies, failing test or alternative proof, implementation outline, verification command, completion condition
    - Include boundary/failure behavior already required by AC in the relevant slice; do not add new product behavior
    - **Risks / Dependencies** when they affect order, verification, or rollback (omit when none)
    - Keep local reversible choices in the plan
3. Escalate to the matching contract stage when planning exposes an unresolved decision: product goals/scope → [`sdd-prd`](../sdd-prd/SKILL.md); shared domain rules or API behavior → [`sdd-design`](../sdd-design/SKILL.md); slice-specific user behavior or acceptance boundaries → [`sdd-spec`](../sdd-spec/SKILL.md)
4. **Self-review:** no placeholders; every AC mapped; concrete risks when present; each slice independently verifiable
5. **Present** for user approval; after approval, persist the plan, mark it Approved, and record who approved it and when. In the joint small-feature flow, present the spec and plan together and, after approval, persist and mark both Approved. Use this only when the plan introduces no product behavior and every AC maps to a verifiable slice.

### Optional parallel analysis

Use sub-agents when the spec has **at least two substantial, independent planning questions** that can be investigated at the same time and the answers are likely to affect slice order, verification, dependencies, or risk. Typical cases include a spec spanning separate code areas, several external integrations, or distinct security and data-migration concerns. Keep the investigation proportional: one focused question per agent, bounded by named requirements or ACs, and request concrete repository evidence and implications for the plan.

Do not fan out for a small or cohesive spec, questions that depend on one another, routine repository lookups, or work where delegation would take longer to coordinate than to investigate directly. Do not ask agents to produce competing complete plans or delegate implementation. The planning agent owns the final plan, reconciles findings, checks conflicts, and ensures every `AC-n` maps to a verifiable slice. If delegation is unavailable, investigate the questions directly.

## Present

**Locale (hard rule):** Present in the **user's language** — not English by default. Keep untranslated: `AC-n`, verification commands.

**Approval Present** (not a Report Present). Sections:

- **Artifact** — Plan; linked Spec path when known
- **Body** — full plan text (template shape); not a summary
- **Open risks / deviations** — omit when none (build-affecting risks may live in Body)
- **Ask** — explicit Approval

Literals: `AC-n`, verification commands.

## Guidelines

### When to use

- After user-approved spec, before implementation
- For an explicitly requested small-feature package, to prepare a plan from a complete Draft spec for joint spec-and-plan approval

### Disambiguation

| Request | Route |
| --- | --- |
| Decompose an approved PRD into feature specs | [`sdd-spec`](../sdd-spec/SKILL.md) |
| Clarify product goals or scope | [`sdd-prd`](../sdd-prd/SKILL.md) |
| Clarify shared domain rules or API behavior | [`sdd-design`](../sdd-design/SKILL.md) |
| Clarify a feature slice's user behavior or acceptance boundary | [`sdd-spec`](../sdd-spec/SKILL.md) |
| Write or fix code | [`sdd-build`](../sdd-build/SKILL.md) — after approved plan |

### Stop

Approved spec and plan → [`sdd-build`](../sdd-build/SKILL.md). In the joint small-feature flow, the single approval makes both artifacts Approved. No implementation starts before the plan is approved.

### What NOT to do

Do not:

- Split by layer only without observable behavior per slice
- Leave spec AC unmapped
- Skip the plan gate for a new increment (thin 1-slice plans are fine; no plan is not)
- Embed commit hashes or status machines
- Start implementation before user approves the plan

Help the team ship in testable vertical slices tied to acceptance criteria.

## References

- [Plan template](references/template.md)
- [Worked example](references/example.md)
