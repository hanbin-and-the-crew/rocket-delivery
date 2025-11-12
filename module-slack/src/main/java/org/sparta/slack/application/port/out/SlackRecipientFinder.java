package org.sparta.slack.application.port.out;

import org.sparta.slack.user.domain.entity.UserSlackView;
import org.sparta.slack.user.domain.enums.UserRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Slack 알림 파이프라인에서 사용할 수신자 조회 추상화
 */
public interface SlackRecipientFinder {

    /**
     * 지정된 역할 집합 중에서 승인된 상태의 사용자 전체를 조회합니다.
     */
    List<UserSlackView> findApprovedByRoles(Set<UserRole> roles);

    /**
     * 지정된 허브 ID + 역할 조건을 만족하는 승인된 사용자만 조회합니다.
     */
    List<UserSlackView> findApprovedByHubAndRoles(UUID hubId, Set<UserRole> roles);
}
