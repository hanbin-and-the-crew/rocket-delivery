package org.sparta.payment.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.payment.domain.enumeration.PaymentAttemptStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "p_payment_attempts")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentAttemptId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentAttemptStatus status;

    @Lob
    @Column(name = "request_payload")
    private String requestPayload;

    @Lob
    @Column(name = "response_payload")
    private String responsePayload;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // --------- Factory Methods --------- //

    public static PaymentAttempt start(UUID paymentId, Integer attemptNo, String requestPayload) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.paymentId = paymentId;
        attempt.attemptNo = attemptNo;
        attempt.status = PaymentAttemptStatus.PENDING;
        attempt.requestPayload = requestPayload;
        attempt.requestedAt = LocalDateTime.now();
        attempt.createdAt = LocalDateTime.now();
        return attempt;
    }

    public void success(String pgTransactionId, String responsePayload) {
        this.status = PaymentAttemptStatus.SUCCESS;
        this.pgTransactionId = pgTransactionId;
        this.responsePayload = responsePayload;
        this.errorCode = null;
        this.errorMessage = null;
        this.respondedAt = LocalDateTime.now();
    }

    public void fail(String errorCode, String errorMessage, String responsePayload) {
        this.status = PaymentAttemptStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.responsePayload = responsePayload;
        this.respondedAt = LocalDateTime.now();
    }

    public void timeout() {
        this.status = PaymentAttemptStatus.TIMEOUT;
        this.respondedAt = LocalDateTime.now();
    }
}
