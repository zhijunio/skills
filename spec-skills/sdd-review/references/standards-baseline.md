# Standards baseline

Repo-documented standards override this list. Paste [smell-baseline.md](smell-baseline.md) into the Standards sub-agent (smells belong under **Structure**).

| Dimension | Look for |
| --- | --- |
| **Correctness** | Real inputs, edge cases, failures, state, lifecycle. **When the diff has signals:** security, concurrency, data/migration, performance. |
| **Structure** | Boundaries, dependency direction, half migrations, dead code, parallel APIs, large duplication — **and all Fowler smells** in [smell-baseline.md](smell-baseline.md). |
| **Verification** | Behavior-focused tests cover the change; CI/local proof; prefer recorded close-out or suite evidence when present. |
| **Traceability** | Spec/plan/CHANGELOG/links, public APIs, config keys, package names, migration paths, install/setup, tooling, DX — still match the tree. |

**Binding:** documented-standard breaches may be hard; baseline dimensions and Fowler smells are judgement calls unless they also break a documented standard or an `AC-n`. Skip tooling-enforced style. Smells alone default to 🟢/🟡 — not 🔴 without AC or a documented hard rule.

## Structural simplification lens

Apply this lens when the change or hotspot has meaningful structural complexity; do not turn it into a universal style gate.

- **Code judo:** look for a reframing that deletes concepts, branches, helper layers, or state instead of merely moving the same complexity around.
- **Spaghetti growth:** scrutinize ad-hoc conditionals, one-off flags, scattered special cases, and feature logic leaking into shared paths. Prefer a focused abstraction, explicit dispatcher, state model, or canonical owner when it materially simplifies the flow.
- **Direct boundaries:** question thin wrappers, magical generic mechanisms, duplicate helpers, unnecessary `any`/`unknown`/casts, and optional parameters that hide a clearer invariant or add indirection without buying clarity.
- **Canonical ownership:** keep logic in the layer/module that owns the concept and reuse existing canonical helpers instead of creating parallel APIs.
- **Orchestration:** when signaled, flag avoidable serialization of independent work and related updates that can leave state half-applied. Prefer the simpler parallel or atomic shape when it also improves clarity.
- **Decomposition:** treat substantial file growth and cohesive-module loss as design signals. A 1,000-line boundary is a heuristic prompt to consider decomposition, never a blocker by itself.

These are judgement calls. In `sdd-review`, they remain delivery findings only when the diff introduced or worsened the issue; in `sdd-improve`, report systemic hotspots and leverage. Do not promote a missed ideal refactor to 🔴 without a documented hard rule or contract gap.
