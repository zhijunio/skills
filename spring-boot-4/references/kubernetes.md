# Kubernetes

Use this reference for Spring Boot workloads deployed to an existing
Kubernetes cluster. Follow the cluster's established Helm, GitOps, ingress,
namespace, and policy conventions. This guide covers the application workload
and does not provision or secure the cluster.

## Prepare the application

Enable Actuator liveness and readiness probes and graceful shutdown:

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

Keep remote dependencies out of liveness. Include a dependency in readiness
only when the application cannot serve its contract without it. If Spring
Security protects Actuator, allow only the probe routes for kubelet access and
keep them off public ingress. Review [observability](observability.md) and
[security](security.md) settings together.

## Workload manifest

Use this as a starting point. Set resource requests from observed workload
needs, configure memory with the full JVM process footprint in mind, and use
an immutable image digest for release deployments.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-api
spec:
  replicas: 2
  selector:
    matchLabels:
      app.kubernetes.io/name: inventory-api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app.kubernetes.io/name: inventory-api
    spec:
      terminationGracePeriodSeconds: 40
      automountServiceAccountToken: false
      securityContext:
        runAsNonRoot: true
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: app
          image: registry.example.com/inventory-api@sha256:REPLACE_WITH_DIGEST
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
          lifecycle:
            preStop:
              sleep:
                seconds: 5
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
              drop: [ALL]
---
apiVersion: v1
kind: Service
metadata:
  name: inventory-api
spec:
  selector:
    app.kubernetes.io/name: inventory-api
  ports:
    - name: http
      port: 80
      targetPort: http
```

The startup probe budget must exceed measured worst-case startup time. Set the
graceful shutdown timeout below `terminationGracePeriodSeconds`, leaving time
for endpoint removal and process cleanup. The `preStop` delay consumes the
same termination budget, so keep it only when the ingress or load balancer
needs a drain window. Validate the installed Kubernetes version supports the
chosen probe and lifecycle fields.

## Configuration and secrets

Put non-sensitive settings in the platform's configuration mechanism. Inject
secrets from the approved secret manager. Base64-encoded Kubernetes Secret
values are not encrypted by encoding. Do not commit real credentials. Disable
service-account token mounting unless the application calls the Kubernetes
API. Keep Services internal by default, then apply the platform's ingress,
TLS, and network policy requirements.

Use rolling updates only when old and new application versions can overlap
safely. Database changes must follow [Flyway migration](flyway.md) guidance
for expand-and-contract compatibility.

## Verify

Render the deployment using the project's actual Helm or GitOps tool. Confirm
image digest, service selectors, probe paths, startup budget, shutdown budget,
resource values, security context, configuration sources, and network exposure.
Run `kubectl apply --dry-run=server` against the target cluster when access is
available. A rendered manifest or local schema check does not prove that the
cluster accepted or successfully rolled out the workload.

See the official Kubernetes guides for [probes](https://kubernetes.io/docs/concepts/workloads/pods/probes/),
[Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/),
[resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/),
and [security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/).
