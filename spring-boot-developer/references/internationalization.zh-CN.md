# 国际化

国际化（i18n）处理语言和区域格式。MVC 页面、校验错误和 API 应使用一致的消息查找、Locale 解析和格式化规则。以不依赖时区的形式保存瞬时时间，在展示边界按用户的 Locale 和时区格式化。

## 消息资源

存在默认资源包时，Spring Boot 会自动配置 `MessageSource`。即使每种支持语言都有单独文件，也要包含 `messages.properties`：

```text
src/main/resources/
  messages.properties       # 必需的回退资源包
  messages_en.properties
  messages_zh_CN.properties
```

消息 key 应稳定并表达语义，例如 `task.status.done`，而不是页面位置 `label.3`。使用参数化消息，让框架负责插值；不要拼接多个翻译片段形成句子。

针对复数和语法差异提供完整的 Locale 专属消息格式；英语的单复数规则不适用于所有语言。数字、日期、时间和货币使用 Locale 感知格式化器，并显式传入货币类型，不要从 Locale 推断币种。如果 `MessageFormat` 无法表达所需复数规则，应使用规则明确的消息格式库；不要用嵌套模板条件自行实现复数选择。

```yaml
spring:
  messages:
    basename: messages
    encoding: UTF-8
    fallback-to-system-locale: false
```

除非需要自定义解析，不要替换 Boot 的 `MessageSource` Bean。`use-code-as-default-message` 可在开发时帮助发现缺失翻译，但在生产中可能掩盖问题。

## Locale 选择

选择并记录一种 Locale 策略。面向浏览器的 MVC 页面可协商 `Accept-Language`，或使用用户资料/Cookie 保存偏好；不要默默依赖服务器操作系统的 Locale。API 通常使用 `Accept-Language`，比自定义查询参数更清晰。规范化并限制支持的 Locale，明确回退行为。

若用户可选择语言，应说明用户保存的偏好和 `Accept-Language` 之间的优先级，并在 MVC、校验和错误响应中使用统一解析策略。语言切换只接受支持的 Locale tag，且不能形成开放重定向；重定向目标应校验为站内路径。

Locale 和时区是不同偏好。Locale 决定语言和区域格式，时区决定时钟显示。将时间戳存为 `Instant` 或其他 UTC 归一化瞬时值，只在展示时应用用户时区。不要按请求修改 JVM 默认时区。

## 校验、Thymeleaf 与 API 错误

Bean Validation 约束使用消息 key，并为约束本身和自定义领域校验消息提供翻译。Thymeleaf 通过 `#{...}` 和 Spring MVC 绑定错误使用 `MessageSource`；保持表单 DTO 属性名与消息 key 一致。参见[Thymeleaf](thymeleaf.zh-CN.md)和[校验](validation.zh-CN.md)。

API 的机器可读错误类型/代码应保持稳定。若要本地化 `ProblemDetail.detail`，根据请求 Locale 解析，但不要改变类型 URI 或应用错误代码。说明哪些字段会随 `Accept-Language` 改变，并保证回退行为确定。参见[错误处理](error-handling.zh-CN.md)。

## 测试

测试每个支持的 Locale 和回退 Locale，包括消息参数、校验消息、日期/数字格式和缺失 key 的行为。不要让测试继承开发者机器的 Locale；显式设置请求 Locale。在 MVC 边界测试 Locale 选择；单独测试 MessageSource 足够时无需启动整个应用。

## 常见问题

- 只添加 `messages_zh_CN.properties` 并期望 Boot 自动配置 `MessageSource`——也要提供默认 `messages.properties`。
- 依赖主机默认 Locale——明确支持列表和回退规则。
- 将 Locale 与时区视为同一设置——分别解析和测试。
- 拼接翻译片段——使用完整的参数化消息。
- 假设所有语言都采用英语单复数规则——提供符合 Locale 的完整消息格式，并测试零/单/多项。
- 仅凭 Locale 格式化金额——应显式指定货币和 Locale。
- 页面、校验和 API 错误使用不同的 Locale 解析器——统一优先级策略并保持一致。
- 接受任意语言切换重定向地址——限制支持的 Locale，并校验站内重定向目标。
- 翻译时更改稳定的 API 错误代码——只本地化面向用户的文本，保留机器可读标识符。
- 测试依赖机器 Locale——显式设置 Locale。

## 使用 Spring Boot 3.5.x 时

本文的消息资源和 Locale 解析模型相同。自定义 MVC Locale Resolver 或消息资源配置前，应检查项目实际管理的 Framework 版本。

## 相关文档

- [Thymeleaf](thymeleaf.zh-CN.md) · [校验](validation.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [配置](configuration.zh-CN.md)
