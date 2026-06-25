---
name: codebase-audit
description: Read-only codebase health audit (审) — architecture, code quality, security hygiene, tests/CI, dependencies, docs/operability. Severity-ranked findings and P0/P1/P2 roadmap. Use for 代码库审查, 代码审核, 架构审查, security/test/debt review; not article writing, topic learning, or URL fetch.
---

# Codebase Audit

**Role: 审** — Read-only assessment of **target codebase health** across multiple pillars, not content workflows or delivery runs.

**Produces:** boundary map (when applicable), verified findings by pillar, prioritized roadmap, optional `.codebase-audit/` baseline.

**Does not produce:** executor-ready implementation `plans/`, code edits, content articles, topic learning workspaces, URL fetch jobs, or step-by-step delivery commits in the target repo.

## Hard rules

1. **Never modify application source** in the target repo. Only write under `.codebase-audit/` when persisting baselines (user-approved or periodic re-audit).
2. **Read-only commands** — no installs, commits, or formatters on the target tree.
3. **Systemic findings first** — prefer patterns with structural, security, or maintainability impact. Isolated line bugs belong in a general advisor skill unless they expose a class of failure.
4. **Vet before report** — re-read cited code for every High+ finding; ADR/documented tradeoffs are by-design, not findings.
5. **No secret values** — `file:line` + credential type only.

## Audit pillars

| Pillar | Reference | Covers |
|--------|-----------|--------|
| **Architecture** | `references/lenses.md` | Boundaries, contracts, state, extension cost, testability of structure (L1–L6) |
| **Code health** | `references/code-health.md` | Complexity hotspots, error handling, dead code, consistency, observability hooks |
| **Security hygiene** | `references/code-health.md` § Security | Secrets, authZ gaps, injection surfaces, unsafe defaults (not full pentest) |
| **Tests & CI** | `references/code-health.md` § Tests | Coverage gaps at boundaries, missing gates, flaky patterns |
| **Deps & docs** | `references/code-health.md` § Deps & docs | Critical outdated deps, license flags when relevant; README/runbook/onboarding gaps |

User may narrow to one pillar (e.g.「只审安全」); state skipped pillars in the report.

## Effort level

User may say `snapshot`, `standard` (default), or `deep` anywhere in the invocation.

| | snapshot | standard | deep |
|---|----------|----------|------|
| Goal | Directionally healthy? Top risks? | Hotspot audit across pillars | Whole-repo audit across pillars |
| Architecture | Shape + boundaries + top risks | L1–L6 on churn hotspots | L1–L6 full scope |
| Code health | Top 3 maintainability/security signals | D1–D5 on hotspots | D1–D5 full scope |
| Parallelism | Single pass | ≤3 batches | ≤6 parallel read-only workers |
| Output | 1–2 pages | Full report | Full report + refuted appendix |

State what was **not** audited.

## Workflow

1. **Recon** — Read `references/recon.md`. Map stack, entrypoints, verification commands, ADRs/specs if present.
2. **Shape** — Classify project shape (library, CLI, API service, runtime, full-stack, …). Sketch boundary map when architecture pillar is in scope.
3. **Audit** — Run selected pillars: `references/lenses.md` + `references/code-health.md`. Deep mode: read `references/deep-parallel.md`.
4. **Vet** — Read `references/vet.md`; drop false positives; merge duplicate root causes.
5. **Report** — Fill `references/report-template.md`. Roadmap items are **systemic moves** (converge X, add contract tests, fix auth gap class), not a flat bug backlog.
6. **Baseline** (optional) — Persist `findings-YYYY-MM-DD.json` under `.codebase-audit/`; diff against prior run when present.

## Validation

Read-only audit — no target-repo edits except optional `.codebase-audit/` baseline.

Self-check before delivery:

- Every High+ finding re-read at cited `file:line`
- Report states skipped pillars and depth limits
- Roadmap items are systemic, not ticket-level bug list

## Scope boundaries

| Use this skill | Do not use |
|----------------|------------|
| Multi-pillar codebase health before refactor or onboarding | Quick dep/LOC checklist only |
| Architecture drift + security/test/debt patterns | General improvement `plans/` only |
| Pre-merge structural or hygiene review on unfamiliar repo | Known fix list + per-step delivery |

## References

- `references/recon.md` — intake checklist
- `references/lenses.md` — architecture lenses L1–L6
- `references/code-health.md` — code quality, security, tests, deps, docs dimensions
- `references/finding-format.md` — table columns and severity rubric
- `references/report-template.md` — deliverable shape
- `references/vet.md` — false-positive classes
- `references/deep-parallel.md` — parallel fan-out for deep mode
- `references/stack-signals.md` — stack-specific search hints
- `references/baseline-schema.json` — optional `.codebase-audit/` JSON shape
