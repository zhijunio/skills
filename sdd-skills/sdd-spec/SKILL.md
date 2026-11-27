---
name: sdd-spec
description: Turn approved product scope and its use cases into feature specs with testable acceptance criteria. Resolve gaps in behavior and shared contracts before spec approval.
---

# Create feature specs

1. Read the current Approved PRD, the requirements document, and relevant existing specs. Use [sdd-prd](../sdd-prd/SKILL.md) for missing approval or changes to product scope. Use [sdd-requirements](../sdd-requirements/SKILL.md) when the use-case inventory is missing or needs reconciliation with the approved PRD.
2. Select the requested `UC-###` IDs and check coverage, dependencies, and observable outcomes. Reuse decisions already made. Ask only about unresolved behavior that affects delivery. For related decisions, ask together when they can be answered independently. Present any material use-case changes for confirmation and update requirements under its [reconciliation rules](../sdd-requirements/SKILL.md#pending-reconciliation).
3. Check domain and API readiness for the selected scope. Settle shared domain models and consumer-facing interfaces before spec approval. Record what covers the behavior, what change was accepted, or why it does not apply. Use [shared contracts](references/shared-contracts.md) for changes to shared rules or interfaces. Mark the affected scope Ready when its contract decisions are settled and Blocked while a required decision remains open.
4. Use the [spec template](references/template.md) and [example](references/example.md). Give each independently deliverable outcome its own spec when that helps the work. For several specs, show the UC coverage, dependencies, and delivery order before drafting. Write stable `AC-n` criteria for observable success, failures, and boundaries. Link the PRD version, UCs, and canonical contracts. When it materially affects verification, record the public interface or user flow through which the behavior can be proved.
5. Present the complete Draft spec for explicit approval when readiness is Ready. Record the approval and save the spec in place under the [path policy](../references/output-paths.md). Start drafts at `0.1`, use `1.0` on first approval, and start implementation at `NOT_IMPLEMENTED` for a new spec. Hand approved specs to [sdd-implement](../sdd-implement/SKILL.md).

For UI work, prototype before spec approval when an unresolved flow, screen state, or error recovery would change acceptance criteria. Use `prototype` when available. Start with a sketch and make it clickable only if someone needs to try the interaction. Review it with the user, write the agreed behavior into the ACs, and link the agreed prototype if saved. Reuse established interaction patterns for routine screens. Route changes to approved product scope to `sdd-prd`.

For substantive AC or constraint changes, return the spec to Draft, then increment its minor version on re-approval. Preserve prior implementation and review records. Reset the current implementation status to `NOT_IMPLEMENTED` when prior evidence no longer covers the revision, and flag the affected UCs for reconciliation. Refresh readiness if a shared contract changed. Log wording-only clarifications without a version bump.

For independent features, use subagents to inspect coverage or draft separate specs when the task merits parallel work. Give each subagent a separate slice and draft. The primary agent handles user decisions, canonical writes, and approval records.
