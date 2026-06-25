---
name: tech-mastery
description: "Master one technical topic end to end (any domain): scoped mission, layered normative sources, mastery map, verified evidence, canonical article, interview layer, ADR learning records, de-AI polish. Modes: new or refresh (including merge and gap-fill). Use for deep source-backed study, research writeups, and interview-ready technical assets — not single-link summaries or shallow cheat sheets."
---

# Tech mastery

Turn **one technical knowledge point** into a **reusable, source-backed, verifiable** topic asset — protocols, runtime behavior, framework internals, APIs, specs, debugging, system design.

Examples: Java reflection, TCP congestion control, Raft, Spring transaction propagation, HTTP caching, Postgres MVCC.

**Not for:** single-link summaries, opinion without sources, shallow interview-only cheat sheets, interactive HTML courses (see teach-style skills elsewhere).

**Skill 布局（v2 精简）：** `SKILL.md`（流程 + checklist）· `references/formats.md`（Mission/Source/Verify/Glossary/LR/TOPIC）· `references/article-structure.md` · `references/interviewization.md` · `templates/`（3 个可复制模板）。

## Invocation

User should provide at minimum:

| Required | Optional |
|----------|----------|
| Topic (one knowledge point) | `mode`: `new` (default) or `refresh` |
| Boundary + out of scope | Depth: 简要 / 标准 / 深入 (prompt wording; no formal matrix) |
| **Success looks like** (≥2 observable outcomes) | Save path (e.g. `23-work/research/<slug>/`) |
| Save directory | Language: `zh` / `en` |

If **Success** or **boundary** is missing → ask **at most 3** clarifying questions. **Do not start Source** until Mission is writable.

Optional phrasing (same modes):

