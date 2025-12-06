package org.sparta.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(
        scanBasePackages = {
                "org.sparta.common",
                "org.sparta.jpa",
                "org.sparta.kafka",
                "org.sparta.delivery"
        }
)

@EnableFeignClients(basePackages = "org.sparta")
@EnableKafka
@EnableJpaAuditing
public class DeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }
}
