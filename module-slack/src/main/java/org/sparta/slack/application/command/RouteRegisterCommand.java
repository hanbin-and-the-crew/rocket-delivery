package org.sparta.slack.application.command;

import org.sparta.slack.domain.vo.RouteStopSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteRegisterCommand(
        UUID deliveryId,
        LocalDate scheduledDate,
        UUID originHubId,
        String originHubName,
        String originAddress,
        UUID destinationCompanyId,
        String destinationCompanyName,
        String destinationAddress,
        List<RouteStopSnapshot> stops
) {
}
