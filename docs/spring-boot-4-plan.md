# Replace spring-boot-developer with a house-style Boot 4 skill

Replace `spring-boot-developer` with `spring-boot-4`. The consumer is an agent writing any stack inside a Boot 4 app. Architecture, MVC, MyBatis-Plus, REST, and schema SQL are examples, not the ceiling. The rule is one house spec per stack, as a sivalabs router, templates, and failing checks. PR1 adds the skill and generator. PR2 encodes every in-skill stack. PR3 deletes the old skill.

## How to read this

One box is one unit of work. Every box names the evidence that checks it. A nested box is a sub-step of the box above it. Check a box only when its evidence exists, a file, a log line, a screenshot, a test run, or a SHA. The body is a how-to. The appendices explain and record.

The program runs `pstack/skills/poteto-mode/playbooks/autopilot-stack.md`. The operator lands PR1, PR2, and PR3. Owners stop at merge-ready.

Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

## Program checklist

### Arm the program

- [ ] State the protocol and this plan to the operator, then stop. Start execution only on the operator's explicit go.
- [ ] Read these from trunk at program start. Re-read them at every tick.
  - [ ] `git show origin/main:pstack/skills/poteto-mode/playbooks/autopilot-stack.md`
  - [ ] `git show origin/main:pstack/skills/swarm/SKILL.md`
  - [ ] `git show origin/main:pstack/skills/control-cli/SKILL.md`
  - [ ] `git show origin/main:pstack/skills/poteto-mode/playbooks/opening-a-pr.md`
  - [ ] `git show origin/main:pstack/skills/writing-for-agents/SKILL.md`
- [ ] On the operator's go, arm the audit tick as `/loop 1h` with the tick prompt below. Never leave the cadence to memory.
- [ ] Use this tick prompt, verbatim. "Re-read the execution playbook from trunk. Audit the operation against it and fix drift in this tick. Probe every active lane and judge progress by side effects only. Stand down a stuck lane and dispatch its replacement now. Then post a short status message to the operator in chat only when the audit found a tracked change that no earlier status message reported, such as a PR opened, a code-ready head, a round launched or closed, a verdict, a merge, a stuck agent and the action taken, a blocker added or cleared, or a decision only the operator can make. Name every such change and nothing else. Do not repeat a table, the merged list, or an unchanged blocker. If the audit found none, end the turn with no reply text. Either way, log this tick's row in your decision trail. The row names the items reported, or none."
- [ ] On the operator's hold or stand-down, send every owner a zero-writes order at once.

### Spawn owners

- [ ] Spawn one owner per PR with the full lifecycle the execution playbook names.
- [ ] Follow this dependency graph. Start dependent work only after its parent merges, or base it on the parent branch when the execution playbook stacks.
  - [ ] PR1 is first. It branches from `main`.
  - [ ] PR2 after PR1. Base it on the PR1 branch.
  - [ ] PR3 after PR2. Base it on the PR2 branch.
- [ ] Hold the file boundaries. PR1 and PR2 touch only `spring-boot-4/**`. PR3 may edit `README.md`, sibling `SKILL.md` files, `sdd-skills/sdd-build/SKILL.md`, and may delete `spring-boot-developer/**`.
- [ ] Hold the review gate. PR1, PR2, and PR3 change an agent-facing skill. They wait for the operator's review in chat with screenshots and a video before merge.

### PR mechanics, for every PR

- [ ] Resolve the forge once. Default to `gh`; if `command -v origin` succeeds and Origin can resolve the repository, use `origin pr` for every PR operation. Record any fallback to `gh`. Never require `gt`.
- [ ] Open the PR ready, never draft, per **Opening a PR**. Use the run's built-in PR tool when it has one, else `origin pr create --status open --base <base-branch>` or `gh pr create --base <base-branch>` according to the resolved forge. A stack child targets its parent branch.
- [ ] Run the repo's lint and typecheck once before the PR-facing push. Push with hooks on.
- [ ] Run `/deslop` before each commit and `/no-comments` before review.
- [ ] Triage every Bugbot and security-reviewer comment per `../references/bugbot-triage.md`.
- [ ] Rebase onto current trunk before the code-ready report and babysit. Keep that merge base in fix rounds. Rebase again only at merge prep, on a `git merge-tree` conflict with trunk, on a CI failure that comes from a change on trunk, or when a known concurrent change to the same files lands.

