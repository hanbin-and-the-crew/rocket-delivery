package org.sparta.slack.presentation.dto.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.slack.application.route.command.RouteRegisterCommand;
import org.sparta.slack.domain.vo.RouteStopSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record RouteRegistrationRequest(

        @NotNull
        UUID deliveryId,
        @NotNull
        LocalDate scheduledDate,
        @NotNull
        UUID originHubId,
        @NotBlank
        String originHubName,
        @NotBlank
        String originAddress,
        @NotNull
        UUID destinationCompanyId,
        @NotBlank
        String destinationCompanyName,
        @NotBlank
        String destinationAddress,
        @Valid
        List<RouteStopRequest> stops
) {

    public RouteRegisterCommand toCommand() {
        List<RouteStopSnapshot> snapshots = stops != null
                ? stops.stream().map(RouteStopRequest::toSnapshot).collect(Collectors.toList())
                : List.of();

        return new RouteRegisterCommand(
                deliveryId,
                scheduledDate,
                originHubId,
                originHubName,
                originAddress,
                destinationCompanyId,
                destinationCompanyName,
                destinationAddress,
                snapshots
        );
    }
}
