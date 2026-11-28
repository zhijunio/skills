# Spring Boot evaluation cases

Status: Q06 seed cases, all independent agent runs NOT RUN. Fourteen scenarios cover high-risk routing, compatibility, HTTP, persistence, transaction, authorization, testing, architecture and migration. They are not an exhaustive evaluation of all 39 Spring Boot skills.

Use [evaluation standard](../../docs/skill-evaluation-standard.md) and [integration review](../../docs/verification/skill-creator-integration.md). The JSON manifest specifies a local case, focal skill set, initial fixture requirements, user prompt, expected assertions and required evidence. Named skills are evaluation targets, not an instruction to force-load every skill. Record actual selection.

## Fixture policy

Reuse maintained upstream fixtures when available and rights are established. The existing Spring collection points at rrezartprebreza/spring-boot-skills revision b90c3dbd9699c0bfb81660c857ef58262168e6 and verification/MATRIX.md. The GitHub tree request at that pinned revision returned 404 during Q06 review. No upstream verification project was retrieved or imported; access failure does not establish deletion or license status. All SB fixtures require manual setup. Existing skill examples are not independently runnable projects by default.

Select the actual approved framework line and build wrapper for each fixture. Pin exact Boot, JDK, dependency and database versions after confirming compatibility from primary documentation and resolved dependency evidence. Record repository and fixture revision, Java version, dependency tree and wrapper command. Do not force Boot 4 on SB02. No blanket version combination is certified by this manifest.

Build isolated local fixtures with synthetic records and separate databases or containers. Use matching production database semantics for SQL, isolation, transaction, migration, and concurrency checks. Do not use H2 or mocked repositories as proof of PostgreSQL semantics. For transaction failure checks verify persisted state after the application transaction finishes; test-managed rollback alone is not the oracle. For concurrent checks use independent connections/transactions and bounded synchronization, not uncontrolled sleeps. Verify fixture red/green behavior before model evaluation.

## Execution and evidence

Run a fresh authorized host session per case, with identical starting state for control/baseline/candidate. Provide only the prompt and actual fixture context; keep expectations separate. Use existing build commands and bounded local dependencies; record unavailable Docker/database/SDK as NOT RUN. No live services, credentials, deployment, or paid model execution without explicit authorization.

Preserve original acceptance tests and check changed assertions, build dependencies, security configurations, migrations and unrelated files. Record actual commands, exit codes, test reports, relevant SQL or persisted state, host/model version, loaded skills where observable, and file diff. A compiling project alone does not establish behavior. A passing module structure check does not establish durable events. A mock-only authorization test does not establish real tenant isolation.

All case results remain NOT RUN. There is no executable Spring fixture or measured compatibility evidence in this change.

S04 extends the original fourteen Spring cases with SB15–SB17 for Redis cache authorization, atomic rate limiting and transaction-aware invalidation. Current total: seventeen; the three added fixtures require manual setup and all independent runs remain NOT RUN.
