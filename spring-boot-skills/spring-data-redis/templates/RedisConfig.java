package com.example.config;

import com.example.cache.dto.OrderCacheEntry;
import com.example.cache.dto.ProductCacheEntry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(jsonSerializer()));

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "orders", defaultConfig.entryTtl(Duration.ofMinutes(15)),
            "products", defaultConfig.entryTtl(Duration.ofHours(1)),
            "users", defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        // Spring Data Redis 4 can otherwise schedule writes asynchronously with Lettuce.
        var writer = RedisCacheWriter.create(connectionFactory,
            config -> config.immediateWrites().batchStrategy(BatchStrategies.scan(1000)));
        return RedisCacheManager.builder(writer)
            .cacheDefaults(defaultConfig)
            .transactionAware()   // regular put/evict deferred until successful commit when synchronization is active
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    /**
     * Jackson 3 (tools.jackson) JSON serializer.
     * - java.time works out of the box — no JavaTimeModule to register.
     * - Default typing is OFF by default. Enable it only when a generic cache
     *   requires runtime types. Review subtype rules and use final DTOs or an
     *   exact-name validator where necessary. Prefer a
     *   type-specific serializer when the value type is stable.
     * - Never allow broad JDK/application packages or use an unrestricted
     *   polymorphic validator for data an attacker could write.
     */
    private GenericJacksonJsonRedisSerializer jsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                // Replace with reviewed final DTOs; class-based rules allow subtypes.
                .allowIfSubType(OrderCacheEntry.class)
                .allowIfSubType(ProductCacheEntry.class)
                // Add concrete non-final JDK value types only when the payload needs them.
                .allowIfSubType(BigDecimal.class)
                .build())
            .build();
    }
}
