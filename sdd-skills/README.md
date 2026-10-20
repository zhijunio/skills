# SDD skills

Use these skills to move from product framing to verified implementation. Each `SKILL.md` is the authoritative procedure; this index only helps choose the next skill.

## Choose a skill

| Need | Skill |
| --- | --- |
| Clarify the problem, evidence, alternatives, or key unknowns before scope | [`sdd-discover`](sdd-discover/SKILL.md) (optional) |
| Create or revise product goals, scope, priorities, and requirements | [`sdd-prd`](sdd-prd/SKILL.md) |
| Assess shared domain and API contract readiness after PRD approval | [`sdd-design`](sdd-design/SKILL.md) (required before approved specs) |
| Break approved product requirements into user-visible feature slices and acceptance criteria | [`sdd-spec`](sdd-spec/SKILL.md) |
| Map one approved spec into verifiable implementation slices, or prepare a joint spec-and-plan approval for a small feature | [`sdd-plan`](sdd-plan/SKILL.md) |
| Implement an approved spec and plan test-first, or fix listed review findings | [`sdd-build`](sdd-build/SKILL.md) |
| Review a branch, PR, or defined change range | [`sdd-review`](sdd-review/SKILL.md) |
| Scan a repository or area for systemic improvement opportunities | [`sdd-improve`](sdd-improve/SKILL.md) |
| Orchestrate the end-to-end workflow when explicitly requested | [`sdd-yolo`](sdd-yolo/SKILL.md) |

## Usual sequence

`sdd-discover` (when useful) → `sdd-prd` → `sdd-design` → `sdd-spec` → `sdd-plan` → `sdd-build`

`sdd-design` is a required readiness gate after PRD approval and before approving implementation specs. `sdd-review` and `sdd-improve` are separate, on-demand activities; neither is automatically chained after build.

## Approval boundaries

- Keep product scope in Draft until the user explicitly approves it.
- A Ready design result covers only its assessed, approved PRD requirements; unresolved decisions block only affected scope.
- Approve specs and plans explicitly before implementation. A small, explicitly requested feature may present a complete spec and plan together for one approval.
- `sdd-yolo` coordinates these stages but does not approve scope or artifacts for the user.

Follow each skill's procedure and the consuming repository's conventions. See [`references/output-paths.md`](references/output-paths.md) only when that repository has no artifact path convention.
