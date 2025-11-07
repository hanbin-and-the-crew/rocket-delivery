package org.sparta.jpa.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "org.sparta")
@EnableJpaRepositories(
        basePackages = "org.sparta",
        enableDefaultTransactions = false
)
public class JpaConfig {
}
