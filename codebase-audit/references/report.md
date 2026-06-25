# Report

**Delivery: chat only** — post the full report in the conversation.

**No disk by default** — do **not** create or update audit files in the target repo (including `.codebase-audit/`, `findings-*.json`, or report Markdown) unless the user **explicitly** asks to persist a baseline or export.

**Report body:** user's language. **Section headings** follow the user's language too (use English headings below when the user writes in English). Literals: paths, lens ids, git refs, **severity emojis** (below).

## Severity emoji

Use emoji **with** the level text in the findings table — not instead of rubric. **Roadmap phases use text only** (`P0` / `P1` / `P2` — no emoji).

| Emoji | Level | When |
|-------|-------|------|
| 🚨 | **Critical** | Systematic contract break; secrets on hot path; auth bypass class; diverging sources of truth |
| 🔴 | **High** | Boundary violation; missing authZ on mutations; critical CVE on path |
| 🟡 | **Medium** | Duplication tax; missing boundary tests; observability gaps |
| 🟢 | **Low** | Non-critical drift; minor smell without pattern |

Cell format: `🚨 Critical` / `🚨 严重` — emoji + localized label.

**Executive summary:** optional severity counts — e.g. `🚨×1 🔴×3 🟡×2`.

## Roadmap phases (text only)

| Phase | When |
|-------|------|
| **P0** | Unblockers first — verify baseline, secrets, contract breaks before structural work |
| **P1** | High-leverage systemic fixes after P0 |
| **P2** | Polish, DX, low-urgency drift |

Cell format: `P0` · `P1` · `P2` only — **no emoji**. Severity and scheduling are independent (e.g. `🟡 Medium` finding may appear under **P0**).

## Finding table

```markdown
| severity | lens | title | files | evidence | evidence_type | confidence | impact | effort | fix_risk | attribution | structural_fix |
```

**snapshot / 快照:** ≤5 rows. **standard / deep / 标准 / 深度:** max **20** rows. Zero → `NO_FINDINGS` + patterns searched.

### Columns

| Col | Rule |
|-----|------|
| severity | 🚨 Critical · 🔴 High · 🟡 Medium · 🟢 Low (+ localized label) |
| lens | **A1–A6**, **C0–C3**, **S1**, **V1–V2**, **D1**, **O1** — `map.md` |
| files | `path:line` comma-separated |
| evidence | ≤3 lines or call-path |
| evidence_type | `observed` \| `inferred` |
| attribution | `—` \| `introduced` / `pre-existing` (English branch audit) \| 本分支引入 / 既有问题 (Chinese branch audit) |
| structural_fix | Systemic move — not typo fix |

## Report template

**snapshot / 快照** → Scope + summary + ≤5 findings. **standard / deep / 标准 / 深度** → full below.

**Section order:** `Scope` → `Executive summary` → `Boundary map` (if in scope) → `Findings` → `Roadmap` → `Direction notes` (optional) → `Rejected findings` → `Not audited` → **`Suggested next steps` last**.

```markdown
# Codebase Audit — {Project}

> Date · Target · Effort · Range

## Scope

| Field | Value |
|-------|-------|
| HEAD | `{sha}` |
| Shape / Stack | … |
| Pillars | A, C, S, V, D, O — list skipped |
| Verification | commands — ok / partial / none |

## Executive summary

2–4 sentences: healthy / drifting / at risk; top risks (optional `🚨`/`🔴`/`🟡` counts); next move.

## Boundary map (architecture)

```text
[layer] → modules → notes
```

## Findings

Finding table above. Branch audits: use **attribution** column or split sections.

## Roadmap

| Phase | Systemic move | Depends on |
| P0 | … | … |
| P1 | … | … |
| P2 | … | … |

Simplicity audits: roadmap rows should favor **delete / merge / collapse** over extract-and-wrap.

## Direction notes (optional)

2–4 options with trade-offs — not ranked vs findings.

## Rejected findings

| title | reason |

## Not audited

…

## Suggested next steps

**Always last section.** Direct edits · implementation plans · behavior spec · diff review · follow-up deep pillar.
```
