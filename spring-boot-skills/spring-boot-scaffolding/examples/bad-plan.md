# BAD: stale version and unrequested stack

Request: create a new Spring Boot 4 service in a new directory using Gradle,
Java 21, and no database or authentication.

Bad plan: hard-code an old Boot version without checking `versions.json` or
Initializr metadata, download and extract it over an existing checkout, replace
Gradle with Maven, and add PostgreSQL, Redis, JWT, and a deployment workflow by
habit. This ignores the version policy, destination boundary, and explicit
feature exclusions.
