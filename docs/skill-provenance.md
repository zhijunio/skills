# Third-party Skill Provenance

Status: Initial register; historical import revisions and redistribution review pending.
Reviewed: 2026-11-28. This register distinguishes observed source declarations from candidate matches. A current upstream license is not proof of the terms at the historical import revision. The repository root MIT license does not replace upstream notices.

## Source groups

| Key | Upstream | License evidence | Revision / remaining work |
| --- | --- | --- | --- |
| A | [Anthropic community plugins](https://github.com/anthropics/claude-plugins-community) | Current root LICENSE is Apache-2.0; eli5 and html-plan plugin manifests declare MIT | Exact imported revision and applicable plugin notices unresolved |
| H | [HumanLayer skills](https://github.com/humanlayer/skills) | Current root MIT, copyright 2026 HumanLayer; plugin-specific LICENSE fetch returned 404 | Historical revision and applicable notices pending |
| E | [web-access](https://github.com/eze-is/web-access) | Upstream SKILL.md declares MIT, author 一泽Eze, version 2.6.0; root LICENSE fetch returned 404 | Confirm revision, full copyright/permission notice, and supporting script licenses |
| M | [Siva Prasad Reddy musings](https://github.com/sivaprasadreddy/musings) | Root LICENSE fetch returned 404; create-spec upstream text matches the pre-remediation local definition in reviewed portions | No license inferred; verify permission before further redistribution |
| P | [SivaLabs agent skills](https://github.com/sivaprasadreddy/sivalabs-agent-skills) | Current root MIT, copyright 2026 K Siva Prasad Reddy | Historical import revision and skill-specific notices pending |
| T | [Matt Pocock skills](https://github.com/mattpocock/skills) | Current root MIT, copyright 2026 Matt Pocock; repository descriptions identify several matching skill names | Candidate only; exact original paths, revision and content comparison pending |
| S | [Rrezart Prebreza Spring Boot skills](https://github.com/rrezartprebreza/spring-boot-skills/tree/b90c3dbd9699c0d0bfb81660c857ef58262168e6/skills/spring-boot-4) | Local [upstream MIT notice](../spring-boot-skills/LICENSE) retained | Collection declares b90c3dbd9699c0d0bfb81660c857ef58262168e6; exact mapping and later additions pending |
| U | Unknown or locally authored candidate | No independent attribution established | Inspect history and obtain maintainer confirmation; do not assume original authorship |

## Per-skill register

The following records all 59 entry points. “Declared” means a local collection index or metadata identifies a source; “candidate” means the source needs confirmation. All imported revisions remain unconfirmed except the collection-level S baseline. No new third-party content is imported here.

| Skill | Source | Evidence / status |
| --- | --- | --- |
| [eli5](../collected-skills/eli5/SKILL.md) | A | Declared eli5; plugin manifest MIT |
| [handoff](../collected-skills/handoff/SKILL.md) | T | Candidate |
| [retro](../collected-skills/retro/SKILL.md) | T | Candidate |
| [show-me](../collected-skills/show-me/SKILL.md) | H | Declared plugins/show-me |
| [teach](../collected-skills/teach/SKILL.md) | T | Candidate |
| [web-access](../collected-skills/web-access/SKILL.md) | E | Declared in frontmatter and index; local consent and scope changes |
| [writing-for-agents](../collected-skills/writing-for-agents/SKILL.md) | T | Candidate |
| [create-spec](../engineering/create-spec/SKILL.md) | M | Declared .agents/skills/create-spec; local contract changes |
| [diagnosing-bugs](../engineering/diagnosing-bugs/SKILL.md) | T | Candidate |
| [domain-modeling](../engineering/domain-modeling/SKILL.md) | T | Candidate; local responsibility changes |
| [grill-with-docs](../engineering/grill-with-docs/SKILL.md) | T | Candidate; local portable composition changes |
| [grilling](../engineering/grilling/SKILL.md) | T | Candidate; local routing changes |
| [html-plan](../engineering/html-plan/SKILL.md) | A | Declared html-plan; plugin manifest MIT, Thariq Shihipar |
| [impl-spec](../engineering/impl-spec/SKILL.md) | M | Declared .agents/skills/impl-spec; local contract changes |
| [java-code-review](../engineering/java-code-review/SKILL.md) | M | Declared .agents/skills/java-code-review |
| [prd-to-requirements](../engineering/prd-to-requirements/SKILL.md) | M | Declared .agents/skills/prd-to-requirements; local contract changes |
| [product-brainstorming](../engineering/product-brainstorming/SKILL.md) | U | Source unresolved; local PRD path and routing changes |
| [progen](../engineering/progen/SKILL.md) | P | Declared skills/progen; local scope and installation changes |
| [project-foundation](../engineering/project-foundation/SKILL.md) | U | Source unresolved; local delegation changes |
| [prototype](../engineering/prototype/SKILL.md) | T | Candidate |
| [agent-qa-browser-regression](../spring-boot-skills/agent-qa-browser-regression/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [archunit](../spring-boot-skills/archunit/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [bootui](../spring-boot-skills/bootui/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [event-driven-messaging](../spring-boot-skills/event-driven-messaging/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [feature-architecture](../spring-boot-skills/feature-architecture/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [flyway-migrations](../spring-boot-skills/flyway-migrations/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [hexagonal-architecture](../spring-boot-skills/hexagonal-architecture/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [http-interface-clients](../spring-boot-skills/http-interface-clients/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [idempotency-patterns](../spring-boot-skills/idempotency-patterns/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [jvm-diagnostics](../spring-boot-skills/jvm-diagnostics/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [layered-architecture](../spring-boot-skills/layered-architecture/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [mise](../spring-boot-skills/mise/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [multi-module-maven](../spring-boot-skills/multi-module-maven/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [multi-tenancy](../spring-boot-skills/multi-tenancy/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [null-safety](../spring-boot-skills/null-safety/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [postgresql](../spring-boot-skills/postgresql/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [resilience-retry](../spring-boot-skills/resilience-retry/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-ai](../spring-boot-skills/spring-ai/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-api-versioning](../spring-boot-skills/spring-boot-api-versioning/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-cicd](../spring-boot-skills/spring-boot-cicd/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-configuration](../spring-boot-skills/spring-boot-configuration/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-docker](../spring-boot-skills/spring-boot-docker/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-email](../spring-boot-skills/spring-boot-email/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-frontend-integration](../spring-boot-skills/spring-boot-frontend-integration/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-gradle](../spring-boot-skills/spring-boot-gradle/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-kubernetes](../spring-boot-skills/spring-boot-kubernetes/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-maven](../spring-boot-skills/spring-boot-maven/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-mybatis-plus](../spring-boot-skills/spring-boot-mybatis-plus/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-observability](../spring-boot-skills/spring-boot-observability/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-openapi](../spring-boot-skills/spring-boot-openapi/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-scaffolding](../spring-boot-skills/spring-boot-scaffolding/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-security](../spring-boot-skills/spring-boot-security/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-testing](../spring-boot-skills/spring-boot-testing/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot-webmvc](../spring-boot-skills/spring-boot-webmvc/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-boot](../spring-boot-skills/spring-boot/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-data-jpa](../spring-boot-skills/spring-data-jpa/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-data-redis](../spring-boot-skills/spring-data-redis/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-modulith](../spring-boot-skills/spring-modulith/SKILL.md) | S | Collection attribution; exact file mapping pending |
| [spring-task-execution](../spring-boot-skills/spring-task-execution/SKILL.md) | S | Collection attribution; exact file mapping pending |

## Required follow-up

- Inspect local introduction commits and compare each file and supporting resource to a pinned upstream revision; record paths, revisions and local changes.
- Verify applicable license text and copyright notices at those revisions, including plugin-specific overrides and third-party dependencies.
- Preserve required notices in source and distributed artifacts. For unclear rights, do not add the affected content to new distributions until permission or a compatible replacement is established. Review existing public distribution with the maintainer; this register alone does not resolve licensing.
- Do not bulk replace notices or label candidate matches as confirmed. Keep this register synchronized with future imports and adaptations.
- F09 must verify that packaged resources include required notices; no release is authorized by this document.

See [initial audit](audits/initial-skill-audit.md) and [remediation tracker](audits/remediation-plan.md) for change evidence. This is an engineering provenance register, not a legal determination.
