# Spring Boot Full-Stack Workshop

Build an inventory application with an AI coding agent and `spring-boot-developer`. This prompt-driven workshop follows a complete project lifecycle: prepare the environment, generate and run an application, understand its structure, add a category feature, improve the Vue interface, test behavior, examine performance, and run it in Docker. The sample uses the generator defaults: JPA persistence and PostgreSQL.

Each implementation chapter gives you a prompt to enter in the coding agent. Review the agent's plan and diff, run the checks, and finish the checkpoint before continuing.

## What you'll build

- A Spring Boot product API with PostgreSQL persistence and versioned migrations
- A Vue frontend served by Spring Boot in production
- Backend and frontend tests for the main CRUD behavior
- Docker Compose support for local development and the full application stack

The base application has no authentication or user ownership. Those are optional extensions with security implications; define the authorization contract before adding them.

## Prerequisites

- A terminal, Git, and an editor
- Node.js and npm
- Java supported by the selected Spring Boot release
- Docker with Compose
- `spring-boot-developer` available to your coding agent

Use [Chapter 01](01-setup.md) to check the environment and prepare the workspace.

## Chapters

| Chapter | You will… |
| --- | --- |
| [00 — Introduction](00-introduction.md) | Understand the skill and the agent workflow |
| [01 — Setup](01-setup.md) | Check tools, skill availability, and workspace |
| [02 — Getting started](02-getting-started.md) | Generate, run, and checkpoint the complete app from one prompt |
| [03 — Generated application](03-generated-application.md) | Tour the backend, database, Vue, and build wiring |
| [04 — Adding categories](04-adding-categories.md) | Add and review a cross-layer inventory feature |
| [05 — Professional frontend](05-professional-frontend.md) | Build a complete Vue user experience |
| [06 — Testing](06-testing.md) | Cover behavior across web, database, and frontend layers |
| [07 — Performance](07-performance.md) | Inspect and measure before making performance changes |
| [08 — Deployment](08-deployment.md) | Validate and run the app with Docker Compose |
| [09 — Going further](09-going-further.md) | Plan and implement a focused extension |

### Appendices

- [A — Prompt cheat sheet](appendix-a-prompts.md)
- [B — Troubleshooting](appendix-b-troubleshooting.md)

Follow the chapters in order. Start the agent in the generated project workspace and give it the chapter prompt. Review changes and command output yourself; an agent's summary is not evidence that the application works. When a checkpoint fails, ask the agent to diagnose and fix the cause before moving on.

## Reference material

Each chapter points to focused topic guides under [`references/`](../references/). [SKILL.md](../SKILL.md) contains the overall workflow and topic-based guide index.

**Next: [Chapter 00 — Introduction](00-introduction.md)**
