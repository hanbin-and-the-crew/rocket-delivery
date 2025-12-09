package org.sparta.deliverylog.domain.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum DeliveryLogErrorType implements ErrorType {

    DELIVERY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "deliveryId는 필수입니다."),
    SEQUENCE_REQUIRED(HttpStatus.BAD_REQUEST, "sequence는 1 이상이어야 합니다."),
    SOURCE_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "sourceHubId는 필수입니다."),
    TARGET_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "targetHubId는 필수입니다."),
    INVALID_ESTIMATED_KM(HttpStatus.BAD_REQUEST, "estimatedKm는 0 이상이어야 합니다."),
    INVALID_ESTIMATED_MINUTES(HttpStatus.BAD_REQUEST, "estimatedMinutes는 0 이상이어야 합니다."),
    DELIVERY_MAN_ID_REQUIRED(HttpStatus.BAD_REQUEST, "deliveryManId는 필수입니다."),
    CANNOT_ASSIGN_ON_CANCELED(HttpStatus.BAD_REQUEST, "취소된 로그에는 담당자를 배정할 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 전이입니다."),
    INVALID_ACTUAL_KM(HttpStatus.BAD_REQUEST, "actualKm는 0 이상이어야 합니다."),
    INVALID_ACTUAL_MINUTES(HttpStatus.BAD_REQUEST, "actualMinutes는 0 이상이어야 합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryLogErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "deliveryLog:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}