- **「refresh，合并已有稿」** → merge (see [Refresh variants](#refresh-variants))
- **「refresh，只补 interview / examples」** → gap-fill

## Run modes

Only two values in `TOPIC.yaml` → `mode`:

| Mode | When |
|------|------|
| **`new`** | No topic workspace yet, or **full first pass** (including 从零重写 after deleting old files) |
| **`refresh`** | Topic exists; update, merge, gap-fill, re-verify, or humanize-only |

### Refresh variants

**Merge (align existing material)**  
Use `refresh` when scattered drafts or a single file should become a standard topic directory. Scope: read existing prose; in mastery-map mark **keep / rewrite / drop**; Write **diff only**, not a full rewrite; re-run Verify if new claims appear; append a learning record noting structural merge.

**Gap-fill (one missing piece)**  
Use `refresh` when README or mastery-map already lists gaps (e.g. main article exists, interview missing). Scope: confirm **gaps** only; Source and Verify **only for the gap**; Write **only missing files**; Interviewize and Humanize **only touched files**.

**Verify / polish refresh**  
Use `refresh` when the user asks to re-run demos, fix `verified_on` vs narrative `runtime`, or humanize main + interview only. Update mastery-map Verification, `examples/README.md` sample output (sync from `code_repo` if used), and touched prose; append a learning record if a misconception or env mismatch was corrected.

## Before you start

1. Read existing material in the topic dir; set `mode` in [`templates/TOPIC.yaml`](templates/TOPIC.yaml)（字段见 [`references/formats.md`](references/formats.md)）。
2. **URL/PDF 抓取** at Source（+ `references/topic-sources.md` when installed）。
3. **去 AI 味润色** at Humanize when available。
4. **按需读 reference**（不要一次全读）：
   - Scope / Source / Verify / Record → [`references/formats.md`](references/formats.md)
   - Write → [`references/article-structure.md`](references/article-structure.md)
   - Interviewize → [`references/interviewization.md`](references/interviewization.md)
5. Bootstrap → [`templates/mastery-map.md`](templates/mastery-map.md) · [`templates/interview-output.md`](templates/interview-output.md)

## Outcome

Reader can: explain the topic; state version/spec boundaries and trade-offs; point to source chain; describe verification; answer interview questions at brief and follow-up depth.

## Flow

```text
Scope → Source → Model → Verify → Write → Interviewize → Record → Humanize → Checklist
```

- **`new`**: full flow.
- **`refresh`**: Scope (delta) → Source (changes) → Model (update map) → Verify (re-run) → Write/Interviewize (changed files) → Record (append) → Humanize (touched files) → Checklist.

**Stop points** (user may say「一次跑完」to skip):

1. After **Scope** — confirm Mission + boundary (recommended for large topics).
2. After **Model** — confirm mastery-map + gaps before Write (recommended for 深入).

## Workspace

One topic per directory.

**File budget:** **refresh / gap-fill** → touch **≤6** new or heavily rewritten files (excluding `sources/`). **`new` full pass** may produce **~8–12** files (README, map, main, 1–2 side articles, interview, glossary, reference, examples); if the session cannot finish, set `phase` in `TOPIC.yaml` and **handoff** — do not drop checklist items to fit the budget.

```text
<topic-dir>/
  TOPIC.yaml
  README.md
  <slug>-mastery-map.md      # single planning source (Mission + Map + Sources + Verify)
  <slug>.md                   # main article
  <slug>-*.md                 # 0–2 side articles (README labels role: mechanism, practice, …)
  <slug>-interview.md
  <slug>-glossary.md          # recommended for mechanism / spec topics
  sources/
    INDEX.md
  reference/                  # optional: ≤5 min review pages
  examples/                   # optional: verify notes + expected output (see External code repo)
  learning-records/
    0001-slug.md
```

`README.md`: file roles, reading order (**Recall**: reference → glossary; **Explain**: main → interview).

Do not scatter **prose artifacts** outside the topic directory. Runnable **source code** may live in an external repo when documented (see below).

### External code repo (optional)

When demos are too heavy for the vault (e.g. Java Maven modules, multi-file services):

1. Set `TOPIC.yaml` → `code_repo` to the module URL or path.
2. Keep **`examples/README.md`** in the topic dir: **commands**, **paste real output**, link to `code_repo`.
3. Record **`verified_on`** for where the command actually ran (local JDK, CI matrix, etc.).
4. On **Verify / refresh**, run commands against `code_repo`; sync sample output back to vault `examples/README.md`.
5. When `code_repo` is set, vault `examples/` holds **`README.md` only** — no duplicate source files in the vault.

If `code_repo` is null, runnable files may live under `examples/` as today.

**Command sync (when demos exist):** the verify command must match in **mastery-map → Verification**, **`examples/README.md`**, topic **`README.md` (验证)**, **practice**, and main article **minimal verification** — `examples/README.md` is the evidence source of truth for sample output; other files link or repeat the same command.

## Step 1: Scope

Write **Mission** into `<slug>-mastery-map.md`（[`references/formats.md` → Mission](references/formats.md)）：

- **Why** — concrete outcome, not「了解 X」
- **Success looks like** — **reader/outcome** abilities (explain, demo, answer interview). Do **not** list run meta here (humanize done, file counts); those belong in the checklist.
- **Out of scope**
- Optional: constraints (version, time, depth)

**Sizing:** if the topic needs **>3 independent core mechanisms**, propose split before Source.

Gate: no Success + Out of scope → do not proceed to Source.

## Step 2: Source

Layer sources（[`references/formats.md` → Sources](references/formats.md)）：

- **L1** normative: spec, RFC, official docs, reference implementation source
- **L2** framework / product official docs or source
- **L3** maintainer blogs, design notes
- **L4** high-quality second-hand (explain only; cannot alone support key claims)

**Hard rules:**

1. Key claims need **≥1 L1 or L2** file under `sources/`.
2. Maintain `sources/INDEX.md` — each entry: link + one line (**use when / supports which section**).
3. List **## Gaps** and **## Open** in INDEX or mastery-map. If a gap blocks a core claim → do not Write (narrow scope or mark Open in main article).
4. Do not fabricate sources; fetch URLs/PDFs with this topic's Source scripts. Before INDEX is populated, do not treat parametric knowledge as verified prose.

## Step 3: Model

Complete mastery-map ([`templates/mastery-map.md`](templates/mastery-map.md)): definition, mental model, main flow, version boundaries, sharp edges, self-test questions, verification plan, optional diagram notes.

Gate: if the map cannot be filled, understanding is insufficient — more Source or narrower scope.

## Step 4: Verify

At least one verification, recorded per [`references/formats.md` → Verification](references/formats.md) in mastery-map **## Verification** and `TOPIC.yaml` (`runtime` = narrative baseline, `verified_on` = demo machine if different):

- Runnable demo: **`examples/README.md`** holds command + **paste real output**; source in `examples/` **or** external `code_repo`, **or**
- Source/spec walkthrough (tag + path or section anchor), **or**
- Version or behavior comparison, mistake reproduction

No verification → not mastered; do not ship interview claims that depend on unverified behavior.

After Verify, propagate the **same command** (and link to `examples/README.md` for output) into README, practice, and main article § minimal verification — do not leave stale vault-local commands (e.g. `javac` in `examples/` when source moved to `code_repo`).

## Step 5: Write

Main article: [`references/article-structure.md`](references/article-structure.md).

Side articles (0–2): label role in README — e.g. **mechanism** walkthrough, **practice**, integration, design. Mechanism-heavy Java/runtime topics often use **mechanism + practice**. **No fixed suffix names required.**

Topic **`README.md`**: list **`examples/README.md`** in the file table when verification exists; **验证** section states whether source is vault `examples/` or `code_repo`.

Optional: `reference/` quick-review pages; `<slug>-glossary.md` for dense terminology.

Mechanism-heavy topics: prefer diagrams (topic map, main flow, version delta) over dense prose alone.

Structure messy → **结构重排** before Humanize.

## Step 6: Interviewize

Mandatory [`references/interviewization.md`](references/interviewization.md) + [`templates/interview-output.md`](templates/interview-output.md):

- One-sentence answer
- Standard answer
- ≥3 follow-ups with answers
- Project expression + no-direct-experience fallback

Terms must match glossary when glossary exists.

## Step 7: Record

Append `learning-records/NNNN-slug.md` when（格式 [`references/formats.md` → Learning record](references/formats.md)）：

1. A **non-obvious misconception** was corrected
2. **Prior knowledge depth** was established (avoid re-teaching later)
3. **Mission or boundary** changed

Do not log mere coverage. Number sequentially.

## Step 8: Humanize

Polish **main article + interview** (side articles may stay technical). Apply de-AI polish only when the user asks; for Chinese, use the user's style guide or topic-local replacement tables. Do not remove sources, verification, or claims.

Set `TOPIC.yaml` → `phase: shipped`, update `verified_on` if demos were run, set or update **`code_repo`** when external source was added, and clear or document remaining `gaps`.

## Hard rules

- One topic per run.
- No mastery map → no Write.
- No L1/L2 for key claims → no Write (or explicit Open).
- No verification → not complete.
- No interview layer → not complete.
- **Learning record:** append **`learning-records/0001-…`** on first **`new` ship** (inaugural delta); on **`refresh`** when the three triggers in Step 7 apply.
- No Humanize on main + interview → not complete.
- All **prose artifacts** in one topic directory (runnable source may live in `code_repo`).

## Completion checklist

Before declaring done:

- [ ] Mission: Success + Out of scope in mastery-map
- [ ] Core claims backed by L1/L2 or marked Open
- [ ] Verification reproducible or anchored; **`verified_on` matches pasted demo output** when examples exist
- [ ] `runtime` (narrative) vs `verified_on` (demo) documented if they differ
- [ ] When `code_repo` is set: vault `examples/` is README-only; **Command matches** across mastery-map, `examples/README.md`, topic README, practice, main minimal-verify
- [ ] When `code_repo` is set: `TOPIC.yaml` → `code_repo` URL/path present
- [ ] Main article: problem, mechanism, mistakes, self-test
- [ ] Interview: one-liner, standard answer, ≥3 follow-ups
- [ ] Glossary/reference consistent with main text (if present)
- [ ] Learning record appended this run (`new` inaugural ≥1; `refresh` when Step 7 triggers apply)
- [ ] Main + interview humanized
- [ ] README index complete
- [ ] `TOPIC.yaml`: `phase: shipped`; gaps empty or listed under Open

Optional later: `scripts/verify-topic.sh` after checklist is stable across several topics.

