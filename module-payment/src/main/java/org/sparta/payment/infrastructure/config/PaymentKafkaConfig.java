package org.sparta.payment.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.sparta.common.error.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PaymentKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * payment 전용 ConsumerFactory
     * - Value를 String으로 역직렬화
     */
    @Bean
    public ConsumerFactory<String, Object> paymentConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                (Deserializer<Object>) (Object) new StringDeserializer()  // Value도 String으로 역직렬화
        );
    }

    /**
     * payment 전용 KafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> paymentKafkaListenerContainerFactory(
            DefaultErrorHandler paymentKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(paymentConsumerFactory());
        factory.setCommonErrorHandler(paymentKafkaErrorHandler); // DLT 관련 설정 카프카 팩토리에 추가

        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // JsonMessageConverter 사용 (String JSON -> POJO 변환)
        RecordMessageConverter converter = new JsonMessageConverter(objectMapper);
        factory.setRecordMessageConverter(converter);

        return factory;
    }

    /**
     * DLT + Retry ErrorHandler
     */
    @Bean
    public DefaultErrorHandler paymentKafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> new TopicPartition(
                                record.topic() + ".DLT",
                                record.partition()
                        )
                );

        // 1초 간격, 3회 재시도 후 DLT 전송
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler handler =
                new DefaultErrorHandler(recoverer, backOff);

        // 비즈니스 예외는 재시도 / DLT 제외
        handler.addNotRetryableExceptions(BusinessException.class);

        return handler;
    }
}
