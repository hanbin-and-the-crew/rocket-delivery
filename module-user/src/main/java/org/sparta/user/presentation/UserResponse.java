package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

public class UserResponse {

    @Schema(description = "회원가입 응답")
    public record SignUpUser(
            @Schema(description = "사용자 PK", example = "151e6741-9723-4724-bfc0-91836773b33c")
            UUID userId,

            @Schema(description = "사용자 Id", example = "user1107")
            String userName
    ) {
        public static SignUpUser from(User user) {
            return new SignUpUser(
                    user.getUserId(),
                    user.getUserName()
            );
        }
    }

    @Schema(description = "회원 정보 조회 응답")
    public record GetUser(
            @Schema(description = "사용자 PK", example = "151e6741-9723-4724-bfc0-91836773b33c")
            UUID userId,

            @Schema(description = "사용자 Id", example = "user1107")
            String userName,

            @Schema(description = "userPw", example = "1234")
            String password,

            @Schema(description = "슬랙 Id", example = "user1108")
            String slackId,

            @Schema(description = "사용자 이름", example = "김철수")
            String realName,

            @Schema(description = "전화 번호", example = "01012341234")
            String userPhoneNumber,

            @Schema(description = "userEmail", example = "email@email.com")
            String email,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            UserRoleEnum role,

            @Schema(description = "허브 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f74")
            UUID hubId
    ) {
        public static GetUser from(User user) {
            return new GetUser(
                    user.getUserId(),
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

    @Schema(description = "회원 정보 수정 응답")
    public record UpdateUser(
            @Schema(description = "사용자 Id", example = "user1107")
            String userName,

            @Schema(description = "userPw", example = "1234")
            String password,

            @Schema(description = "슬랙 Id", example = "user1108")
            String slackId,

            @Schema(description = "사용자 이름", example = "김철수")
            String realName,

            @Schema(description = "전화 번호", example = "01012341234")
            String userPhoneNumber,

            @Schema(description = "userEmail", example = "email@email.com")
            String email,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            UserRoleEnum role,

            @Schema(description = "허브 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f74")
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

    @Schema(description = "회원 ID 찾기 응답")
    public record FindUserId(
            @Schema(description = "사용자 ID", example = "user1107")
            String userName
    ) {
        public static FindUserId from(User user) {
            return new FindUserId(
                    user.getUserName()
            );
        }
    }

}
