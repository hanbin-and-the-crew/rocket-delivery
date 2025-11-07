package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

public class UserRequest {

    @Schema(description = "회원가입 요청")
    public record SignUpUser(
            @Schema(description = "로그인 ID", example = "user1107")
            @NotBlank
            String userName,

            @Schema(description = "userPw", example = "1234")
            @NotBlank
            String password,

            @Schema(description = "슬랙 Id", example = "user1108")
            @NotBlank
            String slackId,

            @Schema(description = "사용자 이름", example = "김철수")
            @NotBlank
            String realName,

            @Schema(description = "전화 번호", example = "01012341234")
            @NotBlank
            String userPhone,

            @Schema(description = "userEmail", example = "email@email.com")
            @Email
            @NotBlank
            String email,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            @NotBlank
            UserRoleEnum role,

            @Schema(description = "허브 ID", example = "123412431234")
            @NotBlank
            UUID hubId
    ) {
    }
}