### Verdict and merge, for every PR

- [ ] A full swarm runs once, at the code-ready head. Another full swarm runs only for new behavior, a redesign, or a large new diff. A rebase, a merge-conflict resolution, or a minor fix gets a single verifier at the new head, and a wording-only change gets none. Run each full swarm per `pstack/skills/swarm/SKILL.md`. One gates lane. The ten live lanes from the PR's **Verify, live** block. The perf lane from its **Verify, perf** block. Two or more audit lanes, each with its own focus, that read the diff and the receipts and distrust the PR body. The root audits the receipts in the merge-ready report before the verdict.
- [ ] Judge every finding by the execution playbook's blocking rule. The verdict is clean when none blocks. A failure on trunk behavior in a lane marked `Regression lane against trunk.` is recorded against trunk and does not block the PR. A new head gets a fresh verdict, except for results that stay valid under the patch-id rule in `playbooks/shipping.md`.
- [ ] Stop at verified. Hand the stack to the operator. Do not merge. A restack that leaves `git patch-id` unchanged keeps its verdict.

### Boot recipe, for every live lane

Each live lane runs on its own cloud VM at the PR head. Drive the generator CLI through `control-cli` from `cursor-team-kit` when that skill is present. If it is missing, run the same commands in the lane shell and capture the terminal plus the generated tree as PNG. Appendix C records this gap.

- [ ] `git fetch origin <head-branch> && git checkout <head SHA>`.
- [ ] Install a JDK that matches `spring-boot-4/versions.json` Java default. Confirm `node` and `gh` or `curl` can reach `https://start.spring.io`.
- [ ] Deliver generator input only as CLI flags. Read-only diagnostics are `cat pom.xml`, `find src -type f`, `node scripts/doctor-project.mjs --strict`, and `./mvnw --version`.
- [ ] Save every screenshot to `/tmp/swarm-<pr-id>/worker-<n>/<slug>.png` and return the paths with the report.

## Add the spring-boot-4 skill and generator (PR1)

**Depends on.** None.

**Files.**

- [ ] Create `spring-boot-4/SKILL.md`.
- [ ] Create `spring-boot-4/versions.json`.
- [ ] Create `spring-boot-4/scripts/create-project.mjs`.
- [ ] Create `spring-boot-4/scripts/check-environment.mjs`.
- [ ] Create `spring-boot-4/scripts/doctor-project.mjs`.
- [ ] Create `spring-boot-4/test/smoke-test.mjs`.
- [ ] Create `spring-boot-4/README.md`.

**Build.**

- [ ] Write a model-invoked router in `spring-boot-4/SKILL.md` in the sivalabs shape. Description names Spring Boot 4 application work across the house stacks listed in PR2. Description excludes Java language, PostgreSQL engine diagnosis, SSO protocols, Spring Cloud, and CI pipelines.
- [ ] Pin Boot, Java, MyBatis-Plus, Jackson 3, springdoc, PostgreSQL image, and Flyway in `versions.json`. Default Boot is the live Initializr release at write time. Default Java is 21. Persistence default is MyBatis-Plus. There is no JPA flag in v1. JSON is Jackson 3. OpenAPI is code-first springdoc.
- [ ] Generator downloads Initializr with `web`, `validation`, `actuator`, `postgresql`, `flyway`, and `docker-compose`. Then it injects `mybatis-plus-spring-boot4-starter`, `@MapperScan`, and `springdoc-openapi-starter-webmvc-ui`. Jackson 3 comes from `spring-boot-starter-webmvc`. It does not add `jdbc`, Jackson 2, Testcontainers, extra test classes, or a domain feature. Testing conventions wait for `references/testing.md`.
- [ ] `doctor-project.mjs --strict` fails when Flyway, MyBatis-Plus, MapperScan, springdoc, Jackson 3, or the wrapper is missing, and when Jackson 2, Gson, JSON-B, or an `ObjectMapper` bean is present.

