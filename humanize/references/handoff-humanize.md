# Handoff — when other skills pass work to humanize

humanize owns **word-level polish, tells, register, and optional layered QA**. It does **not** own research, structure reorder, or new technical claims.

## 1. Upstream skills

| From | humanize receives | humanize must not |
|------|-------------------|-------------------|
| **tech-mastery** | Main article or interview draft; fixed facts | Add JEP/API facts not in draft; reorder § without user ask |
| **rewrite-article** | Confirmed section order + rewritten body | Re-run DAG merge/split unless user asks |
| **fetch-content** | Raw or lightly cleaned material | Publish without user; invent narrative |

**tech-mastery:** run **humanize** after Write and Interviewize (see **`tech-mastery/SKILL.md`** § Humanize).

Chain (typical):

```text
fetch-content → tech-mastery (Scope → Source → … → Write → Interviewize) → humanize
                                    rewrite-article? ─────────────────────────────┘
                         save to second-brain/23-work/research/ (user asks)
```

## 2. AI / human boundary (de-AI)

Inspired by persona-writing workflows: **polish ≠ author.**

| Safe for humanize | Not safe — ask user or upstream |
|-----------------|----------------------------------|
| Cut filler, fix tells, split overloaded sentences | First-hand stories, 「我当时…」 unless already in draft |
| Replace banned phrases (see `write-zh.md`) | New statistics, benchmarks, version defaults |
| Tighten transitions (one bridge sentence) | «比如有一次…» hypothetical scenes |
| Restore causality when a § became too terse after edits | **Compressing paragraphs** or deleting explanation to «sound human» |
| Run [`write-qa-layers.md`](write-qa-layers.md) L1–L2 | Change technical conclusions to «sound better» |

## 3. What to load

1. **`humanize/SKILL.md`** — output contract, in-place rule.
2. **`write-methods.md`** — router (ZH / EN / tech blog / …).
3. Technical Chinese long-form: **`write-zh.md`** + **`write-qa-layers.md`** (L1–L4 when 质检).
4. Pre-publish technical review: **`tech-mastery/SKILL.md`** completion checklist + [`write-qa-layers.md`](write-qa-layers.md) L3.

## 4. Output

- Default: **full revised body only**.
- User asked 质检 / QA report: body + [`write-qa-layers.md`](write-qa-layers.md) report template, or report-only.

## 5. Anti-patterns

| Do not | Do instead |
|--------|------------|
| Reorder sections during humanize | **rewrite-article** |
| Apply persona-only bans (e.g. no colons) to technical RFCs | Match audience in `write-methods.md` |
| Deliver QA report on every polish pass | Report only when requested |
| Strip all headings from tech articles | Keep structure; polish prose in place |
| Compress paragraphs during de-AI | Remove tells only; keep § substance and length ≈ draft unless user asked to shorten |
