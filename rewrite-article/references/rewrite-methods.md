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

If the user asks for structure **and** de-AI in one pass: finish structure work first (confirm order → rewrite → wrap-up), then follow [`handoff-humanize.md`](handoff-humanize.md) — do not merge de-AI word lists into section rewrite.

## 2. Relationship to parent `SKILL.md`

- **Hard rules, boundaries, output contract** → [`SKILL.md`](../SKILL.md).
- This directory adds **DAG patterns, rewrite heuristics, transition checks, post-structure polish** so `SKILL.md` stays short.

## 3. Out of scope (do not auto-start)

| Need | Action |
|------|--------|
| In-place de-AI / register polish after structure is settled | Follow [`handoff-humanize.md`](handoff-humanize.md); user must explicitly ask |
| Research-from-scratch long-form | Out of scope — user starts a separate research workflow |
| Fetch URL/PDF before editing | Use URL/PDF fetch scripts; out of scope for this skill |
