---
name: spring-ai
description: >
  Build Spring AI model integrations, retrieval-augmented generation, tool
  calling, MCP servers, AI security controls, or model-specific observability in
  Spring Boot applications.
---

# Spring AI

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md). If unavailable, inspect resolved dependencies and release-matched official documentation; report missing compatibility evidence.

Resolve the Spring AI BOM, provider and MCP SDK independently of Boot. Verify starter availability and configuration metadata for the selected release. Treat session, memory and native MCP examples as release-specific; never combine them across release lines.

Use the reference that matches the requested boundary:

- [Model integration](references/integration.md) for Spring AI dependencies,
  ChatClient, memory, prompt templates, structured output, RAG, and streaming.
- [AI security](references/security.md) for prompt injection, retrieval access,
  tool authorization, approval, and bounded execution.
- [AI observability](references/observability.md) for model metrics, token usage,
  cost attribution, and protected prompt telemetry.
- [MCP servers](references/mcp-server.md) for exposing application capabilities
  through MCP tools, resources, or prompts.

## Shared implementation rules

Inspect the project's Boot, Spring AI, provider, vector-store, and MCP versions
before changing dependencies. Preserve the managed BOM and use the APIs for that
version.

Derive caller and tenant identity from the authenticated application context.
Keep secrets and unnecessary private data out of prompts, logs, traces, and
tool results. Enforce permissions in application code at each data read and
side effect. Bound input, retrieved context, model iterations, tool calls,
result size, deadlines, retries, and spend. Test policy and failure behavior
without relying on a model's refusal text as proof of authorization.

Use the general [Spring Boot observability skill](../spring-boot-observability/SKILL.md)
for service-wide Actuator, Micrometer, tracing, and OTLP setup. Use this skill
for model- and AI-specific telemetry.
