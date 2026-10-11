# Spring Batch

Boot 4 附带 **Spring Batch 6**。它与 5 的 API 有显著差异，与 4 的差异更大，因此 Agent 见过的大多数示例都不适用。

## 最容易让生成代码出错的四条规则

1. **不要添加 `@EnableBatchProcessing`。**Boot 会自动配置 `JobRepository`、`JobOperator` 和事务管理器。添加该注解会**禁用**自动配置，导致这些 Bean 不再提供。
2. **默认 Job Repository 不保存状态**，只存在内存中。进程崩溃后无法重启作业，也没有审计轨迹。需要持久化 `BATCH_*` 表时添加 `spring-boot-starter-batch-jdbc`。
3. **`JobLauncher` 和 `JobExplorer` 已合并到 `JobOperator`。**注入 `JobOperator`，调用 `start(job, params)`。
4. **`chunk(500, txManager)` 是 Batch 5 的写法。**Batch 6 只传 chunk 大小，并通过单独的 `.transactionManager(…)` 设置事务管理器。`JobBuilderFactory` 和 `StepBuilderFactory` 从 Batch 5 起已移除。

## 是否应该使用 Batch？

它适用于**规模大、可重启、可审计**的批量工作。规模较小时，选择更轻量的方案：

| 需求 | 使用 |
|---|---|
| 每晚处理数百万行，崩溃后可恢复 | Spring Batch |
| 对有界数据集定期执行任务 | `@Scheduled` + 分页查询（参见 [async-and-scheduling.zh-CN.md](async-and-scheduling.zh-CN.md)） |
| 请求触发的即发即弃工作 | `@Async` |
| 需要重试的持久化单项任务 | 消息队列（参见 [messaging.zh-CN.md](messaging.zh-CN.md)） |

Batch 会引入元数据表、Job Repository 和 Chunk 模型。如果不需要断点恢复，就要为用不到的基础设施付出维护成本。

## 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch-jdbc</artifactId>
</dependency>
```

普通的 `spring-boot-starter-batch` 使用不保存状态的 Repository。对于可以从头重跑的作业，这没有问题；对于必须续跑的作业则不适用。

## Job 和 Step

```java
@Configuration
class OrderExportJobConfig {

