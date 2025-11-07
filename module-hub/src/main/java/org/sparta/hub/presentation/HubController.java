package org.sparta.hub.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.hub.application.HubService;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.request.HubUpdateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 일반 사용자 공개용 엔드포인트
 * - 생성
 * - ACTIVE 상태 허브 단건/전체 조회
 * - 수정
 */
@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;


    /**
     * 허브 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HubCreateResponse>> createHub(
            @Valid @RequestBody HubCreateRequest request
    ) {
        HubCreateResponse response = hubService.createHub(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 허브 전체 조회(ACTIVE 상태만 조회, 일반 사용자용)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HubResponse>>> getAllHubs() {
        List<HubResponse> hubs = hubService.getActiveHubsForUser();
        return ResponseEntity.ok(ApiResponse.success(hubs));
    }

    /**
     * 허브 단건 조회(ACTIVE 상태만 조회, 일반 사용자용)
     */
    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> getHubById(@PathVariable UUID hubId) {
        HubResponse hub = hubService.getActiveHubByIdForUser(hubId);
        return ResponseEntity.ok(ApiResponse.success(hub));
    }

    /**
     * 허브 수정
     */
    @PutMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> updateHub(
            @PathVariable UUID hubId,
            @Valid @RequestBody HubUpdateRequest request
    ) {
        HubResponse updated = hubService.updateHub(hubId, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * 허브 삭제
     */
    @DeleteMapping("/{hubId}")
    public ResponseEntity<ApiResponse<Object>> deleteHub(@PathVariable UUID hubId) {
        hubService.deleteHub(hubId);
        return ResponseEntity.ok(ApiResponse.success());
    }

}
