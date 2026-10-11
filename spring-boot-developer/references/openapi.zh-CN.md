# OpenAPI

有两种方式，关键在于**谁拥有 API 契约**。

| | Code-first（springdoc） | Spec-first（生成器） |
|---|---|---|
| 真相来源 | Java 代码 | `openapi.yaml` |
| 谁生成规范 | 运行时检查 | 人工编写 |
| 漂移风险 | 不会漂移，规范从代码生成 | 可能漂移，必须由构建检查 |
| 适用情况 | 一个团队同时负责 API 两端，服务自己的客户端 | 多个团队或语言的消费者共同协商契约 |

**默认使用 Code-first。** 只有契约确实是团队间接口时，Spec-first 的成本才值得；仅供单个团队使用的 API 采用它只会增加流程负担。

## 使用 springdoc 的 Code-first

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version><!-- 选择与你的 Boot 版本线匹配的发行版 --></version>
</dependency>
```

Spring Boot BOM 不管理 springdoc，因此需要指定版本，主版本还必须与 Boot / Framework 版本代际匹配。应查阅 springdoc 的兼容矩阵，不要直接复制版本号。

无需额外配置，规范默认位于 `/v3/api-docs`，Swagger UI 位于 `/swagger-ui.html`。其他需要的信息大多是元数据：

```java
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI orderServiceApi(BuildProperties buildProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version(buildProperties.getVersion())
                        .description("Ordering and fulfilment."))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

从 `BuildProperties` 读取版本，能让发布的 API 规范与部署制品保持一致；生成方式参见[Maven](maven.zh-CN.md)。

### springdoc 能推断什么，不能推断什么

它可以读取映射、record 和 Jakarta Validation 约束，例如将 `@NotBlank` 解释为必填，将 `@Size(max = 100)` 解释为最大长度。把校验放在请求 record 上（参见[校验](validation.zh-CN.md)）还可以免费生成 API 文档。

它无法推断业务含义，只对无法从代码推导的信息加注解：

```java
@Operation(summary = "Cancel an order",
           description = "Only permitted while the order is PENDING.")
@ApiResponse(responseCode = "204", description = "Cancelled")
@ApiResponse(responseCode = "409", description = "Already shipped",
             content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@PostMapping("/{id}/cancel")
ResponseEntity<Void> cancel(@PathVariable OrderId id) { … }
```

不要给每个参数和字段都加注解。注解比代码还多的 Controller 更难读，文档也不会因此更好；springdoc 已能识别类型。

### 使用 Javadoc 生成描述

在 Code-first API 中，可以用 Javadoc 写说明，避免在 Swagger 注解中重复描述：

- Controller 方法的 Javadoc 会成为 OpenAPI 操作描述。
- 方法的 `@return` 文本会成为响应描述。
- DTO 字段的 Javadoc 会成为对应 Schema 属性的描述。

这需要在编译期和运行时都配置 Therapi。当前 springdoc 文档使用 Therapi `0.15.0`：

```xml
<properties>
    <therapi.version>0.15.0</therapi.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.github.therapi</groupId>
        <artifactId>therapi-runtime-javadoc</artifactId>
        <version>${therapi.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.github.therapi</groupId>
                        <artifactId>therapi-runtime-javadoc-scribe</artifactId>
                        <version>${therapi.version}</version>
                    </path>
                    <!-- 保留项目中已使用的其他注解处理器。 -->
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Gradle 配置：

```kotlin
dependencies {
    implementation("com.github.therapi:therapi-runtime-javadoc:0.15.0")
    annotationProcessor("com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0")
}
```

在 API 边界写对调用方有用的契约说明：

```java
/**
 * 将任务推进到下一个允许的生命周期状态。
 *
 * @return 状态变更后的任务
 */
@PutMapping("/{id}/status")
TaskResponse changeStatus(@PathVariable UUID id, @RequestBody ChangeTaskStatusRequest request) { … }
```

Javadoc 应聚焦外部可见行为，例如允许的状态流转、排序或分页默认值、单位、可空性和重要失败条件。不要重复 Java 类型或校验注解已经表达的信息。Java record 组件上的 Javadoc 支持会受编译器和处理器影响；应检查生成的 Schema。若某个属性描述必须保证出现，则显式使用 `@Schema(description = …)`。若 Javadoc 和 Swagger 注解提供了相同描述，Swagger 注解优先。

Therapi 配置容易遗漏，springdoc 可能不会报错，只是生成的规范没有 Javadoc 内容。应检查 `/v3/api-docs` 中代表性的操作、响应和 Schema 描述。现代 `springdoc-openapi-starter-*` 已包含 Javadoc 集成；不要添加旧版 `springdoc-openapi-javadoc` 模块。详见[springdoc Javadoc 官方说明](https://springdoc.org/#javadoc-support)。

错误响应应记录为 `ProblemDetail`，让消费者看到实际结构（参见[错误处理](error-handling.zh-CN.md)）。

Framework 7 的原生 API 版本解析器不一定会被 springdoc 推断为操作参数。若通过请求头传递版本，应在相关操作上显式记录该 header（例如使用 `@Parameter(name = "API-Version", in = ParameterIn.HEADER, required = false, ...)`），或通过经过测试的 OpenAPI customiser 添加。`required` 应与默认版本配置一致，并断言生成的 `/v3/api-docs` 中包含该参数。

### 在 Schema 中表示值对象

带有 `@JsonValue` 的值对象会序列化为标量，但 springdoc 仍可能把它文档化为对象。可以集中注册映射：

```java
static {
    SpringDocUtils.getConfig().replaceWithClass(OrderId.class, UUID.class);
}
```

也可以在 record 上添加 `@Schema(type = "string", format = "uuid")`。否则发布的 Schema 会与实际载荷矛盾。

### 在 CI 中锁定规范

Code-first 不会与代码漂移，但可能与消费者预期不一致。只要有人依赖 API，就应提交生成的规范并在 CI 中检查差异；springdoc Maven 插件可以在 `integration-test` 阶段将 `/v3/api-docs` 写入文件。差异检查失败会要求显式确认破坏性变更，并提醒何时需要升级版本（参见[API 版本管理](api-versioning.zh-CN.md)）。

### 生产环境

发布规范，不发布 UI：

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: false      # 生产环境
```

