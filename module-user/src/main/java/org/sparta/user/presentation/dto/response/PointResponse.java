package org.sparta.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.user.domain.entity.PointReservation;

import java.util.List;

public class PointResponse {

    @Schema(description = "포인트 응답")
    public record PointReservationResult (
            @Schema(description = "예약된 할인 금액 포인트", example = "5000")
            Long discountAmount,

            @Schema(description = "예약된 포인트 목록")
            List<PointReservation> reservations
    ) {
        public static PointReservationResult of(Long discountAmount, List<PointReservation> reservations) {
            return new PointReservationResult(discountAmount, reservations);
        }
    }

    @Schema(description = "현재 포인트 확인")
    public record PointSummary(
            @Schema(description = "전체 포인트", example = "10000")
            Long totalAmount,

            @Schema(description = "예약된 포인트", example = "5000")
            Long reservedAmount,

            @Schema(description = "사용된 포인트", example = "2000")
            Long usedAmount,

            @Schema(description = "사용 가능한 포인트", example = "3000")
            Long availableAmount
    ) {
        public static PointSummary of(Long totalAmount, Long reservedAmount, Long usedAmount, Long availableAmount) {
            return new PointSummary(totalAmount, reservedAmount, usedAmount, availableAmount);
        }
    }
}