# Skill-creator evaluation integration

Status: source review complete; execution and host compatibility NOT RUN. This is a reuse contract, not a new evaluation runner.

## Pinned upstream

Use [Anthropic skill-creator](https://github.com/anthropics/skills/tree/e4bb5d960d87126ff3b3a8af1a104b7db67560ba/skills/skill-creator), revision e4bb5d960d87126ff3b3a8af1a104b7db67560ba. The skill-local LICENSE.txt contains Apache-2.0, copyright 2026 Anthropic, PBC. No upstream files are copied or vendored here. If future redistribution is proposed, inspect all relevant notices and dependencies, preserve applicable notices and document modifications.

Reviewed run_eval.py, scripts/utils.py, aggregate_benchmark.py, references/schemas.md and the entry-point evaluation workflow. Viewer and optimization scripts are not approved for execution solely by this review.

## Choose the correct evaluation path

| Task | Reuse | Limits |
| --- | --- | --- |
| Description trigger experiment | scripts.run_eval | Claude-specific temporary command experiment; not installed-skill or cross-host proof |
| Actual task execution | skill-creator matched agent workflow or fresh manual host sessions | Requires authorized authenticated runtime, isolated fixtures and observed evidence |
| Grading and summary | upstream grading schema and aggregate_benchmark | Supplemental; local required-assertion verdict remains authoritative |
| Human review | upstream viewer after separate source review | Local-only sanitized evidence; no publication |

The trigger dataset is a JSON array of objects with query and should_trigger fields. Behavioral evals use the upstream evals.json structure: skill_name, evals, integer IDs, prompts, expected_output and optional files/expectations. The entry point also describes assertions in eval_metadata.json; inspect the actual consumer before choosing fields. Keep canonical case IDs and approval sequences in local case documentation. Store datasets and workspaces outside packaged skills.

## Source-review findings and restrictions

- run_eval creates uniquely named files under .claude/commands in the discovered project root, starts claude -p with stream-json, detects early Skill/Read events and removes the temporary file. It may return before later tool calls and does not exercise the original installed skill or its manual-only policy.
- It suppresses child stderr and converts worker exceptions to false; a timeout, auth failure or invalid invocation may therefore look like non-triggering. A negative prompt can appear to pass when execution failed. Without independent successful execution evidence, treat those results as inconclusive/NOT RUN. Do not use raw trigger results as a blocking gate.
- Its defaults are ten workers, three runs per query, thirty seconds per query and 0.5 threshold. These are upstream defaults, not approved budget or acceptance criteria. Specify all relevant values explicitly and use unique queries. It does not provide a hard spending cap.
- Its frontmatter reader is a lightweight parser, not the repository's official metadata validator. Verify the exact description under test, especially folded or quoted YAML.
- aggregate_benchmark skips missing/invalid grading records, substitutes zero for missing metrics and can fall back from missing tokens to output_chars. Its delta is determined by configuration order, and metadata defaults to three runs and placeholder model names. Do not treat these defaults or substitutions as observed evidence. Check actual denominators, configuration identity, metric units, trial counts and model versions.
- Upstream grading uses boolean passed values; the repository distinguishes PASS, FAIL and NOT RUN. Keep incomplete runs visible in the local ledger, never fabricate a boolean pass to fit the upstream viewer. Aggregate only comparable complete observations and clearly report exclusions.
- Read relevant host/tool code before execution. Do not use unrestricted permissions, private projects, production secrets or a directory inside an existing .claude ancestor. Inspect host configuration and inherited skills, commands, hooks, tools and permissions. Use a fresh disposable environment. Do not run automatic description optimization before the baseline and held-out queries are established.

## Minimal manual integration procedure (NOT RUN)

1. Obtain the pinned upstream checkout in a separate scratch directory using Git; verify the exact commit and license. Do not install globally. Read the specific scripts before running them.
2. Use Python 3.11+ and confirm module imports using the upstream skill-creator directory as the module root. For a no-model CLI smoke check, use python -m scripts.run_eval --help and python -m scripts.aggregate_benchmark --help from that directory.
3. For actual trigger execution, select a disposable project outside the repository and any existing .claude ancestor. Add the upstream skill-creator directory to PYTHONPATH, keep cwd at the disposable project and use the module command below. Verify Claude CLI version, authentication, model, tools and permissions separately. No credentials in prompts or logs.
4. Only after explicit budget and execution authorization, run a small approved pilot with a single worker, explicit model, timeout, run count and threshold. The example below is a command template, not an approved threshold or executed command. Set all variables to approved values. Capture sanitized stdout/stderr, host execution evidence and exit status; if successful execution is not independently observable, raw output is not an acceptance result.

```sh
cd "$DISPOSABLE_PROJECT"
PYTHONPATH="$UPSTREAM_SKILL_CREATOR" python -m scripts.run_eval \
  --eval-set "$TRIGGER_DATASET" --skill-path "$CANDIDATE_SKILL" \
  --num-workers 1 --runs-per-query "$APPROVED_TRIALS" \
  --timeout "$APPROVED_TIMEOUT" --trigger-threshold "$APPROVED_THRESHOLD" \
  --model "$APPROVED_MODEL" > "$SANITIZED_RESULTS"
```

For actual workflow behavior use [Phase 02 cases](phase-02-workflow-acceptance.md) in fresh matched host sessions. Record outputs, actual file diffs, approval turns and check results using the [evaluation standard](../skill-evaluation-standard.md). For Codex, use native host observation and the same rubric; run_eval does not implement Codex. After collecting complete grading records, the upstream aggregator can produce a supplementary report with python -m scripts.aggregate_benchmark <iteration-directory> --skill-name <name> from its module root. Check its output against the local ledger before using any metric.

## Execution status

Independent Claude/Codex runtime and paid model authorization are not available in the established session context. No upstream checkout, CLI smoke, trigger trial, aggregation run or viewer was executed in this review. Q03 source review is complete; runtime integration is still open. No new evaluation framework, dependency installation or paid workflow added.
