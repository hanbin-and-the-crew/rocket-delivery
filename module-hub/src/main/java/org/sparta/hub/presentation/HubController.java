package org.sparta.hub.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.hub.application.HubService;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 허브 관련 REST 컨트롤러
 * - 생성, 전체 조회, 단건 조회
 */
@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubCreateResponse>> createHub(
            @Valid @RequestBody HubCreateRequest request
    ) {
        HubCreateResponse response = hubService.createHub(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HubResponse>>> getAllHubs() {
        List<HubResponse> hubs = hubService.getAllHubs();
        return ResponseEntity.ok(ApiResponse.success(hubs));
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> getHubById(@PathVariable UUID hubId) {
        HubResponse hub = hubService.getHubById(hubId);
        return ResponseEntity.ok(ApiResponse.success(hub));
    }
}