**You see.**

- [ ] `node scripts/create-project.mjs --help` lists `--persistence` with default `mybatis-plus`.
- [ ] A generated `pom.xml` contains `mybatis-plus-spring-boot4-starter`, `spring-boot-starter-flyway`, and `springdoc-openapi-starter-webmvc-ui`, and does not contain `spring-boot-starter-data-jpa` or `spring-boot-jackson2`.
- [ ] `node scripts/doctor-project.mjs --strict <project>` prints `PASS project contract` and exits 0.
- [ ] `node test/smoke-test.mjs` exits 0.

**Verify, unit.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

- [ ] `spring-boot-4/test/smoke-test.mjs` generates a Maven MyBatis-Plus project, runs doctor `--strict`, and runs `./mvnw verify`. Run `node spring-boot-4/test/smoke-test.mjs`.

**Verify, live.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked. Ten lanes on `grok-4.7-xhigh-fast` at the PR head, per the boot recipe.

- [ ] Lane 1. Regression lane against trunk. Run `ls spring-boot-4` at trunk and head. Trunk lacks the directory. Record that. Gate that head generate plus doctor `--strict` exits 0. Save `pr1-regression.png`. Pass when the trunk listing is empty and the head doctor line is `PASS project contract`.
- [ ] Lane 2. Generate with no persistence flag. Save `pr1-default-mp.png`. Pass when `pom.xml` contains `mybatis-plus-spring-boot4-starter` and does not contain `spring-boot-starter-data-jpa`.
- [ ] Lane 3. Confirm Flyway on Boot 4. Save `pr1-flyway.png`. Pass when `pom.xml` contains `spring-boot-starter-flyway` and `flyway-database-postgresql`.
- [ ] Lane 4. Confirm live Boot pin. Save `pr1-boot-pin.png`. Pass when the generated parent version equals `versions.json` `springBoot.default` and that id exists in `https://start.spring.io/metadata/client`.
- [ ] Lane 5. Confirm Java 21. Save `pr1-java.png`. Pass when the generated Java release is 21.
- [ ] Lane 6. Confirm MapperScan. Save `pr1-mapperscan.png`. Pass when `Application.java` has `@MapperScan`.
- [ ] Lane 7. Reject Boot 3. Save `pr1-reject-boot3.png`. Pass when `--boot-version 3.5.9` exits non-zero.
- [ ] Lane 8. Doctor fails without Flyway. Save `pr1-doctor-flyway.png`. Pass when removing the Flyway dependency makes doctor `--strict` exit 1.
- [ ] Lane 9. Wrapper `verify` on the empty generated app. Save `pr1-verify.png`. Pass when `./mvnw verify` exits 0.
- [ ] Lane 10. Output stays outside the skill tree. Save `pr1-output-dir.png`. Pass when `--output-dir` writes the app under the given workspace and `spring-boot-4/` gains no generated sources.

**Verify, perf.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

- [ ] Metric. Wall time of one generator run. Trunk runs `spring-boot-developer/scripts/create-project.mjs --name trunk-app --output-dir /tmp/pr1-perf`. Head runs `spring-boot-4/scripts/create-project.mjs --name head-app --output-dir /tmp/pr1-perf`. Also record head doctor `--strict` time as the diff-added work, and the time until doctor prints PASS as the user-visible end state.
- [ ] Probe. Interleave trunk then head, three pairs. Use `/usr/bin/time -p`. Both sides must print a real elapsed value.
- [ ] Baseline. Record the trunk elapsed seconds first.
- [ ] Rule. Head generator elapsed must stay under 120s. Head doctor `--strict` must stay under 5s. Do not fail on a ratio against the old JPA generator, because the stacks differ.

**Review gate.** The operator reviews before merge.

- [ ] Copy lane 2 and lane 4 screenshots into `docs/media/pr1-review-default-mp.png` and `docs/media/pr1-review-boot-pin.png`.
- [ ] Record a 30 to 60 second video of generate plus doctor on a lane VM. Save it as `docs/media/pr1-review.mp4`.
- [ ] Post the screenshots and the video in chat. Stop at merge-ready. Wait for the operator's click.

