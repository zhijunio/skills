# 审计字段

审计元数据记录当前行由谁创建/最后修改以及发生时间，适合运维追踪，但不是不可变审计轨迹：`lastModified*` 会被覆盖。受监管或安全敏感的历史记录需要根据要求设计只追加事件、保留周期、访问控制和防篡改机制。

## Spring Data 审计

使用 Spring Data auditing 记录实体生命周期元数据，避免每个 Service 手动设置时间戳。集中启用一次；只有字段确实适用于整个模型时才放入精简的映射父类：

JPA auditing listener 要求 classpath 中存在 `spring-aspects`。如果所选 Boot BOM 没有传递提供该依赖，应添加不带版本号的 `org.springframework:spring-aspects`，由依赖管理选择兼容版本。

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentAuditor")
class JpaAuditingConfig {

    @Bean
    AuditorAware<String> currentAuditor() {
        var trustResolver = new AuthenticationTrustResolverImpl();
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(authentication -> !trustResolver.isAnonymous(authentication))
                .map(Authentication::getName); // 仅当 getName() 是稳定主体 ID 时使用
    }
}
```

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class AuditedEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant lastModifiedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```

只需要时间戳时，无需实现 `AuditorAware`。持久化时间使用 `Instant`；`@Version` 应单独保留：审计记录人员/时间，版本字段用于检测并发写入。参见[Spring Data JPA](spring-data-jpa.zh-CN.md)。

## 主体与后台任务

审计主体应使用不可变主体 ID，而不是展示名、邮箱或复制安全 Token。通过应用自己的映射从已认证 Principal 解析标识；只有认证机制保证 `Authentication.getName()` 稳定时才可直接使用。使用 `AuthenticationTrustResolver` 识别匿名认证，不要比较可配置的用户名字符串。

明确没有已认证请求时的行为：定时任务、迁移、消息消费者和系统维护任务可能没有 `SecurityContext`。使用显式系统主体（例如 `system:reconciliation`；或 Schema 允许时为空），不要使用过期请求身份。用户被删除或去标识化时，保留稳定审计主体，并遵循系统的数据保留/隐私策略；默认不要级联删除历史审计记录。

不要将 Servlet 请求状态带入后台任务。若任务需要保留发起用户，应在命令/事件中显式传递操作者 ID。请求与异步处理应使用相同标识符命名空间。

## 边界与隐私

客户端不得设置或覆盖审计字段。将其排除在请求 DTO 外，只由审计基础设施赋值。只保留必要的操作者标识；用户名、邮箱和 IP 地址可能属于个人数据，应遵循应用的保留和访问规则。不要把密钥或完整请求载荷放入审计元数据。

审计回调属于持久化生命周期，不替代领域事件、合规日志或历史表。当业务审计事件必须在记录更新或删除后继续保留时，应在同一事务内写入独立的只追加记录。

## 测试

尽可能使用固定时钟或可控持久化时间测试创建和更新元数据。测试已认证主体和系统主体，并确认客户端不能提供审计字段。定时任务或消息消费者还要测试无请求上下文路径。持久化行为应在连接真实数据库引擎的 JPA 切片测试中验证。

## 常见问题

- 认为没有主体提供器也能使用 `@CreatedBy`——存在操作者字段时配置 `AuditorAware`。
- 添加 `@EnableJpaAuditing` 却遗漏所需的 `spring-aspects` 依赖——检查运行时 classpath。
- 为定时任务记录 `anonymousUser`——定义显式系统主体或允许为空的策略。
- 用固定用户名字符串判断匿名认证——使用 `AuthenticationTrustResolver`，并将已认证主体映射为稳定 ID。
- 允许请求 DTO 提供 `createdBy` 或时间戳——审计字段由服务端管理。
- 将可变用户名/邮箱用作审计主体，之后又允许重命名或删除——存储稳定主体 ID，并定义保留/去标识策略。
- 将 `@LastModifiedDate` 当作审计历史——它会被覆盖；持久历史应使用只追加记录。
- 将完整载荷或凭据存入审计列——只保留必要且安全的元数据。
- 将审计字段与乐观锁混为一谈——并发检测仍使用 `@Version`。

## 使用 Spring Boot 3.5.x 时

Spring Data 审计注解和 `AuditorAware` 契约相同。在一处配置中启用 `@EnableJpaAuditing`；使用 Provider 扩展前应核对项目管理的 Spring Data 版本。

## 相关文档

- [Spring Data JPA](spring-data-jpa.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [事务](transactions.zh-CN.md)

> 中文要点版。更多实现细节请参阅[英文原文](auditing.md)。
