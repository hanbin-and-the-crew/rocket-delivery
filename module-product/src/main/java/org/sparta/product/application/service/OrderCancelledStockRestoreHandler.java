package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.ProcessedEvent;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancelledStockRestoreHandler {

    private final StockService stockService;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * OrderCancelledEvent 수신 시:
     * 1) 아직 CONFIRMED 전이면 기존 cancelReservation(externalKey=orderId)로 예약 취소 -> reservedQuantity 복구
     * 2) 이미 CONFIRMED까지 진행된 케이스면 "확정 차감"을 되돌리고(재고 증가), 예약 레코드도 CANCELLED 처리
     *
     * 멱등성: processed_event로 이벤트 중복 처리 방지(Unique 충돌은 무시)
     */
    @Transactional
    public void handle(UUID eventId, UUID orderId) {
        try {
            processedEventRepository.save(ProcessedEvent.of(eventId, "OrderCancelledEvent"));
        } catch (DataIntegrityViolationException dup) {
            log.debug("[OrderCancelledStockRestoreHandler] duplicate ignored. eventId={}", eventId);
            return;
        }

        String externalReservationKey = orderId.toString();

        try {
            stockService.cancelReservation(externalReservationKey);
            log.info("[OrderCancelledStockRestoreHandler] reservation cancelled. orderId={}", orderId);
        } catch (BusinessException be) {
            if (be.getErrorType() == ProductErrorType.STOCK_RESERVATION_ALREADY_CONFIRMED) {
                stockService.restoreConfirmedReservation(externalReservationKey);
                log.info("[OrderCancelledStockRestoreHandler] confirmed stock restored. orderId={}", orderId);
                return;
            }
            throw be;
        }
    }
}
