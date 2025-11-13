package org.sparta.slack.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.slack.UserDomainEvent;
import org.sparta.slack.application.service.user.UserSlackViewService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private static final Set<String> UPSERT_EVENT_TYPES = Set.of(
            "UserCreatedEvent",
            "UserUpdatedEvent",
            "UserRoleChangedEvent",
            "UserSlackUpdatedEvent"
    );

    private static final String DELETE_EVENT_TYPE = "UserDeletedEvent";

    private final UserSlackViewService userSlackViewService;

    @KafkaListener(
            topics = "user-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserEvent(UserDomainEvent event) {
        if (event == null) {
            return;
        }

        String eventType = event.eventType();
        if (UPSERT_EVENT_TYPES.contains(eventType)) {
            log.debug("User event 수신(Upsert) - type={}, userId={}", eventType, event.payload() != null ? event.payload().userId() : null);
            userSlackViewService.sync(event);
            return;
        }

        if (DELETE_EVENT_TYPE.equals(eventType)) {
            log.debug("User event 수신(Delete) - userId={}", event.payload() != null ? event.payload().userId() : null);
            userSlackViewService.delete(event);
            return;
        }

        log.debug("지원하지 않는 User 이벤트 type={}", eventType);
    }
}
