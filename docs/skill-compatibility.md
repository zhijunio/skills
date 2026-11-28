# Skill Standards and Compatibility

Status: local distribution and static policy verified; native runtime verification pending  
Reviewed: 2026-11-28  
Plan: [Phase 01](plans/phase-01-repository-foundations.md)

## 1. Scope and decisions

Use the open Agent Skills specification as the portable baseline. Target Codex and Claude Code first; other agents remain candidates until verified. Prefer existing authoring, discovery, validation, and packaging tools. These are working recommendations for subsequent audit and implementation, not a claim of tested support.

## 2. Portable format

A skill is a directory with a YAML-frontmatter Markdown entry point named SKILL.md. Supporting scripts, references, and assets are optional.

| Field | Required | Constraint |
| --- | --- | --- |
| name | Yes | 1–64 characters; lowercase letters, digits, hyphens; no leading, trailing, or consecutive hyphens; matches directory |
| description | Yes | 1–1024 characters; states capability and trigger conditions |
| license | No | License identifier or bundled license reference |
| compatibility | No | 1–500 characters when present; environment requirements |
| metadata | No | String-to-string map |
| allowed-tools | No | Space-separated tool declarations; experimental |

Use concise entry instructions, load references on demand, and run scripts only when necessary. Keep relative paths resolvable within the installed skill or document required sibling skills. Do not assume a host interprets experimental tool declarations as a portable security boundary.

Example:

```yaml
---
name: example-review
description: Review a proposed change against its acceptance criteria and report verification gaps.
---
```

