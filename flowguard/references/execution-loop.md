# Execution Loop

Use after route and preflight are set. Handoff, loop stops, and verification evidence: `references/state-contract.md`.

## Step-test-update loop

1. Select one step with **owned files** and an expected check.
2. Announce the edit boundary before changing files.
3. Make the smallest useful change.
4. Run focused verification for that step when feasible.
5. Record a checkpoint: changed files, command results, decisions, blockers, next step.

## Stop and re-route

Stop when any condition in `state-contract.md` § Loop Guard fires, including:

- Same fix fails 3 times
- Scope expands beyond preflight
- Tool result conflicts with plan
- User sends a newer priority instruction
- Budget (tokens, time, cost) exceeded

Report: hypothesis, evidence, changed files, commands run, smallest next decision.

## Failure modes

| Mode | Response |
|------|----------|
| **Assumption drift** | Route was `execute_direct` but goal/constraints missing → `clarify_first` |
| **Summary-of-summary loss** | Handoff omits files or verification → rebuild from git/files |
| **Stale memory** | Memory conflicts with cwd/git/runtime → trust live evidence |
| **Silent tool failure** | Empty or partial tool output → blocker or rerun narrower check |
| **Parallel merge risk** | Two lanes need same writable file → one integration owner |

## Verification gate

Do not claim completion without **fresh** session evidence (`state-contract.md` § Verification Evidence).

Common stack defaults when repo has no explicit command:

| Stack | Before completion | Before submission |
|-------|-------------------|-------------------|
| Rust | `cargo check` | `cargo test` |
| TypeScript | `npx tsc --noEmit` | project test command |
| Go | `go build ./...` | `go test ./...` |
| Python | lint/type check if present | `pytest` |
| Java | `mvn -q -DskipTests compile` | `mvn test` / `./gradlew test` |

If a check cannot run, say why and report the nearest fallback that did run.

## Multi-agent

Parallel agents only when **file ownership is disjoint** and merge owner is named. Delegation must include: lane, writable paths, read-only context, expected artifact, verification owner, merge owner, stop conditions.

If two agents write the same file → do not run in parallel.

## Automation boundary

Manual workflow first. Read-only automation OK after repeated success: snapshots, handoff drafts, stale-worktree reports, verification summaries.

Require explicit user intent for: code edits, deploys, credential changes, PR merge, force push.
