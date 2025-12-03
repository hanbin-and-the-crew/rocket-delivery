package org.sparta.user.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.user.application.service.UserService;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.sparta.user.infrastructure.security.CustomUserDetailsService;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.sparta.user.presentation.dto.response.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TDD Step2 Case
 * User 서버스 래이어 테스트 및 Mock 활용
 * 도메인 단위 테스트(엔티티 검증)에서 확장하여
 * UserService의 비즈니스 로직과 외부 의존성(Mock Repository, PasswordEncoder 등)을 검증
 * 주요 테스트 시나리오
 * - 유효한 입력으로 회원가입 시 UserRepository.save()가 호출되고 회원이 정상 등록되는가
 * - 중복된 username으로 회원가입 시 예외가 발생하는가
 * - 로그인된 사용자의 정보 조회 시 올바른 데이터를 반환하는가
 * - 본인 탈퇴 요청 시 softDeleteByUserId()가 호출되는가
 * - 존재하지 않거나 이미 탈퇴한 회원 탈퇴 시 예외가 발생하는가
 * - 회원 상태가 PENDING일 때 승인 또는 거절 처리가 정상적으로 수행되는가
 * - 이미 승인된 회원은 상태 변경 시 예외가 발생하는가
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID hubId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private UserService userService;

    @Mock
    private EventPublisher eventPublisher;


    @Test
    @DisplayName("정상 입력으로 회원가입 성공 시 UserRepository.save()가 호출된다")
    void signup_WithValidInput_ShouldSucceed() {

        // given
        UserRequest.SignUpUser request = new UserRequest.SignUpUser(
                "newUser", "password123", "slackId", "홍길동",
                "01012341234", "new@ex.com", UserRoleEnum.MASTER, DeliveryManagerRoleEnum.COMPANY, hubId
        );

        given(userRepository.findByUserName("newUser")).willReturn(Optional.empty());
        given(userRepository.findByEmail("new@ex.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPw");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserResponse.SignUpUser response = userService.signup(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userName()).isEqualTo("newUser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 username으로 회원가입 시 예외 발생")
    void signup_WithDuplicateUsername_ShouldThrowException() {

        // given
        UserRequest.SignUpUser request = new UserRequest.SignUpUser(
                "dupUser", "pw", "slackId", "홍길동",
                "01012345678", "dup@ex.com", UserRoleEnum.DELIVERY_MANAGER, DeliveryManagerRoleEnum.COMPANY, hubId
        );
        given(userRepository.findByUserName("dupUser")).willReturn(Optional.of(mock(User.class)));

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("중복된 사용자 ID가 존재합니다.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인된 사용자의 정보가 존재하면 반환한다")
    void getUserInfo_WhenUserExists_ShouldReturnUser() {

        // given
        User user = mock(User.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        given(userDetails.getId()).willReturn(userId);
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
        given(user.getUserName()).willReturn("testUser");

        // when
        UserResponse.GetUser response = userService.getUserInfo(userDetails);

        // then
        assertThat(response).isNotNull();
        verify(userRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("본인 탈퇴 시 softDeleteByUserId가 호출된다")
    void deleteSelf_WhenUserExists_ShouldSoftDelete() {

        // given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getId()).willReturn(userId);

        User mockUser = mock(User.class);
        given(mockUser.getUserId()).willReturn(userId);

        given(userRepository.findByUserId(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.softDeleteByUserId(eq(userId), any(LocalDateTime.class))).willReturn(1);

        // when
        userService.deleteSelf(userDetails);

        // then
        verify(userRepository).softDeleteByUserId(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("이미 탈퇴했거나 존재하지 않는 유저 탈퇴 시 예외 발생")
    void deleteSelf_WhenUserNotFound_ShouldThrowException() {

        // given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getId()).willReturn(userId);

        // 조회 시 회원 없음
        given(userRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteSelf(userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 탈퇴했거나 존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("회원 상태가 PENDING일 때 승인 처리 시 상태가 변경된다")
    void updateUserStatus_WhenPending_ShouldChangeStatus() {

        // given
        User user = mock(User.class);
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
        given(user.getStatus()).willReturn(UserStatusEnum.PENDING);

        // when
        userService.updateUserStatus(userId, UserStatusEnum.APPROVE);

        // then
        verify(user).updateStatus(UserStatusEnum.APPROVE);
    }

    @Test
    @DisplayName("이미 승인된 회원은 상태를 변경할 수 없다")
    void updateUserStatus_WhenNotPending_ShouldThrowException() {

        // given
        User user = mock(User.class);
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
        given(user.getStatus()).willReturn(UserStatusEnum.APPROVE);

        // when & then
        assertThatThrownBy(() -> userService.updateUserStatus(userId, UserStatusEnum.REJECTED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("대기중인 회원만 상태를 변경할 수 있습니다.");
        verify(user, never()).updateStatus(any());
    }
}
