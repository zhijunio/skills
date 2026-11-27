---
name: bootui
description: >
  Use when adding or configuring BootUI, the local developer console for a Spring Boot 4
  application. Covers starter selection, dev and local activation, production exclusion,
  security boundaries, and read-only diagnostics.
---

# BootUI

Use BootUI for local runtime inspection during development. It is not a production monitoring,
alerting, or tracing system. Read [BootUI guidance](references/bootui.md) before adding the starter,
changing activation, or exposing its console.

Keep activation opt-in to development profiles and loopback access. Review security filter chains,
captured diagnostics, and any panel that can mutate application state.

For application security policy, use [spring-boot-security](../spring-boot-security/SKILL.md). For
Actuator and production telemetry, use [spring-boot-observability](../spring-boot-observability/SKILL.md).
