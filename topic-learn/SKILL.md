---
name: topic-learn
description: "Learn one topic end to end (学): scoped mission, layered sources, mastery map, verified evidence, canonical article, interview layer, learning records. Modes new or refresh. Use for 主题学习, 技术深挖, 溯源验证; not single-link summaries, blank-page article writing, or codebase audit."
---

# Topic Learn

**Role: 学** — Turn **one knowledge point** into a **reusable, source-backed, verifiable** topic asset.

Examples: Java reflection, TCP congestion control, Raft, HTTP caching, Postgres MVCC.

**Not for:** single-link summaries, opinion without sources, shallow cheat sheets, marketing-only article drafts, codebase audit.

## Invocation

| Required | Optional |
|----------|----------|
| Topic (one knowledge point) | `mode`: `new` (default) or `refresh` |
| Boundary + out of scope | Depth: `brief` / `standard` (default) / `deep` |
| **Success looks like** (≥2 outcomes) | Save path, language `zh` / `en` |
| Save directory | |

Missing **Success** or **boundary** → ask **at most 3** questions. **Do not start Source** until Mission is writable.

## Depth

| Level | Deliverables |
|-------|----------------|
| **`brief`** | Mission + map + `sources/INDEX` + slim main article (problem, mechanism, sources); interview: one-liner + ≥3 follow-ups |
| **`standard`** | Full checklist; glossary optional |
| **`deep`** | + glossary and/or `reference/`; 0–2 side articles; mechanism-heavy topics default to diagrams (`article-structure.md`) |

Detail: `references/formats.md` → Depth.

## Modes (`TOPIC.yaml` → `mode`)

| Mode | When |
|------|------|
| **`new`** | No workspace yet, or full first pass |
| **`refresh`** | Merge, gap-fill, re-verify, or humanize-only |

Refresh variants (detail in `references/workflow.md`):

- **Merge** — align scattered drafts; diff-only Write
- **Gap-fill** — only missing pieces (e.g. interview)
- **Verify / polish** — re-run demos, fix `verified_on`, humanize touched files

## Before you start

1. Read existing topic dir; set `mode` in `TOPIC.yaml` (`references/formats.md`).
2. **New workspace** — optional bootstrap:

```bash
/path/to/topic-learn/scripts/topic_scaffold.sh /path/to/<topic-dir> <slug> [zh|en]
```

3. **按需读** — start at `references/README.md`; never bulk-read all references.

## Flow

```text
Scope → Source → Model → Verify → Write → Interviewize → Record → Humanize → Checklist
```

- **`new`**: full flow (`references/workflow.md`).
- **`refresh`**: delta on each step; touch ≤6 files unless full rewrite.

**Stop points** (user may say「一次跑完」):

1. After **Scope** — confirm Mission + boundary.
2. After **Model** — confirm mastery-map + gaps before Write.

## Hard rules

- One topic per run.
- No mastery map → no Write.
- No L1/L2 for key claims → no Write (or explicit Open).
- No verification → not complete.
- No interview layer → not complete.
- All prose artifacts in one topic directory (`code_repo` for runnable code when documented).

## Humanize boundary

Polish **main + interview** when the user asks — preserve sources, verification commands, claims, and glossary terms. User style notes override default tone.

| Need | Skill |
|------|-------|
| Learning-topic polish in this workspace | This skill (Step 8) |
| Neutral de-AI only, no author persona | `humanizer` (user triggers separately) |
| Author voice + publish-ready in writing flow | `article-write` refine stage |

## Related skills

| Skill | When |
|-------|------|
| `article-read` | Source step — fetch URL/PDF into `sources/` (`references/source-ingest.md`) |
| `humanizer` | Finished main/interview, neutral de-AI only |
| `article-write` | Turn mastery assets into publish-oriented articles (user triggers separately) |
| `flowguard` | Long `new`/`refresh` spanning many sessions — wrap with checkpoints |

## Validation

```bash
bash -n topic-learn/scripts/topic_scaffold.sh
bash -n topic-learn/scripts/topic_check.sh
topic-learn/scripts/topic_scaffold.sh /tmp/topic-smoke test-slug zh
topic-learn/scripts/topic_check.sh /tmp/topic-smoke    # expect warnings on fresh scaffold
```

Before marking `phase: shipped`, run `topic_check.sh` on the topic directory.

## References

Start at `references/README.md`. Scripts: `topic_scaffold.sh`, `topic_check.sh`.
