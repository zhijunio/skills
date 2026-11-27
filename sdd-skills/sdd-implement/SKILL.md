---
name: sdd-implement
description: Implement one approved spec with TDD, or make a narrow bug fix or behavior-preserving refactor. Split large specs into verifiable tasks and run independent tasks in parallel. Verify delivery and hand it to sdd-review.
---

# Implement and verify a change

## Small fixes and refactors

Use this path for a narrowly scoped fix that restores established behavior, or a refactor that preserves it. Identify the expected behavior from the applicable approved spec, tests, documented contract, or a confirmed defect description. If the expected behavior is unclear or the request changes it or a shared contract, return to [sdd-spec](../sdd-spec/SKILL.md). Route product scope changes to [sdd-prd](../sdd-prd/SKILL.md).

1. Reproduce a bug with a failing regression test when practical. For a refactor, first verify the behavior with passing tests and add a characterization check if the change needs one. Use rerunnable evidence where an automated test does not apply. For an unclear root cause or performance regression, use `diagnosing-bugs` when available to establish the cause and a reliable reproduction.
2. Make the smallest change that fits the repository conventions. Verify the delivered change and its affected behavior. A bug regression should now pass. A refactor should keep the behavior checks passing.
3. Hand the exact diff, expected behavior and its source, and verification results to [sdd-review](../sdd-review/SKILL.md). Keep the result in the task or conversation when there is no existing spec or issue for it. This path needs no fresh PRD, requirements, or spec and does not mark a use case complete.

## Approved specs

1. Read one Approved spec, its current Approved PRD, linked requirements and contracts, and repository guidance. Verify design readiness is Ready and covers the selected ACs. A UC ID must resolve to one approved spec. Route missing or stale specs and contract decisions to [sdd-spec](../sdd-spec/SKILL.md), and scope changes to [sdd-prd](../sdd-prd/SKILL.md).
2. Inspect the existing code, schema, and conventions. Design the affected tables, keys, constraints, indexes, migrations, internal interfaces, and component structure as needed. Refine local choices during TDD. Settle shared implementation choices before dispatching dependent tasks. Use the repository's existing artifacts for decisions that need to persist.
3. Map every `AC-n` to a task or integration check. Order prerequisites inside the spec. For external prerequisites, require current completion evidence and resolve outstanding [reconciliation](../sdd-requirements/SKILL.md#pending-reconciliation) first. Set the spec and affected UCs to `IN_PROGRESS` when their work starts. The primary agent owns these updates. Work sequentially for small or tightly coupled changes. For a large spec with independent tasks, use [parallel implementation](references/parallel-implementation.md). Keep the execution order in working context.
4. Use **RED → GREEN → REFACTOR** in each vertical slice, including delegated slices. Observe the expected failure through a public interface, make the smallest passing change, then simplify with tests green. Reuse passing evidence for behavior already implemented. Use rerunnable proof for work with no reasonable automated test. Follow repository and relevant stack rules, and reuse established UI components where they fit. If an approved user flow, AC, or contract needs to change, pause the affected task and route it to the owner in step 1.
5. Inspect and simplify the combined diff. Recheck current approval and scope, then deliver while preserving user changes. Run relevant checks on the actual delivered state. If the intended target is blocked or verification fails, report what remains and keep affected statuses `IN_PROGRESS`.
6. Recheck current approval before writing completion. Record the spec version, delivery target, [exact increment](../sdd-review/SKILL.md#increment-identifier), AC coverage, verification outcomes and deviations in the [spec](../sdd-spec/references/template.md). Mark it `COMPLETED` only when delivery and its checks pass. Update both UC status entries, and clear UC reconciliation only when evidence covers its current behavior across all relevant specs.

## Scope recheck

Compare the live PRD and spec approvals, affected UCs and prerequisites, and relevant contract decisions with the inputs used for implementation. Ignore updates to implementation tracking that leave the approved behavior intact. If approval lapses or applicable behavior changes, keep isolated work and pause the affected scope.

## Review handoff

Give [sdd-review](../sdd-review/SKILL.md) the approved spec, exact delivered diff, and verification results. Record the review outcome in the spec. Fix in-scope defects and review the revised increment. Reopen any status whose evidence the finding invalidates.
