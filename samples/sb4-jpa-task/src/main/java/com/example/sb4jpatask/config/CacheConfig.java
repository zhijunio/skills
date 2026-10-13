package com.example.sb4jpatask.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration(proxyBeanMethods = false)
@EnableCaching
class CacheConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    private static final Duration TASK_CACHE_TTL = Duration.ofSeconds(30);

    @Bean
    RedisCacheManagerBuilderCustomizer taskCacheConfiguration() {
        var validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.sb4jpatask.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .build();
        var serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(validator)
                .build();
        var cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TASK_CACHE_TTL)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "sb4-jpa-task:" + cacheName + "::")
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));

        return builder -> builder.withCacheConfiguration("tasks", cacheConfiguration);
    }

    @Bean
    CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logCacheFailure("read", cache, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logCacheFailure("write", cache, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logCacheFailure("eviction", cache, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logCacheFailure("clear", cache, exception);
            }
        };
    }

    private static void logCacheFailure(String operation, Cache cache, RuntimeException exception) {
        LOGGER.warn("Redis cache {} failed for cache '{}'; continuing without the cache.",
                operation, cache.getName(), exception);
    }
}
