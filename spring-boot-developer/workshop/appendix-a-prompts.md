# Appendix A — Prompt cheat sheet

Use the chapter prompts as written first. These patterns help when you need a focused follow-up or when you continue the project after the workshop.

## Project creation

```text
Use spring-boot-developer to generate [application] in [workspace]. Use [build tool],
[Java/Boot constraint], and [database]. Inspect the environment and skill options
first. Do not overwrite existing files. After generation, report selected
versions, files created, and verification commands.
```

For a complete full-stack request:

```text
Use spring-boot-developer to create [application]. Features: [observable behaviors].
Frontend: [Vue, React, Next.js, Angular, or vanilla JavaScript]. Persistence:
[JPA, MyBatis-Plus, or none]. Database: [PostgreSQL or none]. State whether
authentication is in scope. Include tests and Docker support if required.
Follow the skill's version manifest and do not overwrite existing files.
```

To keep the manifest versions:

```text
Use the versions specified in spring-boot-developer/versions.json. Do not override
Spring Boot, Java, Node, or frontend dependency versions unless I ask you to.
```

## Feature additions

```text
Add [one observable behavior]. Inspect the existing implementation and relevant
skill guides first. Preserve the current API, versions, architecture, and project
conventions. Identify API, persistence, frontend, and test changes before editing.
Keep the change focused and report the files changed.
```

## Controlling the agent

```text
Only implement [requested behavior]. Do not refactor unrelated code or add
unrequested dependencies. If the current contract or data model blocks the
change, explain the issue and ask before changing it.
```

Other scope controls:

```text
Keep the current project layout. Do not rename files or refactor beyond the
requested behavior.
```

```text
Do not add dependencies. Use the libraries already in the project.
```

```text
Do not edit files yet. Describe the change file by file and wait for my review.
```

## Review & iteration

```text
Review the current diff against [contract or acceptance criteria]. Report
missing behavior, incorrect status codes, data or authorization risks, test gaps,
and unrelated changes with file paths. Do not edit files during this review.
```

Then ask for one correction at a time:

```text
Fix [specific finding] in [file or behavior]. Preserve the rest of the reviewed
diff and rerun [focused check].
```

For an explanation or a challenge:

```text
Walk me through the changes file by file and explain why each is needed.
```

```text
The change builds, but I think [claim] is wrong. Re-check it against the code
and tests, then report the evidence before editing.
```

## Debugging & diagnostics

```text
[Command] failed with this output:
[paste redacted relevant output]

Inspect the affected code and configuration, trace the failure to its cause, and
propose the smallest fix. Preserve unrelated work. After the fix, rerun the
failing command and report the result.
```

## Refactoring

```text
Refactor [component] to [specific goal]. First identify callers, observable
behavior, and tests that protect it. Keep behavior unchanged, make the smallest
coherent change, and run the focused tests plus the relevant build check.
```

For a semantic rename or extracting a layer, name the affected API, persistence, frontend, and tests explicitly. Ask the agent to use Java-aware rename tools when available and preserve observable behavior.

## Testing

```text
Add tests for [observable behavior]. Choose the narrowest test layer that
exercises the relevant Spring behavior. Use PostgreSQL/Testcontainers for
PostgreSQL-specific behavior. Run the tests and report their exact names and
results; do not claim coverage for behavior the tests do not exercise.
```

## Performance

```text
Start with a read-only performance assessment of [request path]. Identify a
baseline measurement and likely limiting layer. Separate measured findings from
hypotheses. Do not change code until I review the proposed experiment.
```

## Docs & commits

```text
Update the documentation affected by this change. Keep commands and file paths
consistent with the repository. Review the final diff and report checks actually
run. Commit only after I have reviewed the changes, using the repository's
commit convention.
```

## spring-boot-developer meta-prompts

**Read the conventions:**

```text
Read SKILL.md and the focused [reference path] before answering. Summarize the
rules that apply to this change and point to their source sections.
```

**Follow a specific reference:**

```text
Apply the pattern described in references/[file].md. Check that it supports
this project's Spring Boot version and persistence choice. Explain any
adaptation before editing.
```

**Question a skill choice:**

```text
Summarize the guidance in spring-boot-developer about [choice]. Explain the
trade-offs for this project and identify the conditions that would justify a
different choice. Do not change files.
```

**Update the skill itself:**

```text
Update the focused spring-boot-developer reference for [topic]. Check the current
source of truth, supported Boot lines, and related links first. Keep the skill
independent, update its navigation if needed, and verify all local links.
```

When changing generator defaults, inspect `versions.json`, scripts, assets, and every documentation example that consumes the default.

## Things that tend not to work well

- “Make it production-ready” without naming the deployment environment or required behavior.
- “Improve performance” without a request path, baseline, or success measure.
- “Add authentication” without defining identity provider, token or session model, and authorization rules.
- Asking for backend, frontend, Docker, cloud deployment, and unrelated refactoring in one prompt.
- Accepting “done” without reading the diff or command output.
