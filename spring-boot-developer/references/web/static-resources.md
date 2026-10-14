# Static Resources

Spring Boot serves static files from classpath locations such as `static/` by default. For a packaged frontend, keep content-hashed assets cacheable while allowing the entry HTML to revalidate after deployment.

## Cache hashed frontend assets

Vite and similar bundlers emit filenames containing a content hash under `/assets/`. Those files can use a long cache lifetime because a changed file gets a new URL. Keep `index.html` revalidating so a deployment can publish the new asset names:

```java
@Configuration
public class WebCacheConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        registry.addResourceHandler("/index.html", "/")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());
    }
}
```

Do not apply a year-long cache policy to every static resource: it would also cache `index.html`, which can leave clients requesting old bundle filenames after a deployment. Spring Boot's `spring.web.resources.cache.cachecontrol` properties do not expose an `immutable` option; use Spring's `CacheControl` API when that directive is required.

## Gotchas

- Agent gives `index.html` the same long cache lifetime as content-hashed bundles - clients can remain pinned to old asset names.
- Agent assumes `spring.web.resources.cache.cachecontrol.immutable` is a supported property - use the `CacheControl` API for the immutable directive.
- Agent maps `/assets/**` to a directory that does not match the frontend build output - confirm where the frontend build copies its files.

## Related

- [Vue](../frontend/vue.md) · [React](../frontend/react.md) · [Next.js](../frontend/nextjs.md) · [Angular](../frontend/angular.md) · [Vanilla JavaScript](../frontend/vanilla-js.md) · [Docker](../deployment/docker.md)

## References

- [Spring Boot static content](https://docs.spring.io/spring-boot/4.1/reference/web/servlet.html#web.servlet.spring-mvc.static-content)
- [Spring Framework resource handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/static-resources.html)
