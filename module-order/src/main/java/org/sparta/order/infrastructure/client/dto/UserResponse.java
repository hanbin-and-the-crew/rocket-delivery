package org.sparta.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * User 서비스로부터 받는 사용자 정보
 */
public record UserResponse(
        UUID userId,
        String userName,
        String realName,
        String email,
        UUID hubId
) {
}