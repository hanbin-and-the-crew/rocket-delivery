package org.sparta.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    public static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey accessKey;

    public JwtUtil(@Value("${jwt.access.secret.key}") String accessSecretKey) {
        this.accessKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(accessSecretKey));
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims getAccessTokenUserInfo(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public List<SimpleGrantedAuthority> convertToAuthorities(String role) {
        return List.of(new SimpleGrantedAuthority(role));
    }
}