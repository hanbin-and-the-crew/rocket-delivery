package org.sparta.hub.presentation;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.application.HubService;
import org.sparta.hub.presentation.dto.request.HubCreateRequest;
import org.sparta.hub.presentation.dto.response.HubCreateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<HubCreateResponse> createHub(
            @Validated @RequestBody HubCreateRequest request
    ) {
        HubCreateResponse response = hubService.createHub(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
