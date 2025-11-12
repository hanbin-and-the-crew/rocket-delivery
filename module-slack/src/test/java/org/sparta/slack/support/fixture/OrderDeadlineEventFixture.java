package org.sparta.slack.support.fixture;

import org.sparta.slack.shared.event.OrderDeadlineRequestedEvent;
import org.sparta.slack.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 주문 발송 시한 이벤트 샘플 데이터를 제공하는 Fixture.
 */
public final class OrderDeadlineEventFixture {

    private OrderDeadlineEventFixture() {
    }

    public static OrderDeadlineRequestedEvent.Payload payload(
            UUID orderId,
            UUID originHubId,
            UUID destinationCompanyId,
            Set<UserRole> targetRoles
    ) {
        return new OrderDeadlineRequestedEvent.Payload(
                orderId,
                "ORD-" + orderId.toString().substring(0, 4),
                "고객",
                "customer@test.com",
                LocalDateTime.of(2024, 7, 1, 10, 0),
                "테스트 상품",
                10,
                "긴급",
                originHubId,
                "서울허브",
                "서울시 중구",
                "서울 허브",
                destinationCompanyId,
                "A업체",
                "부산시 사하구",
                "A업체",
                "대전 경유",
                LocalDateTime.of(2024, 7, 3, 18, 0),
                9,
                18,
                targetRoles,
                "김매니저",
                "manager@test.com"
        );
    }
}
