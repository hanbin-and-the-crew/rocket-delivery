package org.sparta.hub.domain.model;

import lombok.Getter;

@Getter
public enum HubRouteStatus {

    ACTIVE("활성"),       // 정상적으로 운행 중인 경로
    INACTIVE("비활성"),   // 일시 중단된 경로
    UNDER_REVIEW("검토중"), // 신규 개설이나 점검 중인 경로
    CLOSED("폐쇄");       // 완전히 폐쇄된 경로

    private final String description;

    HubRouteStatus(String description) {
        this.description = description;
    }
}
