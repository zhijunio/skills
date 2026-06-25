# Agent Instructions — zhijunio-skills

This repository is a **Cursor Agent Skills library**. Each skill is a directory
with `SKILL.md` as the router. This file is the **repo-wide** onboarding router;
it does not replace per-skill instructions.

## Precedence

1. User instructions in the current session
2. This `AGENTS.md`
3. The target skill's `SKILL.md` and its `references/` / `scripts/`
4. `README.md` (human catalog and install examples)

## Scope Of This Repo

Work here when the goal is **skill authoring, skill editing, or repo
maintenance** — not application code in another repository.

List skills on disk:

```bash
for d in */; do [ -f "${d}SKILL.md" ] && echo "${d%/}"; done | sort
```

Git-tracked list (may lag until committed):

```bash
git ls-files '*/SKILL.md' | sed 's|/SKILL.md||' | sort
```

## Layout Contract

```text
<skill-name>/
  SKILL.md           # required: YAML name + description
  references/        # optional playbooks
  scripts/           # optional shell / Python
  evals/             # optional behavior checks
```

Rules:

- Keep `SKILL.md` concise; move long tables into `references/`.
- Frontmatter `name` **must match** the install directory name.
- Do not invent scripts, paths, or APIs not present in this repo.
- Do not edit `LICENSE` or acknowledgement sections unless the user asked.
- **Cross-skill references allowed** — In `SKILL.md` and `references/`, you may name
  other skills in this repo when clarifying boundaries or handoffs (e.g. point
  Source ingest at `article-read`). Rules:
  - State whether the user must trigger the other skill separately.
  - Do not silently run another skill's full pipeline without user intent.
  - Prefer one primary skill per user turn; references are routing hints, not auto-chains.

## Core Quartet — 读 / 写 / 学 / 审

Primary mental model. User invokes **one** skill per task. Skill bodies may
**reference** siblings for boundaries and typical sequencing; they must not
auto-invoke another skill's end-to-end workflow without explicit user intent.

| 目标 | Skill | Scope |
|------|-------|--------|
| **读** | `article-read` | URL/PDF → Markdown; fetch, archive, optional summarize |
| **写** | `article-write` | Ideate → gather → compose → refine (articles/documents) |
| **学** | `topic-learn` | One topic: sources, verify, mastery assets, interview layer |
| **审** | `codebase-audit` | MECE six pillars — **A**1–6, **C**0–3, **S**1, **V**1–2, **D**1, **O**1; `references/map.md` |

### Boundaries

| Skill | Is not |
|-------|--------|
| `article-read` | Article drafts, topic-learning workspace, codebase audit |
| `article-write` | Full-page URL ingestion, systematic topic learning, codebase audit |
| `topic-learn` | Single-link summary, marketing-only drafts, codebase audit |
| `codebase-audit` | Article writing, topic courses, URL fetch |

### Versus external tools

