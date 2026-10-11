# Thymeleaf

当 Spring MVC 功能需要在服务端渲染 HTML 时，可使用 Thymeleaf，例如表单、内部工具或小型浏览器应用。它与 JSON REST 接口互补，不替代接口的 DTO 和 HTTP 契约。添加 Starter 后 Spring Boot 会自动配置 Thymeleaf，并从 `src/main/resources/templates` 查找模板。

## 依赖与目录

使用 Spring Boot 管理的 Starter 版本；没有明确兼容性原因时不要单独锁定 Thymeleaf 版本。Boot 4 使用相同的依赖坐标：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

模板和静态资源放在约定目录：

```text
src/main/resources/
  templates/
    tasks/list.html
    tasks/form.html
  static/
    css/app.css
```

## MVC 边界

HTML 视图使用 `@Controller`，JSON API 使用 `@RestController`。视图 Controller 准备小型视图模型并返回逻辑模板名。领域决策和持久化仍由 Service 负责；向视图提供专用数据，不直接传实体。表单提交绑定到带校验的请求/表单 DTO，再转换为 Service 命令。

```java
@Controller
@RequestMapping("/tasks")
class TaskPageController {

    private final TaskService tasks;

    TaskPageController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("tasks", tasks.list());
        return "tasks/list";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") CreateTaskForm form,
                  BindingResult errors,
                  RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "tasks/form";
        }
        tasks.create(form.toCommand());
        redirect.addFlashAttribute("notice", "Task created");
        return "redirect:/tasks";
    }
}
```

表单绑定时，`BindingResult` 必须紧跟在经过校验的 Model Attribute 后面。输入无效时返回表单视图和错误信息；成功后使用 Post/Redirect/Get，避免重复提交；通过 Flash Attribute 在重定向后显示一次性提示。

## 模板约定

- 对不可信文本使用 `th:text`，它会进行 HTML 转义。将 `th:utext` 和未转义内联表达式视为显式信任边界；未经明确的净化策略，不要用于用户输入的 HTML。
- 使用 `th:href`、`th:src` 和 `@{...}` URL 表达式处理 context path 和 URL 参数。不要把不可信输入拼接到 URL 或 JavaScript 中。
- 表单使用 `th:object` 和 `th:field` 绑定 DTO，并通过 `th:errors` 展示字段或全局校验错误。模板字段名要与 DTO 一致。
- 可将可复用页面结构放入 Fragment；合适时将用户可见文本放入消息文件。不要在 SpEL 表达式中实现业务规则。
- 模板只负责展示：不访问数据库、不调用 Service、不负责授权决策或领域状态转换。
- 明确区分页路由和 API 路由。JSON 接口不应意外返回视图，也不要仅为共用 Controller 就把页面处理器放入 API 路由。

## 安全

Thymeleaf 不能替代 Spring Security。基于 Session 的表单应用必须保留 CSRF 防护；Spring Security 的 Thymeleaf 集成可将 CSRF Token 放入表单。检查渲染后的 HTML 和安全测试，确保不安全的表单提交会被拒绝。除了在 UI 中隐藏控件，还要在 Service 或方法安全边界执行授权；隐藏按钮不是访问控制。参见[安全基础](security-fundamentals.zh-CN.md)。

输出优先使用 `th:text`，并使用严格的 Content Security Policy 作为纵深防御。不要把 Model 值插入内联 JavaScript；使用安全的数据属性或正确编码的 JSON 接口。即使浏览器端有校验，也必须在服务端校验提交字段。

## 测试

使用 MVC 切片测试验证路由、视图名、Model 属性、表单校验、重定向和状态码。测试受保护的表单时，合法 POST 应包含 CSRF；同时验证缺少 Token 的请求会被拒绝。若功能依赖转义、Fragment 或消息解析，应增加实际渲染模板的测试；仅测试 Controller 不会执行模板渲染。测试边界选择参见[Web 切片测试](testing-slices-web.zh-CN.md)和[REST API 测试](testing-rest-api.zh-CN.md)。

## 使用 Spring Boot 3.5.x 时

这里的 MVC Controller 和 Thymeleaf 写法相同。让 Boot 依赖管理选择兼容的 Thymeleaf 版本，并根据[版本与升级](boot-versions.zh-CN.md)检查构建 Starter 名称。Boot 4 也提供专用 Thymeleaf 测试 Starter；仅在测试需要其功能时添加。

## 常见问题

- 在 `@RestController` 中返回模板名——视图渲染应使用 `@Controller`。
- 将实体作为视图模型返回——使用专用页面模型或 DTO。
- 用 `th:utext` 让文本按 HTML 显示——不可信内容可能造成 XSS；优先使用会转义的 `th:text`。
- 为让表单 POST 通过而关闭 CSRF——Cookie/Session 认证应保留 CSRF 并提交 Token。
- 只在浏览器校验表单——服务端也要校验请求 DTO。
- 有效表单 POST 后直接渲染同一页面——成功后使用 Post/Redirect/Get。
- 认为 Controller 测试会渲染模板——需要在实际调用视图层的测试中断言渲染结果。
- 只在模板中隐藏受保护操作并将其视为授权——必须在服务端边界执行授权。

## 相关文档

- [REST Controller](rest-controllers.zh-CN.md) · [校验](validation.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md) · [错误处理](error-handling.zh-CN.md)
