# 02 — Getting started

**In this chapter:**
- Generate a complete Spring Boot and Vue inventory application from one prompt
- Run it locally and inspect its REST API
- Create the first reviewed project checkpoint

After this chapter, you will have a working product inventory app with PostgreSQL persistence and no authentication.

---

## 1. Pick a working directory

Keep the generated app outside the skill directory. Use an empty workspace so the generator cannot overwrite unrelated files.

```bash
mkdir -p ~/workshop
cd ~/workshop
```

## 2. Start the coding agent

Start your coding agent in `~/workshop`. If the agent lets you choose a model, pick one that handles multi-file changes and can work across Java and Vue.

## 3. Your first prompt

Ask the agent to generate and implement the application. The database and persistence selections below are the generator defaults; include them in the prompt so the intended setup is explicit.

```text
Use spring-boot-developer to create an inventory management application named
inventory-app in the current workspace. First check the environment and read
the skill instructions. Use Maven, the default compatible Spring Boot and Java
versions, JPA persistence, PostgreSQL, and Docker support. Do not overwrite
existing files.

Implement a product inventory with a responsive Vue interface and REST API.
Products have a server-generated UUID, unique SKU, name, optional description,
non-negative decimal price, non-negative integer stock quantity, and
server-managed creation and update timestamps. Provide list, create, edit, and
delete behavior; show SKU, name, price, and quantity in the UI. Validate input
on the server, use request/response DTOs, and do not expose JPA entities.
Include focused tests, use a version-correct database migration if appropriate,
and keep authentication out of scope.

After implementation, run the relevant build and tests, start the app if the
environment allows, inspect the generated files, and report exact commands,
results, version choices, and any unresolved issues. Do not commit or publish
the project until I have reviewed the diff.
```

Let the agent work through generation, implementation, frontend setup, and checks. If a command fails, provide its actual output and ask for a focused diagnosis rather than restarting the whole task.

## 4. What the agent is doing

The agent should read the skill, use its generator for the backend skeleton, scaffold and normalize the Vue client, implement one vertical slice across the API and UI, and run checks. Review any plan it gives for the larger cross-layer change. The generator does not create the product API or Vue interface by itself.

## 5. Tour the generated project

In a second terminal, inspect the generated directory:

```bash
cd ~/workshop/inventory-app
ls -la
```

Expect a Maven project (`pom.xml`, wrapper, and `src/`), a Vue `frontend/`, project setup files, PostgreSQL Compose configuration, and Docker files. Exact paths can vary by Spring Initializr and skill choices. The next chapter tours the actual files.

## 6. Run the application

Ask the agent to start the application with the project wrapper and report the startup result. With Spring Boot Docker Compose support enabled, the app may start PostgreSQL from `compose.yaml` automatically. Otherwise, start the PostgreSQL service first.

```bash
./mvnw spring-boot:run
```

When startup completes, open [http://localhost:8080](http://localhost:8080). If the UI is not served yet, ask the agent to check the frontend build integration and startup logs.

## 7. Poke the REST API

In another terminal, check the product endpoint and create a sample product:

```bash
curl -i http://localhost:8080/api/products
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"DEMO-001","name":"Workshop notebook","description":"Sample inventory product","price":12.50,"quantity":25}'
```

Use the returned ID to inspect, edit, and delete the product. Check the actual controller route if this implementation chose a documented alternative.

## 8. Stop the application

Press Ctrl+C in the terminal running the app. If you started PostgreSQL separately, stop it with the matching Compose file after the app exits.

## 9. Commit the checkpoint

The generator does not initialize Git. If `git status` reports that the directory is not a repository, run `git init` in `inventory-app/` first; the generated `.gitignore` already excludes build outputs and `.env`.

Review the generated files, run `git status`, and commit only after you are satisfied with the changes. Use the repository's commit convention. This checkpoint gives you a safe point before the later workshop refinements.

## 10. (Optional) Publish to GitHub

Publishing is optional. If you want to follow the Azure deployment guide later, ask the agent to prepare a GitHub repository and GHCR image workflow. Confirm the owner, repository name, visibility, and every Git or GitHub command before it publishes anything. See [project setup](../references/tooling/project-setup.md) for the prerequisites.

---

**Try this yourself**

Ask the agent for one small change, such as displaying the product's update time in the table. Review the diff, run the app, and keep or revert the change before continuing.

**Checkpoint**

- The inventory UI loads and the API returns product data.
- You can create, edit, and delete a product.
- The build and relevant tests pass, or an environment blocker is recorded.
- You reviewed the diff and created a checkpoint commit.

**Next →** [Chapter 03 — Generated application](03-generated-application.md)
