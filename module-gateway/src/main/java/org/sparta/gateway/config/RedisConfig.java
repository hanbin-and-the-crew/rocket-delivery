package org.sparta.gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
@EnableCaching
@ConditionalOnMissingBean(RedisConnectionFactory.class)
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6378}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * ReactiveStringRedisTemplate Bean 생성
     * - 비동기적인 Redis 연결을 위한 템플릿 생성
     * @param redisConnectionFactory redis connection
     * @return ReactiveStringRedisTemplate Bean
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new ReactiveStringRedisTemplate((ReactiveRedisConnectionFactory) redisConnectionFactory);
    }
}