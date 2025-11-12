package org.sparta.slack.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

/**
 * 배송 경로 내 단일 방문 지점의 스냅샷
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteStopSnapshot(
        UUID deliveryId,
        String label,
        String address,
        Double latitude,
        Double longitude,
        Integer sequence
) {

    public RouteStopSnapshot {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("경로 지점 라벨은 필수입니다");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("경로 지점 주소는 필수입니다");
        }
    }

    public RouteStopSnapshot withSequence(int sequence) {
        return new RouteStopSnapshot(deliveryId, label, address, latitude, longitude, sequence);
    }

    public RouteStopSnapshot withCoordinate(double lat, double lng) {
        return new RouteStopSnapshot(deliveryId, label, address, lat, lng, sequence);
    }
}
