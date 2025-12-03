package org.sparta.user.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.event.EventPublisher;
import org.sparta.user.application.service.UserService;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.DeliveryManagerRoleEnum;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.event.publisher.UserCreatedEvent;
import org.sparta.user.infrastructure.security.CustomUserDetailsService;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.sparta.user.presentation.dto.response.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserEventPublisherTest {

    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID hubId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("UserCreatedEvent 발행 시 KafkaTemplate.send가 호출된다")
    void publishUserCreatedEvent_ShouldSendToKafka() {

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
        verify(eventPublisher).publishExternal(any(UserCreatedEvent.class));
    }
}