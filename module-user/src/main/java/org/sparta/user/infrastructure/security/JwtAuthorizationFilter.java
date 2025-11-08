package org.sparta.user.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String accessToken = jwtUtil.getAccessTokenFromHeader(request);
        log.info("JwtAuthorizationFilter accessToken:{}", accessToken);

        // 토큰이 존재하지 않으면 다음 필터로 넘김
        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 엑세스 토큰이 존재한다면
        if(jwtUtil.validateAccessToken(accessToken)) {

            // 클레임 정보를 가져오기
            Claims info = jwtUtil.getAccessTokenUserInfo(accessToken);
            try {
                setAuthentication(info.getSubject());
            } catch (Exception e){
                log.error(e.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    public void setAuthentication(String id) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // 사용자 로그인 ID를 통해 UserDetails 객체를 불러오고
        // Authentication 객체를 생성
        UserDetails userDetails = userDetailsService.loadUserByUsername(id);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // SecurityContextHolder에 인증, 인가된 Authentication 객체를 넣기
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}