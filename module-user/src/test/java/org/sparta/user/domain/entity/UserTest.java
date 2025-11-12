package org.sparta.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sparta.common.error.BusinessException;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;
import org.sparta.user.support.fixtures.UserFixture;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * TDD Step1 Case
 * User 도메인 단위 테스트
 * 생성 시점에 반드시 수행해야하는 점(크게 5가지만)
 * - 유효한 입력을 넣으면 유저가 생성되는가
 * - username이 빈 문자열로 들어오면 어떻게 되는가
 * - email이 빈 문자열로 들어오면 어떻게 되는가
 * - slackId가 빈 문자열로 들어오면 어떻게 되는가
 * - 기본 status는 Pending 상태로 들어오는가
 */
public class UserTest {
    private static final UUID hubId = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("유효한 입력으로 유저를 생성하면 성공한다")
    void create_WithValidInput_ShouldSucceed() {

        // given
        String userName = "testuser";
        String email = "test@example.com";
        String password = "securePass123!";
        String slackId = "testId";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserRoleEnum role = UserRoleEnum.MASTER;

        // when
        User user = UserFixture.createPendingUser();

        // then
        assertThat(user).isNotNull();
        assertThat(user.getUserName()).isEqualTo(userName);
        assertThat(user.getUserPhoneNumber()).isEqualTo(userPhoneNumber);
        assertThat(user.getSlackId()).isEqualTo(slackId);
        assertThat(user.getRealName()).isEqualTo(realName);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getRole()).isEqualTo(role);
    }

    @Test
    @DisplayName("username이 null이거나 빈 문자열이면 예외가 발생한다")
    void create_WithInvalidUsername_ShouldThrowException() {

        // given
        String userName = "";
        String email = "test@example.com";
        String password = "securePass123!";
        String slackId = "testId";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserRoleEnum role = UserRoleEnum.MASTER;
        UUID hubId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> User.create(
                userName, password, slackId, realName,
                userPhoneNumber, email, role, hubId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("username은 필수입니다.");
    }

    @Test
    @DisplayName("email이 null이거나 빈 문자열이면 예외가 발생한다")
    void create_WithInvalidEmail_ShouldThrowException() {

        // given
        String userName = "testuser";
        String email = "";
        String password = "securePass123!";
        String slackId = "testId";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserStatusEnum status = UserStatusEnum.PENDING;
        UserRoleEnum role = UserRoleEnum.MASTER;
        UUID hubId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> User.create(
                        userName, password, slackId, realName,
                        userPhoneNumber, email, role, hubId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email은 필수입니다.");
    }

    @Test
    @DisplayName("slackId가 null이거나 빈 문자열이면 예외가 발생한다")
    void create_WithInvalidSlackId_ShouldThrowException() {

        // given
        String userName = "testuser";
        String email = "test@example.com";
        String password = "securePass123!";
        String slackId = "";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserStatusEnum status = UserStatusEnum.PENDING;
        UserRoleEnum role = UserRoleEnum.MASTER;
        UUID hubId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> User.create(
                userName, password, slackId, realName,
                userPhoneNumber, email, role, hubId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("슬랙 ID는 필수입니다.");
    }

    @Test
    @DisplayName("User 생성하면 기본 Status는 Pending(회원 가입 대기) 상태이다")
    void create_ShouldSetDefaultStatus() {

        // given
        String userName = "testuser";
        String email = "test@example.com";
        String password = "securePass123!";
        String slackId = "testId";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserRoleEnum role = UserRoleEnum.MASTER;
        UUID hubId = UUID.randomUUID();

        // when
        User user = User.create(
                userName, password, slackId, realName,
                userPhoneNumber, email, role, hubId);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatusEnum.PENDING);
    }

    @Test
    @DisplayName("updatePassword, updateEmail, updatePhoneNumber가 정상적으로 작동한다")
    void updateFields_ShouldUpdateProperly() {

        // given
        String userName = "testuser";
        String email = "test@example.com";
        String password = "securePass123!";
        String slackId = "testId";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserRoleEnum role = UserRoleEnum.MASTER;
        UUID hubId = UUID.randomUUID();

        User user = User.create(
                userName, password, slackId, realName,
                userPhoneNumber, email, role, hubId);

        // when
        user.updatePassword("newPw123!");
        user.updateEmail("new@example.com");
        user.updatePhoneNumber("01099998888");

        // then
        assertThat(user.getPassword()).isEqualTo("newPw123!");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getUserPhoneNumber()).isEqualTo("01099998888");
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 유저는 승인/거절 시 예외가 발생한다")
    void updateStatus_WhenNotPending_ShouldThrowException() {

        // given
        String userName = "testuser";
        String email = "test@example.com";
        String password = "securePass123!";
        String slackId = "testId";
        String realName = "John";
        String userPhoneNumber = "01012341234";
        UserRoleEnum role = UserRoleEnum.MASTER;
        UUID hubId = UUID.randomUUID();

        User user = User.create(
                userName, password, slackId, realName,
                userPhoneNumber, email, role, hubId);

        user.updateStatus(UserStatusEnum.APPROVE);

        // when & then
        assertThatThrownBy(() -> user.updateStatus(UserStatusEnum.REJECTED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("대기중인 회원만 상태를 변경할 수 있습니다.");
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, APPROVE, false",
            "PENDING, REJECTED, false",
            "APPROVE, REJECTED, true",
            "REJECTED, APPROVE, true"
    })
    @DisplayName("회원 상태 전환 경계 테스트")
    void updateStatus_BoundaryTest(String currentStatus, String nextStatus, boolean expectException) {

        UserStatusEnum current = UserStatusEnum.valueOf(currentStatus);
        UserStatusEnum next = UserStatusEnum.valueOf(nextStatus);

        // given
        User user = UserFixture.createPendingUser();

        // 현재 상태 세팅
        if (current != UserStatusEnum.PENDING) {
            user.updateStatus(current);
        }

        // when & then
        if (expectException) {
            assertThatThrownBy(() -> user.updateStatus(next))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("대기중인 회원만 상태를 변경할 수 있습니다.");
        } else {
            user.updateStatus(next);
            assertThat(user.getStatus()).isEqualTo(next);
        }
    }
}
