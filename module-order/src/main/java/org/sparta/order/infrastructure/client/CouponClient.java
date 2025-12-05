package org.sparta.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@FeignClient(
        name = "coupon-service"
)
public interface CouponClient {

    @PostMapping("/api/coupons/{couponId}/reserve")
    CouponReserveResponse reserveCoupon(
            @PathVariable("couponId") String couponId,
            @RequestBody CouponReserveRequest request
    );

    // ===== DTO =====
    record CouponReserveRequest(
            String userId,
            String orderId,
            Long orderAmount
    ) {}

    record CouponReserveResponse(
            boolean valid,
            String reservationId,   // UUID 라고 했으니 실제로는 String → UUID 변환 가능
            Long discountAmount,
            String discountType,    // "FIXED" 등
            LocalDateTime expiresAt,
            String errorCode,
            String message
    ) {}
}
