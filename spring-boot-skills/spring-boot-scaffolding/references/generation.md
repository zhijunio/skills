# Spring Initializr generation

1. Check that the requested destination does not exist. Use Spring Initializr at
   `https://start.spring.io`; do not use a bundled project-generation script or another
   provider unless the user asks for one.
2. If the user supplied an exact Boot release, use it directly. Otherwise fetch
   `https://start.spring.io/metadata/client` and select an advertised stable Boot 4 release.
   Fetch metadata to resolve a requested dependency only when its Initializr identifier is
   unclear. Select only the requested build tool, Java version, release and dependencies;
   do not choose an RC, milestone, or snapshot unless requested.
3. Pass the published Boot release version in the generation request. Initializr metadata
   can expose a compatibility id such as `4.2.0.RELEASE` while its display name is `4.2.0`.
   In this case pass `4.2.0`, not the id. Confirm that the generated Maven parent or Gradle
   plugin uses the published coordinate. If it does not, check the corresponding artifact
   in Maven Central and stop if no matching release exists; never leave an unresolvable
   version in the project.
4. Make the generation request, check the HTTP response and generated Boot coordinate, then
   extract the ZIP into the new destination. Do not overwrite an existing project. Batch
   these steps into one tool call when practical.
5. Keep the generated wrapper and project files. Do not add dependencies, features, tests,
   CI, configuration or documentation beyond the user's request.

Report the destination and the selected Spring Boot, Java, and build-tool versions. Run
builds, tests or startup checks only when requested.
