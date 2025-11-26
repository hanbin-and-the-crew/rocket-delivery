package org.sparta.gateway.config;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RateLimiterConfig {

    private final RateLimitProperties props;

    /** RedisRateLimiter를 properties 기반으로 생성 */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
                props.getReplenishRate(),
                props.getBurstCapacity(),
                props.getRequestedTokens()
        );
    }

    /**
     * KeyResolver: 로그인 사용자면 X-USER-ID 기준,
     * 비로그인 사용자는 IP 기준으로 키를 생성한다.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();

            // 헤더에서 첫 번째 값 가져오기
            String userId = request.getHeaders().getFirst("X-USER-ID");
            log.debug("request header X-USER-ID : {}", userId);

            if (StringUtils.hasText(userId)) {
                return Mono.just("USER_" + userId);
            }

            // RemoteAddress가 null일 가능성 대비 안전하게 처리
            String ip = "UNKNOWN";
            if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
                ip = request.getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just("IP_" + ip);
        };
    }
}