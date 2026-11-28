# Quality gates

Status: Q09 deterministic gate extension implemented on review branch; latest execution pending. Model evaluation and publication remain separate.

## Gate responsibilities

| Gate | Required evidence | Execution |
| --- | --- | --- |
| G1 metadata and catalog | Portable metadata, known host extensions, unique names, current catalog | Skill verification / metadata |
| G2 verification helpers and evaluation inputs | Unit tests, dataset integrity, synthetic fixture checks, offline assertion checks | Skill verification / metadata |
| G3 resource integrity | Local Markdown references, including evals; installed entry-point links | Skill verification / local-links and discovery |
| G4 packaging | Maven build, JAR byte comparison, isolated SkillsJars extraction | CI and Skill verification / package |
| G5 distribution | Upstream CLI listing and isolated full/selected installation | Skill verification / discovery |
| G6 activation and behavior | Actual independent host execution, required assertions, trace and test evidence | Separately authorized evaluation |
| G7 efficiency | Matched measured usage and correctness evidence | Separately authorized evaluation |
| G8 release | Maintainer approval, applicable licenses, verified publishing authorization and release criteria | Separate publication process |

G1–G5 are ordinary deterministic review checks. The workflows report failures; branch protection settings and required-check enforcement have not been inspected or changed. Do not claim that a merge is technically prevented by these checks. G6–G8 require separate decisions and evidence. Missing applicable behavioral evidence is NOT RUN, not PASS.

## Evidence artifacts

Metadata job records the checked-out Git revision, Python version, pinned-validator output, verbose helper-test output, catalog check and static context measurement. Logs and JSON are uploaded as a short-retention CI artifact even if a check fails, where available. Each command preserves its own exit status; later checks do not turn an earlier failure into success. The local-links job retains its existing audit artifact and now includes evals Markdown. Packaging and discovery retain their existing job logs. Review the actual job conclusions and available evidence; an uploaded artifact alone proves nothing about success.

The context report measures full entry-point files, not model input tokens. No static size threshold is enforced. CI does not execute arbitrary candidate-generated code, evaluate models, or collect authenticated agent traces.

## Opt-in model evaluation design

No automated model workflow is enabled by this change. Start with a maintainer-authorized local pilot in a disposable project using the pinned evaluation tooling or native host sessions. Before execution record:

- Pinned skill and fixture revisions, selected cases, host/model version, control/baseline/candidate configurations and required assertions.
- Approved number of trials, model spending limit, time limit, concurrency and tool permissions.
- Isolated synthetic data, no production access, no unrestricted execution, no deployment or publication.
- A provider/account-level hard spending cap where available; do not mistake timeout, worker count or an estimate for a monetary cap. If cost cannot be bounded within the approved policy, do not automate the run.
- Sanitized evidence destination and retention; no raw credentials or private transcripts in repository or public Actions artifacts.

If a future manually dispatched evaluation workflow is warranted, review it separately. Run only trusted reviewed revisions, not arbitrary fork PR code with model secrets. Use explicit environment approval, least-privilege permissions, bounded inputs and concurrency, and a fail-closed budget mechanism. Avoid pull_request_target execution of untrusted code. Do not add credentials or enable paid execution under Q09 without explicit approval.

Use the [evaluation standard](../skill-evaluation-standard.md), [integration review](skill-creator-integration.md) and [cross-host procedure](cross-host-verification.md). Before a release, inspect applicable evidence and blockers in the [remediation tracker](../audits/remediation-plan.md). Green CI does not resolve A04 or A13.

## Local reproduction

From repository root:

```sh
python scripts/validate-skill-metadata.py
python -m unittest discover -s scripts -p 'test_*.py' -v
python scripts/generate-skill-catalog.py --check
python scripts/measure_skill_context.py
```

See [CI workflow](../../.github/workflows/skill-verification.yml) for pinned tooling, full installation, package and link checks. Model tests and hosted exports remain NOT RUN.
