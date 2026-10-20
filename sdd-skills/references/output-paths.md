# SDD output paths

Follow the consuming repository's documented paths first. These defaults apply only when the project has no existing convention. The paths below are relative to the project root.

## Durable product and design documents

| Artifact | Default path | Ownership |
| --- | --- | --- |
| Product PRD | `docs/prd/<product-slug>.md` | One master PRD per product; revise it as requirements and delivery batches evolve. Create a separate PRD only for independently managed goals, users, success measures, and approval lifecycle. |
| Design readiness assessment (optional standalone record) | `docs/design/<scope-slug>.md` | Use only when the assessment is substantial, covers multiple specs, the user requests a separate record, or repository convention requires one. Choose a scope slug that does not collide with a canonical domain design file. For a routine scope, record the concise Ready result and canonical contract links in each resulting spec instead. |
| Domain design | `docs/design/<domain-or-capability-slug>.md` | Canonical home for domain rules and models; revise the owning contract in place. |
| Shared glossary (optional) | `GLOSSARY.md` | Create only when multiple cross-cutting terms need a shared home; otherwise define terms in the owning domain document. Follow an existing project convention if it places the glossary elsewhere. |
| Architecture decision record (conditional) | `docs/adr/<decision-slug>.md` | Record an accepted, durable architecture decision when it affects multiple features or constrains future design. Follow the project's ADR convention; do not create ADRs for routine domain/API modeling or unresolved options. |
| API contract | `docs/api.md` | Keep one canonical API contract by default. Split into `docs/api/<capability-slug>.md` only when the contract is too large or the repository already uses that layout. |

## Feature delivery documents

For a single-product repository, group each feature slice's spec and plan together:

```text
docs/sdd/
├── breakdown.md                    # Optional index for multi-spec initiatives
├── 001-device-registration/
│   ├── spec.md
│   └── plan.md
└── 002-alert-handling/
    ├── spec.md
    └── plan.md
```

| Artifact | Default path | Ownership |
| --- | --- | --- |
| Feature spec | `docs/sdd/<NNN>-<feature-slug>/spec.md` | One directory per feature slice; revise the existing spec in place. |
| Implementation plan | `docs/sdd/<NNN>-<feature-slug>/plan.md` | Keep beside its spec; revise in place and append build evidence. |
| Spec breakdown map | `docs/sdd/breakdown.md` | Optional index mapping a multi-spec initiative to its feature slices, covered PRD requirements, dependencies, and confirmed implementation order. Present in chat by default; persist only when useful or requested. Skip for a single spec. |

For multiple independently managed products or initiatives in one repository, add one grouping level and scope numbering within it:

```text
docs/sdd/<initiative-slug>/
├── breakdown.md
└── 001-feature-slug/
    ├── spec.md
    └── plan.md
```

## Optional working notes and reports

These outputs are conversational by default. Save them only when requested or when repository convention requires it.

| Artifact | Optional saved path | Notes |
| --- | --- | --- |
| Discovery summary | `docs/discovery/<initiative-slug>.md` | One provisional summary per initiative; revise in place. It is not a PRD or workflow gate. |
| Review report | `docs/reviews/<scope-slug>-<YYYY-MM-DD>.md` | Point-in-time report; add a numeric suffix if multiple reports share a scope and date. |
| Improvement scan | `docs/improvements/<scope-slug>-<YYYY-MM-DD>.md` | Point-in-time report; add a numeric suffix if multiple scans share a scope and date. |

`sdd-build` does not create a separate report by default; it appends verification evidence to the persisted plan for the implemented spec.

## Naming and maintenance

- Use lowercase kebab-case slugs and stable filenames for evolving artifacts. Do not put dates or semantic versions in PRD, design, API, spec, or plan paths; put version/history/update details in the document as required by the owning skill.
- Assign zero-padded three-digit feature directory numbers (`001`, `002`, ...) from the confirmed implementation order. Numbers help browse the work; they do not imply priority or dependencies, which remain explicit in the PRD and breakdown map.
- If the agreed implementation order changes, update the breakdown map and affected directory numbers and references together.
- Do not create parallel PRDs, specs, plans, domain contracts, or API contracts. Use existing canonical documents.
- Skill-maintainer files (`SKILL.md`, `references/template.md`, and `references/example.md`) stay in the skill distribution and are not consuming-project outputs.
