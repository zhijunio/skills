# 00 — Introduction

**In this chapter:**
- What an Agent Skill is and what `spring-boot-developer` provides
- How it relates to Spring Initializr
- How to work with an AI coding agent during the workshop

No commands to run here. The hands-on starts in [Chapter 01](01-setup.md).

---

This hands-on workshop uses `spring-boot-developer` with an AI coding agent to build an inventory application. You will generate and run the application, understand its structure, add categories, improve the Vue experience, test behavior, measure before tuning, and run the Docker stack.

You do not need to know Spring Boot in advance. You should be comfortable with a terminal, reviewing files, and running commands. The agent does the implementation work; you steer it through prompts and verify its changes.

## 1. What is an Agent Skill?

An AI coding agent can inspect files, run commands, and edit code in response to natural-language instructions. Without project-specific guidance, its implementation choices can vary from one request to another.

An Agent Skill is a portable set of instructions, scripts, and references that gives an AI coding agent focused knowledge for a kind of task. Think of it as a senior engineer's checklist that travels with the agent.

## 2. What spring-boot-developer does

`spring-boot-developer` helps an agent create and maintain Spring Boot applications. It includes a project generator, environment checker, setup assets, references for Spring Boot development, frontend integration guides, and deployment guidance. The generator uses Spring Initializr for the backend; the agent then follows the skill's guides to implement the requested behavior.

The skill creates Spring Boot 4.x projects. Existing Boot 3.5.x projects keep their version and use the differences collected in the version compatibility guide. See the [version compatibility guide](../references/spring-boot-migration.md).

## 3. How this differs from Spring Initializr

Spring Initializr generates a backend skeleton from selected dependencies. This skill adds project setup assets and guides an agent through application code, tests, frontend integration, and delivery. The workshop demonstrates that staged workflow; the generator itself does not create the product API or Vue app.

## 4. The mindset shift

When coding with an agent, describe outcomes and constraints, then review the changes it makes. The agent chooses files and implementation steps; you check whether the result matches the request. Use this loop in implementation chapters:

1. Give the agent the chapter prompt and let it inspect the repository.
2. Review its plan when the change spans layers.
3. Inspect the diff and ask focused follow-up questions.
4. Run the checks and verify the result in the browser or API.
5. Commit a working checkpoint before continuing.

If the agent changes unrelated files, inspect `git diff` and restore only the unwanted changes. Do not accept a completion summary without checking the evidence.

## 5. What you'll learn

By the end, you will be able to generate a Spring Boot application from a prompt, explain its key files, steer an agent through cross-layer changes, review tests and a Vue interface, and run a database-backed application with Docker Compose.

## 6. Supported project baselines

New projects use Spring Boot 4.x. A project's current build files and conventions take precedence over generator defaults. See the [version compatibility guide](../references/spring-boot-migration.md).

---

**Checkpoint**

No commands to run in this chapter. Pause for questions, then prepare the environment in [Chapter 01](01-setup.md).

**Next →** [Chapter 01 — Setup](01-setup.md)
