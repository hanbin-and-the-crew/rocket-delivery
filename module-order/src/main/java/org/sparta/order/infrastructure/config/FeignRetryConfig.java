package org.sparta.order.infrastructure.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignRetryConfig {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                1000,  // period : 재시도 간격 1초
                3000,  // maxPeriod : 최대 간격 3초
                3      // maxAttempts : 최대 3회 시도
        );
    }
}
