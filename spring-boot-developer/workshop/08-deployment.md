# 08 — Deployment

Package and run the application in the same forms a deployment pipeline would use. This chapter covers the executable JAR and the generated Docker stack. Native Image and Azure are optional follow-ups.

Read the [Docker](../references/deployment/docker.md), [GraalVM Native Image](../references/deployment/graalvm.md), [configuration](../references/configuration/configuration.md), and [Azure](../references/deployment/azure.md) guides for detailed instructions.

## 1. Build a production JAR

Ask the agent to package the backend and frontend together:

```text
Use spring-boot-developer to build the production artifact for this inventory application.
Inspect the Maven frontend integration and the configured test phases. Run the
clean package or verify command appropriate to this project. Confirm that the
JAR exists and contains the Vue static assets. Report the artifact path and
command output; do not infer success from the build configuration alone.
```

Review the result. The production build should include the Vue assets so the application can serve the UI and `/api` from one origin. Inspect the artifact path and start the packaged application with the project configuration:

```bash
ls -lh target/*.jar
APP_JAR=$(find target -maxdepth 1 -type f -name '*.jar' | head -n 1)
java -jar "$APP_JAR"
```

With `compose.yaml` in the working directory and Docker Compose support enabled, Spring Boot may start PostgreSQL automatically. Otherwise start the database with `docker compose up -d postgres` first. Stop the packaged app with Ctrl+C, then stop the database with `docker compose down` when finished.

## 2. Run the whole stack in Docker

Ask the agent to validate and start the generated Docker configuration:

```text
Use spring-boot-developer to validate and run this application with Docker Compose.
Inspect compose.yaml, compose.full.yaml, the Dockerfile, and the Docker guide.
Validate both Compose configurations, then build and start the full application
and PostgreSQL stack. Check health and logs. Do not delete named volumes. Report
exact commands and results, and identify any development credentials that must
be replaced before deployment.
```

Review the Compose configuration before accepting it. When the app is ready, open `http://localhost:8080`, create a product, refresh, and confirm persistence. Stop the stack with `docker compose -f compose.full.yaml down`; avoid `down -v` because it deletes database data.

### Understanding the Dockerfile

Read the [Docker guide](../references/deployment/docker.md) and trace the build stages, Maven wrapper use, frontend asset generation, runtime image, user, and health check. The default Dockerfile is a JVM image. The skill also provides AOT, CRaC, and native-image variants; use the guide and project configuration before choosing one.

### Inspect the image

Ask the agent to build with a clear local tag, inspect its size and runtime configuration, and verify the container health. Distroless images do not include a shell; use Compose logs and health endpoints rather than `docker exec ... sh`.

## 3. GraalVM Native Image (optional)

If native startup or memory is relevant, first ask the agent to verify the toolchain and project profile:

```text
Use spring-boot-developer to check whether this project is ready for a GraalVM
native build. Inspect the selected Spring Boot version, Maven profile, installed
GraalVM tooling, and known reflection or native hints. Do not build yet. Report
the exact build and run commands and the measurements needed for a fair
comparison with the JVM image.
```

For a compatible Maven project, the [GraalVM guide](../references/deployment/graalvm.md) documents commands such as:

```bash
./mvnw -Pnative native:compile
docker build -f Dockerfile-native -t inventory-app-native:local .
```

The native build is slower and platform-specific. Test the native executable and compare startup time, memory use, and image size with the JVM version under the same conditions. Keep it only when the trade-offs help this application.

## 4. Production configuration

Ask the agent to review the deployment settings without exposing any real secret values:

```text
Review this application's production configuration for deployment readiness.
Check database credentials, active profiles, management endpoints, health probes,
graceful shutdown, ports, and logging. Do not read or print local .env files or
secret values. Report which values must be supplied through the deployment
platform's secret store and which endpoints should remain private.
```

Use environment variables or the platform's secret store for credentials. Keep Actuator endpoints restricted to the management boundary. Replace local placeholder credentials before using an image outside development.

## 5. A word on secrets

Never commit real credentials or paste `.env` contents into an agent prompt. Keep local secrets in ignored files and provide deployment credentials through the platform's secret store. Review generated Compose defaults before publishing an image. Ask the agent to audit references to secret values without reading those values:

```text
Inspect the project for places where database or application secrets are
configured. Do not read or print .env files, actual secret values, or tokens.
Report only the variable names, configuration sources, and whether each source
is appropriate for development or deployment. Do not edit files.
```

## 6. Optional: deploy to Azure Container Apps

Use the [Azure guide](../references/deployment/azure.md) for Azure Container Apps and PostgreSQL Flexible Server. It covers image publishing, secrets, and identity. Before creating resources, ask the agent for a plan that identifies the subscription, region, resource names, public endpoints, and estimated cost. Deploy only after reviewing the plan and confirming the cloud commands.

```text
Use spring-boot-developer and its Azure deployment guide to prepare a deployment
plan for this inventory application. Do not create or modify cloud resources.
List prerequisites, subscription and region assumptions, resources, public
endpoints, image publishing steps, secret names, and commands with estimated
costs. Flag anything that needs my decision before deployment.
```

**Checkpoint**

- The executable JAR builds and contains the frontend assets.
- Compose configuration validates and the app plus PostgreSQL run successfully.
- The UI and API respond and data persists across an application restart.
- Production secrets and management endpoints have been reviewed.

**Next →** [Chapter 09 — Going further](09-going-further.md)
