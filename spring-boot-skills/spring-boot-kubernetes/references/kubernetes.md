# Kubernetes

Deploy Spring Boot workloads to an existing Kubernetes cluster using its established Helm, GitOps,
ingress, namespace, and policy conventions. This guidance covers the application workload; it does
not provision or secure the cluster.

## Health and shutdown

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
keep them off public ingress. Review [Spring Boot observability](../../spring-boot-observability/SKILL.md) and
[Spring Boot security](../../spring-boot-security/SKILL.md) settings together.

## Deployment and Service

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
      volumes:
        - name: tmp
          emptyDir:
            sizeLimit: 256Mi
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
          volumeMounts:
            - name: tmp
              mountPath: /tmp
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
total observed shutdown duration below `terminationGracePeriodSeconds`, leaving time
for endpoint removal and process cleanup. Spring's timeout is per lifecycle phase, not an overall process deadline. Include all phases, preStop, worker drain and sidecar behavior in the budget. The `preStop` delay consumes the
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

Size temporary storage from observed usage; validate numeric image UID, volume permissions and read-only filesystem behavior. Set rollout progress and availability budgets deliberately. Review PodDisruptionBudget for voluntary evictions (not a guarantee against all disruption), and test rollback with the deployed schema. Use rolling updates only when old and new application versions can overlap
safely. Database changes must follow [Flyway migrations](../../flyway-migrations/SKILL.md) guidance
for expand-and-contract compatibility.

## Verify

Render the deployment using the project's actual Helm or GitOps tool. Confirm
image digest, service selectors, probe paths, startup budget, shutdown budget,
resource values, security context, configuration sources, and network exposure.
Run `kubectl apply --dry-run=server` against the target cluster only with approved access to the intended cluster and namespace. A rendered manifest or local schema check does not prove that the
cluster accepted or successfully rolled out the workload.

See the official Kubernetes guides for [probes](https://kubernetes.io/docs/concepts/workloads/pods/probes/),
[Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/),
[resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/),
and [security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/).
