package org.sparta.order.application.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.order.application.dto.PointReservationResult;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerOpenException;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.infrastructure.client.PointClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 포인트 예약 서비스
 *
 * 단일 책임: 포인트 예약 로직 + Circuit Breaker + 예외 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointReservationService {

    private static final String SERVICE_NAME = "point-service";

    private final PointClient pointClient;
    private final CircuitBreaker circuitBreaker;

    /**
     * 포인트 예약 (선택적)
     *
     * @param customerId 고객 ID
     * @param orderId 주문 ID
     * @param totalPrice 주문 총액
     * @param requestPoint 사용 요청 포인트
     * @return 포인트 예약 결과 (사용 안 할 경우 null)
     * @throws BusinessException 포인트 예약 실패 시
     */
    public PointReservationResult reserve(UUID customerId, UUID orderId, Long totalPrice, Long requestPoint) {
        // 포인트 미사용 시
        if (requestPoint == null || requestPoint <= 0) {
            log.info("[포인트] 미사용 - requestPoint={}", requestPoint);
            return null;
        }

        log.info("[포인트] 예약 시작 - customerId={}, orderId={}, requestPoint={}",
            customerId, orderId, requestPoint);

        try {
            // Circuit Breaker로 감싼 포인트 예약 호출
            PointClient.ApiResponse<PointClient.PointResponse.PointReservationResult> apiResponse =
                circuitBreaker.execute(
                    () -> pointClient.reservePoint(
                        new PointClient.PointRequest.Reserve(customerId, orderId, totalPrice, requestPoint)
                    ),
                    SERVICE_NAME
                );

            log.info("[포인트] API 응답 - result={}, errorCode={}, message={}",
                apiResponse.result(), apiResponse.errorCode(), apiResponse.message());

            // API 응답 검증
            if (!apiResponse.isSuccess()) {
                log.error("[포인트] API 호출 실패 - errorCode={}, message={}",
                    apiResponse.errorCode(), apiResponse.message());
                throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
            }

            PointClient.PointResponse.PointReservationResult pointData = apiResponse.data();

            if (pointData == null) {
                log.error("[포인트] 응답 데이터가 null");
                throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
            }

            // 사용된 포인트 금액
            Long usedAmount = pointData.discountAmount() != null ? pointData.discountAmount() : 0L;

            // 첫 번째 예약 ID 추출
            String reservationId = null;
            if (pointData.reservations() != null && !pointData.reservations().isEmpty()) {
                PointClient.PointResponse.PointReservation firstReservation = pointData.reservations().get(0);
                reservationId = firstReservation.id() != null ? firstReservation.id().toString() : null;

                log.info("[포인트] 예약 상세 - reservationId={}, pointId={}, reservedAmount={}, status={}",
                    firstReservation.id(), firstReservation.pointId(),
                    firstReservation.reservedAmount(), firstReservation.status());
            } else {
                log.warn("[포인트] 예약 목록이 비어있음");
            }

            log.info("[포인트] 예약 완료 - usedAmount={}, reservationId={}", usedAmount, reservationId);

            if (usedAmount == 0L) {
                log.warn("[포인트] 사용된 포인트가 0원 - requestPoint={}", requestPoint);
            }

            return new PointReservationResult(reservationId, usedAmount, "RESERVED");

        } catch (CircuitBreakerOpenException e) {
            log.warn("[포인트] Circuit Breaker OPEN - service unavailable");
            throw new ServiceUnavailableException("포인트 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[포인트] Feign 호출 실패", e);
            throw new BusinessException(OrderErrorType.POINT_RESERVATION_FAILED);
        }
    }
}
