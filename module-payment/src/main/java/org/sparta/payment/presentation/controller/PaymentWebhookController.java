package org.sparta.payment.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta.payment.application.command.refund.RefundPgWebhookCommand;
import org.sparta.payment.application.service.RefundService;
import org.sparta.payment.presentation.dto.request.PgRefundWebhookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PG(카드사)에서 보내주는 결제/환불 웹훅을 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final RefundService refundService;

    /**
     * 환불 결과 웹훅
     * 예: POST /payments/webhook/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<Void> handleRefundWebhook(
            @Valid @RequestBody PgRefundWebhookRequest request
    ) {
        RefundPgWebhookCommand command = new RefundPgWebhookCommand(
                request.paymentKey(),
                request.refundKey(),
                request.amount(),
                request.status(),
                request.failureCode(),
                request.failureMessage(),
                request.occurredAt()
        );

        // 실제 비즈니스 처리는 Service에 위임
        refundService.handlePgRefundWebhook(command);

        // PG 쪽에는 일단 200 OK만 빠르게 응답
        return ResponseEntity.ok().build();
    }
}
