package org.sparta.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.payment.application.command.attempt.*;
import org.sparta.payment.application.dto.PaymentAttemptDetailResult;
import org.sparta.payment.application.dto.PaymentAttemptListResult;
import org.sparta.payment.domain.entity.PaymentAttempt;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.sparta.payment.domain.repository.PaymentAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentAttemptService {

    private final PaymentAttemptRepository attemptRepository;

    @Transactional
    public PaymentAttemptDetailResult createAttempt(PaymentAttemptCreateCommand command) {
        PaymentAttempt attempt = PaymentAttempt.start(
                command.paymentId(),
                command.attemptNo(),
                command.requestPayload()
        );
        PaymentAttempt saved = attemptRepository.save(attempt);
        return PaymentAttemptDetailResult.from(saved);
    }

    public PaymentAttemptDetailResult getAttempt(PaymentAttemptGetByIdCommand command) {
        PaymentAttempt attempt = getAttemptEntity(command.paymentAttemptId());
        return PaymentAttemptDetailResult.from(attempt);
    }

    public PaymentAttemptListResult getAttemptsByPaymentId(PaymentAttemptGetByPaymentIdCommand command) {
        List<PaymentAttempt> list = attemptRepository.findByPaymentId(command.paymentId());
        return PaymentAttemptListResult.from(list);
    }

    @Transactional
    public PaymentAttemptDetailResult markSuccess(PaymentAttemptMarkSuccessCommand command) {
        PaymentAttempt attempt = getAttemptEntity(command.paymentAttemptId());
        attempt.success(command.pgTransactionId(), command.responsePayload());
        PaymentAttempt saved = attemptRepository.save(attempt);
        return PaymentAttemptDetailResult.from(saved);
    }

    @Transactional
    public PaymentAttemptDetailResult markFail(PaymentAttemptMarkFailCommand command) {
        PaymentAttempt attempt = getAttemptEntity(command.paymentAttemptId());
        attempt.fail(command.errorCode(), command.errorMessage(), command.responsePayload());
        PaymentAttempt saved = attemptRepository.save(attempt);
        return PaymentAttemptDetailResult.from(saved);
    }

    @Transactional
    public PaymentAttemptDetailResult markTimeout(PaymentAttemptMarkTimeoutCommand command) {
        PaymentAttempt attempt = getAttemptEntity(command.paymentAttemptId());
        attempt.timeout();
        PaymentAttempt saved = attemptRepository.save(attempt);
        return PaymentAttemptDetailResult.from(saved);
    }

    // ===== 내부 헬퍼 =====

    private PaymentAttempt getAttemptEntity(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(PaymentErrorType.ATTEMPT_NOT_FOUND));
    }
}
