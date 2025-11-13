package org.sparta.hub.presentation.dto.response;

import org.sparta.hub.application.route.RouteLeg;

import java.util.List;
import java.util.UUID;

public record RoutePlanResponse(
        UUID sourceHubId,
        UUID targetHubId,
        double totalDistanceKm,
        int totalDurationMinutes,
        List<RouteLeg> legs
) {}
