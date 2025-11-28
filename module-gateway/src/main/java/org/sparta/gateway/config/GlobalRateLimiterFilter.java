package org.sparta.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GlobalRateLimiterFilter implements GlobalFilter, Ordered {

    private final RedisRateLimiter redisRateLimiter;
    private final KeyResolver userKeyResolver;

    /**
     * routeId는 전역이므로 "global" 같은 고정 식별자를 사용.
     * (필요하면 라우트별로 다르게 호출 가능)
     */
    private static final String GLOBAL_ROUTE_ID = "global";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        return userKeyResolver.resolve(exchange)
                .flatMap(key -> {
                    log.debug("RateLimiter key for request: {}", key);

                    // RedisRateLimiter.isAllowed(routeId, key) 호출
                    return redisRateLimiter.isAllowed(GLOBAL_ROUTE_ID, key)
                            .flatMap(response -> {
                                // RedisRateLimiter가 반환하는 헤더들을 응답 헤더에 복사
                                response.getHeaders()
                                        .forEach((name, value) -> exchange.getResponse().getHeaders().add(name, value.toString()));

                                if (!response.isAllowed()) {
                                    log.debug("Rate limit exceeded for key: {}", key);
                                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                    return exchange.getResponse().setComplete();
                                }

                                return chain.filter(exchange);
                            });
                });
    }

    /**
     * 필터 우선순위: 필요 시 값 조정 (LOWEST_PRECEDENCE 대체)
     */
    @Override
    public int getOrder() {
        // 라우팅/인증 전/후 적용을 원하는 경우 값 변경 가능.
        // 예: AuthenticationFilter 다음에 적용하려면 적절한 우선순위 설정
        return Ordered.LOWEST_PRECEDENCE;
    }
}