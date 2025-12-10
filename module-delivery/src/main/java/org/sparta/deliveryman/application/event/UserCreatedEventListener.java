package org.sparta.deliveryman.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.sparta.deliveryman.presentation.dto.response.DeliveryManResponse;
import org.sparta.deliveryman.infrastructure.event.UserCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [UserCreatedEvent 수신]
 * => User 생성 시 조건에 맞으면 DeliveryMan 자동 생성
 *
 * 생성 조건:
 * - userRole == DELIVERY_MANAGER
 * - userStatus == APPROVE
 *
 * 멱등성 보장:
 * - eventId 기반 중복 이벤트 체크
 * - 동일 이벤트 재처리 시 DeliveryMan 중복 생성 방지
 * - userId 기반 중복 체크 (2차 방어)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreatedEventListener {

    private final DeliveryManService deliveryManService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "user-events",  // ✅ User 도메인의 이벤트 토픽
            groupId = "deliveryman-user-created-group"
    )
    @Transactional
    public void handleUserCreated(String message) {
        try {
            UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
            log.info("Received UserCreatedEvent: userId={}, eventId={}, role={}, status={}",
                    event.payload().userId(), event.eventId(),
                    event.payload().role(), event.payload().status());

            // 1차 방어: eventId 기반 멱등성 체크
            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.info("Event already processed, skipping: eventId={}, userId={}",
                        event.eventId(), event.payload().userId());
                return;
            }

            // 조건 체크: DELIVERY_MANAGER + APPROVE인 경우만 DeliveryMan 생성
            if (!shouldCreateDeliveryMan(event)) {
                log.info("User does not meet DeliveryMan creation criteria, skipping: userId={}, role={}, status={}",
                        event.payload().userId(), event.payload().role(), event.payload().status());

                // 조건 불만족 시에도 이벤트 처리 완료로 기록 (재처리 방지)
                processedEventRepository.save(
                        ProcessedEvent.of(event.eventId(), "USER_CREATED")
                );
                return;
            }

            //  DeliveryMan 생성
            DeliveryManResponse.Detail created = deliveryManService.create(
                    event.payload().userId(),
                    event.payload().hubId(),
                    event.payload().realName(),
                    event.payload().slackId(),
                    event.payload().role(),
                    event.payload().status()
            );

            log.info("DeliveryMan created from UserCreatedEvent: deliveryManId={}, userId={}, type={}, sequence={}",
                    created.id(),
                    created.userId(),
                    created.type(),
                    created.sequence());

            // 이벤트 처리 완료 기록 (같은 트랜잭션)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "USER_CREATED")
            );

            log.info("UserCreatedEvent processing completed successfully: userId={}, deliveryManId={}, eventId={}",
                    event.payload().userId(), created.id(), event.eventId());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse UserCreatedEvent: {}", message, e);
            // JSON 파싱 실패는 재시도해도 소용없으므로 예외를 먹고 넘어감
            // TODO: Dead Letter Queue로 전송

        } catch (org.sparta.common.error.BusinessException e) {
            // 비즈니스 예외 처리
            if ("DELIVERY_MAN_ALREADY_EXISTS".equals(e.getErrorType().toString()) ||
                    "NO_CHANGES_TO_UPDATE".equals(e.getErrorType().toString())) {
                // 이미 존재하는 경우: 멱등성 보장 (2차 방어)
                log.warn("DeliveryMan already exists for userId={}, treating as success",
                        e.getMessage());

                // 이벤트 처리 완료로 기록하고 정상 종료
                try {
                    UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
                    processedEventRepository.save(
                            ProcessedEvent.of(event.eventId(), "USER_CREATED")
                    );
                } catch (Exception ex) {
                    log.error("Failed to save ProcessedEvent after duplicate detection", ex);
                }
                return;
            }

            // 그 외 비즈니스 예외: 재시도
            log.error("Business validation failed for UserCreatedEvent: {}", message, e);
            throw new RuntimeException("DeliveryMan creation failed due to business rule", e);

        } catch (Exception e) {
            // 예기치 못한 오류: 재시도
            log.error("Unexpected error handling UserCreatedEvent: {}", message, e);
            throw new RuntimeException("DeliveryMan creation failed", e);
        }
    }

    /**
     * DeliveryMan 생성 조건 체크
     *
     * 조건:
     * - userRole == DELIVERY_MANAGER
     * - userStatus == APPROVE
     *
     * @param event UserCreatedEvent
     * @return 생성 가능 여부
     */
    private boolean shouldCreateDeliveryMan(UserCreatedEvent event) {
        String role = event.payload().role();
        String status = event.payload().status();

        boolean isDeliveryManager = role.equals("DELIVERY_MANAGER");
        boolean isApproved = status.equals("APPROVE");

        log.debug("Checking DeliveryMan creation criteria: userId={}, role={}, status={}, isDeliveryManager={}, isApproved={}",
                event.payload().userId(), role, status, isDeliveryManager, isApproved);

        return isDeliveryManager && isApproved;
    }
}
