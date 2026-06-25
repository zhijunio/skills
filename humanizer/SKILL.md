---
name: humanizer
description: Remove AI-writing tells from finished prose only — neutral tone, preserve facts and structure. Optional deep QA (tell-focused L1–L4). Use for 去AI味, humanize, de-AI polish, 润色; not 审校定稿 with author voice-default, publish-ready writing pipeline, blank-page drafts, or chapter reorder.
---

# Humanizer

In-place **language polish** on finished or near-finished text. Default: **do not** reorder sections, change heading levels, or add/remove factual claims.

**Produces:** revised text; optional QA report (`references/report-template.md`).

**Does not produce:** new articles, DAG reorder, or platform-specific social copy (user provides style constraints separately).

## Hard rules

1. **Preserve facts** — numbers, names, dates, links, code, quotes stay unless clearly wrong and user allows correction.
2. **Preserve structure** — same headings and section order unless user explicitly asks to restructure.
3. **No invented sources** — do not add citations, studies, or anecdotes to "sound human."
4. **Match draft language** — polish Chinese in Chinese rules, English in English rules; bilingual drafts follow `references/bilingual.md`.
5. **Smallest change** — fix tells first; avoid full rewrite when local edits suffice.

## Effort level

| | touch | standard (default) | deep |
|---|-------|-------------------|------|
| Scope | One section or &lt;800 chars | Full piece | Full piece + QA report |
| Passes | Detect → rewrite | + soul pass → vet | + L1–L4 (`references/qa-layers.md`) |
| Output | Revised text only | Revised text | Text + 质检报告 |

## Workflow

1. **Scope** — Read `references/workflow.md`. Confirm path or paste, language lane, tone (technical / casual / formal), and forbidden edits.
2. **Detect** — Scan `references/patterns.md`; apply `references/zh-tells.md` or `references/en-tells.md`.
3. **Rewrite** — Local fixes first; vary rhythm; remove chatbot artifacts.
4. **Soul pass** — Read `references/soul-pass.md`; add human texture without new facts.
5. **Vet** — Read `references/vet.md`; revert over-edits.
6. **Deliver** — Revised text in place (file edit or paste). Deep: append QA report.

## Anti-pattern finale (standard+)

After draft rewrite, ask internally: *What still sounds obviously AI-generated?* Fix remaining tells in one more pass. Do not narrate this loop to the user unless they want a deep QA report.

## Scope boundaries

| Use this skill | Use instead |
|----------------|-------------|
| 去 AI 味 on **existing** finished text | Blank-page writing → `article-write` |
| Neutral register polish (no author persona) | `article-write` refine — author voice + `qa-layers` publish-ready |
| Light texture without new claims | Chapter reorder / DAG fix |
| Optional structured QA (deep) | Fact-checking / topic mastery → `topic-learn` |

## References

- `references/workflow.md` — intake and delivery
- `references/patterns.md` — 24-pattern index (language-agnostic)
- `references/zh-tells.md` — Chinese tells and replacements
- `references/en-tells.md` — English tells and replacements
- `references/bilingual.md` — mixed ZH/EN spacing and lane rules
- `references/soul-pass.md` — voice without slop
- `references/vet.md` — false positives and stop conditions
- `references/qa-layers.md` — L1–L4 gates (deep mode)
- `references/report-template.md` — 质检报告 shape
- `references/patterns-full.md` — extended EN examples (Wikipedia-derived)
