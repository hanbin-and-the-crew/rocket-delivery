package org.sparta.payment.domain.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.payment.domain.enumeration.RefundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "p_refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_id", nullable = false, updatable = false)
    private UUID refundId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "refund_key", length = 100)
    private String refundKey; // PG 환불 키

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // --------- Factory Methods --------- //

    public static Refund request(UUID paymentId, Long amount, String reason) {
        Refund refund = new Refund();
        refund.paymentId = paymentId;
        refund.amount = amount;
        refund.reason = reason;
        refund.status = RefundStatus.REQUESTED;
        refund.requestedAt = LocalDateTime.now();
        refund.createdAt = LocalDateTime.now();
        return refund;
    }

    public void markCompleted(String refundKey) {
        this.status = RefundStatus.COMPLETED;
        this.refundKey = refundKey;
        this.completedAt = LocalDateTime.now();
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(String code, String message) {
        this.status = RefundStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = message;
        this.failedAt = LocalDateTime.now();
    }
}
