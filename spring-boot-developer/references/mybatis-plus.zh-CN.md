# MyBatis-Plus

当功能需要显式 SQL、Mapper 接口，或需要渐进接入现有 MyBatis schema 时，可选用 MyBatis-Plus。它增强 MyBatis，但不会代替 JPA 式的聚合持久化，也不会免除 SQL 设计和审查。若领域行为和实体生命周期更适合 JPA，请读 [Spring Data JPA](spring-data-jpa.zh-CN.md)。

## 按功能选择一种持久化模型

- 领域状态通过实体行为和状态变更表达、查询适合 Repository 时，优先考虑 **Spring Data JPA**。
- 查询形状和 SQL 需要显式控制、schema 已有历史，或需要数据库专属 SQL / DTO 投影时，考虑 **MyBatis-Plus**。
- 同一张表或聚合尽量只有一个持久化写入方。若 JPA 与 MyBatis-Plus 必须同时访问，明确谁负责写入及读取可见性，并处理 JPA flush/clear 和一级缓存；用真实数据库验证组合行为。

## 依赖与 Boot 版本匹配

根据项目 Boot 主版本选择专用 starter，并让所有 MyBatis-Plus 模块使用同一版本。官方指南当前示例为 `3.5.17`；Boot 4 支持从 `3.5.13` 开始。

```xml
<!-- Spring Boot 4.x -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>

<!-- Spring Boot 3.5.x 使用此 artifact -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
```

除非目标版本的官方兼容指南明确要求，否则不要在 MyBatis-Plus starter 旁再单独加入 `mybatis-spring-boot-starter`、`mybatis-spring` 或 MyBatis 依赖，以免引入不匹配版本。集中管理 MyBatis-Plus 版本，升级后检查依赖树。

在应用入口扫描范围明确的 Mapper 包：

```java
@SpringBootApplication
@MapperScan("com.example.orders.persistence.mapper")
class Application {}
```

## 显式管理 SQL 和领域行为

使用 `BaseMapper<T>` 处理常规单表操作。为自定义查询命名清晰的 Mapper 方法；复杂 join、聚合、锁定语句和数据库专属 SQL 放在经过审查的 Mapper SQL 或 XML 中。通用 CRUD 方法不等于用例边界。

Mapper 接口和持久化模型放在功能自己的持久化包中。Service 协调用例、领域行为和事务；Mapper 执行持久化。将持久化模型或查询投影映射为 Response DTO，不要把 Mapper 模型、`Page` 或 Wrapper 暴露为 API 契约。

schema 变更由 Flyway 或 Liquibase 管理；不要把启动时 DDL 或代码生成作为生产迁移流程。详见 [Flyway](flyway.zh-CN.md)。

## 安全构造条件

优先使用 `LambdaQueryWrapper` / `LambdaUpdateWrapper` 引用属性，并使用 `eq`、`in`、`between` 等方法绑定值。Wrapper 能帮助绑定值，但不会自动保证原始 SQL 片段安全。

- 不要直接接受请求传来的 SQL 片段、列名或 `ORDER BY` 表达式。将公开排序 key 映射到固定白名单列。
- 将 `last`、包含原始片段的 `apply`、`inSql` 和字符串列名重载视为 SQL 构造边界。内容必须固定或经过严格白名单校验，不能拼接请求值。
- 每次操作新建 Wrapper。Wrapper 可变且非线程安全，不要缓存或跨请求共享。
- 不要接收远端传来的序列化 Wrapper；接收业务筛选条件，在服务端组装查询。

