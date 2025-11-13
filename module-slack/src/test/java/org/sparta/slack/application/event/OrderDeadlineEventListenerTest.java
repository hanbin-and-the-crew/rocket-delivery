package org.sparta.slack.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.event.slack.OrderDeadlineRequestedEvent;
import org.sparta.slack.application.notification.command.OrderDeadlineCommand;
import org.sparta.slack.application.service.notification.OrderDeadlineFacade;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** Verifies OrderDeadlineEventListener mapping to facade. */
@ExtendWith(MockitoExtension.class)
class OrderDeadlineEventListenerTest {

    @Mock
    private OrderDeadlineFacade orderDeadlineFacade;

    @InjectMocks
    private OrderDeadlineEventListener listener;

    @Test
    @DisplayName("OrderDeadlineRequestedEvent를 수신하면 Facade를 호출한다")
    void handle_WhenEventArrives_InvokesFacade() {
        OrderDeadlineRequestedEvent.Payload payload = new OrderDeadlineRequestedEvent.Payload(
                UUID.randomUUID(),
                "ORD-99",
                "홍길동",
                "hong@test.com",
                LocalDateTime.of(2024, 7, 1, 8, 0),
                "테스트 상품",
                5,
                "비고",
                UUID.randomUUID(),
                "서울허브",
                "서울시 중구",
                null,
                UUID.randomUUID(),
                "B업체",
                "부산시 사하구",
                null,
                "대전경유",
                LocalDateTime.of(2024, 7, 2, 18, 0),
                9,
                18,
                Set.of("HUB_MANAGER"),
                "김매니저",
                "manager@test.com"
        );
        OrderDeadlineRequestedEvent event = new OrderDeadlineRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                payload
        );

        listener.handle(event);

        ArgumentCaptor<OrderDeadlineCommand> captor = ArgumentCaptor.forClass(OrderDeadlineCommand.class);
        verify(orderDeadlineFacade).notify(captor.capture());
        OrderDeadlineCommand command = captor.getValue();
        assertThat(command.orderNumber()).isEqualTo("ORD-99");
        assertThat(command.destination()).isEqualTo("B업체");
        assertThat(command.transitPath()).isEqualTo("대전경유");
    }
}
