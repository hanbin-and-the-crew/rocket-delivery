package org.sparta.slack.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.slack.domain.vo.RouteStopSnapshot;

import java.util.UUID;

public record RouteStopRequest(
        UUID deliveryId,
        @NotBlank String label,
        @NotBlank String address,
        Double latitude,
        Double longitude,
        Integer sequence
) {

    public RouteStopSnapshot toSnapshot() {
        return RouteStopSnapshot.builder()
                .deliveryId(deliveryId)
                .label(label)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .sequence(sequence)
                .build();
    }
}
