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

/***
 * 다른 모듈과 다르게 Redis 캐시 기능은 크게 사용하지 않고
 * 연결 제한 기능을 위해 사용하기 위한 Redis Config 설정
 */
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