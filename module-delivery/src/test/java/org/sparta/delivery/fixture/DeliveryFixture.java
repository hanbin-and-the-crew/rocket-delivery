package org.sparta.delivery.fixture;

import org.sparta.delivery.domain.entity.Delivery;

import java.util.UUID;

public class DeliveryFixture {

    public static Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구 테헤란로 123",
                "홍길동",
                "@hong"
        );
    }

    public static Delivery createDelivery(UUID orderId) {
        return Delivery.create(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구 테헤란로 123",
                "홍길동",
                "@hong"
        );
    }

    public static Delivery createDeliveryWithHub(UUID departureHubId, UUID destinationHubId) {
        return Delivery.create(
                UUID.randomUUID(),
                departureHubId,
                destinationHubId,
                "서울시 강남구 테헤란로 123",
                "홍길동",
                "@hong"
        );
    }

    public static Delivery createDeliveryWithRecipient(String recipientName, String recipientSlackId) {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구 테헤란로 123",
                recipientName,
                recipientSlackId
        );
    }
}