**Merge.**

- [ ] Root's clean verdict at the exact head SHA.
- [ ] Bugbot triage done.
- [ ] Rebased onto current trunk after the verdict, patch-id unchanged.
- [ ] The root appends PR1 to the base-branch stack and the operator lands it bottom-up.

## Encode house-style templates and checks (PR2)

**Depends on.** PR1.

**Files.**

- [ ] Create `spring-boot-4/references/architecture.md`.
- [ ] Create `spring-boot-4/references/mvc.md`.
- [ ] Create `spring-boot-4/references/mybatis-plus.md`.
- [ ] Create `spring-boot-4/references/rest.md`.
- [ ] Create `spring-boot-4/references/schema-sql.md`.
- [ ] Create `spring-boot-4/references/validation.md`.
- [ ] Create `spring-boot-4/references/error-handling.md`.
- [ ] Create `spring-boot-4/references/json-jackson.md`.
- [ ] Create `spring-boot-4/references/transactions.md`.
- [ ] Create `spring-boot-4/references/configuration.md`.
- [ ] Create `spring-boot-4/references/testing.md`.
- [ ] Create `spring-boot-4/references/maven.md`.
- [ ] Create `spring-boot-4/references/docker-compose.md`.
- [ ] Create `spring-boot-4/references/security.md`.
- [ ] Create `spring-boot-4/references/observability.md`.
- [ ] Create `spring-boot-4/references/http-clients.md`.
- [ ] Create `spring-boot-4/references/caching.md`.
- [ ] Create `spring-boot-4/references/async.md`.
- [ ] Create `spring-boot-4/references/messaging.md`.
- [ ] Create `spring-boot-4/references/openapi.md`.
- [ ] Create `spring-boot-4/references/resilience.md`.
- [ ] Create `spring-boot-4/references/gradle.md`.
- [ ] Create `spring-boot-4/references/api-versioning.md`.
- [ ] Create `spring-boot-4/references/file-storage.md`.
- [ ] Create `spring-boot-4/references/mail.md`.
- [ ] Create `spring-boot-4/assets/templates/` with Controller, Service, Mapper, entity, Request, Response, advice, Flyway SQL, web-slice test, persistence-slice test, and Taikai test templates.
- [ ] Edit `spring-boot-4/SKILL.md` so create and change workflows route to the matching stack file. Inline only what every branch needs.
- [ ] Edit `spring-boot-4/scripts/create-project.mjs` to copy the default-stack templates into a feature package.
- [ ] Edit `spring-boot-4/scripts/doctor-project.mjs` to fail on missing feature packages, annotation SQL, entity-returning controllers, FK constraints, missing `ProblemDetail` advice, `ObjectMapper` beans, and missing slice tests.
- [ ] Edit `spring-boot-4/assets/templates/TaikaiArchitectureTest.java.tmpl` so controllers must not depend on Mapper types.

**Build.**

- [ ] `SKILL.md` is a router over every in-skill stack. Each reference is short rules, one copy-paste template, and a Verify command. Do not restore the 75-file encyclopedia.
- [ ] Default stack is generated every time. Architecture, MVC, MyBatis-Plus, REST, schema SQL, Flyway, validation, RFC 9457 errors, Jackson 3, OpenAPI, transactions, configuration, testing, Maven, Compose, security filter-chain defaults, and Actuator.
- [ ] On-demand stacks get a reference and are added only when the task names them. HTTP clients, Redis cache, async and scheduling, messaging, resilience, Gradle, API versioning, file storage, and mail.
- [ ] Architecture follows sivalabs module packages. One feature owns `api`, `domain`, and persistence types. Do not emit top-level `controller/`, `service/`, and `mapper/` packages.
- [ ] MVC follows piomin role templates inside that feature. Controller binds HTTP. Service owns `@Transactional`. Mapper extends `BaseMapper` and uses Lambda Wrappers. No SQL in annotations or XML.
- [ ] REST uses plural `/api/{resources}`, `*Request` and `*Response` records, `@Valid`, and RFC 9457 `ProblemDetail`.
- [ ] Schema SQL is Flyway. Singular `snake_case` tables. `bigint` primary keys. No `FOREIGN KEY` by default. Unique and `NOT NULL` stay.
- [ ] Testing follows sivalabs levels. Unit for domain logic. Web slice for MVC. Persistence slice plus Testcontainers for SQL. One startup smoke test.
- [ ] Security house rules cover filter chains, CORS, CSRF, and constructor injection. OIDC, SAML, and IdP work stay a pointer to `spring-sso-developer`.
- [ ] Generator emits one sample feature named by `--name` so the agent copies a real shape. Doctor and Taikai fail when that shape is broken.

