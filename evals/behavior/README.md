# Core workflow behavior cases

The case registry maps W01–W18 and two explicit variants to the [Phase 02 acceptance specification](../../docs/verification/phase-02-workflow-acceptance.md). It is a local multi-skill manifest, not the upstream single-skill evals.json schema. Use actual host sessions or the approved skill-creator workflow; do not pass this manifest to run_eval. The named skill is the focal responsibility, not an exclusive routing assertion (W02 deliberately checks unnecessary planning).

Eight cases have generated fixtures; twelve require manual setup from the acceptance specification. The W01 follow-up is conditional on actual review, never preapproved. Full new-product continuation remains separate. Use native explicit invocation where required and record the exact prompt used.

For generated cases, create two independent copies using the [fixture generator](../../docs/verification/fixtures/README.md). Preserve one untouched baseline outside the agent's working directory. Install the selected skill revision only in the candidate directory. Record the initial files and baseline checks, then run the prompt in an authorized fresh host session. Do not expose the expected assertions or evaluator script to the agent as task instructions.

## Offline checks

The checker compares an untouched fixture with the candidate after the session; it does not launch agents or execute candidate code. It examines only selected final-state assertions for W04, W05, W05-cycle, W06, W07 and W16.

    python scripts/check_workflow_outcome.py W04 /tmp/w04-before /tmp/w04-after

A PASS means only that the selected filesystem assertion passed. The checker always reports full behavioral verdict NOT RUN because it cannot establish actual approvals, operation ordering, tool execution, transient changes or statements. Check trace and sanitized command evidence separately. For W02 and W07 use the existing regression suite in an authorized isolated execution environment; verify test assertions were not weakened. W10 needs real SQLite execution evidence. Do not run unknown generated code outside the sandbox.

The baseline and candidate must be separate directories. The checker does not follow symlinks in protected files and fails closed for missing files. It is not a security sandbox and cannot detect remote effects. No agent runs or model spending are authorized by this document.

All cases remain NOT RUN in independent agents.
