# Actuator 管理端点安全隔离示例

独立的 Spring Boot 4.1.1、Spring Security 7、Java 25、Maven 演示项目。业务 API 与 Actuator 使用不同的 `SecurityFilterChain` 和端口；不连接数据库。

> 本项目只用于本地学习。演示用户存放在内存中，服务重启后状态清空。不要将这些配置或端口布局直接当作生产部署方案。

## 本地运行

需要 JDK 25。复制环境变量模板，替换两个密码，并确保它们不同：

```bash
cp .env.sample .env
# 编辑 .env，设置自己的 BUSINESS_PASSWORD 和 MANAGEMENT_PASSWORD
./mvnw spring-boot:run
```

应用界面：<http://localhost:8080/>。业务 API 监听 `8080`，Actuator 管理服务监听 `9090`。启动日志会显示两个端口。可以直接设置 shell 环境变量代替 `.env` 文件。不要把真实密码提交到仓库。

## 安全边界

| 边界 | 配置 | 权限 |
| --- | --- | --- |
| `GET /api/public/status` | 业务链公开演示端点 | 匿名 |
| `GET /api/orders` | 业务链 | `BUSINESS_READ` |
| `GET /actuator/health/liveness` | 管理链，显式允许 | 匿名，只返回探针状态 |
| `GET /actuator/health/readiness` | 管理链，显式允许 | 匿名，只返回探针状态 |
| 其他已暴露的 Actuator 路径 | 管理链 | `ACTUATOR_ADMIN` |

Actuator 安全链以 `@Order(1)` 匹配 `/actuator/**`，位于业务链之前。业务用户只有 `BUSINESS_READ`，管理用户只有 `ACTUATOR_ADMIN`，两种权限互不继承。管理链返回的 401/403 使用 `application/problem+json` 和 RFC 9457 ProblemDetail；业务 API 的安全拒绝也使用同一格式。

Actuator Web exposure allowlist 只包含 `health`。`metrics`、`env`、`configprops`、`mappings` 等不会发布；匿名请求会被管理安全链拒绝，持有管理 authority 的请求也无法成功访问这些未暴露端点。health 汇总路径需要管理 authority，health 详情和组件对任何用户都关闭。只有 liveness/readiness 探针可匿名读取。

## 请求示例

在当前终端导入 `.env` 中的变量，供下面的 curl 命令使用：

```bash
set -a
. ./.env
set +a
```

业务 API 的公开状态无需凭据：

```bash
curl -i http://localhost:8080/api/public/status
```

订单接口不接受匿名请求；业务账户可以访问：

```bash
curl -i http://localhost:8080/api/orders
curl -i -u "$BUSINESS_USERNAME:$BUSINESS_PASSWORD" http://localhost:8080/api/orders
```

公开的匿名探针：

```bash
curl -i http://localhost:9090/actuator/health/liveness
curl -i http://localhost:9090/actuator/health/readiness
```

health 汇总需要管理账户。用业务账户访问应返回 403 ProblemDetail：

```bash
curl -i http://localhost:9090/actuator/health
curl -i -u "$BUSINESS_USERNAME:$BUSINESS_PASSWORD" http://localhost:9090/actuator/health
curl -i -u "$MANAGEMENT_USERNAME:$MANAGEMENT_PASSWORD" http://localhost:9090/actuator/health
```

Actuator 中未发布的路径即使带管理凭据也返回 404：

```bash
curl -i -u "$MANAGEMENT_USERNAME:$MANAGEMENT_PASSWORD" http://localhost:9090/actuator/metrics
curl -i -u "$MANAGEMENT_USERNAME:$MANAGEMENT_PASSWORD" http://localhost:9090/actuator/env
```

管理账户无权访问业务订单；`GET /api/orders` 会返回 403。

## 独立管理端口与生产部署

`management.server.port` 将管理服务与业务 HTTP 服务分开，使其可以从公网 ingress、负载均衡器或防火墙路由中排除。这是网络边界之一，不能代替认证、authority 授权或网络访问控制。生产环境应通过私有网络、防火墙规则或服务网格限制管理端口来源；使用受控的身份凭证，并按需通过代理、编排平台或专用观测系统暴露健康探针。不要将本示例的内存用户或演示密码带入生产。

## 验证

```bash
./mvnw test
./mvnw compile
```

测试覆盖安全链匹配与顺序、业务和管理权限隔离、匿名探针与拒绝访问、ProblemDetail 响应及未暴露管理端点。
