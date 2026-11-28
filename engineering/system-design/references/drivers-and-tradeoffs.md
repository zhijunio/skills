# Architecture drivers and tradeoffs

Use for consequential design decisions. Start from product intent and existing constraints, not a favored pattern.

## Drivers and quality scenarios

Record the affected actor or system, stimulus, operating context, expected response and observable measure. Examples of concerns include availability, latency, throughput, integrity, security, privacy, recoverability, maintainability, evolvability and cost. Only include concerns relevant to the decision.

Separate mandatory constraints, prioritized desired qualities and assumptions. If a numerical target is unknown, describe the measurement and ask the decision owner; do not insert illustrative numbers as commitments. Identify tradeoffs such as stronger consistency versus availability, isolation versus operational overhead, or extensibility versus complexity. State which drivers dominate and why.

## Compare candidates

For each viable shape, describe its boundaries, deployment granularity, state authority, operational obligations, fit with constraints, risk, reversibility and verification path. Consider an existing system or modular single deployment where adequate. Compare additional distribution only when independently justified by quality or ownership needs. A single feasible option is acceptable if alternatives are ruled out with evidence; do not manufacture options.

Record the recommended decision, alternatives rejected, reasons, consequences, unresolved assumptions and owner. Distinguish a proposed decision from an approved one. Use an ADR when the choice is consequential and durable; follow existing conventions.

## Evidence

Map significant drivers to future verification: dependency rules, contract checks, scenario tests, performance experiments, recovery exercises or review of relevant controls. Name the expected observable result and evidence owner. A design-time prediction is not an executed test. Mark missing evidence and recommend bounded feasibility work if it could change the choice.
