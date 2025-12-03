package org.sparta.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.user.domain.entity.PointReservation;

import java.util.List;

public class PointResponse {

    @Schema(description = "포인트 응답")
    public record PointReservationResult (
            @Schema(description = "예약된 포인트", example = "5000")
            Integer totalReservedAmount,

            @Schema(description = "ㅇㅇ", example = "user1107")
            List<PointReservation> reservations
    ) {
    }
}