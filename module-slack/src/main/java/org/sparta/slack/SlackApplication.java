package org.sparta.slack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "org.sparta")
@EnableFeignClients
@EnableScheduling
@ConfigurationPropertiesScan("org.sparta.slack.config.properties")
public class SlackApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlackApplication.class, args);
    }

}
