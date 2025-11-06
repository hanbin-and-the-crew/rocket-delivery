package org.sparta.hub.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 허브 생성 응답 DTO
 * Controller → Client 응답 전용
 */
public record HubCreateResponse(
        UUID hubId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String status,
        LocalDateTime createdAt,
        String createdBy
) {}
