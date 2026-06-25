---
name: codebase-audit
description: Read-only codebase health audit — MECE pillars architecture (A1–A6), code (C0–C3), security (S1), verification (V1–V2), dependencies (D1), operations (O1). Chat-only P0/P1/P2 roadmap; effort snapshot, standard, or deep; variants simplicity, anti-pattern, branch, direction, cicd, and pillar-scoped review. Use for codebase audit, architecture review, security hygiene, CI/CD review, over-engineering and anti-pattern scan; not article writing, topic learning, implementation edits, or plans/.
---

# Codebase Audit

**Role:** Read-only **MECE** multi-pillar health assessment.

**Produces (chat only):** boundary map, findings (one pillar per row), P0/P1/P2 roadmap, optional direction notes, suggested next moves.

**Does not produce:** files in the target repo (unless user explicitly asks), `plans/`, code edits, articles, or delivery commits.

**Report locale:** Skill docs English; **report body in the user's language**. Literals: paths, `file:line`, lens ids, git refs, severity emojis per `report.md` (roadmap phases: text only).

## Hard rules

1. **Never modify the target repo** — read-only; no installs, commits, formatters; **report in chat only** (no `.codebase-audit/` or other audit files unless the user explicitly asks).
2. **Systemic first** — pattern-class findings.
3. **Vet before report** — re-read High+; ADRs are by-design unless contradicted.
4. **No secret values** — `file:line` + credential type only.
5. **MECE findings** — one lens per row; pillar boundaries in `references/map.md`.

## Six pillars

| Pillar | IDs | Checklist |
|--------|-----|-----------|
| Architecture | A1–A6 | `playbook.md` |
| Code | C0–C3 | `playbook.md` |
| Security | S1 | `playbook.md` |
| Verification | V1, V2 | `playbook.md` |
| Dependencies | D1 | `playbook.md` |
| Operations | O1 | `playbook.md` |

## Effort

| Level | Findings | Parallelism |
|-------|----------|-------------|
| `snapshot` | ≤5 | 1 pass |
| `standard` (default) | ≤20 | ≤3 batches |
| `deep` | ≤20 + vet appendix | ≤6 workers |

Chinese effort triggers (same limits): 快照 = snapshot · 标准 = standard · 深度 = deep.

## Invocation

### English

| Keyword | Lenses |
|---------|--------|
| `architecture` | A1–A6 |
| `security` | S1 |
| `tests` | V1 |
| `ci` | V2 |
| `deps` / `dx` | D1 |
| `ops` / `release` / `cd` | O1 |
| `cicd` | V2, O1 |
| `simplicity` / `over-engineering` | A5, A6, C1 |
| `anti-pattern` | A1, A5, A6, C1 |
| `branch` | same scope; tag `introduced` / `pre-existing` |
| `direction` | + optional direction notes |

### Chinese triggers (user phrases — no English words required)

| Trigger | Scope |
|---------|-------|
| 代码库审查 | all pillars (+ 快照/标准/深度 for effort) |
| 架构审查 / 架构 | A1–A6 |
| 安全审查 / 安全 | S1 |
| 测试审查 / 测试 | V1 |
| 持续集成审查 / 流水线审查 | V2 |
| 依赖审查 / 依赖 / 开发体验 | D1 |
| 运维审查 / 发布审查 / 部署审查 | O1 |
| 流水线与发布审查 / 集成与部署审查 | V2 + O1 |
| 简化审查 / 简化 / 过度设计 | A5, A6, C1 |
| 反模式 | A1, A5, A6, C1 |
| 分支审查 / 本分支 | same scope; attribution 本分支引入 / 既有问题 |
| 方向性建议 / 方向 | + optional direction notes |

When the user writes in Chinese, match Chinese triggers; do not require English keywords.

## Workflow

1. `map.md` — pillar routing  
2. `playbook.md` — Recon → invoked pillar sections → Stack signals  
3. `playbook.md` § Vet  
4. `report.md` — post full report in chat  
5. `deep-parallel.md` — `deep` / 深度 only

## Self-check

Valid lens: A1–A6, C0–C3, S1, V1–V2, D1, O1. Simplicity (简化审查) → A5, A6, C1. Anti-pattern (反模式) → A1, A5, A6, C1. One row per root cause. Skipped pillars stated.

## References

`references/map.md` · `references/playbook.md` · `references/report.md`
