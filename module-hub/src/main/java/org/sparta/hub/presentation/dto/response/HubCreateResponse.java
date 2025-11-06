package org.sparta.hub.presentation.dto.response;

import org.sparta.hub.domain.entity.Hub;

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
) {
    public static HubCreateResponse from(Hub hub) {
        return new HubCreateResponse(
                hub.getHubId(),
                hub.getName(),
                hub.getAddress(),
                hub.getLatitude(),
                hub.getLongitude(),
                hub.getStatus().name(),
                null,
                null
        );
    }
}
