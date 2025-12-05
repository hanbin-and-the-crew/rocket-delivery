package org.sparta.payment.application.service;

import lombok.RequiredArgsConstructor;

import org.sparta.common.error.BusinessException;

import org.sparta.payment.application.command.outbox.*;
import org.sparta.payment.application.dto.PaymentOutboxDetailResult;
import org.sparta.payment.application.dto.PaymentOutboxListResult;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.enumeration.OutboxStatus;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentOutboxService {

    private final PaymentOutboxRepository outboxRepository;

    @Transactional
    public PaymentOutboxDetailResult createOutbox(PaymentOutboxCreateCommand command) {
        PaymentOutbox outbox = PaymentOutbox.ready(
                command.aggregateType(),
                command.aggregateId(),
                command.eventType(),
                command.payload()
        );
        PaymentOutbox saved = outboxRepository.save(outbox);
        return PaymentOutboxDetailResult.from(saved);
    }

    public PaymentOutboxDetailResult getOutbox(PaymentOutboxGetByIdCommand command) {
        PaymentOutbox outbox = getOutboxEntity(command.paymentOutboxId());
        return PaymentOutboxDetailResult.from(outbox);
    }

    public PaymentOutboxListResult getReadyOutboxes(PaymentOutboxGetReadyCommand command) {
        // limit 파라미터는 지금은 repository에 Top100만 있지만, 추후 확장 여지
        List<PaymentOutbox> list = outboxRepository.findTop100ByStatus(OutboxStatus.READY);
        return PaymentOutboxListResult.from(list);
    }

    @Transactional
    public PaymentOutboxDetailResult markSent(PaymentOutboxMarkSentCommand command) {
        PaymentOutbox outbox = getOutboxEntity(command.paymentOutboxId());
        outbox.markSent();
        PaymentOutbox saved = outboxRepository.save(outbox);
        return PaymentOutboxDetailResult.from(saved);
    }

    @Transactional
    public PaymentOutboxDetailResult markFailed(PaymentOutboxMarkFailedCommand command) {
        PaymentOutbox outbox = getOutboxEntity(command.paymentOutboxId());
        outbox.markFailed();
        PaymentOutbox saved = outboxRepository.save(outbox);
        return PaymentOutboxDetailResult.from(saved);
    }

    // ===== 내부 헬퍼 =====

    private PaymentOutbox getOutboxEntity(UUID id) {
        return outboxRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PaymentErrorType.OUTBOX_NOT_FOUND));
    }
}
