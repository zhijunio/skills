---
name: implementation-planning
description: Plan a multi-step software change from approved requirements or behavior and applicable design constraints. Use for implementation plans, dependency-ordered tasks, file ownership, integration sequencing and verification mapping; not product scope, architecture decisions, HTML presentation or execution.
license: MIT
---

# Implementation Planning

Turn an agreed change into an actionable, reviewable plan with independently verifiable tasks. Adapted from Superpowers writing-plans; see [source](SOURCE.md).

## Choose the scope

Use for a material multi-step change or an explicit planning request. For a small, clear change, a short task outline is enough; do not impose a separate plan document. Read repository instructions, the authoritative behavior specification or agreed change, applicable PRD, approved architecture/ADRs, existing code, tests and technical baseline. Preserve canonical use-case IDs and established conventions. If approved behavior or a material design decision is missing, identify the blocker and route it to its owner; an exploratory plan must label assumptions and remain unapproved.

Own execution decomposition and ordering, not product intent, architecture or stack selection. Planning is not execution authorization. Do not modify production code, initialize projects, install dependencies, change use-case status, or commit merely because a plan is requested.

## Workflow

1. State the goal, in-scope outcomes, exclusions, applicable constraints and source revisions. Inspect affected entry points, contracts, files and existing checks; distinguish observed paths from proposed ones.
2. Identify the smallest coherent deliverables, shared prerequisites and dependency order. Use [task decomposition](references/task-decomposition.md). Prefer independently reviewable vertical changes when practical; use supporting prerequisite tasks only when they have a concrete deliverable. Do not invent implementation details to conceal unresolved decisions.
3. Define each task's inputs, outputs, ownership, dependencies, relevant interfaces, affected paths and observable completion checks. Specify planned test cases and commands using actual project tools when known. Mark unconfirmed commands as proposed, not executed. Coordinate verification scope with test strategy guidance where available.
4. Identify integration order, shared-file conflicts, compatibility, security, data migrations and rollback/recovery conditions where relevant. Mark parallel tasks only when dependencies and write ownership truly permit it. Do not assume parallel execution capability.
5. Review coverage against the approved behavior and constraints. Use [plan review](references/plan-review.md) to identify gaps, contradictory interfaces, cyclic dependencies, unverified assumptions and missing failure checks. Reconcile with the authoritative owner rather than changing scope silently.
6. Present the plan, outstanding decisions and evidence requirements. Request review of material choices; explicit approval of the actual plan is sufficient. Wait for execution authorization unless already explicitly supplied and applicable to that reviewed plan. Never turn approval of an unresolved proposal into authorization.

## Output and handoff

Use the requested format and existing project location. When a separate document is authorized and no convention exists, use docs/plans/<change-name>.md and the [plan outline](references/plan-outline.md). A plan may be Markdown, issue text or another reviewable format; do not automatically create tickets or remote artifacts. For an explicitly requested interactive HTML review, use html-plan as the presentation format without introducing a second authority for scope or execution tasks.

Report plan location or summary, dependencies, verification approach, outstanding decisions and review status. Hand approved single-use-case work to implement-use-case when explicitly requested, retaining its prerequisites and status lifecycle. Do not require a particular execution framework, TDD workflow, subagent strategy or commit cadence.
