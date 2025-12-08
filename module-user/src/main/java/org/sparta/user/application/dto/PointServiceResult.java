package org.sparta.user.application.dto;

import java.util.List;
import java.util.UUID;

public class PointServiceResult {

    /**
     * 포인트 예약 결과(Kafka Event)
     * 예약 자체는 REST API과정으로 이루어지므로 해당 record는 당장 사용하지 않음
     */
    public record Reserve(
            UUID orderId,
            Long discountAmount,
            List<PointUsageDetail> reservedPoints
    ) {}

    /**
     * 포인트 확정 결과
     */
    public record Confirm(
            UUID orderId,
            Long discountAmount,
            List<PointUsageDetail> confirmedPoints
    ) {}

    /**
     * 포인트 조각 단위 예약/사용 상세
     */
    public record PointUsageDetail(
            UUID pointId,
            Long usedAmount
    ) {}
}
