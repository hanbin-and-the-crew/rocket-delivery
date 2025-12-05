package org.sparta.delivery.presentation.dto.response;

import java.util.UUID;

public record HubLegResponse(
            UUID sourceHubId,
            UUID targetHubId,
            double estimatedKm,
            int estimatedMinutes
    ) { }