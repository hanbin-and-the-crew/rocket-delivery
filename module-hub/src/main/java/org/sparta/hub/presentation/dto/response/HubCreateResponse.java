package org.sparta.hub.presentation.dto.response;

import org.sparta.hub.domain.entity.Hub;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubCreateResponse(
        UUID hubId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String status,
        LocalDateTime createdAt,
        String createdBy
) {
    public static HubCreateResponse from(Hub hub) {
        return new HubCreateResponse(
                hub.getHubId(),
                hub.getName(),
                hub.getAddress(),
                hub.getLatitude(),
                hub.getLongitude(),
                hub.getStatus().name(),
                hub.getCreatedAt(),
                hub.getCreatedBy()

        );
    }
}
