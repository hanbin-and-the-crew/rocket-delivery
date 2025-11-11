package org.sparta.slack.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.slack.SlackApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SlackApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"domain-events"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9093",
        "spring.kafka.consumer.group-id=test-group",
        "app.eventpublisher.enabled=true"
})
@DirtiesContext
@Import(EventPublisherKafkaTest.TestEventListener.class)
class EventPublisherKafkaTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private TestEventListener testEventListener;

    @Test
    @DisplayName("로컬 이벤트가 Spring Event Listener로 전달된다")
    void publishLocal_shouldTriggerSpringEventListener() throws InterruptedException {
        TestEvent event = new TestEvent("로컬 이벤트 테스트");
        eventPublisher.publishLocal(event);

        boolean received = testEventListener.localLatch.await(3, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(testEventListener.lastLocalEvent).isNotNull();
        assertThat(testEventListener.lastLocalEvent.getMessage()).isEqualTo("로컬 이벤트 테스트");
    }

    @Test
    @DisplayName("외부 이벤트가 Kafka로 전송되고 Consumer가 수신한다")
    void publishExternal_shouldSendToKafkaAndReceive() throws InterruptedException {
        TestEvent event = new TestEvent("Kafka 이벤트 테스트");
        eventPublisher.publishExternal(event);

        boolean received = testEventListener.kafkaLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(testEventListener.lastKafkaEvent).isNotNull();
        assertThat(testEventListener.lastKafkaEvent.getMessage()).isEqualTo("Kafka 이벤트 테스트");
    }

    /**
     * 테스트용 이벤트 리스너
     */
    static class TestEventListener {

        CountDownLatch localLatch = new CountDownLatch(1);
        CountDownLatch kafkaLatch = new CountDownLatch(1);

        TestEvent lastLocalEvent;
        TestEvent lastKafkaEvent;

        @EventListener
        public void handleLocalEvent(TestEvent event) {
            this.lastLocalEvent = event;
            localLatch.countDown();
        }

        @KafkaListener(topics = "domain-events", groupId = "test-group")
        public void handleKafkaEvent(TestEvent event) {
            this.lastKafkaEvent = event;
            kafkaLatch.countDown();
        }
    }

}
