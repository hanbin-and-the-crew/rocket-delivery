package org.sparta.payment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;
import org.sparta.payment.domain.error.PaymentErrorType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payment_key", length = 100)
    private String paymentKey; // PG 측 결제 키 (예: toss paymentKey)

    // ===== 금액 관련 =====

    @Column(name = "amount_total", nullable = false)
    private Long amountTotal;      // 주문 총 금액(할인 전)

    @Column(name = "amount_coupon", nullable = false)
    private Long amountCoupon;     // 쿠폰 할인 금액

    @Column(name = "amount_point", nullable = false)
    private Long amountPoint;      // 포인트 사용 금액

    @Column(name = "amount_payable", nullable = false)
    private Long amountPayable;    // 실제 PG로 나가는 금액

    @Column(name = "amount_paid", nullable = false)
    private Long amountPaid;       // 실제 승인 완료된 금액

    @Column(name = "currency", length = 10, nullable = false)
    private String currency;       // KRW 등

    // ===== 상태 / PG 정보 =====

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", length = 30, nullable = false)
    private PaymentType methodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", length = 30, nullable = false)
    private PgProvider pgProvider;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", length = 255)
    private String failureMessage;

    // ===== 타임라인 =====

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    // ===== 쿠폰/포인트 식별자 (외부 서비스 연동용) =====

    @Column(name = "coupon_id")
    private UUID couponId;

    @Column(name = "point_usage_id")
    private UUID pointUsageId;

    // ===== 낙관적 락 / 소프트 삭제 / 감사 컬럼 =====

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ====== 생성 팩토리 ======

    public static Payment createRequested(
            UUID orderId,
            Long amountTotal,
            Long amountCoupon,
            Long amountPoint,
            Long amountPayable,
            PaymentType methodType,
            PgProvider pgProvider,
            String currency,
            UUID couponId,
            UUID pointUsageId
    ) {
        if (amountTotal == null || amountTotal <= 0) {
            throw new BusinessException(PaymentErrorType.INVALID_AMOUNT, "amountTotal must be positive");
        }
        long coupon = nvl(amountCoupon);
        long point = nvl(amountPoint);
        long payable = amountPayable == null ? 0L : amountPayable;

        if (payable < 0) {
            throw new BusinessException(PaymentErrorType.INVALID_AMOUNT, "amountPayable must be >= 0");
        }
        if (amountTotal != coupon + point + payable) {
            throw new BusinessException(PaymentErrorType.PAYMENT_AMOUNT_MISMATCH,
                    "amountTotal != coupon + point + payable");
        }

        Payment p = new Payment();
        p.orderId = orderId;
        p.amountTotal = amountTotal;
        p.amountCoupon = coupon;
        p.amountPoint = point;
        p.amountPayable = payable;
        p.amountPaid = 0L;
        p.methodType = methodType;
        p.pgProvider = pgProvider;
        p.currency = currency;
        p.couponId = couponId;
        p.pointUsageId = pointUsageId;
        p.status = PaymentStatus.REQUESTED;
        p.requestedAt = LocalDateTime.now();
        p.createdAt = LocalDateTime.now();
        p.version = 0;
        return p;
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v;
    }

    // ====== 도메인 메서드 ======

    /**
     * PG 승인 완료 처리
     */
    public void complete(String paymentKey, long approvedAmount) {
        if (this.status != PaymentStatus.REQUESTED && this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제를 완료할 수 없는 상태입니다. status=" + status);
        }
        if (approvedAmount != this.amountPayable) {
            throw new BusinessException(
                    PaymentErrorType.PAYMENT_AMOUNT_MISMATCH,
                    "approvedAmount != amountPayable"
            );
        }
        this.paymentKey = paymentKey;
        this.amountPaid = approvedAmount;
        this.status = PaymentStatus.COMPLETED;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * PG/내부 로직 실패 처리
     */
    public void fail(String failureCode, String failureMessage) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 전체 취소 처리
     */
    public void cancelAll() {
        if (this.status == PaymentStatus.CANCELED || this.status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("이미 취소/환불된 결제입니다.");
        }
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.amountPaid = 0L;
    }

    /**
     * 부분 환불 적용
     */
    public void applyRefund(long refundAmount) {
        if (refundAmount <= 0) {
            throw new BusinessException(
                    PaymentErrorType.REFUND_AMOUNT_INVALID,
                    "refundAmount must be positive"
            );
        }
        if (refundAmount > this.amountPaid) {
            throw new BusinessException(
                    PaymentErrorType.REFUND_AMOUNT_EXCEEDED,
                    "refundAmount > amountPaid"
            );
        }

        this.amountPaid -= refundAmount;
        if (this.amountPaid == 0L) {
            this.status = PaymentStatus.REFUNDED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // 소프트 삭제용 헬퍼 (있으면)
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
