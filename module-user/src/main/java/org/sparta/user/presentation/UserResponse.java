package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

public class UserResponse {

    @Schema(description = "회원가입 응답")
    public record SignUpUser(
            @Schema(description = "사용자 이름", example = "user1107")
            String userName
    ) {
        public static SignUpUser from(User user) {
            return new SignUpUser(
                    user.getUserName()
            );
        }
    }

    @Schema(description = "회원 정보 수정 응답")
    public record UpdateUser(
            @Schema(description = "사용자 이름", example = "user1107")
            String userName,

            @Schema(description = "userPw", example = "1234")
            String password,

            @Schema(description = "슬랙 Id", example = "user1108")
            String slackId,

            @Schema(description = "사용자 이름", example = "김철수")
            String realName,

            @Schema(description = "전화 번호", example = "01012341234")
            String userPhone,

            @Schema(description = "userEmail", example = "email@email.com")
            String email,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            UserRoleEnum role,

            @Schema(description = "허브 ID", example = "123412431234")
            UUID hubId
    ) {
        public static UpdateUser from(User user) {
            return new UpdateUser(
                    user.getUserName(),
                    user.getPassword(),
                    user.getSlackId(),
                    user.getRealName(),
                    user.getUserPhoneNumber(),
                    user.getEmail(),
                    user.getRole(),
                    user.getHubId()
            );
        }
    }


}
