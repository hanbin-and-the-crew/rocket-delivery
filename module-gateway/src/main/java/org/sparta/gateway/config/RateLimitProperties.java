package org.sparta.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitProperties {

    /** 초당 채워지는 토큰 수(초당 허용 ) */
    private int replenishRate = 5;

    /** 버스트 허용 용량 */
    private int burstCapacity = 10;

    /** 요청당 소모 토큰 수 */
    private int requestedTokens = 1;
}