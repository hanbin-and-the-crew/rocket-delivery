package org.sparta.order.application.dto;

import java.util.UUID;

/**
 * 재고 예약 결과 DTO
 */
public record StockReservationResult(
    UUID reservationId,      // 예약 ID
    Integer reservedQuantity, // 예약 수량
    String status            // 예약 상태 (RESERVED)
) {}