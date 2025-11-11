package org.sparta.slack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "org.sparta")
@EnableFeignClients
public class SlackApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlackApplication.class, args);
    }

}
