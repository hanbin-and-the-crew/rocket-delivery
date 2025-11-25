package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.user.domain.enums.UserRoleEnum;

public class AuthRequest {

    @Schema(description = "로그인 요청")
    public record Login(
            @Schema(description = "로그인 ID", example = "user3000")
            @NotBlank
            String userName,

            @Schema(description = "userPw", example = "1234")
            @NotBlank
            String password,

            @Schema(description = "권한", example = "MASTER")
            @NotNull
            UserRoleEnum role,

            @Schema(description = "authVersion", example = "1")
            @NotNull
            Integer authVersion
    ) {
    }
}