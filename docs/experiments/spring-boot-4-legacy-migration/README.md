# Spring Boot 4 技能对照：旧订单数据迁移

2026 年 11 月 24 日，两组代理在同一套订单服务上为已有订单新增非空的内部来源字段。两组通过相同的 PostgreSQL 升级验收。启用组自行编写的测试还通过升级后的应用读取了旧订单。

## 实验设置

从技能内置订单样例复制同一份基线。两组使用同一模型、任务和 Java 21、Spring Boot 4.2.0、PostgreSQL 18 环境，分别在独立目录实施一次。启用组可读取归档内的技能及其参考，未启用组可读取基线源码和任务。两组均未收到共同验收代码。 [完整任务](task.md) · [基线和技能快照](inputs.tar.gz)

任务要求新增非空的 `order_source`。以区分大小写的 `WEB-` 开头的订单来源为 `WEB`，其余为 `OTHER`。旧数据需回填，新数据需继续遵循该规则。现有订单 ID、订单号和 HTTP 契约继续有效。

## 共同验收结果

| 检查 | 启用技能 | 未启用技能 |
| --- | --- | --- |
| 原始 V1 迁移逐字节比较 | 通过 | 通过 |
| 五条旧订单的来源、ID 和 GET 响应 | 通过 | 通过 |
| 新订单的来源、POST 响应、Location 和 GET 读回 | 通过 | 通过 |
| 与旧订单号重复返回 409，非法请求和缺失记录保留原行为 | 通过 | 通过 |
| PostgreSQL 拒绝显式写入 NULL，返回 SQLSTATE 23502 | 通过 | 通过 |
| 各组自行运行 Maven verify | 17 项通过 | 12 项通过 |
| 加入共同验收后运行 Maven verify | 21 项通过 | 16 项通过 |

验收脚本逐字节比较候选与原始迁移文件，然后用原始 V1 在 PostgreSQL 上建库。它在升级前插入五条订单，覆盖正常前缀、小写、前缀出现在中间等情况。之后由应用启动时的 Flyway 执行升级。测试通过完整应用上下文的 MockMvc 调用已有 POST 和 GET，并用 JDBC 检查内部来源和约束。旧版基线的原有测试通过 7 项。加入共同验收后，旧版在 3 项需要新列的测试中按预期失败。 [共同验收代码](LegacyOrderMigrationIT.java) · [验收脚本](verify.sh)

## 两组自行发现并验证的差异

两组都保留 V1，在 V2 中按「新增列、回填、设为非空」的顺序迁移。启用组用 `left(order_number, 4)` 比较前缀，未启用组用 `LIKE 'WEB-%'`。两种 SQL 都使用显式的 `C` 排序规则，Java 实体实现逐字节相同。两组都保留了 Controller 和响应 DTO。 [启用组改动](enabled.patch) · [未启用组改动](disabled.patch)

- **启用组**在 Spring Boot 执行迁移之前，用 Flyway V1 建库并写入五条旧订单。启动后通过 HTTP GET 和 JDBC 验证旧数据和响应，随后检查新订单、重复旧订单号以及非空约束。11 项集成测试中，9 项来自参数化的旧数据与新订单用例。
- **未启用组**用独立 PostgreSQL 容器验证 Flyway V1 到 V2、旧 ID 和订单号、回填边界及非空约束，再用另一套新库跑完整 HTTP 测试。它自行区分了迁移测试和接口测试，也覆盖了空后缀、小写和前缀出现在中间等边界。共同验收补上了升级后的 HTTP GET 和重复旧订单号请求。

这次观察到的主要差异是自行编写的测试如何连接升级与 HTTP 契约。两组的业务结果相同。每组只运行一次，且参数化方式不同，测试数量用于复核运行结果。验证范围是从旧版数据到新版应用的单次升级。新旧应用同时写库的发布方式需要单独验证。

## 复跑归档结果

准备 Java 21、可访问的 Docker 端点、Maven 依赖和 PostgreSQL 18 Alpine 容器镜像。在本报告目录执行：

```sh
work=$(mktemp -d)
tar -xzf inputs.tar.gz -C "$work"

for variant in enabled disabled; do
	cp -R "$work/base" "$work/$variant"
	patch -s -d "$work/$variant" -p1 < "$variant.patch"
	sh verify.sh "$work/base" "$work/$variant"
done
```

要重新实施对照，从归档的 `base/` 创建两份相同的独立副本，分别向两组提供 [任务原文](task.md)。只给启用组归档目录下的 `skill/SKILL.md` 与其引用文件。两组独立完成后，再运行共同验收。
