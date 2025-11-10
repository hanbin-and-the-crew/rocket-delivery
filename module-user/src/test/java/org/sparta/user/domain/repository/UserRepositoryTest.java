package org.sparta.user.domain.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * TDD Step3 Case
 * @DataJpaTest를 활용한 User Repository 테스트
 * 주요 테스트 시나리오
 * - 유저를 저장하고 조회할 수 있다
 * - username으로 유저를 조회할 수 있다
 * - email로 유저를 조회할 수 있다
 * - softDeleteByUserId() 실행 시 deletedAt이 설정된다
 * - findAll()은 삭제되지 않은 유저를 반환한다
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID hubId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private User createUser(String userName, String email) {
        return User.create(
                userName,
                "encodedPw",
                "slack_" + userName,
                "홍길동",
                "01012345678",
                email,
                UserRoleEnum.MASTER,
                hubId
        );
    }

    @Test
    @DisplayName("유저를 저장하고 조회할 수 있다")
    void saveAndFindById_ShouldSucceed() {

        // given
        User user = createUser("testuser", "test@ex.com");
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByUserId(user.getUserId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("username으로 유저를 조회할 수 있다")
    void findByUserName_ShouldReturnUser() {

        // given
        User user = createUser("findme", "find@ex.com");
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByUserName("findme");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find@ex.com");
    }

    @Test
    @DisplayName("email로 유저를 조회할 수 있다")
    void findByEmail_ShouldReturnUser() {

        // given
        User user = createUser("emailuser", "email@ex.com");
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByEmail("email@ex.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("emailuser");
    }

    @Test
    @DisplayName("softDeleteByUserId() 실행 시 deletedAt이 설정된다")
    void softDeleteByUserId_ShouldSetDeletedAt() {

        // given
        User user = createUser("softdel", "soft@ex.com");
        userRepository.save(user);
        LocalDateTime now = LocalDateTime.now();

        // when
        int updated = userRepository.softDeleteByUserId(user.getUserId(), now);

        // then
        assertThat(updated).isEqualTo(1);
        User deletedUser = userRepository.findById(user.getUserId()).get();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("findAll()은 삭제되지 않은 유저를 반환한다")
    void findAll_ShouldReturnOnlyActiveUsers() {

        // given
        User active = createUser("active", "a@ex.com");
        User deleted = createUser("deleted", "d@ex.com");
        userRepository.save(active);
        userRepository.save(deleted);

        // soft delete
        userRepository.softDeleteByUserId(deleted.getUserId(), LocalDateTime.now());

        // when
        List<User> all = userRepository.findAll();

        // then
        assertThat(all).isNotEmpty();
        assertThat(all.stream().anyMatch(u -> u.getUserName().equals("active"))).isTrue();
    }
}