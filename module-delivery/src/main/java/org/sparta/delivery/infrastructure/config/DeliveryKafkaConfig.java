package org.sparta.delivery.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Delivery 모듈 Kafka Consumer 설정
 * <p>
 * 1. deliveryKafkaListenerContainerFactory: Order 이벤트 처리용 (String 역직렬화)
 * 2. deliveryPaymentKafkaListenerContainerFactory: Payment 이벤트 처리용 (GenericDomainEvent 역직렬화)
 */
@Configuration
@RequiredArgsConstructor
public class DeliveryKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    private final DeliveryKafkaErrorHandlerConfig errorHandlerConfig;

    // ===========================
    // Order 이벤트 처리용 (기존)
    // ===========================

    /**
     * Order 이벤트 전용 ConsumerFactory
     * - Value를 String으로 역직렬화
     * - OrderApprovedEvent, OrderCancelledEvent 처리
     */
    @Bean
    public ConsumerFactory<String, String> deliveryConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "delivery-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    /**
     * Order 이벤트 전용 KafkaListenerContainerFactory
     * - ErrorHandler 연결: orderErrorHandler()
     * - 재시도 설정: 5초 간격 3회 ( 에러 핸들러 새로 생성해서 사용하면서 이 부분은 우선 주석으로 제거)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> deliveryKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deliveryConsumerFactory());

        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // JsonMessageConverter 사용 (String JSON -> POJO 변환)
        RecordMessageConverter converter = new JsonMessageConverter(objectMapper);
        factory.setRecordMessageConverter(converter);

        // Kafka Error Handler 연결
        factory.setCommonErrorHandler(errorHandlerConfig.orderErrorHandler());

//        // 재시도 설정: 5초 간격, 3회 재시도
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                new FixedBackOff(5000L, 3L)
//        );
//        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // ===========================
    // Payment 이벤트 처리용 (신규)
    // ===========================

    /**
     * Payment 이벤트 전용 ConsumerFactory
     * - Value를 GenericDomainEvent로 역직렬화
     * - PaymentCanceledEvent 등 처리
     */
    @Bean
    public ConsumerFactory<String, GenericDomainEvent> deliveryPaymentConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "delivery-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // JsonDeserializer 설정
        JsonDeserializer<GenericDomainEvent> deserializer =
                new JsonDeserializer<>(GenericDomainEvent.class);

        deserializer.addTrustedPackages("*");     // 패키지 트러스트
        deserializer.setRemoveTypeHeaders(false);
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Payment 이벤트 전용 KafkaListenerContainerFactory
     * - ErrorHandler 연결: paymentErrorHandler()
     * - 재시도 설정: 5초 간격 3회 ( 에러 핸들러 새로 생성해서 사용하면서 이 부분은 우선 주석으로 제거)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GenericDomainEvent> deliveryPaymentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GenericDomainEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(deliveryPaymentConsumerFactory());

        // Kafka Error Handler 연결
        factory.setCommonErrorHandler(errorHandlerConfig.paymentErrorHandler());

//        // 재시도 설정: 5초 간격, 3회 재시도
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                new FixedBackOff(5000L, 3L)
//        );
//        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * DLT 재처리 전용 KafkaListenerContainerFactory
     * - String 메시지 처리 (원본 JSON 그대로)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltRetryKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dltConsumerFactory());

        // Manual ACK 활성화
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // DLT는 리트라이 없음 (영구 실패 시 로그만)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> dltConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-retry-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual ACK

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    /**
     * DLT 재전송 전용 KafkaTemplate (String 전용)
     */
    @Bean
    public KafkaTemplate<String, String> dltRetryKafkaTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(config);
        return new KafkaTemplate<>(producerFactory);
    }

}
