package com.example.templatemaven.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micrometer.context.ContextRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncContextPropagationTests {

    private static final ThreadLocal<String> requestId = new ThreadLocal<>();

    @Test
    void propagatesSubmittingContextAndClearsItBeforeThreadReuse() throws Exception {
        var contextRegistry = ContextRegistry.getInstance();
        contextRegistry.registerThreadLocalAccessor("template.request-id", requestId);
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.initialize();

        try {
            requestId.set("request-a");
            var propagatedRequestId = executor.submit(requestId::get).get(2, TimeUnit.SECONDS);
            requestId.remove();
            var nextTaskRequestId = executor.submit(requestId::get).get(2, TimeUnit.SECONDS);

            assertEquals("request-a", propagatedRequestId);
            assertNull(nextTaskRequestId);
        } finally {
            requestId.remove();
            contextRegistry.removeThreadLocalAccessor("template.request-id");
            executor.shutdown();
        }
    }
}
