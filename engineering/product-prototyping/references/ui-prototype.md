# Product and UI prototype

Use for questions about navigation, information organization, user journeys or interface interaction.

## Frame the journey

Identify actor, goal, starting condition, entry point, success outcome and relevant constraints. Map screens or steps, actions, decision points and alternate paths. Distinguish observed needs from proposed interface behavior. Do not add product capabilities to make the demo feel complete.

For broad product exploration, sketch capability groups and navigation first. For a bounded screen decision, use the existing journey and avoid redesigning unrelated pages.

## Select fidelity

- Flow map or wireframe for information architecture and step ordering.
- Static visual mock when layout or hierarchy is the question.
- Interactive prototype for navigation, forms, feedback or state-dependent tasks.
- Comparison variants when there are genuine alternatives. Two or three meaningful structural options may be sufficient; do not force a variant count.

Use existing design tokens and component conventions when available. Include representative data density and relevant empty, loading, error, success, permission and recovery states. Consider keyboard interaction, accessible labels, focus order, contrast and responsive behavior where relevant; do not claim formal compliance without verification.

## Build and isolate

Prefer a standalone artifact or a dedicated development-only preview using synthetic fixtures and stubbed actions. Do not call production APIs or reuse live authentication as a shortcut. An explicitly authorized existing-page experiment must preserve its original behavior, isolate variants from production builds and avoid real mutations. Verify the isolation rather than assuming an environment flag is sufficient.

For comparison variants, use structurally distinct layouts or task flows, not merely changed colors. Hold the underlying task and fixtures constant where meaningful. A labeled variant selector or URL parameter can help reviewers compare and reproduce choices. Do not intercept typing or essential keyboard interactions.

Make the primary journey navigable. Label nonfunctional controls and simulated outcomes; never represent a mock as a working backend.

## Hand over

Provide the artifact location, startup instructions, available variants, task walkthrough and known limitations. Verify applicable links and interactions if the environment permits; distinguish observed behavior from expected behavior. Ask reviewers to attempt realistic tasks, explain confusion and identify tradeoffs. Capture the outcome with the review reference; do not automatically promote a winning design into production code.
