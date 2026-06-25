# Recon (Phase 1)

Run before any lens audit.

## Target

- Confirm `{TARGET_DIR}` (user path or repo root). All commands run from there.
- Record branch, `git status --short`, merge-base if branch-scoped audit requested.

## Read first

- `README`, `AGENTS.md` / `CLAUDE.md`, `CONTRIBUTING`
- Root manifests: `package.json`, `pyproject.toml`, `go.mod`, `Cargo.toml`, `pom.xml`, …
- CI config — what gates exist vs what architecture claims to enforce
- **Intent docs** (additive): `docs/adr/`, `docs/adrs/`, `docs/decisions/`, `DESIGN.md`, `PRODUCT.md`, architecture diagrams in `docs/`

Carry ADR decisions forward as **settled tradeoffs** during vet — do not re-litigate unless code contradicts the ADR.

## Capture recon card

```text
stack:
entrypoints:
packages_or_crates:
verification_commands:  # build, test, lint — exact commands
hotspot_dirs:           # churn or size from git log / largest modules
documented_shape:         # what docs claim vs what tree shows
open_questions:
```

## Shape classification (pick primary)

Library · CLI/tool · HTTP API · long-running runtime · full-stack app · agent/workflow system · batch pipeline · other (name it).

Shape drives which lenses get extra depth (e.g. full-stack → contract lens spans FE/BE).
