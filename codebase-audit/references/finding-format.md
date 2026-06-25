# Finding Format

Every finding is one table row. Report body in **Chinese**; paths, identifiers, and code snippets stay literal.

```markdown
| severity | lens | title | files | evidence | evidence_type | confidence | impact | structural_fix |
```

## Severity (codebase rubric)

| Level | When |
|-------|------|
| **Critical** | Systematic contract corruption, exposed secrets on hot paths, auth bypass class, unbounded wrong user-visible output, dual sources of truth diverging in production |
| **High** | Clear boundary violation, silent contract drift, missing authZ on mutating routes, core untestable without full stack, critical dep CVE on execution path |
| **Medium** | Duplication slowing change, missing contract/boundary tests, observability gaps on failure paths, extension tax >2× reasonable |
| **Low** | Informational drift, minor layering smell, doc/code mismatch in non-critical paths, style debt without pattern |

Not for this skill: isolated formatting; single-line bug without systemic pattern unless part of a counted class.

## Columns

- **lens:** L1–L6 (`lenses.md`) or D1–D5 / Security / Tests / Deps (`code-health.md`) — or pillar tag
- **files:** comma-separated `path:line` (dedup key)
- **evidence:** ≤3 lines of code or call-path summary
- **evidence_type:** `observed` | `inferred` (inference chains >2 steps → confidence ≤ medium)
- **confidence:** high | medium | low
- **structural_fix:** converge to X, extract boundary Y, add contract test Z — not "fix typo"

Max **20** findings per report. Zero findings → `NO_FINDINGS` + patterns searched.

Escape `|` in cells as `\|`; use `<br>` for newlines inside cells.
