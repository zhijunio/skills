# SDD Lifecycle and Artifact Contract (E02)

Status: Proposed for maintainer approval; not yet enforced by skill instructions.
Date: 2026-11-28
Related: [Engineering workflow](../engineering-workflow.md), [capability audit](../audits/engineering-capability-audit.md), [Phase 02 plan](../plans/phase-02-engineering-workflow.md).

## 1. Authority and scope

| Artifact | Authoritative for | Owner |
| --- | --- | --- |
| PRD | Product intent, scope, exclusions, product-level outcomes | Product requirements capability |
| Use-case index | Stable use-case identity, title and implementation status | Use-case decomposition; implementation updates status |
| Behavior specification | Observable behavior, rules, acceptance criteria and dependency IDs | Behavior specification capability |
| Architecture / ADR | Approved technical decisions and constraints | System design and existing decision owners |
| Implementation plan | Task ordering, affected components and verification approach | Implementation planning |
| Test strategy | Risks and required evidence | Test strategy |
| Execution evidence | Actual checks, results and residual limitations | Implementer / verifier |

Honor established paths and schemas. Default PRD: docs/PRD.md; index: docs/requirements.md; specifications: docs/specs/. Do not create duplicate authorities. Small changes need not initialize SDD; when an existing project uses SDD, do not bypass its approved contracts.

## 2. PRD approval

For new documents using the existing default metadata convention:

| Event | status | approval |
| --- | --- | --- |
| Summary accepted and PRD drafted | draft | pending |
| Exact PRD revision explicitly approved by product owner | approved | approved |
| Material scope revision awaiting approval | draft | pending |

The second row proposes a new explicit convention. Summary approval authorizes drafting, not the approval of text that has not yet been reviewed. The approval request must identify the actual document and scope; an unambiguous instruction explicitly approving that revision is sufficient. Do not require redundant confirmation.

Preserve legacy approved documents and project-specific lifecycle fields where their authority is clear. A missing metadata field is not proof of rejection or approval. Ask about authority only when it cannot be established from the user's instructions or documented evidence. Never auto-migrate existing documents solely to add metadata.

An unapproved PRD may be reviewed or used for a clearly labeled preview of proposed use cases; do not overwrite the authoritative index from it. Product owner approval is required to promote the proposed scope into the authoritative index. An approved PRD does not automatically approve every newly derived detailed behavior.

## 3. Use-case state

Keep the current codes and icons. The index is canonical; the specification status is a compatibility mirror, not an independent state machine.

| From | To | Preconditions |
| --- | --- | --- |
| New | TODO | Confirmed scope and assigned stable ID |
| TODO | IN_PROGRESS | Approved behavior, authorized implementation, resolved prerequisites |
| IN_PROGRESS | IN_PROGRESS | Work incomplete, blocked, failed or resumed |
| IN_PROGRESS | DONE | Acceptance criteria satisfied, required verification passed, evidence recorded |
| DONE | IN_PROGRESS | Explicitly authorized repair or change, reconciled specification, prior completion evidence identified as stale for affected scope |

A discovery of a defect or proposed scope change alone does not authorize code edits or status changes. Report that previous evidence may no longer establish current correctness; request the needed decision if authorization is unclear. A DONE item with a repair request may be reopened after prerequisites are checked, before implementation edits. Do not claim a stale DONE result is current verified evidence.

No new BLOCKED, TESTING or REVIEWING codes are proposed. Record blockers and check results in a concise implementation record or existing issue/plan; distinguish NOT RUN from FAIL and PASS. A required unavailable check prevents DONE. Do not mark a dependency DONE merely because a caller requests implementation of its dependent.

## 4. Identity, synchronization and dependency rules

