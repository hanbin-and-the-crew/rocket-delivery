package org.sparta.hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HubCreateRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
