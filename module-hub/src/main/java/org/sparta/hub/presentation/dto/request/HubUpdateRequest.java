package org.sparta.hub.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import org.sparta.hub.domain.model.HubStatus;

import java.util.UUID;

public record HubUpdateRequest(
        @NotNull UUID hubId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        HubStatus status
) { }
