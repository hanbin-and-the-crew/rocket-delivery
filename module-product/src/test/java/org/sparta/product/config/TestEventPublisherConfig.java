package org.sparta.product.config;

import org.sparta.common.event.EventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestEventPublisherConfig {

    /**
     * 테스트용 EventPublisher
     */
    @Bean
    public EventPublisher testEventPublisher() {
        ApplicationEventPublisher applicationEventPublisher = event -> {
        };

        return new EventPublisher(applicationEventPublisher);
    }

    /**
     * ProductOutboxPublisher가 필요로 하는 KafkaTemplate<String, String> 목 빈
     * - 실제 카프카 연결 안 함컨텍스트 올릴 때 의존성 만족
     */
    @Bean
    public KafkaTemplate<String, String> testKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
