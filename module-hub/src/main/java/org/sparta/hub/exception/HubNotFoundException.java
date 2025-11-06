package org.sparta.hub.exception;

import org.sparta.common.error.BusinessException;
import org.sparta.common.error.CommonErrorType;

import java.util.UUID;

public class HubNotFoundException extends BusinessException {
    public HubNotFoundException(UUID hubId) {
        super(CommonErrorType.NOT_FOUND, "Hub not found: " + hubId);
    }
}
