# Cross-host verification

Status: Q08 documentation and installation evidence reviewed; native runtime NOT RUN.

## Evidence boundary

CI 46140369160 and Skill verification 46140368602 passed at 28a7616. Existing Skills CLI 1.9.1 installation checks compare both .agents/skills and .claude/skills layouts against source bytes, including local host configuration, and selected grill-with-docs dependency group. The metadata wrapper checks the portable projection and matching manual-only declarations. Neither proves native discovery, invocation, precedence or permissions.

Rechecked [Agent Skills specification](https://agentskills.io/specification), [Claude Code skills](https://code.claude.com/docs/en/skills), [OpenAI skill evaluation guidance](https://developers.openai.com/blog/eval-skills) and [OpenAI configuration documentation](https://developers.openai.com/plugins/deploy/submission-errors) on 2026-11-28. The earlier Codex skills URL redirected to [Build skills](https://learn.chatgpt.com/docs/build-skills); retain historic local-layout evidence and verify native behavior against the installed version. Hosted API and plugin packaging are separate targets, not certified by local installation.

## Native pilot

Use fresh disposable projects and authenticated host versions approved by the maintainer. Record CLI version, model, working directory, effective configuration, inherited skill/command inventory and permission mode. Isolate unrelated personal/global skills and hooks using documented host settings or a disposable account/environment; do not overwrite global configuration or disable permission checks. Use only synthetic data.

Install from the pinned candidate checkout with the existing Skills CLI command documented in distribution verification. For H06 omit the named sibling deliberately; do not interpret missing-resource failure as a normal successful installation. Record exact installed inventory. For collision tests use disposable synthetic skills only, never replace real installed skills.

Use native explicit syntax ($skill-name for Codex, /skill-name for Claude Code) where required. Record the actual submitted prompt and observed source path or loading trace. A model's claim that it used a skill is insufficient. A recommendation or ordinary answer to an implicit prompt does not necessarily mean the manual-only skill was invoked.

The [host cases](../../evals/compatibility/host-cases.json) supplement the eleven paired [invocation cases](../../evals/invocation/explicit-only.json) and existing workflow datasets. Execute each applicable case separately per host. H09 uses the W05 fixture; H10 requires the SB01 fixture. Do not count these as additional independent task successes if the same run supplies both records.

Use the [evaluation standard](../skill-evaluation-standard.md): report PASS, FAIL or NOT RUN per required assertion with actual traces, file changes and limitations. Missing observable loading evidence means the activation assertion is NOT RUN. Host policy is not an authorization boundary by itself; retain body-level approval and scope rules. No model spending or remote operation is authorized here.

## Remaining gaps

No independent Codex/Claude Code session launched, no hosted export tested, no new host support claimed. All ten cases across both hosts and eleven invocation pairs remain NOT RUN. A03 and A12 remain open for native behavior and applicable dependency/export checks.
