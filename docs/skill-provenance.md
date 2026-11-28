# Third-party Skill Provenance

Status: Historical content comparison completed for 18 non-Spring skills; Spring Boot resource mapping recorded; redistribution review pending.
Reviewed: 2026-11-28. This register distinguishes observed source declarations from candidate matches. A current upstream license is not proof of the terms at the historical import revision. The repository root MIT license does not replace upstream notices.

## Source groups

| Key | Upstream | License evidence | Revision / remaining work |
| --- | --- | --- | --- |
| A | [Anthropic community plugins](https://github.com/anthropics/claude-plugins-community) | Current root LICENSE is Apache-2.0; eli5 and html-plan plugin manifests declare MIT | Exact imported revision and applicable plugin notices unresolved |
| H | [HumanLayer skills](https://github.com/humanlayer/skills) | Current root MIT, copyright 2026 HumanLayer; plugin-specific LICENSE fetch returned 404 | Historical revision and applicable notices pending |
| E | [web-access](https://github.com/eze-is/web-access) | Upstream SKILL.md declares MIT, author 一泽Eze, version 2.6.0; root LICENSE fetch returned 404 | Confirm revision, full copyright/permission notice, and supporting script licenses |
| M | [Siva Prasad Reddy musings](https://github.com/sivaprasadreddy/musings) | Root LICENSE fetch returned 404; create-spec upstream text matches the pre-remediation local definition in reviewed portions | No license inferred for create-spec, impl-spec or prd-to-requirements; java-code-review also matches MIT-licensed SivaLabs source |
| P | [SivaLabs agent skills](https://github.com/sivaprasadreddy/sivalabs-agent-skills) | Current root MIT, copyright 2026 K Siva Prasad Reddy | Historical content matches for progen and java-code-review; see pinned comparison below |
| T | [Matt Pocock skills](https://github.com/mattpocock/skills) | Current root MIT, copyright 2026 Matt Pocock; repository descriptions identify several matching skill names | Nine historical directories match all local blobs; see pinned comparison below |
| S | [Rrezart Prebreza Spring Boot skills](https://github.com/rrezartprebreza/spring-boot-skills/tree/b90c3dbd9699c0d0bfb81660c857ef58262168e6/skills/spring-boot-4) | Local [upstream MIT notice](../spring-boot-skills/LICENSE) retained | Collection declares b90c3dbd9699c0d0bfb81660c857ef58262168e6; exact mapping and later additions pending |
| U | Unknown or locally authored candidate | No independent attribution established | Inspect history and obtain maintainer confirmation; do not assume original authorship |

## Per-skill register

The following records all 59 entry points. An identical Git blob establishes content equality, not the actual download revision. The 18 non-Spring matches compare every local file in the historical skill directory, not just SKILL.md. Two remaining non-Spring skills have unresolved origins. No new third-party content is imported here.

| Skill | Source | Evidence / status |
| --- | --- | --- |
| [eli5](../collected-skills/eli5/SKILL.md) | A | Historical directory: 1/1 local blobs identical to eli5/skills/eli5 at pinned upstream snapshot below |
| [handoff](../collected-skills/handoff/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/productivity/handoff at pinned upstream snapshot below |
| [retro](../collected-skills/retro/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/engineering/retro at pinned upstream snapshot below |
| [show-me](../collected-skills/show-me/SKILL.md) | H | Historical directory: 1/1 local blobs identical to plugins/show-me/skills/show-me at pinned upstream snapshot below |
| [teach](../collected-skills/teach/SKILL.md) | T | Historical directory: 6/6 local blobs identical to skills/productivity/teach at pinned upstream snapshot below |
| [web-access](../collected-skills/web-access/SKILL.md) | E | Historical directory: 23/23 local blobs identical to repository root at pinned upstream snapshot below |
| [writing-for-agents](../collected-skills/writing-for-agents/SKILL.md) | T | Historical directory: 3/3 local blobs identical to skills/productivity/writing-for-agents at pinned upstream snapshot below |
| [create-spec](../engineering/create-spec/SKILL.md) | M | Historical directory: 2/2 local blobs identical to .agents/skills/create-spec at pinned upstream snapshot below |
| [diagnosing-bugs](../engineering/diagnosing-bugs/SKILL.md) | T | Historical directory: 3/3 local blobs identical to skills/engineering/diagnosing-bugs at pinned upstream snapshot below |
| [domain-modeling](../engineering/domain-modeling/SKILL.md) | T | Historical directory: 4/4 local blobs identical to skills/engineering/domain-modeling at pinned upstream snapshot below |
| [grill-with-docs](../engineering/grill-with-docs/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/engineering/grill-with-docs at pinned upstream snapshot below |
| [grilling](../engineering/grilling/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/productivity/grilling at pinned upstream snapshot below |
| [html-plan](../engineering/html-plan/SKILL.md) | A | Historical directory: 6/6 local blobs identical to html-plan/skills/html-plan at pinned upstream snapshot below |
| [impl-spec](../engineering/impl-spec/SKILL.md) | M | Historical directory: 2/2 local blobs identical to .agents/skills/impl-spec at pinned upstream snapshot below |
| [java-code-review](../engineering/java-code-review/SKILL.md) | M / P | Historical directory: 1/1 local blobs identical to .agents/skills/java-code-review and skills/java-code-review at pinned upstream snapshot below |
| [prd-to-requirements](../engineering/prd-to-requirements/SKILL.md) | M | Historical directory: 2/2 local blobs identical to .agents/skills/prd-to-requirements at pinned upstream snapshot below |
| [product-brainstorming](../engineering/product-brainstorming/SKILL.md) | U | Source unresolved; local PRD path and routing changes |
| [progen](../engineering/progen/SKILL.md) | P | Historical directory: 2/2 local blobs identical to skills/progen at pinned upstream snapshot below |
| [project-foundation](../engineering/project-foundation/SKILL.md) | U | Source unresolved; local delegation changes |
| [prototype](../engineering/prototype/SKILL.md) | T | Historical directory: 4/4 local blobs identical to skills/engineering/prototype at pinned upstream snapshot below |
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

## Historical comparison evidence

Local baseline [7c9b05d](https://github.com/zhijunio/skills/tree/7c9b05def794b4a2190809ab0bb2cfd3d5f93760) contains the pre-reorganization collected directories. [1bba9d3](https://github.com/zhijunio/skills/tree/1bba9d331edfea01a4dc59c919c3b19bbab07510) moves engineering skills into their current collection and modifies some files. These are earliest observed collection-layout commits, not necessarily first introduction into repository history.

| Key | Compared upstream revision | License evidence at that revision |
| --- | --- | --- |
| T | [bbadaa6](https://github.com/mattpocock/skills/tree/bbadaa631c37af594e5a05c9cefacd9956136b18) | [MIT](https://github.com/mattpocock/skills/blob/bbadaa631c37af594e5a05c9cefacd9956136b18/LICENSE) |
| H | [4d69d20](https://github.com/humanlayer/skills/tree/4d69d20f6e9b57b4fcf0d501ccfb1be961f0f329) | [MIT](https://github.com/humanlayer/skills/blob/4d69d20f6e9b57b4fcf0d501ccfb1be961f0f329/LICENSE) |
| P | [6a3a8f2](https://github.com/sivaprasadreddy/sivalabs-agent-skills/tree/6a3a8f2043373a1b8d6c3051bc28aa7a91731f6a) | [MIT](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/6a3a8f2043373a1b8d6c3051bc28aa7a91731f6a/LICENSE) |
| M | [6852f2c](https://github.com/sivaprasadreddy/musings/tree/6852f2c8b60196279cdfafe6c8809f7ecf6eeb69) | No LICENSE/NOTICE/COPYING file found in recursive tree; no grant inferred |
| A | [24479d5](https://github.com/anthropics/claude-plugins-community/tree/24479d5a1fb7a1c4f068a6d3b74d0c0fbc1acfda) | Both plugin manifests declare MIT and name Thariq Shihipar; root Apache-2.0; confirm applicable notice text |
| E | [d173a75](https://github.com/eze-is/web-access/tree/d173a75978ceb2c63fa0b27f7408cd2bf7fe4310) | SKILL.md declares MIT; no LICENSE/NOTICE/COPYING file found in recursive tree |

The matching 18 historical directories cover 68 local files (including SKILL.md and supporting resources). The java-code-review match is counted once. No claim is made that current modified directories are identical. Source and permission for product-brainstorming and project-foundation remain unresolved; obtain maintainer confirmation or a replacement before public distribution.

## Release blockers

- create-spec, impl-spec, prd-to-requirements: exact historical content match but no explicit license found in the upstream tree. Obtain permission or independently replace with appropriately licensed implementation.
- eli5, html-plan: confirm plugin-specific MIT grant and applicable copyright/permission notices, reconcile root Apache-2.0 evidence.
- web-access: confirm complete MIT notice and any additional script/resource terms.
- product-brainstorming, project-foundation: confirm authorship and source/license.
- Spring Boot collection: distinguish verified resource matches from local additions; confirm all remaining files' provenance.
- Ensure source and distributed archives preserve applicable notices, including files copied by individual installation and extraction. No publication while blockers remain.
