package org.sparta.order.application.dto;

import java.util.UUID;

/**
 * 결제 승인 결과 DTO
 */
public record PaymentApprovalResult(
    UUID orderId,            // 주문 ID
    String paymentKey,       // 결제 키
    boolean approved,        // 승인 여부
    String approvedAt        // 승인 시각
) {}