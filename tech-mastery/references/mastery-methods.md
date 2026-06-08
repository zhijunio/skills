# Mastery methods

Working method for **tech-mastery** (slim v2). Entry: [`SKILL.md`](../SKILL.md).

## Goal

Make one technical knowledge point **teachable, reviewable, answerable, and retainable** — not an encyclopedia dump.

## Modes

| Mode | Use |
|------|-----|
| `new` | First pass on a topic directory |
| `refresh` | Update, **merge** existing drafts, or **gap-fill** missing files |

State in `TOPIC.yaml`: `mode`, `phase`, `gaps`.

## Mission (in mastery-map)

See [`mission-format.md`](mission-format.md). Mission is the compass; without Success + Out of scope, do not collect sources.

## Source layering

See [`resources-format.md`](resources-format.md).

- L1 / L2 ground key claims
- L4 explains only
- INDEX + Gaps + Open in `sources/`

Use **fetch-content**; save under `sources/`.

## Mastery map

Single file `<slug>-mastery-map.md` — see [`../templates/mastery-map.md`](../templates/mastery-map.md).

Before Write: definition, mental model, flow, version boundaries, sharp edges, verification plan, topic gaps for next refresh.

## Verification

At least one of: demo, source/spec walkthrough, version compare, mistake repro.

Record in mastery-map **Verification**; mirror **`runtime`** (narrative) and **`verified_on`** (demo machine) in `TOPIC.yaml`; set **`code_repo`** when runnable source lives outside the vault. See [`verify-format.md`](verify-format.md).

Runnable demos: paste **real output** in `examples/README.md` (commands may target `code_repo`). After Verify, sync the command into README, practice, and main article minimal-verify section.

## Outputs

| Artifact | Role |
|----------|------|
| Main article | Full model |
| 0–2 side articles | Mechanism, practice, integration, design — roles in README |
| `reference/` | ≤5 min review |
| Glossary | Terminology lock |
| Interview | Speakable mastery |
| Learning records | ADR-style deltas across runs |

## Review paths (README)

- **Recall:** reference → glossary → self-test
- **Explain:** main → interview

## When to split a topic

>3 independent core mechanisms → propose separate slugs before Source.

## Related skills

fetch-content (Source), humanize (Humanize), rewrite-article (structure), grill-me (Scope), handoff (multi-session).
