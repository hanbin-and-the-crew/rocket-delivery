package org.sparta.user.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.infrastructure.security.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Company Service!";
    }

    @Operation(
            summary = "로그인 (JWT 발급)",
            description = "userId / password로 로그인 후 JWT를 반환합니다."
    )
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse LoginResponse) {

        // access token 생성
        String accessToken = jwtUtil.createAccessToken(request.userName(), request.role().name());

        // refresh token 생성
        String refreshToken = jwtUtil.createRefreshToken(request.userName(), request.role().name());
        // 헤더에 추가
        LoginResponse.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, accessToken);
        LoginResponse.addHeader(JwtUtil.REFRESH_TOKEN_HEADER, refreshToken);
        return ApiResponse.success(new LoginResponse(accessToken, refreshToken));
    }

    // === DTOs ===
    public record LoginRequest(
           // @Schema(example = "3") Integer userNo,
            @Schema(example = "user03") String userName,
            @Schema(example = "MASTER") UserRoleEnum role,
            @Schema(example = "1") Integer authVersion

    ) {}

    public record LoginResponse(
            @Schema(example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken,
            @Schema(example = "Bearer qyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVwJ9...") String refreshToken
    ) {}
}
