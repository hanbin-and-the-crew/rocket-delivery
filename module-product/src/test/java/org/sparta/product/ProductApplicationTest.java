package org.sparta.product;

import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.config.TestEventPublisherConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestEventPublisherConfig.class)
class ProductApplicationTest {

    @Test
    void contextLoads() {
    }
}