# GOOD: constrained project generation

Request: create a new Spring Boot 4 web application with Maven, Java 21,
PostgreSQL, MyBatis-Plus, Redis, and OpenAPI. Do not add authentication. The
destination is a new directory.

Plan: read the allowed Boot lines and Maven default in `versions.json`, then
confirm the selected stable release and Java version are offered by Spring
Initializr. Generate only the requested web capabilities into a fresh staging
directory, inspect the archive before extraction, and keep the generated Maven
wrapper. Apply the house persistence, caching, architecture-test, and OpenAPI
defaults. Keep Security out. Run Maven verification and a bounded local startup
check when available, then report the versions, commands, test counts, and
checks that could not run.
