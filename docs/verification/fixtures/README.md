# Disposable workflow fixtures

These are synthetic inputs for manual Codex/Claude Code sessions, not a new agent runner. The fixture generator supports W02, W04, W05 (including W05-cycle), W06, W07, W10 and W16 from [workflow acceptance](../phase-02-workflow-acceptance.md). Other cases still require manual setup.

From the skills repository, choose a **nonexistent** directory under an existing disposable parent:

    python docs/verification/fixtures/create_fixture.py W07 /tmp/skills-eval-w07

In that directory, initialize a local git repository, record the initial files and baseline test result before installing skills. Install candidate skills from the intended pinned revision using the repository's documented installation method. Do not commit credentials, use a production checkout, enable unrestricted permissions or publish the fixture. Start a fresh authenticated host session and provide the matching W07 prompt. Supply only the approvals defined by the case.

For W02 and W07 the initial unittest command is expected to fail; after the correct repair it should pass. W06 has an additional deliberately unavailable required acceptance environment; even a green local suite cannot satisfy that obligation. W04/W05/W16 require checking that protected files are unchanged. W10 requires an actual isolated SQLite check; no fixture code pre-proves the hypothesis.

Record the before/after diff (excluding installed skill files), exact commands and outputs, model/host versions, loaded skills where observable, approval turns and assertions in the acceptance evidence record. To inspect ordering of status writes versus code writes, capture the host's actual tool trace; a final diff alone cannot prove ordering. Reset to a newly generated fixture for each run. No independent agent execution is claimed by the generator.

The generator needs Python 3 and the standard library only. It refuses an existing destination. It does not install skills, launch an agent, call a network service or create commits.
