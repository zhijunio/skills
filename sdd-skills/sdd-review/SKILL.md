---
name: sdd-review
description: Review a branch, pull request, or change range against repository standards and its originating spec when available. Use for incremental change review, not broad repository health scans.
---

# sdd-review

Review a change set and report actionable findings. Read-only by default. Use the user's language.
Reports are point-in-time outputs, not versioned contracts. Present them in chat by default; if the user asks to save one, follow the [SDD artifact path policy](../references/output-paths.md) and include its date and reviewed range.

## Review

1. Establish the range from the user's reference. If none is given, compare `HEAD` with the repository's default branch. State the actual range. Include uncommitted changes only when the user asks for a working-tree review.
2. Inspect the changed code and relevant repository guidance. Locate the originating issue, PRD, spec, and plan when available; use `AC-n` as the behavioral contract. If no spec is available, say so and review against repository standards only.
3. Look for correctness, security, regressions, verification gaps, maintainability problems introduced by the change, and mismatches with the spec. Use the shared [standards baseline](references/standards-baseline.md) when it helps. Load [smell baseline](references/smell-baseline.md) for deeper structural review.
4. Report only evidence-backed, actionable findings. Keep the report brief for small changes; broaden coverage for large, risky, or explicitly deep reviews. Parallel analysis is optional when the change has independent review areas and delegation is available.

## Output

```markdown
## Findings
- [severity] **Title** — impact and evidence (`path:line`). Include a concrete correction.

## Spec coverage
- <ACs covered, gaps, or "No spec available">

## Summary
- <Reviewed range, important checks or limitations, and overall assessment>
```

Use `high`, `medium`, and `low` for severity. A finding belongs in the report only when it is in scope, supported by evidence, and worth acting on. Omit empty sections where appropriate. Do not invent a pass verdict when the review was limited; state coverage and gaps plainly.

## Boundaries

- Review the requested range; do not turn pre-existing repository debt into a finding unless the change worsens it.
- Do not edit files or automatically start fixes, commits, pull requests, or merges.
- Use [`sdd-improve`](../sdd-improve/SKILL.md) for a broad repository or module scan; use [`sdd-build`](../sdd-build/SKILL.md) to fix accepted findings.
