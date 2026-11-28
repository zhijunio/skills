# Distribution Verification

Status: Remediation under review; post-change CI results and actual agent behavior remain to be recorded.

## 1. Verified pre-change evidence

At main revision [34ac468](https://github.com/zhijunio/skills/commit/34ac468db79d01f3955a2ef976b8acd9a6e1e6e0), the source tree contains 59 SKILL.md entry points across engineering (13), spring-boot-skills (39) and collected-skills (7).

- [CI run 46106506092](https://github.com/zhijunio/skills/actions/runs/46106506092), job 140860360395: Maven build passed under JDK 17; SkillsJars 0.0.7 logged that it skipped all three collection directories because they lack immediate SKILL.md markers. This establishes a packaging defect, not a successful skill inventory.
- [Snapshot run 46106506068](https://github.com/zhijunio/skills/actions/runs/46106506068), job 140860376897: build and verification passed, but deployment of the POM to Sonatype Central's snapshot repository failed with HTTP 401. This is a separate publishing authorization issue. Do not infer a specific secret or token failure without checking configuration.

The local assistant execution environment could not resolve github.com (git ls-remote failed), and Maven and skills-ref were not installed there. No local checkout or remote build was fabricated.

## 2. Packaging remediation (A05)

Keep SkillsJars 0.0.7 and configure one package execution per collection. Its [upstream README](https://github.com/skillsjars/skillsjars-maven-plugin/blob/main/README.md) describes direct skill subdirectories and the META-INF/skills layout. The current upstream PackageMojo source was inspected, but the exact historical tag for 0.0.7 was unavailable; the actual 0.0.7 behavior is supported by the CI logs above.

The [JAR verification script](../../scripts/verify-skill-jar.py) compares discovered skill names and source resource bytes against the built archive, detects duplicates and missing or unexpected entries, and verifies preservation of the two existing license files in META-INF/skill-licenses. This is not a determination of third-party redistribution rights.

From an approved checkout:

```sh
./mvnw clean install -B -ntp
python3 scripts/verify-skill-jar.py target/skills-0.1.0-SNAPSHOT.jar
```

The verification workflow also creates a temporary Maven consumer using the locally installed artifact, invokes com.skillsjars:maven-plugin:0.0.7:extract with useSkillsNameAsDirectory=true, and checks the resulting flat skill tree. Neither install nor extraction deploys to Maven Central.

## 3. Discovery and dependency closure (A03)

Use the upstream [Skills CLI](https://github.com/vercel-labs/skills) version 1.9.1. Its [command documentation](https://github.com/vercel-labs/skills/blob/e7fd0e9e52f029fe87729d8185cb8caf7049f678/docs/commands.md) documents listing, local sources, explicit agent selection and copy mode. The version was observed in the upstream package manifest and npm listing.

```sh
npx --yes skills@1.9.1 add /path/to/approved/checkout --list
```

A complete installation is tested in a temporary project, with no global installation or agent execution. The installation verifier compares each target's installed names and resources with the source inventory, and checks simple relative Markdown links in SKILL.md that resolve inside the source tree. This is intentionally not a complete Markdown parser or a behavioral evaluation.

Install the Spring Boot collection as a unit when using the spring-boot router. For grill-with-docs include grilling and domain-modeling. Single-skill installation does not imply automatic dependency resolution. For SkillsJars extraction, a common flat destination preserves sibling-relative paths; prefixed per-skill directory names may break those references. License files outside individual skill roots are archived but not necessarily propagated by extraction or CLI installation; verify notice distribution separately before publishing.

## 4. Metadata and host policy (A12)

Reuse the [official reference validator](https://github.com/agentskills/agentskills/tree/83021e2be457a9b6679148719c1f6074f0ad975e/skills-ref) pinned at commit 83021e2be457a9b6679148719c1f6074f0ad975e. Its strict validator rejects unexpected frontmatter keys. The repository wrapper calls the upstream parser and validator on a portable metadata projection, checks the two known Claude Code extensions separately, and checks agreement with the Codex policy. Unknown keys remain errors. Original source is never stripped or rewritten.

```sh
python -m pip install 'skills-ref @ git+https://github.com/agentskills/agentskills.git@83021e2be457a9b6679148719c1f6074f0ad975e#subdirectory=skills-ref'
python scripts/validate-skill-metadata.py
```

Use an isolated Python environment. The upstream commit is pinned; transitive dependency versions are not yet fully locked. This is a local source conformance check with a repository extension policy, not certification of strict hosted uploads, agent runtime invocation or portable exports.

## 5. Automation and verification states

The [skill verification workflow](../../.github/workflows/skill-verification.yml) is separate from publishing. It builds and installs locally in CI, verifies the JAR, extracts to a temporary directory, checks metadata using the upstream reference library, and exercises discovery and installation using the upstream CLI.

| Check | State at document creation |
| --- | --- |
| Pre-change Maven build | Passed in cited CI |
| Pre-change nested packaging | Defect confirmed in cited logs |
| Post-change Maven packaging and resource equality | CI pending |
| Post-change local Maven extraction | CI pending |
| CLI discovery and target project installation | CI pending |
| Installed entry-point dependency links | CI pending |
| Portable metadata projection and extension policy | CI pending |
| Actual Codex and Claude Code invocation behavior | Not run |
| Full reference/script audit and strict exports | Not run |
| Snapshot publishing | HTTP 401; authorization review needed |
| Applicable third-party rights and notices | Unresolved; release blocker |

Record post-change job URLs and results before closing findings. Keep the distribution fix on a review branch until the checks and redistribution/publishing decisions are resolved. Do not retry deployment with secrets in logs or change credentials automatically.

## 6. Publishing follow-up

The maintainer should confirm that the Maven Central Portal namespace matches io.github.zhijunio, snapshots are enabled for the intended namespace, the configured GitHub Actions secrets contain a valid Central Portal token pair, and the reusable workflow supplies the corresponding Maven server ID. The logs identify a 401 but do not prove which condition failed. Do not expose, rotate or request secret values in repository files.

Before merging a packaging change that makes previously omitted third-party skills distributable, confirm applicable permissions, notice preservation, intended publication scope, and release authorization. See [provenance register](../skill-provenance.md).
