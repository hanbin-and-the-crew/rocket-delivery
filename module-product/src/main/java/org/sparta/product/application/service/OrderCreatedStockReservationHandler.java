package org.sparta.product.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.product.StockConfirmedEvent;
import org.sparta.product.domain.entity.ProcessedEvent;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.dao.DataIntegrityViolationException; // [수정] 중복 eventId 저장 시 예외 처리
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * OrderCreated 이벤트 기반 재고 예약 처리 핸들러
 *
 * - All-or-Nothing: 라인 중 1개라도 실패하면 부분 예약/부분 성공 outbox가 남지 않게 한다.
 * - 실패 outbox는 롤백과 무관하게 반드시 남긴다(REQUIRES_NEW).
 * - 멱등성: 동일 upstreamEventId 중복 처리는 무시한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreatedStockReservationHandler {

    private static final String EVENT_TYPE = "OrderCreated";

    private final StockService stockService;
    private final ProductOutboxEventRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderCreatedFailureRecorder failureRecorder;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(UUID upstreamEventId,
                       UUID orderId,
                       String externalReservationKey,
                       List<OrderLine> lines) {


        try {
            processedEventRepository.save(ProcessedEvent.of(upstreamEventId, EVENT_TYPE));
        } catch (DataIntegrityViolationException e) {
            log.info("[OrderCreatedStockReservationHandler] duplicate ignored: eventId={}", upstreamEventId);
            return;
        }

        try {
            // 1) All-or-Nothing 예약: 하나라도 실패하면 예외를 던져 롤백시킨다.
            for (OrderLine line : lines) {
                stockService.reserveStock(line.productId(), externalReservationKey, line.quantity());
            }

            // 2) 성공 outbox: 전체 성공했을 때만 남긴다.
            for (OrderLine line : lines) {
                StockConfirmedEvent confirmed = StockConfirmedEvent.of(orderId, line.productId(), line.quantity());
                ProductOutboxEvent outbox = ProductOutboxEvent.stockConfirmed(confirmed, toJson(confirmed));
                outboxRepository.save(outbox);
            }


        } catch (BusinessException ex) {
            // 3) 실패 outbox + 처리 완료(실패) 기록은 REQUIRES_NEW로 저장해 롤백 영향을 받지 않게 한다.
            failureRecorder.recordFailureIfFirst(upstreamEventId, orderId, externalReservationKey, ex);
            throw ex;
        }
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("outbox payload serialization failed", e);
        }
    }

    public record OrderLine(UUID productId, int quantity) {
    }
}
