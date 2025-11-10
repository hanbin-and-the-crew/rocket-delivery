package org.sparta.user;

import org.junit.jupiter.api.Test;
import org.sparta.user.infrastructure.SecurityDisabledConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = SecurityDisabledConfig.class)
@ActiveProfiles("test")
class UserApplicationTest {

    @Test
    void contextLoads() {
    }
}