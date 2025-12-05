package org.sparta.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.sparta.common.event.EventEnvelope;

import org.sparta.payment.application.command.payment.*;

import org.sparta.payment.application.dto.PaymentDetailResult;
import org.sparta.payment.application.dto.PaymentListResult;
import org.sparta.payment.application.event.PaymentCompletedPayload;
import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.entity.Refund;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.sparta.payment.domain.repository.PaymentRepository;
import org.sparta.payment.domain.repository.RefundRepository;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.sparta.common.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;
    /**
     * 결제 생성 (REQUESTED 상태)
     */


    @Transactional
    public PaymentDetailResult storeCompletedPayment(PaymentCreateCommand command, String paymentKey) {
        // 0) 멱등 체크: 동일 paymentKey 로 이미 결제가 존재하면 그대로 반환
        return paymentRepository.findByPaymentKey(paymentKey)
                .map(PaymentDetailResult::from)
                .orElseGet(() -> {
                    // 1) 금액 검증
                    validateAmounts(command);

                    // 2) REQUESTED 상태로 결제 객체 생성
                    Payment payment = Payment.createRequested(
                            command.orderId(),
                            command.amountTotal(),
                            command.amountCoupon(),
                            command.amountPoint(),
                            command.amountPayable(),
                            command.methodType(),
                            command.pgProvider(),
                            command.currency(),
                            command.couponId(),
                            command.pointUsageId()
                    );

                    // 3) PG 승인 완료 반영 (status를 COMPLETED로, amountPaid/approvedAt 세팅)
                    payment.complete(paymentKey, command.amountPayable());

                    // 4) 저장
                    Payment saved = paymentRepository.save(payment);

                    // 5) PAYMENT_COMPLETED Outbox 이벤트 생성
                    createPaymentCompletedOutbox(saved);

                    return PaymentDetailResult.from(saved);
                });
    }
    private void validateAmounts(PaymentCreateCommand command) {
        if (command.amountTotal() == null || command.amountTotal() <= 0) {
            throw new BusinessException(PaymentErrorType.INVALID_AMOUNT);
        }
        long coupon = nvl(command.amountCoupon());
        long point = nvl(command.amountPoint());
        long payable = nvl(command.amountPayable());

        if (payable < 0) {
            throw new BusinessException(PaymentErrorType.INVALID_AMOUNT);
        }
        if (!command.amountTotal().equals(coupon + point + payable)) {
            throw new BusinessException(PaymentErrorType.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private void createPaymentCompletedOutbox(Payment payment) {
        PaymentCompletedPayload payload = PaymentCompletedPayload.from(payment);

        EventEnvelope<PaymentCompletedPayload> envelope =
                EventEnvelope.of(
                        "PAYMENT_COMPLETED",
                        "PAYMENT",
                        payment.getPaymentId().toString(),
                        "payment-service",
                        1,
                        payload
                );

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            //log.error("[PaymentService] PAYMENT_COMPLETED 이벤트 직렬화 실패. paymentId={}",payment.getPaymentId(), e);
            throw new BusinessException(PaymentErrorType.OUTBOX_PUBLISH_FAILED, "이벤트 직렬화 실패");
        }

        PaymentOutbox outbox = PaymentOutbox.ready(
                envelope.aggregateType(),
                payment.getPaymentId(),
                envelope.eventType(),
                json
        );
        paymentOutboxRepository.save(outbox);

        //log.info("[PaymentService] PAYMENT_COMPLETED Outbox 저장 완료. outboxId={}", outbox.getPaymentOutboxId());
    }
    /**
     * 결제 단건 조회
     */
    public PaymentDetailResult getPayment(PaymentGetByIdCommand command) {
        Payment payment = getPaymentEntity(command.paymentId());
        return PaymentDetailResult.from(payment);
    }

    /**
     * 주문 기준 결제 조회 (주문 1건당 결제 1건이라는 가정)
     */
    public PaymentDetailResult getPaymentByOrderId(PaymentGetByOrderIdCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new BusinessException(PaymentErrorType.PAYMENT_NOT_FOUND));
        return PaymentDetailResult.from(payment);
    }

    /**
     * 특정 상태의 결제 목록 조회
     */
    public PaymentListResult getPaymentsByStatus(PaymentGetByStatusCommand command) {
        List<Payment> payments = paymentRepository.findAllByStatus(command.status());
        return PaymentListResult.from(payments);
    }

    /**
     * 결제 전체 취소 처리
     * - Payment 상태를 CANCELED로 변경
     * - amountPaid를 0으로 세팅
     * - 환불 이력(전체 환불) 생성
     */
    @Transactional
    public PaymentDetailResult cancelPayment(PaymentCancelCommand command) {
        Payment payment = getPaymentEntity(command.paymentId());

        if (payment.getStatus() == PaymentStatus.CANCELED ||
                payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException(PaymentErrorType.PAYMENT_ALREADY_CANCELED);
        }

        Long refundAmount = payment.getAmountPaid();
        if (refundAmount == null || refundAmount <= 0) {
            refundAmount = payment.getAmountPayable();
        }

        Refund refund = Refund.request(payment.getPaymentId(), refundAmount, command.reason());
        refundRepository.save(refund);

        payment.cancelAll();
        Payment saved = paymentRepository.save(payment);

        return PaymentDetailResult.from(saved);
    }

    /**
     * 부분 환불 처리
     * - Refund 엔티티 추가
     * - Payment의 amountPaid 감소 및 상태 전이
     */
    @Transactional
    public PaymentDetailResult refundPartial(PaymentRefundPartialCommand command) {
        Payment payment = getPaymentEntity(command.paymentId());

        Long refundAmount = command.refundAmount();
        if (refundAmount == null || refundAmount <= 0) {
            throw new BusinessException(PaymentErrorType.REFUND_AMOUNT_INVALID);
        }

        payment.applyRefund(refundAmount);

        Refund refund = Refund.request(payment.getPaymentId(), refundAmount, command.reason());
        refundRepository.save(refund);

        Payment saved = paymentRepository.save(payment);
        return PaymentDetailResult.from(saved);
    }

    // ===== 내부 헬퍼 =====

    private Payment getPaymentEntity(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorType.PAYMENT_NOT_FOUND));
    }
}
