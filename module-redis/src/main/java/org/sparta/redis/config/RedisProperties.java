package org.sparta.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 설정 프로퍼티
 */
@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(
        String host,
        int port
) {
    public RedisProperties {
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0) {
            port = 6379;
        }
    }
}