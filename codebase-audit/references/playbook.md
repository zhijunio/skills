# Playbook

Load order: **Recon** → pillar sections for invoked lenses → **Stack signals** (after recon) → **Vet** before reply. Routing → `map.md`.

---

## Recon

Run before any lens audit.

### Target

- Confirm `{TARGET_DIR}` (user path or repo root). All commands run from there.
- Record `git rev-parse --short HEAD`, branch name, `git status --short`.
- **Branch-scoped audit** (`branch` / 分支审查 / 本分支): merge-base with default branch, then `git diff --name-only <merge-base>..HEAD`. Scope = changed files + direct importers/callers when tractable.

### Verification baseline

- Exact commands for build, test, lint, typecheck (from README, CI, or manifests).
- Run read-only checks when cheap (`tsc --noEmit`, lint check mode, ecosystem audit in check mode).
- If **no working verification story**, record on recon card — often roadmap P0 before risky structural work.

### Read first

- `README`, `AGENTS.md` / `CLAUDE.md`, `CONTRIBUTING`
- Root manifests: `package.json`, `pyproject.toml`, `go.mod`, `pom.xml`, `build.gradle.kts`, `Cargo.toml`, …
- CI/CD: `.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile`, `Dockerfile*`, `deploy/`, `infra/`, `k8s/`, `helm/`, terraform
- Intent docs: `docs/adr/`, `DESIGN.md`, `PRODUCT.md`, architecture diagrams in `docs/`

Carry ADR decisions as **settled tradeoffs** during vet — do not re-litigate unless code contradicts the ADR.

### Git signal (standard/deep)

- `git log --oneline -30`; churn hotspots (`git log --since=90.days --name-only … | sort | uniq -c | sort -rn | head -20`)
- Largest modules vs median file size — prioritize audit depth there

### Recon card

```text
git_head:
branch:
merge_base:             # branch audit only
scope:                  # whole-repo | branch:<name>
stack:
entrypoints:
packages_or_crates:
verification_commands:
verification_status:    # ok | partial | none
ci_cd_paths:
hotspot_dirs:
documented_shape:
open_questions:
```

### Shape classification

Library · CLI/tool · HTTP API · long-running runtime · full-stack app · agent/workflow · batch pipeline · other. Shape triggers → `map.md`.

---

## Architecture (A1–A6)

**Pillar: Architecture** — system **shape** only. Implementation bugs → **C**; manifests → **D**.

- **A1** Boundaries & layers: import direction; cycles; god cross-layer modules; **DIP** (domain→concrete infra/UI); **a11y** structure
- **A2** Source of truth: one concept, multiple owners (code, SQL, config, docs)
- **A3** Contracts: public API schemas; cross-boundary field mismatch; versioning
- **A4** State & lifecycle: ownership; init/shutdown; concurrency **architecture**
- **A5** Extension & migration: half migrations; parallel implementations; speculative `v2/`
- **A6** Testability & interface shape: headless domain; contract seams; **ISP**; pass-through (**deletion test**)

Not A6: tests missing → **V1**. Not A4: single `await` bug → **C0**.

---

## Code (C0–C3)

**Pillar: Code** — **implementation** at scale.

- **C0** Correctness: swallowed errors; async races; null/bounds; silent state machines; idempotency gaps; type-escape clusters
- **C1** Complexity: duplication; dead code; parallel APIs **same layer**; KISS/YAGNI clutter (see Design principles)
- **C2** Observability: structured logs/traces missing; weak error responses (non-security). Not PII in logs → **S1**
- **C3** Performance: N+1; unbounded fetch; hot-path blocking; over-fetch; FE bundle/waterfalls

---

## Security (S1)

Evidence only — `file:line` + credential **type**; never values. Out of scope: pentest, compliance cert.

Vet by-design: `https_proxy` / `NO_PROXY`; documented local-dev shell-outs.

- Secrets; injection (SQL/shell/template, XSS, `eval`, path traversal)
- AuthN/Z; IDOR; CSRF on browser state changes
- Validation; unsafe uploads; mass assignment
- Headers/cookies: CORS+credentials; CSP; session flags
- PII in logs; internal errors to clients
- **Unsafe API use** at `file:line` — not manifest version alone (**D1**)
- Missing rate limits on expensive/auth endpoints (systemic)

---

## Verification (V1–V2)

**A6** = structure testable; **V1/V2** = tests and CI **exist and protect**.

### V1 — Tests

- No verification baseline (often P0); gaps on domain, auth, money, contracts
- Mocks internals vs IO boundaries; flaky patterns; E2E-only for headless logic

### V2 — CI

Not O1: deploy/rollback story. Not S1: secret values in YAML.

