---
name: sdd-improve
description: >
  Broad-scope Standards scan for improvement candidates. Use when the user wants a repo, module, area, or branch health/improve pass — hotspot-first, four-dimension report, pick a candidate next. Not increment delivery review.
---

# sdd-improve

Independent Standards-only **improve pass** over a broad scope (Matt Pocock `improve-codebase-architecture` rhythm + shared four-dimension Standards). **No HTML** — Markdown in chat. Not part of the delivery loop; usable alone.

- **Standards** — Correctness · Structure (Fowler smells) · Verification · Traceability — see [`standards-baseline.md`](../sdd-review/references/standards-baseline.md) and [`smell-baseline.md`](../sdd-review/references/smell-baseline.md) (same content as [`sdd-review`](../sdd-review/SKILL.md) Standards; different scope)
- **Structural simplification lens** — apply the shared code-judo, spaghetti-growth, direct-boundary, canonical-ownership, orchestration, and decomposition checks when the scope shows meaningful complexity
- **Scope** — repo / module / area / branch, not `fixed-point...HEAD`
- **No Spec axis** — do not judge Spec/AC compliance here

Read-only: no file edits, installs, formatters, commits, or fixes.

## When

Use for a broad repo, module, area, or branch health scan that produces improvement candidates.

## Skip

Use [`sdd-review`](../sdd-review/SKILL.md) for an incremental fixed-point, branch, or PR review. This skill does not implement fixes or act as a delivery gate.

## Process

### 1. Scope before you scan (YAGNI / hotspot-first)

- If the user named a module, subsystem, path, or pain point — take it.
- Else walk recent history (`git log --oneline` / churn hotspots) and prefer those areas. Scattered history → widen the net.
- If the request is clearly an increment diff quality report (branch/PR/since X) → [`sdd-review`](../sdd-review/SKILL.md).

Capture: target path/scope, branch/`HEAD`, effort `snapshot` | `standard` | `deep` (default `standard`).

Large scope: triage stack, entrypoints, manifests, CI, docs, public surfaces; state sampled vs skipped in Coverage.

### 2. Standards sources

Repo docs (`AGENTS.md`, README, CONTRIBUTING, CODING_STANDARDS, CI, …) + the shared baselines above. Repo overrides; baseline/smells are judgement calls; skip tooling-enforced style.

Prefer **systemic** Structure/smell hits (repeat across a module or boundary). Do not flood the report with one-off Mysterious Name nits.

For Structure, search for hotspots where a simpler model could delete branches, flags, helper layers, or state; ad-hoc conditionals and feature leakage; thin or magical abstractions; unclear type boundaries; duplicate non-canonical helpers; avoidable serialized orchestration; partial-update shapes; and cohesive files/modules growing without a clear ownership boundary. Treat a 1,000-line boundary as a decomposition prompt, not an automatic finding. Prefer systemic evidence and report leverage rather than demanding an ideal refactor.

When a Structure finding is an over-engineering **cut**, label it with one of these tags:

- `delete:` — dead / speculative; replacement nothing
- `stdlib:` — hand-rolled; name the stdlib equivalent
- `native:` — platform already does it; name the feature
- `yagni:` — one impl / unused config / single caller layer
- `shrink:` — same logic, fewer lines

Do not collapse the whole report into cut-tags only — Correctness / Verification / Traceability stay normal cards.

### 3. Collect candidates

Inspect for friction and drift along the four dimensions. Classify **follow-up priority** (not a merge/delivery gate):

| Priority | When |
| --- | --- |
| **🚨 Critical** | Systematic contract break, secret/auth class, source-of-truth divergence blocking safe work |
| **🔴 High** | Boundary / verification / security-ops risk with broad impact |
| **🟡 Medium** | Duplication tax, test/doc drift, local Structure debt |
| **🟢 Low** | Minor drift or mild smell |

Never use these (or P0/P1/P2) to block merge. Do not treat them as increment-review delivery groups (must-fix / should-fix / suggestion).

Optional read-only commands when they cut uncertainty; report evidence, not a substitute for the scan.

### 4. Present (Markdown)

**Locale (hard rule):** Present in the **user's language** — not English by default. Keep untranslated: paths, `file:line`, skill ids, severity emojis, cut-tags.

**Report Present.** Literals: paths, `file:line`, refs, skill ids, severity emojis.

**Required** sections:

- **Scope** — target, effort, hotspot rationale, standards sources
- **Coverage** — dimensions examined, commands, sampled/skipped areas
- **Standards** — **one subsection per dimension** (Correctness / Structure / Verification / Traceability); within each, severity groups + numbered **candidate cards**
- **Top recommendation** — which candidate to tackle first and why (priority × leverage)

**Optional** (omit by default; include only when evidence warrants):

- **Strengths** — evidence-backed systemic positives
- **Largest Structure cut** — biggest over-engineering cut if any (may differ from Top)
- **`net` (Structure cuts)** — `net: -<N> lines, -<M> deps possible` across tagged cuts only
- **Roadmap** — P0/P1/P2 when useful

No **Suggested next steps** — Stop / Ask is enough.

Candidate card:

```markdown
**1. Short title** · Dimension · Priority
- **Problem:** why this hurts (friction / drift)
- **Evidence:** observed evidence · `path:line`
- **Fix:** systemic move (not a typo patch)
- **Cut-tag:** optional — `delete` | `stdlib` | `native` | `yagni` | `shrink` (Structure over-engineering only)
- **Meta:** optional effort, risk, ADR conflict note
```

**Stop.** Ask: *Which of these would you like to explore?* (`snapshot` may skip the question and just list Top / optional Largest cut.) Do not auto-chain further work. Wait for the user.

## Guidelines

### Effort

| Level | Cap |
| --- | --- |
| `snapshot` | ≤5 candidates |
| `standard` | ≤20 (default) |
| `deep` | broader + explicit skips |

### Trigger → dimension

| Request | Primary dimension |
| --- | --- |
| architecture / 架构 | Structure |
| simplicity / 过度设计 / bloat | Structure — prefer cut-tagged candidates; a cuts-only pass limits its scope to removal opportunities |
| security / 安全 | Correctness (signaled) |
| tests / 测试 | Verification |
| deps / dx / docs | Traceability |
| ops / release / deploy / migration | Correctness (signaled) + Traceability |
| branch / 分支 | Prefer hotspots on that branch; still four-dimension report |

### Disambiguation

| Request | Route |
| --- | --- |
| Branch / PR / since X / quality report | [`sdd-review`](../sdd-review/SKILL.md) |
| Spec / AC compliance on a diff | [`sdd-review`](../sdd-review/SKILL.md) |
| Over-engineering / bloat / what to delete only | This skill, with scope limited to removal opportunities |
| Trade-offs / grill a design | Upstream design-interview skill |
| Fix while scanning | Decline — finish the improve report first; Stop |

### What NOT to do

Do not: edit the tree; emit HTML reports; judge Spec/AC compliance; treat priority as a merge gate; duplicate an increment quality report on a scoped PR diff; one-line findings without evidence; auto-start follow-up work after the report.
