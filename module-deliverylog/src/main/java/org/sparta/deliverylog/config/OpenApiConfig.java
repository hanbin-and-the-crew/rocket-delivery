package org.sparta.deliverylog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("배송 경로 관리 API")
                        .description("배송 경로 생성, 조회, 관리 API")
                        .version("1.0.0"));
    }
}