- Lint without tests (or reverse); local scripts ≠ CI jobs
- Missing/bypassable checks; `continue-on-error` on critical jobs
- Missing cache; flaky jobs undocumented; unpinned actions (`@main`); over-privileged roles
- `pull_request_target` + secrets; monorepo path filters

Evidence: test layout; workflow files; branch protection in CONTRIBUTING/README.

---

## Dependencies (D1)

What **enters** the repo and **local** dev — not prod runtime (**O1**).

- Critical outdated deps; lockfile drift; license conflicts; duplicate libs
- Abandoned deps; deprecated APIs still primary; ecosystem audit failures
- Stale/`latest` base image in **Dockerfile**; SBOM/dep-scan policy gaps
- README missing install/run/test/lint; stale examples; missing verify scripts
- ADR misleading vs code (only if misleads onboarding)

Not D1: missing tests → **V1**; insecure call site → **S1**; prod deploy gate → **O1**; CI job → **V2**.

---

## Operations (O1)

How the system **ships and runs in prod** — not what libraries are pinned (**D1**).

- Env promotion; prod deploy without approval when risk warrants; changelog drift
- DB migration order vs app deploy unsafe
- Rollback/incident runbook missing; no canary/kill-switch notes
- Health/readiness; post-deploy smoke when repo deploys services
- IaC prod values diverge; image scan gate missing in **release** pipeline

Not O1: outdated `package.json` → **D1**; test gap → **V1**; workflow secret → **S1**.

Evidence: deploy workflows, `deploy/`, `infra/`, k8s, helm, terraform, runbooks in `docs/`.

---

## Design principles

Not a pillar — map to lens ids per `map.md`.

| Principle | Signals | Lens |
|-----------|---------|------|
| KISS | Multiple mechanisms; ceremony layers; config explosion | **A5**, **C1** |
| YAGNI | Unused hooks; premature generics; stale `v2/` | **C1**, **A5** |
| Deletion test | Pass-through module | **A6**, **C1** |
| Information hiding | Same knowledge 2+ modules; leaky contract | **A2**, **A3**, **A6** |
| Cohesion | Cross-layer `utils/` fan-in | **A1**; same-layer clutter **C1** |
| Decoupling | Wrong import direction; globals | **A1**, **A4** |
| ISP | Fat interface; forced unused methods | **A6** |
| DIP | Domain→infra; `new` adapter in core | **A1**, **A6** |

Effort: **snapshot / 快照** → 1 simplicity + 1 module signal; **standard / 标准** → C1 + A5/A6 hotspots; **deep / 深度** → full tables on top fan-in modules.

---

## Anti-patterns

**Not a pillar** — name recurring **structural** smells; map to lens ids. Do **not** GoF name-dropping on a single class. Report when the smell is **repeated** in a module, layer, or package (same bar as systemic first).

| Anti-pattern | Signals | Lens |
|--------------|---------|------|
| **God module / blob** | One module/class owns unrelated concerns; extreme fan-in | **A1**, **C1** |
| **Big ball of mud** | No discernible layers; anything imports anything | **A1** |
| **Anemic domain** | Entities/DTOs are bags; rules live only in `*Service` / `*Manager` / handlers | **A6**, **C1** |
| **Service proliferation** | One service per table/endpoint; no domain seam or aggregate boundary | **A5**, **A6** |
| **Middle man / pass-through** | Delegates without invariant, validation, or translation | **A6** |
| **Speculative generality** | Hooks/parameters for one caller; unused extension points; config for futures never built | **A5**, **A6** |
| **Golden hammer** | Same mechanism everywhere (events, factories, decorators, base classes) regardless of fit | **A5**, **C1** |
| **Shotgun surgery** | One concept change ripples across many files (duplicate knowledge) | **A2**, **C1** |
| **Data clumps / primitive obsession** | Same parameter groups everywhere; stringly IDs; no shared value type | **A3**, **C1** |
| **DTO / mapper ceremony** | Entity→DTO→VO→DTO chains with no rule between hops | **A6**, **C1** |
| **Lava flow** | Dead branches, commented blocks, unreachable flags left in place | **C1**, **A5** |
| **Copy-paste programming** | Near-identical logic across files (not shared helper) | **C1** |
| **Feature envy** | Module manipulates another module's data more than its own | **A1**, **C1** |
| **Circular dependency** | Package/module import cycles | **A1** |
| **Yo-yo / parallel hierarchies** | Deep inheritance or mirror class trees; logic split across levels | **A5**, **A1** |
| **Magic glue** | Hidden globals, static mutable singletons as integration | **A4**, **A1** |
| **Database as integration** | Shared tables/schema as the only contract between modules | **A2**, **A3** |
| **Reinvented mini-framework** | In-house DI/router/ORM inside one product | **A5**, **C1** |
| **Callback / promise pyramid** | Deep nested async control flow as a file-class pattern | **C1** |

