# Parallel implementation

Use this procedure for one approved spec with enough independent work to justify separate worktrees. Keep the task graph in the working context and report progress against it. The spec remains the record for acceptance criteria and final verification.

## Split and schedule

1. List each task's ID, observable result, covered `AC-n` IDs, direct blockers, owned files or modules, and verification command or proof. Prefer vertical slices with a complete, verifiable outcome. Assign cross-slice criteria to an integration check. Check that every AC has a verification owner and the dependencies have no cycle.
2. Give shared files and decisions a single owner. Land shared prerequisites such as an API contract, database migration, or test fixture before starting dependent tasks. Identify shared test resources such as ports, databases, containers, and output directories. Give concurrent workers isolated resources or run their conflicting checks sequentially. Combine or sequence tasks that still share writes or cannot be verified independently.
3. Pin the target and its base revision. Create a local integration branch in its own worktree, then one local branch and worktree per worker from the latest verified integration revision. Keep existing user changes in place and check whether the work needs any of them. If a clean isolated baseline cannot represent the needed changes, or the environment cannot provide isolated workers, use sequential TDD in the current workspace.
4. Dispatch one fresh implementation subagent per ready task, up to available capacity. A task is ready when its blockers have been integrated and verified. Give the worker its worktree, base revision, ownership, approved spec and ACs, relevant repository rules, and verification commands. Supply a read-only copy of approved inputs when they are outside the worker's checkout. Workers stay in their own worktrees and return their patch or local commit, AC evidence, and any blocker. The primary agent owns the integration branch, spec, and requirements statuses.

## Integrate and verify

1. Before dispatching another batch or integrating returned work, run the [scope recheck](../SKILL.md#scope-recheck) on the live inputs. The canonical documents govern what may proceed.
2. Integrate one task at a time. Check its diff against its ownership and verify the candidate against the latest verified integration revision. Advance the integration branch only when the checks pass. Keep failed candidates isolated while fixing or requeuing them. Their dependents wait, and other ready tasks can continue from the verified revision.
3. After integrating all tasks, return to the main skill for cleanup and verification on the actual delivered state. Preserve the integration branch and report pending delivery if applying the change would overwrite user work.
4. Remove only task-created worktrees and branches once their changes are delivered or safely retained. Keep any work needed to resume. Publishing branches or opening a PR requires user authorization.

For example, once a shared access rule and its test fixture are integrated, two independent feature tasks can use them in parallel. A cross-feature check that exercises both features becomes ready after both tasks have passed integration checks.
