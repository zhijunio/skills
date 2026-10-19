# <Title> Implementation Plan

<!--
  Plan writing guide:
  - Each slice must be independently testable and deliver observable behavior
    (user or external system) plus a verification command — not layer-only scaffolds.
  - Prefer vertical slices over horizontal layers. A single thin slice is valid
    when it covers every AC; do not invent slices for ceremony.
  - Keep slices small enough to complete and verify independently; split a slice when it bundles unrelated outcomes, not to meet a time estimate.
  - Every acceptance criterion (`AC-n`) from the spec must be covered by at least
    one slice. The `AC-n` criteria are the delivery contract.
  - One plan covers one approved spec. For a multi-spec initiative, create a
    separate plan per spec and record only real cross-spec dependencies.
  - Risks / Dependencies (optional): build-affecting risks, external dependencies, or rollback
    notes that change slice order or verification — add this section only when needed;
    do not leave an empty heading in the plan file.
  - During build, only flip "Done" to true after verification passes; append results at the bottom.
-->

**Status:** Draft / Approved
**Approval record:** <Pending / approved by user on date>
**Spec:** `<relative path to approved spec file>`

<!--
  Optional section — include only when build-affecting risks or dependencies exist:

  ## Risks / Dependencies

  - The external test service must be available before verification.
  - Slice 2 depends on the interface introduced in Slice 1.
-->

## Slice 1: <Observable behavior — what the user or system can do after this slice>

<!--
  Goal: what this slice accomplishes in one sentence.
  Acceptance: which AC(s) this slice covers (e.g., AC-1, AC-2).
  Depends on: prior slice numbers, or "None" if this is the first slice.
  Test or proof: the failing test to write first, or a concrete alternative
    proof for non-testable changes.
  Implementation outline: files to create/modify, key functions, data changes.
    Keep it high-level — not line-by-line code.
  Verification: the exact project-specific command to run.
  Done: false (set to true after verification passes).
-->

- Goal:
- Acceptance: AC-1
- Depends on: None
- Test or proof:
- Implementation outline:
- Verification:
- Done: false

<!-- Repeat the Slice section only when needed; one slice is valid if it covers every AC. -->

<!--
  Material deviations during build:
  - If a slice boundary changes, record the new boundary and return to sdd-plan.
  - If an acceptance criterion changes, record it and return to sdd-spec.
  - Keep a running log of verified slices at the bottom of this file during build.
-->

## Appendix

### Related documents

- Parent PRD: <path>
- Ready design assessment: <path and covered requirement IDs>
- Dependencies: <prerequisite spec paths, or none>

### Supporting material (optional)

- <Build-affecting evidence or reference not already captured in a slice; omit when none>

### Build evidence (add on the first build update)

| Date | Slice | Verification command and result | Deviation |
| --- | --- | --- | --- |
| <date> | <Slice ID> | <Command and outcome> | <None or recorded deviation> |

### Revision history

| Date | Reason and affected slices | Approval |
| --- | --- | --- |
| <date> | <Initial plan / reason; slice IDs> | <Pending / approved by user on date> |