生产环境中的 Swagger UI 会增加不必要的攻击面，还提供了 API 交互客户端。若必须公开，应放在认证保护之后，并确认安全配置没有意外将 `/v3/api-docs` 设为 `permitAll`（参见[安全基础](security-fundamentals.zh-CN.md)）。

## 使用生成器的 Spec-first

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version><!-- 使用当前发行版 --></version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <apiPackage>com.example.app.api</apiPackage>
                <modelPackage>com.example.app.api.model</modelPackage>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <skipDefaultInterface>true</skipDefaultInterface>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useTags>true</useTags>
                    <dateLibrary>java8</dateLibrary>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- 尽管名称如此，`useSpringBoot3=true` 仍是生成 Jakarta / Spring 6+ 代码的选项。遗漏后会生成无法编译的 `javax.*` import。
- `delegatePattern=true` 会生成供你实现的接口，生成的 Controller 保持不变。
- `skipDefaultInterface=true` 可避免生成的接口带有返回 501 的默认方法，否则缺失实现也能静默编译。
- 生成到 `target/`，不要放进 `src/`；加入 `.gitignore`，交给构建生成。

生成器的 Spring 模板可能落后于 Boot 版本。采用前应确认生成代码能在项目版本下编译。

### 实现 Delegate

```java
@Service
class OrdersApiDelegateImpl implements OrdersApiDelegate {

    private final OrderService orderService;

    OrdersApiDelegateImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(CreateOrderRequest request) {
        var id = orderService.place(OrderApiMapper.toCommand(request));
        return ResponseEntity.created(URI.create("/api/orders/" + id.value()))
                .body(OrderApiMapper.toResponse(orderService.getById(id)));
    }
}
```

**生成的模型不是领域模型。** 应在此边界转换。若生成类传入 Service，重新生成规范就会波及领域层。

### 保持规范可信

只有规范是真正权威来源时，Spec-first 才成立。应执行以下约束：

- 在 CI 中校验规范（例如使用 Spectral 或生成器的 `validate` 目标），再生成代码。
- 永远不要编辑生成代码；下一次构建会覆盖它。
- 像 API 变更一样审查规范变更。

## 使用 Spring Boot 3.5.x 时

**springdoc 主版本与 Spring 代际绑定**，这是实际版本约束：为 Framework 6 构建的 springdoc 无法用于 Framework 7，反之亦然。应按 Boot 版本查看兼容矩阵；它不受 BOM 管理，构建不会自动警告。

其他内容相同。3.5.x 使用 Jackson 2 生成规范，不影响 Schema。Spec-first 使用的 OpenAPI Generator Spring 模板也有对应 Spring 代际，因此必须确认生成代码能在目标 Boot 版本下编译。

springdoc 2.x 也使用相同的 Therapi 运行时库和注解处理器；仍应选择与 Boot 版本匹配的 springdoc 主版本。

## 常见问题

- 单团队 API 也采用 Spec-first——使用机制更少、不会漂移的 springdoc Code-first。
- 锁定与 Boot 版本不兼容的 springdoc——查兼容矩阵，BOM 不管理它。
- 每个字段都标注 `@Schema`——springdoc 已能推断类型和校验约束。
- 认为不加 Therapi 运行时库和注解处理器也能使用 Javadoc——支持可能静默失效，应检查生成规范。
- 在现代 starter 中添加旧版 `springdoc-openapi-javadoc` 模块——集成已包含在 starter 中；实际需要的是 Therapi 读取库和处理器。
- 不检查记录 DTO 组件上的 Javadoc 是否进入 Schema——应验证 `/v3/api-docs`，对必须有的描述使用 `@Schema`。
- 用自定义对象文档化错误——应记录 `ProblemDetail`。
- 生产环境继续启用 Swagger UI——保留规范，关闭 UI。
- 未经考虑就将 `/v3/api-docs` 加入 `permitAll`——这会匿名公开整个 API 面。
- `@JsonValue` 值对象在文档中显示为对象——这会与实际载荷矛盾。
- 假设 springdoc 会自动发现 Framework 7 的 API 版本 header——应显式声明或使用经过测试的 OpenAPI customiser，并检查生成的规范。
- 遗漏 `useSpringBoot3=true`——会生成无法编译的 `javax.*` import。
- 遗漏 `skipDefaultInterface=true`——缺失实现仍能编译，运行时返回 501。
- 提交生成源码——生成到 `target/` 并加入 gitignore。
- 修改生成 Controller——应实现 Delegate，否则下次构建会丢失改动。
- 将生成模型传入 Service——应在 Delegate 中转换。

## 相关文档

- [REST Controller](rest-controllers.zh-CN.md) · [校验](validation.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [API 版本管理](api-versioning.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md)
