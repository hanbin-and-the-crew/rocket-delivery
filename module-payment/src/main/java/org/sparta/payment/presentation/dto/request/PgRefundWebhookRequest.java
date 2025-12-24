package org.sparta.payment.presentation.dto.request;

import java.time.OffsetDateTime;

/**
 * PG(카드사)에서 보내주는 환불 웹훅 페이로드를 받기 위한 DTO
 * 실제 PG 스펙과 맞춰서 필드는 나중에 조정하면 됨.
 */
public record PgRefundWebhookRequest(
        String paymentKey,         // PG 결제 키
        String refundKey,          // PG 환불 키 (부분 환불 구분용)
        Long amount,               // 환불 금액
        String status,             // SUCCESS / FAIL 등
        String failureCode,        // 실패 시 코드
        String failureMessage,     // 실패 시 메시지
        OffsetDateTime occurredAt  // PG 기준 발생 시각
) {
}
