---
name: project-foundation
description: Choose and record a project's technical foundation, then initialize and verify its runnable skeleton before feature specifications or implementation.
disable-model-invocation: true
---

# Project Foundation

Choose the project's technical options and establish a small, runnable baseline. This step follows product requirements and precedes feature-level behavior specifications. It decides how the product will be built and prepares the project; it does not implement product features.

## Inputs

- Accept a project directory, product brief, PRD, requirements document, or a combination.
- Honor paths and constraints supplied by the user. When a PRD or `docs/requirements.md` exists, read it before proposing a stack.
- Inspect the target directory and repository state before writing. Preserve existing work and conventions. Never replace or delete existing project files as part of initialization.
- If the product intent or constraints are too unclear to compare meaningful options, ask focused questions before recommending a stack.

## Workflow

1. **Establish constraints.** Identify the product's client type (web, mobile, desktop, CLI, or service), target operating systems, runtime needs, expected scale, security or regulatory needs, deployment environment, team skills, existing systems, and explicit preferences. For desktop products, clarify native OS integration, offline use, supported platforms, packaging, updates, and distribution requirements. Separate confirmed constraints from assumptions.
2. **Research current options.** Discover candidates from the product's constraints and the user's preferences rather than relying on a fixed technology catalog. When versions, support, compatibility, or ecosystem health could have changed, verify candidates using current primary sources such as official documentation, release notes, and maintained project repositories. Check licensing and maintenance for third-party dependencies. Cite sources that materially affect the decision.
3. **Compare viable foundations.** Present two or three realistic combinations of client approach, language/runtime, framework, data store, build and dependency tools, testing approach, and deployment shape. Discover candidates from the project's constraints and current research rather than treating examples or familiar technologies as a complete menu. Evaluate user-named technologies alongside other credible options. For desktop products, consider native UI, cross-platform native UI toolkits, and webview-based shells as distinct approaches; assess OS integration, supported targets, packaging, code signing and notarization, update delivery, security, and maintenance. For compiled or systems-oriented options, consider runtime needs, ecosystem maturity, cross-compilation, and FFI boundaries. Explain fit, tradeoffs, operational cost, and migration or lock-in risks against the constraints. Recommend a foundation while keeping the user's decision explicit.
4. **Confirm the technical baseline.** Summarize the recommended choices and unresolved decisions. Ask the user to approve or revise the material choices before planning project initialization. Do not turn assumptions into approved decisions.
5. **Choose initialization method.** Inspect the target repository for existing setup scripts, generators, templates, and conventions. Ask whether the user already has a preferred initialization script or generator. Inspect its effects and fit with the approved stack. If none is available, recommend a suitable maintained generator or official initializer and explain why. Check its actual capabilities against the requested features. Present the method and expected changes, then wait for the user to choose or approve it before running it; approval of the stack alone does not approve an initializer. Never run an initializer that overwrites existing files without explicit approval.
6. **Initialize the project.** Use the selected initialization method to create the minimum conventional structure needed to build, start, and test the application. If no suitable generator exists, create only foundational wiring such as dependency management, environment configuration, a health endpoint where appropriate, a minimal test, and basic developer commands. Do not implement product use cases.
7. **Verify the baseline.** Run the project's standard format or lint check, build, focused tests, and a startup smoke check where the environment supports them. Report each result separately and identify checks that could not run. Fix failures caused by the initialization before handing off.
8. **Record and hand off.** Write or update a brief `docs/tech-stack.md` centered on the frontend and backend technologies and versions. Add data storage, messaging, or desktop/mobile platform only when they are part of the chosen stack. Use a compact table and add only a short note for a choice that needs context; keep initialization commands, option comparisons, and detailed rationale out of this file unless the user asks for them. Keep product behavior in the PRD and requirements. Tell the user the initialized path, selected stack, and verification results; the next step is `create-spec` for a use case.

## Decision summary

Before initialization, present:

- Confirmed constraints and assumptions
- Two or three viable options with concrete tradeoffs
- Recommended stack and why it fits
- Existing or recommended initialization method and its effects
- Project skeleton scope
- Open technical decisions

Keep this review focused on choices that materially affect implementation, deployment, security, maintenance, or cost. Do not ask the user to approve routine file names or conventional defaults.

## Safety and scope

- Treat the approved PRD and requirements as product scope. Flag contradictions rather than silently changing them.
- Initialize only after the user has approved both the technical baseline and the selected initialization method. Ask before writing to a different directory or replacing any existing file.
- Keep secrets out of generated files. Use placeholders and document how developers provide local values.
- Prefer supported, boring technologies that satisfy the constraints. Explain meaningful alternatives without expanding the project into a framework showcase.
- Stop at a working foundation. Leave product behavior and feature acceptance criteria to `create-spec`.
