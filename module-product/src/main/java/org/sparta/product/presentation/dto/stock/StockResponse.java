package org.sparta.product.presentation.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.enums.StockReservationStatus;

import java.util.UUID;

/**
 * 재고 관련 응답 DTO 모음
 */
public class StockResponse {

    @Schema(description = "재고 예약 응답")
    public record Reserve(
            @Schema(description = "예약 ID", example = "9f4e4f2e-1a2b-3c4d-5e6f-7a8b9c0d1e2f")
            UUID reservationId,

            @Schema(description = "재고 ID", example = "3f1a2b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b")
            UUID stockId,

            @Schema(description = "예약 키 (orderId 등)", example = "orderItem-20251203-0001")
            String reservationKey,

            @Schema(description = "예약 수량", example = "3")
            int reservedQuantity,

            @Schema(description = "예약 상태", example = "RESERVED")
            String status
    ) {
        public static Reserve of(StockReservation reservation) {
            return new Reserve(
                    reservation.getId(),
                    reservation.getStockId(),
                    reservation.getReservationKey(),
                    reservation.getReservedQuantity(),
                    reservation.getStatus().name()
            );
        }
    }
}
