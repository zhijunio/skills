# Boundaries and runtime behavior

Load when describing components, integration or execution flows.

## Structure and authority

Describe the system context: actors, external systems, trust boundaries and interactions. Group responsibilities by coherent business or technical capability, not by framework classes. For each component identify its purpose, responsibility, provided and required interfaces, owned state, dependency direction and constraints. Distinguish logical modules from deployment units; do not assume one service per module.

For important data, identify authoritative owner, allowed writers, readers, lifecycle and invariants. Clarify the boundary of atomic changes and the expectations for derived copies. Detailed physical schemas and contracts belong in their respective design or implementation work, subject to these decisions.

## Runtime scenarios

Walk a representative request or event from entry to response or eventual outcome, identifying ownership, authorization, validation, state change and external interaction. Include applicable failure paths: dependency unavailable, timeout, duplicate delivery, partial write, concurrent request, stale state or restart. Specify required semantics, responsibility for recovery and user-observable consequences; leave unconfirmed policy choices open.

For asynchronous/distributed flows, identify relevant delivery, ordering, idempotency, consistency, retry limits, reconciliation and failure visibility. Do not assume distributed transactions, exactly-once delivery or an unbounded retry policy. Discuss alternatives against the drivers rather than prescribing mechanisms without evidence.

## Operational and evolution concerns

Identify required deployment boundaries, observability signals, access controls, recovery objectives and ownership where relevant. Record migration compatibility, coexistence, rollback limitations and data integrity risks for changes to existing systems. Do not provision infrastructure or execute migrations. Route unresolved high-risk mechanisms to an isolated feasibility experiment, and required checks to implementation and verification owners.
