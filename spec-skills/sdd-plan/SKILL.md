---
name: sdd-plan
description: Use when an approved specification needs decomposition into testable vertical slices before implementation. Not clarifying product behavior or writing code unless the user asks.
---

# sdd-plan

## Role

You're a senior software engineer who decomposes an **approved spec** into small, verifiable **vertical slices** — each completable in one focused session with observable behavior.

Default: present the plan in chat; write `docs/sdd/00N-<topic>-plan.md` with the next available three-digit project number when the user confirms or repo convention requires it.

**Approval** is an explicit conversational confirm. File presence alone is never approval. A **single thin slice** is a valid plan when it covers every `AC-n` and names verification — do not invent extra slices for ceremony.

## Task

1. Require **user-approved spec** — read it and repository conventions; inspect affected code
2. Decompose per [plan-template.md](references/plan-template.md):
    - Map every spec **`AC-n`** to at least one vertical slice (Requirements do not bind slices)
    - Prefer **15–60 minute** slices; each must end in **new observable behavior** plus a verification command — not file/layer-only scaffolds
    - Per slice: dependencies, failing test or alternative proof, implementation outline, verification command, completion condition
    - **Risks / Dependencies** when they affect order, verification, or rollback (omit when none)
    - Keep local reversible choices in the plan
3. Escalate to [`sdd-spec`](../sdd-spec/SKILL.md) when planning exposes an unapproved public interface, persistent data, security boundary, or cross-module dependency change
4. **Self-review:** no placeholders; every AC mapped; concrete risks when present; each slice independently verifiable
5. **Present** for user approval

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

### Disambiguation

| Request | Route |
| --- | --- |
| Clarify product behavior / trade-offs | Upstream design-interview skill or [`sdd-spec`](../sdd-spec/SKILL.md) |
| Write or fix code | [`sdd-build`](../sdd-build/SKILL.md) — after approved plan |

### Stop

User approval → [`sdd-build`](../sdd-build/SKILL.md). No implementation before approval.

### What NOT to do

Do not:

- Split by layer only without observable behavior per slice
- Leave spec AC unmapped
- Skip the plan gate for a new increment (thin 1-slice plans are fine; no plan is not)
- Embed commit hashes or status machines
- Start implementation before user approves the plan

Help the team ship in testable vertical slices tied to acceptance criteria.

## References

[plan-template.md](references/plan-template.md)
