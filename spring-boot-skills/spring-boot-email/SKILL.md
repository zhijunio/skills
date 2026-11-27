---
name: spring-boot-email
description: >
  Use when sending transactional or notification email from Spring Boot, configuring JavaMailSender
  or SMTP, rendering email templates, or testing mail delivery with Mailpit. Does not cover inbound
  mail processing or email-provider administration.
---

# Spring Boot Email

Read [Email guidance](references/email.md) when adding or changing outbound email. Keep composition
in an application service, SMTP configuration external, and delivery behavior explicit around
database transactions.

Use [Spring Boot Docker](../spring-boot-docker/SKILL.md) for local Mailpit Compose setup,
[Spring Boot Web MVC](../spring-boot-webmvc/SKILL.md) for transaction boundaries, and
[Spring Boot testing](../spring-boot-testing/SKILL.md) for general test selection.
