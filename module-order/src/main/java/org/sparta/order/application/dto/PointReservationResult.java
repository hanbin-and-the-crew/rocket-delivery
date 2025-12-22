package org.sparta.order.application.dto;

/**
 * 포인트 예약 결과 DTO
 */
public record PointReservationResult(
    String reservationId,    // 예약 ID
    Long usedAmount,         // 사용된 포인트 금액
    String status            // 예약 상태
) {}