    @Bean
    Job orderExportJob(JobRepository jobRepository, Step exportStep) {
        return new JobBuilder("orderExportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(exportCompletionListener())
                .start(exportStep)
                .build();
    }

    @Bean
    Step exportStep(JobRepository jobRepository,
                    PlatformTransactionManager transactionManager,
                    ItemReader<Order> reader,
                    ItemProcessor<Order, OrderRow> processor,
                    ItemWriter<OrderRow> writer) {

        return new StepBuilder("exportStep", jobRepository)
                .<Order, OrderRow>chunk(500)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(50)
                .build();
    }
}
```

注入 Boot 创建的 `PlatformTransactionManager`，不要自行构造。

**`chunk(500)` 同时定义提交间隔和事务边界**：读取 500 条、逐条处理、将 500 条交给 Writer、提交，然后重复。Chunk 也是重启和回滚的单位。太小会导致每几行就提交一次；太大会产生长事务，并在失败时损失更多工作。通常取数百到数千。

## 可恢复性与 Job 参数

`JobInstance` 由具有**标识作用**的 Job 参数确定。使用相同参数启动已完成的 Job 会得到：

```
JobInstanceAlreadyCompleteException: A job instance already exists and is complete
```

这是预期行为：Batch 会阻止已完成的工作再次执行。创建新运行有两种方式：

```java
// A: Job 上的 RunIdIncrementer 会在每次启动时增加 run.id
// B: 自行添加唯一的标识参数
var params = new JobParametersBuilder()
        .addString("region", "EU")                            // identifying - part of the key
        .addLocalDate("businessDate", LocalDate.now())         // identifying
        .addString("requestId", requestId, false)              // NON-identifying
        .toJobParameters();
```

`false` 表示参数不参与标识：它会被记录且可读取，但不包含在实例键中。

**失败的 Job 不同。**使用*相同*参数重新启动时，它会继续执行：跳过已完成的 Step，并从最后提交的 Chunk 恢复失败的 Step。这只有在使用 **JDBC** Repository 时有效；不保存状态的 Repository 会在 JVM 退出时丢失所有信息。

这里存在实际取舍：每次都添加唯一参数可以保证创建新实例，但会**破坏失败后续跑**。需要明确选择。以业务日期为键的夜间 Job 应将日期作为标识参数，并支持续跑。

Batch 6 中，`JobParameter` 是携带自身名称的不可变 Record，`JobParameters` 保存 `Set<JobParameter>`；上面的 Builder 写法不变。

## Reader

```java
@Bean
@StepScope
JdbcPagingItemReader<Order> orderReader(DataSource dataSource,
                                        @Value("#{jobParameters['businessDate']}") LocalDate businessDate) {

    var queryProvider = new PostgresPagingQueryProvider();
    queryProvider.setSelectClause("select id, customer_id, total_amount");
    queryProvider.setFromClause("from orders");
    queryProvider.setWhereClause("where placed_on = :businessDate");
    queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));    // unique, mandatory

    return new JdbcPagingItemReaderBuilder<Order>()
            .name("orderReader")
            .dataSource(dataSource)
            .queryProvider(queryProvider)
            .parameterValues(Map.of("businessDate", businessDate))
            .pageSize(500)                                        // match the chunk size
            .rowMapper(new OrderRowMapper())
            .build();
}
```

有三类问题会导致数据悄悄损坏：

- **读取 `jobParameters` 必须使用 `@StepScope`。**否则 Bean 会在应用上下文启动时创建，此时没有运行中的 Job，`@Value("#{jobParameters[… ]}")` 无法解析。
- **分页 Reader 必须使用唯一列进行确定性排序。**缺少排序时，数据库可能在不同页返回不同顺序，造成记录**漏处理或重复处理**。不会报错，只会产出错误结果。
- **不要按本 Job 中会被 Writer 修改的列分页。**例如 Writer 将 `status` 从 `PENDING` 改成 `DONE`，Reader 按 `WHERE status = 'PENDING'` 分页时，结果集会在游标下移动，整页记录可能被跳过。应按不可变 ID 分页，或先对数据集做快照。

线程安全方面：`JdbcCursorItemReader` **不是**线程安全的；`JdbcPagingItemReader` 和 `JpaPagingItemReader` 是线程安全的。多线程 Step 使用非线程安全 Reader 时，将其包装在 `SynchronizedItemStreamReader` 中。

JPA Reader 的持久化上下文会随 Chunk 增长；`JpaPagingItemReader` 会在每页清理上下文，但包含关联的大 Chunk 仍可能耗尽堆。纯 ETL 场景优先考虑 JDBC Reader。

## Processor

```java
@Component
class OrderProcessor implements ItemProcessor<Order, OrderRow> {

    @Override
    public OrderRow process(Order order) {
        if (order.total().isZero()) {
            return null;        // ⚠ null FILTERS the item - not written, not an error
        }
        return OrderRow.from(order);
    }
}
```

返回 `null` 会静默丢弃该条目。这是过滤功能；如果原本想表示“直接传递”，却意外返回 `null`，就很容易造成问题。

## Writer

从 Batch 5 起，Writer 接收的是 `Chunk<? extends T>`，而不是 `List`：

```java
@Override
public void write(Chunk<? extends OrderRow> chunk) {
    repository.saveAll(chunk.getItems());
}
```

使用 SQL 时，批量 JDBC Writer 会一次调用 `addBatch()`，而不是每行执行一条语句：

```java
@Bean
JdbcBatchItemWriter<OrderRow> orderWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<OrderRow>()
            .dataSource(dataSource)
            .sql("insert into order_export (id, total) values (:id, :total)")
            .beanMapped()
            .build();
}
```

**Writer 在 Chunk 事务内部运行。**不要在 Writer 中发邮件、发布消息或调用 Webhook；Chunk 回滚时副作用已经发生。应将这些操作绑定到 Job 完成时机（参见[事务](transactions.zh-CN.md)）。

## 启动

Boot 默认会在启动时运行每个 `Job` Bean。在 Web 应用中，这几乎从来不是预期行为：

```yaml
spring:
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: never
```

```java
@Component
class OrderExportScheduler {

