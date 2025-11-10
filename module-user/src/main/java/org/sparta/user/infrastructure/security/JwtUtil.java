package org.sparta.user.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    public static final String ACCESS_TOKEN_HEADER = "Authorization";
    public static final String REFRESH_TOKEN_HEADER = "RefreshToken";
    public static final String AUTHORIZATION_KEY = "auth";
    public static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(@Value("${jwt.access.secret.key}") String accessSecretKey,
                   @Value("${jwt.refresh.secret.key}") String refreshSecretKey,
                   @Value("${jwt.access.token.expire}") long accessTokenExpiration,
                   @Value("${jwt.refresh.token.expire}") long refreshTokenExpiration) {
        this.accessKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(accessSecretKey));
        this.refreshKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(refreshSecretKey));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    private String createToken(String id, String role, long validity, Key key) {
        // 유효기간 설정
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + validity);

        // 토큰 설정
        // subject에는 사용자를 식별할 수 있는 유일 값을 넣음
        // claim은 추가 정보로 보통 권한을 넣음
        return BEARER_PREFIX +
                Jwts.builder()
                        .subject(id)
                        .claim(AUTHORIZATION_KEY, role)
                        .issuedAt(now)
                        .expiration(validityDate)
                        .signWith(key)
                        .compact();
    }

    public String createAccessToken(String id, String role) {
        return createToken(id, role, accessTokenExpiration, accessKey);
    }
    public String createRefreshToken(String id, String role) {
        return createToken(id, role, refreshTokenExpiration, refreshKey);
    }

    // JwtAuthorizationFilter에서 헤더로부터 토큰을 가져오기위해 사용
    public String getAccessTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(ACCESS_TOKEN_HEADER);
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }

        return false;
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessKey);
    }
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshKey);
    }

    // 추후에 JwtAuthorizationFilter에서 인증 정보를 담는데 사용
    private Claims getUserInfo(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Claims getAccessTokenUserInfo(String token) {
        return getUserInfo(token, accessKey);
    }
    public Claims getRefreshTokenUserInfo(String token) {
        return getUserInfo(token, refreshKey);
    }

}