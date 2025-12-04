package org.sparta.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthResponse {

    @Schema(description = "로그인 응답")
    public record Login(
            @Schema(description = "Jwt Access Token",  example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @NotBlank
            String accessToken,

            @Schema(description = "Jwt Refresh Token", example = "Bearer qyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVwJ9...")
            @NotBlank
            String refreshToken
    ) {
    }
}
