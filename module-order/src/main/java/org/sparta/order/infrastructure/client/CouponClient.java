package org.sparta.order.infrastructure.client;

import org.sparta.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.UUID;

@FeignClient(
        name = "coupon-service"
)
public interface CouponClient {

    @PostMapping("/api/coupons/{couponId}/reserve")
    ApiResponse<CouponReserveResponse.Reserve> reserveCoupon(
            @PathVariable("couponId") UUID couponId,
            @RequestBody CouponRequest.Reverse request
    );

    @GetMapping("/actuator/health")
    void health();

    // ===== DTO =====
    class CouponRequest {
        public record Reverse(
                UUID userId,
                UUID orderId,
                Long orderAmount
        ) {}
    }


    // ===== ApiResponse 래퍼 (meta로 감싸진 구조) =====
    record ApiResponse<T>(
            Meta meta,  // ← meta 객체로 감싸짐
            T data
    ) {
        // meta의 필드를 편하게 접근할 수 있는 헬퍼 메서드
        public boolean isSuccess() {
            return meta != null && "SUCCESS".equals(meta.result());
        }

        public String result() {
            return meta != null ? meta.result() : null;
        }

        public String errorCode() {
            return meta != null ? meta.errorCode() : null;
        }

        public String message() {
            return meta != null ? meta.message() : null;
        }
    }

    // Meta 객체 정의
    record Meta(
            String result,      // "SUCCESS" or "FAIL"
            String errorCode,   // "order:payment_approve_failed"
            String message      // "결제 승인 실패"
    ) {}

    // 실제 데이터 (쿠폰의 컨트롤러 형식에 맞춰서 중첩으로 사용)
    class CouponReserveResponse {
        public record Reserve(
                boolean valid,
                UUID reservationId,
                Long discountAmount,
                String discountType,
                LocalDateTime expiresAt,
                String errorCode,
                String message
        ) {}
    }


}
