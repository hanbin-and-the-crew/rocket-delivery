package org.sparta.hub.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

/**
 * 허브 이름 중복 시 발생하는 예외
 */
public class DuplicateHubNameException extends BusinessException {
    public DuplicateHubNameException(String name) {
        super(CommonErrorType.CONFLICT, "이미 존재하는 허브명입니다: " + name);
    }
}
