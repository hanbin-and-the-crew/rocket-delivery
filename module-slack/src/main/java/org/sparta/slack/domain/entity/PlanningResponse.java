package org.sparta.slack.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.domain.vo.Deadline;
import org.sparta.slack.error.SlackErrorType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI Planning 응답 정보
 * Planning Aggregate의 일부
 */
@Entity
@Getter
@Table(name = "p_planning_responses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanningResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "planning_request_id", nullable = false)
    private UUID planningRequestId;

    @Embedded
    private Deadline deadline;

    @Column(name = "route_summary", length = 1000)
    private String routeSummary;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "ai_raw_output", columnDefinition = "TEXT")
    private String aiRawOutput;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    private PlanningResponse(
            UUID planningRequestId,
            Deadline deadline,
            String routeSummary,
            String reason,
            String aiRawOutput
    ) {
        this.planningRequestId = planningRequestId;
        this.deadline = deadline;
        this.routeSummary = routeSummary;
        this.reason = reason;
        this.aiRawOutput = aiRawOutput;
        this.respondedAt = LocalDateTime.now();
    }

    static PlanningResponse create(
            UUID planningRequestId,
            LocalDateTime deadline,
            String routeSummary,
            String reason,
            String aiRawOutput
    ) {
        if (planningRequestId == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "Planning 요청 ID는 필수입니다");
        }
        if (deadline == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "최종 발송 시한은 필수입니다");
        }

        return new PlanningResponse(
                planningRequestId,
                Deadline.of(deadline),
                routeSummary,
                reason,
                aiRawOutput
        );
    }

    /**
     * 경유지 요약 정보 업데이트
     */
    public void updateRouteSummary(String routeSummary) {
        this.routeSummary = routeSummary;
    }

    /**
     * 발송 시한 결정 사유 업데이트
     */
    public void updateReason(String reason) {
        this.reason = reason;
    }
}