    private final JobOperator jobOperator;
    private final Job orderExportJob;

    @Scheduled(cron = "0 0 2 * * *", zone = "Europe/London")
    void runNightly() throws JobExecutionException {
        var params = new JobParametersBuilder()
                .addLocalDate("businessDate", LocalDate.now(clock).minusDays(1))
                .toJobParameters();
        jobOperator.start(orderExportJob, params);
    }
}
```

使用 `start(Job, JobParameters)`；`start(String, Properties)` 重载已弃用并计划移除。

默认 `JobOperator` 是**同步的**：`start(…)` 会阻塞到整个 Job 完成，因此夜间 Job 会占用调度线程直至运行结束，并延迟其他所有定时任务（参见[异步与定时任务](async-and-scheduling.zh-CN.md)）。即发即弃的调用应通过异步 `TaskExecutor` 配置它。

多副本部署中，每个副本都会启动定时 Job。Batch 的实例键检查会使失败副本收到 `JobInstanceAlreadyCompleteException`，而不是重复执行；这只是保护措施，并非完整设计。若运行归属很重要，应显式认领该次运行。

## 元数据 Schema

`initialize-schema: always` 会让 Batch 在启动时执行 DDL。临时环境可以这样做，生产环境不应如此。设置为 `never`，并通过 [Flyway](flyway.zh-CN.md) 发布 Schema；标准脚本位于 `spring-batch-core` 的 `org/springframework/batch/core/schema-*.sql`。

升级现有 Boot 3 数据库时注意：Batch 6 将 `BATCH_JOB_SEQ` 重命名为 `BATCH_JOB_INSTANCE_SEQ`。需要为该变更添加迁移，否则升级后的应用无法启动。

## Listener

必须在整个 Job 成功后执行、而非每个 Chunk 后执行的工作：

```java
@Bean
JobExecutionListener exportCompletionListener() {
    return new JobExecutionListener() {
        @Override
        public void afterJob(JobExecution execution) {
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                notifier.exportReady(execution.getJobParameters());   // all chunks committed
            } else {
                alerting.jobFailed(execution.getJobInstance().getJobName(),
                        execution.getAllFailureExceptions());
            }
        }
    };
}
```

始终处理失败分支。凌晨两点失败却没有告警的批处理任务，最终会在依赖其输出的人发现问题时才被注意到。

## 容错

```java
.faultTolerant()
.skip(FlatFileParseException.class)
.skipLimit(50)
.retry(TransientDataAccessException.class)
.retryLimit(3)
```

Skip 和 Retry 不同：Skip 会丢弃条目并继续；Retry 会重新尝试。跳过格式错误的行可能合理；跳过数据库故障则会隐藏服务中断。**始终设置 Skip 上限**；无限跳过可能让作业在实际什么都没处理的情况下显示成功。使用 `SkipListener` 记录被跳过的条目，否则会造成静默数据丢失。

## 测试

`spring-batch-test` 提供 `JobLauncherTestUtils`：

```java
@SpringBootTest
@SpringBatchTest
@Testcontainers
class OrderExportJobTest {

