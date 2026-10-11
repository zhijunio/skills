# Spring Batch

Spring Batch 用于可靠处理大量、可重启、可分块的批量工作。短小的一次性任务不一定需要引入 Batch；先确认是否需要作业元数据、重启、分块事务、跳过 / 重试和运行监控。

## 最容易写错的四条规则

1. Job / Step 的参数应表达一次运行的身份，使重启和重复启动行为明确。
2. Reader、Processor、Writer 的职责分开；不要在 Reader 中塞业务逻辑。
3. 分块事务边界要与数据源及外部副作用一致。
4. 作业必须可观测、可恢复；不要假设进程不会在中途崩溃。

## 依赖与 Job / Step

按所用 Boot 版本添加 Batch Starter。Job 由一个或多个 Step 组成；Chunk Step 按固定大小读取、处理、写入，并以块作为事务单位。任务定义应清楚表达业务流程，复杂逻辑委托给领域服务。

## 可重启性与 Job 参数

Job 参数决定一次运行实例。区分标识运行的参数和仅作说明的参数，避免每次启动都意外创建新实例。Reader / Writer 状态需要持久化时，应提供稳定名称和 ExecutionContext 支持。重跑必须定义是从失败点恢复还是重新处理。

## Reader、Processor、Writer

- Reader 负责可分页、可重启地读取，并避免一次性载入全部数据。
- Processor 执行业务转换和校验；可过滤记录，但过滤与失败要可观测。
- Writer 批量写入并遵循事务语义。外部 API 调用需考虑限流、幂等和重试，不要把网络故障伪装成数据库事务。

## 启动作业与元数据 Schema

明确由谁启动 Job：应用启动、调度器、管理接口或外部编排器。不要在多个副本中无意重复启动。Batch 元数据表属于数据库 Schema，应由迁移管理；生产中不要让应用自动初始化表结构。

## Listener 与容错

Listener 用于审计、指标和生命周期钩子，不应承载难以追踪的主业务逻辑。配置重试和跳过时，限制异常类型与次数；记录跳过记录和原因。对外部副作用使用幂等键或 Outbox。

## 测试

覆盖 Job / Step 成功、失败、重启、跳过与重试；使用真实数据库和迁移验证元数据 Schema。测试数据应隔离，避免依赖上次运行留下的 Batch 元数据。

## 使用 Spring Boot 3.5.x 时

Boot 3.5.x 使用对应 Spring Batch 版本。Boot 4 对应 Batch 6，元数据 Schema 和观测 API 有变化；例如 Batch 6 的序列表名有调整，升级现有数据库时需要迁移。检查所用版本的初始化与 `ObservationRegistry` 配置。

## 常见问题

- 将简单定时脚本改造成不必要的 Job / Step。
- Reader 一次性加载整个数据集。
- Job 参数导致重复实例，或重启无法识别原运行。
- Processor / Writer 混入大量不相关业务逻辑。
- 多副本部署时每个实例都自动启动同一作业。
- 失败记录无限重试或静默跳过。
- 数据库事务覆盖不可回滚的外部副作用。
- 让 Batch 在生产环境自动创建元数据表，而不是由迁移管理。
- 只测试成功路径，不验证重启和失败恢复。

## 相关文档

- [事务](transactions.zh-CN.md) · [Flyway](flyway.zh-CN.md) · [消息](messaging.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [可观测性](observability.zh-CN.md)

> 中文要点版。完整 API、作业配置和故障处理示例请参阅[英文原文](spring-batch.md)。
