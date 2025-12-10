package org.sparta.deliveryman.application.event;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.sparta.deliveryman.infrastructure.event.UserDeletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [UserDeletedEvent 수신]
 * => User 삭제 시 DeliveryMan도 소프트 삭제
 *
 * 멱등성 보장:
 * - eventId 기반 중복 이벤트 체크
 * - 이미 삭제된 경우 무시
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedEventListener {

    private final DeliveryManService deliveryManService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "user-events",
            groupId = "deliveryman-user-deleted-group"
    )
    @Transactional
    public void handleUserDeleted(String message) {
        try {
            UserDeletedEvent event = objectMapper.readValue(message, UserDeletedEvent.class);
            log.info("Received UserDeletedEvent: userId={}, eventId={}",
                    event.userId(), event.eventId());

            // 멱등성 체크
            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.info("Event already processed, skipping: eventId={}, userId={}",
                        event.eventId(), event.userId());
                return;
            }

            // DeliveryMan 삭제 (소프트 삭제)
            deliveryManService.delete(event.userId());

            log.info("DeliveryMan deleted from UserDeletedEvent: userId={}", event.userId());

            // 이벤트 처리 완료 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "USER_DELETED")
            );

            log.info("UserDeletedEvent processing completed: userId={}, eventId={}",
                    event.userId(), event.eventId());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse UserDeletedEvent: {}", message, e);

        } catch (Exception e) {
            // delete는 멱등성이 보장되어 있음 (이미 삭제된 경우 아무것도 안함)
            log.error("Unexpected error handling UserDeletedEvent: {}", message, e);
            throw new RuntimeException("DeliveryMan deletion failed", e);
        }
    }
}
