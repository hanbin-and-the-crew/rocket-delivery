package org.sparta.user.support.fixtures;

import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.enums.UserStatusEnum;

import java.util.UUID;

public final class UserFixture {
    public static UUID hubId() {
        return UUID.fromString("10000000-0000-0000-0000-000000000001");
    }

    public static User createPendingUser() {
        return User.create(
                "testuser", "securePass123!", "testId", "John",
                "01012341234", "test@example.com", UserRoleEnum.MASTER, hubId()
        );
    }

    public static User createUser(String userName, String email, UserStatusEnum status) {
        User user = User.create(
                userName, "pw123!", "slack01", "홍길동",
                "01011112222", email, UserRoleEnum.MASTER, hubId()
        );
        user.updateStatus(status);
        return user;
    }
}
