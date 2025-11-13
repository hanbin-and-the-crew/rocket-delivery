package org.sparta.slack.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.notification.command.OrderDeadlineCommand;
import org.sparta.common.event.slack.OrderDeadlineRequestedEvent;
import org.sparta.slack.application.mapper.OrderDeadlineCommandMapper;
import org.sparta.slack.application.service.notification.OrderDeadlineFacade;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 도메인 이벤트를 수신해 Slack 발송 시한 알림을 실행하는 리스너.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadlineEventListener {

    private final OrderDeadlineFacade orderDeadlineFacade;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderDeadlineRequestedEvent event) {
        if (event == null || event.payload() == null) {
            log.debug("OrderDeadlineRequestedEvent payload가 없어 처리하지 않습니다.");
            return;
        }

        try {
            OrderDeadlineCommand command = OrderDeadlineCommandMapper.from(event.payload());
            orderDeadlineFacade.notify(command);
        } catch (Exception ex) {
            log.error("OrderDeadlineRequestedEvent 처리 중 오류 - eventId={}, orderId={}",
                    event.eventId(), event.payload().orderId(), ex);
        }
    }
}
