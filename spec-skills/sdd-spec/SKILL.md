---
name: sdd-spec
description: Use after an Approved PRD has passed the required sdd-design readiness gate to decompose it into implementation-sized feature specs with user cases, testable acceptance criteria, edge cases, and verification. Exploratory drafts may use a Draft PRD but cannot be approved or planned. For multi-capability PRDs, first define a traceable breakdown. Not for unresolved product scope, shared contracts, implementation, trivial fixes, or fully specified direct edits.
---

# sdd-spec

## Role

You're a senior software engineer who decomposes a PRD into one or more **feature-slice specifications** before planning. First determine the independently specifiable user outcomes and their relationships; then define each slice's user-visible behavior, scope, and acceptance contract. Each spec refines upstream contracts; it does not replace them. Specs are the units that `sdd-plan` plans individually. An unapproved PRD can support exploratory drafts and breakdown discussion; a spec becomes an approved implementation contract only after the parent PRD is approved and the required `sdd-design` gate is Ready for its covered requirements.

Default: present the spec in chat. Persist it using the repository's established spec location and naming convention; when none exists, use `docs/sdd/00N-<topic>-spec.md` with the next available three-digit project number when the user confirms or repository convention requires it.

**Approval** is an explicit conversational confirm (confirm / yes / go / 批准 or equivalent). File presence alone is never approval.

For persisted specs, start at version `0.1`; set `1.0` at the first approval, then increment the minor version for each substantive, re-approved behavior or constraint change (`1.1`, `1.2`, ...). Clarification-only edits are recorded in the revision log without incrementing the version. Chat-only drafts do not need version metadata.

## Task

### PRD decomposition

For an Approved PRD, first verify a persisted **Ready** `sdd-design` assessment covering the approved delivery batch or selected subset of approved PRD requirements in scope. Do not require assessment of Deferred/Future roadmap candidates. If the requested scope is not approved in the PRD, return to [`sdd-prd`](../sdd-prd/SKILL.md); if its assessment is missing or Blocked, run or return to [`sdd-design`](../sdd-design/SKILL.md) before approving the breakdown or feature specs. A Draft PRD may support an explicitly exploratory breakdown without passing the gate.

When the request covers a whole PRD or multiple capabilities:

1. Identify user outcomes and product requirements that can be accepted and planned as coherent slices.
2. Propose a **spec breakdown map** with a stable ID/title, each candidate spec's goal, covered PRD sections/requirements, dependencies, and suggested order.
3. Check that every in-scope PRD requirement is covered, specs do not claim conflicting behavior, and shared domain/API questions remain in their canonical design documents.
4. Split by independently verifiable user outcomes and meaningful dependency boundaries; avoid one spec per PRD bullet or technical layer.
5. Present the map before drafting detailed specs. If the user requested the full set, draft each spec using the per-spec workflow below and keep every spec independently reviewable and approvable. Treat the map as a proposal; it does not approve the individual specs.

When the request already names one feature slice, skip the map unless its boundary or upstream coverage is unclear.

### New spec — [template.md](references/template.md), [example.md](references/example.md)

