package org.sparta.deliveryman.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class DeliveryManRequest {

    public record Create(
            @NotNull UUID userId,
            @NotBlank String userName,
            @Email String email,
            @NotBlank String phoneNumber,
            UUID affiliationHubId,
            String slackId,
            @NotBlank String deliveryManType
    ) {}

    public record Update(
            @NotBlank String userName,
            @Email String email,
            @NotBlank String phoneNumber,
            UUID affiliationHubId,
            String slackId,
            @NotBlank String deliveryManType,
            @NotBlank String status
    ) {}
}
