package org.sparta.product.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.product.StockReservationFailedEvent;
import org.sparta.product.domain.entity.ProcessedEvent;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 실패 outbox를 반드시 남기기 위한 별도 트랜잭션(REQUIRES_NEW) 기록기
 *
 * - All-or-Nothing 메인 트랜잭션이 롤백되더라도 실패 outbox는 커밋되어야 한다.
 * - 같은 이벤트가 반복 소비되어도 실패 outbox가 중복 생성되지 않게 processedEvent로 차단한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreatedFailureRecorder {

    private static final String EVENT_TYPE_FAILED = "OrderCreatedFailed";

    private final ProductOutboxEventRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailureIfFirst(UUID upstreamEventId,
                                     UUID orderId,
                                     String externalReservationKey,
                                     BusinessException ex) {

        if (processedEventRepository.existsByEventId(upstreamEventId)) {
            // 이미 성공/실패로 처리 완료됨
            return;
        }

        try {
            processedEventRepository.save(ProcessedEvent.of(upstreamEventId, EVENT_TYPE_FAILED));

            StockReservationFailedEvent failed = StockReservationFailedEvent.of(
                    orderId,
                    externalReservationKey,
                    ex.getErrorType().getCode(),
                    ex.getMessage()
            );

            String payloadJson = objectMapper.writeValueAsString(failed);
            ProductOutboxEvent outbox = ProductOutboxEvent.stockReservationFailed(failed, payloadJson);
            outboxRepository.save(outbox);

            log.warn("[OrderCreatedFailureRecorder] failure outbox saved. eventId={}, orderId={}, errorCode={}",
                    upstreamEventId, orderId, ex.getErrorType().getCode());

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failure outbox payload serialization failed", e);
        }
    }
}
