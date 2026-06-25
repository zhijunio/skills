# Topic Learn — workflow

Load this file when executing steps after intake. Formats: `formats.md`. Templates: `templates/`.

## Refresh variants

**Merge** — scattered drafts → standard topic dir; mastery-map **keep / rewrite / drop**; Write diff only; append learning record on structural merge.

**Gap-fill** — README or map lists gaps (e.g. interview missing); Source/Verify only for gap; Write only missing files.

**Verify / polish** — re-run demos; fix `verified_on` vs `runtime`; humanize touched files only; sync `examples/README.md` from `code_repo`.

## Workspace layout

One topic per directory.

**File budget:** **refresh / gap-fill** → touch **≤6** new or heavily rewritten files (excluding `sources/`). **`new` full pass** may produce **~8–12** files; if the session cannot finish, set `phase` in `TOPIC.yaml` and **handoff** — do not drop checklist items to fit the budget.

```text
<topic-dir>/
  TOPIC.yaml
  README.md
  <slug>-mastery-map.md
  <slug>.md
  <slug>-*.md                 # 0–2 side articles
  <slug>-interview.md
  <slug>-glossary.md
  sources/INDEX.md
  reference/                  # optional
  examples/                   # optional; README-only when code_repo set
  learning-records/
```

`README.md`: file roles, reading order (**Recall**: reference → glossary; **Explain**: main → interview).

Do not scatter prose outside the topic directory. Runnable code may live in external `code_repo` (see below).

### External code repo (optional)

1. Set `TOPIC.yaml` → `code_repo` URL or path.
2. **`examples/README.md`**: commands, pasted real output, link to `code_repo`.
3. Record **`verified_on`** where commands actually ran.
4. On Verify / refresh, run against `code_repo`; sync output back to vault `examples/README.md`.
5. When `code_repo` is set, vault `examples/` is **README-only**.

**Command sync:** verify command must match in mastery-map → Verification, `examples/README.md`, topic README (验证), practice, and main article minimal verification.

## Step 1: Scope

Write **Mission** into `<slug>-mastery-map.md` (`formats.md` → Mission):

- **Why** — concrete outcome, not「了解 X」
- **Success looks like** — reader abilities (explain, demo, answer interview); not run meta
- **Out of scope**
- Optional: constraints (version, time, depth)

**Sizing:** >3 independent core mechanisms → propose split before Source.

**Gate:** no Success + Out of scope → do not proceed to Source.

## Step 2: Source

Layer sources (`formats.md` → Sources):

- **L1** normative: spec, RFC, official docs, reference implementation
- **L2** framework / product official docs or source
- **L3** maintainer blogs, design notes
- **L4** second-hand (explain only; cannot alone support key claims)

**Hard rules:**

1. Key claims need **≥1 L1 or L2** under `sources/`.
2. Maintain `sources/INDEX.md` — link + one line (use when / supports which section).
3. List **## Gaps** and **## Open**. Gap blocks core claim → do not Write.
4. Do not fabricate sources. Before INDEX is populated, do not treat parametric knowledge as verified prose.
5. Archive normative URLs/PDFs per `source-ingest.md` before citing in prose.

## Step 3: Model

Complete mastery-map (`templates/mastery-map.md`): definition, mental model, main flow, version boundaries, sharp edges, self-test questions, verification plan.

**Gate:** map cannot be filled → more Source or narrower scope.

## Step 4: Verify

At least one verification (`formats.md` → Verification) in mastery-map and `TOPIC.yaml`:

- Runnable demo in `examples/README.md` (vault or `code_repo`), **or**
- Source/spec walkthrough (tag + path), **or**
- Version comparison, mistake reproduction

No verification → not complete. Propagate the **same command** to README, practice, main article after Verify.

## Step 5: Write

Main article: `article-structure.md`.

| Depth | Write |
|-------|-------|
| `brief` | Slim main only — problem, mechanism, minimal verification, sources |
| `standard` | Full main skeleton; 0–1 side article if needed |
| `deep` | Full main + 0–2 side articles; `reference/` / glossary as planned |

Structure messy → reorder before Humanize.

## Step 6: Interviewize

`interviewization.md` + `templates/interview-output.md`: one-liner, standard answer, ≥3 follow-ups, project expression + fallback. Terms match glossary when present.

## Step 7: Record

Append `learning-records/NNNN-slug.md` when (`formats.md` → Learning record):

1. Non-obvious misconception corrected
2. Prior knowledge depth established
3. Mission or boundary changed

Do not log mere coverage.

## Step 8: Humanize

Polish **main + interview** when user asks. Use user style guide; do not remove sources, verification, or claims.

Set `TOPIC.yaml` → `phase: shipped`; update `verified_on`, `code_repo`, gaps.

## Completion checklist

- [ ] Mission: Success + Out of scope in mastery-map
- [ ] Core claims backed by L1/L2 or marked Open
- [ ] Verification reproducible; `verified_on` matches demo output when examples exist
- [ ] `runtime` vs `verified_on` documented if they differ
- [ ] `code_repo` set → vault `examples/` README-only; command synced everywhere
- [ ] Main article: problem, mechanism, mistakes, self-test
- [ ] Interview: one-liner, standard, ≥3 follow-ups
- [ ] Glossary/reference consistent (if present)
- [ ] Learning record appended (`new` inaugural ≥1; `refresh` when Step 7 triggers)
- [ ] Main + interview humanized when requested
- [ ] README index complete; `TOPIC.yaml` `phase: shipped`
