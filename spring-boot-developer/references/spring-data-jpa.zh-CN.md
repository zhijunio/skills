# Spring Data JPA

本文整理 JPA 实体、身份、值对象 ID、Repository、Projection、N+1、分页、批量写入和配置方面的实践。持久化映射是基础设施边界；不要让实体直接成为 API 契约。

## 实体是 class，不是 record

JPA 实体需要可变状态、无参构造器和可代理的非 final 类型，因此使用 class。请求和响应仍应使用 record。实体字段访问、构造器可见性和代理策略要符合 Hibernate 要求。

## 身份：值对象 ID 与时间有序 UUID

为实体 ID 定义独立类型（例如 `OrderId`），避免不同实体 ID 混用。新系统可采用时间有序 UUID（UUIDv7）以改善索引局部性；不要将内部自增整数直接暴露为 API ID。生成 ID 的策略应由系统统一决定，数据库和应用不能各自随意生成。

### 类型和值

ID 值对象负责有效性和转换；JSON 表示、路径变量转换分别通过 Jackson 注解和 Spring Converter 处理，详见[JSON 与 Jackson](json-and-jackson.zh-CN.md)及[REST Controller](rest-controllers.zh-CN.md)。

### UUIDv7 与备用方案

UUIDv7 按时间排序，较 UUIDv4 更适合 B-Tree 插入局部性。若目标 JDK / 数据库库尚不支持，应使用项目统一的可靠生成器，不要用时间戳拼接或自制不安全算法。

## 基类与 equals / hashCode

共享基类只放真正通用的身份、审计或版本字段，不要让基类成为所有实体状态的容器。实体相等性要避免基于可变字段；持久化前后 ID 变化也会影响 Hash 集合。使用代理时需谨慎比较类型。不要随意生成 Lombok `@Data`。

使用赋值 ID（包括 UUID `@Id` 和 `@EmbeddedId`）时，建议使用可空的 `@Version Long`：Spring Data 可据此区分新实体与已有实体，避免错误地走 `merge` 并额外查询。不要使用原始类型 `long`，它默认值 `0` 会被当作已有版本。

若 API 响应包含新版本号，应在映射 DTO 前调用 Repository 的 `flush()`。Hibernate 执行更新时才会递增托管实体的版本；flush 前内存中的值可能仍是旧版本。flush 也会让乐观锁冲突在构造响应前暴露。它不等于提交，事务之后仍可能失败。

## 关系

优先单向关系和明确的聚合边界。谨慎使用级联、`orphanRemoval` 和 EAGER 抓取；它们会让一次操作加载 / 删除意外的对象图。双向关系需要维护两侧一致性。跨聚合引用优先使用 ID。

## Repository

Repository 提供领域需要的持久化操作。简单派生查询可直接声明；复杂查询使用 `@Query`、Projection 或 Specification，并为查询行为做持久化切片测试。缺失项可以返回 `Optional`，需要必有值时提供抛出领域异常的 `getBy…` 方法。

## Projection

只需部分字段时使用 DTO / Interface Projection，避免加载完整实体图。对外响应仍映射为独立 Response record，不直接返回持久化 Projection。

## N+1

检查关联访问触发的额外 SQL。使用明确的 `@EntityGraph`、Fetch Join 或批量抓取解决；不要把所有关系改为 EAGER。通过 SQL 统计断言查询次数，并在本地关闭 Open Session in View，让边界错误尽早暴露。

## 分页

页面大小必须设上限。深分页应考虑 Keyset Pagination，并匹配排序条件和索引。排序字段要白名单验证，避免客户端按未索引属性触发全表扫描。

## 批量写入

批量写入要关注 JDBC Batch、持久化上下文内存和事务大小。分块 flush / clear 可控制内存，但要确认级联、回调和版本检查行为。批量操作可能绕过实体回调与领域行为，不能用于需要逐项不变量保护的场景。

## 配置

生产 Schema 由 Flyway / Liquibase 管理，Hibernate 使用 `ddl-auto=validate`。配置连接池、超时、事务边界和 SQL 日志；避免生产环境 `update` / `create`。详见[事务](transactions.zh-CN.md)和[Flyway](flyway.zh-CN.md)。

## 使用 Spring Boot 3.5.x 时

JPA / Repository 基本原则不变。Boot 4 采用 Hibernate 新版本和 Jakarta Persistence 新能力，实体映射、Embeddable record 及依赖模块需按版本核实。UUIDv7 API 是否可用取决于运行 JDK 与所用库。

## 常见问题

- 将实体写成 record，或直接从 Controller 返回实体。
- 用自增整数作为公开 ID，或不同实体之间混用 ID 类型。
- 让关系默认 EAGER，导致巨大对象图和 N+1。
- 暴露可变子集合，绕过聚合规则。
- 对实体使用 Lombok `@Data` 或基于可变字段的相等性。
- 所有查询都加载实体，忽略 Projection / EntityGraph。
- 分页不限制大小，排序不做白名单。
- 批量更新绕过实体行为仍被当作普通实体更新。
- 在 flush 前返回包含版本号的 DTO，导致响应携带旧版本。
- 让 Hibernate 自动更新生产 Schema。
- 用 H2 测试 PostgreSQL 专属 SQL，或不检查查询次数。

## 相关文档

- [领域建模](domain-modelling.zh-CN.md) · [事务](transactions.zh-CN.md) · [Flyway](flyway.zh-CN.md) · [持久化切片测试](testing-slices-persistence.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md)

> 中文要点版。实体 / Repository 代码示例、UUID 类型细节与完整 Hibernate 配置请参阅[英文原文](spring-data-jpa.md)。