**You see.**

- [ ] A generated tree contains `{feature}/*Controller.java`, `{feature}/*Service.java`, `{feature}/*Mapper.java`, `src/main/resources/db/migration/V1__*.sql`, a web-slice test, and a persistence-slice test.
- [ ] `./mvnw verify` on the generated app exits 0, including the architecture test.
- [ ] Moving the Mapper injection into the Controller makes the architecture test fail.
- [ ] Adding `FOREIGN KEY` to the Flyway file makes doctor `--strict` exit 1.
- [ ] Declaring an `ObjectMapper` `@Bean` makes doctor `--strict` exit 1.
- [ ] `SKILL.md` has a route line for each in-skill stack file listed above.

**Verify, unit.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

- [ ] `spring-boot-4/test/smoke-test.mjs` asserts the sample feature files exist, slice tests exist, and doctor catches a controller-to-mapper mutation plus an `ObjectMapper` bean. Run `node spring-boot-4/test/smoke-test.mjs`.

**Verify, live.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked. Ten lanes on `grok-4.7-xhigh-fast` at the PR head, per the boot recipe.

- [ ] Lane 1. Regression lane against trunk. Generate at the PR1 parent and at PR2 head. Record that PR1 has no sample feature. Gate that PR2 generate has Controller, Service, Mapper, Flyway SQL, and slice tests. Save `pr2-regression.png`. Pass when the PR2 tree lists those artifacts and `./mvnw verify` exits 0.
- [ ] Lane 2. Feature package layout. Save `pr2-packages.png`. Pass when Java sources live under a feature package and not under `controller/`, `service/`, or `mapper/` at the application root.
- [ ] Lane 3. MVC roles. Save `pr2-mvc.png`. Pass when the Controller has no `@Transactional`, the Service has it, and the Mapper extends `BaseMapper`.
- [ ] Lane 4. MyBatis-Plus wrappers. Save `pr2-wrappers.png`. Pass when the Mapper file has no `@Select`, `@Insert`, `@Update`, `@Delete`, or mapper XML.
- [ ] Lane 5. REST, validation, and errors. Save `pr2-rest.png`. Pass when the Controller uses `/api/` plus a plural noun, `@Valid` on the request record, and a `@RestControllerAdvice` returning `ProblemDetail`.
- [ ] Lane 6. Schema SQL. Save `pr2-schema.png`. Pass when the Flyway file uses a singular snake_case table, a `bigint` primary key, and no `FOREIGN KEY`.
- [ ] Lane 7. Testing stack. Save `pr2-testing.png`. Pass when the generated app has a `@WebMvcTest` and a Testcontainers persistence test, and both run under `./mvnw verify`.
- [ ] Lane 8. Jackson 3 and config. Save `pr2-jackson.png`. Pass when no `ObjectMapper` bean exists, JSON config uses `JsonMapper` guidance, and `application.yaml` binds through `@ConfigurationProperties`.
- [ ] Lane 9. Security and on-demand routes. Save `pr2-security.png`. Pass when a Security filter-chain config exists, and `SKILL.md` has route lines for HTTP clients, cache, async, messaging, resilience, Gradle, API versioning, file storage, and mail.
- [ ] Lane 10. `./mvnw verify` on the untouched generated app. Save `pr2-verify.png`. Pass when verify exits 0.

