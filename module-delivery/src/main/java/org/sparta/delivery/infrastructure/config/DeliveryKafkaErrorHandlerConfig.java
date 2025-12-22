package org.sparta.delivery.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.time.Duration;

/**
 * Kafka ErrorHandler 설정
 * - 리트라이: 5초 간격 3번
 * - DLT: 리트라이 실패 시 DLT 토픽으로 이동
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeliveryKafkaErrorHandlerConfig {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * PaymentCancelledListener용 ErrorHandler
     * - DeliveryNotFoundYetException → 리트라이
     * - 기타 일시적 예외 → 리트라이
     * - 비즈니스 예외 → 바로 DLT
     */
    @Bean
    public DefaultErrorHandler paymentErrorHandler() {
        // DLT 토픽으로 이동
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.warn("Sending to DLT topic: {} -> {}, exception={}",
                            record.topic(), dltTopic, exception.getMessage());
                    return new org.apache.kafka.common.TopicPartition(dltTopic, record.partition());
                }
        );

        // 리트라이 정책: 5초 간격, 3번 시도
        FixedBackOff backoff = new FixedBackOff(Duration.ofSeconds(5).toMillis(), 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backoff);

        // 리트라이 가능한 예외들
        errorHandler.addRetryableExceptions(
                org.springframework.dao.DataAccessException.class,     // DB 연결 오류
                org.springframework.web.client.RestClientException.class, // Feign 타임아웃
                java.net.SocketTimeoutException.class,                 // 네트워크 타임아웃
                org.sparta.delivery.infrastructure.event.consumer.PaymentCancelledListener.DeliveryNotFoundYetException.class // Delivery 없음
        );

        // 리트라이 불가능한 예외들 (바로 DLT)
        errorHandler.addNotRetryableExceptions(
                org.springframework.dao.DataIntegrityViolationException.class, // 영속적 데이터 문제
                org.springframework.validation.BindException.class           // 파싱 실패
        );

        return errorHandler;
    }

    /**
     * OrderApprovedListener용 ErrorHandler
     * - 일시적 예외만 리트라이
     */
    @Bean
    public DefaultErrorHandler orderErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.warn("Sending to DLT topic: {} -> {}, exception={}",
                            record.topic(), dltTopic, exception.getMessage());
                    return new org.apache.kafka.common.TopicPartition(dltTopic, record.partition());
                }
        );

        FixedBackOff backoff = new FixedBackOff(Duration.ofSeconds(5).toMillis(), 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backoff);

        errorHandler.addRetryableExceptions(
                org.springframework.dao.DataAccessException.class,
                org.springframework.web.client.RestClientException.class,
                java.net.SocketTimeoutException.class
        );

        return errorHandler;
    }
}
