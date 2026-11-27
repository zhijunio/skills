---
name: spring-boot-kubernetes
description: >
  Use when deploying or reviewing Spring Boot workloads on Kubernetes, including Deployment and
  Service manifests, probes, graceful shutdown, resources, configuration, secrets, and rollout
  safety. Assumes a cluster and its platform conventions already exist.
---

# Spring Boot Kubernetes

Use this skill when preparing a Spring Boot workload for an existing Kubernetes cluster. Read
[Kubernetes guidance](references/kubernetes.md) and follow the cluster's Helm, GitOps, ingress,
namespace, and policy conventions. This skill covers the application workload, not provisioning
or securing the cluster.

Coordinate probes and Actuator exposure with
[Spring Boot observability](../spring-boot-observability/SKILL.md), image and runtime choices with
[Spring Boot Docker](../spring-boot-docker/SKILL.md), access to protected endpoints with
[Spring Boot security](../spring-boot-security/SKILL.md), and schema rollout compatibility with
[Flyway migrations](../flyway-migrations/SKILL.md).