1. Read repository guidance and locate the parent PRD, its input sources and status, the persisted `sdd-design` coverage assessment, relevant domain/API contracts, glossary, ADRs, code facts, and prior decisions. If the PRD is absent, return to `sdd-prd`; do not draft a spec. If the PRD is Approved but the assessment does not cover the selected requirements or is not Ready, stop approval work and route to `sdd-design`. Do not block a scope because of unrelated deferred candidates. If the PRD is Draft, label any requested spec draft exploratory and trace each behavior to it; do not imply product approval. If an upstream contract conflicts with the requested behavior, surface the gap and route it upstream before inventing a rule.
2. Ask only for blocking product or contract decisions not discoverable from the repository. Keep non-blocking unknowns visible and bounded.
3. Define this slice's outcome, scope, and relevant user cases. Each case describes a user goal and system response, not implementation steps.
4. Consider only boundary and failure cases relevant to this slice, such as permission, retries, dependency failures, or lifecycle conflicts; capture their required outcomes in ACs.
5. Convert each behavior that must be delivered into stable **`AC-n`** criteria. Each AC must be independently verifiable with a clear pass/fail result. Include relevant failure and boundary outcomes as ACs; don't add a separate edge-case list when the cases are already clear in the ACs.
6. Link upstream contracts and add constraints or a verification note only when they resolve ambiguity not already covered by the ACs. Do not duplicate their full rules.
7. Keep the spec proportional: omit sections that add no decision or acceptance value. Self-review for unresolved blocking questions, vague ACs, unrepresented in-scope behavior, and scope outside the PRD.
8. **Present** the complete spec. Request approval only when the parent PRD is Approved and required shared contracts are sufficiently settled; otherwise present it as an exploratory draft and identify the product or contract decisions that must be resolved before approval. After explicit user approval, mark a persisted spec **Approved** and record the approval; until then it remains **Draft**.

### Revision — revise the existing spec in place (no `-v2` copy)

1. Edit the outcome, scope, ACs, and any affected constraints
2. Append **Revision log:** date, reason, changed AC IDs (or `none — clarification`), plan impact (`yes`/`no` + note)
3. Self-review (same checks as new)
4. **Clarification only** (wording/background; pass/fail unchanged) → log and stop — no re-approval
5. **AC or constraint change** → present for re-approval; after approval, increment the persisted spec's minor version
6. After re-approval: mark the persisted spec **Approved** and record the approval; re-check the plan's AC mapping and update it when coverage, implementation implications, slice order, or verification changed. Record when no plan change is needed, then return to the appropriate prior stage.

Examples: reword AC-2 without changing pass/fail → log only. AC-3 limit 200ms→500ms → re-approve.

## Present

**Locale (hard rule):** Present in the **user's language** — not English by default. Keep untranslated: `AC-n`, `file:line`, git literals.

**Approval Present** (not a Report Present). Sections:

- **Artifact** — Spec (new | revision) or Spec breakdown map; path when known
- **Body** — full spec text (template shape); not a summary
- **Open risks / deviations** — include only if material; resolve questions that block planning before Ask
- **Ask** — explicit Approval (or re-approval)

Literals: `AC-n`, `file:line`, git literals.

## Guidelines

### When to use

- New feature, bug fix, migration, or meaningful behavior change with sufficiently clear intent
- In-place revision when AC or constraints change during plan, build, or review

### Disambiguation

| Request | Route |
| --- | --- |
| Product goals or scope are too unclear to draft a meaningful slice | Clarify and revise [`sdd-prd`](../sdd-prd/SKILL.md) |
| Approved PRD has no Ready design assessment, or shared domain/API contracts are unresolved | [`sdd-design`](../sdd-design/SKILL.md) |
| One approved spec needs an implementation plan | [`sdd-plan`](../sdd-plan/SKILL.md) |
| Implementation | [`sdd-build`](../sdd-build/SKILL.md) — after the spec's approved plan |

### Stop

- New spec **approved with an approved parent PRD** → [`sdd-plan`](../sdd-plan/SKILL.md)
- Clarification-only revision → no stage change; continue prior work
- AC/constraint change re-approved → [`sdd-plan`](../sdd-plan/SKILL.md) if plan impact yes; else prior stage

### What NOT to do

Do not:

- Put implementation steps inside AC
- Drive Plan/Build from prose that has not been made into an `AC-n`
- Write a spec for trivial one-line fixes or fully specified direct edits
- Paste verbatim interview transcripts
- Leave open questions that block planning
- Treat file existence as user approval
- Approve a spec or start its implementation plan while its parent PRD remains Draft
- Approve a spec for an Approved PRD without a Ready `sdd-design` assessment covering it
- Create a new spec file instead of revising in place
- Skip re-approval after AC or constraint change

Help the team agree on observable user behavior, acceptance criteria, and boundary outcomes before planning and building.

## References

[template.md](references/template.md)
[example.md](references/example.md)
