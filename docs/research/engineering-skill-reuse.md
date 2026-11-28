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


## Prototype capability research (expanded E01 scope)

Research date: 2026-11-28. Existing prototype entry point, UI.md and LOGIC.md were inspected. The UI mode compares alternative renderings; the logic mode explores state transitions. Neither provides a complete technical feasibility workflow. See the [prototype contract](../contracts/prototyping.md).

| Candidate and pinned source | Repository stars | License evidence | Fit and constraints |
| --- | ---: | --- | --- |
| [Matt Pocock prototype](https://github.com/mattpocock/skills/tree/bbadaa631c37af594e5a05c9cefacd9956136b18/skills/engineering/prototype) | Earlier E01 snapshot | Existing pinned MIT provenance | Retain local UI and logic exploration techniques; correct blanket no-test guidance, route isolation and automatic promotion/capture assumptions |
| [magnus919 product-design-and-ux](https://github.com/magnus919/agent-skills/blob/0f83b91d44d692a665b6f487c4f60fac07e632cb/product-design-and-ux/SKILL.md) | 197 | MIT root notice inspected earlier at same revision; entry point MIT | Strong information architecture, task/state modeling, recovery and usability planning; many sibling references and assumes approved scope; adapt to bounded draft exploration, do not import entire suite |
| [mblode product-design](https://github.com/mblode/agent-skills/blob/56bf97e8f6078bfada4c91c23fcf7c14b6e84a07/skills/product-design/SKILL.md) | 226 | Repository metadata MIT; exact notice pending | Useful action semantics, reversibility, recovery and reachable states; references rule registry and sibling implementation skills |
| [Hermes spike](https://github.com/NousResearch/hermes-agent/blob/29bbd2d97b8e5bcda44d7e02ee2239a93afd5e58/skills/software-development/spike/SKILL.md) | 266648 | Repository and entry point MIT declarations; entry point attributes adaptation from gsd-build/get-shit-done; complete applicable notices and upstream origin review pending | Strong standalone experiment loop, alternatives, observed verdict; remove Hermes tool assumptions, automatic delegation and blanket hardcoding/disposal defaults |
| [tomzx validate-assumptions](https://github.com/tomzx/agents/blob/c21886950f9287841ac4c46dee793f3437f88cc3/skills/validate-assumptions/SKILL.md) | 18 | Repository metadata MIT; exact notice pending | Risk inventory, threshold before run, downstream impact; tightly coupled to .sdlc artifacts and review commands |
| [Anthropic frontend-design](https://github.com/anthropics/skills/blob/e4bb5d960d87126ff3b3a8af1a104b7db67560ba/skills/frontend-design/SKILL.md) | 201666 | Entry point refers to skill-specific LICENSE.txt; do not assume repository-wide MIT | Visual direction and frontend implementation, not product authority or full prototype lifecycle |

Repository counts are observations, not skill popularity or quality scores. Selected entry-point sections were inspected at pinned revisions; complete dependency and resource audits and behavioral tests have not been performed. No upstream material was imported.

### Proposed prototype disposition

Prefer two independent responsibilities over three new skills: adapt the existing prototype into a product/behavior exploration capability with progressive references, and develop a separate technical spike capability for empirical engineering questions. Candidate names: product-prototyping and technical-spike. Product/behavior exploration shares stakeholder feedback, synthetic state and reviewable artifacts; technical spikes need explicit hypotheses, isolated real dependencies where authorized, measurements, reproducibility and distinct verdicts. Keep the existing UI/logic techniques where they add value; product design decisions remain with PRD and behavior owners, technical decisions with system design/ADR. HTML planning remains a separate presentation capability. No rename or split has occurred.

Before copying any candidate, inspect complete applicable licenses, dependencies and references; preserve notices and document transformations. Independently authored experiment and feedback contracts can be implemented without importing an entire upstream workflow. E03-A/B and E04-A should exercise the scenarios in the prototype contract, including unavailable environments, production isolation and separate implementation authorization.


## Prototype capability extension

Research snapshot: 2026-11-28. Existing local prototype entry point and both UI/LOGIC references were inspected. Additional candidates:

| Candidate | Fixed revision | License evidence | Fit |
| --- | --- | --- | --- |
| [Hermes spike](https://github.com/NousResearch/hermes-agent/blob/29bbd2d97b8e5bcda44d7e02ee2239a93afd5e58/skills/software-development/spike/SKILL.md) | 29bbd2d97b8e5bcda44d7e02ee2239a93afd5e58 | Repository metadata and skill declare MIT; entry point attributes GSD | Technical feasibility experiments and comparison verdicts |
| [Product Manager Skills probe advisor](https://github.com/deanpeters/Product-Manager-Skills/blob/476ae12917b8e77609eb956bcc2203a4e4ec857d/skills/pol-probe-advisor/SKILL.md) | 476ae12917b8e77609eb956bcc2203a4e4ec857d | Repository metadata NOASSERTION; redistribution rights not established | Match experiment to hypothesis, not preferred tool |
| [UX designer](https://github.com/rahulgupta2018/agent-skills/blob/0154922bf5a445de71cd5e2f775de90284b0db15/skills/ux-designer/SKILL.md) | 0154922bf5a445de71cd5e2f775de90284b0db15 | Entry point declares MIT and adaptation from awesome-llm-apps; root notice not verified | Journey, IA, interface state and usability review |
| [robjsliwa skills](https://github.com/robjsliwa/skills/tree/fabfbca7f6017fa51f00b24ccfebbf6a2f33cda1) | fabfbca7f6017fa51f00b24ccfebbf6a2f33cda1 | Repository metadata MIT; full notice not inspected | README describes LOGIC/UI/FLOW prototypes and separate spike; implementation not yet reviewed |

Observed repository stars: 266648, 8179, 0, 0 respectively. These are repository-wide snapshots, not skill-level evaluation results. Inspected selected entry-point sections for the first three and README for the last; not a full source or dependency audit.

### Decision

Prefer adapting the existing prototype skill into a focused product/behavior exploration owner with separate progressively loaded UI and logic references. Extend the UI reference to cover multi-screen journeys, navigation and reviewable states where appropriate, without importing a full UX methodology or requiring frontend production work. Preserve the existing logic simulator as a behavior exploration mode. Do not create a separate behavior skill unless actual usage or context measurements establish a need.

Prefer a separate technical-spike capability for evidence-producing engineering experiments: its activation, measurable results, dependency setup, repeatability and failure handling differ from interactive product/behavior review. Hermes spike is a promising adaptation reference, not an approved source import. Its host-specific tool calls, default decomposition count, sample numeric thresholds, delegation, and hardcoding advice must not become universal requirements. Preserve authorized isolated execution, explicit evidence, dependency compatibility and no secrets or production mutations. Research alone is sufficient where experimentation would add no evidence.

This two-owner approach is a proposed implementation direction. Keep existing prototype name until migration analysis; product-prototyping and technical-spike are candidate names. Evaluate the need for dedicated UX design separately from prototype experiments. No third-party material imported. Before copying, inspect complete source, resources, root notices and GSD attribution chain. Unclear-license UX and product sources are research-only. Existing local prototype retains its recorded Matt Pocock provenance. See [prototype contract](../contracts/prototyping.md) for acceptance cases P01–P07; all NOT RUN.
