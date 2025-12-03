package org.sparta.user.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.user.application.service.UserService;
import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;
import org.sparta.user.infrastructure.SecurityDisabledConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Step4 Case
 * User API Controller 및 통합 테스트
 * 서비스/도메인 레이어에서 확장되어 실제 HTTP 요청-응답 흐름을 검증
 * 주요 테스트 시나리오
 * - 회원가입 API 성공 시 200 OK 및 응답 JSON 검증
 * - 잘못된 입력값 시 400 Bad Request 반환
 * - 회원 단건 조회 API의 응답 데이터 검증
 * - 회원가입 승인/거절 API 호출 시 정상 메시지 반환
 * - 전체 통합 플로우: 회원가입 → 승인 → DB 상태 검증 (RestAssured)
 */
@WebMvcTest(UserController.class)
@Import(SecurityDisabledConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID hubId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 API - 성공 시 200 OK 반환")
    void signup_ShouldReturnOk() throws Exception {

        // given
        UserResponse.SignUpUser response = new UserResponse.SignUpUser(userId, "testuser");
        given(userService.signup(any(UserRequest.SignUpUser.class))).willReturn(response);

        UserRequest.SignUpUser request = new UserRequest.SignUpUser(
                "testuser", "pw123!", "slack01", "홍길동",
                "01011112222", "test@ex.com", UserRoleEnum.MASTER, DeliveryManagerRoleEnum.COMPANY, hubId
        );

        // when & then
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("testuser"));
    }

    @Test
    @DisplayName("회원가입 API - 유효성 검증 실패 시 400 반환")
    void signup_WithInvalidInput_ShouldReturnBadRequest() throws Exception {

        // given: email 누락
        UserRequest.SignUpUser invalidRequest = new UserRequest.SignUpUser(
                "testuser", "pw123!", "slack01", "홍길동",
                "01011112222", "", UserRoleEnum.MASTER, DeliveryManagerRoleEnum.COMPANY, UUID.randomUUID()
        );

        // when & then
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원 단건 조회 API - 성공 시 200 OK 반환")
    void getUserInfo_ShouldReturnOk() throws Exception {

        // given
        UserResponse.GetUser response =
                new UserResponse.GetUser(userId, "tester", "q1w2e3r4", "slackId",
                        "김철수","01011112222", "test@ex.com", UserRoleEnum.MASTER, hubId);

        given(userService.getSpecificUserInfo(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/bos/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("tester"))
                .andExpect(jsonPath("$.data.email").value("test@ex.com"));
    }

    @Test
    @DisplayName("회원가입 승인 API - 성공 시 메시지 반환")
    void approveUser_ShouldReturnSuccessMessage() throws Exception {

        // given
        Mockito.doNothing().when(userService).updateUserStatus(userId, UserStatusEnum.APPROVE);

        // when & then
        mockMvc.perform(patch("/api/users/bos/{userId}/approve", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("회원가입이 승인되었습니다."));
    }

    @Test
    @DisplayName("회원가입 거절 API - 성공 시 메시지 반환")
    void rejectUser_ShouldReturnRejectMessage() throws Exception {

        // given
        Mockito.doNothing().when(userService).updateUserStatus(userId, UserStatusEnum.REJECTED);

        // when & then
        mockMvc.perform(patch("/api/users/bos/{userId}/reject", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("회원가입이 거절되었습니다."));
    }
}