- Keep IDs unique and stable across index, specs, plans and tests. Allocate a new ID after the highest known ID, including retired IDs when a register exists; do not reuse retired IDs. Do not silently renumber, delete or merge existing use cases.
- Resolve each dependency to a canonical ID; identify self-dependencies, missing IDs and cycles before proceeding. Dependencies required for implementation must be DONE with applicable evidence; resolve approved exceptions explicitly without silently changing scope.
- Before modifying status, verify both documents and the icon agree. Update the intended index row and mirror together as one logical change, then read both back before editing code or reporting completion.
- If a partial write occurs, stop; report the mismatch and restore the prior consistent values only if no concurrent edits would be overwritten. Otherwise obtain reconciliation. Never report a successful transition until verified.
- Do not reset statuses during regeneration or specification edits. A changed behavior that invalidates acceptance evidence requires an explicit change decision and a reopening plan, not automatic status mutation by the drafting skill.

## 5. Change control

1. Classify: clarification without changed behavior; in-scope elaboration; changed approved behavior; new/out-of-scope product capability; or defect against existing behavior.
2. Identify the authoritative source and affected IDs, dependencies, specs, plans, contracts and test evidence. Distinguish facts from hypotheses.
3. For product scope changes, obtain approval of the revised product authority first. For behavior changes within scope, obtain approval of the affected behavior; for defects against existing approved behavior, do not redefine the requirement to match broken code.
4. Reconcile the index and specs deliberately, preserving identity and history. Record affected downstream work and evidence that must be refreshed. Do not silently delete retired use cases or impose a new schema; obtain a migration decision when needed.
5. Reopen and implement only authorized use cases, verify relevant dependency effects, and record actual evidence before completion.

Do not expand a single-use-case implementation into unapproved work on dependent use cases. Report downstream impact and route each additional implementation separately.

## 6. Cross-artifact preflight

Before implementing a use case, check product scope authority when applicable, unique ID and spec, approval of behavior, aligned title/status/icon, resolvable acyclic dependencies, acceptance observability, and conflicting architecture or plan decisions. A read-only consistency review may report gaps without modifying files. Existing projects without a PRD may use explicitly authorized feature scope rather than forcing product documentation.

## 7. Acceptance scenarios (proposed, NOT RUN)

| ID | Scenario | Expected outcome |
| --- | --- | --- |
| C01 | User confirms brainstorming summary | Draft PRD, not approved |
| C02 | User explicitly approves exact draft | Approval recorded; eligible for decomposition |
| C03 | Decomposition requested against unapproved draft | Preview only or request approval; authoritative index unchanged |
| C04 | Existing legacy PRD explicitly established as approved | Preserve schema; no forced metadata migration |
| C05 | New feature conflicts with approved PRD | Report scope change; no silent index or spec expansion |
| C06 | Regenerate index with existing DONE and new scope | Preserve stable IDs/status; new approved scope TODO |
| C07 | Missing dependency or cyclic dependency | Block affected implementation; report exact conflict |
| C08 | Status mirror mismatch or interrupted update | Stop, reconcile safely; no implementation until consistent |
| C09 | Authorized repair of DONE behavior | Reopen to IN_PROGRESS before edits; refresh evidence |
| C10 | Required test fails or cannot run | Remain IN_PROGRESS; truthful evidence |
| C11 | Changed behavior affects downstream use cases | Report affected IDs and invalidated evidence; no unauthorized implementation |
| C12 | Lightweight fix in non-SDD project | Follow existing conventions; no forced PRD/index creation |

## 8. Migration and decisions

Proposed skill names: product-requirements, prd-to-use-cases, behavior-specification and implement-use-case. Rename after semantics are approved, update invocation configurations, references, provenance and catalog, and verify distribution. Preserve applicable notices; existing SDD notice gap remains unresolved and publication remains blocked.

Maintainer decisions required before enforcing material lifecycle semantics: approve or revise the proposed PRD approval values, DONE reopening rule, and dependency exception policy. Whether to remove the mirrored specification status in a future schema revision is deferred; preserve current target-project compatibility now. No runtime evaluation, skill migration or release is claimed by this proposal.
