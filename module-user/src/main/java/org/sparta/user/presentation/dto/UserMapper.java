package org.sparta.user.presentation.dto;

import org.sparta.user.application.command.UserCommand;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    // 추후 MapStruct 라이브러리를 쓴다면 일일히 써줄 필요 없이 손쉽게 toCommand 정의 가능

    public UserCommand.SignUpUser toCommand(UserRequest.SignUpUser request) {
        return new UserCommand.SignUpUser(
                request.userName(),
                request.password(),
                request.slackId(),
                request.realName(),
                request.userPhoneNumber(),
                request.email(),
                request.role(),
                request.deliveryRole(),
                request.hubId()
        );
    }

    public UserCommand.UpdateUser toCommand(UserRequest.UpdateUser request) {
        return new UserCommand.UpdateUser(
                request.userName(),
                request.slackId(),
                request.realName(),
                request.userPhoneNumber(),
                request.email(),
                request.oldPassword(),
                request.newPassword(),
                request.role(),
                request.deliveryRole(),
                request.hubId()
        );
    }

    public UserCommand.FindUserId toCommand(UserRequest.FindUserId request) {
        return new UserCommand.FindUserId(request.email());
    }
}