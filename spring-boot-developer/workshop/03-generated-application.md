# 03 — Anatomy of the generated application

**In this chapter:**
- Locate the backend, frontend, database, and build configuration
- Understand how the REST API, persistence layer, and Vue client fit together
- Identify the skill assets and Spring Initializr files

No code to write here. Explore what the agent generated so the feature work in the next chapters is easier to review.

---

## 1. Where things live

Open the project in your editor and compare its actual tree with this outline:

```text
inventory-app/
├── pom.xml                         # Maven dependencies and build plugins
├── src/main/java/                  # Spring Boot application and API
├── src/main/resources/             # YAML configuration and static assets
├── src/test/java/                  # backend tests
├── frontend/                       # Vue application and frontend tests
├── compose.yaml                    # local PostgreSQL service
├── Dockerfile                      # app image, if generated
├── compose.full.yaml               # app and database, if generated
└── .github/lsp.json                # JDTLS integration for compatible agents
```

The build tool or generated options may change some filenames. Ask the agent to explain the real tree if yours differs.

## 2. The backend, layer by layer

Ask the agent to map the product request to the actual backend code:

```text
Use spring-boot-developer to explain the product API in this project, file by file.
Show where HTTP requests are validated, how product data is read and written,
how database constraints are enforced, and which tests protect those behaviors.
Explain why each layer exists and point to real paths. Do not edit files. If a
service layer is absent, explain whether the current logic needs one.
```

Locate the product model or persistence mapping, repository/mapper, controller, request/response DTOs, and any service. A simple CRUD app may not need a pass-through service; follow the project's actual design and [code organization guide](../references/architecture/code-organization.md).

## 3. Configuration — `application.yaml` or `application.properties`

Find the active Spring configuration. Identify server settings, datasource properties, migration/schema settings, and management endpoints. When adding configuration, prefer YAML as this skill recommends. Confirm local credentials come from Compose/environment defaults and that secret values are not committed. See [configuration](../references/configuration/configuration.md).

## 4. The database — `compose.yaml`

Inspect the PostgreSQL service, health check, port, and persistent volume. Spring Boot's Docker Compose integration can start the database during local development. Confirm the datasource URL points to `localhost` for a host process and the Compose service name when the app itself runs in a container. Use the [local development guide](../references/tooling/local-development.md).

## 5. The front-end — `frontend/`

### 5.1 The Vue build

Inspect `package.json`, the Vite configuration, and the Maven frontend plugin. The `/api` development proxy should point to Spring Boot, and the production build should place browser assets where the packaged app can serve them. See the [Vue integration guide](../references/frontend/vue.md).

### 5.2 Development and production

During development, Vite serves the UI and proxies API requests. In production, the Maven build packages the Vue output into Spring Boot's static resources. Verify the paths in the actual project rather than assuming a default.

## 6. The setup files

Review `.gitignore`, `.editorconfig`, `.gitattributes`, `.dockerignore`, `.env.sample`, and `.github/lsp.json`, plus `.devcontainer/` if you added it. Identify which files Spring Initializr created and which the skill generator added. Check that `.env` and build outputs are ignored.

## 7. Commits so far

Inspect `git log --oneline` and `git status`. The initial application should have the baseline checkpoint from chapter 02. Do not continue until any unexplained changes are reviewed.

---

**Checkpoint**

You can locate and explain the main backend, frontend, configuration, database, and build files. Any mismatch between the agent's explanation and the real project is resolved or recorded.

**Next →** [Chapter 04 — Adding categories](04-adding-categories.md)
