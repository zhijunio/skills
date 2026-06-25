# Architecture Lenses (pillar: Architecture)

Apply when the architecture pillar is in scope. A finding needs **file/symbol evidence** and **structural impact** — not style nits.

## L1 — Boundaries & layers

- Can every major module name its boundary (domain / runtime / adapter / app)?
- Do imports violate intended direction (domain importing adapters, UI calling DB)?
- God modules/files without a single ownership story
- Circular dependency clusters

**Evidence:** import graph samples, `rg` for cross-layer imports, largest files list.

## L2 — Source of truth

- Same product contract defined in code + SQL seed + JSON config + generated docs — which wins?
- Duplicate types/models for one concept (parallel `*Config`, `*Context`, registries)
- Compatibility shims without owner, test, or removal condition

**Evidence:** two+ definitions with divergent fields; call paths using different copies.

## L3 — Contracts & integration

- Public API surfaces: REST/GraphQL schemas, shared TS types, protobuf, event schemas
- Full-stack: field names/types across boundary; serializers that drop fields (`extra=ignore`, silent defaults)
- Versioning and breaking-change story missing where multiple clients exist

**Evidence:** both sides of boundary with mismatch; serializer config on hot paths.

## L4 — State ownership & lifecycle

- Who owns mutable state (singleton globals, session stores, caches)?
- Init → run → shutdown: are all phases explicit and testable?
- Async/concurrency: shared locks across await, fire-and-forget tasks, missing backpressure

**Evidence:** mutation sites, lifecycle hooks, spawn without join.

## L5 — Extension cost & migration debt

- Cost to add a new variant/type/plugin (file touch count, registry wiring)
- Half-finished migrations (two routers, dual config loaders, feature flags stuck on)
- Generated vs hand-written drift

**Evidence:** count files to add one feature; list parallel implementations.

## L6 — Testability of architecture

- Can core/domain run headless without browser/OS/GPU?
- Contract tests at boundaries vs only E2E
- Tests that mock own modules instead of fakes at IO edge

**Evidence:** test layout vs production entrypoints; missing tests for contract owners.

---

## Scoring hints (standard/deep)

| Signal | Weight |
|--------|--------|
| Active churn directory | audit deeper |
| Documented ADR for pattern | vet as by-design |
| Exported but zero inbound refs | L2/L5 candidate |
| >800 LOC single file | L1/L5 candidate |

Stack-specific `rg` patterns: `references/stack-signals.md`.
