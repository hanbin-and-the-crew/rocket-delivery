package org.sparta.hub.presentation.dto.request;

import java.util.UUID;

public record HubRouteRequest(
        UUID sourceHubId,
        UUID targetHubId,
        int distance,
        int duration
) {}
