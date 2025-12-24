package org.sparta.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

public class UserRequest {

    @Schema(description = "회원가입 요청")
    public record SignUpUser(
            @Schema(description = "사용자 ID", example = "user4000")
            @NotBlank
            String userName,

            @Schema(description = "userPw", example = "1234")
            @NotBlank
            String password,

            @Schema(description = "슬랙 Id", example = "user4000")
            @NotBlank
            String slackId,

            @Schema(description = "사용자 이름", example = "박민수")
            @NotBlank
            String realName,

            @Schema(description = "전화 번호", example = "01012341234")
            @NotBlank
            String userPhoneNumber,

            @Schema(description = "userEmail", example = "user4000@email.com")
            @Email
            @NotBlank
            String email,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            @NotNull
            UserRoleEnum role,

            @Schema(description = "배송 담당자 종류", example = "HUB")
            DeliveryManagerRoleEnum deliveryRole, // DELIVERY_MANAGER일때만 유효

            @Schema(description = "허브 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f74")
            UUID hubId
    ) {
    }

    @Schema(description = "회원 정보 수정 요청")
    public record UpdateUser(
            @Schema(description = "사용자 Id", example = "user4000")
            @Size(min = 1, max = 50, message = "아이디는 n 자 입니다.")
            @NotBlank
            String userName,

            @Schema(description = "슬랙 Id", example = "user4000")
            @NotBlank
            String slackId,

            @Schema(description = "사용자 이름", example = "박민수")
            @Size(min = 1, max = 50, message = "이름은 1~50자여야 합니다.")
            @NotBlank
            String realName,

            @Schema(description = "휴대폰 번호(숫자만 10~11자리)", example = "01012345678")
            @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자 10~11자리여야 합니다.")
            @NotBlank
            String userPhoneNumber,

            @Schema(description = "이메일", example = "user4000@example.com")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            @Size(max = 100, message = "이메일은 최대 100자입니다.")
            @NotBlank
            String email,

            @Schema(description = "기존 패스워드", example = "1234")
            @NotBlank
            String oldPassword,

            @Schema(description = "신규 패스워드", example = "2345")
            @NotBlank
            String newPassword,

            @Schema(description = "권한", example = "DELIVERY_MANAGER")
            @NotNull
            UserRoleEnum role,

            @Schema(description = "배송 담당자 종류", example = "HUB")
            DeliveryManagerRoleEnum deliveryRole, // DELIVERY_MANAGER일때만 유효

            @Schema(description = "허브 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f74")
            UUID hubId
    ) {
    }

    @Schema(description = "회원 ID 찾기 요청")
    public record FindUserId(
            @Schema(description = "이메일", example = "user4000@example.com")
            @NotBlank
            String email
    ) {
    }
}
