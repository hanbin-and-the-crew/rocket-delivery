package org.sparta.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 인증 예외 경로
    private static final String[] WHITE_LIST = {
            "/auth/**",
            "/api/auth/**",

            // Swagger UI
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",

            // Springdoc
            "/v3/api-docs",
            "/v3/api-docs/**",

            // 각 서비스 Swagger 문서
            "/*/api-docs",
            "/*/api-docs/**",

            // Actuator
            "/actuator/**"
    };

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // 경로별 인가
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(WHITE_LIST).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        //.anyExchange().authenticated()
                        .anyExchange().permitAll() // 프로젝트 마지막쯤에 다시 모든 요청에 대해 인가 과정 적용할 예정
                )

                // JWT 인증 필터 등록(가장 중요한 부분)
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }
}