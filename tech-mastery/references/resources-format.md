# Sources & INDEX format

Use with **fetch-content** at Source step. Files under `sources/`; index at `sources/INDEX.md`.

## Layering

| Layer | Examples (any domain) |
|-------|------------------------|
| **L1** | RFC, W3C, JLS, OpenJDK tag, IETF, vendor normative docs |
| **L2** | Framework/product official docs or source (Spring, K8s, Postgres) |
| **L3** | Maintainer blogs, design docs, release notes |
| **L4** | Textbooks, articles — explain only; cannot alone support key claims |

Prefer higher layer on conflict unless a lower layer demonstrates behavior.

## INDEX.md template

```markdown
# Sources — {Topic}

## L1 / L2

- [Title](sources/l1-foo.md) — {one line: use when / supports main §N}

## L3 / L4 (supporting)

- …

## Gaps

- {What normative evidence is still missing}

## Open

- {Claims that depend on future verify or user input}
```

## File frontmatter (recommended)

```yaml
url: https://…
layer: L1
fetched_at: 2026-06-05
version_or_tag: jdk-21+35 | rfc9110 | …
```

## Rules

- **Annotate every entry** — bare links rot.
- **Prune** wrong or off-mission sources; do not bury them.
- **No Write** on core claims while a blocking Gap remains (unless scoped down or marked Open in main article).
