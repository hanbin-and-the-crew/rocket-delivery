package org.sparta.user.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.presentation.dto.request.LoginRequestDto;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        objectMapper = new ObjectMapper();
        // 기본 로그인 URL을 POST /api/user/login으로 설정
        // 여기서도 context-path는 톰캣에서 먼저 로드하기 때문에 /api는 제외
        // 따로 지정 안하면 POST /login
        setFilterProcessesUrl("/user/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("로그인 시도");
        // JSON을 LoginRequestDto 객체로 변환
        try{
            LoginRequestDto requestDto = objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);

            // 아이디, 패스워드, 권한으로 인증용 Authentication 객체 생성
            // 로그인이라 권한은 없기 때문에 마지막은 null
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getUserId(),
                            requestDto.getPassword(),
                            null
                    )
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // 로그인 성공 및 실패시 함수가 넘어올 수 있는 이유는
    // UsernamePasswordAuthenticationFilter의 getAuthenticationManager().authenticate()
    // 함수에 정의되어 있기 때문
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, Authentication authentication) throws IOException, ServletException {
        log.info("로그인 성공 및 JWT 생성");

        // authentication에서 사용자 정보 가져오기
        String id = ((CustomUserDetails) authentication.getPrincipal()).getUsername();
        String role = ((CustomUserDetails) authentication.getPrincipal()).getAuthorities().iterator().next().getAuthority();

        // accessToken 생성
        String accessToken = jwtUtil.createAccessToken(id, role);
        // refreshToken 생성
        String refreshToken = jwtUtil.createRefreshToken(id, role);

        // 응답 헤더에 토큰 추가
        response.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, accessToken);
        response.addHeader(JwtUtil.REFRESH_TOKEN_HEADER, refreshToken);

        // 응답 상태 코드 및 메시지 설정
        // 200
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"message\": \"로그인에 성공했습니다.\"}");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.info("로그인 실패");

        // 1. 응답 상태 코드를 401로 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 2. 응답 본문에 에러 메시지 작성
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"message\": \"로그인에 실패하였습니다.\"}");
    }
}