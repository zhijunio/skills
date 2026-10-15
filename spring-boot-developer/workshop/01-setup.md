# 01 — Setup

**In this chapter:**
- Install and verify Node.js, Java, Docker, and Git
- Make the coding agent and `spring-boot-developer` available in your workspace
- Install JDTLS for Java code intelligence when your agent supports LSP

Everything installed here is useful beyond this workshop. Follow the official installation guide for your operating system when a tool is missing.

---

## 1. Install the prerequisites

### 1.1 Node.js and npm

The Vue client and its production build use Node.js. Install the supported release listed in [`versions.json`](../versions.json) using your operating system's package manager or a version manager.

**Verify:**

```bash
node --version
npm --version
```

### 1.2 Java

The generator defaults are listed in [`versions.json`](../versions.json). Install a JDK within the Java range recorded there for Spring Boot 4.

**Verify:**

```bash
java --version
```

If Java reports the wrong version, check `JAVA_HOME` and the Java executable on your `PATH`.

### 1.3 Docker

Docker runs PostgreSQL for local development and the complete application stack later in the workshop. Install Docker Desktop on macOS or Windows, or Docker Engine and the Compose plugin on Linux.

**Verify:**

```bash
docker --version
docker compose version
docker ps
```

## 2. Install or choose a coding agent

Use an AI coding agent that can read and edit the workspace, run terminal commands, and load Agent Skills. Install it by following its official instructions, then verify its command or launch its application. If it supports model selection, choose a model that can work across Java, SQL, Vue, and shell commands.

## 3. Make spring-boot-developer available

If the skill is not already installed, clone or copy this repository into the skill directory configured for your agent. Start the agent in a separate project workspace, not inside the skill directory. Confirm that the agent can see `spring-boot-developer`; otherwise provide the absolute path to `SKILL.md` in your prompt.

## 4. Install JDTLS (optional Java code intelligence)

JDTLS gives compatible agents Java-aware navigation, references, and diagnostics. Install it using the [JDTLS guide](../references/tooling/jdtls.md) for your operating system.

**Verify:**

```bash
jdtls --help
```

The generated project includes `.github/lsp.json`. If your agent does not support LSP, continue without JDTLS.

## 5. Install a Git client

Install Git if it is missing. Configure your author name and email if you plan to create the workshop checkpoints.

**Verify:**

```bash
git --version
git config user.name
git config user.email
```

## 6. Sanity check

Run the skill's environment checker from the skill directory. It checks the versions and tools needed for the project you plan to generate.

```bash
node scripts/check-environment.mjs --docker --frontend
```

Also verify Git and, when installed, JDTLS:

```bash
git --version
jdtls --help
```

---

**Checkpoint**

- Node.js, npm, Java, Docker Compose, and Git are available.
- Your coding agent can load `spring-boot-developer`.
- JDTLS is available when the agent supports LSP.

**Next →** [Chapter 02 — Getting started](02-getting-started.md)
