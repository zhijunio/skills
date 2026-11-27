---
name: sdd-improve
description: Scan a repository, module, or area for evidence-backed improvement opportunities. Use for broad health reviews, not a specific change range.
---

# sdd-improve

Perform a read-only, hotspot-first scan and report a small set of useful improvement candidates. Use the user's language.
Reports are point-in-time outputs, not versioned contracts. Present them in chat by default; if the user asks to save one, follow the [SDD artifact path policy](../references/output-paths.md) and include its date and inspected scope.

## Process

1. Set scope from the user's request. If none is named, use recent churn and important entry points to choose a practical sample; state what was inspected and skipped.
2. Read relevant repository guidance and inspect for correctness, structural complexity, verification gaps, and documentation or contract drift. Prefer recurring or high-impact problems over isolated style preferences. Use [standards baseline](../sdd-review/references/standards-baseline.md) and [smell baseline](../sdd-review/references/smell-baseline.md) when they add useful coverage.
3. Present evidence-backed candidates with severity, impact, and a systemic correction. Prioritize by risk and leverage; do not treat suggestions as delivery gates.

## Output

```markdown
## Scope
- <Target, inspected areas, and limitations>

## Findings
1. **<Title>** · <high / medium / low>
   - Problem: <impact>
   - Evidence: <observed behavior or code, `path:line`>
   - Improvement: <systemic correction>

## First candidate
- <Which improvement to explore first and why>
```

Keep the list short; include strengths or a staged roadmap only when they help a decision. For an explicit over-engineering review, identify removable complexity and estimate the impact only when evidence supports it.

## Boundaries

- Stay read-only; do not edit, install, format, or fix while scanning.
- For a branch, pull request, or fixed change range, use [`sdd-review`](../sdd-review/SKILL.md).
- For product scope or shared design/API decisions, route to [`sdd-prd`](../sdd-prd/SKILL.md) or the design-readiness phase in [`sdd-spec`](../sdd-spec/SKILL.md).
