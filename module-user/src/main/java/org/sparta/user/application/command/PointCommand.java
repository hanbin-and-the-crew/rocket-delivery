package org.sparta.user.application.command;

import java.util.UUID;

public class PointCommand {

    public record ReservePoint(
            UUID userId,
            UUID orderId,
            Long orderAmount,
            Long requestPoint
    ) {}

    public record ConfirmPoint(
            UUID orderId
    ) {}
}