# Third-party Skill Provenance

Status: Historical content comparison completed for 18 non-Spring skills; Spring Boot resource mapping recorded; redistribution review pending.
Reviewed: 2026-11-28. This register distinguishes observed source declarations from candidate matches. A current upstream license is not proof of the terms at the historical import revision. The repository root MIT license does not replace upstream notices.

## Source groups

| Key | Upstream | License evidence | Revision / remaining work |
| --- | --- | --- | --- |
| A | [Anthropic community plugins](https://github.com/anthropics/claude-plugins-community) | Current root LICENSE is Apache-2.0; eli5 and html-plan plugin manifests declare MIT | Exact imported revision and applicable plugin notices unresolved |
| H | [HumanLayer skills](https://github.com/humanlayer/skills) | Current root MIT, copyright 2026 HumanLayer; plugin-specific LICENSE fetch returned 404 | Historical revision and applicable notices pending |
| E | [web-access](https://github.com/eze-is/web-access) | Upstream SKILL.md declares MIT, author 一泽Eze, version 2.6.0; root LICENSE fetch returned 404 | Confirm revision, full copyright/permission notice, and supporting script licenses |
| M | [Siva Prasad Reddy musings](https://github.com/sivaprasadreddy/musings) | Maintainer explicitly confirmed Siva as source of the three SDD skills; historical directory blobs match the pinned musings snapshot; no LICENSE/NOTICE/COPYING found | Maintainer confirms MIT for create-spec, impl-spec and prd-to-requirements; upstream license text or author grant remains to be independently located; java-code-review also matches MIT-licensed SivaLabs source |
| P | [SivaLabs agent skills](https://github.com/sivaprasadreddy/sivalabs-agent-skills) | Current root MIT, copyright 2026 K Siva Prasad Reddy | Historical content matches for progen and java-code-review; see pinned comparison below |
| T | [Matt Pocock skills](https://github.com/mattpocock/skills) | Current root MIT, copyright 2026 Matt Pocock; repository descriptions identify several matching skill names | Nine historical directories match all local blobs; see pinned comparison below |
| S | [Rrezart Prebreza Spring Boot skills](https://github.com/rrezartprebreza/spring-boot-skills/tree/b90c3dbd9699c0d0bfb81660c857ef58262168e6/skills/spring-boot-4) | Local [upstream MIT notice](../spring-boot-skills/LICENSE) retained | Collection declares b90c3dbd9699c0d0bfb81660c857ef58262168e6; exact mapping and later additions pending |
| O | Original skills by repository maintainer (Zhijunio) | Authorship explicitly confirmed by maintainer on 2026-11-28; repository root MIT license is the documented default | No third-party upstream identified for these skills; any separately incorporated third-party material remains subject to its own terms |

## Per-skill register

The following records all 59 entry points. An identical Git blob establishes content equality, not the actual download revision. The 18 non-Spring matches compare every local file in the historical skill directory, not just SKILL.md. The remaining two non-Spring skills are confirmed maintainer-authored. No new third-party content is imported here.

| Skill | Source | Evidence / status |
| --- | --- | --- |
| [eli5](../collected-skills/eli5/SKILL.md) | A | Historical directory: 1/1 local blobs identical to eli5/skills/eli5 at pinned upstream snapshot below |
| [handoff](../collected-skills/handoff/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/productivity/handoff at pinned upstream snapshot below |
| [retro](../collected-skills/retro/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/engineering/retro at pinned upstream snapshot below |
| [show-me](../collected-skills/show-me/SKILL.md) | H | Historical directory: 1/1 local blobs identical to plugins/show-me/skills/show-me at pinned upstream snapshot below |
| [teach](../collected-skills/teach/SKILL.md) | T | Historical directory: 6/6 local blobs identical to skills/productivity/teach at pinned upstream snapshot below |
| [web-access](../collected-skills/web-access/SKILL.md) | E | Historical directory: 23/23 local blobs identical to repository root at pinned upstream snapshot below |
| [writing-for-agents](../collected-skills/writing-for-agents/SKILL.md) | T | Historical directory: 3/3 local blobs identical to skills/productivity/writing-for-agents at pinned upstream snapshot below |
| [behavior-specification](../engineering/behavior-specification/SKILL.md) | M | Maintainer-confirmed Siva origin and MIT license; historical directory: 2/2 local blobs identical to .agents/skills/create-spec at pinned upstream snapshot below |
| [diagnosing-bugs](../engineering/diagnosing-bugs/SKILL.md) | T | Historical directory: 3/3 local blobs identical to skills/engineering/diagnosing-bugs at pinned upstream snapshot below |
| [domain-modeling](../engineering/domain-modeling/SKILL.md) | T | Historical directory: 4/4 local blobs identical to skills/engineering/domain-modeling at pinned upstream snapshot below |
| [grill-with-docs](../engineering/grill-with-docs/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/engineering/grill-with-docs at pinned upstream snapshot below |
| [grilling](../engineering/grilling/SKILL.md) | T | Historical directory: 2/2 local blobs identical to skills/productivity/grilling at pinned upstream snapshot below |
| [html-plan](../engineering/html-plan/SKILL.md) | A | Historical directory: 6/6 local blobs identical to html-plan/skills/html-plan at pinned upstream snapshot below |
| [implement-use-case](../engineering/implement-use-case/SKILL.md) | M | Maintainer-confirmed Siva origin and MIT license; historical directory: 2/2 local blobs identical to .agents/skills/impl-spec at pinned upstream snapshot below |
| [java-code-review](../engineering/java-code-review/SKILL.md) | M / P | Historical directory: 1/1 local blobs identical to .agents/skills/java-code-review and skills/java-code-review at pinned upstream snapshot below |
| [prd-to-use-cases](../engineering/prd-to-use-cases/SKILL.md) | M | Maintainer-confirmed Siva origin and MIT license; historical directory: 2/2 local blobs identical to .agents/skills/prd-to-requirements at pinned upstream snapshot below |
| [product-requirements](../engineering/product-requirements/SKILL.md) | O | Maintainer-authored (explicit confirmation); local PRD path and routing changes |
| [progen](../engineering/progen/SKILL.md) | P | Historical directory: 2/2 local blobs identical to skills/progen at pinned upstream snapshot below |
| [project-foundation](../engineering/project-foundation/SKILL.md) | O | Maintainer-authored (explicit confirmation); local delegation changes |
| [product-prototyping](../engineering/product-prototyping/SKILL.md) | T | Historical directory: 4/4 local blobs identical to skills/engineering/prototype at pinned upstream snapshot below |
| [agent-qa-browser-regression](../spring-boot-skills/agent-qa-browser-regression/SKILL.md) | S | Historical resource matches 3/4; upstream: agent-qa-browser-regression |
| [archunit](../spring-boot-skills/archunit/SKILL.md) | S | Historical resource matches 0/2; upstream: unresolved |
| [bootui](../spring-boot-skills/bootui/SKILL.md) | S | Historical resource matches 0/3; upstream: unresolved |
| [event-driven-messaging](../spring-boot-skills/event-driven-messaging/SKILL.md) | S | Historical resource matches 3/5; upstream: event-driven-messaging |
| [feature-architecture](../spring-boot-skills/feature-architecture/SKILL.md) | S | Historical resource matches 0/2; upstream: unresolved |
| [flyway-migrations](../spring-boot-skills/flyway-migrations/SKILL.md) | S | Historical resource matches 3/7; upstream: flyway-migrations |
| [hexagonal-architecture](../spring-boot-skills/hexagonal-architecture/SKILL.md) | S | Historical resource matches 4/6; upstream: hexagonal-architecture |
| [http-interface-clients](../spring-boot-skills/http-interface-clients/SKILL.md) | S | Historical resource matches 3/5; upstream: http-interface-clients |
| [idempotency-patterns](../spring-boot-skills/idempotency-patterns/SKILL.md) | S | Historical resource matches 3/4; upstream: idempotency-patterns |
| [jvm-diagnostics](../spring-boot-skills/jvm-diagnostics/SKILL.md) | S | Historical resource matches 4/5; upstream: jvm-diagnostics |
| [layered-architecture](../spring-boot-skills/layered-architecture/SKILL.md) | S | Historical resource matches 5/7; upstream: layered-architecture |
| [mise](../spring-boot-skills/mise/SKILL.md) | S | Historical resource matches 0/3; upstream: unresolved |
| [multi-module-maven](../spring-boot-skills/multi-module-maven/SKILL.md) | S | Historical resource matches 0/5; upstream: multi-module-maven (name only) |
| [multi-tenancy](../spring-boot-skills/multi-tenancy/SKILL.md) | S | Historical resource matches 8/10; upstream: multi-tenancy |
| [null-safety](../spring-boot-skills/null-safety/SKILL.md) | S | Historical resource matches 2/4; upstream: null-safety |
| [postgresql](../spring-boot-skills/postgresql/SKILL.md) | S | Historical resource matches 0/3; upstream: unresolved |
| [resilience-retry](../spring-boot-skills/resilience-retry/SKILL.md) | S | Historical resource matches 3/5; upstream: resilience-retry |
| [spring-ai](../spring-boot-skills/spring-ai/SKILL.md) | S | Historical resource matches 0/11; upstream: unresolved |
| [spring-boot-api-versioning](../spring-boot-skills/spring-boot-api-versioning/SKILL.md) | S | Historical resource matches 2/4; upstream: api-versioning |
| [spring-boot-cicd](../spring-boot-skills/spring-boot-cicd/SKILL.md) | S | Historical resource matches 0/4; upstream: unresolved |
| [spring-boot-configuration](../spring-boot-skills/spring-boot-configuration/SKILL.md) | S | Historical resource matches 3/5; upstream: configuration-properties |
| [spring-boot-docker](../spring-boot-skills/spring-boot-docker/SKILL.md) | S | Historical resource matches 1/14; upstream: container-native-deployment |
| [spring-boot-email](../spring-boot-skills/spring-boot-email/SKILL.md) | S | Historical resource matches 0/3; upstream: unresolved |
| [spring-boot-frontend-integration](../spring-boot-skills/spring-boot-frontend-integration/SKILL.md) | S | Historical resource matches 0/5; upstream: unresolved |
| [spring-boot-gradle](../spring-boot-skills/spring-boot-gradle/SKILL.md) | S | Historical resource matches 0/4; upstream: unresolved |
| [spring-boot-kubernetes](../spring-boot-skills/spring-boot-kubernetes/SKILL.md) | S | Historical resource matches 0/3; upstream: unresolved |
| [spring-boot-maven](../spring-boot-skills/spring-boot-maven/SKILL.md) | S | Historical resource matches 0/4; upstream: unresolved |
| [spring-boot-mybatis-plus](../spring-boot-skills/spring-boot-mybatis-plus/SKILL.md) | S | Historical resource matches 0/4; upstream: unresolved |
| [spring-boot-observability](../spring-boot-skills/spring-boot-observability/SKILL.md) | S | Historical resource matches 3/5; upstream: production-observability |
| [spring-boot-openapi](../spring-boot-skills/spring-boot-openapi/SKILL.md) | S | Historical resource matches 1/7; upstream: openapi-first |
| [spring-boot-scaffolding](../spring-boot-skills/spring-boot-scaffolding/SKILL.md) | S | Historical resource matches 0/5; upstream: spring-boot-scaffolding (name only) |
| [spring-boot-security](../spring-boot-skills/spring-boot-security/SKILL.md) | S | Historical resource matches 5/21; upstream: security-audit-logging; oauth2-resource-server; spring-security-jwt |
| [spring-boot-testing](../spring-boot-skills/spring-boot-testing/SKILL.md) | S | Historical resource matches 1/5; upstream: testing-pyramid |
| [spring-boot-webmvc](../spring-boot-skills/spring-boot-webmvc/SKILL.md) | S | Historical resource matches 8/16; upstream: problem-details-rfc9457; rest-api-conventions; transactional-patterns |
| [spring-boot](../spring-boot-skills/spring-boot/SKILL.md) | S | Historical resource matches 0/2; upstream: unresolved |
| [spring-data-jpa](../spring-boot-skills/spring-data-jpa/SKILL.md) | S | Historical resource matches 3/5; upstream: spring-data-jpa |
| [spring-data-redis](../spring-boot-skills/spring-data-redis/SKILL.md) | S | Historical resource matches 5/8; upstream: spring-data-redis |
| [spring-modulith](../spring-boot-skills/spring-modulith/SKILL.md) | S | Historical resource matches 3/4; upstream: spring-modulith |
| [spring-task-execution](../spring-boot-skills/spring-task-execution/SKILL.md) | S | Historical resource matches 4/11; upstream: scheduled-tasks; async-execution |

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
| M | [6852f2c](https://github.com/sivaprasadreddy/musings/tree/6852f2c8b60196279cdfafe6c8809f7ecf6eeb69) | No LICENSE/NOTICE/COPYING file found in recursive tree; maintainer confirms MIT for the three SDD skills, but the upstream grant and exact notice remain unverified |
| A | [24479d5](https://github.com/anthropics/claude-plugins-community/tree/24479d5a1fb7a1c4f068a6d3b74d0c0fbc1acfda) | Both plugin manifests declare MIT and name Thariq Shihipar; root Apache-2.0; confirm applicable notice text |
| E | [d173a75](https://github.com/eze-is/web-access/tree/d173a75978ceb2c63fa0b27f7408cd2bf7fe4310) | SKILL.md declares MIT; no LICENSE/NOTICE/COPYING file found in recursive tree |

The matching 18 historical directories cover 68 local files (including SKILL.md and supporting resources). The java-code-review match is counted once. No claim is made that current modified directories are identical. The maintainer explicitly confirmed authorship of product-brainstorming and project-foundation on 2026-11-28. Both are classified as original repository skills; the source-origin blocker for these two entries is removed. The repository root MIT license remains the documented default; no separate license grant is inferred from the authorship statement.

## Release blockers

- create-spec, impl-spec, prd-to-requirements: origin confirmed by the maintainer on 2026-11-28 as Siva; exact historical content match corroborates the source. Maintainer confirmed MIT licensing on 2026-11-28. No explicit license file was found in the pinned musings tree. Record MIT as maintainer-confirmed, not independently verified; locate the author’s license declaration or permission and preserve the applicable notice before closing the publication review.
- eli5, html-plan: plugin manifests explicitly declare MIT; individual SOURCE.md records author, pinned manifest and root Apache-2.0 evidence. Confirm complete applicable copyright/permission notices before publication.
- web-access: pinned README, plugin manifest and SKILL.md all declare MIT; individual SOURCE.md preserves attribution and evidence. Complete MIT notice and any additional script/resource terms remain to be confirmed.
- Spring Boot collection: distinguish verified resource matches from local additions; confirm all remaining files' provenance.
- Ensure source and distributed archives preserve applicable notices, including files copied by individual installation and extraction. No publication while blockers remain.

## Spring Boot historical resource comparison

Compared all 230 files under the 39 skill directories at local [aa2459c](https://github.com/zhijunio/skills/tree/aa2459c28bc8bdbc7d37a3f5d8bce09070e44a7d) against all blobs under upstream skills/spring-boot-4 at b90c3db. 80 local files have an exact blob match anywhere under that upstream collection. 23 skill directories have at least one match; 16 have none. The per-skill table above records candidate upstream directories from the matching blobs, or name-only matches explicitly marked as such. None of these counts establishes authorship of unmatched files, including rewritten entry points. The local Spring Boot LICENSE has the exact same blob as the upstream root LICENSE at the pinned revision. Preserve it and investigate remaining files.

## Notice preservation batch

Copied the exact MIT LICENSE text from the pinned upstream snapshots into the individual skill directories for the nine Matt Pocock skills (diagnosing-bugs, domain-modeling, grill-with-docs, grilling, prototype, handoff, retro, teach, writing-for-agents), HumanLayer show-me, and SivaLabs progen and java-code-review. These are 12 notices with independently located upstream license text. The maintainer-confirmed MIT classification of the three musings SDD skills is recorded separately; their exact upstream notice has not yet been located, so no substitute copyright statement was fabricated. The existing Spring Boot collection notice remains in place. New individual notice files must be covered by the next package and installation verification run.

### Second notice preservation batch

Added individual SOURCE.md evidence files for eli5, html-plan and web-access. The pinned upstream web-access README, plugin manifest and SKILL.md each explicitly declare MIT; no standalone LICENSE was found. For eli5 and html-plan, the specific plugin manifests declare MIT while the repository root LICENSE is Apache-2.0. No substitute copyright notice was fabricated.

Copied the existing, byte-identical upstream Spring Boot MIT notice into all 39 Spring Boot skill directories ([c819a31](https://github.com/zhijunio/skills/commit/c819a312f49ee88ba10e901528078a9744d52369)) so it can travel with individually copied or extracted skills. This preserves the collection's upstream attribution for any incorporated upstream material; it does not assert that Rrezart authored every local addition or that all unmatched content is cleared. Keep the collection-level notice as well. New files must pass archive, extraction and installation checks before closure. No publication authorized.

## SDD name migration

Current names preserve the provenance of their historical predecessors: product-brainstorming → product-requirements (O); prd-to-requirements → prd-to-use-cases (M); create-spec → behavior-specification (M); impl-spec → implement-use-case (M). Historical source paths and licensing evidence above remain unchanged. The outstanding SDD notice blocker remains open.

The former prototype skill is now product-prototyping. Its existing Matt Pocock MIT notice is retained byte-for-byte; the historical upstream directory name remains unchanged. Product journey, isolation and review guidance are local adaptations; no new third-party source was imported.
