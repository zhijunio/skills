# Engineering Skill Reuse Research (E01)

Status: source inspection and provisional reuse decisions, 2026-11-28. No import or runtime execution.
Related: [capability audit](../audits/engineering-capability-audit.md), [Phase 02 plan](../plans/phase-02-engineering-workflow.md).

## Pinned candidates

Repository API observations at research time; stars are approximate and not selection criteria. Root MIT declarations are not a substitute for reviewing individual files and dependencies before importing.

| Repository | Pinned revision | License evidence | Observed commit date | Stars |
| --- | --- | --- | --- | ---: |
| [Superpowers](https://github.com/obra/superpowers) | [22f0b46](https://github.com/obra/superpowers/tree/22f0b463e7a5e579904dd6211a520321035526f7) | [MIT, Jesse Vincent](https://github.com/obra/superpowers/blob/22f0b463e7a5e579904dd6211a520321035526f7/LICENSE) | 2026-11-03 | 328576 |
| [Spec Kit](https://github.com/github/spec-kit) | [f201054](https://github.com/github/spec-kit/tree/f2010547121b3351e4923fe467205079e4593a32) | [MIT, GitHub Inc.](https://github.com/github/spec-kit/blob/f2010547121b3351e4923fe467205079e4593a32/LICENSE) | 2026-11-25 | 152440 |
| [Matt Pocock skills](https://github.com/mattpocock/skills) | [bbadaa6](https://github.com/mattpocock/skills/tree/bbadaa631c37af594e5a05c9cefacd9956136b18) | [MIT](https://github.com/mattpocock/skills/blob/bbadaa631c37af594e5a05c9cefacd9956136b18/LICENSE) | 2026-11-27 | 334446 |
| [SivaLabs skills](https://github.com/sivaprasadreddy/sivalabs-agent-skills) | [6a3a8f2](https://github.com/sivaprasadreddy/sivalabs-agent-skills/tree/6a3a8f2043373a1b8d6c3051bc28aa7a91731f6a) | [MIT](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/6a3a8f2043373a1b8d6c3051bc28aa7a91731f6a/LICENSE) | 2026-11-27 | 196 |

Recursive inventories inspected. Selected instruction sections inspected; not a complete dependency or security audit. SivaLabs inventory contains specialized Java review, Progen and Spring Boot guidance rather than an obvious generic P0 planning skill. Existing local SDD skills originate from Siva's musings, not necessarily this repository; retain the separate provenance evidence.

## Candidate comparison

| Source | Observed capability | Fit / limitations |
| --- | --- | --- |
| [Superpowers brainstorming](https://github.com/obra/superpowers/blob/22f0b463e7a5e579904dd6211a520321035526f7/skills/brainstorming/SKILL.md) | Approaches, design approval, planning handoff | Broad activation overlaps product discovery, feature specification and grilling; mandatory named handoff is unsuitable for independent use |
| [Superpowers writing-plans](https://github.com/obra/superpowers/blob/22f0b463e7a5e579904dd6211a520321035526f7/skills/writing-plans/SKILL.md) | File responsibility, small testable tasks, spec constraints and review focus | Strong planning candidate; default paths, commit cadence and required executor skills need adaptation |
| [Superpowers TDD](https://github.com/obra/superpowers/blob/22f0b463e7a5e579904dd6211a520321035526f7/skills/test-driven-development/SKILL.md) | Test-first red/green/refactor and verification | Execution method, not full risk strategy; do not import destructive deletion rule or universal mandatory activation |
| [Spec Kit clarify](https://github.com/github/spec-kit/blob/f2010547121b3351e4923fe467205079e4593a32/templates/commands/clarify.md) | Structured ambiguity and coverage scan | Feature-level rather than product lifecycle; script, feature directory and hooks dependencies |
| [Spec Kit plan](https://github.com/github/spec-kit/blob/f2010547121b3351e4923fe467205079e4593a32/templates/commands/plan.md) | Technical research, data model and interface artifacts | Useful design checklist; fixed project conventions, scripts, constitution and hooks do not match local contracts |
| [Spec Kit tasks](https://github.com/github/spec-kit/blob/f2010547121b3351e4923fe467205079e4593a32/templates/commands/tasks.md) | Dependency-ordered story tasks and coverage | Useful decomposition; not standalone Agent Skills entry point |
| [Spec Kit analyze](https://github.com/github/spec-kit/blob/f2010547121b3351e4923fe467205079e4593a32/templates/commands/analyze.md) | Read-only cross-artifact consistency | Adapt checks to local artifacts; mandatory hook execution requires separate review |
| [Matt codebase-design](https://github.com/mattpocock/skills/blob/bbadaa631c37af594e5a05c9cefacd9956136b18/skills/engineering/codebase-design/SKILL.md) | Module depth, interface and test seams | Useful component-level design; not full system architecture; avoid overriding established domain vocabulary |
| [Matt to-spec](https://github.com/mattpocock/skills/blob/bbadaa631c37af594e5a05c9cefacd9956136b18/skills/engineering/to-spec/SKILL.md) | Synthesize discussed behavior into spec | No interview; publishes to configured tracker, overlaps local create-spec; no automatic publication |
| [Matt to-tickets](https://github.com/mattpocock/skills/blob/bbadaa631c37af594e5a05c9cefacd9956136b18/skills/engineering/to-tickets/SKILL.md) | Vertical slices, blocking edges, expand-contract exception | Strong planning reference; tracker setup and ticket publication require adaptation |
| [Matt tdd](https://github.com/mattpocock/skills/blob/bbadaa631c37af594e5a05c9cefacd9956136b18/skills/engineering/tdd/SKILL.md) | Behavior tests, public seams, incremental test-first work | Useful implementation technique; depends on references and codebase-design, not a general risk strategy |

## Provisional reuse decisions

| Capability | Decision | Rationale and remaining research |
| --- | --- | --- |
| Product requirements | Adapt existing product-brainstorming | Existing PRD owner is adequate for initial scope; clarify lifecycle first, use ambiguity taxonomy selectively |
| SDD consistency | Adapt existing contracts | Spec Kit analyze offers a useful read-only approach; no parallel index or constitution |
| System design | Compose / Adapt candidate | Use current domain/ADR and foundation owners plus design references; compare dedicated system design alternatives before deciding whether to create a skill |
| Implementation planning | Adapt candidate | Combine task/test decomposition from writing-plans with vertical slices and dependency edges from to-tickets; preserve optional HTML review |
| TDD | Adapt / Compose candidate | Matt TDD and Superpowers offer techniques; preserve existing work, use explicit or appropriate activation |
| Test strategy | Research open | Neither TDD source is comprehensive risk-based strategy; compare dedicated strategy sources, stack-specific guidance and acceptance evidence before deciding |
| Debugging/review | Retain existing baseline | No demonstrated need for wholesale replacement in P0 scope |

Do not import either entire workflow framework. Record exact adopted files, modifications, upstream copyright and MIT notice if actual material is reused. Independently authored summaries of general engineering concepts are not a claim that upstream content has been imported. Check all supporting resources and host assumptions before adaptation. No script or hook was executed, and no runtime quality or compatibility claims are made.

## Next actions

E02 resolves PRD approval, repair and status semantics. E04 and E06 require deeper candidate research for whole-system design and risk-based verification; E05 should examine complete planning instructions and supporting resources before implementation. Official skill-creator remains the existing authoring/evaluation reference, not an engineering workflow substitute.

## Dedicated system design and test strategy candidates

Additional source inspection on 2026-11-28:

| Candidate | Revision | License evidence | Decision |
| --- | --- | --- | --- |
| [software-architecture](https://github.com/magnus919/agent-skills/tree/0f83b91d44d692a665b6f487c4f60fac07e632cb/software-architecture) | 0f83b91d44d692a665b6f487c4f60fac07e632cb | [MIT notice, Magnus Hedemark, 2026](https://github.com/magnus919/agent-skills/blob/0f83b91d44d692a665b6f487c4f60fac07e632cb/LICENSE.md) | Adapt selectively |
| [test-strategy](https://github.com/l-gevity/l-gevity-skills/tree/5d5cbc523c835a6e2fb2795a914c9fa27c7e3c64/.agents/skills/test-strategy) | 5d5cbc523c835a6e2fb2795a914c9fa27c7e3c64 | [MIT notice, L-GEVITY, 2026](https://github.com/l-gevity/l-gevity-skills/blob/5d5cbc523c835a6e2fb2795a914c9fa27c7e3c64/LICENSE) | Adapt selectively |
| [architecture-design](https://github.com/sauljabin/agentskills/tree/299fd1f1ad57450b09536b26496367ccedf15403/skills/architecture-design) | 299fd1f1ad57450b09536b26496367ccedf15403 | Repository metadata and entry point declare CC-BY-4.0; full notice not examined | Do not adopt HTML-first workflow |
| [test-strategy-skill](https://github.com/jovd83/test-strategy-skill/tree/35f44773e790c6a25b07991a81da8260129f4221) | 35f44773e790c6a25b07991a81da8260129f4221 | Repository metadata declares MIT; full notice not examined | Do not adopt lifecycle-specific scripts and thresholds |

The two preferred candidates have complete root MIT notices inspected at their pinned revisions. The architecture tree contains the entry point, README, eight references, three templates and evaluation cases. Its source index expressly excludes reproduction of purchased books and identifies external specialist and DDIA-reference dependencies. Do not import those book-derived dependencies or presume rights to them. The design brief template and source index were inspected; remaining reference contents require review before copying. The test-strategy tree contains an entry point and two references, both examined for technique and portfolio guidance. It assumes the upstream Alchemy lifecycle and named neighboring skills; remove those assumptions in an adaptation rather than importing an orchestrator.

The selected design approach should cover drivers, quality scenarios, boundaries, data authority, runtime/failure behavior and tradeoffs with optional evidence and evolution planning. The selected test strategy should map requirements and risks to observable failure detection, technique, fidelity, dependency realism, data/environment and residual risk. Do not equate TDD with test strategy. Avoid fixed coverage or pass-rate percentages that lack a project requirement.

Observed repository metadata: architecture candidate 197 stars, pinned commit 2026-11-27; test strategy candidate 93 stars, pinned commit 2026-11-22; HTML architecture candidate 1 star, pinned commit 2026-11-06; lifecycle test strategy candidate 0 stars, pinned commit 2026-09-28. These are snapshot observations, not verified adoption or quality indicators.

## E01 decision summary

Proceed to E02 with existing SDD contract adaptation; assess renames after semantics are agreed. E03 should adapt the existing product owner rather than introduce competing PRD ownership. E04 system-design, E05 implementation-planning and E06 test-strategy are justified candidate additions subject to focused source/dependency review and proportional scope. Preserve HTML planning as an optional presentation capability only if useful; assess TDD separately as an implementation technique. No imports or runtime executions have occurred. Full dependency and notice review is a prerequisite of each actual adaptation, not a completed certification.
