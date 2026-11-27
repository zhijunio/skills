# CI/CD

Configure GitHub Actions, Renovate, and release workflows only when requested.

## GitHub Actions

- Keep workflows focused on the requested events and checks. Do not add
  release, publish, or deployment jobs unless requested.
- Use the wrapper and verification task for the project's selected build
  tool. Read the Java version from the active build configuration or the
  repository's authoritative version file; do not silently choose a different JDK.
- Use GitHub-hosted runners and official actions where suitable. Pin actions
  to full commit SHAs when the repository's security policy requires
  immutable references; otherwise use maintained major-version tags and
  review updates regularly.
- Grant the workflow only the `permissions` it needs. Build and test jobs
  should use read-only repository contents unless a specific operation needs
  more. Do not expose secrets to pull requests from forks or execute
  untrusted pull-request code in a privileged `pull_request_target` workflow.
- Configure the dependency cache appropriate to the selected build tool. Do
  not cache generated build output as a substitute for a reproducible build.
- For service-backed tests, either make Compose services available to the
  runner per [Spring Boot Docker](../../spring-boot-docker/SKILL.md), or ensure Docker is available for
  Testcontainers per [Spring Boot testing](../../spring-boot-testing/SKILL.md). Keep service credentials
  test-only. CI must execute required container tests; do not configure them
  to silently pass as skipped when Docker is unavailable.
- Keep workflow output and artifacts free of credentials and sensitive
  configuration. Do not print environment variables or upload unrestricted
  build directories.
- Report exactly which workflow triggers, jobs, and checks were added. A
  workflow file added locally is not evidence that a remote GitHub Actions
  run passed.

## Renovate

Use [the Renovate template](../templates/renovate.json) for a new configuration.
Copy it to `renovate.json` at the project root. If the project already has a
Renovate configuration, edit that file and keep its existing policies.

The `config:recommended` preset includes the Dependency Dashboard. The template
labels update PRs `dependencies` and leaves them for manual review. When the
project needs a weekly update window, set `schedule` and `timezone` for the
team. If the project opts into automerge, limit it to the requested updates and
require passing CI checks through branch protection.

Run the [config validator](https://docs.renovatebot.com/config-validation/) from
the project root.

```bash
npx --yes --package renovate -- renovate-config-validator --strict
```

For hosted Renovate, the Renovate GitHub App must have access to the repository.

## Tags and releases

GitHub Releases are based on Git tags. Keep this separate from
[Maven Release](../../spring-boot-maven/SKILL.md): the Maven Release Plugin can update POM versions,
commit, and create a tag; GitHub can then use that tag for a Release page and
attached artifacts. A GitHub Release can also be created manually without
using the Maven Release Plugin.

- Tags and release workflows are opt-in. Do not add a tag trigger, publish
  job, or release job by default. Create them only when the user asks for a
  release flow.
- Agree on the tag format and version source before creating a tag. Keep the tag version consistent with the project's released version source.
  Do not silently rewrite project version files as part of a GitHub Release
  workflow.
- Create annotated tags from a clean, reviewed commit. If the repository
  requires signed tags, use its signing policy. Pushing a tag and publishing
  a release are remote repository changes; do them only when explicitly
  requested.
- A GitHub Release may be drafted and published from the GitHub UI or GitHub
  CLI. Include concise release notes and attach only the intended build
  artifacts. Do not upload credentials, configuration files, or the entire
  workspace.
- If a workflow publishes a Release from a pushed tag, scope the trigger to
  the agreed tag pattern, grant `contents: write` only to that release job,
  and use the tag as the release version. Never run a privileged release
  workflow from untrusted pull-request code.

Manual tag and release flow:

```bash
git status --short
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
gh release create v1.2.3 --generate-notes path/to/release-artifact.jar
```

The commands above change remote state. Run them only for a specifically
requested release and after confirming the tag, commit, and artifact.

## Example workflow

This example is Maven-specific. For another build tool, use its committed
wrapper and equivalent verification task. Replace `JAVA_VERSION_FROM_BUILD_CONFIG` with
the project's configured Java version and adjust the triggers:

```yaml
name: Verify

on:
  push:
    branches: [main]
  pull_request:

permissions:
  contents: read

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: JAVA_VERSION_FROM_BUILD_CONFIG
          cache: maven
      - run: ./mvnw --batch-mode --no-transfer-progress verify
```

If the application has a datasource on the test classpath, ensure the runner
can run Docker Compose as required by [Spring Boot testing](../../spring-boot-testing/SKILL.md). Do not remove
production dependencies or disable application auto-configuration just to
make CI start.

## Verify

Open the workflows changed or added under `.github/workflows/`. Confirm
triggers match the request, permissions are minimal, Java matches the project,
the committed build wrapper is used, required services are available, and no
secrets are exposed. Report workflow configuration separately from any remote
run result.

The Maven workflow above is an example. Adapt it to the project’s configured Java version, build wrapper, triggers, and required checks.
