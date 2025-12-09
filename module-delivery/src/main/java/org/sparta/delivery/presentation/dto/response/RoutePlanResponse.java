package org.sparta.delivery.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoutePlanResponse(

        UUID sourceHubId,
        UUID targetHubId,
        int totalDistanceKm,
        int totalEstimatedMinutes,
        List<RouteLeg> legs
) {

    public record RouteLeg(
            UUID sourceHubId,
            UUID targetHubId,
            double distanceKm,
            int estimatedMinutes
    ) { }
}