| Need | Use |
|------|-----|
| Multi-pillar read-only audit + systemic roadmap (chat-only report) | `codebase-audit` (this repo) |
| General improvement audit → executor-ready `plans/` (+ `execute`) | [shadcn/improve](https://github.com/shadcn/improve) — **not vendored** |
| SDD-loop opportunity scan → spec/plan/build handoff | `sdd-improve` in Cursor SDD plugin — **not vendored** here |
| De-AI on finished prose (neutral, no author voice) | `humanizer` |
| Author-voice refine / publish-ready in writing flow | `article-write` refine stage |
| Long / multi-step / resume work | `flowguard` |

## Skill Inventory (on disk)

| Directory | Role |
|-----------|------|
| `article-read` | Core — read |
| `article-write` | Core — write |
| `topic-learn` | Core — learn |
| `codebase-audit` | Core — audit |
| `humanizer` | Polish existing prose |
| `flowguard` | Long-task lifecycle guard |
| `skill-audit` | Meta — audit skill design |
| `skill-creator` | Meta — create skills and evals |
| `keep` | Keep API → `running.json` |
| `jinrishici` | Jinrishici poem API |

## Install And Discovery

**Recommended:** [skills CLI](https://github.com/vercel-labs/skills) (`npx skills`).  
GitHub repo: [zhijunio/skills](https://github.com/zhijunio/skills) (`zhijunio-skills` redirects).

```bash
# List skills in this repo
npx skills add zhijunio/skills --list

# Install all skills for Cursor (global)
npx skills add zhijunio/skills -g -a cursor -y --all

# Core quartet only
npx skills add zhijunio/skills -g -a cursor -y \
  --skill article-read --skill article-write \
  --skill topic-learn --skill codebase-audit

# Verify
npx skills ls -a cursor -g
```

**Local clone** (install from working tree without fetching GitHub):

```bash
cd /path/to/zhijunio-skills
npx skills add . -g -a cursor -y --all
```

Files land in `~/.agents/skills/<name>/`; the CLI registers them for Cursor.
Start a **new Agent chat** after install.

**Manual symlink** (optional, for live edits without re-running `npx`):

```bash
REPO=~/github/zhijunio-skills
mkdir -p ~/.cursor/skills
for skill in article-read article-write codebase-audit topic-learn \
  humanizer flowguard skill-audit skill-creator keep jinrishici; do
  ln -sfn "$REPO/$skill" ~/.cursor/skills/$skill
done
```

Do not duplicate the same skill via both `npx` global install and manual symlink.

Remove legacy names: `npx skills remove architecture-audit content-craft tech-mastery -g -a cursor -y`

## Scripted Skills

| Skill | Entry | Notes |
|-------|-------|-------|
| `article-read` | `scripts/read.sh` | `bash`, `python3`; `scripts/requirements.txt` |
| `keep` | `scripts/fetch_keep_run.py` | `python3`, `requests` |
| `jinrishici` | `scripts/fetch_poem.py` | `python3` |
| `topic-learn` | `scripts/topic_scaffold.sh`, `scripts/topic_check.sh` | bootstrap + checklist |
| `flowguard` | `scripts/workflow_state_snapshot.sh` | read-only repo snapshot |

Before changing a script: read the skill's documented commands, preserve CLI
interfaces unless the user asked for a breaking change, run `bash -n` on edited
shell scripts.

## Skill Authoring

| Task | Read |
|------|------|
| Should this be a skill? Design, taxonomy, triggers | `skill-audit/SKILL.md` |
| Implement, eval loop, package | `skill-creator/SKILL.md` |

When adding a skill: search for duplicate `name` frontmatter; add to README
catalog and install loop; add `evals/` when behavior is non-obvious.

## Session Routing (user-driven)

Do not chain skills from within a skill. The user picks the next step.

| User goal | Skill |
|-----------|-------|
| Read URL/PDF as Markdown | `article-read` |
| Write article or document | `article-write` |
| Deep topic learning assets | `topic-learn` |
| Codebase health review | `codebase-audit` |
| Remove AI tells from existing text | `humanizer` |
| Guard long multi-step work | `flowguard` |
| Review or improve a skill | `skill-audit` |
| Create or benchmark a skill | `skill-creator` |

Handoff field shapes: `flowguard/references/state-contract.md` (load only when
doing flowguard work — do not bulk-load other skill routers from there).

External session tools (not vendored): [mattpocock/skills](https://github.com/mattpocock/skills) `handoff`, `grill-me`, `caveman`, `edit-article`.

## When To Plan First

- New skill or split touching `SKILL.md` + `references/` + `scripts/`
- README / AGENTS / install loop changes across many skills
- Rewriting a skill's public contract

For a one-file fix in a single skill, edit after reading that skill only.

## Validation

After changing routers or scripts:

```bash
bash scripts/validate-skills.sh
```

Manual checks:

- YAML frontmatter has `name` and `description`; `name` matches directory.
- No duplicate symlinks under `~/.cursor/skills/`.

`validate-skills.sh` checks: name match, description, eval JSON, `bash -n` on
`scripts/*.sh`, `py_compile` on `scripts/*.py`, SKILL line-count warning (≤120
except `skill-creator`), missing evals warning (except `skill-creator`).

## Open Facts (do not guess)

- De-AI install directory is **`humanizer/`** (not `humanize/`).
- `article-read` modes: convert (default), summarize, save, preview — via `scripts/read.sh`; run `bash -n` on shell scripts after edits.
- Core quartet skills with `evals/`: all ten skills except `skill-creator` (meta; eval loop built-in).
- Run `bash scripts/validate-skills.sh` after skill or router changes.
- `improve`, `handoff`, `grill-me`, `caveman`, `edit-article` are **not** in this repo.
- SDD-lite skills live in the Cursor plugin (`~/.cursor/plugins/local/sdd-lite/skills/`) — do not duplicate into `~/.agents/skills/`.