**Verify, perf.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

- [ ] Metric. Wall time of generate plus `./mvnw verify` at the PR1 parent and at PR2 head. Also record the added template copy time and the time until verify exits 0.
- [ ] Probe. Interleave parent then head, three pairs. `/usr/bin/time -p` on generate, then on `./mvnw verify`.
- [ ] Baseline. Record the PR1 parent generate plus verify seconds first.
- [ ] Rule. Head generate plus verify must stay under 180s. Head must not be more than 30s slower than the parent generate plus verify, except the first Maven download on a cold VM.

**Review gate.** The operator reviews before merge.

- [ ] Copy lane 2, lane 5, and lane 7 screenshots into `docs/media/pr2-review-packages.png`, `docs/media/pr2-review-rest.png`, and `docs/media/pr2-review-testing.png`.
- [ ] Record a 30 to 60 second video of generate, tree, and `./mvnw verify` on a lane VM. Save it as `docs/media/pr2-review.mp4`.
- [ ] Post the screenshots and the video in chat. Stop at merge-ready. Wait for the operator's click.

**Merge.**

- [ ] Root's clean verdict at the exact head SHA.
- [ ] Bugbot triage done.
- [ ] Rebased onto the PR1 branch after the verdict, patch-id unchanged.
- [ ] The root appends PR2 to the stack and the operator lands it bottom-up.

## Replace spring-boot-developer (PR3)

**Depends on.** PR2.

**Files.**

- [ ] Delete `spring-boot-developer/`.
- [ ] Edit `README.md`.
- [ ] Edit `sdd-skills/sdd-build/SKILL.md`.
- [ ] Edit `java-performance-developer/SKILL.md`.
- [ ] Edit `spring-cloud-developer/SKILL.md`.
- [ ] Edit `postgresql-developer/SKILL.md`.

**Build.**

- [ ] Remove `spring-boot-developer` from the repo and from SkillsJars by deleting its directory.
- [ ] Point every remaining `spring-boot-developer` mention at `spring-boot-4`.
- [ ] Keep Vue, Azure, Kubernetes, GraalVM, Spring AI, MCP, Batch, JDTLS, and Git out of this skill. If a sample README still names the old skill, retarget or delete that sample in this PR only when it would otherwise keep the old name.

**You see.**

- [ ] `ls spring-boot-developer` fails.
- [ ] `./mvnw clean verify` exits 0 and `jar tf target/skills-*.jar` lists `META-INF/skills/spring-boot-4/` and does not list `META-INF/skills/spring-boot-developer/`.
- [ ] `README.md` table row for Boot names `spring-boot-4`.

**Verify, unit.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

- [ ] Repo package test. Run `./mvnw clean verify` and `jar tf target/skills-0.1.0-SNAPSHOT.jar | grep '^META-INF/skills/'`.

**Verify, live.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked. Ten lanes on `grok-4.7-xhigh-fast` at the PR head, per the boot recipe.

- [ ] Lane 1. Regression lane against trunk. List skill directories at trunk and head. Trunk still has `spring-boot-developer`. Record that. Gate that head has `spring-boot-4` and no `spring-boot-developer`. Save `pr3-regression.png`. Pass when the head listing matches that pair.
- [ ] Lane 2. SkillsJars contents. Save `pr3-jar.png`. Pass when the jar contains `spring-boot-4` and does not contain `spring-boot-developer`.
- [ ] Lane 3. README index. Save `pr3-readme.png`. Pass when the skills table links `spring-boot-4/SKILL.md`.
- [ ] Lane 4. sdd-build pointer. Save `pr3-sdd.png`. Pass when `sdd-skills/sdd-build/SKILL.md` names `spring-boot-4`.
- [ ] Lane 5. postgresql-developer pair line. Save `pr3-postgres.png`. Pass when it pairs with `spring-boot-4`.
- [ ] Lane 6. No leftover path. Save `pr3-grep.png`. Pass when `rg spring-boot-developer --glob '!docs/spring-boot-4-plan.md'` returns no paths under the repo root except git history.
- [ ] Lane 7. New skill still generates. Save `pr3-generate.png`. Pass when `node spring-boot-4/scripts/create-project.mjs` plus doctor `--strict` plus `./mvnw verify` exits 0.
- [ ] Lane 8. Description exclusion. Save `pr3-description.png`. Pass when `spring-boot-4/SKILL.md` frontmatter description contains `Not for` Java language, PostgreSQL engine, SSO, Spring Cloud, and CI pipelines.
- [ ] Lane 9. Sibling Java skill still owns language. Save `pr3-java-sibling.png`. Pass when `java-developer/SKILL.md` is unchanged in role and `spring-boot-4/SKILL.md` does not restate records, sealed types, or Error Prone.
- [ ] Lane 10. `./mvnw clean verify` at head. Save `pr3-mvn.png`. Pass when the Maven build exits 0.

