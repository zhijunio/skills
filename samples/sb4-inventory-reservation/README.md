# 库存预留服务

这是一次从空目录运行 `spring-boot-4` 技能的试用。场景是仓库为订单预留 SKU 库存。系统需要查询库存、记录预留，以及避免两笔并发请求预留同一件商品。

| 操作 | 成功 | 典型错误 |
| --- | --- | --- |
| `GET /inventory/{sku}` | `200`，返回可用数量 | 未知 SKU 返回 `404` |
| `POST /reservations` | `201`，返回预留记录和 `Location` | 数量无效返回 `400`，未知 SKU 返回 `404`，库存不足返回 `409` |
| `GET /reservations/{id}` | `200`，返回预留记录 | 未知记录返回 `404` |
| `POST /reservations/{id}/cancel` | `204`，归还库存。重复取消仍返回 `204` | 未知记录返回 `404` |

## 生成和验证

从仓库根目录运行以下命令可以生成同款基础工程。输出目录需要是新目录。

```bash
node spring-boot-4/scripts/create-project.mjs \
  --name inventory-reservation --group-id com.example \
  --output-dir /path/to/new-project --persistence jpa-postgresql
```

生成器提供 Spring Boot 4.2.0、Java 21、Maven Wrapper、MVC、JPA、Flyway 和 PostgreSQL 测试配置。库存业务和架构测试是在骨架上实现的。

用 Java 21 和 Docker，在样例目录运行 `./mvnw -B verify`。测试通过随机端口访问应用，并用 Testcontainers 启动真实的 PostgreSQL。

并发测试先在独立事务中锁住目标行，再确认两个 HTTP 请求都在 PostgreSQL 等锁。释放锁后检查响应和剩余库存。回滚测试用临时数据库约束分别模拟预留写入失败和库存归还失败，检查同一事务内的先前修改是否撤销。

## 本地试用

在样例目录给本地数据库配置开发密码，然后启动服务。

```bash
export DATABASE_PASSWORD=local-demo-only
docker compose up -d
./mvnw spring-boot:run
```

Flyway 加入两条样例库存。`PEN-100` 有 5 件，`LAST-001` 有 1 件。对新数据库重复发送以下预留请求，第一次返回 `201`，第二次返回 `409`。

```bash
curl -i http://localhost:8080/reservations \
  -H 'Content-Type: application/json' \
  --data '{"sku":"LAST-001","quantity":1}'
curl -s http://localhost:8080/inventory/LAST-001
```

从成功的响应里取预留记录 ID，然后取消两次。两次均返回 `204`，库存只回到 `1`。原有的查询接口继续返回原预留记录。

```bash
curl -i -X POST http://localhost:8080/reservations/1/cancel
curl -i -X POST http://localhost:8080/reservations/1/cancel
curl -s http://localhost:8080/inventory/LAST-001
```

## 代码结构

单应用包含 `stock` 和 `reservation` 两个功能包。`reservation` 通过 `StockService` 操作库存，两个功能各自管理自己的持久化代码。库存更新在同一条 SQL 里校验剩余数量并扣减，与保存预留记录共用一个事务。取消通过条件更新将预留标记为已取消，只有实际改变状态的请求才归还库存。两步都在同一个事务里。ArchUnit 明确列出库存模块公开的服务及其结果类型，并检查 HTTP 层和跨功能调用的依赖。

每个有效的创建请求都代表一次新的预留。取消请求按预留 ID 幂等。若调用方需要安全重试创建请求，需要另行约定创建请求的幂等键。
