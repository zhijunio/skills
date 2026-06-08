---
name: rewrite-article
description: Split by headings, reorder by information dependency (DAG), then rewrite long articles section by section; keep paragraphs under ~240 characters when possible. Use to fix structure, reorder chapters, rewrite existing articles — not de-AI polish (use humanize). Load references/rewrite-methods.md before editing.
---

# Rewrite article — reorder and section rewrite

Completes [mattpocock/skills `edit-article`](https://github.com/mattpocock/skills/tree/main/skills/personal/edit-article) (MIT). Split from **`humanize`**: this skill handles **structure and in-section rewrite**; **humanize** defaults to in-place polish without reordering sections.

**DAG patterns, rewrite heuristics, transitions, and humanize handoff** live in [`references/`](references/); this page keeps boundaries and flow only.

## Before you start

1. Open **[`references/rewrite-methods.md`](references/rewrite-methods.md)** and load references for the current phase (see router table).
2. Need **article path or full text**; otherwise ask the user.
3. Confirm whether **heading levels**, deletions, and merges are allowed (default: propose in the DAG confirmation step — see [`references/dag-reorder.md`](references/dag-reorder.md)).

## Boundaries

- **Targets**: blogs, newsletters, long technical **Markdown/articles** — not code, not commit messages.
- **Does not replace humanize**: after structure is settled, follow [`references/handoff-humanize.md`](references/handoff-humanize.md) and **`Read` `humanize`** for de-AI polish.
- **Does not replace tech-mastery**: research-from-scratch long-form → **`tech-mastery`**.

## Flow

### 1. Sections and dependencies

Follow [`references/dag-reorder.md`](references/dag-reorder.md):

1. Split by **headings**; clarify what each section must convey.
2. Treat information as a **DAG**: prerequisites before dependents.
3. Propose a **section order** (merge/split allowed); **confirm with the user** before editing body text.

### 2. Section-by-section rewrite

Follow [`references/section-rewrite.md`](references/section-rewrite.md). For each section (in confirmed order):

1. **Rewrite** for clarity, flow, and pace; **aim for ≤ 240 characters per paragraph** (exceptions in reference).
2. Preserve the author’s facts and stance; do not add unverified claims.
3. Finish one section before the next (report progress per section on very long pieces).

### 3. Wrap-up

Follow [`references/transitions-wrapup.md`](references/transitions-wrapup.md):

1. Read end-to-end: transitions, duplication, gaps between sections.
2. Deliver the **full revised text** (default: update the user’s path in place, or only changed regions if asked).
3. If the user wants de-AI polish → [`references/handoff-humanize.md`](references/handoff-humanize.md); do not apply humanize word lists inside this skill.

## Output

Default: **revised full text only** (same as humanize — minimal meta). Add a short change summary only if the user asks.

## References

| File | Contents |
|------|----------|
| [`rewrite-methods.md`](references/rewrite-methods.md) | Loading router |
| [`dag-reorder.md`](references/dag-reorder.md) | Split, DAG, confirmation outline, worked example |
| [`section-rewrite.md`](references/section-rewrite.md) | Per-section rewrite, 240-char rule |
| [`transitions-wrapup.md`](references/transitions-wrapup.md) | Inter-section bridges, final scan |
| [`handoff-humanize.md`](references/handoff-humanize.md) | When and how to chain **humanize** |
| [`tech-article-dag.md`](references/tech-article-dag.md) | Technical explainer default order |
