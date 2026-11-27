# Shared contracts

Read this when a feature touches a shared domain rule or interface. Check the glossary, ADRs, domain or API documents, and existing behavior that constrains compatibility. Use the consuming repository's conventions.

- When shared terminology, relationships, or lifecycle rules need clarification, use `domain-modeling` if available. Apply its scenario checks to the proposed model and save shared contract decisions after the responsible owner accepts them.
- Trace a proposed contract change to approved PRD scope and its use cases. Define the affected concepts, ownership, lifecycle, permissions, and invariants. Document field shape, requiredness, and constraints where the contract depends on them.
- Capture affected consumer-facing operations, inputs, results, errors, permissions, and compatibility. Record device protocols such as MQTT in the integration or protocol contract.
- Update the canonical contract only for a real gap. Record durable architectural decisions in an ADR when appropriate. Capture externally visible guarantees such as tenant isolation or migration compatibility here. Design their physical schema, internal interfaces, and other implementation details in [sdd-implement](../../sdd-implement/SKILL.md).
- Obtain acceptance from the contract owner for required changes before marking readiness Ready. Unrelated Ready slices can proceed. Record the relevant links and decisions in the spec.

Use the [assessment template](design-readiness-template.md) and [example](design-readiness-example.md) if the user or repository calls for a standalone assessment.