**Verify, perf.** Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

- [ ] Metric. `./mvnw clean verify` wall time at the PR2 parent and at PR3 head. Also record jar listing time as the diff-added work, and the time until the grep of `META-INF/skills/` returns as the user-visible end state.
- [ ] Probe. Interleave parent then head, three pairs. `/usr/bin/time -p ./mvnw clean verify`.
- [ ] Baseline. Record the PR2 parent elapsed seconds first.
- [ ] Rule. Head Maven verify must stay within 20% of the parent, or under 180s on a cold VM, whichever is larger.

**Review gate.** The operator reviews before merge.

- [ ] Copy lane 1 and lane 2 screenshots into `docs/media/pr3-review-regression.png` and `docs/media/pr3-review-jar.png`.
- [ ] Record a 30 to 60 second video of the jar listing and the README table on a lane VM. Save it as `docs/media/pr3-review.mp4`.
- [ ] Post the screenshots and the video in chat. Stop at merge-ready. Wait for the operator's click.

**Merge.**

- [ ] Root's clean verdict at the exact head SHA.
- [ ] Bugbot triage done.
- [ ] Rebased onto the PR2 branch after the verdict, patch-id unchanged.
- [ ] The root appends PR3 to the stack and the operator lands it bottom-up.

## Close the program

- [ ] Every box above is checked with its evidence.
- [ ] Reply to the operator with the report the execution playbook names.

## Appendix A. Prototype evidence

Live Initializr metadata on 2026-11-24, measured with `https://start.spring.io/metadata/client`. Default Boot is `4.2.0.RELEASE`. `4.1.2` is absent. Default Java is 17. `web` still maps to Spring MVC.

Current generator, measured by reading `spring-boot-developer/scripts/create-project.mjs` and `doctor-project.mjs` without running them. Default persistence is JPA. No Controller, Service, Mapper, or Flyway SQL is emitted. Doctor checks build and config only.

GitHub skills, measured by fetching SKILL.md and references. sivalabs is a 6KB router with one reference per concern. piomin inlines MVC templates so CRUD looks the same. Dancan `spring-scaffold` copies templates into a feature-sliced tree. 0avi is the encyclopedia family that this repo already copied. jdubois is a generator plus `versions.json` plus live Boot fetch, with a frontend encyclopedia we will not copy.

Unproven. Whether Taikai fails a MyBatis `@Mapper` named `*Mapper` under the old repository-name rule. Whether the Boot 4 MyBatis-Plus starter scans mappers without `@MapperScan`. Run those two checks in PR1 smoke-test, not as a guess.

## Appendix B. Alternatives rejected

Patch `spring-boot-developer` in place. Rejected because the 128-file encyclopedia is the 0avi shape the operator already rejected, and the generator cannot grow house style without a redesign.

Keep two skills. Rejected because both descriptions would fire on "create a Spring Boot app".

Copy piomin global `controller` / `service` / `repository` packages. Rejected because sivalabs and Dancan put MVC roles inside a feature module, which matches the operator's architecture plus MVC ask.

Copy Dancan micro-skills. Rejected because one Boot 4 house style is one skill with two workflows.

Copy Dancan Lombok and OpenTelemetry defaults. Rejected because this house forbids Lombok and does not need OTel in v1.

