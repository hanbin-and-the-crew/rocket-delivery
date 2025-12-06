package org.sparta.coupon.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Coupon API", description = "쿠폰 관리")
public interface CouponApiSpec {

    @Operation(
            summary = "쿠폰 검증 및 예약",
            description = "주문 시작 시 쿠폰을 검증하고 예약합니다. 검증 성공 시 쿠폰 상태가 RESERVED로 변경되며, 5분간 다른 주문에서 사용할 수 없습니다."
    )
    ApiResponse<CouponResponse.Reserve> reserveCoupon(
            @PathVariable UUID couponId,
            @Valid @RequestBody CouponRequest.Reserve request
    );

    @Operation(
            summary = "쿠폰 사용 확정",
            description = "주문 완료 후 쿠폰 사용을 확정합니다. 쿠폰 상태가 PAID로 변경되며 재사용이 불가능합니다."
    )
    ApiResponse<CouponResponse.Confirm> confirmCoupon(
            @Valid @RequestBody CouponRequest.Confirm request
    );

    @Operation(
            summary = "쿠폰 예약 취소",
            description = "주문 실패 또는 취소 시 쿠폰 예약을 해제합니다. 쿠폰 상태가 AVAILABLE로 복원되어 다시 사용할 수 있습니다."
    )
    ApiResponse<CouponResponse.Cancel> cancelReservation(
            @Valid @RequestBody CouponRequest.Cancel request
    );
}