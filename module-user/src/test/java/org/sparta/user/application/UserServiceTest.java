package org.sparta.user.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.sparta.common.error.BusinessException;
import org.sparta.user.application.service.UserService;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Import(UserService.class)
@ActiveProfiles("test")
public class UserServiceTest {

    private static final UUID hubId = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, passwordEncoder, customUserDetailsService
        );
    }


    @Test
    @DisplayName("허브를 생성하면 DB에 저장되고, 생성된 허브 정보를 반환한다")
    void createHub_success() {
        // given
        UserCreateRequest request = new HubCreateRequest(
                "경기 북부 허브",
                "경기도 고양시 덕양구 무슨로 123",
                37.6532,
                126.8321
        );

        // when
        HubCreateResponse response = hubService.createHub(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("경기 북부 허브");
        assertThat(response.status()).isEqualTo("ACTIVE");

        // DB 검증
        Optional<Hub> found = hubRepository.findById(response.hubId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("경기 북부 허브");
        assertThat(found.get().getStatus()).isEqualTo(HubStatus.ACTIVE);
    }


    @Test
    @DisplayName("존재하지 않는 유저 조회 시 예외 발생")
    void getUser_WithInvalidId_ShouldThrowException() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorType.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("유저 비활성화 시 상태가 INACTIVE로 변경된다")
    void deactivateUser_ShouldChangeStatus() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.create(
                "testuser",
                "securePass123!",
                "slackId",
                "John",
                "01012341234",
                "test@example.com",
                UserRoleEnum.USER,
                hubId
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.deactivateUser(userId);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatusEnum.INACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("중복된 userName으로 회원가입 시 예외 발생")
    void createUser_WithDuplicateUserName_ShouldThrowException() {
        // given
        given(userRepository.existsByUserName("testuser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(
                "testuser",
                "securePass123!",
                "slackId",
                "John",
                "01012341234",
                "test@example.com",
                UserRoleEnum.USER,
                hubId
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorType.USERNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("허브 이름이 중복될 경우 예외를 발생시킨다")
    void createHub_duplicateName_fail() {
        // given
        HubCreateRequest request = new HubCreateRequest(
                "서울 허브",
                "서울특별시 강남구 무슨로 45",
                37.55,
                127.01
        );
        hubService.createHub(request);

        // when & then
        assertThatThrownBy(() -> hubService.createHub(request))
                .isInstanceOf(DuplicateHubNameException.class)
                .hasMessageContaining("이미 존재하는 허브명");
    }

    /**
     * 허브 수정 테스트
     * - 성공
     */
    @Test
    @DisplayName("허브 수정 성공 - 주소, 위도, 경도 변경")
    void updateHub_success() {
        // given
        HubCreateRequest createRequest = new HubCreateRequest(
                "서울 허브",
                "서울시 강남구 테헤란로 123",
                37.55,
                127.03
        );
        HubCreateResponse created = hubService.createHub(createRequest);

        HubUpdateRequest updateRequest = new HubUpdateRequest(
                "서울 허브",
                "서울시 송파구 중대로 77",
                37.51,
                127.10,
                HubStatus.ACTIVE
        );

        // when
        HubResponse updated = hubService.updateHub(created.hubId(), updateRequest);

        // then
        assertThat(updated).isNotNull();
        assertThat(updated.address()).isEqualTo("서울시 송파구 중대로 77");
        assertThat(updated.latitude()).isEqualTo(37.51);
        assertThat(updated.longitude()).isEqualTo(127.10);
    }

    /**
     * 허브 삭제 테스트
     * - 성공
     * - 재삭제 예외
     * - 미존재 예외
     */
    @Test
    @DisplayName("허브를 삭제하면 status가 INACTIVE로 변경된다")
    void deleteHub_success() {
        // given
        HubCreateRequest request = new HubCreateRequest(
                "삭제 테스트 허브",
                "서울특별시 영등포구 여의대로 10",
                37.52,
                126.93
        );
        HubCreateResponse created = hubService.createHub(request);

        // when
        hubService.deleteHub(created.hubId());

        // then
        Hub deletedHub = hubRepository.findById(created.hubId()).orElseThrow();
        assertThat(deletedHub.getStatus()).isEqualTo(HubStatus.INACTIVE);
        assertThat(deletedHub.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 삭제된 허브를 다시 삭제하면 예외가 발생한다")
    void deleteHub_alreadyDeleted_fail() {
        // given
        HubCreateRequest request = new HubCreateRequest(
                "중복 삭제 허브",
                "서울시 강서구 허브로 88",
                37.56,
                126.82
        );
        HubCreateResponse created = hubService.createHub(request);
        hubService.deleteHub(created.hubId());

        // when & then
        assertThatThrownBy(() -> hubService.deleteHub(created.hubId()))
                .isInstanceOf(AlreadyDeletedHubException.class)
                .hasMessageContaining("이미 삭제된 허브입니다");
    }

    @Test
    @DisplayName("존재하지 않는 허브 삭제 시 HubNotFoundException 발생")
    void deleteHub_notFound_fail() {
        // given
        UUID randomId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> hubService.deleteHub(randomId))
                .isInstanceOf(HubNotFoundException.class)
                .hasMessageContaining("Hub not found");
    }


    /**
     * 허브 조회 테스트
     *
     */
    @Test
    @DisplayName("사용자 조회 - ACTIVE만 반환")
    void getActiveHubsForUser_onlyActiveReturned() {
        hubRepository.save(Hub.create("A1","addr",1.0,1.0)); // 기본 ACTIVE
        Hub inactive = Hub.create("I1","addr",2.0,2.0);
        inactive.markDeleted("tester"); // INACTIVE 처리
        hubRepository.save(inactive);

        var list = new HubService(hubRepository).getActiveHubsForUser();

        assertThat(list).extracting(HubResponse::name).contains("A1");
        assertThat(list).extracting(HubResponse::name).doesNotContain("I1");
    }

    @Test
    @DisplayName("사용자 단건 조회 - INACTIVE는 404")
    void getActiveHubByIdForUser_inactiveReturnsNotFound() {
        Hub inactive = Hub.create("I2","addr",2.0,2.0);
        inactive.markDeleted("tester");
        Hub saved = hubRepository.save(inactive);

        assertThatThrownBy(() ->
                new HubService(hubRepository).getActiveHubByIdForUser(saved.getHubId())
        ).isInstanceOf(HubNotFoundException.class);
    }

    @Test
    @DisplayName("운영자 조회 - ALL/ACTIVE/INACTIVE 필터")
    void getHubsForAdmin_allAndFiltered() {
        Hub a1 = hubRepository.save(Hub.create("A1","addr",1.0,1.0));
        Hub i1 = Hub.create("I1","addr",2.0,2.0);
        i1.markDeleted("tester");
        hubRepository.save(i1);

        HubService svc = new HubService(hubRepository);

        var all = svc.getHubsForAdmin("ALL");
        var act = svc.getHubsForAdmin("ACTIVE");
        var inact = svc.getHubsForAdmin("INACTIVE");

        assertThat(all).hasSize(2);
        assertThat(act).extracting(HubResponse::name).containsExactly("A1");
        assertThat(inact).extracting(HubResponse::name).containsExactly("I1");
    }
}
