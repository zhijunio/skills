# Release notes / changelog

## Prep

1. Read existing **`CHANGELOG.md`**, **`README`**, or **`AGENTS.md` / `CLAUDE.md`** release conventions.
2. If on GitHub: `gh release view --json body -R owner/repo` for **latest** tone, length, density.
3. **Breaking** must stand out; internal refactors with no user-visible change → Fixes or omit.

## Structure (trim to project habit)

Suggested blocks (bilingual one-liners OK: user impact before tech detail):

- **Breaking changes**
- **New features**
- **Fixes & improvements**
- **Deprecations**

## Per bullet

- **One user-facing line**: what symptom fixed or what you can do now — not raw PR title stacks.
- **Numbering / bullets** consistent with past releases (don't switch from list to essay mid-stream).
- **No invented version or date**; if tag unknown use «unreleased» or leave blank.

## Bilingual releases

- Per item: **ZH then EN one-liner**, or **EN then ZH** — pick one pattern for the whole note.
- Product names and CLI subcommands identical both sides.

## Omit from release notes

- Long architecture vision unrelated to the version (unless user asked).
- «Major performance boost» without ticket/PR or benchmark reference.
