package org.sparta.slack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackOpenApiConfig {

    @Bean
    public OpenAPI slackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Slack Notification Service API")
                        .description("발송 시한 알림·일일 배송 루트 알림·메시지 관리 REST API 문서")
                        .version("v1.0.0"));
    }
}
