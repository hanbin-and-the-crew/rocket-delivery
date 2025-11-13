package org.sparta.hub.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.hub.application.HubRoutePlanner;
import org.sparta.hub.presentation.dto.response.RoutePlanResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "HubRoute-Plan API", description = "허브 간 경로 API")
@RestController
@RequestMapping("/api/hub-routes")
@RequiredArgsConstructor
public class RoutePlanController {

    private final HubRoutePlanner planner;

    @Tag(name = "HubRoute - Plan", description = "허브 간 경로 계획(릴레이 포함)")
    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<RoutePlanResponse>> plan(
            @RequestParam UUID sourceHubId,
            @RequestParam UUID targetHubId
    ) {
        RoutePlanResponse plan = planner.plan(sourceHubId, targetHubId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }
}
