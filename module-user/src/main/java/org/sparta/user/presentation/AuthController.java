package org.sparta.user.presentation;

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
public class AuthController implements AuthApiSpec{

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Company Service!";
    }

    @Override
    @PostMapping("/login")
    public ApiResponse<AuthResponse.Login> login(@Valid @RequestBody AuthRequest.Login request, HttpServletResponse LoginResponse) {

        // token 생성
        String accessToken = jwtUtil.createAccessToken(request.userName(), request.role().name());
        String refreshToken = jwtUtil.createRefreshToken(request.userName(), request.role().name());

        // 헤더에 추가
        LoginResponse.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, accessToken);
        LoginResponse.addHeader(JwtUtil.REFRESH_TOKEN_HEADER, refreshToken);

        return ApiResponse.success(new AuthResponse.Login(accessToken, refreshToken));
    }
}
