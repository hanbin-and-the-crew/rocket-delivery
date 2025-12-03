package org.sparta.order.application.command;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderCommand {

    public record Create(
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiptCompanyId,
            UUID receiptHubId,
            UUID productId,
            Integer quantity,
            Integer productPrice,
            String address,
            String userName,
            String userPhoneNumber,
            String slackId,
            LocalDateTime dueAt,
            String requestMemo
    ) {}

    public record ChangeDueAt(
            LocalDateTime dueAt
    ) {}

    public record changeRequestMemo(
            String requestedMemo
    ) {}

    public record ChangeAddress(
            String addressSnapshot
    ) {}

    public record Cancel(
            UUID orderId,
            String reasonCode,
            String reasonMemo
    ) {}

    public record ShipOrder(
            UUID orderId
    ) {}

    public record DeliverOrder(
            UUID orderId
    ) {}

    public record DeleteOrder(
            UUID orderId
    ) {}
}