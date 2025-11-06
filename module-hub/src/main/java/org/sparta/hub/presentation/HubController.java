package org.sparta.hub.presentation;

import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.hub.application.HubService;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 허브 관련 API 컨트롤러
 * - 생성, 조회 (전체/단건)
 * - ApiResponse 공통 응답 포맷 적용
 */
@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    /**
     * 허브 생성 API
     * POST /api/hubs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HubCreateResponse>> createHub(
            @Validated @RequestBody HubCreateRequest request
    ) {
        HubCreateResponse response = hubService.createHub(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 전체 허브 조회 API
     * GET /api/hubs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HubResponse>>> getAllHubs() {
        List<HubResponse> hubs = hubService.getAllHubs();
        return ResponseEntity
                .ok(ApiResponse.success(hubs));
    }

    /**
     * 단건 허브 조회 API
     * GET /api/hubs/{hubId}
     */
    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> getHubById(
            @PathVariable UUID hubId
    ) {
        HubResponse hub = hubService.getHubById(hubId);
        return ResponseEntity
                .ok(ApiResponse.success(hub));
    }
}
