package org.sparta.hub.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

import java.util.UUID;



/**
 * 허브가 존재하지 않을 때 발생하는 예외
 */
public class HubNotFoundException extends BusinessException {
    public HubNotFoundException(UUID hubId) {
        // 테스트 기대 메시지에 맞춰 uuid 미포함
        super(CommonErrorType.NOT_FOUND, "Hub not found");
    }
}
