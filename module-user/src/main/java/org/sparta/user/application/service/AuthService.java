package org.sparta.user.application.service;

import jakarta.servlet.http.HttpServletResponse;
import org.sparta.user.infrastructure.security.JwtUtil;
import org.sparta.user.presentation.AuthRequest;
import org.sparta.user.presentation.AuthResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final JwtUtil jwtUtil;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * /auth/login
     */
    @Transactional
    public AuthResponse.Login login(AuthRequest.Login request, HttpServletResponse response) {
        // 토큰 생성
        String accessToken = jwtUtil.createAccessToken(request.userName(), request.role().name());
        String refreshToken = jwtUtil.createRefreshToken(request.userName(), request.role().name());

        // 헤더에 추가
        response.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, accessToken);
        response.addHeader(JwtUtil.REFRESH_TOKEN_HEADER, refreshToken);

        // 응답 DTO 반환
        return new AuthResponse.Login(accessToken, refreshToken);
    }

    /**
     * /auth/logout
     */
    @Transactional
    public void logout(HttpServletResponse response) {
        // 토큰 무효화를 위해 헤더 비우기 (클라이언트에서도 삭제하도록 유도)
        response.setHeader(JwtUtil.ACCESS_TOKEN_HEADER, "");
        response.setHeader(JwtUtil.REFRESH_TOKEN_HEADER, "");

        // 필요하다면 Redis 등에 블랙리스트 처리 추가 가능
        // e.g. redisService.addToBlacklist(accessToken, remainingTime);
    }
}
