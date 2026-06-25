# Transitions and wrap-up read

Run after all sections are rewritten in confirmed order — **before** delivery or humanize handoff.

## 1. Inter-section transitions

For each boundary **§N → §N+1**, check:

| Check | Fix if failing |
|-------|----------------|
| **Forward reference** | §N promises something §N+1 never delivers → add content or trim the promise |
| **Backward reference** | §N+1 says “as above” but order moved → rewrite to name the section or restate briefly |
| **Term drift** | Same concept, different label → pick one term; align headings if needed |
| **Tone cliff** | §N casual, §N+1 manifesto → soften or tighten one side |
| **Duplicate opener** | Two sections start with “In this section we will…” → cut one; vary the other |

**Light bridges (optional):** one sentence at end of §N or start of §N+1 — only when the DAG reorder left a gap. Do not add filler paragraphs.

## 2. Whole-document scan

Read start to finish once:

1. **Duplication** — same example or diagram explained twice; keep the stronger instance, cross-link or “see §X.”
2. **Gaps** — reader hits a term or acronym with no prior definition.
3. **Intro vs body** — intro should not repeat §2–§3 verbatim; tighten to promise + roadmap.
4. **Conclusion** — should reflect final section order (not an old outline).
5. **Heading hierarchy** — no skipped levels (`##` then `####`); consistent `#` title if present.

## 3. Deliver

- Default: **full revised Markdown** (minimal meta).
- Optional one-line caveat only if truncation, ambiguous source, or user must confirm a factual flag.

## 4. Then what?

| User signal | Next step |
|-------------|-----------|
| “Good”, “ship it”, no polish ask | Stop; deliver text |
| “De-AI”, “润色”, “sound less AI”, “humanize” | **`Read` `handoff-humanize.md`** then invoke **`humanize`** |
| “Research more on §X” | **`tech-mastery`** or fetch via **`fetch-content`** — not another full reorder unless structure broke |
