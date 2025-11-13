package org.sparta.slack.domain.vo;

/**
 * 위경도 좌표
 */
public record GeoPoint(
        double latitude,
        double longitude
) {
}
