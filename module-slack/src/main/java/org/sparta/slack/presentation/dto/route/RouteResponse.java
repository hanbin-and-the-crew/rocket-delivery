package org.sparta.slack.presentation.dto.route;

import org.sparta.slack.domain.entity.CompanyDeliveryRoute;
import org.sparta.slack.domain.enums.RouteStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RouteResponse(
        UUID routeId,
        UUID deliveryId,
        LocalDate scheduledDate,
        RouteStatus status,
        String originHubName,
        String destinationCompanyName,
        String destinationAddress,
        String deliveryManagerName,
        LocalDateTime dispatchedAt
) {

    public static RouteResponse from(CompanyDeliveryRoute route) {
        return new RouteResponse(
                route.getId(),
                route.getDeliveryId(),
                route.getScheduledDate(),
                route.getStatus(),
                route.getOriginHubName(),
                route.getDestinationCompanyName(),
                route.getDestinationAddress(),
                route.getDeliveryManagerName(),
                route.getDispatchedAt()
        );
    }
}
