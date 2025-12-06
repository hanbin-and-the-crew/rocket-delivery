package org.sparta.user.presentation.ApiSpec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.presentation.dto.request.AuthRequest;
import org.sparta.user.presentation.dto.response.AuthResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth API",  description = "인증 관리" )
public interface AuthApiSpec {

    @Operation(
            summary = "로그인 (JWT 발급)",
            description = "userId / password로 로그인 후 JWT를 반환합니다."
    )
    @PostMapping("/login")
    ApiResponse<AuthResponse.Login> login(
            @Valid @RequestBody AuthRequest.Login request,
            HttpServletResponse LoginResponse
    );

    @Operation(
            summary = "로그아웃",
            description = "JWT 토큰을 무효화합니다."
    )
    @PostMapping("/logout")
    void logout(HttpServletResponse response);

}
