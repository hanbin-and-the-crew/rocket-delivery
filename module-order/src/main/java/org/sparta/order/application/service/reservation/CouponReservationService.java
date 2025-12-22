package org.sparta.order.application.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.order.application.dto.CouponReservationResult;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerOpenException;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.infrastructure.client.CouponClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 쿠폰 예약 서비스
 * 단일 책임: 쿠폰 예약 로직 + Circuit Breaker + 예외 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponReservationService {

    private static final String SERVICE_NAME = "coupon-service";

    private final CouponClient couponClient;
    private final CircuitBreaker circuitBreaker;

    /**
     * 쿠폰 예약
     */
    public CouponReservationResult reserve(UUID couponId, UUID customerId, UUID orderId, Long totalPrice) {
        // 쿠폰 미사용 시
        if (couponId == null) {
            log.info("[쿠폰] 미사용 - couponId is null");
            return null;
        }

        log.info("[쿠폰] 예약 시작 - couponId={}", couponId);

        try {
            // Circuit Breaker로 감싼 쿠폰 예약 호출
            CouponClient.ApiResponse<CouponClient.CouponReserveResponse.Reserve> apiResponse =
                circuitBreaker.execute(
                    () -> couponClient.reserveCoupon(
                        couponId,
                        new CouponClient.CouponRequest.Reverse(customerId, orderId, totalPrice)
                    ),
                    SERVICE_NAME
                );

            log.info("[쿠폰] API 응답 - result={}, errorCode={}, message={}",
                apiResponse.result(), apiResponse.errorCode(), apiResponse.message());

            // API 호출 결과 확인
            if (!"SUCCESS".equals(apiResponse.result())) {
                log.error("[쿠폰] API 호출 실패 - errorCode={}, message={}",
                    apiResponse.errorCode(), apiResponse.message());
                throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
            }

            // data에서 실제 쿠폰 데이터 추출
            CouponClient.CouponReserveResponse.Reserve couponData = apiResponse.data();

            if (couponData == null) {
                log.error("[쿠폰] 데이터가 null - couponId={}", couponId);
                throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
            }

            log.info("[쿠폰] 데이터 추출 - valid={}, reservationId={}, discountAmount={}",
                couponData.valid(), couponData.reservationId(), couponData.discountAmount());

            // 할인액과 예약 ID 할당
            Long discountAmount = couponData.discountAmount() != null ? couponData.discountAmount() : 0L;
            UUID reservationId = couponData.reservationId();

            log.info("[쿠폰] 할인 적용 완료 - discountAmount={}, reservationId={}",
                discountAmount, reservationId);

            // 0원 할인 경고
            if (discountAmount == 0L) {
                log.warn("[쿠폰] 할인액이 0원 - couponId={}, 쿠폰 정책 확인 필요", couponId);
            }

            return new CouponReservationResult(reservationId, discountAmount, couponData.valid());

        } catch (CircuitBreakerOpenException e) {
            log.warn("[쿠폰] Circuit Breaker OPEN - service unavailable");
            throw new ServiceUnavailableException("쿠폰 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[쿠폰] Feign 호출 실패 - couponId={}", couponId, e);
            throw new BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED);
        }
    }
}
