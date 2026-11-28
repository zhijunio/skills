# Context efficiency baseline

Status: static byte baseline measured from GitHub tree at 76ffd8cb79e28fb605c83a6bb7251320f7c979cc; actual model context and efficiency NOT RUN.

The [snapshot](context-size-baseline.json) contains all 63 SKILL.md paths, Git blob IDs and byte sizes from that revision. Total 327266 bytes: engineering 17 / 99242; spring-boot-skills 39 / 174458; collected-skills 7 / 53566. These are repository bytes, not loaded context, token counts or evidence of inefficiency.

Reproduce local measurements with:

    python scripts/measure_skill_context.py

The helper reads entry points only, reports raw UTF-8 bytes, Unicode code points, physical lines and Git-compatible blob hashes. Historical snapshot is immutable evidence, not a generated current catalog. The helper's own deterministic tests are pending CI.

## Prioritize investigation, not automatic truncation

The largest entry points are web-access (23989 bytes), spring-boot-testing (13869), html-plan (12596), spring-data-jpa (12402), spring-data-redis (12392), progen (11927), writing-for-agents (10886), teach (9733), multi-module-maven (9324), and flyway-migrations (9196). Size alone is not a defect. Preserve security boundaries and essential workflow instructions in entry points. Review A06 web-access script safety and A04 unresolved provenance before proposing broader changes there.

Start matched behavior comparisons with spring-data-jpa, spring-boot-testing and spring-boot router, using SB04, SB09, SB10, SB01 and SB08 in [Spring cases](../../evals/spring-boot/README.md). Record which entry points and references were actually loaded, not the total size of all installed skills. Inspect repeated instructions and unnecessary tool calls from actual traces. A router table may be worth its context if it prevents loading irrelevant skills.

## Measurement contract

For each matched run record model and host version, task/fixture, installed skill revisions, initial context policy, loaded skill/reference paths if observable, provider-reported input/output and cached tokens where available, elapsed wall time, tool counts, and actual correctness assertions. Keep total tokens distinct from input/output and cache fields; do not sum overlapping categories. If only aggregate tokens are exposed, label them aggregate. Do not estimate tokens from bytes or characters; missing values remain unavailable. Control for different host caching behavior and compare fresh sessions under equivalent permissions and workload.

Compare control, unchanged baseline and candidate with predeclared trial count and budget. Evaluate required correctness and authorization before efficiency. Report distribution and denominators, not only mean. A smaller entry point may increase reference reads or errors; optimize total observed task cost only when correctness is preserved. No universal token or size threshold and no refactoring solely from this static report.

Actual agent runs, reference-loading traces, token consumption, elapsed time and cost comparisons remain NOT RUN. A11 remains open.
