package org.sparta.hub.presentation.dto.response;

import org.sparta.hub.domain.entity.Hub;

import java.util.UUID;

public record HubResponse(
        UUID hubId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String status
) {
    public static HubResponse from(Hub hub) {
        return new HubResponse(
                hub.getHubId(),
                hub.getName(),
                hub.getAddress(),
                hub.getLatitude(),
                hub.getLongitude(),
                hub.getStatus().name()
        );
    }
}
