# System design brief

Use the repository's established design artifact or adapt the following outline. Scale to the decision; not every view or heading is necessary.

- Scope, source documents and design status
- Current system and constraints (where applicable)
- Architecture drivers and prioritized quality scenarios
- Candidate structures and tradeoff analysis
- System context and external dependencies
- Logical components, responsibilities and dependency direction
- Data authority, key invariants and integration boundaries
- Representative runtime and failure scenarios
- Deployment assumptions, trust boundaries and operational implications
- Decisions, rationale and links to existing ADRs
- Evolution, compatibility and migration risks (where relevant)
- Evidence plan, unresolved decisions and handoff owners

Choose diagrams only when useful: context, logical components, deployment or sequence views. Label boundaries, relationships and direction. A component diagram is not evidence of deployment or runtime behavior. Use existing project diagram tooling or plain text; do not require a new runtime.

Review: each significant decision traces to a driver; data authority and important failure paths have owners; assumptions and proposed thresholds are visible; design does not redefine approved behavior; no unapproved stack or implementation has been introduced. Record which revision is reviewed and which decisions remain pending. Actual verification and implementation remain separate.