    @Autowired JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    void exportsPlacedOrders() throws Exception {
        var execution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLocalDate("businessDate", LocalDate.of(2026, 8, 9))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(exportRepository.count()).isEqualTo(3);
    }
}
```

使用 `launchStep("exportStep", params)` 单独测试 Step。也要测试**重启**路径：让 Job 中途失败，使用相同参数重新启动，并断言它从中断处继续，而不是重新处理。这正是引入 JDBC Repository 的功能，却也是最常被漏测的部分。

## 使用 Boot 3.5.x 时

使用 Spring Batch **5**；与上面四条规则的差异正好相反：

- Job Repository 默认使用 JDBC，无需也不存在 `-batch-jdbc` Starter。
- 启动 Job 时注入 `JobLauncher`，查询时注入 `JobExplorer`；`JobOperator` 不会将二者合并。
- 正确写法是 `chunk(500, transactionManager)`。
- 序列名称为 `BATCH_JOB_SEQ`。

仍应省略 `@EnableBatchProcessing`，Reader 仍需要 `@StepScope`，Writer 仍接收 `Chunk`，Reader 规则也相同。Batch 6 还移除了 Micrometer 的全局静态 Registry，因此 Boot 4 Job 需要一个连接 `MeterRegistry` 的 `ObservationRegistry` Bean 才能输出指标（参见[可观测性](observability.zh-CN.md)）。

## 常见问题

- Agent 添加 `@EnableBatchProcessing`：它会**禁用** Boot 自动配置。
- Agent 使用 `spring-boot-starter-batch`，却期望可恢复或有审计记录：Batch 6 默认 Repository 仅保存在内存中。
- Agent 使用 `JobBuilderFactory` / `StepBuilderFactory`：它们从 Batch 5 起已移除。
- Agent 在 Batch 6 使用 `.chunk(500, txManager)`：应写为 `.chunk(500)` 后接 `.transactionManager(...)`。
- Agent 在 Boot 4 注入 `JobLauncher` 或 `JobExplorer`：它们现在由 `JobOperator` 取代。
- Agent 调用 `jobOperator.start("jobName", properties)`：该方法已弃用并计划移除；使用 `start(Job, JobParameters)`。
- Agent 的 Reader 使用 `jobParameters` 却未标 `@StepScope`：表达式在上下文启动时无法解析。
- Agent 的分页查询没有 `ORDER BY` 或排序列不唯一：页面会静默漏项或重复项。
- Agent 按 Writer 会修改的列分页：结果集移动会导致漏页。
- Agent 在多线程 Step 中使用 `JdbcCursorItemReader`：它不是线程安全的。
- Agent 误将 Processor 返回的 `null` 当作透传：`null` 会过滤掉条目。
- Agent 实现 `write(List<? extends T>)`：方法签名应为 `Chunk<? extends T>`。
- Agent 从 Writer 发邮件或发布事件：Writer 在 Chunk 事务中运行；应使用 `afterJob`。
- Agent 让 Web 应用启动时自动运行 Job：设置 `spring.batch.job.enabled=false`。
- Agent 允许 `initialize-schema: always` 在生产执行 DDL：使用 `never` 和 Flyway 迁移。
- Agent 在 Boot 4 沿用 Boot 3 Batch Schema：`BATCH_JOB_SEQ` 已重命名为 `BATCH_JOB_INSTANCE_SEQ`。
- Agent 对需要续跑的 Job 每次添加唯一参数：这会破坏可恢复性。
- Agent 不设置 Skip 上限：作业可能在“跳过”所有条目后仍显示成功。
- Agent 在 `afterJob` 中只处理成功分支：凌晨两点失败的 Job 必须告警。
- Agent 使用同步 `JobOperator` 阻塞调度线程：应配置异步 `TaskExecutor`。
- Agent 只测试成功路径：还要测试失败后重启。

## 相关文档

- [Flyway](flyway.zh-CN.md) · [事务](transactions.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [消息](messaging.zh-CN.md) · [可观测性](observability.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md)
