package org.sparta.order.infrastructure.client.dto;

import org.sparta.order.domain.enumeration.UserRoleEnum;

import java.util.UUID;

/**
 * User 서비스로부터 받는 사용자 정보
 */
public record UserResponse(
        UUID userId,
        String userName,
        String realName,
        String email,
        String userPhoneNumber,
        UUID hubId,
        UserRoleEnum role
) {
}