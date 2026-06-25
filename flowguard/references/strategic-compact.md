# Strategic Context Compaction

Compress context at **logical phase boundaries** — not mid-implementation.

## When to compact

| Transition | Compact? |
|------------|----------|
| explore → plan | Yes |
| plan → implement | Yes |
| task A → task B | Yes |
| mid-file edit / mid-test fix | **No** |
| user sent a correction | Checkpoint first; compact only at next boundary |

## Keep

- Active goal, constraints, done-when (or preflight block)
- Modified files list (paths)
- Verification commands + **fresh** results from this session
- Key decisions and blockers
- Current route (`execute_direct` / `plan_first` / `clarify_first`)
- Next single step with owned files

## Discard

- Verbose tool output already reflected in decisions
- Explored-but-rejected paths (keep one-line reason if costly to re-derive)
- Duplicate summaries of summaries
- Old handoff fields superseded by current git/file state

## Before compacting

1. Write a **checkpoint** (`references/state-contract.md` § Checkpoint).
2. Run focused verification if the phase claimed a done-state.
3. State what the **next phase** owns.

## After compacting

Resume using `references/state-contract.md` § Resume Checklist — verify handoff against live repo state before editing.
