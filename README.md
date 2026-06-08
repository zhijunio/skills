# zhijunio-skills

**Agent Skills** for **Cursor** and similar AI coding assistants: one directory per skill, with **`SKILL.md`** as the entry (`name` / `description` in YAML for discovery). Some skills add **`references/`** playbooks and **`scripts/`** for reproducible runs.

## Skills

| Directory | Summary |
|-----------|---------|
| [**jinrishici**](jinrishici/SKILL.md) | Today’s poem API; fixed Chinese display format. |
| [**keep**](keep/SKILL.md) | Sync Keep runs to Garmin-style `running.json` (`keep/scripts/`). |
| [**tech-mastery**](tech-mastery/SKILL.md) | One topic end-to-end: `new` or `refresh` (merge/gap-fill), mastery-map, sources, verify, article, interview, learning-records, humanize. |
| [**fetch-content**](fetch-content/SKILL.md) | URL / PDF → Markdown; `fetch-content/references/` — decision tree, CN platforms, MCP, **tech-mastery** handoff. |
| [**humanize**](humanize/SKILL.md) | De-AI polish; `write-zh.md` / `write-en.md` replacement tables; **L1–L4** `write-qa-layers.md` (optional 质检报告). |
| [**rewrite-article**](rewrite-article/SKILL.md) | Split by headings, DAG reorder, section rewrite; see `rewrite-article/references/` for DAG examples and **humanize** handoff. |
| [**grill-me**](grill-me/SKILL.md) | One question at a time on a plan/design, with recommended answers, until the decision tree aligns. |
| [**handoff**](handoff/SKILL.md) | Compress the session to a handoff doc (temp file + path refs) for the next agent. |

## Optional chains

```text
fetch-content → tech-mastery (Source → … → Humanize)
                    ↘                      ↗
                 rewrite-article (reorder) ──┘

grill-me (stress-test plan) ──→ tech-mastery / sdd-plan (user switches to persist)
handoff (new session) ──→ points at fetch-content / tech-mastery / humanize / rewrite-article, etc.
```

- **tech-mastery** Source: **fetch-content** + `tech-mastery-sources.md`; files under topic `sources/`.
- **tech-mastery** Write → **Interviewize** → **humanize**; messy structure → **rewrite-article** first.
- **tech-mastery** output: `second-brain/23-work/research/<slug>/` (or user-given topic dir).
- **grill-me** / **handoff** are outside SDD increment contracts; app repos use installed `sdd-plan`, etc.

## Usage

### Cursor install (one path only)

Cursor indexes **`~/.cursor/skills/<name>/`**. Symlink from this repo — **do not** also link into `~/.agents/skills/`, or the same skill loads twice.

```bash
REPO=~/github/zhijunio-skills
mkdir -p ~/.cursor/skills
for skill in fetch-content grill-me handoff humanize jinrishici keep tech-mastery rewrite-article; do
  ln -sfn "$REPO/$skill" ~/.cursor/skills/$skill
done
```

**SDD-lite:** use the Cursor plugin (`~/.cursor/plugins/local/sdd-lite/skills/`). Do not duplicate those into `~/.agents/skills/`.

Remove stale duplicates if you see doubled skills in Agent:

```bash
rm -f ~/.agents/skills/{fetch-content,grill-me,handoff,humanize,jinrishici,keep,tech-mastery,rewrite-article,research-write,sdd-*}
rm -rf ~/.cursor/skills/research-write ~/.agents/skills/research-write
```

Then start a **new Agent chat** so the skill index refreshes.

### General

1. Skills usually trigger from **`description`** match; you can also name a skill in the prompt.
2. Scripted skills (**fetch-content**, **keep**) document commands in `SKILL.md`; install deps (`bash`, `python3`, `requests`, …) on the machine.

## Layout

```text
<skill>/
  SKILL.md           # entry: name, description, boundaries
  references/        # optional playbooks
  scripts/           # optional shell / Python
```

## Acknowledgements

Early **fetch-content / humanize** layering drew on **[Waza](https://github.com/tw93/Waza)** (MIT). **handoff**, **grill-me**, and **rewrite-article** flows reference **[mattpocock/skills](https://github.com/mattpocock/skills)** (MIT). This repo’s `SKILL.md`, `references/`, and `scripts/` are maintained independently; **credits do not imply 1:1 parity** with upstream installs.

## License

**[MIT License](LICENSE)** for the repo. Per-file third-party notices in skill bodies or scripts apply where stated.

---

Open an Issue if you want install screenshots or CI checks for SKILL frontmatter.
