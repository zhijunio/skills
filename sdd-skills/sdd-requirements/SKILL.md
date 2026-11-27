---
name: sdd-requirements
description: Derive or reconcile the use-case inventory from the current approved PRD. Preserve use-case IDs, dependencies, and implementation history.
---

# Maintain requirements

1. Read the current Approved PRD and existing requirements. Use user-supplied or repository paths, defaulting to `docs/PRD.md` and `docs/requirements.md`. If PRD approval is missing, return to [sdd-prd](../sdd-prd/SKILL.md) before editing.
2. Translate approved capabilities into user outcomes. Capture direct dependencies and concise, observable acceptance criteria supported by the PRD. Put uncertain behavior in Open Questions. Detail specific delivery scenarios and tests in the spec.
3. Reconcile the inventory in place. Preserve valid `UC-###` IDs and recorded statuses. Assign new UCs the next unused ID and `NOT_IMPLEMENTED`. For scope changes, splits, merges, or retirement, record the date, approved PRD version, affected IDs and any mappings in Revision History. Keep past IDs reserved.
4. Check PRD coverage, traceability, dependency references, and agreement between the summary and detail entries. Report the output path and material changes.

## Document shape

- `# <Product Name> Requirements` and a short introduction naming the approved PRD path and version, exclusions, and status meanings.
- `## Use Case Summary` with columns `UseCase ID`, `Use Case Title`, and `Status`.
- `## Detailed Use Cases` with one entry per row, in the same order. Use `None` for no dependency and `TBD` for an unresolved prerequisite.

```markdown
### UC-001 — Verb-Led Title

- **Status:** NOT_IMPLEMENTED
- **Depends On:** None
- **Description:** Actor, intent, and outcome.
- **Acceptance Criteria:**
  - Observable behavior supported by the PRD.
```

Use Open Questions, Pending Reconciliation, and Revision History when applicable.

## Pending reconciliation

When this skill or `sdd-spec` changes an existing UC's behavior, acceptance criteria, or dependencies in a way that can invalidate prior evidence, record its ID, change, and affected specs or dependent UCs under `## Pending Reconciliation`. Keep the prior evidence. A `COMPLETED` UC with a pending entry cannot satisfy an external dependency. Review linked specs, then let `sdd-implement` clear the entry once current verification covers the affected behavior.
