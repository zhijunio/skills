# Rewrite-article reference router (this repo)

Read this file before restructuring or rewriting. Load references for the phase you are in.

## 1. Loading router

| Phase / need | Required reference |
|--------------|-------------------|
| Split headings, merge/split, DAG reorder, user confirmation outline | [`dag-reorder.md`](dag-reorder.md) |
| Technical explainer default section order | [`tech-article-dag.md`](tech-article-dag.md) |
| Per-section rewrite, paragraph length, facts/stance | [`section-rewrite.md`](section-rewrite.md) |
| Inter-section transitions, duplication, gaps, final read | [`transitions-wrapup.md`](transitions-wrapup.md) |
| Structure done; user wants de-AI or copy polish | [`handoff-humanize.md`](handoff-humanize.md) |

If the user asks for structure **and** de-AI in one pass: finish **rewrite-article** first (confirm order → rewrite → wrap-up), then **`Read` `humanize`** — do not merge humanize word lists into this skill.

## 2. Relationship to parent `SKILL.md`

- **Hard rules, boundaries, output contract** → **`rewrite-article/SKILL.md`**.
- This directory adds **DAG patterns, rewrite heuristics, transition checks, humanize handoff** so `SKILL.md` stays short.

## 3. Sibling skills

| Skill | When |
|-------|------|
| **`humanize`** | In-place de-AI / register polish after structure is settled |
| **`tech-mastery`** | Research-from-scratch long-form; messy draft → rewrite-article then humanize |
| **`fetch-content`** | Fetch URL/PDF before editing if the user only has a link |
