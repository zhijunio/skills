# Library audit checklist

Use when auditing **all skills in this repo** (not a single candidate). Pair with `assets/skill-brief-template.md` per skill.

## Per-skill pass

For each directory with `SKILL.md`:

1. **Decision** — keep / improve / split / merge / retire
2. **Category** — one primary from `skill-taxonomy.md`
3. **Name contract** — `name` frontmatter matches directory
4. **Trigger** — description has concrete user phrases, not marketing
5. **Progressive disclosure** — SKILL ≤120 lines (except `skill-creator`); details in `references/`
6. **Boundaries** — explicit "is not" list; may name sibling skills when handoff is clear
7. **Verification** — scripts + `bash -n`, or `evals/*.json`, or explicit smoke command
8. **Gotchas** — at least one non-obvious failure mode if the skill exists because agents err

## Library pass

- [ ] Core quartet (读/写/学/审) boundaries documented in `README.md` + `AGENTS.md`
- [ ] Install loop matches on-disk directories
- [ ] No legacy symlink names documented without cleanup command
- [ ] Scripted skills listed with deps
- [ ] External skills (improve, mattpocock) not vendored but linked
- [ ] Run `scripts/validate-skills.sh` — zero errors

## Output format

```markdown
## Library summary
Decision: improve | ...
P0: ...
P1: ...

## Per skill
### <name>
- Decision / Category / Risks
- Smallest file changes
```
