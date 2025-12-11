package org.sparta.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.payment.application.command.refund.RefundCreateCommand;
import org.sparta.payment.application.command.refund.RefundGetByIdCommand;
import org.sparta.payment.application.command.refund.RefundGetByPaymentIdCommand;
import org.sparta.payment.application.command.refund.RefundPgWebhookCommand;
import org.sparta.payment.application.dto.RefundDetailResult;
import org.sparta.payment.application.dto.RefundListResult;
import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.entity.Refund;
import org.sparta.common.domain.OutboxStatus;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.sparta.payment.domain.repository.PaymentRepository;
import org.sparta.payment.domain.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;

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

        Payment payment = paymentRepository.findById(command.paymentId())
                        .orElseThrow(() -> new BusinessException(PaymentErrorType.PAYMENT_NOT_FOUND));

                // 기존 환불 총액 + 요청 환불 금액이 결제 금액을 초과하는지 검증
                        Long totalRefunded = refundRepository.findByPaymentId(command.paymentId())
                        .stream()
                        .mapToLong(Refund::getAmount)
                        .sum();
        if (totalRefunded + command.amount() > payment.getAmountPaid()) {
               throw new BusinessException(PaymentErrorType.REFUND_AMOUNT_EXCEEDED);
        }
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

    @Transactional
    public void handlePgRefundWebhook(RefundPgWebhookCommand command) {
        // 1) PG에서 온 paymentKey 로 Payment 조회
        Payment payment = paymentRepository.findByPaymentKey(command.paymentKey())
                .orElseThrow(() -> new BusinessException(PaymentErrorType.PAYMENT_NOT_FOUND));

        Long amount = command.amount();
        if (amount == null || amount <= 0) {
            throw new BusinessException(PaymentErrorType.INVALID_AMOUNT);
        }

        // 2) 기존 환불 총액 + 이번 환불 금액이 결제 금액을 초과하는지 검증
        Long totalRefunded = refundRepository.findByPaymentId(payment.getPaymentId())
                .stream()
                .mapToLong(Refund::getAmount)
                .sum();

        if (totalRefunded + amount > payment.getAmountPaid()) {
            throw new BusinessException(PaymentErrorType.REFUND_AMOUNT_EXCEEDED);
        }

        // 3) 상태에 따라 처리 분기
        //    - SUCCESS: Refund 레코드 생성 (실제 환불 완료 기록)
        //    - FAIL   : 일단 도메인 예외를 던져 상위에서 처리하거나,
        //              이후 Outbox 이벤트 발행 로직으로 확장 가능
        String status = command.status();
        if ("SUCCESS".equalsIgnoreCase(status)) {
            // PG 환불이 최종 성공한 케이스 → 환불 기록만 남긴다.
            Refund refund = Refund.request(
                    payment.getPaymentId(),
                    amount,
                    "PG_REFUND_WEBHOOK" // 필요 시 나중에 reason 필드 확장
            );
            refundRepository.save(refund);
        } else if ("FAIL".equalsIgnoreCase(status)) {
            // PAYMENT_REFUND_FAILED Outbox 이벤트 생성 (사가/보상 트랜잭션 트리거용)
            String payload = buildRefundFailedPayload(payment, command, amount);

            PaymentOutbox outbox = PaymentOutbox.create(
                    "PAYMENT",                          // aggregateType
                    payment.getPaymentId(),            // aggregateId
                    "PAYMENT_REFUND_FAILED",           // eventType
                    payload,                           // JSON payload
                    OutboxStatus.READY                 // 초기 상태
            );
            paymentOutboxRepository.save(outbox);
            return;
        } else {
            // 알 수 없는 status 값인 경우, 일단 무시하거나 로깅만 하고 종료
            // TODO: 필요 시 unknown status 에 대한 별도 모니터링/알림 로직 추가
            return;
        }
    }

    /**
     * PAYMENT_REFUND_FAILED 이벤트에 사용할 JSON 페이로드 생성
     * 필요한 필드는 추후 Event DTO로 분리해도 된다.
     */
    private String buildRefundFailedPayload(Payment payment, RefundPgWebhookCommand command, Long amount) {
        String failureCode = command.failureCode() != null ? command.failureCode() : "";
        String failureMessage = command.failureMessage() != null ? command.failureMessage() : "";

        return """
                {
                  "paymentId": "%s",
                  "paymentKey": "%s",
                  "refundAmount": %d,
                  "failureCode": "%s",
                  "failureMessage": "%s"
                }
                """.formatted(
                payment.getPaymentId(),
                payment.getPaymentKey(),
                amount,
                failureCode,
                failureMessage
        );
    }
}
