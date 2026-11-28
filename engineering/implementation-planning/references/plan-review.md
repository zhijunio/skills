# Plan review

Review the complete plan against the approved sources before handing it off.

- Coverage: each in-scope acceptance criterion and consequential constraint maps to one or more tasks and relevant verification. Exclusions remain excluded.
- Consistency: task identifiers are unique, prerequisites resolve without cycles, and interfaces match between producer and consumer.
- Ownership: affected files and shared resources have clear ownership; proposed parallel work does not create conflicting writes.
- Evidence: expected checks cover material success, failure, authorization, integrity and compatibility risks as applicable. Planned checks are not execution evidence.
- Integration: include relevant migration ordering, coexistence, rollback/recovery limitations and end-to-end verification. Do not assume rollback is safe for irreversible data changes.
- Proportion: remove redundant steps and unnecessary artifacts. A plan should enable implementation, not duplicate the implementation or requirements.
- Authority: label open decisions and blockers; do not resolve product or design conflicts by assumption. No status transition, deployment, publication or commit is implied by plan review.

Report gaps and their owners. Fix plan-local omissions, then recheck affected dependencies and coverage. Route scope and architecture conflicts to the appropriate owner. Keep the plan pending if material blockers remain.
