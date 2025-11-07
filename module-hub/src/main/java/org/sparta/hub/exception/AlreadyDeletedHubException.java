package org.sparta.hub.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

/**
 * 이미 삭제된 허브를 다시 삭제하려 할 때 발생하는 예외
 */
public class AlreadyDeletedHubException extends BusinessException {
    public AlreadyDeletedHubException() {
        super(CommonErrorType.CONFLICT, "이미 삭제된 허브입니다");
    }
}
