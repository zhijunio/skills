# Thymeleaf

Use Thymeleaf when a Spring MVC feature should render HTML on the server: forms, internal tools, or a small browser-facing application. It complements JSON REST endpoints; it does not replace their DTO and HTTP contracts. Spring Boot auto-configures Thymeleaf when its starter is present and looks for templates under `src/main/resources/templates`.

## Dependency and layout

Use the Boot-managed starter version; do not pin Thymeleaf independently without a compatibility reason. Boot 4 uses the same starter coordinate:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Keep templates and static assets in their conventional locations:

```text
src/main/resources/
  templates/
    tasks/list.html
    tasks/form.html
  static/
    css/app.css
```

## MVC boundary

Use `@Controller` for HTML views and `@RestController` for JSON APIs. A view controller prepares a small view model and returns a logical template name. Keep domain decisions and persistence in services; expose view-specific data rather than entities. When a form posts data, bind a request/form DTO with validation, then translate it to a service command.

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

For form binding, put `BindingResult` immediately after the validated model attribute. Return the form view with its errors for invalid input. On success, use Post/Redirect/Get to prevent accidental resubmission; flash attributes carry one-time feedback across the redirect.

## Template rules

- Use `th:text` for untrusted text; it HTML-escapes values. Treat `th:utext` and unescaped inlining as an explicit trust boundary. Never pass user-authored HTML to them without a deliberate sanitization policy.
- Use `th:href`, `th:src`, and `@{...}` URL expressions so context paths and URL parameters are handled correctly. Avoid concatenating untrusted input into URLs or JavaScript.
- Bind forms to form DTOs with `th:object` and `th:field`; render field and global validation messages with `th:errors`. Keep the template's field names aligned with the DTO.
- Put reusable page structure in fragments and localized user-facing text in message bundles when appropriate. Avoid moving business rules into SpEL expressions.
- Keep templates presentation-only: no persistence access, service calls, authorization decisions, or domain transitions from template expressions.
- Keep the API and page routes explicit. A JSON endpoint should not unexpectedly return a view, and a page handler should not be placed under an API route merely to share a controller.

## Security

Thymeleaf is not a substitute for Spring Security. Session-authenticated form applications must keep CSRF protection enabled; Spring Security's Thymeleaf integration can include the CSRF token in forms. Check the rendered HTML and security tests to ensure unsafe form submissions are rejected. Apply authorization in the service or method-security boundary as well as hiding controls in the UI; hiding a button is not access control. See [security-fundamentals.md](../security/security-fundamentals.md).

Prefer `th:text` for output and a restrictive Content Security Policy for defense in depth. Do not interpolate model values into inline JavaScript; use safe data attributes or a JSON endpoint with proper encoding. Validate all submitted fields on the server even when the browser also validates them.

## Testing

Use MVC slice tests to verify the route, selected view name, model attributes, form validation behavior, redirect, and status. For secured forms, include CSRF in valid POST tests and assert that a request without it is rejected. Add a rendered-template test when escaping, fragments, or message resolution are important to the feature; a controller-only test does not exercise template rendering. See [testing-slices-web.md](../testing/testing-slices-web.md) and [testing-rest-api.md](../testing/testing-rest-api.md) for the appropriate web test boundary.

## Gotchas

- Agent returns a template name from `@RestController` - use `@Controller` for view rendering.
- Agent returns an entity as the view model - expose a purpose-built page model or DTO.
- Agent uses `th:utext` to make markup render - untrusted content becomes an XSS vector; prefer escaped `th:text`.
- Agent disables CSRF to make a form POST pass - retain CSRF for cookie/session authentication and send the token.
- Agent validates a form only in the browser - validate its request DTO on the server.
- Agent handles a valid form POST by rendering the same page - use Post/Redirect/Get after success.
- Agent assumes a controller test renders the template - assert rendered output in a test that actually invokes the view layer.
- Agent hides a protected action in the template and treats it as authorization - enforce access at the server boundary.

## Related

- [rest-controllers.md](rest-controllers.md) · [validation.md](validation.md) · [security-fundamentals.md](../security/security-fundamentals.md) · [testing-slices-web.md](../testing/testing-slices-web.md) · [error-handling.md](error-handling.md)

## References

- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Boot: Template Engines](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.template-engines)
