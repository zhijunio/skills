---
name: sdd-review
description: Review a delivered increment, branch, or PR against its approved spec or established behavior and repository standards. Report evidence-backed findings.
---

# Review an increment

1. Pin the diff from the user or the `sdd-implement` handoff. Include owned uncommitted and new files in a delivery handoff and exclude unrelated edits. For a standalone review with no range, compare HEAD with the default branch. Include workspace changes when requested. State the actual range.
2. Review the same diff from two perspectives:
   - **Behavior:** Match behavior and verification to the approved ACs. For a small fix or refactor without a spec, use the expected behavior and evidence in the handoff. Check missing behavior, regressions, and scope expansion. Report the limit if there is no behavioral baseline.
   - **Standards:** Check correctness, security, and the repository's coding standards. Use the [standards baseline](references/standards-baseline.md) and [code smells](references/smell-baseline.md) when warranted. Report problems introduced or worsened by the change.
3. Report actionable findings with severity (`high`, `medium`, `low`), `path:line`, evidence, impact, and a correction. Summarize behavior coverage and verification limits. For substantial reviews, the two perspectives can run in parallel. Keep this review read-only and route accepted fixes to [sdd-implement](../sdd-implement/SKILL.md).

For a workflow review, return the spec version or behavioral baseline, [increment identifier](#increment-identifier), outcome, and remaining findings. The caller records the result in the spec when applicable. Present the full report in chat by default. Use the [path policy](../references/output-paths.md) if the user asks to save it.

## Increment identifier

For committed work, record immutable base and head commits for the reviewed diff. For uncommitted work, use a digest of the scoped patch including the contents of owned new files. Exclude generated status and evidence entries. Use the same identifier in implementation and review records.
