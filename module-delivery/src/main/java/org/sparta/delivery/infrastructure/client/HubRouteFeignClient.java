package org.sparta.delivery.infrastructure.client;

//import org.sparta.common.route.dto.RoutePlanResponse;
import org.sparta.delivery.presentation.dto.response.HubLegResponse;

//import org.sparta.delivery.application.service.DeliveryServiceImpl.HubLegResponse;
import org.sparta.delivery.presentation.dto.response.RoutePlanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "hub-service"
//        name = "hub-route-client"
//        url = "${external.hub-service.url}"
)
public interface HubRouteFeignClient {

    @GetMapping("/api/hub-routes/plan")
    RoutePlanResponse planRoute(
            @RequestParam("sourceHubId") UUID sourceHubId,
            @RequestParam("targetHubId") UUID targetHubId
    );

    default List<HubLegResponse> getRouteLegs(UUID sourceHubId, UUID targetHubId) {
        RoutePlanResponse plan = planRoute(sourceHubId, targetHubId);

        return plan.legs().stream()
                .map(leg -> new HubLegResponse(
                        leg.sourceHubId(),
                        leg.targetHubId(),
                        leg.distanceKm(),
                        leg.estimatedMinutes()
                ))
                .toList();
    }
}
