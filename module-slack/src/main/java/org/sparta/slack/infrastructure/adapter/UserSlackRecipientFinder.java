package org.sparta.slack.infrastructure.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.port.out.SlackRecipientFinder;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.enums.UserStatus;
import org.sparta.slack.domain.repository.UserSlackViewRepository;
import org.springframework.stereotype.Component;

/**
 * UserSlackView 기반의 Slack 수신자 조회 구현체
 */
@Component
@RequiredArgsConstructor
public class UserSlackRecipientFinder implements SlackRecipientFinder {

    private final UserSlackViewRepository userSlackViewRepository;

    @Override
    public List<UserSlackView> findApprovedByRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return userSlackViewRepository.findAllByRolesAndStatus(roles, UserStatus.APPROVE);
    }

    @Override
    public List<UserSlackView> findApprovedByHubAndRoles(UUID hubId, Set<UserRole> roles) {
        if (hubId == null || roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return userSlackViewRepository.findAllByHubIdAndRolesAndStatus(hubId, roles, UserStatus.APPROVE);
    }
}
