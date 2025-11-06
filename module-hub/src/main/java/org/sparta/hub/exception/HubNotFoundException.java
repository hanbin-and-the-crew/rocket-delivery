package org.sparta.hub.exception;

import java.util.UUID;

public class HubNotFoundException extends RuntimeException {
    public HubNotFoundException(UUID hubId) {
        super("해당 허브를 찾을 수 없습니다. hubId=" + hubId);
    }
}
