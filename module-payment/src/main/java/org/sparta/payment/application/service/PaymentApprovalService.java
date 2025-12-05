package org.sparta.payment.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;

import org.sparta.payment.application.command.payment.PaymentApprovalCommand;
import org.sparta.payment.application.dto.PaymentApprovalResult;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentApprovalService {

    // private final PgClient pgClient; // 실제 PG 연동 시 주입

    @Transactional
    public PaymentApprovalResult approve(PaymentApprovalCommand command, UUID userId) {
        /*log.info("[PaymentApproval] PG 승인 요청 시작. orderId={}, userId={}, pgToken={}, amountPayable={}",
                command.orderId(), userId, command.pgToken(), command.amountPayable());*/

        Long amountPayable = command.amountPayable();
        if (amountPayable == null || amountPayable < 0) {
            throw new BusinessException(PaymentErrorType.INVALID_AMOUNT);
        }
        if (command.pgToken() == null || command.pgToken().isBlank()) {
            throw new BusinessException(PaymentErrorType.INVALID_REQUEST);
        }

        // 🔹 여기서 실제라면:
        //    pgClient.approve(command.pgToken(), command.amountPayable(), ...);
        //    PG에서 paymentKey, status 등을 받아옴.
        // 🔹 지금은 mock 승인을 구현:

        boolean approved = true; // mock: 항상 성공

        if (!approved) {
            //log.warn("[PaymentApproval] PG 승인 실패. orderId={}", command.orderId());
            // 필요시 failureCode, failureMessage 채워서 반환하는 방식으로도 가능
            throw new BusinessException(PaymentErrorType.PAYMENT_APPROVAL_FAILED);
        }

        String paymentKey = UUID.randomUUID().toString();
        LocalDateTime approvedAt = LocalDateTime.now();

        //log.info("[PaymentApproval] PG 승인 성공. orderId={}, paymentKey={}", command.orderId(), paymentKey);

        return PaymentApprovalResult.success(
                command.orderId(),
                paymentKey,
                approvedAt
        );
    }
}
