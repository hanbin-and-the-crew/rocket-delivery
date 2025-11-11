package org.sparta.hub.fixture;

import org.sparta.hub.domain.entity.HubRoute;
import org.sparta.hub.domain.model.HubRouteStatus;

import java.util.UUID;

public class HubRouteFixture {

    public static HubRoute activeRoute() {
        return HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(150)
                .duration(100)
                .status(HubRouteStatus.ACTIVE)
                .build();
    }

    public static HubRoute inactiveRoute() {
        return HubRoute.builder()
                .sourceHubId(UUID.randomUUID())
                .targetHubId(UUID.randomUUID())
                .distance(150)
                .duration(100)
                .status(HubRouteStatus.INACTIVE)
                .build();
    }
}
