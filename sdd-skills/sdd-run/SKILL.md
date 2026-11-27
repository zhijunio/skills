---
name: sdd-run
description: Run the SDD workflow for a requested scope when the user explicitly asks for end-to-end delivery.
---

# Run SDD end to end

## Preflight

Before resuming, confirm the requested product and delivery scope match the PRD, requirements, specs, and repository being changed. Check product identity and terminology in the documents against the relevant source or repository purpose; a valid document path alone does not establish that the artifacts belong to this task. If they conflict, report the mismatch and ask which product or workspace is authoritative. Keep existing artifacts unchanged until scope is resolved.

For an explicitly requested workflow demonstration, use an isolated fixture when the real artifacts cannot pass preflight. Label approvals and evidence in the fixture as simulated. Never change approval or implementation status in the real project to make a demonstration proceed.

For small fixes and behavior-preserving refactors that meet the [lightweight criteria](../sdd-implement/SKILL.md#small-fixes-and-refactors), go directly to `sdd-implement` and `sdd-review`. Review the delivered diff against the existing behavior and verification. On resume, reuse evidence only if it covers the same baseline and exact delivered diff. Continue with the approved spec flow if the expected behavior needs a decision or change.

For spec delivery, read the existing artifacts and resume at the earliest stage that lacks current evidence. Follow the linked skill and wait for required user approvals. Reuse current artifacts and work one approved spec at a time.

| Need | Stage |
| --- | --- |
| The requested product scope lacks current approval | [sdd-prd](../sdd-prd/SKILL.md) |
| Use cases are missing or out of date against the approved PRD | [sdd-requirements](../sdd-requirements/SKILL.md) |
| The selected work lacks a current, Ready, Approved spec | [sdd-spec](../sdd-spec/SKILL.md) |
| The approved behavior lacks evidence from the delivered implementation | [sdd-implement](../sdd-implement/SKILL.md) |
| The delivered increment lacks a matching review record | [sdd-review](../sdd-review/SKILL.md) |

For a spec, reuse review only when both the approved spec version and exact delivered increment match. Record the result in the spec if implementation has not already done so. Fix in-scope defects through `sdd-implement` and review the updated increment.

Report the delivered scope, evidence, and remaining findings. When waiting for approval or a blocking decision, identify the artifact and the next action.
