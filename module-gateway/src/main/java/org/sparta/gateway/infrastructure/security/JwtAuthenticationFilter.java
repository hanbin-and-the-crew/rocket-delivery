package org.sparta.gateway.infrastructure.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    private final ServerSecurityContextRepository securityContextRepository =
            new WebSessionServerSecurityContextRepository();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String token = resolveToken(exchange);

        if (token == null) {
            return chain.filter(exchange); // 토큰 없음 → 통과
        }

        if (!jwtUtil.validateAccessToken(token)) {
            log.warn("Invalid JWT Token");
            return chain.filter(exchange);
        }

        Claims claims = jwtUtil.getAccessTokenUserInfo(token);
        String userId = claims.getSubject();
        String role = (String) claims.get("auth");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, jwtUtil.convertToAuthorities(role));

        SecurityContextImpl context = new SecurityContextImpl(auth);

        // SecurityContext 저장
        return securityContextRepository.save(exchange, context)
                .then(chain.filter(exchange));
    }

    private String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}