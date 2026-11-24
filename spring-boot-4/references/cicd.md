# CI/CD

Read this when the user asks to add or change CI/CD for a Spring Boot project.
This reference uses GitHub Actions. Workflows are opt-in and are not added to
new projects by default.

## When to use

- The user requests GitHub Actions workflows or CI checks
- Change an existing workflow that builds, tests, or packages this application

## When not to use

- CI/CD was not requested → do not create workflow files
- Deployments to a specific cloud or Kubernetes platform → use that platform's
  deployment guidance when available
- GitHub repository administration unrelated to this application

## Rules

1. Add `.github/workflows/` only when the user requests CI/CD. Keep workflows
   focused on the requested events and checks; do not add release, publish, or
   deployment jobs unless requested.
2. Use the wrapper and verification task for the project's selected build
   tool. Read the Java version from the active build configuration or the
   repository's authoritative version file; do not silently choose a different JDK.
3. Use GitHub-hosted runners and official actions where suitable. Pin actions
   to full commit SHAs when the repository's security policy requires
   immutable references; otherwise use maintained major-version tags and
   review updates regularly.
4. Grant the workflow only the `permissions` it needs. Build and test jobs
   should use read-only repository contents unless a specific operation needs
   more. Do not expose secrets to pull requests from forks or execute
   untrusted pull-request code in a privileged `pull_request_target` workflow.
5. Configure the dependency cache appropriate to the selected build tool. Do
   not cache generated build output as a substitute for a reproducible build.
6. For service-backed tests, either make Compose services available to the
   runner per [docker.md](docker.md), or ensure Docker is available for
   Testcontainers per [testing.md](testing.md). Keep service credentials
   test-only.
7. Keep workflow output and artifacts free of credentials and sensitive
   configuration. Do not print environment variables or upload unrestricted
   build directories.
8. Report exactly which workflow triggers, jobs, and checks were added. A
   workflow file added locally is not evidence that a remote GitHub Actions
   run passed.

## GitHub Tags and Releases

GitHub Releases are based on Git tags. Keep this separate from
[Maven Release](maven.md): the Maven Release Plugin can update POM versions,
commit, and create a tag; GitHub can then use that tag for a Release page and
attached artifacts. A GitHub Release can also be created manually without
using the Maven Release Plugin.

1. Tags and release workflows are opt-in. Do not add a tag trigger, publish
   job, or release job by default. Create them only when the user asks for a
   release flow.
2. Agree on the tag format and version source before creating a tag. Keep the tag version consistent with the project's released version source.
   Do not silently rewrite project version files as part of a GitHub Release
   workflow.
3. Create annotated tags from a clean, reviewed commit. If the repository
   requires signed tags, use its signing policy. Pushing a tag and publishing
   a release are remote repository changes; do them only when explicitly
   requested.
4. A GitHub Release may be drafted and published from the GitHub UI or GitHub
   CLI. Include concise release notes and attach only the intended build
   artifacts. Do not upload credentials, configuration files, or the entire
   workspace.
5. If a workflow publishes a Release from a pushed tag, scope the trigger to
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

## Maven verification example

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
can run Docker Compose as required by [testing.md](testing.md). Do not remove
production dependencies or disable application auto-configuration just to
make CI start.

## Verify

Open every workflow under `.github/workflows/`. Confirm triggers match the
request, permissions are minimal, Java matches the project, its committed
build wrapper is used, required services are available, and no secrets are exposed.
Report the workflow definition separately from any remote run result.
