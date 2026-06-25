# Map — MECE pillars

**Lens** = findings `lens` column. Checklists → `playbook.md`. Report shape → `report.md`.

Each finding maps to **one pillar** via its lens id. Cross-pillar topics use **one primary lens** below — no second row.

## Six pillars (mutually exclusive)

| Pillar | Question | IDs | Checklist |
|--------|----------|-----|-----------|
| **Architecture** | How is the system **shaped**? | A1–A6 | `playbook.md` § Architecture |
| **Code** | What do **implementations** get wrong at scale? | C0–C3 | `playbook.md` § Code |
| **Security** | What can be **attacked or leaked**? | S1 | `playbook.md` § Security |
| **Verification** | How do we **know** it works in automation? | V1, V2 | `playbook.md` § Verification |
| **Dependencies** | What **enters** the repo (libs, toolchain, dev surface)? | D1 | `playbook.md` § Dependencies |
| **Operations** | How does it **run and ship** in prod? | O1 | `playbook.md` § Operations |

### Boundary rules (MECE)

| Belongs to | Not |
|------------|-----|
| **A** — layer direction, contracts, state places, extension topology, interface **shape**, a11y structure | Line bugs (**C0**); log content (**C2**); manifest versions (**D1**) |
| **C** — correctness classes, in-repo complexity/dead code, logs/metrics **in code**, hot-path algorithms | Import-layer violations (**A1**); CVE in lockfile (**D1**); missing CI job (**V2**) |
| **S** — secrets, injection, authZ gaps, unsafe defaults, **exploitable usage** of deps at `file:line` | Outdated package version alone (**D1**); flaky test job (**V2**) |
| **V** — test files, coverage gaps, **CI workflow** gates and pipeline config | README install instructions (**D1**); prod rollback runbook (**O1**) |
| **D** — lockfiles/manifests, licenses, duplicate packages, README/**local** scripts, base image **in Dockerfile**, SBOM/audit **policy for deps** | Prod deploy approval (**O1**); domain DIP (**A1**) |
| **O** — deploy/release workflows, prod env/runbook, health/readiness, migration↔deploy order, IaC **runtime**, post-deploy smoke | `package.json` outdated (**D1**); unit test gap (**V1**) |

Design principles (`playbook.md` § Design principles) are **not** a pillar — map ISP/DIP to **A**, KISS/YAGNI clutter to **C** or **A**.

## Primary lens (overlap resolution)

| Topic | Lens |
|-------|------|
| Swallowed exceptions (wrong behavior) | **C0** |
| Missing logs/metrics on code path | **C2** |
| Async race in one handler | **C0** |
| Shared mutable architecture | **A4** |
| Missing tests | **V1** |
| Untestable structure | **A6** |
| N+1 in code | **C3** |
| API forces per-item calls (contract) | **A3** |
| Secret value or exposure | **S1** |
| CI merge when red / missing check | **V2** |
| README ≠ CI commands | **V2** (gate); fix README under **D1** only if separate finding |
| Outdated / vulnerable **dependency version** | **D1** |
| Unsafe **use** of dependency API | **S1** |
| Flaky CI job | **V2** |
| Deploy / rollback / runbook / health endpoint | **O1** |
| Migration vs deploy order | **O1** |
| a11y | **A1** |
| Pass-through module (structure) | **A6** |
| Same-layer junk file (no layer breach) | **C1** |
| Cross-layer `utils/` fan-in | **A1** |
| Duplicate contract definitions | **A2** |
| ISP fat interface | **A6** |
| DIP domain→infra import | **A1** |
| YAGNI / half migration | **A5** |
| PII in logs | **S1** |
| God module / big ball of mud | **A1** |
| Anemic domain / service proliferation | **A6**, **A5** |
| Middle man / DTO ceremony | **A6**, **C1** |
| Shotgun surgery / data clumps | **A2**, **A3**, **C1** |
| Lava flow / copy-paste cluster | **C1** |
| Circular dependency / feature envy | **A1** |
| Database as integration | **A2**, **A3** |

## Shape triggers

| Shape | Deepen |
|-------|--------|
| API, runtime, full-stack | S1; V2; O1 |
| Full-stack + UI | A1, A3 |
| Agent / workflow | A1, A2 |
| Library / CLI | A3, V1, A6, D1 |
| Batch pipeline | C0, C3, A4, O1 |

Record `documented_shape` on recon card.

## Variants

| English | Chinese trigger | Lenses | Skipped (default) |
|---------|-----------------|--------|-------------------|
| simplicity | 简化审查 / 简化 / 过度设计 | **A5**, **A6**, **C1** | A1–A4, S, V, D, O |
| anti-pattern | 反模式 | **A1**, **A5**, **A6**, **C1** | S, V, D, O |
| architecture | 架构审查 / 架构 | A1–A6 | per user |
| security | 安全审查 / 安全 | S1 | others per user |

Details → `playbook.md` § Simplicity, § Anti-patterns.
