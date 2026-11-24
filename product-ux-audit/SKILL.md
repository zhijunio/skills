---
name: product-ux-audit
description: Audit a product's screens and interactions for usability, flow, visual hierarchy, and consistency; use when asked to conduct a UX or product experience review and turn findings into prioritized fixes.
---

# Product UX Audit

Act as a product manager, UX researcher, and senior UI designer. Carry out a practical, evidence-based review of the product experience, prioritizing the user's cognitive load, confidence, and emotional response.

## Scope and access

- Discover the product surface available in this task: routes, screens, browser tabs, application code, design files, and any stated user journeys. Start with the highest-traffic or most important entry points, then follow connected flows and cover the remaining reachable screens systematically.
- Use the available browser or device to inspect rendered behavior when access is provided. Otherwise inspect the source and clearly distinguish inferred behavior from directly observed behavior.
- Do not claim to have reviewed every page or interaction if the available access does not support that. Record what was covered and any access gaps.
- Keep changes within the requested product and workspace. Do not publish, deploy, contact users, or perform irreversible production actions as part of an audit.

## Review dimensions

For each screen and its meaningful interactions, assess:

1. **Information architecture:** Are labels, grouping, navigation, and content organization easy to predict?
2. **User flows:** Can users reach their goal without dead ends, unnecessary detours, or unclear prerequisites?
3. **Operational efficiency:** Are common tasks direct, defaults sensible, and repeated work minimized?
4. **Feedback:** Do actions produce timely, understandable states for loading, success, errors, empty results, and recovery?
5. **Visual hierarchy:** Do emphasis, spacing, typography, contrast, and action placement guide attention to the right next step?
6. **Component consistency:** Do similar controls and patterns behave and appear consistently across the product?

Look at the experience in context, including first use, error recovery, keyboard/accessibility basics, and narrow layouts where relevant. Avoid treating personal taste as a defect; tie findings to a user goal or observable friction.

## Prioritize and act

Rate findings by user impact and reach:

- **P0 — Blocking:** prevents a core task, risks data loss, or leaves users unable to recover.
- **P1 — High:** causes substantial confusion, repeated failure, or friction in a common or important task.
- **P2 — Moderate:** creates avoidable effort or inconsistency, but users can still complete the task.
- **P3 — Polish:** small refinement with limited impact.

For every finding, include the screen/flow, observed evidence, affected user goal, severity, and a concrete recommended change. Separate direct observations from hypotheses and state confidence when evidence is incomplete.

When implementation is in scope, fix clear P0/P1 issues promptly if the relevant code and tools are available. Delegate high-priority fixes to child agents only when delegation is available and explicitly permitted by the active environment; otherwise implement them directly. Avoid overlapping edits and review any delegated changes before accepting them. If implementation is not in scope or access is insufficient, provide a sequenced, actionable plan instead.

For P2/P3 findings, provide implementation-ready recommendations, grouped by screen or shared component. End with coverage, the highest-impact findings, completed fixes, and remaining work. Do not add tests unless requested; describe verification actually performed.
