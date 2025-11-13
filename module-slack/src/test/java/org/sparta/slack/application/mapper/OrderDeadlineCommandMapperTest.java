package org.sparta.slack.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.slack.OrderDeadlineRequestedEvent;
import org.sparta.slack.application.command.OrderDeadlineCommand;
import org.sparta.slack.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies OrderDeadlineCommandMapper converts event payloads to commands. */
class OrderDeadlineCommandMapperTest {

    @Test
    @DisplayName("OrderDeadlineRequestedEvent.Payload를 Command로 변환한다")
    void from_WithValidPayload_ReturnsCommand() {
        OrderDeadlineRequestedEvent.Payload payload = new OrderDeadlineRequestedEvent.Payload(
                UUID.randomUUID(),
                "ORD-1",
                "홍길동",
                "hong@test.com",
                LocalDateTime.of(2024, 7, 1, 9, 0),
                "상품",
                3,
                "급함",
                UUID.randomUUID(),
                "서울허브",
                "서울시 중구",
                "서울 허브",
                UUID.randomUUID(),
                "A업체",
                "경기도 성남시",
                "A업체",
                "대전 경유",
                LocalDateTime.of(2024, 7, 2, 18, 0),
                9,
                18,
                Set.of("HUB_MANAGER"),
                "매니저",
                "manager@test.com"
        );

        OrderDeadlineCommand command = OrderDeadlineCommandMapper.from(payload);

        assertThat(command.orderId()).isEqualTo(payload.orderId());
        assertThat(command.origin()).isEqualTo("서울 허브");
        assertThat(command.destination()).isEqualTo("A업체");
        assertThat(command.transitPath()).isEqualTo(payload.transitPath());
        assertThat(command.targetRoles()).containsExactly(UserRole.HUB_MANAGER);
    }
}
