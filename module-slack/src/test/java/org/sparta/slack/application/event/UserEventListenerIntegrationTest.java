package org.sparta.slack.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.slack.UserDomainEvent;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.enums.UserStatus;
import org.sparta.slack.domain.repository.UserSlackViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Kafka 사용자 이벤트가 UserSlackView를 갱신하는지 검증하는 통합 테스트. */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = "user-events")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserEventListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserSlackViewRepository userSlackViewRepository;

    @Test
    @DisplayName("UserCreatedEvent를 수신하면 Slack 사용자 뷰가 저장된다")
    void onUserEvent_UserCreatedEvent_ShouldPersistUserSlackView() {
        // given
        UUID userId = UUID.randomUUID();
        UserDomainEvent event = newUserEvent("UserCreatedEvent", userId, "slack-user-1");

        // when
        kafkaTemplate.send("user-events", event.eventId().toString(), event);

        // then
        await().atMost(Duration.ofSeconds(5))
                .until(() -> userSlackViewRepository.findByUserId(userId).isPresent());

        UserSlackView view = userSlackViewRepository.findByUserId(userId).orElseThrow();
        assertThat(view.getSlackId()).isEqualTo("slack-user-1");
        assertThat(view.getUserName()).isEqualTo(event.payload().userName());
        assertThat(view.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("UserDeletedEvent를 수신하면 Slack 사용자 뷰가 삭제된다")
    void onUserEvent_UserDeletedEvent_ShouldMarkUserSlackViewDeleted() {
        // given
        UUID userId = UUID.randomUUID();
        UserDomainEvent createdEvent = newUserEvent("UserCreatedEvent", userId, "slack-user-2");
        kafkaTemplate.send("user-events", createdEvent.eventId().toString(), createdEvent);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> userSlackViewRepository.findByUserId(userId).isPresent());

        UserDomainEvent deleteEvent = newUserEvent("UserDeletedEvent", userId, "slack-user-2");

        // when
        kafkaTemplate.send("user-events", deleteEvent.eventId().toString(), deleteEvent);

        // then
        await().atMost(Duration.ofSeconds(5))
                .until(() -> userSlackViewRepository.findByUserId(userId)
                        .map(view -> view.getDeletedAt() != null)
                        .orElse(false));

        UserSlackView deletedView = userSlackViewRepository.findByUserId(userId).orElseThrow();
        assertThat(deletedView.getDeletedAt()).isNotNull();
    }

    private UserDomainEvent newUserEvent(String eventType, UUID userId, String slackId) {
        UserDomainEvent.Payload payload = new UserDomainEvent.Payload(
                userId,
                "user-" + userId.toString().substring(0, 8),
                "홍길동",
                slackId,
                UserRole.MASTER.name(),
                UserStatus.APPROVE.name(),
                UUID.randomUUID()
        );

        return new UserDomainEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                payload
        );
    }
}
