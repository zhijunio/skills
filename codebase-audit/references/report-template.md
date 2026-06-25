# Codebase Audit Report Template

```markdown
# Codebase Audit — {Project Name}

> Date: YYYY-MM-DD
> Target: {path}
> Shape: {classification}
> Effort: snapshot | standard | deep
> Stack: {detected stack}
> Pillars run: Architecture (L1–L6), Code health (D1–D5) — list any skipped

## Executive summary

2–4 sentences: directional verdict (healthy / drifting / at risk), top cross-pillar risks, recommended next move.

## Boundary map (when architecture pillar ran)

```text
[layer] → modules/packages → notes (ownership, known debt)
```

## Findings

{finding table per finding-format.md — tag pillar/lens per row}

## Roadmap

| Phase | Scope | Est. touch surface | Depends on |
|-------|-------|-------------------|------------|
| P0 | … | ~N modules | — |
| P1 | … | … | P0 |
| P2 | … | … | … |

Roadmap items are **systemic moves** (converge registries, add contract tests, fix auth gap class, improve CI gate), not a flat bug backlog.

## 已排除项 (vet rejects)

| title | reason |

## Baseline

- New: `.codebase-audit/findings-YYYY-MM-DD.json`
- Diff vs prior: {new | fixed | still_open counts} or N/A

## Not audited

{explicit gaps — dirs, pillars, depth limits}
```

Deliver in **Chinese** unless the user asked for English. Finding table columns: `finding-format.md`.
