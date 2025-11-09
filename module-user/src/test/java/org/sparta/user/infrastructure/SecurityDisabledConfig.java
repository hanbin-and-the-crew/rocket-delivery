package org.sparta.user.infrastructure;

import org.sparta.user.infrastructure.security.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
public class SecurityDisabledConfig {

    // UserControllerTest 시 Spring Security 필터 단계에서 차단되는거 허용해주는 Config
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    JwtUtil jwtUtil() {
        return new JwtUtil("UCSaJGmqBGgioaS3yJb9CJor9lbZ19lCA4DLVovb6XheSpMhk2j/q1iRTdkooyZOhDTT3JDrkP6dEmklyL+uCA==",
                "Jvnoe8vkGXFD5Ogg2+liSJdXGkw1XP2x9KtYBTjrwdSo9YV/3K2nIoDzSn8eme/Kx6o3pAwfrFaiMOhS2SGM8g==",
                1800000,
                1209600000
        );
    }
}