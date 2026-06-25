# Stack Signals

Use with `lenses.md`. Patterns are hints — confirm with call paths before reporting.

## TypeScript / JavaScript (Node, Next, Vite)

| Lens | Signals |
|------|---------|
| L1 | `import` from `app/` into `lib/domain`; barrel `index.ts` re-export cycles |
| L2 | Parallel `types/`, `schemas/`, `validators/` for same entity; Zod + TS interface drift |
| L3 | `fetch`/`axios` client types ≠ server DTO; `JSON.parse` without schema at boundary |
| L5 | Multiple routers (`pages/` + `app/`); duplicate middleware stacks |
| L6 | Tests only in `e2e/`; domain logic under `components/` |

## Python

| Lens | Signals |
|------|---------|
| L1 | `models` importing `views`; circular `__init__.py` re-exports |
| L2 | SQLAlchemy model + Pydantic schema + dict config for same entity |
| L3 | FastAPI response_model ≠ client expectations; `extra="ignore"` on hot models |
| L4 | Module-level mutable singletons; Celery tasks without idempotency story |
| L5 | `legacy/` + `v2/` parallel packages |

## Go

| Lens | Signals |
|------|---------|
| L1 | `internal/` bypassed via `pkg/` misuse; `main` packages importing each other |
| L2 | Proto + hand-written struct for same message |
| L3 | JSON tags vs OpenAPI spec mismatch |
| L6 | `_test.go` only in `cmd/` not `internal/` |

## Rust

| Lens | Signals |
|------|---------|
| L1 | `crate` dependency cycles; `pub use` flattening hiding boundaries |
| L2 | `serde` types duplicated across crates |
| L5 | Feature-flag explosion without default path |

## Full-stack (any)

| Lens | Signals |
|------|---------|
| L3 | OpenAPI/GraphQL schema stale vs implementation; shared package version skew |
| L2 | Env-specific config in FE and BE with different keys |
| L4 | WebSocket/SSE state split across client cache and server session |

## Agent / workflow repos

| Lens | Signals |
|------|---------|
| L1 | Skills/rules importing each other in cycles; orchestration mixed with domain |
| L2 | Duplicate prompt templates; two registries for same capability |
| L5 | Vendored copy of upstream skill vs symlink/install doc |
