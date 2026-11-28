# Contributing

Contributions should improve the reliability, clarity, maintainability or coverage of reusable software engineering skills. Review [AGENTS.md](AGENTS.md), the [design principles](docs/skill-design-principles.md), and the [authoring guide](docs/skill-authoring-guide.md) before changing skills.

## Choose a contribution

| Contribution | Expected evidence |
| --- | --- |
| Bug fix | Reproduction, observed defect, focused correction and regression check |
| Documentation | Accurate source references and working links |
| Existing skill improvement | Concrete behavior gap, relevant before/after cases, and scope review |
| New or imported skill | Capability gap, reuse decision, provenance, license and representative cases |
| Build or tooling change | Reproducible commands, failure-path tests and distribution impact |

For substantial additions, architecture changes, skill renames or removals, discuss the proposal in a [GitHub issue](https://github.com/zhijunio/skills/issues) before implementation. Small corrections do not require a separate issue. Do not add a skill solely to increase the collection size.

## Set up a local checkout

Fork the repository if you do not have write access, clone your checkout and create a focused branch from the intended base. The default integration branch is main. Keep changes isolated from unrelated work.

The verification workflow uses JDK 17, the Maven Wrapper, Python 3.12 and Node 22 (use at least 22.20.0 for Skills CLI 1.9.1). A network connection may be needed to fetch build and validation dependencies. No publishing credentials or global agent installation are required for local verification.

From the repository root, in a disposable or isolated Python environment:

```sh
python -m pip install 'skills-ref @ git+https://github.com/agentskills/agentskills.git@83021e2be457a9b6679148719c1f6074f0ad975e#subdirectory=skills-ref'
python scripts/validate-skill-metadata.py
python -m unittest discover -s scripts -p 'test_*.py'
./mvnw clean verify -B -ntp
```

On Windows, use mvnw.cmd instead of the shell wrapper. See the [authoring guide](docs/skill-authoring-guide.md#3-verification) for JAR and discovery checks, and the [CI workflow](.github/workflows/skill-verification.yml) for full isolated installation and extraction commands. These commands reflect the existing workflow and review-branch implementation; check their availability on your base branch.

## Make the change

Use the appropriate collection and retain directory-matching skill names. Follow the existing file conventions. Reuse existing capabilities before building new ones. Keep essential instructions in SKILL.md, load references on demand, and avoid unrelated formatting or structural changes.

If changing a skill contract, check adjacent skills, dependency paths and shared artifacts. Preserve existing invocation policies unless the change explicitly revises them. Update affected collection indexes, README summaries, provenance and workflow documentation when relevant. Regenerate the [skill catalog](docs/skill-catalog.md) with `python scripts/generate-skill-catalog.py` after changing its inputs; CI checks it with `--check`. Commit the generated file with the source changes. Do not rewrite historical audit reports to conceal defects; append evidence to the remediation tracker instead.

## Licensing and attribution

Original contributions follow the repository's applicable license; contribute only material you are authorized to submit under that license. Third-party material retains its applicable upstream terms. Identify upstream repository, path, pinned revision, author, license evidence and local modifications. Keep the original notices and skill-local SOURCE.md as described in the design principles.

Consult the [provenance register](docs/skill-provenance.md). Do not assume that a public repository is permissively licensed, or that another repository by the same author grants permission. If a grant or notice is unclear, record the blocker and obtain clarification before introducing or redistributing the content. Do not commit secrets, private project data or unreviewed third-party scripts.

## Verify your contribution

Choose checks according to the risk and scope of the change.

| Change | Minimum relevant review |
| --- | --- |
| Documentation only | Read-back, accuracy and affected links |
| Skill instructions or metadata | Metadata validation, reference checks, representative positive/negative behavior and relevant boundary cases |
| Skill resources or licenses | Resource integrity, packaging, extraction and installation checks |
| Verification scripts | Unit tests including a representative failure case, and affected integration checks |
| Collection or build configuration | Complete inventory, Maven build, JAR comparison, extraction and target installation checks |

Actual agent behavior is distinct from installing files into an agent's skills directory. Use the upstream skill-creator evaluation workflow when feasible and compare against the old skill or no-skill baseline. Record the model, input, expected behavior, observed result and limitations. If a required check cannot run, mark it NOT RUN with the reason; do not substitute a successful static check for runtime evidence.

CI runs regular Maven verification and skill verification on pull requests. See [quality gates](docs/verification/quality-gates.md) for evidence artifacts, applicable behavioral review and separate publication requirements. Check the actual run results, not just a locally successful command. The existing distribution report documents tested versions and coverage. A green CI does not clear outstanding license or publishing blockers.

## Commit and submit a pull request

Prefer one logical change per commit and a concise imperative subject, such as docs: clarify skill activation or fix(skills): preserve dependency references. Do not overwrite unrelated contributor changes.

Target main for ordinary PRs unless the maintainer requests a different integration branch. Describe the purpose, changed files, compatibility and migration effects, reuse decision, provenance and verification. Use this compact PR outline:

```markdown
## Summary
- Problem and intended outcome
- Changed scope and any breaking changes

## Reuse and provenance
- Adopt / adapt / compose / build decision (if applicable)
- Upstream revision, license, retained notices and local modifications

## Verification
- PASS / FAIL / NOT RUN: command or case, observed result
- Remaining risks and blockers
```

Address review findings with focused commits. A contribution is ready for review when the relevant design criteria are met and limitations are visible; acceptance, merging and publication are separate decisions.

## Publishing

Do not run Maven deploy, manually dispatch release workflows or merge a publication-triggering change merely to test it. Pushes to main trigger the snapshot workflow. Publishing requires maintainer authorization, appropriate credentials, resolved redistribution permissions and applicable verification. Review existing [distribution limitations](docs/verification/distribution.md) before release.

Report suspected security problems without exposing credentials, private data or exploit details in public issues. Coordinate a suitable private disclosure channel with the maintainer when necessary.
