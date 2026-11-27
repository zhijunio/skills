# SDD skills

**[sdd-prd](sdd-prd/SKILL.md) → [sdd-requirements](sdd-requirements/SKILL.md) → [sdd-spec](sdd-spec/SKILL.md) → [sdd-implement](sdd-implement/SKILL.md) → [sdd-review](sdd-review/SKILL.md)**

| Skill | Result |
| --- | --- |
| [sdd-prd](sdd-prd/SKILL.md) | One master PRD with approved product scope |
| [sdd-requirements](sdd-requirements/SKILL.md) | Use-case inventory with stable IDs and dependencies |
| [sdd-spec](sdd-spec/SKILL.md) | Approved spec with observable ACs and readiness evidence |
| [sdd-implement](sdd-implement/SKILL.md) | Tested delivery and verification evidence in the spec |
| [sdd-review](sdd-review/SKILL.md) | Findings against the spec and repository standards |

Use [sdd-run](sdd-run/SKILL.md) to run or resume the entire workflow. Use [sdd-improve](sdd-improve/SKILL.md) for a broader repository scan.

Small fixes that restore established behavior and refactors that preserve it go directly through [sdd-implement](sdd-implement/SKILL.md#small-fixes-and-refactors) and [sdd-review](sdd-review/SKILL.md).

Settle shared domain models, consumer-facing APIs, and key UI interactions in `sdd-spec`. Prototype when an open interaction decision would change the ACs. Design the physical schema and internal code structure in `sdd-implement`.

Implement small specs in sequence and split larger specs into independent tasks when their dependencies and write scopes allow it. See [parallel implementation](sdd-implement/references/parallel-implementation.md) and the [artifact path policy](references/output-paths.md).
