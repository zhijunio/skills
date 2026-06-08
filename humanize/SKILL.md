---
name: humanize
description: De-AI polish and in-place editing for Chinese/English prose; remove AI tells without compressing paragraphs or stripping substance. Use for humanize, polish, remove AI tells, release notes, tweet review. Not for code comments, commit messages, or inline API docs. For chapter reordering or section rewrite use rewrite-article. Load references/write-methods.md before editing.
---

# Humanize — sound human, not generated

**De-AI ≠ shorten.** Remove template phrases and hollow filler; **do not** compress paragraphs into outline-style stubs or cut explanation the reader still needs.

**Word lists and mode-specific checklists** live in `humanize/references/`; this page keeps boundaries and hard rules only.

You may inline `🥷` at the start of the first reply (not as its own paragraph).

## Before editing

1. Open **[`humanize/references/write-methods.md`](references/write-methods.md)** and follow the **loading router** for the right reference (ZH / EN / bilingual / release / social / document review, etc.).
2. Read those references before editing; **do not** apply ZH/EN rules from memory and mix them up.

## Boundaries

- **Natural language only**: not code review; do not rewrite commit messages unless the user explicitly treats one as copy to polish.
- **Meaning over style**: cutting filler must not distort the author’s intent.
- **Do not compress paragraphs** (see [`write-zh.md`](references/write-zh.md) § Do not compress): replace AI tells in place; keep or restore **complete sentences**, causal links, and technical explanation. If a § reads like bullet notes, **expand** with detail already in the draft — do not invent facts.
- **Structure changes** → use **`rewrite-article`** (sections, DAG order); this skill does **not** merge §§ or delete paragraphs for brevity.

## Preflight (ask one question if missing)

1. **Body text?** Instruction only, no paste → ask for paste or path.
2. **Audience locked?** Cannot infer blog vs email vs RFC → ask first.
3. **Language**: choose references from the **text being edited** (see `write-methods.md`); the user’s UI language does not imply the draft language.

## Hard rules

- **In-place edit**: do not change heading levels, merge sections, or reorder paragraphs unless the user asks for structure changes (or **`rewrite-article`** already fixed structure).
- **Default zero meta**: output **revised body only**; no default change log unless the user wants changelog-style notes or a **QA report** (see below).
- **No prestige vocabulary**: goal is natural and clear, not “sounding smart.”
- **No invented facts**: do not add experiences, stats, or API/runtime behavior not in the draft or cited sources ([`references/handoff-humanize.md`](references/handoff-humanize.md)).
- **No draft-as-skill-sample**: skill `references/` use placeholders only; do not treat vault topic drafts as golden examples when polishing.

## QA report mode (optional)

When the user asks for **质检**, **QA report**, **检查报告**, or **去 AI 味并给报告**:

1. Load [`references/write-qa-layers.md`](references/write-qa-layers.md).
2. Run **L1–L4**; fix L1–L2 in the body when safe.
3. Output **revised body** then the report template from `write-qa-layers.md`, unless the user asked for **report-only**.

Default humanize still runs L1–L2 mentally but **does not** attach a report.

## Mode details (in references)

| Need | See |
|------|-----|
| Chinese tells, register | [`write-zh.md`](references/write-zh.md) |
| English tells, register | [`write-en.md`](references/write-en.md) |
| Mixed ZH/EN, bilingual pairs | [`write-bilingual.md`](references/write-bilingual.md) |
| Chinese technical blog / long-form tech | [`write-zh.md`](references/write-zh.md) + [`write-qa-layers.md`](references/write-qa-layers.md) |
| Layered de-AI QA (L1–L4) + optional report | [`write-qa-layers.md`](references/write-qa-layers.md) |
| Upstream handoff boundaries | [`handoff-humanize.md`](references/handoff-humanize.md) |
| Release / changelog | [`write-release-notes.md`](references/write-release-notes.md) |
| Social / tweets | [`write-social.md`](references/write-social.md) |
| Long-doc review, privacy, coherence scan | [`write-document-review.md`](references/write-document-review.md) |

## Gotchas

| Situation | Handling |
|-----------|----------|
| Blog turned into academic tone | Match audience; keep acceptable colloquialisms |
| Draft got shorter but not clearer | Re-expand § that lost causality; only delete true filler |
| User did not ask for a diff list | Deliver body only |
| English-only draft gets CJK spacing rules | Apply only for mixed ZH/EN (see `write-bilingual.md`) |

## Output

Default: **polished full text only**. At most **one sentence** at the end if truncation or ambiguity forced a caveat; otherwise no preamble or postscript.
