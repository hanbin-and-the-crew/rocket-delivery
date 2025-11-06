package org.sparta.user.presentation.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.sparta.user.domain.enums.UserRoleEnum;

@Getter
public class SignUpUserRequestDto {

    @Schema(description = "userId", example = "testId")
    @NotBlank
    private final String userId;

    @Schema(description = "userPw", example = "1234")
    @NotBlank
    private final String password;

    @Schema(description = "유저 명", example = "테스터1")
    @NotBlank
    private final String userName;

    @Schema(description = "userPhone", example = "01012341234")
    @NotBlank
    private final String userPhone;

    @Schema(description = "userEmail", example = "email@email.com")
    @Email
    @NotBlank
    private final String email;

    @Schema(description = "권한", example = "CUSTOMER")
    private final UserRoleEnum role;

    private final String adminToken;

    public SignUpUserRequestDto(String userId, String password, String userName, String userPhone, String email, UserRoleEnum role, String adminToken) {
        this.userId = userId;
        this.password = password;
        this.userName = userName;
        this.userPhone = userPhone;
        this.email = email;
        this.role = role == null ? UserRoleEnum.DELIVERY_PERSON : role;
        this.adminToken = adminToken == null ? "" : adminToken;
    }
}
