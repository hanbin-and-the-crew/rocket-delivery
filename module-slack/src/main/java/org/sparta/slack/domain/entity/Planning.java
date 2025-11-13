package org.sparta.slack.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.slack.domain.enums.PlanningStatus;
import org.sparta.slack.domain.vo.PayloadSnapshot;
import org.sparta.slack.error.SlackErrorType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Planning Aggregate Root (AI Planning Context)
 * AI를 통한 최종 발송 시한 계산 관리
 * 주문 기반 AI 계산 요청부터 결과 확정까지 단일 트랜잭션 경계로 관리
 */
@Entity
@Getter
@Table(
        name = "p_plannings",
        indexes = {
                @Index(name = "idx_order_id_status", columnList = "order_id, status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Planning extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanningStatus status;

    @Embedded
    private PayloadSnapshot payloadSnapshot;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "response_id")
    private PlanningResponse response;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    private Planning(
            UUID orderId,
            PayloadSnapshot payloadSnapshot
    ) {
        this.orderId = orderId;
        this.status = PlanningStatus.REQUESTED;
        this.payloadSnapshot = payloadSnapshot;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * Planning 요청 생성 팩토리 메서드
     * 동일 orderId에 동시에 하나의 REQUESTED 상태만 존재해야 함 (Idempotency)
     */
    public static Planning create(
            UUID orderId,
            String productInfo,
            Integer quantity,
            LocalDateTime deliveryDeadline,
            UUID originHubId,
            UUID destinationHubId,
            String transitRoute
    ) {
        validateOrderId(orderId);

        PayloadSnapshot snapshot = PayloadSnapshot.of(
                productInfo,
                quantity,
                deliveryDeadline,
                originHubId,
                destinationHubId,
                transitRoute
        );

        return new Planning(orderId, snapshot);
    }

    /**
     * Planning 요청 생성 팩토리 메서드 (근무시간 포함)
     */
    public static Planning create(
            UUID orderId,
            String productInfo,
            Integer quantity,
            LocalDateTime deliveryDeadline,
            UUID originHubId,
            UUID destinationHubId,
            String transitRoute,
            Integer workStartHour,
            Integer workEndHour
    ) {
        validateOrderId(orderId);

        PayloadSnapshot snapshot = PayloadSnapshot.of(
                productInfo,
                quantity,
                deliveryDeadline,
                originHubId,
                destinationHubId,
                transitRoute,
                workStartHour,
                workEndHour
        );

        return new Planning(orderId, snapshot);
    }

    private static void validateOrderId(UUID orderId) {
        if (orderId == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "주문 ID는 필수입니다");
        }
    }

    /**
     * AI 응답 처리 및 완료 상태로 전환
     */
    public void completeWithResponse(
            LocalDateTime deadline,
            String routeSummary,
            String reason,
            String aiRawOutput
    ) {
        if (this.status != PlanningStatus.REQUESTED) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "REQUESTED 상태에서만 완료 처리가 가능합니다");
        }

        this.response = PlanningResponse.create(
                this.id,
                deadline,
                routeSummary,
                reason,
                aiRawOutput
        );
        this.status = PlanningStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * AI 요청 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        if (this.status != PlanningStatus.REQUESTED) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "REQUESTED 상태에서만 실패 처리가 가능합니다");
        }

        this.status = PlanningStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 재요청 가능 여부 확인
     */
    public boolean canRetry() {
        return this.status == PlanningStatus.FAILED;
    }

    /**
     * 재요청 처리
     */
    public void retry() {
        if (!canRetry()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "재요청할 수 없는 상태입니다");
        }
        this.status = PlanningStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
        this.completedAt = null;
        this.errorMessage = null;
        this.response = null;
    }

    /**
     * 완료 여부 확인
     */
    public boolean isCompleted() {
        return this.status == PlanningStatus.COMPLETED;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailed() {
        return this.status == PlanningStatus.FAILED;
    }

    /**
     * 최종 발송 시한 조회
     */
    public LocalDateTime getDeadline() {
        if (!isCompleted() || this.response == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_STATE, "완료된 Planning이 아니거나 응답이 없습니다");
        }
        return this.response.getDeadline().getValue();
    }
}
