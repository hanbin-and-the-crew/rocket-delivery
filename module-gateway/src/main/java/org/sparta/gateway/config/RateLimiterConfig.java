package org.sparta.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class RateLimiterConfig {

    /** 초당 버킷에 추가되는 토큰 개수 */
    private static final int REPLENISH_RATE = 1;

    /** 버킷에 담을 수 있는 최대 토큰 개수 */
    private static final int BURST_CAPACITY = 1;

    /** API 요청 시 소비되는 토큰 개수 */
    private static final int REQUESTED_TOKENS = 1;

    /**
     * RedisRateLimiter Bean 생성
     * @return RedisRateLimiter Bean
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(REPLENISH_RATE, BURST_CAPACITY, REQUESTED_TOKENS);
    }

    /**
     * RequestRateLimiter Filter 기준 Bean 생성
     * - 사용자 ID 기준으로 토큰 키를 세팅한다 .
     * @return 사용자 ID
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();

            String userId = String.valueOf(request.getHeaders().get("X-USER-ID"));
            log.debug("request userId : {}", userId);

            // 로그인 사용자: userId 기준
            if (StringUtils.hasText(userId)) {
                return Mono.just(userId);
            }

            // 비로그인 사용자: IP 기준
            String ip = request.getRemoteAddress().getAddress().getHostAddress();
            return Mono.just("IP_" + ip);

        };
    }

}