Source: [Agent Skills specification](https://agentskills.io/specification).

## 3. Compatibility matrix

The following reflects documentation reviewed on the date above, not execution against installed agent versions.

| Feature | Open standard | Codex | Claude Code |
| --- | --- | --- | --- |
| SKILL.md with name and description | Required | Documented | Documented |
| Supporting resources | Supported | Documented | Documented |
| On-demand activation | Design model | Documented | Documented |
| Explicit invocation | Client-defined | Dollar-prefixed skill name | Slash-prefixed skill name |
| Project discovery | Client-defined | .agents/skills | .claude/skills |
| User discovery | Client-defined | ~/.agents/skills | ~/.claude/skills |
| Manual-only activation | Not standardized | agents/openai.yaml policy.allow_implicit_invocation | disable-model-invocation frontmatter |
| allowed-tools | Experimental | Do not assume equivalent behavior | Supported in local skills, subject to host permissions |
| Dynamic context and subagent extensions | Not standardized | No equivalence assumed | Host-specific features |

Sources: [Codex skill documentation](https://developers.openai.com/codex/skills), [Claude Code skills](https://code.claude.com/docs/en/skills).

Codex documentation now describes .agents/skills as its repository and user discovery root. Older .codex/skills references should not be treated as the portable default. Verify actual installed versions during F09. Claude Code's account uploads and Skills API have stricter frontmatter requirements than its local skills; local compatibility does not imply upload compatibility.

## 4. Invocation policy and existing repository risks

The reviewed definitions for engineering/behavior-specification, engineering/implement-use-case, and engineering/project-foundation use disable-model-invocation: true. This is a Claude Code extension. Their approval and scope boundaries must not depend solely on host metadata.

For Codex, the documented counterpart is a skill-local agents/openai.yaml:

```yaml
policy:
  allow_implicit_invocation: false
```

For Claude Code local skills, the corresponding frontmatter is:

```yaml
disable-model-invocation: true
```

Do not automatically add, remove, or translate these fields across all skills. F06 must inventory extensions and intended invocation policies; F09 must test explicit and implicit activation. F03/F04 should establish how portable exports preserve necessary approval controls. If a strict consumer rejects extensions, either produce a validated target-specific export or explicitly limit support. Never silently strip a control that changes behavior.

The Spring Boot entry skill routes to sibling skill directories. Single-skill installation may therefore leave references unavailable. Audit cross-skill dependencies and test both collection and individual installation. Avoid duplicating reference knowledge to hide this issue.

## 5. Reuse and tooling decisions

| Capability | Candidate | Decision for this phase |
| --- | --- | --- |
| Format standard | [Agent Skills](https://github.com/agentskills/agentskills) | Adopt |
| Reference validation | [skills-ref](https://github.com/agentskills/agentskills/tree/main/skills-ref) | Trial before CI enforcement |
| Discovery and installation | [Vercel Skills CLI](https://github.com/vercel-labs/skills) | Reuse; verify nested discovery |
| Authoring and evaluation | [Anthropic skill-creator](https://github.com/anthropics/skills/tree/main/skills/skill-creator) | Reference/reuse rather than duplicate |
| Codex authoring | Bundled skill-creator and skill-installer, [OpenAI Skills](https://github.com/openai/skills) | Prefer host-provided tools |
| Repository instructions | [AGENTS.md](https://github.com/agentsmd/agents.md) and existing repository prompt | Adopt format; adapt local guidance |
| Maven distribution | [SkillsJars Maven Plugin](https://github.com/skillsjars/skillsjars-maven-plugin) | Retain pending verification |
| Markdown and links | [markdownlint-cli2](https://github.com/DavidAnson/markdownlint-cli2), [lychee](https://github.com/lycheeverse/lychee) | Candidates for F10; verify configuration and licensing |

No third-party code is imported by this document. Pin versions or revisions, review applicable licenses and dependency integrity, and record results before installing or vendoring tools.

### Reference validator

Upstream describes skills-ref as a demonstration library, not a production implementation. Use it as a conformance reference and trial tool, not an unconditional enforcement dependency. Its documented commands include:

```sh
skills-ref validate engineering/behavior-specification
skills-ref read-properties engineering/behavior-specification
```

Test extension handling and validator limitations before enforcing CI. Separate portable-schema findings from host-specific findings. Reuse existing parsing libraries and add minimal repository checks only where needed. Never treat a successful format check as proof of behavior.

### Discovery and distribution

The Skills CLI documents source listing with the add --list option. Candidate smoke check:

```sh
npx skills add zhijunio/skills --list
```

Use an explicitly selected CLI version for reproducible verification. Compare discovered names and paths against the recursive source inventory; check nested directory discovery and skill dependencies.

The current pom.xml declares SkillsJars Maven plugin 0.0.7, skillsDir at the repository root, and Java release 17. The actual inclusion of nested skills is not established by configuration alone. Current upstream plugin documentation may describe a different version.

F09 should build the current repository, inspect the generated JAR, and test extraction with the configured version before considering upgrades.

## 6. Audit rules for F06

Record each skill's path, name, description, standard fields, extensions, intended activation, scope, dependencies, references, provenance, license, and verification status. Classify findings separately:

- Standard violations: malformed frontmatter, invalid names, missing descriptions, invalid field types.
- Host extensions: documented, unsupported, or unknown behavior on target agents.
- Repository defects: duplicate names, unresolved links, missing dependencies, stale documentation.
- Distribution risks: incomplete discovery or loss of dependent resources.
- Behavioral risks: ambiguous triggers, missing approvals, contradictory workflows, unverified technical advice.

A host extension is not automatically a defect. Do not modify skills solely to satisfy a validator without preserving intended behavior.

## 7. Verification plan

| Check | Stage | State |
| --- | --- | --- |
| Standard and host documentation review | F01 | Reviewed |
| Representative local metadata inspection | F01 | Reviewed |
| Full inventory and extension audit | F06 | Static checks passed for current 63 entries |
| Validator execution and invalid fixtures | F06/F10 | CI passed |
| Native agent discovery, explicit and implicit invocation | Q08 | NOT RUN |
| Nested CLI discovery and dependency closure | F09 | Full installation and selected dependency group passed; other subsets unverified |
| Maven build, JAR inventory, extraction | F09 | CI passed |
| CI integration and pinned tooling | F10 | Implemented |
| Systematic model-based evaluation | Phase 3 | Pending |

Record exact commands, versions, environment, exit status, and observable evidence. Distinguish documented behavior, static inspection, and runtime results. Do not publish artifacts or modify global agent configuration as part of smoke testing.

## 8. Open decisions

- Whether Codex and Claude Code local skills are the initial officially supported targets.
- Whether strict portable exports are needed for hosted skill uploads.
- How to represent and distribute required sibling skills.
- Which validator revision and extension policy should be enforced.
- Whether current Maven plugin coverage meets distribution requirements.
- Whether additional agents should enter the support matrix.

## 9. Source register

Documentation accessed 2026-11-28; these are living sources, not pinned dependency versions.

- [Agent Skills specification](https://agentskills.io/specification)
- [skills-ref README](https://github.com/agentskills/agentskills/tree/main/skills-ref)
- [Codex skills](https://developers.openai.com/codex/skills)
- [OpenAI skill evaluation guidance](https://developers.openai.com/blog/eval-skills)
- [Claude Code skills](https://code.claude.com/docs/en/skills)
- [Skills CLI commands](https://github.com/vercel-labs/skills/blob/main/docs/commands.md)
- [Skills CLI authoring and discovery](https://github.com/vercel-labs/skills/blob/main/docs/creating-skills.md)
- [SkillsJars Maven plugin](https://github.com/skillsjars/skillsjars-maven-plugin)

Repository evidence: [pom.xml](../pom.xml), [behavior-specification](../engineering/behavior-specification/SKILL.md), [implement-use-case](../engineering/implement-use-case/SKILL.md), [project-foundation](../engineering/project-foundation/SKILL.md), and [spring-boot](../spring-boot-skills/spring-boot/SKILL.md). Representative definitions were reviewed; a complete audit remains F06 work.

## 10. Q08 follow-up

See [cross-host verification](verification/cross-host-verification.md) for current documentation review, native pilot cases and evidence boundaries. CI passed at 28a7616; native runtime remains NOT RUN. Hosted exports remain separate and unverified.