Keep JPA as a generator flag. Rejected because the operator named MyBatis-Plus as the persistence practice.

Limit the skill to architecture, MVC, MyBatis-Plus, REST, and schema SQL. Rejected because the operator asked for a house spec on every stack a Boot project uses.

Restore the 75-file encyclopedia so every old topic has a long guide. Rejected because sivalabs and Dancan keep one short file per stack. Coverage is the router map. Depth is rules, a template, and Verify.

Put OIDC, SAML, Gateway, pipelines, and Java language into this skill. Rejected because sibling skills already own those domains. This skill points at them.

Put Vue, Azure, Kubernetes, GraalVM, Spring AI, MCP, Batch, JDTLS, and Git into v1. Rejected because those topics bloated the old skill and are not required to write a consistent Boot 4 service. Add a stack later as one short reference when the operator names it.

Include Boot 3.5 to 4 migration in v1. Rejected because the job is consistent new Boot 4 apps.

## Appendix C. Risks

`control-cli` is not installed in this workspace. PR1 through PR3 live lanes still run the CLI and must save PNGs. The owner watches for a missing control skill and falls back to shell capture.

Live Initializr can drop a pinned Boot id. PR1 smoke-test must fail closed when `versions.json` is absent from metadata.

`postgresql-developer` requires declared foreign keys. This house omits them. PR2 doctor enforces omit-FK. PR3 description must say Boot-app schema conventions live here, and engine EXPLAIN lives in `postgresql-developer`.

A later task can name Redis or Kafka. PR2 must already have the on-demand route and the short file, or the agent will improvise. The owner watches that on-demand files exist even when the generator does not emit those dependencies. OpenAPI is default-stack in PR1.

Sibling `java-developer` still triggers on "writing Java". PR3 cannot retarget that skill's description without a separate product call.

Deleting `spring-boot-developer` will break bookmarked workshop prompts. PR3 retargets in-repo pointers only.

## Appendix D. Links and reading list

Read before PR1. [sivalabs spring-boot SKILL.md](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/main/skills/spring-boot/SKILL.md), [piomin spring-boot SKILL.md](https://github.com/piomin/claude-ai-spring-boot/blob/main/skills/spring-boot/SKILL.md), [Dancan spring-scaffold SKILL.md](https://github.com/Dancan254/spring-boot-skills/blob/main/skills/spring-scaffold/SKILL.md), [jdubois dr-jskill SKILL.md](https://github.com/jdubois/dr-jskill/blob/main/SKILL.md), [0avi spring-boot-developer SKILL.md](https://github.com/0avi/skills/blob/main/spring-boot-developer/SKILL.md).

In-skill default stack. Architecture, MVC, MyBatis-Plus, REST, schema SQL, Flyway, validation, errors, Jackson 3, OpenAPI, transactions, configuration, testing, Maven, Compose, security defaults, Actuator.

In-skill on demand. HTTP clients, Redis cache, async, messaging, resilience, Gradle, API versioning, file storage, mail.

Pointer only. Java language to `java-developer`. Postgres engine work to `postgresql-developer`. OIDC, SAML, and IdP to `spring-sso-developer`. Gateway and Cloud to `spring-cloud-developer`. Pipelines to `ci-cd-developer`. JVM performance to `java-performance-developer`.

Out of v1. Vue, React, Angular, Next, Thymeleaf, WebSocket, HATEOAS, i18n, Azure, Kubernetes, GraalVM, Spring AI, MCP, Batch, JDTLS, Git, hexagonal architecture, Boot 3.5 migration. Add one short reference later if the operator names that stack.

Read `pstack/skills/writing-for-agents/SKILL.md` and `pstack/skills/writing-for-agents/SKILL-MECHANICS.md` before editing `spring-boot-4/SKILL.md`.

PR1 and PR2 take `pstack/skills/how/SKILL.md` before coding, because the layout is a subsystem. PR2 also takes `pstack/skills/interrogate/SKILL.md` on the architecture plus MVC template choice.

Keep a local decision trail per `pstack/skills/show-me-your-work/SKILL.md`. Commit it only if the operator asks for an auditable record.
