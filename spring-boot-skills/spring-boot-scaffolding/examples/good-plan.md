# GOOD: request-only scaffolding

Request: create a new Maven project with Java 21, PostgreSQL, MyBatis-Plus and Redis. Do not
add Security. The destination does not exist.

Plan: inspect Initializr metadata, choose an advertised stable Boot 4 release, and request
only Maven, Java 21, PostgreSQL and Redis. Add MyBatis-Plus because it was explicitly
requested and is not an Initializr capability. Use the published version name rather than a
metadata alias if they differ. Keep the generated application and files; add no Security,
business code, CI or unrelated configuration. Report the project path and selected versions.
