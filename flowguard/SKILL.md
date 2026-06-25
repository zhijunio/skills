---
name: flowguard
description: "Guard long, ambiguous, or stateful AI-agent work from drift. Use for multi-step implementation, compaction at phase boundaries, task contracts, handoff/resume, or work spanning many tool calls, files, or sessions — not single-shot article writing, topic learning, or codebase audit."
---

# Flowguard

Lifecycle guard for work that can **drift**, lose context, or compound errors. Routes first, verifies live state, runs bounded loops, leaves resumable handoff.

Does **not** replace the user's chosen workflow (writing, learning, audit, etc.) — only wraps execution discipline.

## Operating contract

- No long loop until route, scope, and stop conditions are explicit.
- Memory and handoffs are hints until git, files, runtime, or remote state are verified.
- No completion claims without fresh verification evidence.
- No scope expansion or destructive surfaces without re-route.

## Route first

| Route | Use when |
|-------|----------|
| `execute_direct` | Goal, constraints, done-when clear; small scope |
| `plan_first` | Many files/sessions, migrations, risky sequencing |
| `clarify_first` | Missing goal, files, constraints, or destructive permission |

Wrong assumption → large rewrite, prod risk, or data loss → `clarify_first`.

## Startup

1. Search for existing plans, artifacts, or prior work.
2. Load applicable `AGENTS.md` for editable paths.
3. Snapshot (from installed flowguard dir):

```bash
/path/to/flowguard/scripts/workflow_state_snapshot.sh /path/to/target/repo
```

4. Verify cwd, branch, dirty files if resuming — do not trust memory alone.
5. Capture goal, context, constraints, done-when; clarify if risky gaps.

## Preflight

State compact preflight (`references/state-contract.md` § Preflight). High-risk kickoff → expand with `references/task-contract.yaml`.

## Phase work (load on demand)

| Phase | Read |
|-------|------|
| Context compaction | `references/strategic-compact.md` |
| Prioritize findings / regression | `references/scoring-matrix.md`, `references/review-template.md` |
| Implement loop | `references/execution-loop.md` |
| Handoff / resume | `references/state-contract.md` |

## Validation

```bash
bash -n flowguard/scripts/workflow_state_snapshot.sh
```

## Resources

- `scripts/workflow_state_snapshot.sh` — read-only repo snapshot
- `references/state-contract.md` — preflight, checkpoint, handoff, resume
- `references/strategic-compact.md` — boundary compaction
- `references/execution-loop.md` — loop, verification, multi-agent
- `references/task-contract.yaml` — high-risk kickoff checklist
- `references/scoring-matrix.md` — finding priority
- `references/review-template.md` — regression template
