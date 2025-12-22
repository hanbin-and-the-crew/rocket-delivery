package org.sparta.order.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Order 전용 ConsumerFactory
     * - value를 GenericDomainEvent 로 역직렬화
     */
    @Bean
    public ConsumerFactory<String, GenericDomainEvent> orderPaymentConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service");
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
     * Order-Payment 전용 KafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GenericDomainEvent> orderPaymentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GenericDomainEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderPaymentConsumerFactory());

        return factory;
    }

    // ========== Delivery Events용 (String → POJO 변환) ==========

    /**
     * Order-Delivery 전용 ConsumerFactory
     * - Value를 String으로 역직렬화
     * - JsonMessageConverter가 String JSON → POJO 변환 처리
     */
    @Bean
    public ConsumerFactory<String, Object> orderDeliveryConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                (Deserializer<Object>) (Object) new StringDeserializer()  // Value도 String으로 역직렬화
        );
    }

    /**
     * Order-Delivery 전용 KafkaListenerContainerFactory
     * - JsonMessageConverter를 사용하여 String JSON → POJO 자동 변환
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    orderDeliveryKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderDeliveryConsumerFactory());

        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // JsonMessageConverter 사용 (String JSON → POJO 변환)
        RecordMessageConverter converter = new JsonMessageConverter(objectMapper);
        factory.setRecordMessageConverter(converter);

        return factory;
    }
}