package org.sparta.hub.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse; // ← 공통 규칙: common.api 패키지
import org.sparta.hub.application.HubRouteService;
import org.sparta.hub.domain.entity.HubRoute;
import org.sparta.hub.presentation.dto.request.HubRouteRequest;
import org.sparta.hub.presentation.dto.response.HubRouteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HubRoute CRUD API")
@RestController
@RequestMapping("/api/hub-routes")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubRouteResponse>> createRoute(@RequestBody HubRouteRequest req) {
        HubRoute saved = hubRouteService.createRoute(
                req.sourceHubId(), req.targetHubId(), req.duration(), req.distance()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HubRouteResponse.from(saved)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HubRouteResponse>> getRoute(@PathVariable UUID id) {
        HubRoute found = hubRouteService.getRoute(id);
        return ResponseEntity.ok(ApiResponse.success(HubRouteResponse.from(found)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HubRouteResponse>> updateRoute(@PathVariable UUID id,
                                                                     @RequestBody HubRouteRequest req) {
        HubRoute updated = hubRouteService.updateRoute(id, req.duration(), req.distance());
        return ResponseEntity.ok(ApiResponse.success(HubRouteResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable UUID id) {
        hubRouteService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HubRouteResponse>>> getAllActiveRoutes() {
        List<HubRoute> list = hubRouteService.getAllActiveRoutes();
        return ResponseEntity.ok(
                ApiResponse.success(list.stream().map(HubRouteResponse::from).toList())
        );
    }
}
