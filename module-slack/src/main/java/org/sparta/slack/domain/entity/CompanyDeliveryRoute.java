package org.sparta.slack.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.slack.domain.converter.RouteStopConverter;
import org.sparta.slack.domain.enums.RouteStatus;
import org.sparta.slack.domain.vo.RoutePlanningResult;
import org.sparta.slack.domain.vo.RouteStopSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 업체 배송 경로 기록 Aggregate
 */
@Getter
@Entity
@Table(
        name = "p_company_delivery_routes",
        uniqueConstraints = @UniqueConstraint(name = "uk_route_delivery", columnNames = "delivery_id"),
        indexes = {
                @Index(name = "idx_route_schedule_status", columnList = "scheduled_date,status"),
                @Index(name = "idx_route_manager_schedule", columnList = "delivery_manager_id,scheduled_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyDeliveryRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "origin_hub_id", nullable = false)
    private UUID originHubId;

    @Column(name = "origin_hub_name", length = 200)
    private String originHubName;

    @Column(name = "origin_address", length = 1000)
    private String originAddress;

    @Column(name = "destination_company_id", nullable = false)
    private UUID destinationCompanyId;

    @Column(name = "destination_company_name", nullable = false, length = 200)
    private String destinationCompanyName;

    @Column(name = "destination_address", nullable = false, length = 1000)
    private String destinationAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RouteStatus status;

    @Column(name = "delivery_manager_id")
    private UUID deliveryManagerId;

    @Column(name = "delivery_manager_name", length = 100)
    private String deliveryManagerName;

    @Column(name = "delivery_manager_slack_id", length = 200)
    private String deliveryManagerSlackId;

    @Column(name = "delivery_order")
    private Integer deliveryOrder;

    @Column(name = "expected_distance_meters")
    private Long expectedDistanceMeters;

    @Column(name = "expected_duration_minutes")
    private Integer expectedDurationMinutes;

    @Column(name = "actual_distance_meters")
    private Long actualDistanceMeters;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "route_summary", columnDefinition = "TEXT")
    private String routeSummary;

    @Column(name = "ai_reason", columnDefinition = "TEXT")
    private String aiReason;

    @Column(name = "naver_route_link", length = 1000)
    private String naverRouteLink;

    @Column(name = "dispatch_message_id")
    private UUID dispatchMessageId;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "scheduled_dispatch_time")
    private LocalTime scheduledDispatchTime;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "waypoints_payload", columnDefinition = "TEXT")
    private String waypointsPayload;

    @Convert(converter = RouteStopConverter.class)
    @Column(name = "stops_payload", columnDefinition = "TEXT")
    private List<RouteStopSnapshot> stops = new ArrayList<>();

    private CompanyDeliveryRoute(
            UUID deliveryId,
            LocalDate scheduledDate,
            UUID originHubId,
            String originHubName,
            String originAddress,
            UUID destinationCompanyId,
            String destinationCompanyName,
            String destinationAddress,
            List<RouteStopSnapshot> stops
    ) {
        validateMandatory(deliveryId, scheduledDate, originHubId, destinationCompanyId, originAddress, destinationAddress);

        this.deliveryId = deliveryId;
        this.scheduledDate = scheduledDate;
        this.originHubId = originHubId;
        this.originHubName = originHubName;
        this.originAddress = originAddress;
        this.destinationCompanyId = destinationCompanyId;
        this.destinationCompanyName = destinationCompanyName;
        this.destinationAddress = destinationAddress;
        this.status = RouteStatus.PENDING;
        this.stops = stops != null ? new ArrayList<>(stops) : new ArrayList<>();
    }

    public static CompanyDeliveryRoute create(
            UUID deliveryId,
            LocalDate scheduledDate,
            UUID originHubId,
            String originHubName,
            String originAddress,
            UUID destinationCompanyId,
            String destinationCompanyName,
            String destinationAddress,
            List<RouteStopSnapshot> stops
    ) {
        return new CompanyDeliveryRoute(
                deliveryId,
                scheduledDate,
                originHubId,
                originHubName,
                originAddress,
                destinationCompanyId,
                destinationCompanyName,
                destinationAddress,
                stops
        );
    }

    private void validateMandatory(
            UUID deliveryId,
            LocalDate scheduledDate,
            UUID originHubId,
            UUID destinationCompanyId,
            String originAddress,
            String destinationAddress
    ) {
        if (deliveryId == null) {
            throw new IllegalArgumentException("deliveryId는 필수입니다");
        }
        if (scheduledDate == null) {
            throw new IllegalArgumentException("scheduledDate는 필수입니다");
        }
        if (originHubId == null) {
            throw new IllegalArgumentException("originHubId는 필수입니다");
        }
        if (destinationCompanyId == null) {
            throw new IllegalArgumentException("destinationCompanyId는 필수입니다");
        }
        if (originAddress == null || originAddress.isBlank()) {
            throw new IllegalArgumentException("originAddress는 필수입니다");
        }
        if (destinationAddress == null || destinationAddress.isBlank()) {
            throw new IllegalArgumentException("destinationAddress는 필수입니다");
        }
    }

    public boolean requiresAssignment() {
        return deliveryManagerId == null && (status == RouteStatus.PENDING || status == RouteStatus.FAILED);
    }

    public void assignManager(UUID managerId, String managerName, String slackId, int order) {
        if (managerId == null) {
            throw new IllegalArgumentException("managerId는 필수입니다");
        }
        if (slackId == null || slackId.isBlank()) {
            throw new IllegalArgumentException("담당자 Slack ID는 필수입니다");
        }

        this.deliveryManagerId = managerId;
        this.deliveryManagerName = managerName;
        this.deliveryManagerSlackId = slackId;
        this.deliveryOrder = order;
        this.status = RouteStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
    }

    public void applyPlanningResult(RoutePlanningResult result) {
        Objects.requireNonNull(result, "경로 계획 결과는 필수입니다");
        this.stops = new ArrayList<>(result.orderedStops());
        this.expectedDistanceMeters = result.expectedDistanceMeters();
        this.expectedDurationMinutes = result.expectedDurationMinutes();
        this.routeSummary = result.routeSummary();
        this.aiReason = result.aiReason();
        this.naverRouteLink = result.naverRouteLink();
        this.waypointsPayload = result.rawWaypoints();
        this.status = RouteStatus.PLANNED;
        this.failureReason = null;
    }

    public void markPlanningFailed(String reason) {
        this.failureReason = reason;
        this.status = RouteStatus.FAILED;
    }

    public void markDispatched(UUID messageId) {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId는 필수입니다");
        }
        this.dispatchMessageId = messageId;
        this.dispatchedAt = LocalDateTime.now();
        this.status = RouteStatus.DISPATCHED;
    }

    public void markInProgress() {
        this.status = RouteStatus.IN_PROGRESS;
    }

    public void markCompleted(long actualDistanceMeters, int actualDurationMinutes) {
        this.actualDistanceMeters = actualDistanceMeters;
        this.actualDurationMinutes = actualDurationMinutes;
        this.status = RouteStatus.COMPLETED;
    }

    public void updateBasicInfo(
            LocalDate scheduledDate,
            String originHubName,
            String originAddress,
            String destinationCompanyName,
            String destinationAddress,
            List<RouteStopSnapshot> newStops
    ) {
        if (scheduledDate != null) {
            this.scheduledDate = scheduledDate;
        }
        if (originHubName != null && !originHubName.isBlank()) {
            this.originHubName = originHubName;
        }
        if (originAddress != null && !originAddress.isBlank()) {
            this.originAddress = originAddress;
        }
        if (destinationCompanyName != null && !destinationCompanyName.isBlank()) {
            this.destinationCompanyName = destinationCompanyName;
        }
        if (destinationAddress != null && !destinationAddress.isBlank()) {
            this.destinationAddress = destinationAddress;
        }
        if (newStops != null && !newStops.isEmpty()) {
            this.stops = new ArrayList<>(newStops);
            this.status = RouteStatus.PENDING;
        }
    }
}
