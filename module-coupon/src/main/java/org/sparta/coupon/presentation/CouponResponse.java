package org.sparta.coupon.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.sparta.coupon.domain.enums.DiscountType;

import java.time.LocalDateTime;
import java.util.UUID;

public class CouponResponse {

    @Schema(description = "쿠폰 예약 응답")
    public record Reserve(
            @Schema(description = "검증 성공 여부")
            Boolean valid,

            @Schema(description = "예약 ID")
            UUID reservationId,

            @Schema(description = "할인 금액")
            Long discountAmount,

            @Schema(description = "할인 타입")
            DiscountType discountType,

            @Schema(description = "예약 만료 시간")
            LocalDateTime expiresAt,

            @Schema(description = "에러 코드 (검증 실패 시)")
            String errorCode,

            @Schema(description = "에러 메시지 (검증 실패 시)")
            String message
    ) {
        /**
         * 검증 성공 응답
         */
        public static Reserve success(CouponServiceResult.Reserve result) {
            return new Reserve(
                    true,
                    result.reservationId(),
                    result.discountAmount(),
                    result.discountType(),
                    result.expiresAt(),
                    null,
                    null
            );
        }

        /**
         * 검증 실패 응답
         */
        public static Reserve failure(String errorCode, String message) {
            return new Reserve(
                    false,
                    null,
                    null,
                    null,
                    null,
                    errorCode,
                    message
            );
        }
    }

    @Schema(description = "쿠폰 사용 확정 응답")
    public record Confirm(
            @Schema(description = "성공 여부")
            Boolean success,

            @Schema(description = "사용 일시")
            LocalDateTime usedAt
    ) {
        public static Confirm of(LocalDateTime usedAt) {
            return new Confirm(true, usedAt);
        }
    }

    @Schema(description = "쿠폰 예약 취소 응답")
    public record Cancel(
            @Schema(description = "성공 여부")
            Boolean success,

            @Schema(description = "쿠폰 상태")
            CouponStatus status
    ) {
        public static Cancel of(CouponStatus status) {
            return new Cancel(true, status);
        }
    }
}