参见官方 [Wrapper 指南](https://baomidou.com/en/guides/wrapper/)和 [SQL 注入防护说明](https://baomidou.com/en/guides/security/)。

## 使用 `MetaObjectHandler` 自动填充审计字段

可使用 MyBatis-Plus 自动填充创建 / 修改时间或可信操作者 ID 等持久化字段。在实体属性上标注 `@TableField(fill = FieldFill.INSERT)` 或 `FieldFill.INSERT_UPDATE`，并注册由 Spring 管理的 `MetaObjectHandler`：

```java
@Component
class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdAt", Instant.class, Instant.now());
        strictInsertFill(metaObject, "updatedAt", Instant.class, Instant.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
    }
}
```

填充调用中的 Java 属性名和类型必须精确匹配。操作者 ID 应从可信的服务端认证上下文中解析，不能来自客户端可控的 DTO 字段。严格填充通常不会覆盖实体中已有的非空值；不要让客户端构造带有审计字段值的持久化实体。自动填充只在受支持的 MyBatis-Plus 操作中运行：自定义 SQL、直接 JDBC 和某些更新形式可能需要显式赋值。应针对选定版本和数据库测试插入及更新路径，包括仅使用 Wrapper 的更新。

## 逻辑删除

若记录需要保留在数据库中，但从普通生成操作中隐藏，可在字段上使用 `@TableLogic`，或配置全局逻辑删除属性。MyBatis-Plus 会将受支持的删除操作改为更新，并为受支持的查询和更新添加未删除条件。逻辑删除不是物理删除、数据保留策略或授权边界。

审查自定义 XML / 注解 SQL、join、报表和直接 JDBC 语句：它们可能需要显式添加逻辑删除条件；管理查询也可能有意包含已删除记录。应决定记录删除后唯一约束如何工作，以及是否支持恢复；可能需要数据库专属的部分索引，或在唯一键中包含删除标记。测试生成 SQL 和自定义 SQL，包括重复创建与恢复行为。参见官方[逻辑删除指南](https://baomidou.com/en/guides/logic-delete/)。

## 数据权限与租户隔离是不同概念

当行可见性取决于可信用户、角色或组织范围时，可使用 `DataPermissionInterceptor` 配合 `MultiDataPermissionHandler`（或所选 MyBatis-Plus 版本提供的对应处理器 API）。这与租户行插件不同：租户隔离按租户划分记录，数据权限则在租户内进一步缩小访问范围。权限条件应根据服务端解析的授权信息构造，并安全绑定值；不要将请求输入拼接到 SQL，也不要接受客户端提供的权限 SQL 片段。

应将拦截器视为纵深防御。审查自定义 SQL 和 join、通过拦截器忽略注解 / 配置排除的映射语句、批处理与后台任务，以及选定 SQL parser 支持的各种查询形状。权限规则缺失或执行失败时，不得静默放行；应定义明确的拒绝 / 允许行为并默认拒绝。使用生产数据库验证生成的 SQL，以及跨用户、角色和组织的访问场景。参见官方[数据权限插件指南](https://baomidou.com/en/plugins/data-permission/)和[多租户](multi-tenancy.zh-CN.md)。

## 使用 `BaseTypeHandler` 自定义 JDBC 映射

只有当领域值需要内置处理器无法提供的 JDBC 表示时，才编写 MyBatis `BaseTypeHandler<T>`，例如数据库专属值对象或编码。实现 `setNonNullParameter` 和全部三个 `getNullableResult` 重载（按列名、列索引和 CallableStatement 索引读取）。根据场景通过 `@MappedTypes` / `@MappedJdbcTypes`、配置、显式结果映射或 `@TableField(typeHandler = ...)` 注册处理器；用于结果映射时，需确认 Mapper 为该字段启用了所需的结果映射行为。

转换逻辑应保持确定性，并明确处理 SQL `NULL` 和 JDBC 类型差异。若内置枚举或 JSON 支持符合 schema，优先使用内置支持。使用生产数据库驱动测试参数绑定和结果读取，包括空值及历史 / 格式错误的数据；不要让处理器掩盖无效的持久化数据。

```java
@MappedTypes(Money.class)
@MappedJdbcTypes(JdbcType.DECIMAL)
final class MoneyTypeHandler extends BaseTypeHandler<Money> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Money value, JdbcType jdbcType)
            throws SQLException {
        ps.setBigDecimal(i, value.amount());
    }

    @Override
    public Money getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return moneyOrNull(rs.getBigDecimal(columnName));
    }

    @Override
    public Money getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return moneyOrNull(rs.getBigDecimal(columnIndex));
    }

    @Override
    public Money getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return moneyOrNull(cs.getBigDecimal(columnIndex));
    }

    private static Money moneyOrNull(BigDecimal amount) {
        return amount == null ? null : new Money(amount);
    }
}
```

此示例假设 `Money` 会在其他位置校验精度和币种；实际映射还必须保留领域类型所需的全部值。参见 MyBatis [类型处理器配置指南](https://mybatis.org/mybatis-3/configuration.html)和 [`BaseTypeHandler` API](https://mybatis.org/mybatis-3/apidocs/org/apache/ibatis/type/BaseTypeHandler.html)。

## 限制分页并设置拦截器顺序

`PaginationInnerInterceptor` 负责数据库分页，但请求大小和排序选项仍由应用限制。设置最大 page size，校验页码和排序白名单，并添加确定性的次级排序字段，避免翻页时记录漂移。再通过 `maxLimit` 增加一道上限。

MyBatis-Plus 3.5.9 起分页 SQL parser 被拆为独立依赖。使用分页时，添加同版本 `mybatis-plus-jsqlparser`；旧 Java 基线需按官方安装指南选择兼容的 parser artifact。

通过一个 `MybatisPlusInterceptor` bean 注册插件。按官方顺序，先执行租户和动态表名改写，再执行分页与乐观锁，最后执行 SQL 检查和全表更新 / 删除保护。只注册应用确实需要的插件。参见官方[分页指南](https://baomidou.com/en/plugins/pagination/)和[插件顺序说明](https://baomidou.com/en/plugins/)。

## 事务、并发与租户隔离

在应用 Service / 用例边界放置 `@Transactional`。使用同一个 Spring 管理的 `DataSource` 时，Mapper 调用会参与该事务。不要在事务中吞掉失败的 Mapper 操作并继续当作写入成功；参见[事务](transactions.zh-CN.md)和[数据库并发](database-concurrency.zh-CN.md)。

乐观锁需要配置 `OptimisticLockerInnerInterceptor`，版本列标注 `@Version`，并在用例中检查影响行数。更新 0 行应视为冲突，而非写入成功。自定义 SQL 要自行包含并测试等价的版本条件，不要假设插件会重写所有语句。参见官方[乐观锁插件指南](https://baomidou.com/en/plugins/optimistic-locker/)。

多租户 ID 必须来自已认证的服务端上下文。租户插件是纵深防御，不是授权：要审查自定义 SQL、排除表、join、批处理和后台任务，并用集成测试证明租户间隔离。作用域化的 `ThreadLocal` 租户上下文和认证后的过滤器是可选的上下文载体，并非必需；只有租户处理器无法直接从可信 Principal 解析租户时才使用。关于上下文生命周期、异步边界、插件绕过和测试用例，参见[多租户](multi-tenancy.zh-CN.md)及官方[租户插件指南](https://baomidou.com/plugins/tenant/)。

## 使用生产同类数据库测试

使用 Testcontainers 和生产同类数据库测试自定义 SQL、结果映射、插件、乐观锁、租户隔离和分页。MyBatis-Plus 的 `@MybatisPlusTest` 由 `mybatis-plus-boot-starter-test` 提供；测试依赖版本应与 MyBatis-Plus 对齐，并确认它兼容项目的 Boot 主版本。确保测试切片不会把 Testcontainers `DataSource` 替换成内嵌 H2。断言 SQL 行为和影响行数，不要重复测试 `BaseMapper` 自身的 CRUD。参见官方[测试指南](https://baomidou.com/en/getting-started/test/)、[持久化切片测试](testing-slices-persistence.zh-CN.md)和[Testcontainers](testcontainers.zh-CN.md)。

## Boot 3.5.x 说明

Boot 3.5.x 使用 `mybatis-plus-spring-boot3-starter`；Boot 4 使用不同的 `mybatis-plus-spring-boot4-starter`，不能互换。MyBatis-Plus 从 3.5.13 起支持 Boot 4。`mybatis-plus-boot-starter-test` 版本需与 MyBatis-Plus 对齐；升级前通过官方[安装指南](https://baomidou.com/en/getting-started/install/)确认当前兼容版本和依赖约束。

## 常见误区

- 在 Boot 3.5 或 Boot 4 中使用 Boot 2 的 `mybatis-plus-boot-starter`。
- 在 MyBatis-Plus starter 旁重复添加 MyBatis / MyBatis-Spring 依赖，造成版本冲突。
- 把 `BaseMapper` 当作领域 Repository，或从 Controller 返回持久化模型。
- 接受客户端 Wrapper、原始排序表达式或 SQL 片段。
- 假设乐观锁或租户插件无需验证就能保护自定义 SQL。
- 不限制分页大小，或允许客户端控制 `ORDER BY` 列。
- 使用 H2 测试生产 SQL，或允许测试切片将容器数据库替换为 H2。
- 未明确写入方及缓存 / flush 策略，就让 JPA 和 MyBatis-Plus 同时写同一聚合。

## 相关文档

- [Spring Data JPA](spring-data-jpa.zh-CN.md) · [事务](transactions.zh-CN.md) · [数据库并发](database-concurrency.zh-CN.md) · [多租户](multi-tenancy.zh-CN.md) · [持久化切片测试](testing-slices-persistence.zh-CN.md) · [Flyway](flyway.zh-CN.md)

