package org.sparta.order.infrastructure.client.dto.response;

import java.util.UUID;

/**
 * Hub 서비스로부터 받는 허브 정보
 */
public record HubResponse(
        UUID hubId,
        String name,
        String address,
        Double latitude,
        Double longitude
) {
}