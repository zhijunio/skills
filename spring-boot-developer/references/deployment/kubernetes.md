# Kubernetes

Use this guide when deploying a Spring Boot HTTP service to an existing Kubernetes cluster. It covers the application workload contract; it does not provision or secure the cluster itself. Follow the platform's Helm, GitOps, namespace, ingress, and policy conventions when they already exist. For local images, Compose, and image hardening, see [Docker](docker.md).

## Quick lookup

| Symptom or task | Start here |
| --- | --- |
| Pod restarts before the application finishes starting | [Probes, shutdown, and rollout behavior](#probes-shutdown-and-rollout-behavior) |
| Pods are running but the Service has no ready endpoints | [Deployment and Service](#deployment-and-service), then [Validate and troubleshoot](#validate-and-troubleshoot) |
| Runtime settings or mounted secrets are missing | [Configuration and secrets](#configuration-and-secrets) |
| A rollout stalls or rollback affects a database change | [Probes, shutdown, and rollout behavior](#probes-shutdown-and-rollout-behavior) |
| Need to configure Spring health endpoints for kubelet probes | [Prepare the application](#prepare-the-application) |

## Prepare the application

Use Actuator's liveness and readiness health groups. Add a startup probe when initialization can take longer than the normal liveness window:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never

server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s
```

Spring Boot exposes `/actuator/health/liveness` and `/actuator/health/readiness`. Liveness should report whether this process can make progress; do not include a database or remote service in it. Readiness may include a dependency only when the service cannot safely serve requests without that dependency. A failed readiness probe removes the Pod from Service endpoints; a failed liveness probe restarts the container. See [Actuator health probes](../observability/observability.md).

If Spring Security protects Actuator, allow the kubelet to call only the probe routes without application credentials, and keep those routes off public ingress. A separate management port can be used as a network boundary, but the probe should test the main server as well: consider Spring Boot's `management.endpoint.health.probes.add-additional-paths=true` to expose `/livez` and `/readyz` on the application port. Do not expose health details to anonymous callers.

## Deployment and Service

This manifest is a starting example. Replace the image, resource values, and labels; align the container identity and writable paths with the built image. Use an immutable registry digest for release deployments.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orders-api
  labels:
    app.kubernetes.io/name: orders-api
spec:
  replicas: 2
  selector:
    matchLabels:
      app.kubernetes.io/name: orders-api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app.kubernetes.io/name: orders-api
    spec:
      terminationGracePeriodSeconds: 40
      automountServiceAccountToken: false
      securityContext:
        runAsNonRoot: true
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: app
          image: registry.example.com/orders-api:1.0.0
          ports:
            - name: http
              containerPort: 8080
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              memory: 1Gi
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: ["ALL"]
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir:
            sizeLimit: 64Mi
---
apiVersion: v1
kind: Service
metadata:
  name: orders-api
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: orders-api
  ports:
    - name: http
      port: 80
      targetPort: http
```

The probe timings and resource values above are examples, not defaults to copy blindly. Set the startup budget from measured cold-start time. Set CPU and memory requests from observed workload needs; set memory limits with JVM heap, metaspace, direct buffers, thread stacks, and native libraries in mind. CPU limits can cause throttling, so follow the cluster policy and validate their effect under load rather than adding an arbitrary limit. The JVM's `MaxRAMPercentage` controls heap sizing, not total process memory.

`maxUnavailable: 0` requires capacity to run an extra Pod during rollout and enough ready replicas to continue serving. Adjust the rollout policy to the service's capacity and availability needs. A readiness probe is necessary for traffic gating; it does not replace application-level smoke checks or rollback criteria.

## Configuration and secrets

Keep non-sensitive runtime settings in a ConfigMap or the platform's configuration mechanism. Inject secrets from the cluster's approved secret manager. Kubernetes Secret manifests are base64 encoded, not encrypted by that encoding; do not commit real credentials or tokens to source control.

For file-mounted secrets, Spring Boot config trees map filenames to property names. For example, mount files named `spring.datasource.username` and `spring.datasource.password` under `/run/secrets`, then configure:

```yaml
spring:
  config:
    import: "configtree:/run/secrets/"
```

Mount the Secret read-only and grant the workload identity only the required secret access. Environment variables are also supported, but avoid exposing secrets through diagnostics, logs, process inspection, or Actuator configuration endpoints. Do not bake environment-specific configuration or credentials into the image.

Disable automatic service-account token mounting when the application does not call the Kubernetes API. If it does, use a dedicated ServiceAccount and grant only the required RBAC permissions. Keep Services internal by default; configure ingress, TLS termination, and NetworkPolicy according to the cluster's approved platform. A NetworkPolicy only has effect when the cluster's network plugin enforces it.

## Probes, shutdown, and rollout behavior

- **Startup** protects slow initialization. Kubernetes waits for it to succeed before starting liveness or readiness checks. Set `failureThreshold × periodSeconds` above the measured worst-case startup window.
- **Liveness** should detect a stuck or irrecoverable process. It should not depend on a shared database, cache, or remote API; that can restart every replica during an upstream outage.
- **Readiness** controls whether a Pod receives Service traffic. Include only conditions that mean the instance cannot serve its contract; do not use it as a general dependency dashboard.
- **Graceful termination**: Kubernetes sends `SIGTERM` and waits up to `terminationGracePeriodSeconds` before force-killing the container. Set Spring's graceful shutdown timeout below that budget, leaving time for endpoint removal and process cleanup. Add a `preStop` delay only when the ingress/load-balancer drain behavior requires it; the hook consumes the same termination grace period.

Use readiness and rollout status to gate traffic changes. Keep database migrations backward-compatible across the old and new application versions, because reverting a Deployment does not roll back a schema change. See [CI/CD and safe releases](ci-cd-release.md).

## Validate and troubleshoot

Render and review the exact manifests before applying them. With a configured cluster, useful checks include:

```bash
kubectl apply --dry-run=server -f k8s/
kubectl diff -f k8s/
kubectl apply -f k8s/
kubectl rollout status deployment/orders-api
kubectl describe deployment/orders-api
kubectl logs deployment/orders-api
```

For a failed rollout, inspect Pod events and probe failures before increasing thresholds. Check image pull access, required ConfigMaps and Secrets, startup logs, memory termination events, CPU throttling, and whether the Service selector matches the Pod labels. Do not solve a probe failure by exposing a broader Actuator surface or disabling security without checking which endpoint and network path the kubelet uses.

## Gotchas

- Liveness includes the database; a database outage restarts all replicas and can amplify the incident.
- Startup is slow but no startup probe exists; liveness restarts the process before it can become ready.
- The management port is probed while the application port is broken; expose probe paths on the main port when that better reflects serving ability.
- Health routes are permitted for probes and also exposed through public ingress; keep the management path out of public routing and hide health details.
- A read-only root filesystem is enabled without identifying application write paths; mount only the required writable locations.
- Secret values are committed as base64 Kubernetes Secret data; use the cluster's secret-management workflow.
- Container resources are copied from an example without measuring JVM process memory or startup behavior.
- Deployment rollback is treated as database rollback; keep schema changes compatible and use a forward repair when needed.

## Related

- [Docker](docker.md) · [Observability](../observability/observability.md) · [Configuration](../configuration/configuration.md) · [CI/CD and safe releases](ci-cd-release.md) · [Security fundamentals](../security/security-fundamentals.md)

## References

- [Spring Boot: Kubernetes Probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes)
- [Kubernetes: Liveness, Readiness, and Startup Probes](https://kubernetes.io/docs/concepts/workloads/pods/probes/)
- [Kubernetes: Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [Kubernetes: Security Context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/)
- [Kubernetes: Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Kubernetes: Resource Management for Pods and Containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)
