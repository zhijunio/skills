package com.example.templatemaven.config;

import com.example.templatemaven.user.api.UserResponse;
import java.time.Duration;
import java.util.Set;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var userResponseSerializer = new JacksonJsonRedisSerializer<>(UserResponse.class);
        var userCache = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(userResponseSerializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(userCache)
                .initialCacheNames(Set.of("users"))
                .transactionAware()
                .build();
    }
}