**Vet:** one `UserService` or one long mapper is not a finding — look for **class of modules** or **hotspot package**. ADR-mandated pattern → by-design unless code contradicts ADR.

Simplicity, anti-pattern, and architecture audits: scan this table on recon hotspots after Design principles.

---

## Simplicity (`simplicity` / 简化审查 / 简化 / 过度设计)

Read-only **simplification audit** — where to delete, merge, or collapse abstraction. **Does not edit code**; roadmap = systemic moves (`structural_fix`), not line-by-line rewrites.

### Scope

| In scope | Lenses |
|----------|--------|
| Over-abstraction, YAGNI, pass-through, duplication, dead code | **A5**, **A6**, **C1** |
| Design principles + Anti-patterns tables above | map to those lenses |

| Skip unless user widens | Pillars / lenses |
|-------------------------|------------------|
| Security, CI, deps, ops | **S**, **V**, **D**, **O** |
| Layer shape (unless ceremony is the root cause) | **A1–A4** |

Optional **A1** one row only when cross-layer `utils/` fan-in or DIP ceremony **directly** drives clutter — not a full architecture pass.

State skipped pillars in report: typically **A1–A4, S, V, D, O**.

### Scan order

1. Recon card + churn hotspots / largest modules  
2. Deletion test on suspected pass-through modules  
3. Design principles + **Anti-patterns** tables on top fan-in files  
4. Stack signals for invoked stack (A5/A6/C1 columns only)

### Heuristics (signals — not lint gates)

Report when **repeated in a module, file class, or layer** — still systemic first:

| Signal | Lens |
|--------|------|
| Pass-through wrapper / shallow module (interface ≈ implementation) | **A6** |
| Speculative `v2/`, unused hooks, single-impl abstraction, premature generics | **A5**, **C1** |
| Duplicate helpers, parallel same-layer APIs, dead code, unread config flags | **C1** |
| Long functions, high param count, deep nesting **as a pattern** in one module | **C1** |
| Ceremony layers (DTO→DTO→mapper chains with no invariant) | **A6**, **C1** |

**Downgrade or Rejected findings:** one long function in an otherwise fine module; single unused import; style-only nit without pattern.

### Roadmap emphasis

Prefer: delete layer · merge modules · inline pass-through · collapse duplicate types · remove unused extension points.  
Defer extract/refactor until deletion test fails or callers need a seam.

`branch` / 分支审查 + simplicity / 简化审查: same scope; tag `introduced` / `pre-existing` or 本分支引入 / 既有问题 per report language.

---

## Stack signals

Hints only — confirm with call paths.

### TypeScript / JavaScript

| Lens | Signals |
|------|---------|
| A1 | `app/`→`lib/domain`; barrel cycles; domain imports `prisma`/UI (**DIP**) |
| A6 | E2E-only; fat interface; no injectable port |
| A2 | Parallel `types/`/`schemas/`; Zod vs TS drift |
| A3 | Client DTO ≠ server |
| C3 | `useEffect` per-row fetch |
| S1 | Server actions without auth; `dangerouslySetInnerHTML` |

### Python

| Lens | Signals |
|------|---------|
| A1 | `models`→`views`; circular `__init__.py` |
| A2 | SQLAlchemy + Pydantic + dict same entity |
| C0 | Missing `await` on async ORM |
| S1 | SQL f-strings; `shell=True` |

### Java

| Lens | Signals |
|------|---------|
| A1 | Domain imports Spring/JPA; **`new` repo in domain** |
| A6 | Only `@SpringBootTest`; fat `*Service` |
| C3 | N+1 lazy `@OneToMany` |
| V2 | Surefire skipped in CI |

### Go / Rust

| Lens | Signals |
|------|---------|
| A1 | `internal/` bypass; crate cycles |
| A2 | Proto + hand struct duplicate |
| S1 | `ListenAndServe` no timeouts; `unwrap()` on user paths |

### Full-stack / Agent / Runtime / Batch / CI/CD

| Context | Lens | Signals |
|---------|------|---------|
| Full-stack | A3, O1 | OpenAPI stale; deploy order undocumented |
| Agent | A1, D1 | Skill cycles; README missing verify |
| Runtime | O1, C2 | No `/health`; no correlation id |
| Batch | C0, A4 | Partial writes; unclear stage ownership |
| CI/CD | V2, O1, D1 | `continue-on-error` on tests; `image: latest` in release; stale Dockerfile base |

---

## Vet

1. Re-read **Critical** / **High** cites; spot-check ≥30% **Medium**
2. Apply `map.md` overlap — one row per root cause
3. Counter-evidence: tests, ADR, flags, comments
4. `inferred` → trace or downgrade
5. Log rejects in **Rejected findings**

Reject: by-design ADR; proxy convention; mis-attributed file; non-structural single bug; duplicates (keep richest row). Consolidate clusters → one finding, multiple cites.
