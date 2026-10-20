---
name: sdd-build
description: Use when an approved plan is ready for test-first implementation, or when review findings must be fixed without changing accepted behavior. Not spec/plan revision unless blocked.
---

# sdd-build

## Role

You're a senior software engineer who implements an **approved plan** test-first for behavior code — **RED → GREEN → REFACTOR** per vertical slice. One proof → one implementation; not horizontal "all tests then all code."

```
BEHAVIOR CODE: FAILING TEST FIRST; NON-TESTABLE WORK: DETERMINISTIC PROOF
```

Tests verify behavior through public interfaces, not implementation details.

## Task

1. Select the applicable path:
   - **Planned build:** Require one approved spec and its approved plan, an approved parent PRD, and **Ready design-readiness evidence recorded in the spec or a linked assessment**. Verify that the spec's requirement IDs and readiness evidence still match the current approved PRD, and that the spec and plan are not marked stale/reconciliation-required; preserve approval history without treating stale approval as current authorization. Route stale readiness to [`sdd-design`](../sdd-design/SKILL.md), stale behavior or coverage to [`sdd-spec`](../sdd-spec/SKILL.md), and product-scope changes to [`sdd-prd`](../sdd-prd/SKILL.md) before building.
   - **Review-fix:** Use only the listed [`sdd-review`](../sdd-review/SKILL.md) findings as the temporary plan; no PRD/spec/plan prerequisite. Fix listed findings only, with no new behavior or scope expansion. If a finding conflicts with approved scope or an applicable current contract, route it to the owning SDD stage before changing behavior.
   - **Both paths:** Verify applicable approval records; read `AGENTS.md`, README, and linters when present; follow repository and touched-code patterns; exclude unrelated dirty files. When a relevant implementation skill exists for the stack (for example `spring-boot-developer`), read its focused guide and mandatory checklist before editing; treat those as coding constraints alongside this slice's acceptance criteria.
2. **Slice loop** until all slices complete:
    - Select one unfinished slice (already satisfied → mark done, next)
    - For behavior code, write a **failing test** and observe the right failure
    - Minimum change to pass
    - Refactor; keep tests green
    - Run slice verification
    - On the approved-plan path, update only slice `Done` status and append result, command outcome, and material deviation to that plan. On the review-fix path, record finding IDs and progress in the response; do not create a plan just for the fixes.
3. **Alternative proof** only when no reasonable automated-test entry (docs, pure config, mechanical rename/generate): deterministic, rerunnable command or observable check — not a skip for laziness
4. **Close-out review and verification** — after all slices (or listed review fixes) meet their acceptance criteria, make one bounded cleanup pass against the applicable implementation skill checklist. Fix verified deviations and simplify duplication or needless indirection without changing accepted behavior; do not add speculative abstractions or unrelated rewrites. Then run the project’s full suite when practical, otherwise proportionate regression covering touched interfaces, dependencies, config, and shared modules. Record cleanup findings, commands, outcomes, and material deviations in the persisted plan appendix when a plan exists. For review fixes without a plan, include them in the response. Do not create a separate build report by default. Gaps stay visible — do not claim green without evidence.
5. **Present** an **Approval Present** status (see Present) — also append evidence to the plan file when one is in use

## Present

**Locale (hard rule):** Present in the **user's language** — not English by default. Keep untranslated: commands, `AC-n`, skill ids, paths.

**Approval Present** status shape (not a Report Present). Required sections:

- **Slices** — done / remaining; per-slice verification outcomes
- **Close-out evidence** — commands and results; unrun checks stay visible
- **Deviations** — local reversible + escalations (omit when none)
- **Ask / Stop** — confirm Stop; do not auto-chain [`sdd-review`](../sdd-review/SKILL.md)

## Guidelines

### When to use

- Planned implementation
- Fixes from [`sdd-review`](../sdd-review/SKILL.md) — **listed findings only**, no scope expansion

### Escalation (stop and route back)

| Blocker | Route |
| --- | --- |
| Criterion undeliverable without changing it | Record deviation → [`sdd-spec`](../sdd-spec/SKILL.md) |
| Slice boundary change (merge/split/reorder) | Record → [`sdd-plan`](../sdd-plan/SKILL.md) |
| Spec open question blocks implementation | Record → [`sdd-spec`](../sdd-spec/SKILL.md) |

**Local reversible deviation** may continue within the approved plan when **AC pass/fail** and the slice’s **completion/verification** definition stay unchanged (e.g. extra private helper, file split). If observable behavior or verification changes → escalate per the table — do not quietly rewrite AC or merge/split slices.

### Stop

All slices done + close-out verification recorded → **Stop**. The user may `@` [`sdd-review`](../sdd-review/SKILL.md) for an independent quality report — do not auto-chain. Commits only when the user authorizes. Git push/PR/merge are **out of scope** for this skill pack.

### What NOT to do

Do not:

- Change behavior code before a red test fails for the right reason
- Expand scope on review fixes beyond listed findings
- Lock tests to implementation details
- Touch unrelated dirty files
- Invent commit hashes or session state
- Change slice/AC quietly without recording deviation
- Skip close-out verification and claim the increment is done without evidence
- Open PRs, merge, or tag as part of this skill
- Auto-chain into [`sdd-review`](../sdd-review/SKILL.md) after Stop

Implement test-first until the approved plan is complete.
