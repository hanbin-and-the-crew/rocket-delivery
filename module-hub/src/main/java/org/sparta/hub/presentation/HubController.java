package org.sparta.hub.presentation;

import lombok.RequiredArgsConstructor;
import org.sparta.common.api.ApiResponse;
import org.sparta.hub.application.HubService;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 허브 관련 HTTP 요청을 처리하는 Controller
 * 모든 응답은 ApiResponse<T> 형식으로 통일
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
}
