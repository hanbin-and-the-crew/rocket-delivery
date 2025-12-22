package org.sparta.order.application.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.order.application.dto.StockReservationResult;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerOpenException;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.infrastructure.client.StockClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 재고 예약 서비스
 *
 * 단일 책임: 재고 예약 로직 + Circuit Breaker + 예외 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private static final String SERVICE_NAME = "stock-service";

    private final StockClient stockClient;
    private final CircuitBreaker circuitBreaker;

    /**
     * 재고 예약
     *
     * @param productId 상품 ID
     * @param orderId 주문 ID
     * @param quantity 수량
     * @return 재고 예약 결과
     * @throws BusinessException 재고 예약 실패 시
     */
    public StockReservationResult reserve(UUID productId, String orderId, Integer quantity) {
        log.info("[재고 예약] 시작 - productId={}, orderId={}, quantity={}", productId, orderId, quantity);

        try {
            // Circuit Breaker로 감싼 재고 예약 호출
            StockClient.ApiResponse<StockClient.StockReserveResponse> response = circuitBreaker.execute(
                () -> stockClient.reserveStock(
                    new StockClient.StockReserveRequest(productId, orderId, quantity)
                ),
                SERVICE_NAME
            );

            // API 응답 검증
            if (!response.isSuccess()) {
                log.error("[재고 예약] API 실패 - productId={}, errorCode={}, message={}",
                    productId, response.errorCode(), response.message());
                throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
            }

            StockClient.StockReserveResponse data = response.data();

            // 재고 예약 상태 검증
            if (!"RESERVED".equalsIgnoreCase(data.status())) {
                log.error("[재고 예약] 상태 비정상 - status={}, reservationKey={}",
                    data.status(), data.reservationKey());
                throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
            }

            log.info("[재고 예약] 성공 - reservationId={}, quantity={}",
                data.reservationId(), data.reservedQuantity());

            return new StockReservationResult(
                data.reservationId(),
                data.reservedQuantity(),
                data.status()
            );

        } catch (CircuitBreakerOpenException e) {
            log.warn("[재고 예약] Circuit Breaker OPEN - service unavailable");
            throw new ServiceUnavailableException("재고 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[재고 예약] 통신 실패 - productId={}, quantity={}", productId, quantity, e);
            throw new BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED);
        }
    }
}
