package org.sparta.hub.application.route;

import java.util.UUID;

public record RouteLeg(
        UUID sourceHubId,
        UUID targetHubId,
        double distanceKm,
        int estimatedMinutes
) {}
