package org.sparta.order.application.dto;

import java.util.UUID;

/**
 * 쿠폰 예약 결과 DTO
 */
public record CouponReservationResult(
    UUID reservationId,      // 예약 ID
    Long discountAmount,     // 할인 금액
    boolean valid            // 쿠폰 유효 여부
) {}