package org.sparta.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.sparta.common.error.BusinessException;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.user.domain.error.UserErrorType;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId; // PK, userName을 PK로 설정하지 않은 이유는 로그인용 ID는 변경될 수 있는 값이기 때문ㅇ

    @Column(name = "user_name", length = 100, nullable = false, unique = true)
    private String userName; // 로그인용 이름, ID (VO)

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "slack_id")
    private String slackId;

    @Column(name = "real_name", nullable = false)
    private String realName; // 실명

    @Column(name = "user_phone_number", nullable = false, length = 20)
    private String userPhoneNumber; // 전화번호 (VO)

    @Column(name = "email", nullable = false, unique = true)
    private String email; // 이메일 (VO)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatusEnum status; // 회원가입 대기/승인 상태

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRoleEnum role; // 사용자 역할

    @Column(name = "hub_id", nullable = false)
    private UUID hubId; // 허브 UUID (회원가입 시 소속 허브 필요)

    /**
     * 주문 생성 팩토리 메서드
     * 모든 비즈니스 규칙을 여기서 검증
     */
    public static User create(
            String userName, String password, String slackId, String realName,
            String userPhoneNumber, String email, UserRoleEnum role, UUID hubId) {

        // 비즈니스 규칙 검증
        validateUserName(userName);
        validateHubId(hubId);
        validatePassword(password);
        validateSlackId(slackId);
        validateEmail(email);

        // User 엔티티 생성
        User user = new User();
        user.userName = userName;
        user.password = password;
        user.slackId = slackId;
        user.realName = realName;
        user.userPhoneNumber = userPhoneNumber;
        user.email = email;
        user.status = UserStatusEnum.PENDING;
        user.role = role;
        user.hubId = hubId;

        return user;
    }

    /**
     * 유효성 검증
     */
    private static void validateUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new BusinessException(UserErrorType.USERNAME_REQUIRED);
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(UserErrorType.PASSWORD_REQUIRED);
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(UserErrorType.EMAIL_REQUIRED);
        }
    }

    private static void validateSlackId(String slackId) {
        if (slackId == null || slackId.isBlank()) {
            throw new BusinessException(UserErrorType.SLACK_ID_REQUIRED);
        }
    }

    private static void validateHubId(UUID hubId) {
        if (hubId == null) {
            throw new BusinessException(UserErrorType.HUB_ID_REQUIRED);
        }
    }

    public void updateUserName(String userName) {this.userName = userName;}
    public void updatePassword(String encodedPassword) {this.password = encodedPassword;}
    public void updateRealName(String realName) {this.realName = realName;}
    public void updatePhoneNumber(String userPhoneNumber) {this.userPhoneNumber = userPhoneNumber;}
    public void updateEmail(String newEmail) {this.email = newEmail;}
    public void updateStatus(UserStatusEnum newStatus) {
        if (this.status != UserStatusEnum.PENDING) {
            throw new BusinessException(UserErrorType.INVALID_STATUS_CHANGE);
        }
        this.status = newStatus;
    }
    public void updateRole(UserRoleEnum newRole) {this.role = newRole;}
    public void updateSlack(String slackId) {this.slackId = slackId;}
    public void updateHub(UUID hubId) {this.hubId = hubId;}
}