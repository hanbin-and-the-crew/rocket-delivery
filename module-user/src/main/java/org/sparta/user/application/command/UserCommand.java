package org.sparta.user.application.command;

import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

public class UserCommand {

    public record SignUpUser(
            String userName,
            String password,
            String slackId,
            String realName,
            String userPhoneNumber,
            String email,
            UserRoleEnum role,
            DeliveryManagerRoleEnum deliveryRole,
            UUID hubId
    ) {}

    public record UpdateUser(
            String userName,
            String slackId,
            String realName,
            String userPhoneNumber,
            String email,
            String oldPassword,
            String newPassword,
            UserRoleEnum role,
            DeliveryManagerRoleEnum deliveryRole,
            UUID hubId
    ) {}

    public record FindUserId(
            String email
    ) {}
}
