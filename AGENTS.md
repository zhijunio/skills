# Agent Instructions — zhijunio-skills

This repository is a **Cursor / Codex Agent Skills library**. Each skill is a
directory with `SKILL.md` as the router. This file is the repo-wide onboarding
router; it does not replace per-skill instructions.

## Precedence

1. User instructions in the current session
2. This `AGENTS.md`
3. The target skill's `SKILL.md` and its `references/` / `scripts/`
4. `README.md` (human catalog and install examples — may lag git; see Open Facts)

## Start Here

1. Confirm the goal is skill authoring, skill editing, or repo maintenance — not
   application code in another repo.
2. List tracked skills when unsure:

```bash
git ls-files '*/SKILL.md' | sed 's|/SKILL.md||' | sort
```

3. Before creating a skill, read `skill-creator/SKILL.md` and search for an
   existing skill with the same `name` frontmatter.
4. Before editing a skill, read its `SKILL.md` and any linked `references/`.
5. For Codex CLI collaboration, read `codex-agent/AGENTS.md`.

## Layout Contract

```text
<skill-name>/
  SKILL.md           # required router: YAML name + description
  references/        # optional playbooks
  scripts/           # optional shell / Python entry points
  evals/             # optional behavior checks (e.g. agentsmd-scaffold)
shared/              # repo-wide references linked as ../shared/*.md
```

Rules:

- Keep `SKILL.md` concise; move long policy tables into `references/`.
- Frontmatter `name` must match the install directory name.
- Do not invent APIs, scripts, or install paths not present in this repo.
- Do not edit `LICENSE` or acknowledgement sections unless the user asked.
- **Do not reference other skills by name, path, or “use skill X” in `SKILL.md` or `references/`** — describe capabilities in plain language; the user or repo router chooses skills.

## Install And Discovery

Cursor indexes `~/.cursor/skills/<name>/`. Symlink from this repo:

```bash
REPO=~/github/zhijunio-skills   # adjust to your clone path
mkdir -p ~/.cursor/skills
for skill in agentsmd-scaffold caveman codex-agent fetch-content flowguard \
  grill-me handoff improve jinrishici keep mj-writer rewrite-article \
  skill-creator strategic-compact tech-mastery vibeguard; do
  ln -sfn "$REPO/$skill" ~/.cursor/skills/$skill
done
```

Do **not** also symlink the same skill into `~/.agents/skills/` — duplicates load
twice in Agent. See `README.md` for stale-entry cleanup commands.

After adding or renaming a skill, update `README.md` Skills table and install loop
in the same change set when possible.

## Tracked Skills (git source of truth)

`agentsmd-scaffold`, `caveman`, `codex-agent`, `fetch-content`, `flowguard`,
`grill-me`, `handoff`, `improve`, `jinrishici`, `keep`, `mj-writer`,
`rewrite-article`, `skill-creator`, `strategic-compact`, `tech-mastery`, `vibeguard`,

## Scripted Skills

| Skill | Scripts | Notes |
|-------|---------|-------|
| `fetch-content` | `fetch-content/scripts/*` | `bash`, `python3`; see `requirements-fetch.txt` |
| `keep` | `keep/scripts/*` | `python3`; see `requirements.txt` |
| `jinrishici` | `jinrishici/scripts/fetch_poem.py` | `python3` |
| `flowguard` | `flowguard/scripts/workflow_state_snapshot.sh` | read-only repo snapshot |
| `agentsmd-scaffold` | `agentsmd-scaffold/scripts/scan_repo_context.py` | read-only context scan |
| `codex-agent` | `codex-agent/scripts/*` | requires Codex CLI; see scoped AGENTS |

Before changing a script: read the skill's documented commands, preserve CLI
interfaces unless the user asked for a breaking change, and run `bash -n` on
edited shell scripts.

## Lifecycle Routing (other repos)

When work leaves this skill library, **do not chain skills from within a skill**. The user (or session router) picks the next workflow explicitly.

| Situation | Capability (user must invoke explicitly) |
|-----------|------------------------------------------|
| Long / multi-step / resume work | Lifecycle guard + state verification |
| Task kickoff / anti-hallucination contract | Task contract before edits |
| Context compaction at phase boundaries | Phase-boundary compression summary |
| Generate `AGENTS.md` for another repo | Repo context scan + scoped AGENTS draft |
| Session handoff to a fresh agent | OS-temp handoff document |

Checkpoint and handoff field shapes: `flowguard/references/state-contract.md` (same skill only — do not load other skill routers from there).

## When To Plan First

- New skill or skill split touching `SKILL.md` + `references/` + `scripts/`
- README catalog / install loop changes across many skills
- Rewriting an existing skill's public contract

For a one-file typo fix in a single skill, edit directly after reading that skill.

## Validation

After changing agent instructions or skill routers:

```bash
python3 agentsmd-scaffold/scripts/scan_repo_context.py .
```

Manual checks:

- Changed `SKILL.md` files have valid YAML frontmatter with `name` and
  `description`.
- Shell script edits pass `bash -n <path>`.
- No duplicated install symlinks (`~/.cursor/skills` only).

There is no repo CI yet for frontmatter linting.

## Open Facts (do not guess)

- `README.md` still links to `personal/`, `productivity/`, `util/`, and
  `humanize/` paths that are not in git; use root-level skill directories above
  until README is reconciled.
- Install name for de-AI polish is **`humanizer/`** (not `humanize/`).
- Shared delivery/BDD references live under **`shared/`** (`delivery-base.md`, `bdd-guide.md`); `fixflow` and `optflow` link there — do not duplicate copies under each skill.
- Install examples may use a different clone path than your machine; set `REPO`
  explicitly when symlinking.

## Escalation

- Scoped Codex CLI rules: `codex-agent/AGENTS.md`
- AGENTS scaffold for other repositories: follow the agentsmd-scaffold workflow in the current session; do not bulk-normalize unrelated repos from here
