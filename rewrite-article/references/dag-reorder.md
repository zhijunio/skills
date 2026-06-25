# DAG reorder — split, dependencies, confirmation

Use after you have the full article (path or paste). Do **not** rewrite body text until the user confirms the proposed outline.

## 1. Split by headings

1. List every `##` / `###` (and deeper if the piece is long) with a **one-line job**: what the reader must know or do after that section.
2. Flag sections that are **empty**, **duplicate**, or **only transition fluff** — candidate for merge or cut (propose in the outline, do not delete silently).
3. Note **heading level** changes if a subsection should become a top-level chapter (or the reverse).

## 2. Build the dependency graph

Treat each section as a node. Draw edges **A → B** when B requires A’s concepts, terms, or conclusions.

| Edge type | Meaning | Example |
|-----------|---------|---------|
| **Prerequisite** | B uses terms or facts introduced in A | “Install” before “First run” |
| **Motivation** | Reader needs the problem in A to care about B | “Pain today” before “Our approach” |
| **Evidence** | Claim in B relies on setup or data from A | “Benchmark setup” before “Results” |
| **No edge** | Sections are independent | Two case studies — order by narrative, not logic |

Rules:

- **Cycles** → merge, split, or add a short bridge section; do not ship circular “learn X in §3 that §1 already used.”
- **Orphans** (no in/out edges) → often belongs in intro, appendix, or merged into a neighbor.
- **Conclusion too early** → move “So what” after the evidence chain; keep a **one-sentence hook** in the intro if needed.

## 3. Propose order (merge / split allowed)

Deliver a **confirmation block** before editing:

```markdown
## Proposed outline (confirm before rewrite)

| # | Section (new) | Source | Notes |
|---|---------------|--------|-------|
| 1 | … | was §X + §Y | merged: both described setup |
| 2 | … | was §Z | moved up: prerequisite for §4 |

**DAG fixes:** …
**Cuts / merges:** …
**Heading level changes:** …
```

Wait for explicit approval (or “proceed with this outline”). If the user edits the table, use their version.

## 4. Worked example (before → after)

**Before (published order):**

1. Results — “We cut latency 40%.”
2. Architecture — components and data flow
3. Problem — why latency mattered
4. How we measured — benchmark harness
5. Future work

**DAG issues:**

- §1 uses architecture and measurement context not yet introduced.
- §3 should motivate §2 and §4, not follow results.

**After (proposed order):**

1. Problem — stakes and constraints
2. Architecture — map before numbers
3. How we measured — harness and baseline
4. Results — 40% with defined terms
5. Future work

**Intro hook (optional one paragraph):** state the outcome once, then “Below: how we got there.”

## 5. When order is subjective

If two sections are **DAG-independent** (e.g. “Team” vs “Roadmap”):

- Prefer **reader journey**: context → method → outcome → implications.
- Prefer **increasing specificity**: overview before deep dive.
- Say so in the confirmation notes: “Order is narrative, not hard dependency.”
