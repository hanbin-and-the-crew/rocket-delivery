package org.sparta.product;

import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductApplicationTest {

    @MockBean
    private EventPublisher eventPublisher;

    @Test
    void contextLoads() {
    }
}