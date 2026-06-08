# Humanize reference router (this repo)

Read this file before editing. Load references from the **text being edited** (not the user’s instruction language).

## 1. Loading router

| Text and task | Required reference |
|---------------|-------------------|
| Primarily Chinese body copy | [`write-zh.md`](write-zh.md) |
| Chinese **technical blog** (API, protocol, runtime explainers) | [`write-zh.md`](write-zh.md) + [`write-qa-layers.md`](write-qa-layers.md) L1–L4 |
| Primarily English body copy | [`write-en.md`](write-en.md) + [`write-qa-layers.md`](write-qa-layers.md) L1–L2 |
| Mixed ZH/EN, bilingual pairs, translation review | [`write-bilingual.md`](write-bilingual.md); add `write-zh.md` / `write-en.md` as needed |
| Release / changelog | [`write-release-notes.md`](write-release-notes.md) |
| Twitter / X / short social | [`write-social.md`](write-social.md) |
| Whitepaper / long-doc review (incl. privacy scan) | [`write-document-review.md`](write-document-review.md) |
| Technical long-form **pre-publish review** (accuracy, versions) | **`tech-mastery/SKILL.md`** checklist + [`write-qa-layers.md`](write-qa-layers.md) L3–L4 |
| De-AI **with QA report** (质检 / 检查报告) | [`write-qa-layers.md`](write-qa-layers.md) — report template in § Optional QA report |
| Upstream handoff (tech-mastery / rewrite-article) | [`handoff-humanize.md`](handoff-humanize.md) |
| Coherence scan only, no full rewrite | [`write-document-review.md`](write-document-review.md) — “paragraph coherence” section |

If the user asks for multiple modes (e.g. bilingual release notes), **merge rules top-down**; on conflict, prefer **unchanged meaning, in-place edit, minimal meta** (see parent `SKILL.md`).

## 2. Relationship to parent `SKILL.md`

- **Hard rules, boundaries, output contract** → **`humanize/SKILL.md`** in this repo.
- This directory adds **checklists, word lists, mode notes** so `SKILL.md` stays short.

## 3. External links (not vendored)

- Chinese copy style: [中文文案排版指北](https://github.com/mzlogin/chinese-copywriting-guidelines)
