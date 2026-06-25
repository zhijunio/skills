# Handoff to humanize

**rewrite-article** owns **structure and in-section clarity**. **humanize** owns **register, tells, bilingual layout, and copy modes** without reordering.

## 1. When to hand off

Hand off when **all** are true:

1. Section order is **confirmed** and rewritten.
2. Wrap-up read in [`transitions-wrapup.md`](transitions-wrapup.md) is done.
3. User wants **de-AI**, **polish**, **release/social/bilingual** treatment, or says “humanize” / “润色” / “去 AI 味”.

Do **not** hand off mid-outline or mid-section unless the user explicitly skips structure work.

## 2. What rewrite-article leaves for humanize

| Already settled (do not redo in humanize unless user asks) | humanize should focus on |
|------------------------------------------------------------|---------------------------|
| Section order and merges | Word-level tells, rhythm, register |
| Paragraph splits for length | ZH/EN spacing, banned phrases (see `humanize/references/`) |
| DAG-fixed terminology consistency | Audience-appropriate tone; **do not compress §** — see `humanize` «Do not compress paragraphs» |
| Factual content and stance | Mode-specific checklists (release, social, bilingual) |

Tell the user (one sentence, only if natural): structure is fixed; next step is **`humanize`** for language polish. Load **`humanize/references/handoff-humanize.md`** for fact boundaries and optional **`write-qa-layers.md`** if they want a QA report.

## 3. How to invoke humanize

1. **`Read`** the **`humanize`** skill (`humanize/SKILL.md`).
2. Follow **`humanize/references/write-methods.md`** — pick ZH / EN / bilingual / etc. from the **draft language**, not the chat language.
3. Pass the **latest full revised text** (path or paste). humanize defaults to **in-place polish** — it must **not** reorder sections or change heading levels unless the user asks.

## 4. Chains from README

```
fetch-content → tech-mastery (research) → rewrite-article (reorder + rewrite) → humanize (de-AI)
```

- **tech-mastery** Write: if structure is messy → **rewrite-article** first, then **humanize**.
- If the user only needs light touch-up with **no** reorder → skip rewrite-article; use **humanize** alone.

## 5. Anti-patterns

| Do not | Do instead |
|--------|------------|
| Apply humanize word lists during rewrite-article rewrite | Structure first; humanize second |
| Re-run full DAG reorder inside humanize | User must ask for structure again → rewrite-article |
| Deliver humanize output inside the same turn without reading humanize skill | Read skill + references; separate pass |
| Change facts “to sound better” in humanize after rewrite-article locked content | Flag ambiguity; ask user |
