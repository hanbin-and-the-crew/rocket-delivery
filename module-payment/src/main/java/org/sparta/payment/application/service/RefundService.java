package org.sparta.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.payment.application.command.refund.RefundCreateCommand;

import org.sparta.payment.application.command.refund.RefundGetByIdCommand;
import org.sparta.payment.application.command.refund.RefundGetByPaymentIdCommand;
import org.sparta.payment.application.dto.RefundDetailResult;
import org.sparta.payment.application.dto.RefundListResult;
import org.sparta.payment.domain.entity.Refund;
import org.sparta.payment.domain.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


import org.sparta.payment.domain.error.PaymentErrorType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;

    public RefundDetailResult getRefund(RefundGetByIdCommand command) {
        Refund refund = getRefundEntity(command.refundId());
        return RefundDetailResult.from(refund);
    }

    public RefundListResult getRefundsByPaymentId(RefundGetByPaymentIdCommand command) {
        List<Refund> list = refundRepository.findByPaymentId(command.paymentId());
        return RefundListResult.from(list);
    }

    @Transactional
    public RefundDetailResult createRefund(RefundCreateCommand command) {
        Refund refund = Refund.request(
                command.paymentId(),
                command.amount(),
                command.reason()
        );
        Refund saved = refundRepository.save(refund);
        return RefundDetailResult.from(saved);
    }

    // ===== 내부 헬퍼 =====

    private Refund getRefundEntity(UUID refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(PaymentErrorType.REFUND_NOT_FOUND));
    }
}
