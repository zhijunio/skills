# 多租户与授权隔离

多租户首先是数据隔离边界。租户标识本身不代表访问权限：应从经过认证的可信身份或服务端校验的成员关系中确定当前租户，并在每次读写时执行隔离。

## 在边界解析租户

对于 Token API，先校验 issuer 和 audience，再将可信 claim 或成员关系查询映射为内部租户 ID。对于 Session 应用，从已认证用户的有效成员关系中解析租户。路径或请求体中的 `tenantId` 只能在授权用户选择其有权访问的成员身份后使用，不能单独作为信任依据。租户上下文缺失或有歧义时应明确拒绝，不要静默回退到默认租户。

在请求边界建立租户上下文，并显式传入应用命令和查询。不要使用可变静态字段或单例 Bean 字段，也不要假设 `ThreadLocal` 会自动跨越异步、定时、响应式或执行器边界。显式传播不可变租户 ID；后台工作需重新校验操作者，并在所有完成路径清理请求级上下文。

## 在持久化层执行隔离

明确选择隔离模型：共享表加 `tenant_id`、独立 Schema 或独立数据库。共享表运维简单，但必须一致地执行隔离；更强的物理隔离会增加配置和运维成本。将租户过滤放在 Repository 方法或经过严格测试的持久化边界，不要只把它当作可选的 Controller 约定。

共享表应遵循以下规则：

- 在唯一约束、适用的外键和租户查询索引中纳入 `tenant_id`。
- 要求租户范围查询和更新，例如 `findByTenantIdAndId`；不要按客户端提供的全局对象 ID 读取后就认为安全。
- 创建记录时从可信上下文确定租户，不复制不可信的请求字段。
- 批量更新、删除、导出和原生 SQL 都要显式限定租户。
- 可以用数据库行级安全作为纵深防御，但必须在事务内设置和清理数据库租户上下文，并测试连接池复用。

跨租户对象 ID 应采用一致的 Not Found 或 Forbidden 策略，避免泄露其他租户是否拥有该记录。租户过滤不能替代对操作者动作权限的检查。参见[安全基础](security-fundamentals.zh-CN.md)和[OAuth2 资源服务器](oauth2-resource-server.zh-CN.md)。

## 在系统边界间保持隔离

租户标识应纳入缓存键、幂等键范围、审计记录、限流键、文件所有权和 Outbox 事件。除非租户集合有界且经过批准，不要把原始租户 ID 用作指标标签；日志和追踪中的租户标识也应采用与其他标识相同的隐私和访问控制。参见[缓存](caching.zh-CN.md)、[API 幂等](api-idempotency.zh-CN.md)、[审计](auditing.zh-CN.md)和[限流](rate-limiting.zh-CN.md)。

租户后台任务应显式枚举或认领租户工作，遵循同一隔离规则，并限制并发，避免单个租户耗尽其他租户的资源。不要让租户任务继承偶然触发它的请求上下文。

## 将隔离作为安全属性测试

集成测试至少创建两个租户，并验证读取、更新、删除、唯一性、分页、导出、缓存、幂等重放和异步处理不会跨租户。覆盖上下文缺失、伪造租户输入、成员权限撤销和连接池复用。依赖行级安全、约束或数据库特有行为时，使用生产数据库引擎测试。

## 使用 Spring Boot 3.5.x 时

租户隔离属于应用和数据库设计，不是 Boot 特性。使用项目 Boot 管理的 Spring Security 和持久化 API；引用其他文档中的 Boot 4 API 前先核实当前版本是否支持。

## 常见问题

- 把请求中的 `tenantId` 当成授权凭据：应从可信身份解析有效成员关系。
- Controller 做了租户路由，但 Repository、更新或原生查询仍是全局范围：在持久化边界强制租户条件。
- 假设 `ThreadLocal` 会跨执行器传递：显式传递不可变上下文并可靠清理。
- 缓存键只有实体 ID：将租户和其他可见性维度纳入键。
- 认为租户过滤可以替代角色或对象授权：两者都要检查。
- 只用单租户测试：增加跨租户否定测试，并覆盖异步和连接池路径。

## 相关文档

[security-fundamentals.zh-CN.md](security-fundamentals.zh-CN.md) · [oauth2-resource-server.zh-CN.md](oauth2-resource-server.zh-CN.md) · [spring-data-jpa.zh-CN.md](spring-data-jpa.zh-CN.md) · [caching.zh-CN.md](caching.zh-CN.md) · [auditing.zh-CN.md](auditing.zh-CN.md) · [api-idempotency.zh-CN.md](api-idempotency.zh-CN.md)
