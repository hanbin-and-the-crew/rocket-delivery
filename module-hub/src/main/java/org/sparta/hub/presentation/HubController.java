package org.sparta.hub.presentation;

import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @PostMapping
    public ResponseEntity<HubCreateResponse> createHub(
            @Validated @RequestBody HubCreateRequest request
    ) {
        HubCreateResponse response = hubService.createHub(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<HubResponse>> getAllHubs() {
        return ResponseEntity.ok(hubService.findAllHubs());
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<HubResponse> getHubById(@PathVariable UUID hubId) {
        return ResponseEntity.ok(hubService.findHubById(hubId));
    }





}
