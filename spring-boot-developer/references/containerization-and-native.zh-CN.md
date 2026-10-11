# 容器化与 Native Image

## 优先使用 Buildpacks，而非 Dockerfile

Spring Boot 无需 Dockerfile 就能构建 OCI 镜像：

```bash
./mvnw spring-boot:build-image -DskipTests
./gradlew bootBuildImage
```

生成的镜像有分层、以非 root 用户运行、选择合适的 JDK、采用合理的容器 JVM 默认参数，并包含 SBOM。手写 Dockerfile 则必须正确处理这些事项，并持续维护。

只有需要合规要求指定的基础镜像、额外系统包或 Buildpacks 不支持的构建步骤时才使用 Dockerfile。其他情况下，维护成本不值得。

```xml
<configuration>
    <image>
        <name>${docker.registry}/${project.artifactId}:${project.version}</name>
        <env>
            <BP_JVM_VERSION>21</BP_JVM_VERSION>
        </env>
    </image>
</configuration>
```

## 手写 Dockerfile 时使用分层 JAR

依赖很少变化，业务代码则经常变化。分层后重新部署只需推送几百 KB，而不是整个 JAR：

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
COPY target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /app/app/dependencies/ ./
COPY --from=builder /app/app/spring-boot-loader/ ./
COPY --from=builder /app/app/snapshot-dependencies/ ./
COPY --from=builder /app/app/application/ ./

USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
```

分层顺序很重要：从最少变化到最频繁变化。反过来会失去分层的意义。

`-Djarmode=tools ... extract` 是 Boot 3.3+ 的用法，旧的 `-Djarmode=layertools` 已弃用。

**使用非 root 用户运行容器。** 默认用户是 root；容器进程以 root 身份运行会无谓扩大权限。

## 容器中的 JVM 设置

现代 JVM 可以识别容器并读取 cgroup 限制，因此**不要硬编码 `-Xmx`**。否则内存限制变化后 JVM 无法自适应，部署调整后容器可能被 OOM Kill。

```bash
JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
```

`MaxRAMPercentage` 会按内存限制自动调整。`ExitOnOutOfMemoryError` 也很重要：否则堆耗尽的 JVM 可能在降级状态中继续运行，请求失败但简单健康检查仍通过。退出后才能由编排平台重启。

为非堆内存留出空间，包括 Metaspace、线程栈、直接缓冲区和 JVM 本身。堆占限制的 75% 是合理起点。

## 优雅关闭

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s
```

在途请求会完成，而不是在写入中途被终止。超时必须**短于**编排平台的终止宽限期，否则 Pod 仍会被杀死，配置毫无作用。参见[异步与定时任务](async-and-scheduling.zh-CN.md)。

## 探针

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  server:
    port: 9090
```

存活探针地址是 `/actuator/health/liveness`，就绪探针地址是 `/actuator/health/readiness`。两者区别十分重要：数据库故障属于**就绪状态**失败；如果把它接入存活探针，故障期间每个副本都会重启，情况会更糟。完整说明见[可观测性](observability.zh-CN.md)。

使用独立管理端口，避免 Actuator 暴露在公网入口。

## 启动速度

按投入成本从低到高：

- **Class Data Sharing**——Buildpacks 可以启用，几乎零成本即可缩短启动时间。
- **延迟初始化**（`spring.main.lazy-initialization=true`）——通过推迟创建 Bean 加快启动，但错误会从启动阶段移动到首个请求。生产环境通常不应这样取舍：Bean 定义错误应在部署时失败，而不是由首位用户触发。本地开发可使用。
- **AOT 处理**——Spring Boot 的提前处理可提升 JVM 启动速度，无需完全转为原生镜像。
- **Native Image**——收益最大，成本也最高。

## GraalVM Native Image

```bash
./mvnw -Pnative native:compile
```

| 收益 | 成本 |
|---|---|
| 启动时间为数十毫秒 | 构建需要数分钟，而非数秒 |
| 内存占用大幅降低 | 未注册的运行时反射不可用 |
| 攻击面较小 | Mockito 不可用，许多测试不能在原生模式下运行 |
| | 部分库需要显式配置或完全不可用 |
| | 峰值吞吐量可能低于预热后的 JVM |

**Native 适合缩容至零、无服务器、CLI 工具和副本数非常多的场景。** 对于长期运行且流量稳定的服务，预热后的 JVM 往往性能更好，构建和调试成本也低得多。不要仅为并不需要的启动速度迁移。

Spring Boot 的 AOT 会生成大部分反射配置，Spring 库也携带可达性元数据，但第三方库可能没有。需要时自行添加 Hint：

```java
@Configuration
@ImportRuntimeHints(OrderRuntimeHints.class)
class NativeConfig {
}

class OrderRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("prompts/*.st");
        hints.reflection().registerType(LegacyPayload.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
```

### 测试 Native 构建

```bash
./mvnw -PnativeTest test
```

**Native Image 不支持 Mockito**：Mockito 需要在运行时生成类，而闭世界镜像禁止这种行为。因此 `@Mock`、`@MockitoBean` 和 `@MockitoSpyBean` 都会失败。

- 使用 `@DisabledInNativeImage` 标注受影响的测试类。
- 需要兼容 Native 的 Bean 替换时，使用返回真实对象或手写 Stub 的静态工厂 `@TestBean`。
- **Testcontainers 测试在这里最有价值。** 它们不使用 Mock，可以在原生模式运行并真正验证兼容性。这也是优先使用真实组件测试的另一理由，参见[测试策略](testing-strategy.zh-CN.md)。

Native 构建能够编译不代表运行正常。反射问题只会在实际执行对应代码路径时暴露，因此 Native 测试不能省略。

## 镜像卫生

- **固定基础镜像标签**并定期重建以获取安全补丁。镜像构建一次后运行一年，会积累这一年发布的所有 CVE。
- **绝不把秘密写进镜像。** 应在部署时注入环境变量或挂载文件（参见[配置](configuration.zh-CN.md)）。镜像层中的秘密会永久留在镜像仓库，所有能拉取镜像的人都能访问。
- **在 CI 扫描镜像。** 常见部署安全问题中，基础镜像包含已知 CVE 的概率最高。
- 保持镜像精简：使用 JRE 而非完整 JDK；依赖允许时考虑 Alpine 或 Distroless。

## 使用 Spring Boot 3.5.x 时

Buildpacks、分层 JAR、优雅关闭、探针、AOT 和 Native Image 的行为相同。`-Djarmode=tools` 要求 Boot 3.3+；更早的 3.x 使用 `-Djarmode=layertools`。

## 常见问题

- Buildpacks 已足够却仍手写 Dockerfile——Buildpacks 会处理非 root 用户、JVM 调优、分层和 SBOM。
- 把胖 JAR 放在单独一层——每次部署都要推送整个文件；应提取分层。
- 按变化频率从高到低排列层——会破坏层缓存。
- 使用已弃用的 `-Djarmode=layertools`——应使用 `-Djarmode=tools ... extract`。
- 以 root 运行容器——会无谓扩大权限。
- 硬编码 `-Xmx`——使用 `MaxRAMPercentage` 跟随容器限制。
- 未设置 `ExitOnOutOfMemoryError`——JVM 可能在持续失败时仍表现为健康。
- 关闭超时大于编排平台的宽限期——Pod 仍会被杀死。
- 将数据库连接到存活探针——数据库故障会导致所有副本重启。
- 生产中启用延迟初始化——错误会延迟到首个用户请求。
- 长期运行服务采用 Native Image——预热 JVM 通常性能更好、投入更低。
- 在 `nativeTest` 中运行基于 Mock 的测试——Mockito 无法工作。
- 发布 Native 构建却不运行 Native 测试——反射故障只在运行时出现。
- 把秘密烘焙进镜像层——秘密会永久留在仓库中。
- 固定基础镜像后从不重建——会不断累积新发布的 CVE。

## 相关文档

- [Maven](maven.zh-CN.md) · [Gradle](gradle.zh-CN.md) · [可观测性](observability.zh-CN.md) · [配置](configuration.zh-CN.md) · [测试策略](testing-strategy.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md)
