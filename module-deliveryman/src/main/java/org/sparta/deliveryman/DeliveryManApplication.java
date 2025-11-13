package org.sparta.deliveryman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableFeignClients
@EnableJpaAuditing
public class DeliveryManApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryManApplication.class, args);
    }
}
