package org.sparta.coupon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.application.service.CouponService;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponApiSpec {

    private final CouponService couponService;

    @Override
    @PostMapping("/{couponId}/reserve")
    public ApiResponse<CouponResponse.Reserve> reserveCoupon(
            @PathVariable UUID couponId,
            @Valid @RequestBody CouponRequest.Reserve request
    ) {
        try {
            CouponServiceResult.Reserve result = couponService.reserveCoupon(request,couponId);
            CouponResponse.Reserve response = CouponResponse.Reserve.success(result);
            return ApiResponse.success(response);
        } catch (BusinessException e) {
            CouponResponse.Reserve response = CouponResponse.Reserve.failure(
                    e.getErrorType().getCode(),
                    e.getErrorType().getMessage()
            );
            return ApiResponse.success(response);
        }
    }

    @Override
    @PostMapping("/confirm")
    public ApiResponse<CouponResponse.Confirm> confirmCoupon(
            @Valid @RequestBody CouponRequest.Confirm request
    ) {
        couponService.confirmCoupon(request.reservationId(), request.orderId());

        CouponResponse.Confirm response = CouponResponse.Confirm.of(LocalDateTime.now());
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/cancel")
    public ApiResponse<CouponResponse.Cancel> cancelReservation(
            @Valid @RequestBody CouponRequest.Cancel request
    ) {
        couponService.cancelReservation(request.reservationId());

        CouponResponse.Cancel response = CouponResponse.Cancel.of(CouponStatus.AVAILABLE);
        return ApiResponse.success(response);
    }
}