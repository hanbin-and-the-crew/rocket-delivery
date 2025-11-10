package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

public class UserRequest {

    @Schema(description = "회원가입 요청")
    public record SignUpUser(
            @Schema(description = "사용자 ID", example = "user1107")
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
            @NotNull
            UserRoleEnum role,

            @Schema(description = "허브 ID", example = "123412431234")
            @NotNull
            UUID hubId
    ) {
    }

    @Schema(description = "회원 정보 수정 요청")
    public record UpdateUser(
            @Schema(description = "사용자 Id", example = "testId")
            @Size(min = 1, max = 50, message = "아이디는 n 자 입니다.")
            @NotBlank
            String userName,

            @Schema(description = "슬랙 Id", example = "user1108")
            @NotBlank
            String slackId,

            @Schema(description = "사용자 이름", example = "김철수")
            @Size(min = 1, max = 50, message = "이름은 1~50자여야 합니다.")
            @NotBlank
            String realName,

            @Schema(description = "휴대폰 번호(숫자만 10~11자리)", example = "01012345678")
            @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자 10~11자리여야 합니다.")
            @NotBlank
            String userPhone,

            @Schema(description = "이메일", example = "asdf1234@example.com")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            @Size(max = 100, message = "이메일은 최대 100자입니다.")
            @NotBlank
            String email,

            @Schema(description = "기존 패스워드", example = "q1w2e3r4!")
            @NotBlank
            String oldPassword,

            @Schema(description = "신규 패스워드", example = "q1w2e3r4@")
            @NotBlank
            String newPassword,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            @NotNull
            UserRoleEnum role,

            @Schema(description = "허브 ID", example = "123412431234")
            @NotNull
            UUID hubId
    ) {
    }

    @Schema(description = "회원 ID 찾기 요청")
    public record FindUserId(
            @Schema(description = "이메일", example = "asdf1234@example.com")
            @NotBlank
            String email
    ) {
    }
}
