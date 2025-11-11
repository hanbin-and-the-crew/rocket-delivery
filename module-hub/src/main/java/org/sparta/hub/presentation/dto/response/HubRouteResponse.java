package org.sparta.hub.presentation.dto.response;

import org.sparta.hub.domain.entity.HubRoute;

import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteResponse(
        UUID routeId,
        UUID sourceHubId,
        UUID targetHubId,
        int distance,
        int duration,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubRouteResponse from(HubRoute e) {
        return new HubRouteResponse(
                e.getRouteId(),
                e.getSourceHubId(),
                e.getTargetHubId(),
                e.getDistance(),
                e.getDuration(),
                e.getStatus().name(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
