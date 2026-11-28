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
