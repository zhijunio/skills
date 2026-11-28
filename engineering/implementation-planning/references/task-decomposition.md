# Task decomposition

Map approved behavior to coherent deliverables before naming tasks. Inspect existing code and tests; preserve established interfaces and architecture unless an approved change requires otherwise.

For each task record a stable task ID, intended result, source requirement or acceptance criterion, prerequisites, affected paths or proposed paths, interfaces consumed/produced, verification steps and expected outcome. Include setup/configuration/documentation in the task that needs it unless a shared prerequisite is independently reviewable. Size tasks by coherent review and verification, not by arbitrary line count or duration.

Prefer a slice that delivers observable behavior across necessary layers when feasible. Shared contracts, schema compatibility, or migration sequencing may require prerequisite slices. Make their dependency and integration checks explicit. A plan for one use case must not silently include other use cases; list external dependencies separately.

Order prerequisites before consumers. Identify shared-file conflicts, interface changes and integration points. Only label tasks parallelizable when their prerequisites are resolved and their edits and contracts do not conflict. If parallel tools are unavailable, a valid sequential order must remain.

Use project test commands where verified from repository configuration. Include acceptance checks and relevant negative paths. Specify what should be observed, not a fabricated execution result. Use TDD if appropriate or required by the project; never substitute a prescribed method for evidence. Do not include complete production implementations in the plan unless the user specifically requests a code-level design.
