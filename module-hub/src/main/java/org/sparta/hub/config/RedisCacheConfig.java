package org.sparta.hub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 허브 전용 Redis 캐시 설정
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(
        prefix = "hub.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RedisCacheConfig {

    @Bean(name = "hubRedisConnectionFactory")
    public RedisConnectionFactory hubRedisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6378);
    }

    @Bean(name = "hubCacheManager")
    public CacheManager hubCacheManager(RedisConnectionFactory hubRedisConnectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(hubRedisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
