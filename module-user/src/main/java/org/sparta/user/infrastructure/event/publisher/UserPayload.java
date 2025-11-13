package org.sparta.user.infrastructure.event.publisher;

import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;

import java.util.UUID;

public record UserPayload(
        UUID userId,
        String userName,
        String realName,
        String slackId,
        UserRoleEnum role,
        UserStatusEnum status,
        UUID hubId
) {
    public static UserPayload from(User user) {
        return new UserPayload(
                user.getUserId(),
                user.getUserName(),
                user.getRealName(),
                user.getSlackId(),
                user.getRole(),
                user.getStatus(),
                user.getHubId()
        );
    }
}