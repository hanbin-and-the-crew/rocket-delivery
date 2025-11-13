package org.sparta.hub.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.hub.application.HubService;
import org.sparta.hub.presentation.dto.response.HubResponse;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 운영자 전용 엔드포인트
 * - 모든 상태의 허브 조회 가능
 * - 삭제: Soft Delete (INACTIVE 전환)
 */
@Tag(name = "Hub API (Admin)", description = "운영자용 허브 관리 API")
@RestController
@RequestMapping("/api/admin/hubs")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')") // 프로젝트의 시큐리티 정책에 맞춰 적용
public class AdminHubController {

    private final HubService hubService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HubResponse>>> list(@RequestParam(defaultValue = "ALL") String status) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getHubsForAdmin(status)));
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> get(@PathVariable UUID hubId) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getHubByIdForAdmin(hubId)));
    }

    @DeleteMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> delete(@PathVariable UUID hubId) {
        HubResponse deleted = hubService.deleteHub(hubId);
        return ResponseEntity.ok(ApiResponse.success(deleted));
    }
}
