---
name: product-prototyping
description: Explore product journeys, information architecture, interface alternatives and business behavior through reviewable wireframes, interactive UI prototypes or isolated state simulations. Use to resolve product or behavior uncertainty before implementation; not production UI delivery, implementation planning or technical feasibility experiments.
---

# Product Prototyping

Answer a bounded product or behavior question with the least costly reviewable artifact. Treat experimental code as non-production and separate learning from implementation authorization.

## Select the mode

- Product journey or interface: use [UI prototype](references/ui-prototype.md) for navigation, user flows, wireframes and interactive alternatives.
- Business behavior: use [behavior prototype](references/behavior-prototype.md) to explore rules, data shapes and state transitions.
- Combine the two only when the same question requires both. A headless product need not have a visual UI.
- Technical feasibility, compatibility, capacity or performance: route architecture questions to system-design and evidence gaps to an authorized technical experiment rather than claiming that a UI mock or simulation proves real system behavior.

Read existing product scope, relevant decisions and design conventions. Identify the audience, decision owner, question, assumptions, success criteria and authorized experiment boundary. Ask only about material gaps. If a diagram or discussion can settle the question, do not build code unnecessarily.

## Build within the review boundary

Choose the fidelity needed to answer the question; sketch, wireframe, standalone HTML or a safe isolated development preview are alternatives, not mandatory sequential steps. Reuse existing design conventions where useful. Use synthetic or sanitized fixtures, local/in-memory state and stubbed mutations by default. Keep experimental code out of production routes, bundles and data paths. An existing-page experiment requires explicit authorization, an isolated development environment and verification that production behavior is unaffected. Hiding a switcher alone is not isolation.

Make the artifact easy to inspect, reset and run. Show relevant states and explain assumptions. Run the applicable smoke checks or scenario assertions; report what actually ran. Do not force production-level polish or tests that do not contribute to the learning goal. Never claim accessibility, usability, compatibility or performance evidence that was not collected.

## Review and handoff

Use [prototype review](references/prototype-review.md) to capture alternatives, observed feedback, decisions, limitations and unresolved questions. Clearly distinguish a proposed choice from an approved decision. Route confirmed product feedback to product-requirements, and behavior feedback to behavior-specification or domain-modeling; do not silently modify authoritative scope, acceptance criteria or use-case status.

Provide the actual artifact path, run instructions and review status. If the user has not reviewed it, say so. Do not promote experimental code, create branches, commit, publish, deploy or modify implementation issues solely because the prototype is accepted. Preserve or clean up artifacts within the authorized scope; production reuse requires separately authorized implementation and normal quality checks.
