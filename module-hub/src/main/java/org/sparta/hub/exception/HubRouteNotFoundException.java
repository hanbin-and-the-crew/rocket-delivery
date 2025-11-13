package org.sparta.hub.exception;

import java.util.UUID;

public class HubRouteNotFoundException extends RuntimeException {
    public HubRouteNotFoundException(UUID id) {
        super("HubRoute not found with id: " + id);
    }
}
