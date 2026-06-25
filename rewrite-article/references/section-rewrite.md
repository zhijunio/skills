# Section-by-section rewrite

Rewrite **one section at a time** in the **confirmed order**. On very long pieces, report progress after each major `##` (e.g. “§3/8 done”).

## 1. Per-section checklist

1. **Opening sentence** — states what this section adds (no throat-clearing).
2. **One main idea per paragraph** — split if two claims share a paragraph.
3. **Terminology** — use terms already introduced upstream; define once on first use in this piece.
4. **Lists and code** — keep; tighten prose around them, do not prose-ify bullet lists.
5. **Facts and stance** — preserve author intent; do not add statistics, quotes, or “common knowledge” without source.
6. **Closing** — optional one-line bridge to the next section (full transition pass in [`transitions-wrapup.md`](transitions-wrapup.md)).

## 2. Paragraph length (~240 characters)

**Default:** aim for **≤ 240 characters per paragraph** (CJK and Latin both count characters, not bytes).

| OK to exceed | Why |
|--------------|-----|
| Single definition that must stay together | Splitting breaks the term/definition pair |
| Quoted material | Do not chop quotes for length |
| List intro line + list | Count the intro; list items are separate |
| Code block or ASCII diagram | Exempt; shorten surrounding prose instead |

**When a paragraph is too long:**

1. Split at **logical joints** (claim → evidence, step → step), not mid-sentence.
2. Prefer **two short paragraphs** over one long paragraph with em dashes stacking clauses.
3. If still long after split, check for **two sections merged** — consider splitting the heading instead.

**When strict 240 hurts readability** (dense RFC-style spec): note in wrap-up: “§4 kept longer paragraphs for definition blocks”; do not silently ignore the rule across the whole doc.

## 3. What this pass is not

- **Not de-AI polish** — do not run humanize tell lists or register scrubbing here; see [`handoff-humanize.md`](handoff-humanize.md).
- **Not fact-checking research** — flag “needs source” inline only if the original already looks wrong; do not invent citations.
- **Not tone makeover** — clarity and pace only; voice stays the author’s unless the user asked otherwise.

## 4. In-place vs deliverable

| User intent | Action |
|-------------|--------|
| Gave a file path, no other instruction | Update file in place when tooling allows; else output full revised Markdown |
| Paste only | Output full revised Markdown |
| “Show only changed sections” | Output changed `##` blocks only; still run wrap-up read mentally |

Default output contract: **revised full text only** (see parent `SKILL.md`).
