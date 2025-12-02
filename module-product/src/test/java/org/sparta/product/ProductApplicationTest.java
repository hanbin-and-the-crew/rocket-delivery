package org.sparta.product;

import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.config.TestExcludeConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExcludeConfig.class)
class ProductApplicationTest {

    @MockBean
    private EventPublisher eventPublisher;

    @Test
    void contextLoads() {
    }
}