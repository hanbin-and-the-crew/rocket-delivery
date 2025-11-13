package org.sparta.deliverylog.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class DeliveryLogRequest {

    public record Create(
            @NotNull(message = "배송 ID는 필수입니다")
            UUID deliveryId,

            @NotNull(message = "허브 순서는 필수입니다")
            @Positive(message = "허브 순서는 양수여야 합니다")
            Integer hubSequence,

            @NotNull(message = "출발 허브 ID는 필수입니다")
            UUID departureHubId,

            @NotNull(message = "도착 허브 ID는 필수입니다")
            UUID destinationHubId,

            Double expectedDistance,

            Integer expectedTime
    ) {}

    public record Assign(
            @NotNull(message = "배송 담당자 ID는 필수입니다")
            UUID deliveryManId
    ) {}

    public record Complete(
            @NotNull(message = "실제 거리는 필수입니다")
            @Positive(message = "실제 거리는 양수여야 합니다")
            Double actualDistance,

            @NotNull(message = "실제 시간은 필수입니다")
            @Positive(message = "실제 시간은 양수여야 합니다")
            Integer actualTime
    ) {}

    public record Update(
            Double expectedDistance,
            Integer expectedTime
    ) {}
}
